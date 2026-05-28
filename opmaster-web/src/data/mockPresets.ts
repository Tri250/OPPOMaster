export type Section = {
  title: string;
  content: string;
};

export type CameraParams = {
  mode: string;
  filter: string;
  iso: number;
  shutter: string;
  ev: string;
  wb: string;
  hasselblad_hncs: boolean;
};

export type Preset = {
  id: string;
  name: string;
  coverPath: string;
  sections: Section[];
  cameraParams: CameraParams | null;
  deviceModel: string;
  source: 'omaster_cloud' | 'community';
  isFavorite: boolean;
  isNew?: boolean;
  category?: string;
  difficulty?: string;
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

// 扩展的预设数据
export const mockPresets: Preset[] = [
  {
    id: '1',
    name: '哈苏 X2D | 慵懒午后的佛罗伦萨',
    coverPath: 'hasselblad_florence_01',
    sections: [
      { title: '光感设置', content: '降低对比度，提高高光保留' },
      { title: '色彩调校', content: '暖色调偏移，饱和度适中' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '复古',
      iso: 200,
      shutter: '1/250',
      ev: '+0.3',
      wb: '5600K',
      hasselblad_hncs: true
    },
    deviceModel: 'Find X8 Pro',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '风景',
    difficulty: '中等'
  },
  {
    id: '2',
    name: '京都夜色 | 霓虹光斑',
    coverPath: 'kyoto_night_01',
    sections: [
      { title: '夜景优化', content: '高ISO降噪，长曝光' },
      { title: '色彩强化', content: '霓虹色饱和度提升' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '夜景',
      iso: 800,
      shutter: '1/30',
      ev: '-0.7',
      wb: '4200K',
      hasselblad_hncs: false
    },
    deviceModel: 'Find X8 Ultra',
    source: 'omaster_cloud',
    isFavorite: true,
    isNew: false,
    category: '夜景',
    difficulty: '进阶'
  },
  {
    id: '3',
    name: '北欧森林 | 自然清新',
    coverPath: 'nordic_forest_01',
    sections: [
      { title: '绿色优化', content: '树叶色彩还原' },
      { title: '动态范围', content: '高对比度保留细节' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '自然',
      iso: 100,
      shutter: '1/500',
      ev: '0',
      wb: '5200K',
      hasselblad_hncs: true
    },
    deviceModel: 'Reno 12 Pro',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '风景',
    difficulty: '简单'
  },
  {
    id: '4',
    name: '海边日落 | 温暖橙调',
    coverPath: 'sunset_beach_01',
    sections: [
      { title: '金色时刻', content: '暖色调强化' },
      { title: '天空细节', content: '渐变层次保留' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '暖调',
      iso: 100,
      shutter: '1/200',
      ev: '+0.7',
      wb: '6000K',
      hasselblad_hncs: true
    },
    deviceModel: 'Find X7 Ultra',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '风景',
    difficulty: '中等'
  },
  {
    id: '5',
    name: '城市街头 | 黑白纪实',
    coverPath: 'city_street_01',
    sections: [
      { title: '黑白模式', content: '高对比度黑白' },
      { title: '颗粒感', content: '胶片颗粒模拟' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '黑白',
      iso: 400,
      shutter: '1/1000',
      ev: '0',
      wb: '自动',
      hasselblad_hncs: false
    },
    deviceModel: 'Find X8',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '街拍',
    difficulty: '进阶'
  },
  {
    id: '6',
    name: '春日樱花 | 粉调柔焦',
    coverPath: 'sakura_spring_01',
    sections: [
      { title: '粉色优化', content: '樱花色彩还原' },
      { title: '柔焦效果', content: '轻微虚化处理' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '人像',
      iso: 200,
      shutter: '1/320',
      ev: '+0.3',
      wb: '5800K',
      hasselblad_hncs: true
    },
    deviceModel: 'Reno 12',
    source: 'omaster_cloud',
    isFavorite: true,
    isNew: false,
    category: '人像',
    difficulty: '简单'
  },
  // 新增更多预设
  {
    id: '7',
    name: '美食探店 | 诱人食欲',
    coverPath: 'food_dining_01',
    sections: [
      { title: '色彩提升', content: '食物色彩更鲜艳' },
      { title: '高光处理', content: '保留食物表面光泽' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '美食',
      iso: 160,
      shutter: '1/125',
      ev: '+0.5',
      wb: '5000K',
      hasselblad_hncs: false
    },
    deviceModel: 'Find X8',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '美食',
    difficulty: '简单'
  },
  {
    id: '8',
    name: '建筑大师 | 线条与光影',
    coverPath: 'architecture_01',
    sections: [
      { title: '几何校正', content: '透视畸变最小化' },
      { title: '光影对比', content: '明暗层次鲜明' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '专业',
      iso: 100,
      shutter: '1/640',
      ev: '-0.3',
      wb: '5500K',
      hasselblad_hncs: true
    },
    deviceModel: 'Find X8 Ultra',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '建筑',
    difficulty: '进阶'
  },
  {
    id: '9',
    name: '胶片模拟 | Kodak Portra',
    coverPath: 'film_kodak_01',
    sections: [
      { title: '胶片色彩', content: 'Portra 400色彩模拟' },
      { title: '颗粒质感', content: '轻微颗粒增加质感' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '胶片',
      iso: 400,
      shutter: '1/250',
      ev: '0',
      wb: '5300K',
      hasselblad_hncs: false
    },
    deviceModel: 'Reno 12 Pro',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: true,
    category: '胶片',
    difficulty: '中等'
  },
  {
    id: '10',
    name: '星空银河 | 长曝光',
    coverPath: 'starry_night_01',
    sections: [
      { title: '长曝光', content: '捕捉星星移动轨迹' },
      { title: '噪点控制', content: '高ISO下的降噪处理' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '星空',
      iso: 3200,
      shutter: '20s',
      ev: '0',
      wb: '4000K',
      hasselblad_hncs: true
    },
    deviceModel: 'Find X8 Pro',
    source: 'omaster_cloud',
    isFavorite: false,
    isNew: false,
    category: '夜景',
    difficulty: '专家'
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
  { id: 'brand-simple', name: '品牌简约', description: 'OMaster标识' },
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
