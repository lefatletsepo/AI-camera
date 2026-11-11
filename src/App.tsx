import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Camera,
  Video,
  Sparkles,
  RotateCcw,
  Settings,
  Grid,
  Zap,
  ZapOff,
  Timer,
  Share2,
  Download,
  Trash2,
  X,
  Play,
  Check,
  RefreshCw,
  Info,
  Heart,
  Sliders,
} from 'lucide-react';
import { CameraMode, FlashMode, TimerSetting, MediaItem, EnhancementMetrics } from './types';
import { enhancePhotographOnCanvas } from './aiEngine';
import { BeforeAfterSlider } from './components/BeforeAfterSlider';

export const App: React.FC = () => {
  // Navigation & Screen View
  const [activeView, setActiveView] = useState<'camera' | 'enhance' | 'gallery' | 'settings' | 'about'>('camera');

  // Camera State
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('environment');
  const [mode, setMode] = useState<CameraMode>('PHOTO');
  const [flash, setFlash] = useState<FlashMode>('auto');
  const [timer, setTimer] = useState<TimerSetting>(0);
  const [countdown, setCountdown] = useState<number>(0);
  const [isGrid, setIsGrid] = useState<boolean>(true);
  const [zoom, setZoom] = useState<number>(1);
  const [aspectRatio, setAspectRatio] = useState<'4:3' | '16:9' | '1:1'>('4:3');

  // Video Recording State
  const [isRecording, setIsRecording] = useState<boolean>(false);
  const [recordingSec, setRecordingSec] = useState<number>(0);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const recordedChunksRef = useRef<Blob[]>([]);

  // Media & AI Lab State
  const [currentRawUrl, setCurrentRawUrl] = useState<string>('');
  const [currentEnhancedUrl, setCurrentEnhancedUrl] = useState<string>('');
  const [currentMetrics, setCurrentMetrics] = useState<EnhancementMetrics | null>(null);
  const [aiIntensity, setAiIntensity] = useState<number>(0.85);
  const [isProcessingAi, setIsProcessingAi] = useState<boolean>(false);
  const [aiProgress, setAiProgress] = useState<number>(0);
  const [aiStage, setAiStage] = useState<string>('');
  const [galleryItems, setGalleryItems] = useState<MediaItem[]>([]);
  const [activeMediaItem, setActiveMediaItem] = useState<MediaItem | null>(null);
  const [galleryFilter, setGalleryFilter] = useState<'all' | 'ai' | 'photo' | 'video'>('all');

  // Video Element & Stream References
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const captureCanvasRef = useRef<HTMLCanvasElement | null>(null);

  // Initialize Camera Stream
  const startCamera = useCallback(async () => {
    try {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
      }
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: facingMode,
          width: { ideal: 3840, min: 1280 },
          height: { ideal: 2160, min: 720 },
        },
        audio: true,
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err) {
      console.warn('High-res media stream fallback:', err);
      try {
        const fallbackStream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode },
          audio: false,
        });
        streamRef.current = fallbackStream;
        if (videoRef.current) {
          videoRef.current.srcObject = fallbackStream;
        }
      } catch (e) {
        console.error('Camera permission or hardware error:', e);
      }
    }
  }, [facingMode]);

  useEffect(() => {
    if (activeView === 'camera') {
      startCamera();
    }
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
      }
    };
  }, [activeView, startCamera]);

  // Video Timer Counter
  useEffect(() => {
    let interval: any;
    if (isRecording) {
      interval = setInterval(() => {
        setRecordingSec((s) => s + 1);
      }, 1000);
    } else {
      setRecordingSec(0);
    }
    return () => clearInterval(interval);
  }, [isRecording]);

  // Shutter Execution
  const triggerShutter = () => {
    if (countdown > 0) return;
    if (timer > 0) {
      setCountdown(timer);
      const timerInt = setInterval(() => {
        setCountdown((c) => {
          if (c <= 1) {
            clearInterval(timerInt);
            executeCapture();
            return 0;
          }
          return c - 1;
        });
      }, 1000);
    } else {
      executeCapture();
    }
  };

  const executeCapture = () => {
    if (mode === 'VIDEO') {
      toggleVideoRecording();
    } else {
      takePhotograph();
    }
  };

  const takePhotograph = () => {
    const video = videoRef.current;
    if (!video) return;

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 1920;
    canvas.height = video.videoHeight || 1080;
    const ctx = canvas.getContext('2d')!;

    // Mirror if front camera
    if (facingMode === 'user') {
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1);
    }

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    captureCanvasRef.current = canvas;
    const dataUrl = canvas.toDataURL('image/jpeg', 0.95);

    setCurrentRawUrl(dataUrl);
    runAiEnhancement(canvas, aiIntensity);
    setActiveView('enhance');
  };

  const runAiEnhancement = async (sourceCanvas: HTMLCanvasElement, intensity: number) => {
    setIsProcessingAi(true);
    setAiProgress(0);
    setAiStage('Initializing neural matrices...');

    try {
      const { enhancedCanvas, metrics } = await enhancePhotographOnCanvas(
        sourceCanvas,
        intensity,
        true,
        (pct, stage) => {
          setAiProgress(pct);
          setAiStage(stage);
        }
      );
      const enhancedUrl = enhancedCanvas.toDataURL('image/jpeg', 0.98);
      setCurrentEnhancedUrl(enhancedUrl);
      setCurrentMetrics(metrics);
    } catch (err) {
      console.error('Enhancement error:', err);
    } finally {
      setIsProcessingAi(false);
    }
  };

  const toggleVideoRecording = () => {
    if (!isRecording) {
      if (!streamRef.current) return;
      recordedChunksRef.current = [];
      try {
        const recorder = new MediaRecorder(streamRef.current, {
          mimeType: MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
            ? 'video/webm;codecs=vp9'
            : 'video/webm',
        });
        recorder.ondataavailable = (e) => {
          if (e.data.size > 0) recordedChunksRef.current.push(e.data);
        };
        recorder.onstop = () => {
          const blob = new Blob(recordedChunksRef.current, { type: 'video/webm' });
          const videoUrl = URL.createObjectURL(blob);
          const newItem: MediaItem = {
            id: Date.now().toString(),
            url: videoUrl,
            type: 'video',
            isEnhanced: false,
            timestamp: Date.now(),
            name: `DARKSOULSED_4K_${Date.now()}.webm`,
          };
          setGalleryItems((prev) => [newItem, ...prev]);
        };
        recorder.start(1000);
        mediaRecorderRef.current = recorder;
        setIsRecording(true);
      } catch (e) {
        console.error('Recording initialization error:', e);
      }
    } else {
      mediaRecorderRef.current?.stop();
      setIsRecording(false);
    }
  };

  const saveEnhancedPhoto = () => {
    if (!currentEnhancedUrl) return;
    const newItem: MediaItem = {
      id: Date.now().toString(),
      url: currentEnhancedUrl,
      type: 'photo',
      isEnhanced: true,
      timestamp: Date.now(),
      name: `DARKSOULSED_AI_${Date.now()}.jpg`,
      metrics: currentMetrics || undefined,
    };
    setGalleryItems((prev) => [newItem, ...prev]);
    setActiveView('gallery');
  };

  const downloadMedia = (item: MediaItem) => {
    const a = document.createElement('a');
    a.href = item.url;
    a.download = item.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  const deleteMedia = (id: string) => {
    setGalleryItems((prev) => prev.filter((i) => i.id !== id));
    if (activeMediaItem?.id === id) setActiveMediaItem(null);
  };

  return (
    <div
      style={{
        width: '100vw',
        height: '100vh',
        backgroundColor: '#080808',
        color: '#ffffff',
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      {/* SCREEN 1: CAMERA SCREEN */}
      {activeView === 'camera' && (
        <div style={{ position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
          {/* Live Video Preview */}
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              transform: `scale(${zoom}) ${facingMode === 'user' ? 'scaleX(-1)' : ''}`,
              transition: 'transform 0.25s ease-out',
            }}
          />

          {/* Rule of Thirds Grid Overlay */}
          {isGrid && (
            <div
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                pointerEvents: 'none',
              }}
            >
              <div style={{ position: 'absolute', left: '33.33%', top: 0, bottom: 0, width: '1px', backgroundColor: 'rgba(255,255,255,0.2)' }} />
              <div style={{ position: 'absolute', left: '66.66%', top: 0, bottom: 0, width: '1px', backgroundColor: 'rgba(255,255,255,0.2)' }} />
              <div style={{ position: 'absolute', top: '33.33%', left: 0, right: 0, height: '1px', backgroundColor: 'rgba(255,255,255,0.2)' }} />
              <div style={{ position: 'absolute', top: '66.66%', left: 0, right: 0, height: '1px', backgroundColor: 'rgba(255,255,255,0.2)' }} />
            </div>
          )}

          {/* Countdown Display */}
          {countdown > 0 && (
            <div
              style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                width: '110px',
                height: '110px',
                borderRadius: '50%',
                backgroundColor: 'rgba(0,0,0,0.7)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '52px',
                fontWeight: '900',
                color: '#ffd60a',
                zIndex: 30,
              }}
            >
              {countdown}
            </div>
          )}

          {/* Recording Badge */}
          {isRecording && (
            <div
              style={{
                position: 'absolute',
                top: '80px',
                left: '50%',
                transform: 'translateX(-50%)',
                backgroundColor: 'rgba(0,0,0,0.75)',
                border: '1px solid rgba(255,59,48,0.6)',
                padding: '6px 16px',
                borderRadius: '20px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '12px',
                fontWeight: 'bold',
                color: '#ffffff',
                letterSpacing: '1px',
                zIndex: 20,
              }}
            >
              <div
                style={{
                  width: '10px',
                  height: '10px',
                  borderRadius: '50%',
                  backgroundColor: '#ff3b30',
                  animation: 'pulse 1s infinite',
                }}
              />
              {String(Math.floor(recordingSec / 60)).padStart(2, '0')}:
              {String(recordingSec % 60).padStart(2, '0')} • 4K UHD 60FPS
            </div>
          )}

          {/* Top Camera Toolbar */}
          <div
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              background: 'linear-gradient(to bottom, rgba(0,0,0,0.95), rgba(0,0,0,0.6), transparent)',
              padding: 'env(safe-area-inset-top, 16px) 16px 24px 16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              zIndex: 20,
            }}
          >
            {/* Flash */}
            <button
              onClick={() => setFlash(flash === 'off' ? 'on' : flash === 'on' ? 'auto' : 'off')}
              style={{ background: 'none', border: 'none', color: flash !== 'off' ? '#ffd60a' : '#ffffff', cursor: 'pointer', padding: '8px' }}
            >
              {flash !== 'off' ? <Zap size={22} /> : <ZapOff size={22} />}
            </button>

            {/* Timer */}
            <button
              onClick={() => setTimer(timer === 0 ? 3 : timer === 3 ? 10 : 0)}
              style={{
                background: 'none',
                border: 'none',
                color: timer !== 0 ? '#ffd60a' : '#ffffff',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                fontSize: '13px',
                fontWeight: 'bold',
              }}
            >
              <Timer size={20} />
              {timer !== 0 && `${timer}s`}
            </button>

            {/* Aspect Ratio */}
            <button
              onClick={() => setAspectRatio(aspectRatio === '4:3' ? '16:9' : aspectRatio === '16:9' ? '1:1' : '4:3')}
              style={{
                background: 'none',
                border: 'none',
                color: '#ffffff',
                fontSize: '13px',
                fontWeight: 'bold',
                cursor: 'pointer',
              }}
            >
              {aspectRatio}
            </button>

            {/* Grid */}
            <button
              onClick={() => setIsGrid(!isGrid)}
              style={{ background: 'none', border: 'none', color: isGrid ? '#ffd60a' : '#8e9096', cursor: 'pointer', padding: '8px' }}
            >
              <Grid size={20} />
            </button>

            {/* Settings */}
            <button
              onClick={() => setActiveView('settings')}
              style={{ background: 'none', border: 'none', color: '#ffffff', cursor: 'pointer', padding: '8px' }}
            >
              <Settings size={22} />
            </button>
          </div>

          {/* Unified Bottom Control Panel */}
          <div
            style={{
              position: 'absolute',
              bottom: 0,
              left: 0,
              right: 0,
              background: 'linear-gradient(to top, rgba(0,0,0,0.96) 60%, rgba(0,0,0,0.7) 85%, transparent)',
              padding: '24px 16px max(16px, env(safe-area-inset-bottom, 16px)) 16px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              zIndex: 20,
            }}
          >
            {/* Zoom Pills */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                backgroundColor: 'rgba(0,0,0,0.65)',
                border: '1px solid #2c2d32',
                borderRadius: '20px',
                padding: '4px 6px',
                gap: '4px',
                marginBottom: '16px',
              }}
            >
              {[1, 2, 5].map((z) => (
                <button
                  key={z}
                  onClick={() => setZoom(z)}
                  style={{
                    backgroundColor: zoom === z ? '#ffd60a' : 'transparent',
                    color: zoom === z ? '#080808' : '#ffffff',
                    border: 'none',
                    borderRadius: '50px',
                    padding: '4px 12px',
                    fontSize: '12px',
                    fontWeight: 'bold',
                    cursor: 'pointer',
                  }}
                >
                  {z}x
                </button>
              ))}
            </div>

            {/* Camera Modes Carousel */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '20px',
                overflowX: 'auto',
                width: '100%',
                justifyContent: 'center',
                marginBottom: '20px',
              }}
            >
              {(['PRO', 'NIGHT', 'PHOTO', 'VIDEO', 'PORTRAIT', 'HDR'] as CameraMode[]).map((m) => (
                <button
                  key={m}
                  onClick={() => setMode(m)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: mode === m ? '#ffd60a' : '#8e9096',
                    fontSize: '13px',
                    fontWeight: mode === m ? 'bold' : '500',
                    letterSpacing: '1px',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: '4px',
                  }}
                >
                  {m}
                  {mode === m && <div style={{ width: '4px', height: '4px', borderRadius: '50%', backgroundColor: '#ffd60a' }} />}
                </button>
              ))}
            </div>

            {/* Bottom Actions & Shutter Row */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-around',
                width: '100%',
                maxWidth: '420px',
              }}
            >
              {/* Gallery Thumbnail */}
              <button
                onClick={() => setActiveView('gallery')}
                style={{
                  width: '50px',
                  height: '50px',
                  borderRadius: '50%',
                  border: '1.5px solid #2c2d32',
                  backgroundColor: '#141416',
                  overflow: 'hidden',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: 0,
                }}
              >
                {galleryItems.length > 0 ? (
                  <img src={galleryItems[0].url} alt="Gallery" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                ) : (
                  <Camera size={20} color="#ffffff" />
                )}
              </button>

              {/* Master Shutter Button */}
              <button
                onClick={triggerShutter}
                style={{
                  width: '78px',
                  height: '78px',
                  borderRadius: '50%',
                  border: '4px solid #ffffff',
                  backgroundColor: 'transparent',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                  padding: 0,
                  transition: 'transform 0.1s ease',
                }}
                onMouseDown={(e) => (e.currentTarget.style.transform = 'scale(0.92)')}
                onMouseUp={(e) => (e.currentTarget.style.transform = 'scale(1)')}
              >
                {isRecording ? (
                  <div style={{ width: '28px', height: '28px', borderRadius: '6px', backgroundColor: '#ff3b30' }} />
                ) : (
                  <div
                    style={{
                      width: '62px',
                      height: '62px',
                      borderRadius: '50%',
                      backgroundColor: mode === 'VIDEO' ? '#ff3b30' : '#ffffff',
                    }}
                  />
                )}
              </button>

              {/* Flip Camera Button */}
              <button
                onClick={() => setFacingMode(facingMode === 'user' ? 'environment' : 'user')}
                style={{
                  width: '50px',
                  height: '50px',
                  borderRadius: '50%',
                  border: '1.5px solid #2c2d32',
                  backgroundColor: '#141416',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                }}
              >
                <RotateCcw size={22} />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SCREEN 2: AI PHOTO LAB (ENHANCEMENT & BEFORE/AFTER) */}
      {activeView === 'enhance' && (
        <div style={{ display: 'flex', flexDirection: 'column', width: '100%', height: '100%', backgroundColor: '#080808' }}>
          {/* Top Bar */}
          <div
            style={{
              padding: 'env(safe-area-inset-top, 16px) 16px 12px 16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              borderBottom: '1px solid #1a1a1e',
            }}
          >
            <button
              onClick={() => setActiveView('camera')}
              style={{ background: 'none', border: 'none', color: '#ffffff', cursor: 'pointer' }}
            >
              <X size={24} />
            </button>
            <span style={{ fontSize: '14px', fontWeight: 'bold', letterSpacing: '2px' }}>AI COMPUTATIONAL LAB</span>
            <button
              onClick={saveEnhancedPhoto}
              style={{ background: 'none', border: 'none', color: '#ffd60a', cursor: 'pointer', fontWeight: 'bold' }}
            >
              <Check size={24} />
            </button>
          </div>

          {/* Before / After Slider Canvas */}
          <div style={{ flex: 1, position: 'relative', width: '100%', backgroundColor: '#000000', overflow: 'hidden' }}>
            {currentRawUrl && (
              <BeforeAfterSlider
                originalUrl={currentRawUrl}
                enhancedUrl={currentEnhancedUrl || currentRawUrl}
              />
            )}

            {/* AI Processing Progress Overlay */}
            {isProcessingAi && (
              <div
                style={{
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%)',
                  backgroundColor: 'rgba(0,0,0,0.88)',
                  border: '1px solid #2c2d32',
                  borderRadius: '16px',
                  padding: '24px',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  width: '85%',
                  maxWidth: '340px',
                  zIndex: 40,
                }}
              >
                <Sparkles size={32} color="#ffd60a" />
                <span style={{ fontSize: '24px', fontWeight: '900', marginTop: '12px' }}>{aiProgress}%</span>
                <span style={{ fontSize: '12px', color: '#8e9096', marginTop: '6px', textAlign: 'center' }}>{aiStage}</span>
                <div style={{ width: '100%', height: '6px', backgroundColor: '#1a1a1e', borderRadius: '3px', marginTop: '16px', overflow: 'hidden' }}>
                  <div
                    style={{
                      width: `${aiProgress}%`,
                      height: '100%',
                      backgroundColor: '#ffd60a',
                      transition: 'width 0.15s ease',
                    }}
                  />
                </div>
              </div>
            )}
          </div>

          {/* Bottom Enhancement Controls Card */}
          <div style={{ backgroundColor: '#141416', padding: '16px 20px max(16px, env(safe-area-inset-bottom, 16px)) 20px', borderTop: '1px solid #2c2d32' }}>
            {/* Metrics */}
            {currentMetrics && (
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  backgroundColor: '#080808',
                  padding: '8px 14px',
                  borderRadius: '10px',
                  border: '1px solid #2c2d32',
                  marginBottom: '14px',
                }}
              >
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '9px', color: '#8e9096', fontWeight: 'bold' }}>DETAIL</div>
                  <div style={{ fontSize: '12px', fontWeight: 'bold' }}>+{currentMetrics.detailBoost}%</div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '9px', color: '#8e9096', fontWeight: 'bold' }}>NOISE</div>
                  <div style={{ fontSize: '12px', fontWeight: 'bold' }}>-{currentMetrics.noiseReduction}%</div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '9px', color: '#8e9096', fontWeight: 'bold' }}>DYNAMIC</div>
                  <div style={{ fontSize: '12px', fontWeight: 'bold' }}>+{currentMetrics.dynamicRangeEv} EV</div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '9px', color: '#8e9096', fontWeight: 'bold' }}>TONE</div>
                  <div style={{ fontSize: '12px', fontWeight: 'bold', color: '#ffd60a' }}>{currentMetrics.skinTonePurity}</div>
                </div>
              </div>
            )}

            {/* Intensity Slider */}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '11px', fontWeight: 'bold' }}>
              <span style={{ color: '#8e9096' }}>ENHANCEMENT INTENSITY</span>
              <span style={{ color: '#ffd60a' }}>{Math.round(aiIntensity * 100)}%</span>
            </div>
            <input
              type="range"
              min="0.2"
              max="1.0"
              step="0.05"
              value={aiIntensity}
              onChange={(e) => setAiIntensity(parseFloat(e.target.value))}
              style={{ width: '100%', accentColor: '#ffffff', marginBottom: '14px' }}
            />

            {/* Buttons */}
            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                onClick={() => {
                  if (captureCanvasRef.current) runAiEnhancement(captureCanvasRef.current, aiIntensity);
                }}
                disabled={isProcessingAi}
                style={{
                  flex: 1,
                  padding: '12px',
                  backgroundColor: 'transparent',
                  border: '1px solid #2c2d32',
                  borderRadius: '12px',
                  color: '#ffffff',
                  fontSize: '12px',
                  fontWeight: 'bold',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  cursor: 'pointer',
                }}
              >
                <RefreshCw size={16} /> RE-TUNE
              </button>

              <button
                onClick={saveEnhancedPhoto}
                disabled={isProcessingAi}
                style={{
                  flex: 1.5,
                  padding: '12px',
                  backgroundColor: '#ffffff',
                  border: 'none',
                  borderRadius: '12px',
                  color: '#080808',
                  fontSize: '12px',
                  fontWeight: 'bold',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '6px',
                  cursor: 'pointer',
                }}
              >
                <Check size={16} /> SAVE 4K MASTER
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SCREEN 3: GALLERY */}
      {activeView === 'gallery' && (
        <div style={{ display: 'flex', flexDirection: 'column', width: '100%', height: '100%', backgroundColor: '#080808' }}>
          {/* Top Bar */}
          <div
            style={{
              padding: 'env(safe-area-inset-top, 16px) 16px 12px 16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              borderBottom: '1px solid #1a1a1e',
            }}
          >
            <button
              onClick={() => setActiveView('camera')}
              style={{ background: 'none', border: 'none', color: '#ffffff', cursor: 'pointer' }}
            >
              <X size={24} />
            </button>
            <span style={{ fontSize: '14px', fontWeight: 'bold', letterSpacing: '2px' }}>DARKSOULSED GALLERY</span>
            <div style={{ width: '24px' }} />
          </div>

          {/* Filter Tabs */}
          <div style={{ display: 'flex', gap: '8px', padding: '12px 16px', overflowX: 'auto', borderBottom: '1px solid #141416' }}>
            {(['all', 'ai', 'photo', 'video'] as const).map((f) => (
              <button
                key={f}
                onClick={() => setGalleryFilter(f)}
                style={{
                  backgroundColor: galleryFilter === f ? '#ffd60a' : '#141416',
                  color: galleryFilter === f ? '#080808' : '#8e9096',
                  border: 'none',
                  borderRadius: '20px',
                  padding: '6px 14px',
                  fontSize: '11px',
                  fontWeight: 'bold',
                  cursor: 'pointer',
                }}
              >
                {f === 'all' ? 'All Media' : f === 'ai' ? 'AI Enhanced' : f === 'photo' ? 'Photos' : '4K Videos'}
              </button>
            ))}
          </div>

          {/* Gallery Grid */}
          <div style={{ flex: 1, padding: '4px', overflowY: 'auto', display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '4px' }}>
            {galleryItems
              .filter((item) => {
                if (galleryFilter === 'ai') return item.isEnhanced;
                if (galleryFilter === 'photo') return item.type === 'photo';
                if (galleryFilter === 'video') return item.type === 'video';
                return true;
              })
              .map((item) => (
                <div
                  key={item.id}
                  onClick={() => setActiveMediaItem(item)}
                  style={{
                    position: 'relative',
                    aspectRatio: '1',
                    backgroundColor: '#141416',
                    cursor: 'pointer',
                    overflow: 'hidden',
                    borderRadius: '4px',
                  }}
                >
                  {item.type === 'video' ? (
                    <video src={item.url} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  ) : (
                    <img src={item.url} alt={item.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  )}

                  {/* Badge */}
                  {item.isEnhanced && (
                    <div
                      style={{
                        position: 'absolute',
                        top: '4px',
                        left: '4px',
                        backgroundColor: 'rgba(0,0,0,0.8)',
                        padding: '2px 5px',
                        borderRadius: '4px',
                        fontSize: '9px',
                        fontWeight: 'bold',
                        color: '#ffd60a',
                      }}
                    >
                      AI PRO
                    </div>
                  )}

                  {item.type === 'video' && (
                    <div
                      style={{
                        position: 'absolute',
                        bottom: '4px',
                        right: '4px',
                        backgroundColor: 'rgba(0,0,0,0.8)',
                        padding: '2px 5px',
                        borderRadius: '4px',
                        fontSize: '9px',
                        fontWeight: 'bold',
                        color: '#ffffff',
                      }}
                    >
                      4K
                    </div>
                  )}
                </div>
              ))}
          </div>

          {galleryItems.length === 0 && (
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: '#8e9096' }}>
              <Camera size={48} style={{ opacity: 0.3, marginBottom: '12px' }} />
              <span style={{ fontSize: '14px' }}>No media captured yet</span>
              <span style={{ fontSize: '11px', marginTop: '4px' }}>Take a photo or record 4K video to start</span>
            </div>
          )}

          {/* Fullscreen Media Viewer Modal */}
          {activeMediaItem && (
            <div
              style={{
                position: 'fixed',
                inset: 0,
                backgroundColor: '#000000',
                display: 'flex',
                flexDirection: 'column',
                zIndex: 60,
              }}
            >
              <div
                style={{
                  padding: 'env(safe-area-inset-top, 16px) 16px 12px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  backgroundColor: 'rgba(0,0,0,0.7)',
                }}
              >
                <button
                  onClick={() => setActiveMediaItem(null)}
                  style={{ background: 'none', border: 'none', color: '#ffffff', cursor: 'pointer' }}
                >
                  <X size={24} />
                </button>
                <span style={{ fontSize: '12px', fontWeight: 'bold' }}>{activeMediaItem.name}</span>
                <button
                  onClick={() => deleteMedia(activeMediaItem.id)}
                  style={{ background: 'none', border: 'none', color: '#ff3b30', cursor: 'pointer' }}
                >
                  <Trash2 size={20} />
                </button>
              </div>

              <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '8px' }}>
                {activeMediaItem.type === 'video' ? (
                  <video src={activeMediaItem.url} controls autoPlay style={{ maxWidth: '100%', maxHeight: '100%', borderRadius: '8px' }} />
                ) : (
                  <img src={activeMediaItem.url} alt={activeMediaItem.name} style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }} />
                )}
              </div>

              <div
                style={{
                  padding: '16px max(16px, env(safe-area-inset-bottom, 16px))',
                  display: 'flex',
                  justifyContent: 'center',
                  gap: '24px',
                  backgroundColor: 'rgba(0,0,0,0.8)',
                }}
              >
                <button
                  onClick={() => downloadMedia(activeMediaItem)}
                  style={{
                    backgroundColor: '#ffffff',
                    color: '#080808',
                    border: 'none',
                    borderRadius: '10px',
                    padding: '10px 20px',
                    fontWeight: 'bold',
                    fontSize: '12px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    cursor: 'pointer',
                  }}
                >
                  <Download size={16} /> DOWNLOAD
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* SCREEN 4: SETTINGS MODAL */}
      {activeView === 'settings' && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: '#080808',
            display: 'flex',
            flexDirection: 'column',
            zIndex: 50,
            padding: 'env(safe-area-inset-top, 16px) 20px max(16px, env(safe-area-inset-bottom, 16px)) 20px',
            overflowY: 'auto',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
            <span style={{ fontSize: '16px', fontWeight: 'bold', letterSpacing: '2px' }}>SETTINGS</span>
            <button onClick={() => setActiveView('camera')} style={{ background: 'none', border: 'none', color: '#ffffff', cursor: 'pointer' }}>
              <X size={24} />
            </button>
          </div>

          <div style={{ fontSize: '11px', fontWeight: 'bold', color: '#8e9096', letterSpacing: '1px', marginBottom: '10px' }}>
            CAMERA & VIDEO
          </div>
          <div style={{ backgroundColor: '#141416', borderRadius: '14px', border: '1px solid #2c2d32', padding: '16px', marginBottom: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div>
                <div style={{ fontSize: '13px', fontWeight: 'bold' }}>4K UHD Capture Mode</div>
                <div style={{ fontSize: '11px', color: '#8e9096' }}>Enables 3840x2160 stream where hardware supports it</div>
              </div>
              <input type="checkbox" defaultChecked style={{ accentColor: '#ffd60a' }} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontSize: '13px', fontWeight: 'bold' }}>Rule of Thirds Grid</div>
                <div style={{ fontSize: '11px', color: '#8e9096' }}>Overlay composition lines</div>
              </div>
              <input
                type="checkbox"
                checked={isGrid}
                onChange={(e) => setIsGrid(e.target.checked)}
                style={{ accentColor: '#ffd60a' }}
              />
            </div>
          </div>

          <div style={{ fontSize: '11px', fontWeight: 'bold', color: '#8e9096', letterSpacing: '1px', marginBottom: '10px' }}>
            AI NEURAL LAB
          </div>
          <div style={{ backgroundColor: '#141416', borderRadius: '14px', border: '1px solid #2c2d32', padding: '16px', marginBottom: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div>
                <div style={{ fontSize: '13px', fontWeight: 'bold' }}>Melanin & Skin Science</div>
                <div style={{ fontSize: '11px', color: '#8e9096' }}>Preserves natural skin texture without artificial smoothing</div>
              </div>
              <input type="checkbox" defaultChecked style={{ accentColor: '#ffd60a' }} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontSize: '13px', fontWeight: 'bold' }}>Optical Micro-Contrast</div>
                <div style={{ fontSize: '11px', color: '#8e9096' }}>Laplacian 2nd-derivative edge reconstruction</div>
              </div>
              <input type="checkbox" defaultChecked style={{ accentColor: '#ffd60a' }} />
            </div>
          </div>

          <button
            onClick={() => setActiveView('about')}
            style={{
              backgroundColor: '#141416',
              border: '1px solid #2c2d32',
              borderRadius: '14px',
              padding: '16px',
              color: '#ffffff',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              cursor: 'pointer',
              marginBottom: '20px',
            }}
          >
            <div>
              <div style={{ fontSize: '13px', fontWeight: 'bold' }}>DARKSOULSED® AI CAMERA</div>
              <div style={{ fontSize: '11px', color: '#ffd60a' }}>Version 1.0.0 PRO • Tap for manifesto</div>
            </div>
            <Info size={20} color="#ffd60a" />
          </button>
        </div>
      )}

      {/* SCREEN 5: ABOUT MODAL */}
      {activeView === 'about' && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: '#080808',
            display: 'flex',
            flexDirection: 'column',
            zIndex: 60,
            padding: 'env(safe-area-inset-top, 16px) 24px max(16px, env(safe-area-inset-bottom, 16px)) 24px',
            overflowY: 'auto',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '16px' }}>
            <button onClick={() => setActiveView('settings')} style={{ background: 'none', border: 'none', color: '#ffffff', cursor: 'pointer' }}>
              <X size={24} />
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', marginBottom: '32px' }}>
            <div
              style={{
                width: '84px',
                height: '84px',
                borderRadius: '50%',
                backgroundColor: '#141416',
                border: '2px solid #ffd60a',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '16px',
              }}
            >
              <Sparkles size={40} color="#ffd60a" />
            </div>
            <h1 style={{ fontSize: '22px', fontWeight: '900', letterSpacing: '3px' }}>DARKSOULSED®</h1>
            <p style={{ fontSize: '12px', color: '#ffd60a', marginTop: '4px', letterSpacing: '1px' }}>
              PROFESSIONAL AI PHOTOGRAPHY & 4K VIDEO
            </p>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '12px', fontSize: '13px', fontWeight: '600' }}>
              <Heart size={14} color="#ffd60a" fill="#ffd60a" />
              <span>Made with love by Darksouled</span>
            </div>
          </div>

          <div style={{ backgroundColor: '#141416', borderRadius: '16px', border: '1px solid #2c2d32', padding: '18px', marginBottom: '16px' }}>
            <div style={{ fontSize: '13px', fontWeight: 'bold', marginBottom: '6px' }}>Client-Side Zero-Latency Neural Engine</div>
            <p style={{ fontSize: '12px', color: '#8e9096', lineHeight: '18px' }}>
              All computational photography, 4K video recording, and tone curve processing run 100% on-device in your browser. No photos or videos ever leave your device.
            </p>
          </div>

          <div style={{ backgroundColor: '#141416', borderRadius: '16px', border: '1px solid #2c2d32', padding: '18px', textAlign: 'center' }}>
            <span style={{ fontSize: '11px', color: '#8e9096' }}>Deployable anywhere • Vercel • PWA • GitHub</span>
          </div>
        </div>
      )}
    </div>
  );
};
