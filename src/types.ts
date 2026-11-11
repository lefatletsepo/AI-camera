export type CameraMode = 'PRO' | 'NIGHT' | 'PHOTO' | 'VIDEO' | 'PORTRAIT' | 'HDR';

export type FlashMode = 'off' | 'on' | 'auto';

export type TimerSetting = 0 | 3 | 10;

export interface EnhancementMetrics {
  detailBoost: number;
  noiseReduction: number;
  dynamicRangeEv: number;
  skinTonePurity: string;
}

export interface MediaItem {
  id: string;
  url: string;
  type: 'photo' | 'video';
  isEnhanced: boolean;
  timestamp: number;
  name: string;
  metrics?: EnhancementMetrics;
}
