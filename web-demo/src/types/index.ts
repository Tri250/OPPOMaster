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
