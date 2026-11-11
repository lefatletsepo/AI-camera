package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.AspectRatioSetting
import com.example.model.CameraMode
import com.example.model.FlashMode
import com.example.storage.MediaStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraManagerController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val previewView: PreviewView
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set

    var currentMode: CameraMode = CameraMode.PHOTO
        private set

    var currentFlash: FlashMode = FlashMode.AUTO
        private set

    var currentAspectRatio: AspectRatioSetting = AspectRatioSetting.RATIO_4_3
        private set

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec = _recordingDurationSec.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio = _zoomRatio.asStateFlow()

    private val _exposureCompensation = MutableStateFlow(0)
    val exposureCompensation = _exposureCompensation.asStateFlow()

    private val _exposureRange = MutableStateFlow(Range(-4, 4))
    val exposureRange = _exposureRange.asStateFlow()

    private val _is4kSupported = MutableStateFlow(false)
    val is4kSupported = _is4kSupported.asStateFlow()

    init {
        checkHardwareCapabilities()
    }

    private fun checkHardwareCapabilities() {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    if (map != null) {
                        for (sz in map.getOutputSizes(android.media.MediaRecorder::class.java)) {
                            if (sz.width >= 3840 && sz.height >= 2160) {
                                _is4kSupported.value = true
                                break
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startCamera(onReady: () -> Unit = {}) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindUseCases()
                onReady()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindUseCases()
    }

    fun setMode(mode: CameraMode) {
        if (currentMode == mode) return
        if (currentMode == CameraMode.VIDEO && _isRecording.value) {
            stopVideoRecording()
        }
        currentMode = mode
        bindUseCases()
    }

    fun setFlashMode(flash: FlashMode) {
        currentFlash = flash
        val nativeFlash = when (flash) {
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.TORCH -> {
                camera?.cameraControl?.enableTorch(true)
                ImageCapture.FLASH_MODE_OFF
            }
        }
        if (flash != FlashMode.TORCH) {
            camera?.cameraControl?.enableTorch(false)
        }
        imageCapture?.flashMode = nativeFlash
    }

    fun setAspectRatio(setting: AspectRatioSetting) {
        currentAspectRatio = setting
        bindUseCases()
    }

    private fun bindUseCases() {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val cameraAspectRatio = if (currentAspectRatio == AspectRatioSetting.RATIO_16_9) {
            AspectRatio.RATIO_16_9
        } else {
            AspectRatio.RATIO_4_3
        }

        val preview = Preview.Builder()
            .setTargetAspectRatio(cameraAspectRatio)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        if (currentMode == CameraMode.VIDEO) {
            val cameraInfo = try {
                provider.getCameraInfo(selector)
            } catch (e: Exception) {
                null
            }

            val videoCapabilities = if (cameraInfo != null) {
                try {
                    Recorder.getVideoCapabilities(cameraInfo)
                } catch (e: Exception) {
                    null
                }
            } else null

            val supported: List<Quality> = videoCapabilities?.getSupportedQualities(DynamicRange.SDR) ?: emptyList()

            val qualitySelector = if (supported.isNotEmpty()) {
                val candidateList = mutableListOf<Quality>()
                if (_is4kSupported.value && supported.contains(Quality.UHD)) {
                    candidateList.add(Quality.UHD)
                }
                if (supported.contains(Quality.FHD)) candidateList.add(Quality.FHD)
                if (supported.contains(Quality.HD)) candidateList.add(Quality.HD)
                if (supported.contains(Quality.SD)) candidateList.add(Quality.SD)

                if (candidateList.isNotEmpty()) {
                    QualitySelector.fromOrderedList(
                        candidateList,
                        FallbackStrategy.lowerQualityOrHigherThan(candidateList.last())
                    )
                } else {
                    QualitySelector.from(
                        supported.first(),
                        FallbackStrategy.lowerQualityOrHigherThan(supported.first())
                    )
                }
            } else {
                QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.LOWEST)
                )
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .setExecutor(cameraExecutor)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            try {
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
                updateExposureAndZoomStates()
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful fallback to preview-only if device/emulator videoCapture binding encounters a HAL failure
                try {
                    camera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                    updateExposureAndZoomStates()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } else {
            val captureMode = when (currentMode) {
                CameraMode.NIGHT -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                CameraMode.PRO -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                else -> ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            }

            val flash = when (currentFlash) {
                FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(captureMode)
                .setTargetAspectRatio(cameraAspectRatio)
                .setFlashMode(flash)
                .build()

            try {
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                updateExposureAndZoomStates()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateExposureAndZoomStates() {
        camera?.cameraInfo?.exposureState?.let { exp ->
            _exposureRange.value = Range(exp.exposureCompensationRange.lower, exp.exposureCompensationRange.upper)
            _exposureCompensation.value = exp.exposureCompensationIndex
        }
        camera?.cameraInfo?.zoomState?.value?.let { z ->
            _zoomRatio.value = z.zoomRatio
        }
    }

    fun takePhoto(
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera image capture is not initialized"))
            return
        }

        val destFile = MediaStorageManager.createTempPhotoFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(destFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    ContextCompat.getMainExecutor(context).execute {
                        onSuccess(destFile)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exception)
                    }
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        onStarted: () -> Unit,
        onFinished: (File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = videoCapture ?: run {
            onError(IllegalStateException("Video capture is not available on this device sensor."))
            return
        }
        val videoFile = MediaStorageManager.createTempVideoFile(context)
        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

        val pendingRecording = capture.output
            .prepareRecording(context, fileOutputOptions)
            .apply {
                try {
                    withAudioEnabled()
                } catch (e: Exception) {
                    // Audio permission denied or microphone hardware absent
                }
            }

        _recordingDurationSec.value = 0
        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _isRecording.value = true
                    onStarted()
                }
                is VideoRecordEvent.Status -> {
                    _recordingDurationSec.value = (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                }
                is VideoRecordEvent.Finalize -> {
                    _isRecording.value = false
                    if (!event.hasError()) {
                        onFinished(videoFile)
                    } else {
                        onError(event.cause ?: RuntimeException("Video recording finalized with error"))
                    }
                }
            }
        }
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
        _isRecording.value = false
    }

    fun focusOnPoint(x: Float, y: Float) {
        val cam = camera ?: return
        val factory = SurfaceOrientedMeteringPointFactory(
            previewView.width.toFloat(),
            previewView.height.toFloat()
        )
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
        _zoomRatio.value = ratio
    }

    fun setExposureCompensation(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
        _exposureCompensation.value = index
    }

    fun shutdown() {
        stopVideoRecording()
        cameraExecutor.shutdown()
    }
}
