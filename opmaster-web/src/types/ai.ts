// AI场景类型 - 与Android端保持同步
export enum SceneType {
  LANDSCAPE = 'LANDSCAPE',
  PORTRAIT = 'PORTRAIT',
  NIGHT = 'NIGHT',
  SUNSET = 'SUNSET',
  FOOD = 'FOOD',
  STREET = 'STREET',
  NATURE = 'NATURE',
  ARCHITECTURE = 'ARCHITECTURE',
  MACRO = 'MACRO',
  UNKNOWN = 'UNKNOWN'
}

// 场景显示名称 - 与Android端保持同步
export const SceneDisplayNames: Record<SceneType, string> = {
  [SceneType.LANDSCAPE]: '风景',
  [SceneType.PORTRAIT]: '人像',
  [SceneType.NIGHT]: '夜景',
  [SceneType.SUNSET]: '日落',
  [SceneType.FOOD]: '美食',
  [SceneType.STREET]: '街头',
  [SceneType.NATURE]: '自然',
  [SceneType.ARCHITECTURE]: '建筑',
  [SceneType.MACRO]: '微距',
  [SceneType.UNKNOWN]: '未知'
};

// 场景描述 - 与Android端保持同步
export const SceneDescriptions: Record<SceneType, string> = {
  [SceneType.LANDSCAPE]: '适合户外风景、山川湖海',
  [SceneType.PORTRAIT]: '适合人物摄影',
  [SceneType.NIGHT]: '适合夜间城市、星空',
  [SceneType.SUNSET]: '适合日落、黄金时刻',
  [SceneType.FOOD]: '适合美食拍摄',
  [SceneType.STREET]: '适合街头纪实',
  [SceneType.NATURE]: '适合自然生态、植物',
  [SceneType.ARCHITECTURE]: '适合城市建筑、室内空间',
  [SceneType.MACRO]: '适合特写、微距摄影',
  [SceneType.UNKNOWN]: '自动识别场景'
};

// 场景关键词 - 与Android端保持同步
export const SceneKeywords: Record<SceneType, string[]> = {
  [SceneType.LANDSCAPE]: ['风景', '自然', '森林', '海边'],
  [SceneType.PORTRAIT]: ['人像', '樱花', '柔焦'],
  [SceneType.NIGHT]: ['夜景', '夜色', '霓虹'],
  [SceneType.SUNSET]: ['日落', '橙调', '佛罗伦萨'],
  [SceneType.FOOD]: ['美食', '自然', '清新'],
  [SceneType.STREET]: ['街头', '纪实', '黑白'],
  [SceneType.NATURE]: ['自然', '森林', '清新'],
  [SceneType.ARCHITECTURE]: ['建筑', '城市', '纪实'],
  [SceneType.MACRO]: ['自然', '清新', '特写'],
  [SceneType.UNKNOWN]: []
};

// AI调整参数 - 与Android端保持同步
export interface AiAdjustmentParams {
  brightness: number;
  contrast: number;
  saturation: number;
  warmth: number;
  tint: number;
  highlights: number;
  shadows: number;
  clarity: number;
  vignette: number;
  toDisplayMap?: () => Record<string, number>;
}

// 扩展默认AI参数
export const DEFAULT_AI_PARAMS: AiAdjustmentParams = {
  brightness: 0,
  contrast: 0,
  saturation: 0,
  warmth: 0,
  tint: 0,
  highlights: 0,
  shadows: 0,
  clarity: 0,
  vignette: 0,
  toDisplayMap() {
    return {
      '亮度': this.brightness,
      '对比度': this.contrast,
      '饱和度': this.saturation,
      '色温': this.warmth,
      '色调': this.tint,
      '高光': this.highlights,
      '阴影': this.shadows,
      '清晰度': this.clarity,
      '暗角': this.vignette
    };
  }
};

// 预设数据结构 - 与Android端保持同步
export interface Section {
  title: string;
  content?: string;
  items?: Array<{
    label: string;
    value: string | number;
    span?: number;
  }>;
}

export interface CameraParams {
  hasselblad_hncs?: boolean;
  [key: string]: any;
}

export interface Preset {
  id: string;
  name: string;
  coverPath: string;
  sections: Section[];
  cameraParams?: CameraParams;
  deviceModel: string;
  source: string;
  isFavorite: boolean;
  isNew?: boolean;
  [key: string]: any;
}

// 创建AI参数工厂函数
export function createAiParams(
  brightness: number,
  contrast: number,
  saturation: number,
  warmth: number,
  tint: number,
  highlights: number,
  shadows: number,
  clarity: number,
  vignette: number
): AiAdjustmentParams {
  return {
    brightness,
    contrast,
    saturation,
    warmth,
    tint,
    highlights,
    shadows,
    clarity,
    vignette,
    toDisplayMap() {
      return {
        '亮度': this.brightness,
        '对比度': this.contrast,
        '饱和度': this.saturation,
        '色温': this.warmth,
        '色调': this.tint,
        '高光': this.highlights,
        '阴影': this.shadows,
        '清晰度': this.clarity,
        '暗角': this.vignette
      };
    }
  };
}

// AI服务 - 模拟Android端的AiService
export class AiService {
  // 模拟场景识别
  static async detectScene(imageUrl: string | null = null): Promise<SceneType> {
    // 模拟延迟 - 实际应该用真实模型
    await new Promise(resolve => setTimeout(resolve, 100));
    
    const scenes = Object.values(SceneType).filter(s => s !== SceneType.UNKNOWN);
    return scenes[Math.floor(Math.random() * scenes.length)];
  }

  // 获取推荐预设
  static async getRecommendedPresets(scene: SceneType, allPresets: Preset[]): Promise<Preset[]> {
    // 模拟延迟
    await new Promise(resolve => setTimeout(resolve, 50));
    
    const keywords = SceneKeywords[scene] || [];
    
    const matched = allPresets.filter(preset => {
      return keywords.some(keyword => 
        preset.name.includes(keyword) || 
        preset.sections.some(section => 
          (section.content && section.content.includes(keyword)) ||
          (section.title && section.title.includes(keyword))
        )
      );
    });
    
    return matched.length > 0 ? matched : allPresets.slice(0, 3);
  }

  // 精细调整图像
  static async fineTuneImage(imageUrl: string, preset: Preset | null = null): Promise<AiAdjustmentParams> {
    // 模拟延迟
    await new Promise(resolve => setTimeout(resolve, 100));
    
    if (preset?.cameraParams?.hasselblad_hncs) {
      return createAiParams(8, 5, 12, 5, 0, 0, 0, 8, 0);
    }
    
    return createAiParams(5, 8, 10, 0, 0, 0, 0, 5, 0);
  }
}
