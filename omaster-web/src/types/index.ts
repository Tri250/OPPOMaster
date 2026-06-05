// 品牌类型
export type BrandType = "OPPO" | "REALME" | "VIVO" | "HONOR";

// 远程预设数据结构（匹配JSON格式）
export interface RemotePresetSection {
  title: string;
  items: {
    label: string;
    value: string;
    span: number;
  }[];
}

export interface RemotePreset {
  name: string;
  coverPath: string;
  galleryImages: string[];
  author: string;
  isNew?: boolean;
  sections: RemotePresetSection[];
  tags: string[];
  description: {
    title: string;
    content: string;
  };
}

export interface RemotePresetData {
  version: number;
  name: string;
  author: string;
  build: number;
  presets: RemotePreset[];
}

// 本地展示用的预设类型（带品牌和ID）
export interface Preset {
  id: string;
  brand: BrandType;
  name: string;
  coverUrl: string;
  galleryImages: string[];
  author: string;
  isNew?: boolean;
  sections: RemotePresetSection[];
  tags: string[];
  description: {
    title: string;
    content: string;
  };
  // 以下为展示用的扩展字段
  rating?: number;
  downloadCount?: number;
  isHncsCertified?: boolean;
  isFavorite: boolean;
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
  | "OPPO"
  | "REALME"
  | "VIVO"
  | "HONOR"
  | "FAVORITES"
  | "NEW";

// 品牌配置
export const BRAND_CONFIG: Record<BrandType, { label: string; color: string; icon: string }> = {
  OPPO: {
    label: "OPPO / 一加",
    color: "from-green-500/20 to-green-500/0",
    icon: "OPPO",
  },
  REALME: {
    label: "Realme",
    color: "from-yellow-500/20 to-yellow-500/0",
    icon: "Realme",
  },
  VIVO: {
    label: "vivo / 蔡司",
    color: "from-blue-500/20 to-blue-500/0",
    icon: "vivo",
  },
  HONOR: {
    label: "荣耀",
    color: "from-purple-500/20 to-purple-500/0",
    icon: "荣耀",
  },
};