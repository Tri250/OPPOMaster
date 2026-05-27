// 官方预设数据类型
export interface CloudPresetResponse {
  version: number;
  name: string;
  author: string;
  build: number;
  presets: CloudPreset[];
}

export interface CloudPreset {
  name: string;
  coverPath: string;
  galleryImages: string[];
  author: string;
  isNew?: boolean;
  sections: CloudSection[];
  tags: string[];
  description?: CloudDescription;
}

export interface CloudSection {
  title: string;
  items: CloudParamItem[];
}

export interface CloudParamItem {
  label: string;
  value: string;
  span?: number;
}

export interface CloudDescription {
  title: string;
  content: string;
}

// 现有类型定义
export interface CameraParams {
  mode: string;
  filter: string;
  iso: number;
  shutter: string;
  ev: string;
  wb: string;
  hasselblad_hncs: boolean;
  contrast: number;
  saturation: number;
  sharpness: number;
  vignette: number;
  videoLut: string;
  sceneTags: string[];
  colorProfile?: ColorProfile;
}

export interface ColorProfile {
  dominantColors: number[];
  toneCurve: number[];
}

export interface Section {
  title: string;
  content: string;
}

export interface Preset {
  id: string;
  name: string;
  coverPath: string;
  sections: Section[];
  cameraParams: CameraParams;
  deviceModel: string;
  source: string;
  isFavorite: boolean;
  createdAt: number;
  updatedAt: number;
  usageCount: number;
  rating: number;
  author: string;
  description?: string;
}

export interface SceneType {
  id: string;
  name: string;
  icon: string;
  description: string;
  color: string;
}

export interface ColorExtractionResult {
  dominantColors: string[];
  toneCurve: number[];
  matchedPresets: { preset: Preset; similarity: number }[];
  customPreset?: Preset;
}
