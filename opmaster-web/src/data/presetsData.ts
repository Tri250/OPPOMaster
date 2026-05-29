export interface PresetParams {
  exposureCompensation: number;
  iso: number;
  shutterSpeed: string;
  whiteBalance: { temperature: number; tint: number };
  hsl: { hue: number; saturation: number; lightness: number };
  contrast: number;
  saturation: number;
  sharpness: number;
  vignette: number;
  highlights: number;
  shadows: number;
  clarity: number;
}

export interface Preset {
  id: string;
  name: string;
  description: string;
  author: string;
  coverImage: string;
  tags: string[];
  style: string;
  scene: string;
  compatibleModels: string[];
  params: PresetParams;
  downloads: number;
  favorites: number;
  usageCount: number;
  createdAt: string;
  isFeatured?: boolean;
  badge?: string;
}

export const styleCategories = [
  { id: 'all', label: '全部风格' },
  { id: 'film', label: '胶片' },
  { id: 'retro', label: '复古' },
  { id: 'fresh', label: '清新' },
  { id: 'japanese', label: '日系' },
  { id: 'german', label: '德系' },
  { id: 'bw', label: '黑白' },
  { id: 'cyberpunk', label: '赛博朋克' },
  { id: 'portrait', label: '人像' },
  { id: 'landscape', label: '风光' },
  { id: 'food', label: '美食' },
  { id: 'night', label: '夜景' },
];

export const sceneCategories = [
  { id: 'all', label: '全部场景' },
  { id: 'portrait', label: '人像' },
  { id: 'food', label: '美食' },
  { id: 'landscape', label: '风光' },
  { id: 'night', label: '夜景' },
  { id: 'street', label: '街拍' },
  { id: 'still-life', label: '静物' },
  { id: 'pet', label: '宠物' },
  { id: 'architecture', label: '建筑' },
];

export const modelCategories = [
  { id: 'all', label: '全部机型' },
  { id: 'find', label: 'Find 系列' },
  { id: 'reno', label: 'Reno 系列' },
  { id: 'oneplus', label: '一加' },
  { id: 'other', label: '其他' },
];

export const sortOptions = [
  { id: 'match', label: '匹配度' },
  { id: 'hot', label: '热度' },
  { id: 'favorites', label: '收藏量' },
  { id: 'new', label: '最新' },
  { id: 'usage', label: '使用频率' },
];

export const mockPresets: Preset[] = [
  {
    id: '1',
    name: '哈苏经典人像',
    description: '复刻哈苏X2D色彩，皮肤通透自然，适合专业人像拍摄',
    author: '哈苏大师',
    coverImage: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=300&fit=crop',
    tags: ['哈苏', '人像', '胶片'],
    style: 'portrait',
    scene: 'portrait',
    compatibleModels: ['find', 'reno', 'oneplus'],
    params: {
      exposureCompensation: 0.3,
      iso: 100,
      shutterSpeed: '1/125',
      whiteBalance: { temperature: 5200, tint: 5 },
      hsl: { hue: 0, saturation: 10, lightness: 5 },
      contrast: 15,
      saturation: 8,
      sharpness: 25,
      vignette: 10,
      highlights: -5,
      shadows: 8,
      clarity: 12,
    },
    downloads: 12847,
    favorites: 3456,
    usageCount: 8923,
    createdAt: '2026-05-15',
    isFeatured: true,
    badge: '精选',
  },
  {
    id: '2',
    name: '赛博霓虹夜景',
    description: '霓虹色彩，高对比度，适合城市夜景街拍',
    author: '赛博摄影',
    coverImage: 'https://images.unsplash.com/photo-1493225255756-d9584f8606e9?w=400&h=300&fit=crop',
    tags: ['赛博朋克', '夜景', '霓虹'],
    style: 'cyberpunk',
    scene: 'night',
    compatibleModels: ['find', 'reno'],
    params: {
      exposureCompensation: -0.5,
      iso: 800,
      shutterSpeed: '1/30',
      whiteBalance: { temperature: 3800, tint: -10 },
      hsl: { hue: 15, saturation: 35, lightness: -5 },
      contrast: 40,
      saturation: 45,
      sharpness: 35,
      vignette: 20,
      highlights: 10,
      shadows: -15,
      clarity: 30,
    },
    downloads: 8956,
    favorites: 2134,
    usageCount: 5621,
    createdAt: '2026-05-20',
    isFeatured: true,
    badge: '热门',
  },
  {
    id: '3',
    name: '日系清新人像',
    description: '日系小清新风格，色彩柔和，适合户外人像',
    author: '日系摄影',
    coverImage: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400&h=300&fit=crop',
    tags: ['日系', '清新', '人像'],
    style: 'japanese',
    scene: 'portrait',
    compatibleModels: ['find', 'reno', 'oneplus', 'other'],
    params: {
      exposureCompensation: 0.7,
      iso: 100,
      shutterSpeed: '1/200',
      whiteBalance: { temperature: 5800, tint: 8 },
      hsl: { hue: 5, saturation: -10, lightness: 15 },
      contrast: -8,
      saturation: -5,
      sharpness: 15,
      vignette: 5,
      highlights: 10,
      shadows: 5,
      clarity: 8,
    },
    downloads: 15678,
    favorites: 4231,
    usageCount: 10234,
    createdAt: '2026-05-10',
  },
  {
    id: '4',
    name: '德系黑白风光',
    description: '徕卡风格黑白，高对比度，层次丰富',
    author: '黑白大师',
    coverImage: 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=400&h=300&fit=crop',
    tags: ['黑白', '德系', '风光'],
    style: 'bw',
    scene: 'landscape',
    compatibleModels: ['find'],
    params: {
      exposureCompensation: 0,
      iso: 100,
      shutterSpeed: '1/60',
      whiteBalance: { temperature: 5000, tint: 0 },
      hsl: { hue: 0, saturation: -100, lightness: 0 },
      contrast: 35,
      saturation: -100,
      sharpness: 30,
      vignette: 15,
      highlights: -10,
      shadows: 15,
      clarity: 25,
    },
    downloads: 7234,
    favorites: 1876,
    usageCount: 4321,
    createdAt: '2026-05-18',
  },
  {
    id: '5',
    name: '复古胶片美食',
    description: '富士胶片色彩，美食拍摄首选',
    author: '美食摄影师',
    coverImage: 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=400&h=300&fit=crop',
    tags: ['复古', '胶片', '美食'],
    style: 'retro',
    scene: 'food',
    compatibleModels: ['reno', 'oneplus'],
    params: {
      exposureCompensation: 0.5,
      iso: 200,
      shutterSpeed: '1/100',
      whiteBalance: { temperature: 4800, tint: -3 },
      hsl: { hue: -5, saturation: 20, lightness: 3 },
      contrast: 12,
      saturation: 18,
      sharpness: 22,
      vignette: 8,
      highlights: -3,
      shadows: 6,
      clarity: 15,
    },
    downloads: 9876,
    favorites: 2543,
    usageCount: 6789,
    createdAt: '2026-05-22',
  },
];
