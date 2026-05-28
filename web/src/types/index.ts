
export interface CameraParams {
  mode: string;
  filter: string;
  iso: number;
  shutter: string;
  ev: string;
  wb: string;
  hasselblad_hncs: boolean;
}

export interface Preset {
  id: string;
  name: string;
  coverPath: string;
  deviceModel: string;
  source: string;
  isFavorite: boolean;
  cameraParams?: CameraParams;
  description?: string;
}

export type FilterType = 'ALL' | 'FAVORITES' | 'HNCS' | 'FIND_X' | 'RENO';

export type ThemeMode = 'light' | 'dark' | 'system';
