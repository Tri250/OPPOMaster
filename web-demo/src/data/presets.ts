import type { Preset, CloudPreset, CloudPresetResponse, SceneType } from '../types';

// 场景类型
export const SCENE_TYPES: SceneType[] = [
  {
    id: 'portrait',
    name: '人像',
    icon: '👤',
    description: '适合拍摄人物肖像，自然美肤',
    color: 'from-pink-500 to-rose-600',
  },
  {
    id: 'landscape',
    name: '风景',
    icon: '🏔️',
    description: '增强天空和自然色彩，提升画质',
    color: 'from-blue-500 to-cyan-600',
  },
  {
    id: 'food',
    name: '美食',
    icon: '🍜',
    description: '增强食物色彩，让美食更诱人',
    color: 'from-orange-500 to-yellow-600',
  },
  {
    id: 'night',
    name: '夜景',
    icon: '🌙',
    description: '低光环境优化，减少噪点',
    color: 'from-purple-500 to-indigo-600',
  },
  {
    id: 'street',
    name: '街拍',
    icon: '🏙️',
    description: '城市街头纪实，高对比风格',
    color: 'from-gray-600 to-gray-800',
  },
  {
    id: 'auto',
    name: '智能',
    icon: '🤖',
    description: 'AI自动识别场景，智能匹配参数',
    color: 'from-green-500 to-emerald-600',
  },
];

// 官方预设 URLs
const OPPO_PRESETS_URL = 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json';
const REALME_PRESETS_URL = 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json';

// 预设主题对应的图片关键词
const presetThemeImages: Record<string, string> = {
  '德味预设': 'architecture',
  '富士胶片': 'film,portrait',
  '胶片感': 'vintage,portrait',
  '童话': 'fantasy,fairytale',
  '高对比黑白': 'monochrome,street',
  '理光绿': 'nature,forest',
  '理光蓝': 'sky,ocean',
  '蓝调时刻': 'night,bluehour,city',
  '梦幻黑柔': 'portrait,dreamy',
  '富士NC': 'film,portrait',
  '哈苏红': 'portrait,red',
  '徕卡M10': 'street,documentary',
  '哈苏自然色彩': 'landscape,nature',
  '哈苏蓝调': 'landscape,blue',
  '复古胶片': 'vintage,film',
  '理光正片': 'street,vibrant',
  '理光负片': 'street,negative',
};

// 处理封面路径
const getFullCoverPath = (path: string, presetName: string, source: 'oppo' | 'realme'): string => {
  console.log(`[Preset] Processing cover path: ${path} for: ${presetName}`);
  
  // 直接URL的情况
  if (path.startsWith('http')) {
    return path;
  }
  
  // 获取主题关键词
  const themeKeyword = presetThemeImages[presetName] || 'nature,landscape';
  
  // 使用专门的相机预设图片服务
  const imageUrl = `https://loremflickr.com/600/400/${encodeURIComponent(themeKeyword)}?lock=${encodeURIComponent(presetName)}`;
  
  console.log(`[Preset] Generated image URL: ${imageUrl}`);
  return imageUrl;
};

// 转换云端预设到应用预设
const convertCloudPresetToPreset = (cloudPreset: CloudPreset, index: number, source: 'oppo' | 'realme'): Preset => {
  console.log(`[Preset] Converting: ${cloudPreset.name} (${source})`);
  
  const sections = cloudPreset.sections.map((section) => ({
    title: section.title.replace('@string/', ''),
    content: section.items
      .map((item) => `${item.label.replace('@string/param_', '')}: ${item.value}`)
      .join('\n')
  }));

  // 从 sections 中提取参数
  const paramMap: Record<string, string> = {};
  cloudPreset.sections.forEach((section) => {
    section.items.forEach((item) => {
      const label = item.label.replace('@string/param_', '');
      paramMap[label] = item.value;
    });
  });

  const parseNumber = (value: string | undefined, defaultValue: number): number => {
    if (!value) return defaultValue;
    const num = parseFloat(value.replace('+', '').replace('%', ''));
    return isNaN(num) ? defaultValue : num;
  };

  const parseVignette = (value: string): number => {
    return value === '开' ? 0.2 : 0;
  };

  // 确定机型显示
  const getDeviceModel = () => {
    if (source === 'oppo') {
      return 'OPPO Find X 系列';
    } else if (source === 'realme') {
      return 'Realme GT 系列';
    }
    return '通用';
  };

  const preset = {
    id: `${source}-${cloudPreset.name.replace(/\s+/g, '-')}-${index}`,
    name: cloudPreset.name,
    coverPath: getFullCoverPath(cloudPreset.coverPath, cloudPreset.name, source),
    sections: sections,
    cameraParams: {
      mode: 'master',
      filter: paramMap['filter'] || paramMap['soft_light'] || '',
      iso: 100,
      shutter: '1/125',
      ev: paramMap['tone_curve'] || '0',
      wb: '5500K',
      hasselblad_hncs: false,
      contrast: parseNumber(paramMap['tone_curve'], 1.0),
      saturation: parseNumber(paramMap['saturation'], 1.0),
      sharpness: parseNumber(paramMap['sharpness'], 1.0),
      vignette: parseVignette(paramMap['vignette'] || '关'),
      videoLut: '',
      sceneTags: cloudPreset.tags
    },
    deviceModel: getDeviceModel(),
    source: source,
    isFavorite: false,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    usageCount: Math.floor(Math.random() * 5000) + 100, // 模拟使用次数
    rating: 4.0 + Math.random() * 1.0, // 随机评分
    author: cloudPreset.author,
    description: cloudPreset.description?.content
  };

  console.log(`[Preset] Success: ${preset.name}`);
  return preset;
};

// 加载官方预设数据
export const loadCloudPresets = async (): Promise<Preset[]> => {
  console.log('[Preset] Starting to load cloud presets...');
  
  try {
    console.log('[Preset] Fetching OPPO presets from:', OPPO_PRESETS_URL);
    const [oppoResponse, realmeResponse] = await Promise.all([
      fetch(OPPO_PRESETS_URL),
      fetch(REALME_PRESETS_URL)
    ]);

    if (!oppoResponse.ok) {
      throw new Error(`OPPO API error: ${oppoResponse.status} ${oppoResponse.statusText}`);
    }
    if (!realmeResponse.ok) {
      throw new Error(`Realme API error: ${realmeResponse.status} ${realmeResponse.statusText}`);
    }

    const oppoData: CloudPresetResponse = await oppoResponse.json();
    const realmeData: CloudPresetResponse = await realmeResponse.json();

    console.log('[Preset] OPPO data loaded:', oppoData.presets.length, 'presets');
    console.log('[Preset] Realme data loaded:', realmeData.presets.length, 'presets');

    const oppoPresets = oppoData.presets.map((preset, index) =>
      convertCloudPresetToPreset(preset, index, 'oppo'));
    const realmePresets = realmeData.presets.map((preset, index) =>
      convertCloudPresetToPreset(preset, index, 'realme'));

    const allPresets = [...oppoPresets, ...realmePresets];
    console.log('[Preset] Total presets loaded:', allPresets.length);
    
    return allPresets;
  } catch (error) {
    console.error('[Preset] Failed to load cloud presets:', error);
    throw error;
  }
};

// 示例预设数据（备用）
export const samplePresets: Preset[] = [
  {
    id: 'sample-1',
    name: '德味预设',
    coverPath: 'https://loremflickr.com/600/400/architecture,street?lock=德味预设',
    sections: [
      { title: '色彩调校', content: 'filter: 明艳 100%\nsoft_light: 无\ntone_curve: -35\nsaturation: 0\nwarm_cool: -5\ncyan_magenta: 4\nsharpness: 10\nvignette: 开' }
    ],
    cameraParams: {
      mode: 'master',
      filter: '明艳 100%',
      iso: 100,
      shutter: '1/125',
      ev: '-35',
      wb: '5500K',
      hasselblad_hncs: false,
      contrast: 0.65,
      saturation: 1.0,
      sharpness: 1.1,
      vignette: 0.2,
      videoLut: '',
      sceneTags: ['Auto']
    },
    deviceModel: 'OPPO Find X 系列',
    source: 'oppo',
    isFavorite: false,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    usageCount: 1245,
    rating: 4.5,
    author: '@波子Booz'
  }
];
