import type { Preset, CloudPreset, CloudPresetResponse } from '../types';

// 官方预设 URLs
const OPPO_PRESETS_URL = 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json';
const REALME_PRESETS_URL = 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json';

// 处理封面路径
const getFullCoverPath = (path: string): string => {
  if (path.startsWith('http')) return path;
  if (path.startsWith('images/')) {
    return `https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/${path}`;
  }
  return path;
};

// 转换云端预设到应用预设
const convertCloudPresetToPreset = (cloudPreset: CloudPreset, index: number, source: string): Preset => {
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

  return {
    id: `${source}-${cloudPreset.name.replace(/\s+/g, '-')}-${index}`,
    name: cloudPreset.name,
    coverPath: getFullCoverPath(cloudPreset.coverPath),
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
    deviceModel: source === 'oppo' ? 'OPPO Find X' : 'Realme GR',
    source: source,
    isFavorite: false,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    usageCount: 0,
    rating: 4.5,
    author: cloudPreset.author,
    description: cloudPreset.description?.content
  };
};

// 加载官方预设数据
export const loadCloudPresets = async (): Promise<Preset[]> => {
  try {
    const [oppoResponse, realmeResponse] = await Promise.all([
      fetch(OPPO_PRESETS_URL),
      fetch(REALME_PRESETS_URL)
    ]);

    const oppoData: CloudPresetResponse = await oppoResponse.json();
    const realmeData: CloudPresetResponse = await realmeResponse.json();

    const oppoPresets = oppoData.presets.map((preset, index) =>
      convertCloudPresetToPreset(preset, index, 'oppo'));
    const realmePresets = realmeData.presets.map((preset, index) =>
      convertCloudPresetToPreset(preset, index, 'realme'));

    return [...oppoPresets, ...realmePresets];
  } catch (error) {
    console.error('Failed to load cloud presets:', error);
    return [];
  }
};

// 示例预设数据（备用）
export const samplePresets: Preset[] = [
  {
    id: 'sample-1',
    name: '德味预设',
    coverPath: 'https://cdn.fky.ltd/dw_01.webp',
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
    deviceModel: 'OPPO Find X',
    source: 'oppo',
    isFavorite: false,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    usageCount: 0,
    rating: 4.5,
    author: '@波子Booz'
  }
];
