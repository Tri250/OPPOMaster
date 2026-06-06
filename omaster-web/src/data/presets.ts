// 预设数据
export interface CameraParams {
  mode: string;
  iso: number;
  shutter: string;
  ev: string;
  wb: string;
  aperture: string;
  hasselblad_hncs: boolean;
  colorStyle: string;
}

export interface Preset {
  id: string;
  name: string;
  coverUrl: string;
  deviceModel: string;
  author: string;
  description: string;
  sceneType: string;
  tags: string[];
  rating: number;
  downloadCount: number;
  isHncsCertified: boolean;
  cameraParams: CameraParams;
}

export const presets: Preset[] = [
  {
    id: '1',
    name: '哈苏人像大师',
    coverUrl: 'https://images.unsplash.com/photo-1531746020798-e695blllll?w=400',
    deviceModel: 'OPPO Find X8 Ultra',
    author: '哈苏影像实验室',
    description: '专业人像拍摄预设，完美还原肤色质感',
    sceneType: '人像',
    tags: ['人像', '哈苏', '专业'],
    rating: 4.9,
    downloadCount: 158642,
    isHncsCertified: true,
    cameraParams: {
      mode: '哈苏大师',
      iso: 100,
      shutter: '1/200',
      ev: '+0.3',
      wb: '5500K',
      aperture: 'f/1.6',
      hasselblad_hncs: true,
      colorStyle: 'Portrait'
    }
  },
  {
    id: '2',
    name: '夜景璀璨',
    coverUrl: 'https://images.unsplash.com/photo-1519501021993-2dbdd66c1e1e?w=400',
    deviceModel: 'OPPO Find X8 Pro',
    author: 'OPPO影像团队',
    description: '城市夜景专用，智能降噪与高光抑制',
    sceneType: '夜景',
    tags: ['夜景', '城市', '长曝光'],
    rating: 4.8,
    downloadCount: 98234,
    isHncsCertified: true,
    cameraParams: {
      mode: '夜景模式',
      iso: 800,
      shutter: '1/30',
      ev: '-0.5',
      wb: '3200K',
      aperture: 'f/1.8',
      hasselblad_hncs: true,
      colorStyle: 'Night'
    }
  },
  {
    id: '3',
    name: '美食诱惑',
    coverUrl: 'https://images.unsplash.com/photo-1504674900606-5c5c5c5c5c5c?w=400',
    deviceModel: '一加 13',
    author: '美食摄影达人',
    description: '美食拍摄专属，色彩鲜艳饱和度高',
    sceneType: '美食',
    tags: ['美食', '鲜艳', '暖色调'],
    rating: 4.7,
    downloadCount: 76543,
    isHncsCertified: false,
    cameraParams: {
      mode: '美食模式',
      iso: 200,
      shutter: '1/125',
      ev: '+0.5',
      wb: '5000K',
      aperture: 'f/2.0',
      hasselblad_hncs: false,
      colorStyle: 'Vivid'
    }
  },
  {
    id: '4',
    name: '风光大片',
    coverUrl: 'https://images.unsplash.com/photo-1506905927340-1234567890ab?w=400',
    deviceModel: 'OPPO Find X8 Ultra',
    author: '风光摄影师',
    description: '风景拍摄预设，HDR增强与广角优化',
    sceneType: '风景',
    tags: ['风景', 'HDR', '广角'],
    rating: 4.9,
    downloadCount: 123456,
    isHncsCertified: true,
    cameraParams: {
      mode: '哈苏风景',
      iso: 100,
      shutter: '1/500',
      ev: '+0.0',
      wb: '5600K',
      aperture: 'f/8.0',
      hasselblad_hncs: true,
      colorStyle: 'Landscape'
    }
  },
  {
    id: '5',
    name: '街拍纪实',
    coverUrl: 'https://images.unsplash.com/photo-1477959855332-1234567890ab?w=400',
    deviceModel: 'realme GT7 Pro',
    author: '街头摄影师',
    description: '街头抓拍预设，快速对焦与自然色彩',
    sceneType: '街拍',
    tags: ['街拍', '纪实', '黑白'],
    rating: 4.6,
    downloadCount: 65432,
    isHncsCertified: false,
    cameraParams: {
      mode: '街拍模式',
      iso: 400,
      shutter: '1/250',
      ev: '+0.0',
      wb: 'AUTO',
      aperture: 'f/2.8',
      hasselblad_hncs: false,
      colorStyle: 'Street'
    }
  },
  {
    id: '6',
    name: '微距细节',
    coverUrl: 'https://images.unsplash.com/photo-1490750601234-1234567890ab?w=400',
    deviceModel: 'OPPO Find X8 Ultra',
    author: '微距爱好者',
    description: '微距拍摄预设，景深控制与锐度增强',
    sceneType: '微距',
    tags: ['微距', '细节', '花卉'],
    rating: 4.8,
    downloadCount: 45678,
    isHncsCertified: true,
    cameraParams: {
      mode: '微距模式',
      iso: 200,
      shutter: '1/160',
      ev: '+0.2',
      wb: '5200K',
      aperture: 'f/2.4',
      hasselblad_hncs: true,
      colorStyle: 'Macro'
    }
  }
];
