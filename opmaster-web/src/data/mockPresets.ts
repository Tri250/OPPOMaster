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
    isFavorite: false
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
    isFavorite: true
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
    isFavorite: false
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
    isFavorite: false
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
    isFavorite: false
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
    isFavorite: true
  }
];
