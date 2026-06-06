// 水印模板数据
export interface WatermarkTemplate {
  id: string;
  name: string;
  description: string;
  previewUrl: string;
  category: 'brand' | 'functional' | 'opensource';
  features: string[];
}

export const watermarkTemplates: WatermarkTemplate[] = [
  // 品牌水印
  {
    id: 'hasselblad',
    name: '哈苏认证',
    description: '官方哈苏HNCS认证水印',
    previewUrl: 'https://images.unsplash.com/photo-1516722768290-1234567890ab?w=400',
    category: 'brand',
    features: ['HNCS认证', '官方授权', '专业风格']
  },
  {
    id: 'oppo',
    name: 'OPPO品牌',
    description: 'OPPO官方品牌水印',
    previewUrl: 'https://images.unsplash.com/photo-1511707171634-1234567890ab?w=400',
    category: 'brand',
    features: ['品牌标识', '设备型号', '参数展示']
  },
  {
    id: 'oneplus',
    name: '一加风格',
    description: '一加手机专属水印',
    previewUrl: 'https://images.unsplash.com/photo-1518770660439-1234567890ab?w=400',
    category: 'brand',
    features: ['一加标识', '简洁设计', '参数信息']
  },
  // 功能水印
  {
    id: 'camera-info',
    name: '相机信息',
    description: '显示完整相机参数信息',
    previewUrl: 'https://images.unsplash.com/photo-1502920917128-1234567890ab?w=400',
    category: 'functional',
    features: ['ISO/快门', '光圈/焦距', '日期时间']
  },
  {
    id: 'timestamp',
    name: '时间戳',
    description: '简洁的时间日期水印',
    previewUrl: 'https://images.unsplash.com/photo-1507400476072-1234567890ab?w=400',
    category: 'functional',
    features: ['日期显示', '时间记录', '自定义格式']
  },
  {
    id: 'date-stamp',
    name: '日期印章',
    description: '证件照专用日期印章',
    previewUrl: 'https://images.unsplash.com/photo-1506798755371-1234567890ab?w=400',
    category: 'functional',
    features: ['用途声明', '红色警示', '防伪标记']
  },
  // 开源水印
  {
    id: 'tile-pattern',
    name: '平铺水印',
    description: '防盗用平铺水印',
    previewUrl: 'https://images.unsplash.com/photo-1518186282298-1234567890ab?w=400',
    category: 'opensource',
    features: ['全覆盖', '难去除', '防盗用']
  },
  {
    id: 'diagonal',
    name: '对角线',
    description: '对角线文字水印',
    previewUrl: 'https://images.unsplash.com/photo-1518793788026-1234567890ab?w=400',
    category: 'opensource',
    features: ['对角布局', '版权保护', '视觉冲击']
  },
  {
    id: 'copyright',
    name: '版权符号',
    description: '简洁版权声明水印',
    previewUrl: 'https://images.unsplash.com/photo-1504634568763-1234567890ab?w=400',
    category: 'opensource',
    features: ['©符号', '年份作者', '极简风格']
  },
  {
    id: 'signature',
    name: '签名水印',
    description: '手写风格签名效果',
    previewUrl: 'https://images.unsplash.com/photo-1507003216419-1234567890ab?w=400',
    category: 'opensource',
    features: ['手写体', '斜体效果', '个性化']
  },
  {
    id: 'social',
    name: '社交媒体',
    description: '社交媒体账号水印',
    previewUrl: 'https://images.unsplash.com/photo-1506784365847-1234567890ab?w=400',
    category: 'opensource',
    features: ['平台图标', '@账号', '多平台']
  },
  {
    id: 'minimal',
    name: '极简角标',
    description: '右下角极简文字',
    previewUrl: 'https://images.unsplash.com/photo-1507184682762-1234567890ab?w=400',
    category: 'opensource',
    features: ['极简设计', '角标位置', '低调优雅']
  }
];
