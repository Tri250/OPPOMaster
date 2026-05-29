import {
  Camera,
  Sparkles,
  Palette,
  Layers,
  Edit3,
  Settings,
  UploadCloud,
  Users,
  Star,
  Scan,
  Filter,
  Smartphone,
  Zap,
  Sliders,
  Award,
  Compass,
  Monitor,
} from 'lucide-react';

export interface FeatureEntry {
  id: string;
  title: string;
  description: string;
  icon: any;
  color: string;
  path: string;
  category: 'photography' | 'ai' | 'tools' | 'community' | 'settings';
  isFeatured?: boolean;
  badge?: string;
}

export const featureEntries: FeatureEntry[] = [
  // 摄影核心功能
  {
    id: 'native-camera',
    title: '原生相机',
    description: 'ColorOS相机深度集成，实时参数调节',
    icon: Camera,
    color: 'from-oppo-orange to-hasselblad-orange',
    path: '/native-camera',
    category: 'photography',
    isFeatured: true,
    badge: '核心',
  },
  {
    id: 'filter-library',
    title: '滤镜预设库',
    description: '海量大师预设，哈苏色彩调校',
    icon: Palette,
    color: 'from-oppo-blue to-oppo-purple',
    path: '/filter-library',
    category: 'photography',
    isFeatured: true,
  },
  {
    id: 'preset-editor',
    title: '预设编辑器',
    description: '自定义调色，创造专属风格',
    icon: Sliders,
    color: 'from-oppo-green to-oppo-blue',
    path: '/editor',
    category: 'photography',
  },
  {
    id: 'lut-manager',
    title: 'LUT管理器',
    description: '导入专业3D LUT，电影级调色',
    icon: Filter,
    color: 'from-oppo-purple to-oppo-pink',
    path: '/lut-manager',
    category: 'photography',
  },
  {
    id: 'master-params',
    title: '大师参数',
    description: '专业摄影师独家参数分享',
    icon: Award,
    color: 'from-hasselblad-orange to-hasselblad-gold',
    path: '/master-params',
    category: 'photography',
    badge: '精选',
  },
  {
    id: 'watermark',
    title: '水印大师',
    description: '哈苏风格水印，专业照片签名',
    icon: Edit3,
    color: 'from-oppo-orange to-oppo-pink',
    path: '/watermark',
    category: 'photography',
  },

  // AI智能功能
  {
    id: 'ai-scene',
    title: 'AI场景识别',
    description: '666种场景，智能匹配最佳预设',
    icon: Sparkles,
    color: 'from-oppo-blue to-oppo-purple',
    path: '/ai-demo',
    category: 'ai',
    isFeatured: true,
    badge: 'AI',
  },
  {
    id: 'ai-finetune',
    title: 'AI精细微调',
    description: '机器学习优化你的照片效果',
    icon: Zap,
    color: 'from-oppo-purple to-oppo-blue',
    path: '/ai-finetune',
    category: 'ai',
  },
  {
    id: 'scene-detection',
    title: '场景检测',
    description: '实时分析照片场景特征',
    icon: Compass,
    color: 'from-oppo-green to-oppo-purple',
    path: '/scene-detection',
    category: 'ai',
  },
  {
    id: 'ocr-demo',
    title: 'OCR参数识别',
    description: '智能识别并提取相机参数',
    icon: Scan,
    color: 'from-oppo-orange to-oppo-blue',
    path: '/ocr-demo',
    category: 'ai',
  },

  // 工具功能
  {
    id: 'floating-window',
    title: '悬浮窗助手',
    description: '边拍边调，专业参数实时可见',
    icon: Layers,
    color: 'from-oppo-green to-oppo-blue',
    path: '/floating-window',
    category: 'tools',
    isFeatured: true,
  },
  {
    id: 'cloud-sync',
    title: '云同步',
    description: '跨设备同步你的预设库',
    icon: UploadCloud,
    color: 'from-oppo-blue to-oppo-green',
    path: '/cloud-sync',
    category: 'tools',
  },
  {
    id: 'preset-ecosystem',
    title: '预设生态',
    description: '探索更多摄影师的精彩作品',
    icon: Layers,
    color: 'from-oppo-purple to-oppo-orange',
    path: '/preset-ecosystem',
    category: 'tools',
  },
  {
    id: 'test-verification',
    title: '测试验证',
    description: '验证预设效果和设备兼容性',
    icon: Monitor,
    color: 'from-oppo-blue to-oppo-green',
    path: '/test-verification',
    category: 'tools',
  },
  {
    id: 'settings',
    title: '系统设置',
    description: '个性化配置，打造专属体验',
    icon: Settings,
    color: 'from-neutral-500 to-neutral-700',
    path: '/settings',
    category: 'settings',
  },

  // 社区功能
  {
    id: 'community',
    title: '摄影社区',
    description: '与全球摄影师分享交流',
    icon: Users,
    color: 'from-oppo-orange to-oppo-pink',
    path: '/community',
    category: 'community',
    isFeatured: true,
  },
  {
    id: 'subscription',
    title: '订阅管理',
    description: '高级功能解锁，畅享全部体验',
    icon: Star,
    color: 'from-hasselblad-orange to-oppo-orange',
    path: '/subscription',
    category: 'community',
  },
  {
    id: 'xiao-o-help',
    title: '小O帮帮',
    description: '智能助手，随时解答你的问题',
    icon: Smartphone,
    color: 'from-oppo-green to-oppo-orange',
    path: '/xiao-o-help',
    category: 'community',
    badge: '助手',
  },
];

export const categories = [
  { id: 'all', label: '全部' },
  { id: 'photography', label: '摄影' },
  { id: 'ai', label: 'AI智能' },
  { id: 'tools', label: '工具' },
  { id: 'community', label: '社区' },
];
