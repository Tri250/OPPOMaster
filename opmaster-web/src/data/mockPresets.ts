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
  style?: string;
  scene?: string;
};

// 筛选条件类型
export type FilterConfig = {
  selectedStyle: string | null;
  selectedScene: string | null;
  searchQuery: string;
  isFavoriteOnly: boolean;
  isNewOnly: boolean;
};

// 风格分类常量
export const PresetStyles = {
  ALL: '全部',
  FILM: '胶片',
  VINTAGE: '复古',
  FRESH: '清新',
  BLUE: '蓝调',
  DOCUMENTARY: '纪实',
  PORTRAIT: '人像',
  LANDSCAPE: '风景',
  CINEMA: '电影',
  FOOD: '美食',
  LIFE: '生活',
  STREET: '街拍',
  ARCHITECTURE: '建筑',
  BLACK_WHITE: '黑白',
  NEON: '霓虹',
  DREAM: '梦幻',
};

export const ALL_STYLES = Object.values(PresetStyles);

// 场景分类常量
export const PresetScenes = {
  ALL: '全部',
  PORTRAIT: '人像',
  NIGHT: '夜景',
  LANDSCAPE: '风景',
  SPORTS: '运动',
  FOOD: '美食',
  STREET: '街拍',
  ARCHITECTURE: '建筑',
  NATURE: '自然',
  CINEMA: '电影',
  TRAVEL: '旅行',
  PARTY: '派对',
  FAMILY: '家庭',
  SELFIE: '自拍',
};

export const ALL_SCENES = Object.values(PresetScenes);

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
  { id: 'encrypted-storage', title: '加密存储', description: 'AES-256-GCM加密存储，密钥由Android Keystore系统管理', icon: '🔐', category: 'security', demoAvailable: true },
  { id: 'https-only', title: 'HTTPS强制', description: '禁用明文流量，网络安全配置，证书钉扎支持', icon: '🔒', category: 'security', demoAvailable: true },
  { id: 'min-permissions', title: '最小权限', description: '遵循最小权限原则，无强制授权，隐私政策透明', icon: '🛡️', category: 'security', demoAvailable: true },
  { id: 'data-integrity', title: '数据完整性', description: 'SHA-256校验和，防止篡改，输入注入防护', icon: '✓', category: 'security', demoAvailable: true },
  
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

// 生成基础预设数据
function getBasicPresets(): Preset[] {
  return [
    // 胶片风格
    {
      id: 'hasselblad_dewei',
      name: '德味预设',
      coverPath: 'https://picsum.photos/seed/dewei/400/600',
      galleryImages: ['https://picsum.photos/seed/dewei2/400/600', 'https://picsum.photos/seed/dewei3/400/600'],
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
      tags: ['德味', '哈苏', '大师模式', '胶片', '复古'],
      description: {
        title: '拍摄建议',
        content: '环境建议:日间户外或光线充足的室内，场景推荐:街拍、建筑、风景、人文'
      },
      style: '胶片',
      scene: '街拍'
    },
    {
      id: 'fujifilm_film',
      name: '富士胶片',
      coverPath: 'https://picsum.photos/seed/fujifilm/400/600',
      galleryImages: ['https://picsum.photos/seed/fujifilm2/400/600', 'https://picsum.photos/seed/fujifilm3/400/600'],
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
        content: '适合追求经典胶片质感的场景'
      },
      style: '胶片',
      scene: '街拍'
    },
    // 清新风格
    {
      id: 'ricoh_green',
      name: '理光绿',
      coverPath: 'https://picsum.photos/seed/green/400/600',
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
        content: '环境建议:户外自然光，森林、草地、植物丰富的场景'
      },
      style: '清新',
      scene: '风景'
    },
    // 蓝调风格
    {
      id: 'blue_hour',
      name: '蓝调时刻',
      coverPath: 'https://picsum.photos/seed/bluehour/400/600',
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
        content: '环境建议:日出前或日落后20分钟的蓝调时刻'
      },
      style: '蓝调',
      scene: '夜景'
    },
    // 人像风格
    {
      id: 'fairy_tale',
      name: '童话',
      coverPath: 'https://picsum.photos/seed/fairytale/400/600',
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
        content: '环境建议:清晨、黄昏或阴天散射光，场景推荐:儿童摄影、花园'
      },
      style: '梦幻',
      scene: '人像'
    },
    // 黑白风格
    {
      id: 'high_contrast_bw',
      name: '高对比黑白',
      coverPath: 'https://picsum.photos/seed/bw/400/600',
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
        content: '环境建议:强烈光影对比场景，场景推荐:建筑、纪实摄影、街头'
      },
      style: '黑白',
      scene: '街拍'
    }
  ];
}

// 生成扩展预设数据（用于性能测试）
function getExtendedPresets(): Preset[] {
  const presets: Preset[] = [];
  const styles = ['胶片', '复古', '清新', '蓝调', '黑白', '梦幻', '霓虹', '纪实'];
  const scenes = ['人像', '风景', '街拍', '夜景', '美食', '建筑', '生活', '电影'];
  const baseNames = [
    '春日樱花', '秋日枫叶', '雪景纯净', '海天一色', '街拍故事',
    '夕阳暖调', '黑白情绪', '文艺日常', '城市夜景', '自然纪实'
  ];
  
  for (let i = 0; i < 100; i++) {
    const style = styles[i % styles.length];
    const scene = scenes[i % scenes.length];
    const baseName = baseNames[i % baseNames.length];
    
    presets.push({
      id: `extended_preset_${i}`,
      name: `${baseName} ${i + 1}`,
      coverPath: `https://picsum.photos/seed/ext${i}/400/600`,
      sections: [
        {
          title: '色彩调校',
          items: [
            { label: '滤镜', value: `${style === '胶片' ? '复古' : '标准'} ${70 + (i % 30)}%`, span: 2 }
          ]
        }
      ],
      cameraParams: {
        mode: 'master',
        filter: style === '胶片' ? '复古' : '标准',
        filter_intensity: 70 + (i % 30),
        soft_light: i % 3 === 0 ? '无' : i % 3 === 1 ? '柔美' : '梦幻',
        tone_curve: (i % 40) - 20,
        saturation: i % 30,
        warm_cool: (i % 60) - 30,
        cyan_magenta: i % 10 === 0 ? 0 : (i % 20) - 10,
        sharpness: i % 15,
        vignette: i % 2 === 0,
        hncs: true
      },
      deviceModel: i % 2 === 0 ? 'Find X9spro' : 'GT 6',
      author: `@摄影师${i + 1}`,
      source: i % 3 === 0 ? 'omaster_cloud' : 'community',
      isFavorite: i % 5 === 0,
      isNew: i % 7 === 0,
      category: style,
      difficulty: i % 3 === 0 ? '简单' : i % 3 === 1 ? '中等' : '进阶',
      tags: [style, scene, `预设${i + 1}`],
      description: {
        title: '拍摄建议',
        content: `这是一个示例预设，编号 ${i}，风格:${style}，场景:${scene}`
      },
      style: style,
      scene: scene
    });
  }
  
  return presets;
}

// OPPO FindX9spro哈苏大师模式预设 - 完整数据
export const mockPresets: Preset[] = [
  ...getBasicPresets(),
  ...getExtendedPresets()
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
  { id: 'film-style', name: '胶片风格', description: '复古胶片边框' },
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
      '网络安全配置（NSS）',
      '证书钉扎（可选）',
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
  },
];

// 构建生成功能演示数据
export const buildFeatures = [
  {
    title: '构建流程',
    items: [
      'Gradle 8.x构建工具',
      'dependencyLocking依赖锁定',
      '代码混淆和资源压缩',
      'CI/CD自动化（GitHub Actions）'
    ]
  },
  {
    title: '发布管理',
    items: [
      '版本号管理（Semantic Versioning）',
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
  },
];
