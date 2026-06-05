export interface Preset {
  id: string;
  name: string;
  coverUrl: string;
  author: string;
  deviceModel: string;
  sceneType: string;
  tags: string[];
  rating: number;
  downloadCount: number;
  isHncsCertified: boolean;
  isFavorite: boolean;
  version: string;
  description: string;
  cameraParams: {
    iso: number;
    shutter: string;
    aperture: string;
    ev: string;
    wb: string;
    mode: string;
    focalLength?: string;
  };
  sections: { title: string; content: string }[];
}

export type SceneType =
  | "portrait"
  | "landscape"
  | "night"
  | "food"
  | "street"
  | "macro"
  | "sunset"
  | "cityscape";

export interface SceneDetectionResult {
  scene: SceneType;
  sceneName: string;
  confidence: number;
  detectionTime: number;
  isOffline: boolean;
  recommendedPresetIds: string[];
}

export type WatermarkTemplate =
  | "HASSELBLAD"
  | "OPPO"
  | "ONEPLUS"
  | "REALME"
  | "CUSTOM";

export type WatermarkPosition =
  | "TOP_LEFT"
  | "TOP_CENTER"
  | "TOP_RIGHT"
  | "CENTER"
  | "BOTTOM_LEFT"
  | "BOTTOM_CENTER"
  | "BOTTOM_RIGHT";

export interface WatermarkConfig {
  template: WatermarkTemplate;
  position: WatermarkPosition;
  opacity: number;
  scale: number;
  customText: string;
  showTimestamp: boolean;
  showDevice: boolean;
}

export interface CameraConfig {
  id: string;
  name: string;
  description: string;
  iso: number;
  shutter: string;
  aperture: string;
  ev: string;
  wb: string;
  isFavorite: boolean;
  createdAt: number;
  tags: string[];
}

export type FilterType =
  | "ALL"
  | "FAVORITES"
  | "HNCS"
  | "FIND_X"
  | "RENO"
  | "NEW"
  | "TRENDING";
