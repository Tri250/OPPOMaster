export type Section = {
  title: string;
  items: SectionItem[];
};

export type SectionItem = {
  label: string;
  value: string;
  span: number;
};

// OPPO FindX9spro哈苏大师模式相机参数格式
export type HasselbladMasterParams = {
  mode: 'master' | 'pro' | 'auto';
  filter: string;
  filter_intensity: number;
  soft_light: string;
  tone_curve: number;
  saturation: number;
  warm_cool: number;
  cyan_magenta: number;
  sharpness: number;
  vignette: boolean;
  custom_wb?: number;
  exposure_compensation?: string;
  iso?: number;
  shutter_speed?: string;
  hncs?: boolean;
};

export type Preset = {
  id: string;
  name: string;
  coverPath: string;
  galleryImages?: string[];
  sections: Section[];
  cameraParams: HasselbladMasterParams | null;
  deviceModel: string;
  author?: string;
  source: 'omaster_cloud' | 'community';
  isFavorite: boolean;
  isNew?: boolean;
  category?: string;
  difficulty?: string;
  tags?: string[];
  description?: {
    title: string;
    content: string;
  };
};

// 功能展示数据类型
export type AppFeature = {
  id: string;
  title: string;
  description: string;
  icon: string;
  category: 'core' | 'security' | 'ux' | 'build';
  demoAvailable?: boolean;
};

export const FilterType = {
  ALL: 'all',
  FAVORITES: 'favorites',
  HNCS: 'hncs',
  FIND_X: 'find_x',
  RENO: 'reno',
  NEW: 'new',
  TRENDING: 'trending'
} as const;

export type FilterType = typeof FilterType[keyof typeof FilterType];

// 完整的APP功能列表
export const appFeatures: AppFeature[] = [
  // 核心功能
  { id: 'preset-library', title: '预设库', description: '超过100个专业调色预设，涵盖人像、风景、夜景等场景', icon: '📷', category: 'core', demoAvailable: true },
  { id: 'floating-window', title: '悬浮窗', description: '系统级悬浮窗，实时显示相机参数，支持自由拖拽和透明度调节', icon: '🪟', category: 'core', demoAvailable: true },
  { id: 'camera-params', title: 'Camera2参数', description: '实时读取ISO、快门速度、EV、白平衡等相机参数', icon: '⚙️', category: 'core', demoAvailable: true },
  { id: 'screenshot-tool', title: '参数截图', description: '一键生成精美参数卡片，支持多种水印风格和尺寸', icon: '📸', category: 'core', demoAvailable: true },
  { id: 'watermark', title: '水印功能', description: '10种水印模板，支持批量处理，OPPO/OnePlus/realme品牌风格', icon: '🎨', category: 'core', demoAvailable: true },
  
  // 安全隐私
  { id: 'encrypted-storage', title: '加密存储', description: '使用AES-256-GCM加密存储用户数据，密钥存储在Android Keystore', icon: '🔐', category: 'security', demoAvailable: true },
  { id: 'https-only', title: 'HTTPS强制', description: '禁用明文流量，网络安全配置，证书钉扎支持', icon: '🔒', category: 'security', demoAvailable: true },
  { id: 'min-permissions', title: '最小权限', description: '遵循最小权限原则，无强制授权，隐私政策透明', icon: '🛡️', category: 'security', demoAvailable: true },
  { id: 'data-integrity', title: '数据完整性', description: 'SHA-256校验和，防止数据篡改，输入注入防护', icon: '✓', category: 'security', demoAvailable: true },
  
  // UX动画
  { id: 'animations', title: '精美动画', description: '遵循Material Design 3，页面转场、微交互、状态动画完美呈现', icon: '✨', category: 'ux', demoAvailable: true },
  { id: 'responsive', title: '响应式设计', description: '完美适配各种屏幕尺寸，从手机到平板都有最佳体验', icon: '📱', category: 'ux', demoAvailable: true },
  { id: 'skeleton', title: '骨架屏加载', description: '优雅的加载状态，用户体验流畅自然', icon: '⏳', category: 'ux', demoAvailable: true },
  { id: 'dark-mode', title: '深色主题', description: '完美的深色/浅色主题切换，护眼设计', icon: '🌙', category: 'ux', demoAvailable: true },
  
  // 构建生成
  { id: 'dependency-lock', title: '依赖锁定', description: '使用dependencyLocking确保依赖安全，防止投毒攻击', icon: '📦', category: 'build', demoAvailable: true },
  { id: 'code-obfuscation', title: '代码混淆', description: 'ProGuard/R8混淆，日志移除，签名保护', icon: '🔧', category: 'build', demoAvailable: true },
  { id: 'ci-cd', title: 'CI/CD流程', description: '自动化构建、测试、发布流程，GitHub Actions集成', icon: '🚀', category: 'build', demoAvailable: true },
];

// OPPO FindX9spro哈苏大师模式预设
export const mockPresets: Preset[] = [
  // OPPO官方预设
  {
    id: 'hasselblad_dewei',
    name: '德味预设',
    coverPath: 'https://cdn.fky.ltd/dw_01.webp',
    galleryImages: ['https://cdn.fky.ltd/dw_02.webp', 'https://cdn.fky.ltd/dw_03.webp'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '明艳 100%', span: 2 },
          { label: '柔光', value: '无', span: 1 },
          { label: '色调曲线', value: '-35', span: 1 },
          { label: '饱和度', value: '0', span: 1 },
          { label: '冷暖调', value: '-5', span: 1 },
          { label: '青红调', value: '4', span: 1 },
          { label: '锐度', value: '10', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '明艳',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: -35,
      saturation: 0,
      warm_cool: -5,
      cyan_magenta: 4,
      sharpness: 10,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@波子Booz',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '街拍',
    difficulty: '中等',
    tags: ['德味', '哈苏', '大师模式'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】日间户外或光线充足的室内\n【场景推荐】街拍、建筑、风景、人文\n【拍摄要点】德味风格，影调偏暗，色彩浓郁，适合追求经典德系胶片质感的摄影爱好者'
    }
  },
  {
    id: 'fujifilm_film',
    name: '富士胶片',
    coverPath: 'https://picsum.photos/400/600?random=1',
    galleryImages: [
      'https://picsum.photos/400/600?random=2',
      'https://picsum.photos/400/600?random=3'
    ],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '复古 100%', span: 2 },
          { label: '柔光', value: '无', span: 1 },
          { label: '色调曲线', value: '0', span: 1 },
          { label: '饱和度', value: '+19', span: 1 },
          { label: '冷暖调', value: '-5', span: 1 },
          { label: '青红调', value: '0', span: 1 },
          { label: '锐度', value: '15', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '复古',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: 0,
      saturation: 19,
      warm_cool: -5,
      cyan_magenta: 0,
      sharpness: 15,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@OPPO影像',
    source: 'omaster_cloud',
    isFavorite: true,
    isNew: false,
    category: '胶片',
    difficulty: '简单',
    tags: ['胶片', '富士', '经典'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】日间户外或光线充足的室内\n【场景推荐】街拍、人像、风景、建筑\n【拍摄要点】适合追求经典胶片质感的场景，色彩浓郁复古，建议寻找有光影对比的场景增强层次感'
    }
  },
  {
    id: 'film_sense',
    name: '胶片感',
    coverPath: 'https://picsum.photos/400/600?random=4',
    galleryImages: [
      'https://picsum.photos/400/600?random=5',
      'https://picsum.photos/400/600?random=6'
    ],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '复古 75%', span: 2 },
          { label: '柔光', value: '柔美', span: 1 },
          { label: '色调曲线', value: '-5', span: 1 },
          { label: '饱和度', value: '+20', span: 1 },
          { label: '冷暖调', value: '-3', span: 1 },
          { label: '青红调', value: '+4', span: 1 },
          { label: '锐度', value: '7', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '复古',
      filter_intensity: 75,
      soft_light: '柔美',
      tone_curve: -5,
      saturation: 20,
      warm_cool: -3,
      cyan_magenta: 4,
      sharpness: 7,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@OPPO影像',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '人文',
    difficulty: '简单',
    tags: ['胶片', '文艺', '人像'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】自然光或柔和人工光源\n【场景推荐】人像写真、静物、咖啡馆、文艺场景\n【拍摄要点】柔光效果营造梦幻氛围，适合拍摄情绪感照片，建议对焦主体保持清晰'
    }
  },
  {
    id: 'fairy_tale',
    name: '童话',
    coverPath: 'https://picsum.photos/400/600?random=7',
    galleryImages: [
      'https://picsum.photos/400/600?random=8',
      'https://picsum.photos/400/600?random=9'
    ],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '童话 73%', span: 2 },
          { label: '柔光', value: '梦幻', span: 1 },
          { label: '色调曲线', value: '-24', span: 1 },
          { label: '饱和度', value: '+12', span: 1 },
          { label: '冷暖调', value: '+3', span: 1 },
          { label: '青红调', value: '+7', span: 1 },
          { label: '锐度', value: '0', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '童话',
      filter_intensity: 73,
      soft_light: '梦幻',
      tone_curve: -24,
      saturation: 12,
      warm_cool: 3,
      cyan_magenta: 7,
      sharpness: 0,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@OPPO影像',
    source: 'omaster_cloud',
    isFavorite: true,
    isNew: false,
    category: '人像',
    difficulty: '中等',
    tags: ['童话', '梦幻', '儿童'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】清晨、黄昏或阴天散射光\n【场景推荐】儿童摄影、花园、公园、浪漫场景\n【拍摄要点】影调偏暗营造神秘感，梦幻柔光适合营造童话氛围，建议寻找色彩丰富的场景'
    }
  },
  {
    id: 'high_contrast_bw',
    name: '高对比黑白',
    coverPath: 'https://picsum.photos/400/600?random=10',
    galleryImages: [
      'https://picsum.photos/400/600?random=11',
      'https://picsum.photos/400/600?random=12'
    ],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '黑白 100%', span: 2 },
          { label: '柔光', value: '柔美', span: 1 },
          { label: '色调曲线', value: '-61', span: 1 },
          { label: '饱和度', value: '0', span: 1 },
          { label: '冷暖调', value: '+100', span: 1 },
          { label: '青红调', value: '-39', span: 1 },
          { label: '锐度', value: '0', span: 1 },
          { label: '暗角', value: '关', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '黑白',
      filter_intensity: 100,
      soft_light: '柔美',
      tone_curve: -61,
      saturation: 0,
      warm_cool: 100,
      cyan_magenta: -39,
      sharpness: 0,
      vignette: false,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@OPPO影像',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '纪实',
    difficulty: '进阶',
    tags: ['黑白', '纪实', '街拍'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】强烈光影对比场景，如阳光直射或聚光灯\n【场景推荐】建筑、纪实摄影、街头、艺术人像\n【拍摄要点】利用明暗对比突出主体轮廓，适合几何线条和纹理丰富的场景，注意构图简洁有力'
    }
  },
  {
    id: 'ricoh_green',
    name: '理光绿',
    coverPath: 'https://picsum.photos/400/600?random=13',
    galleryImages: [
      'https://picsum.photos/400/600?random=14',
      'https://picsum.photos/400/600?random=15'
    ],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '清新 100%', span: 2 },
          { label: '柔光', value: '梦幻', span: 1 },
          { label: '色调曲线', value: '+39', span: 1 },
          { label: '饱和度', value: '+12', span: 1 },
          { label: '冷暖调', value: '-2', span: 1 },
          { label: '青红调', value: '-9', span: 1 },
          { label: '锐度', value: '10', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '清新',
      filter_intensity: 100,
      soft_light: '梦幻',
      tone_curve: 39,
      saturation: 12,
      warm_cool: -2,
      cyan_magenta: -9,
      sharpness: 10,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@OPPO影像',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '风景',
    difficulty: '简单',
    tags: ['清新', '绿色', '自然'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】户外自然光，森林、草地、植物丰富的场景\n【场景推荐】植物摄影、森林漫步、春日户外、清新人像\n【拍摄要点】影调偏亮突出清新感，绿色表现自然通透，适合拍摄植物和户外自然场景'
    }
  },
  {
    id: 'ricoh_blue',
    name: '理光蓝',
    coverPath: 'https://picsum.photos/400/600?random=16',
    galleryImages: [
      'https://picsum.photos/400/600?random=17',
      'https://picsum.photos/400/600?random=18'
    ],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '通透 100%', span: 2 },
          { label: '柔光', value: '柔美', span: 1 },
          { label: '色调曲线', value: '+18', span: 1 },
          { label: '饱和度', value: '-2', span: 1 },
          { label: '冷暖调', value: '-8', span: 1 },
          { label: '青红调', value: '+19', span: 1 },
          { label: '锐度', value: '11', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '通透',
      filter_intensity: 100,
      soft_light: '柔美',
      tone_curve: 18,
      saturation: -2,
      warm_cool: -8,
      cyan_magenta: 19,
      sharpness: 11,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@OPPO影像',
    source: 'omaster_cloud',
    isFavorite: true,
    isNew: true,
    category: '建筑',
    difficulty: '中等',
    tags: ['蓝色', '通透', '建筑'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】晴朗天气或蓝天背景\n【场景推荐】海边、城市建筑、天空、冷色调场景\n【拍摄要点】偏冷色调增强蓝色表现力，适合拍摄天空、水面和城市建筑，营造通透冷静的氛围'
    }
  },
  {
    id: 'blue_hour',
    name: '蓝调时刻',
    coverPath: 'https://picsum.photos/400/600?random=19',
    galleryImages: ['https://picsum.photos/400/600?random=20'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '复古 100%', span: 2 },
          { label: '柔光', value: '梦幻', span: 1 },
          { label: '色调曲线', value: '-5', span: 1 },
          { label: '饱和度', value: '+15', span: 1 },
          { label: '冷暖调', value: '+47', span: 1 },
          { label: '青红调', value: '+28', span: 1 },
          { label: '锐度', value: '12', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '复古',
      filter_intensity: 100,
      soft_light: '梦幻',
      tone_curve: -5,
      saturation: 15,
      warm_cool: 47,
      cyan_magenta: 28,
      sharpness: 12,
      vignette: true,
      iso: 800,
      shutter_speed: '1/30',
      exposure_compensation: '-0.7',
      custom_wb: 4200,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@OPPO影像',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '夜景',
    difficulty: '进阶',
    tags: ['蓝调', '夜景', '城市'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】日出前或日落后20分钟的蓝调时刻\n【场景推荐】城市夜景、灯光璀璨的场景、水面倒影\n【拍摄要点】冷暖对比强烈，适合拍摄城市灯光和夜景，建议寻找有水面的场景增强倒影效果'
    }
  },
  {
    id: 'dream_soft',
    name: '梦幻黑柔',
    coverPath: 'https://picsum.photos/400/600?random=21',
    galleryImages: [
      'https://picsum.photos/400/600?random=22',
      'https://picsum.photos/400/600?random=23'
    ],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '标准 0%', span: 2 },
          { label: '柔光', value: '梦幻', span: 1 },
          { label: '色调曲线', value: '-25', span: 1 },
          { label: '饱和度', value: '+11', span: 1 },
          { label: '冷暖调', value: '+30', span: 1 },
          { label: '青红调', value: '-9', span: 1 },
          { label: '锐度', value: '0', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '标准',
      filter_intensity: 0,
      soft_light: '梦幻',
      tone_curve: -25,
      saturation: 11,
      warm_cool: 30,
      cyan_magenta: -9,
      sharpness: 0,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@OPPO影像',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '人像',
    difficulty: '专家',
    tags: ['黑柔', '梦幻', '唯美人像'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】逆光或侧逆光场景\n【场景推荐】人像写真、情绪摄影、艺术场景、柔美人像\n【拍摄要点】黑柔滤镜效果营造梦幻氛围，适合拍摄唯美人像，建议利用逆光创造光晕效果'
    }
  },
  // Realme预设
  {
    id: 'ricoh_positive',
    name: '理光正片',
    coverPath: 'https://cdn.fky.ltd/zwzp_01.webp',
    galleryImages: ['https://cdn.fky.ltd/zwzp_02.webp', 'https://cdn.fky.ltd/zwzp_03.webp'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '正片', span: 2 },
          { label: '饱和度', value: '+4', span: 1 },
          { label: '色相', value: '+1', span: 1 },
          { label: '色调曲线', value: '-1', span: 1 },
          { label: '对比度', value: '+3', span: 1 },
          { label: '高光对比度', value: '+2', span: 1 },
          { label: '阴影对比度', value: '-2', span: 1 },
          { label: '锐度', value: '-1', span: 1 },
          { label: '亮度', value: '-1', span: 1 },
          { label: '清晰度', value: '-1', span: 1 },
          { label: '颗粒感', value: '+3', span: 1 },
          { label: '颗粒大小', value: '+2', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '正片',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: -1,
      saturation: 4,
      warm_cool: 0,
      cyan_magenta: 0,
      sharpness: -1,
      vignette: false
    },
    deviceModel: 'GT 6',
    author: '@尼克lin',
    source: 'community',
    isFavorite: false,
    isNew: false,
    category: '街拍',
    difficulty: '中等',
    tags: ['理光', '正片', 'GR'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】日间户外，光线充足的场景\n【场景推荐】街拍、建筑、人文、日常记录\n【拍摄要点】模拟理光GR正片风格，色彩鲜艳对比度高，适合追求胶片质感的拍摄场景'
    }
  },
  {
    id: 'ricoh_negative',
    name: '理光负片',
    coverPath: 'https://cdn.fky.ltd/lgfp_01.webp',
    galleryImages: ['https://cdn.fky.ltd/lgfp_02.webp', 'https://cdn.fky.ltd/lgfp_03.webp'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '负片', span: 2 },
          { label: '饱和度', value: '+3', span: 1 },
          { label: '色相', value: '+3', span: 1 },
          { label: '色调曲线', value: '+1', span: 1 },
          { label: '对比度', value: '+4', span: 1 },
          { label: '高光对比度', value: '+1', span: 1 },
          { label: '阴影对比度', value: '-2', span: 1 },
          { label: '锐度', value: '+1', span: 1 },
          { label: '亮度', value: '+1', span: 1 },
          { label: '清晰度', value: '0', span: 1 },
          { label: '颗粒感', value: '0', span: 1 },
          { label: '颗粒大小', value: '0', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '负片',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: 1,
      saturation: 3,
      warm_cool: 0,
      cyan_magenta: 0,
      sharpness: 1,
      vignette: false
    },
    deviceModel: 'GT 6',
    author: '@尼克lin',
    source: 'community',
    isFavorite: true,
    isNew: true,
    category: '人文',
    difficulty: '简单',
    tags: ['理光', '负片', 'GR'],
    description: {
      title: '拍摄建议',
      content: '【环境建议】日间户外，光线充足的场景\n【场景推荐】街拍、建筑、人文、日常记录\n【拍摄要点】模拟理光GR负片风格，色彩自然略带胶片感，适合追求真实质感的拍摄场景'
    }
  },
  // 扩展预设库 - 新增20款
  {
    id: 'sunset_warm',
    name: '夕阳暖调',
    coverPath: 'https://picsum.photos/seed/sunset-warm/400/600',
    galleryImages: ['https://picsum.photos/seed/sunset-warm2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '明艳 100%', span: 2 },
          { label: '饱和度', value: '+25', span: 1 },
          { label: '冷暖调', value: '+50', span: 1 },
          { label: '色调曲线', value: '+15', span: 1 },
          { label: '对比度', value: '+10', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '明艳',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: 15,
      saturation: 25,
      warm_cool: 50,
      cyan_magenta: 0,
      sharpness: 8,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '风景',
    difficulty: '简单',
    tags: ['夕阳', '暖调', '风景']
  },
  {
    id: 'neon_night',
    name: '霓虹夜色',
    coverPath: 'https://picsum.photos/seed/neon-night/400/600',
    galleryImages: ['https://picsum.photos/seed/neon-night2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '通透 100%', span: 2 },
          { label: '饱和度', value: '+30', span: 1 },
          { label: '冷暖调', value: '-30', span: 1 },
          { label: '青红调', value: '+20', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '通透',
      filter_intensity: 100,
      soft_light: '梦幻',
      tone_curve: -10,
      saturation: 30,
      warm_cool: -30,
      cyan_magenta: 20,
      sharpness: 12,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: true,
    isNew: true,
    category: '夜景',
    difficulty: '中等',
    tags: ['霓虹', '夜景', '城市']
  },
  {
    id: 'fresh_green',
    name: '清新绿野',
    coverPath: 'https://picsum.photos/seed/fresh-green/400/600',
    galleryImages: ['https://picsum.photos/seed/fresh-green2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '清新 100%', span: 2 },
          { label: '饱和度', value: '+15', span: 1 },
          { label: '冷暖调', value: '-10', span: 1 },
          { label: '青红调', value: '-15', span: 1 },
          { label: '亮度', value: '+10', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '清新',
      filter_intensity: 100,
      soft_light: '柔美',
      tone_curve: 20,
      saturation: 15,
      warm_cool: -10,
      cyan_magenta: -15,
      sharpness: 6,
      vignette: false,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '风景',
    difficulty: '简单',
    tags: ['清新', '绿植', '自然']
  },
  {
    id: 'portrait_soft',
    name: '柔光人像',
    coverPath: 'https://picsum.photos/seed/portrait-soft/400/600',
    galleryImages: ['https://picsum.photos/seed/portrait-soft2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '童话 80%', span: 2 },
          { label: '柔光', value: '柔美', span: 1 },
          { label: '饱和度', value: '+10', span: 1 },
          { label: '冷暖调', value: '+5', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '童话',
      filter_intensity: 80,
      soft_light: '柔美',
      tone_curve: 5,
      saturation: 10,
      warm_cool: 5,
      cyan_magenta: 0,
      sharpness: 0,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: true,
    isNew: false,
    category: '人像',
    difficulty: '简单',
    tags: ['人像', '柔光', '清新']
  },
  {
    id: 'bw_mood',
    name: '黑白情绪',
    coverPath: 'https://picsum.photos/seed/bw-mood/400/600',
    galleryImages: ['https://picsum.photos/seed/bw-mood2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '黑白 100%', span: 2 },
          { label: '对比度', value: '+30', span: 1 },
          { label: '色调曲线', value: '-20', span: 1 },
          { label: '颗粒感', value: '+10', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '黑白',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: -20,
      saturation: 0,
      warm_cool: 50,
      cyan_magenta: -20,
      sharpness: 5,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '纪实',
    difficulty: '中等',
    tags: ['黑白', '情绪', '纪实']
  },
  {
    id: 'food_vibrant',
    name: '美食诱人',
    coverPath: 'https://picsum.photos/seed/food-vibrant/400/600',
    galleryImages: ['https://picsum.photos/seed/food-vibrant2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '明艳 90%', span: 2 },
          { label: '饱和度', value: '+20', span: 1 },
          { label: '对比度', value: '+15', span: 1 },
          { label: '锐度', value: '+10', span: 1 },
          { label: '暖色调', value: '+15', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '明艳',
      filter_intensity: 90,
      soft_light: '无',
      tone_curve: 15,
      saturation: 20,
      warm_cool: 15,
      cyan_magenta: 0,
      sharpness: 10,
      vignette: false,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '美食',
    difficulty: '简单',
    tags: ['美食', '明艳', '暖色']
  },
  {
    id: 'vintage_film',
    name: '复古胶片',
    coverPath: 'https://picsum.photos/seed/vintage-film/400/600',
    galleryImages: ['https://picsum.photos/seed/vintage-film2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '复古 100%', span: 2 },
          { label: '饱和度', value: '+15', span: 1 },
          { label: '冷暖调', value: '+25', span: 1 },
          { label: '颗粒感', value: '+15', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '复古',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: 0,
      saturation: 15,
      warm_cool: 25,
      cyan_magenta: 0,
      sharpness: 0,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: true,
    isNew: false,
    category: '胶片',
    difficulty: '中等',
    tags: ['复古', '胶片', '怀旧']
  },
  {
    id: 'cinema_wide',
    name: '电影宽幅',
    coverPath: 'https://picsum.photos/seed/cinema-wide/400/600',
    galleryImages: ['https://picsum.photos/seed/cinema-wide2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '通透 85%', span: 2 },
          { label: '饱和度', value: '+10', span: 1 },
          { label: '对比度', value: '+20', span: 1 },
          { label: '冷暖调', value: '-15', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '通透',
      filter_intensity: 85,
      soft_light: '无',
      tone_curve: 10,
      saturation: 10,
      warm_cool: -15,
      cyan_magenta: 5,
      sharpness: 10,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '电影',
    difficulty: '进阶',
    tags: ['电影', '宽幅', '质感']
  },
  {
    id: 'spring_blossom',
    name: '春日樱花',
    coverPath: 'https://picsum.photos/seed/spring-blossom/400/600',
    galleryImages: ['https://picsum.photos/seed/spring-blossom2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '清新 100%', span: 2 },
          { label: '柔光', value: '柔美', span: 1 },
          { label: '饱和度', value: '+20', span: 1 },
          { label: '冷暖调', value: '+10', span: 1 },
          { label: '亮度', value: '+15', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '清新',
      filter_intensity: 100,
      soft_light: '柔美',
      tone_curve: 25,
      saturation: 20,
      warm_cool: 10,
      cyan_magenta: -10,
      sharpness: 5,
      vignette: false,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '风景',
    difficulty: '简单',
    tags: ['樱花', '春日', '清新']
  },
  {
    id: 'autumn_maple',
    name: '秋日枫叶',
    coverPath: 'https://picsum.photos/seed/autumn-maple/400/600',
    galleryImages: ['https://picsum.photos/seed/autumn-maple2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '明艳 100%', span: 2 },
          { label: '饱和度', value: '+25', span: 1 },
          { label: '暖色调', value: '+40', span: 1 },
          { label: '对比度', value: '+15', span: 1 },
          { label: '锐度', value: '+8', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '明艳',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: 15,
      saturation: 25,
      warm_cool: 40,
      cyan_magenta: 0,
      sharpness: 8,
      vignette: false,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '风景',
    difficulty: '简单',
    tags: ['秋日', '枫叶', '暖色']
  },
  {
    id: 'snow_white',
    name: '雪景纯净',
    coverPath: 'https://picsum.photos/seed/snow-white/400/600',
    galleryImages: ['https://picsum.photos/seed/snow-white2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '通透 100%', span: 2 },
          { label: '饱和度', value: '-5', span: 1 },
          { label: '对比度', value: '+20', span: 1 },
          { label: '冷暖调', value: '-20', span: 1 },
          { label: '亮度', value: '+25', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '通透',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: 30,
      saturation: -5,
      warm_cool: -20,
      cyan_magenta: 10,
      sharpness: 8,
      vignette: false,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '风景',
    difficulty: '简单',
    tags: ['雪景', '纯净', '冷色']
  },
  {
    id: 'sea_blue',
    name: '海天一色',
    coverPath: 'https://picsum.photos/seed/sea-blue/400/600',
    galleryImages: ['https://picsum.photos/seed/sea-blue2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '通透 100%', span: 2 },
          { label: '饱和度', value: '+15', span: 1 },
          { label: '冷色调', value: '-25', span: 1 },
          { label: '青色调', value: '+30', span: 1 },
          { label: '对比度', value: '+10', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '通透',
      filter_intensity: 100,
      soft_light: '柔美',
      tone_curve: 15,
      saturation: 15,
      warm_cool: -25,
      cyan_magenta: 30,
      sharpness: 10,
      vignette: false,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '风景',
    difficulty: '简单',
    tags: ['海边', '蓝色', '清新']
  },
  {
    id: 'street_story',
    name: '街拍故事',
    coverPath: 'https://picsum.photos/seed/street-story/400/600',
    galleryImages: ['https://picsum.photos/seed/street-story2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '复古 75%', span: 2 },
          { label: '饱和度', value: '+10', span: 1 },
          { label: '对比度', value: '+25', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '复古',
      filter_intensity: 75,
      soft_light: '无',
      tone_curve: -15,
      saturation: 10,
      warm_cool: 10,
      cyan_magenta: 0,
      sharpness: 8,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '纪实',
    difficulty: '中等',
    tags: ['街拍', '纪实', '黑白']
  },
  {
    id: 'night_cyber',
    name: '赛博夜景',
    coverPath: 'https://picsum.photos/seed/night-cyber/400/600',
    galleryImages: ['https://picsum.photos/seed/night-cyber2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '通透 100%', span: 2 },
          { label: '饱和度', value: '+40', span: 1 },
          { label: '对比度', value: '+35', span: 1 },
          { label: '冷色调', value: '-40', span: 1 },
          { label: '青色调', value: '+35', span: 1 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '通透',
      filter_intensity: 100,
      soft_light: '无',
      tone_curve: 20,
      saturation: 40,
      warm_cool: -40,
      cyan_magenta: 35,
      sharpness: 15,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '夜景',
    difficulty: '进阶',
    tags: ['赛博', '霓虹', '夜景']
  },
  {
    id: 'coffee_mood',
    name: '咖啡时光',
    coverPath: 'https://picsum.photos/seed/coffee-mood/400/600',
    galleryImages: ['https://picsum.photos/seed/coffee-mood2/400/600'],
    sections: [
      {
        title: '色彩调校',
        items: [
          { label: '滤镜', value: '复古 85%', span: 2 },
          { label: '饱和度', value: '+15', span: 1 },
          { label: '暖色调', value: '+30', span: 1 },
          { label: '颗粒感', value: '+10', span: 1 },
          { label: '暗角', value: '开', span: 2 }
        ]
      }
    ],
    cameraParams: {
      mode: 'master',
      filter: '复古',
      filter_intensity: 85,
      soft_light: '无',
      tone_curve: -5,
      saturation: 15,
      warm_cool: 30,
      cyan_magenta: 0,
      sharpness: 5,
      vignette: true,
      hncs: true
    },
    deviceModel: 'Find X9spro',
    author: '@小陈工',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '生活',
    difficulty: '简单',
    tags: ['咖啡', '文艺', '复古']
  }
];

// 水印模板数据
export const watermarkTemplates = [
  { id: 'hasselblad', name: '哈苏风格', description: '专业金色边框，哈苏标识' },
  { id: 'oppo', name: 'OPPO风格', description: '品牌橙色，简洁大方' },
  { id: 'oneplus', name: 'OnePlus风格', description: '红色点缀，Never Settle' },
  { id: 'realme', name: 'realme风格', description: '黄色活力，敢越级' },
  { id: 'minimal', name: '简约参数', description: '仅显示相机参数' },
  { id: 'timestamp', name: '时间戳', description: '显示拍摄时间' },
  { id: 'location', name: '地理位置', description: '显示拍摄地点' },
  { id: 'custom', name: '自定义', description: '完全自定义内容' },
  { id: 'brand-simple', name: '品牌简约', description: '小O帮帮标识' },
  { id: 'film-style', name: '胶片风格', description: '复古胶片边框' }
];

// 安全隐私功能演示数据
export const securityFeatures = [
  {
    title: '加密存储',
    items: [
      'AES-256-GCM加密算法',
      'Android Keystore密钥管理',
      'SecurePreferences安全存储',
      '数据完整性SHA-256校验'
    ]
  },
  {
    title: '网络安全',
    items: [
      'HTTPS强制，禁用明文流量',
      '网络安全配置(NSS)',
      '证书钉扎(可选)',
      '自定义更新源风险提示'
    ]
  },
  {
    title: '权限管理',
    items: [
      '最小权限原则',
      '动态申请权限',
      '用途透明说明',
      '拒绝授权不影响核心功能'
    ]
  },
  {
    title: '安全防护',
    items: [
      '输入注入防护',
      '代码混淆ProGuard/R8',
      '日志移除Release',
      '签名保护V1+V2+V3+V4'
    ]
  }
];

// 构建生成功能演示数据
export const buildFeatures = [
  {
    title: '构建流程',
    items: [
      'Gradle 8.x构建工具',
      'dependencyLocking依赖锁定',
      '代码混淆和资源压缩',
      'CI/CD自动化(GitHub Actions)'
    ]
  },
  {
    title: '发布管理',
    items: [
      '版本号管理(Semantic Versioning)',
      '多渠道发布一致性',
      '更新包SHA-256校验',
      '版本回退防护'
    ]
  },
  {
    title: '性能优化',
    items: [
      '首屏加载<2秒',
      '代码分割和懒加载',
      '图片WebP格式优化',
      '动画帧率>60fps'
    ]
  }
];
