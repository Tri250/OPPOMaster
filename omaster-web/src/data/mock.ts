import type { Preset, BrandType, RemotePresetData, CameraConfig, SceneType, SceneDetectionResult, WatermarkTemplate } from "../types";

// 远程数据 URL
const PRESET_URLS: Record<BrandType, string> = {
  OPPO: "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json",
  REALME: "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json",
  VIVO: "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json",
  HONOR: "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json",
};

// 预设数据（按品牌分组）
let presetsCache: Preset[] = [];
let presetsByBrandCache: Record<BrandType, Preset[]> = {
  OPPO: [],
  REALME: [],
  VIVO: [],
  HONOR: [],
};

// 获取远程预设数据
export async function fetchPresets(): Promise<Preset[]> {
  if (presetsCache.length > 0) return presetsCache;

  const allPresets: Preset[] = [];
  const brandOrder: BrandType[] = ["OPPO", "REALME", "VIVO", "HONOR"];

  for (const brand of brandOrder) {
    try {
      const response = await fetch(PRESET_URLS[brand]);
      const data: RemotePresetData = await response.json();
      
      const brandPresets = data.presets.map((p, index) => {
        // 处理 coverPath，如果是相对路径则转为 CDN 基础路径
        let coverUrl = p.coverPath;
        if (coverUrl.startsWith("images/")) {
          const baseUrl = PRESET_URLS[brand].replace("/presets/v2/", "/presets/v2/images/");
          coverUrl = baseUrl.replace(brand.toLowerCase() + ".json", p.coverPath.replace("images/", ""));
        }
        
        // 处理 galleryImages
        const galleryImages = p.galleryImages.map(img => {
          if (img.startsWith("images/")) {
            const baseUrl = PRESET_URLS[brand].replace("/presets/v2/", "/presets/v2/images/");
            return baseUrl.replace(brand.toLowerCase() + ".json", img.replace("images/", ""));
          }
          return img;
        });

        return {
          id: `${brand.toLowerCase()}-${index + 1}`,
          brand,
          name: p.name,
          coverUrl,
          galleryImages,
          author: p.author,
          isNew: p.isNew,
          sections: p.sections,
          tags: p.tags,
          description: p.description,
          rating: 4.5 + Math.random() * 0.5,
          downloadCount: Math.floor(50000 + Math.random() * 200000),
          isHncsCertified: brand === "OPPO" && p.tags.includes("Auto"),
          isFavorite: false,
        };
      });

      presetsByBrandCache[brand] = brandPresets;
      allPresets.push(...brandPresets);
    } catch (error) {
      console.error(`Failed to fetch ${brand} presets:`, error);
    }
  }

  presetsCache = allPresets;
  return allPresets;
}

// 获取按品牌分组的预设
export function getPresetsByBrand(brand: BrandType): Preset[] {
  return presetsByBrandCache[brand] || [];
}

// 获取所有品牌预设（同步版本，使用预加载的静态数据）
export const presets: Preset[] = [
  // OPPO / 一加 预设
  {
    id: "oppo-1",
    brand: "OPPO",
    name: "德味预设",
    coverUrl: "https://cdn.fky.ltd/dw_01.webp",
    galleryImages: ["https://cdn.fky.ltd/dw_02.webp", "https://cdn.fky.ltd/dw_03.webp"],
    author: "@波子Booz",
    isNew: true,
    sections: [
      {
        title: "色彩调节",
        items: [
          { label: "滤镜", value: "明艳 100%", span: 2 },
          { label: "柔光", value: "无", span: 1 },
          { label: "色调曲线", value: "-35", span: 1 },
          { label: "饱和度", value: "0", span: 1 },
          { label: "冷暖", value: "-5", span: 1 },
          { label: "青品", value: "4", span: 1 },
          { label: "锐度", value: "10", span: 1 },
          { label: "暗角", value: "开", span: 2 },
        ],
      },
    ],
    tags: ["Auto", "德味", "街拍"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】日间户外或光线充足的室内\n【场景推荐】街拍、建筑、风景、人文\n【拍摄要点】德味风格，影调偏暗，色彩浓郁",
    },
    rating: 4.9,
    downloadCount: 158642,
    isHncsCertified: true,
    isFavorite: false,
  },
  {
    id: "oppo-2",
    brand: "OPPO",
    name: "富士胶片",
    coverUrl: "https://picsum.photos/seed/fuji-film/800/600",
    galleryImages: ["https://picsum.photos/seed/fuji-1/800/600", "https://picsum.photos/seed/fuji-2/800/600"],
    author: "@OPPO影像",
    sections: [
      {
        title: "色彩调节",
        items: [
          { label: "滤镜", value: "复古 100%", span: 2 },
          { label: "柔光", value: "无", span: 1 },
          { label: "色调曲线", value: "0", span: 1 },
          { label: "饱和度", value: "+19", span: 1 },
          { label: "冷暖", value: "-5", span: 1 },
          { label: "锐度", value: "15", span: 1 },
          { label: "暗角", value: "开", span: 2 },
        ],
      },
    ],
    tags: ["Auto", "胶片", "复古"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】日间户外或光线充足的室内\n【场景推荐】街拍、人像、风景、建筑\n【拍摄要点】经典胶片质感，色彩浓郁复古",
    },
    rating: 4.8,
    downloadCount: 152342,
    isHncsCertified: true,
    isFavorite: false,
  },
  {
    id: "oppo-3",
    brand: "OPPO",
    name: "胶片感",
    coverUrl: "https://picsum.photos/seed/film-feel/800/600",
    galleryImages: ["https://picsum.photos/seed/film-1/800/600", "https://picsum.photos/seed/film-2/800/600"],
    author: "@OPPO影像",
    sections: [
      {
        title: "色彩调节",
        items: [
          { label: "滤镜", value: "复古 75%", span: 2 },
          { label: "柔光", value: "柔美", span: 1 },
          { label: "色调曲线", value: "-5", span: 1 },
          { label: "饱和度", value: "+20", span: 1 },
          { label: "冷暖", value: "-3", span: 1 },
          { label: "青品", value: "+4", span: 1 },
          { label: "锐度", value: "7", span: 1 },
          { label: "暗角", value: "开", span: 2 },
        ],
      },
    ],
    tags: ["Auto", "胶片", "柔光"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】自然光或柔和人工光源\n【场景推荐】人像写真、静物、咖啡馆\n【拍摄要点】柔光效果营造梦幻氛围",
    },
    rating: 4.7,
    downloadCount: 87642,
    isHncsCertified: true,
    isFavorite: false,
  },
  {
    id: "oppo-4",
    brand: "OPPO",
    name: "童话",
    coverUrl: "https://picsum.photos/seed/fairy-tale/800/600",
    galleryImages: ["https://picsum.photos/seed/fairy-1/800/600", "https://picsum.photos/seed/fairy-2/800/600"],
    author: "@OPPO影像",
    sections: [
      {
        title: "色彩调节",
        items: [
          { label: "滤镜", value: "明艳 100%", span: 2 },
          { label: "柔光", value: "无", span: 1 },
          { label: "色调曲线", value: "-25", span: 1 },
          { label: "饱和度", value: "+15", span: 1 },
          { label: "冷暖", value: "+8", span: 1 },
          { label: "锐度", value: "8", span: 1 },
        ],
      },
    ],
    tags: ["Auto", "童话", "梦幻"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】光线柔和的场景\n【场景推荐】儿童、宠物、梦幻场景\n【拍摄要点】暖色调，色彩鲜艳",
    },
    rating: 4.6,
    downloadCount: 67890,
    isHncsCertified: true,
    isFavorite: false,
  },
  // Realme 预设
  {
    id: "realme-1",
    brand: "REALME",
    name: "理光正片",
    coverUrl: "https://cdn.fky.ltd/zwzp_01.webp",
    galleryImages: ["https://cdn.fky.ltd/zwzp_02.webp", "https://cdn.fky.ltd/zwzp_03.webp"],
    author: "@尼克lin",
    sections: [
      {
        title: "色彩调节",
        items: [
          { label: "滤镜", value: "正片", span: 2 },
          { label: "饱和度", value: "+4", span: 1 },
          { label: "色调", value: "+1", span: 1 },
          { label: "色调曲线", value: "-1", span: 1 },
          { label: "对比度", value: "+3", span: 1 },
          { label: "高光对比", value: "+2", span: 1 },
          { label: "阴影对比", value: "-2", span: 1 },
          { label: "锐度", value: "-1", span: 1 },
          { label: "颗粒", value: "+3", span: 1 },
          { label: "颗粒大小", value: "+2", span: 2 },
        ],
      },
    ],
    tags: ["Auto", "理光", "正片"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】日间户外，光线充足\n【场景推荐】街拍、建筑、人文\n【拍摄要点】模拟理光GR正片风格，色彩鲜艳对比度高",
    },
    rating: 4.8,
    downloadCount: 98642,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "realme-2",
    brand: "REALME",
    name: "理光负片",
    coverUrl: "https://cdn.fky.ltd/lgfp_01.webp",
    galleryImages: ["https://cdn.fky.ltd/lgfp_02.webp", "https://cdn.fky.ltd/lgfp_03.webp"],
    author: "@尼克lin",
    sections: [
      {
        title: "色彩调节",
        items: [
          { label: "滤镜", value: "负片", span: 2 },
          { label: "饱和度", value: "+3", span: 1 },
          { label: "色调", value: "+3", span: 1 },
          { label: "色调曲线", value: "+1", span: 1 },
          { label: "对比度", value: "+4", span: 1 },
          { label: "锐度", value: "+1", span: 1 },
          { label: "颗粒", value: "0", span: 1 },
        ],
      },
    ],
    tags: ["Auto", "理光", "负片"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】日间户外，光线充足\n【场景推荐】街拍、建筑、人文\n【拍摄要点】模拟理光GR负片风格，色彩自然略带胶片感",
    },
    rating: 4.7,
    downloadCount: 45230,
    isHncsCertified: false,
    isFavorite: false,
  },
  // vivo 预设
  {
    id: "vivo-1",
    brand: "VIVO",
    name: "富士胶片",
    coverUrl: "https://picsum.photos/seed/vivo-fuji/800/600",
    galleryImages: ["https://picsum.photos/seed/vivo-1/800/600", "https://picsum.photos/seed/vivo-2/800/600"],
    author: "@vivo",
    sections: [
      {
        title: "基本调节",
        items: [
          { label: "曝光", value: "-6", span: 1 },
          { label: "亮度", value: "+5", span: 1 },
          { label: "对比度", value: "+10", span: 1 },
          { label: "高光", value: "-4", span: 1 },
          { label: "阴影", value: "-15", span: 1 },
        ],
      },
      {
        title: "色彩调节",
        items: [
          { label: "光感", value: "+8", span: 1 },
          { label: "饱和度", value: "+10", span: 1 },
          { label: "色温", value: "-10", span: 1 },
          { label: "锐度", value: "+5", span: 1 },
        ],
      },
      {
        title: "专业参数",
        items: [
          { label: "ISO", value: "Auto", span: 1 },
          { label: "快门", value: "1/200", span: 1 },
          { label: "EV", value: "-0.7", span: 1 },
          { label: "白平衡", value: "4800K", span: 1 },
        ],
      },
    ],
    tags: ["胶片", "复古", "vivo", "蔡司"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】光线适中的场景\n【场景推荐】街拍、人像、风景\n【拍摄要点】低曝光、低阴影，营造胶片质感",
    },
    rating: 4.8,
    downloadCount: 112345,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "vivo-2",
    brand: "VIVO",
    name: "拍萌宠",
    coverUrl: "https://picsum.photos/seed/vivo-pet/800/600",
    galleryImages: ["https://picsum.photos/seed/pet-1/800/600", "https://picsum.photos/seed/pet-2/800/600"],
    author: "@vivo",
    sections: [
      {
        title: "基本调节",
        items: [
          { label: "曝光", value: "+8", span: 1 },
          { label: "亮度", value: "+10", span: 1 },
          { label: "对比度", value: "-8", span: 1 },
          { label: "高光", value: "+8", span: 1 },
          { label: "阴影", value: "-5", span: 1 },
        ],
      },
      {
        title: "色彩调节",
        items: [
          { label: "光感", value: "+15", span: 1 },
          { label: "饱和度", value: "+15", span: 1 },
          { label: "色温", value: "+6", span: 1 },
          { label: "锐度", value: "+20", span: 1 },
        ],
      },
    ],
    tags: ["萌宠", "宠物", "vivo", "蔡司"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】室内或室外光线充足\n【场景推荐】宠物、动态抓拍\n【拍摄要点】高锐度捕捉毛发细节",
    },
    rating: 4.6,
    downloadCount: 38900,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "vivo-3",
    brand: "VIVO",
    name: "拍夜景",
    coverUrl: "https://picsum.photos/seed/vivo-night/800/600",
    galleryImages: ["https://picsum.photos/seed/night-1/800/600", "https://picsum.photos/seed/night-2/800/600"],
    author: "@vivo",
    sections: [
      {
        title: "基本调节",
        items: [
          { label: "曝光", value: "-5", span: 1 },
          { label: "亮度", value: "+5", span: 1 },
          { label: "对比度", value: "+7", span: 1 },
          { label: "高光", value: "+10", span: 1 },
          { label: "阴影", value: "-8", span: 1 },
        ],
      },
      {
        title: "色彩调节",
        items: [
          { label: "光感", value: "+8", span: 1 },
          { label: "饱和度", value: "+15", span: 1 },
          { label: "色温", value: "-18", span: 1 },
          { label: "锐度", value: "+5", span: 1 },
        ],
      },
    ],
    tags: ["夜景", "城市", "vivo", "蔡司"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】夜晚或弱光环境\n【场景推荐】城市灯光、建筑\n【拍摄要点】降低色温保持冷调",
    },
    rating: 4.7,
    downloadCount: 54321,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "vivo-4",
    brand: "VIVO",
    name: "清透感",
    coverUrl: "https://picsum.photos/seed/vivo-clear/800/600",
    galleryImages: ["https://picsum.photos/seed/clear-1/800/600", "https://picsum.photos/seed/clear-2/800/600"],
    author: "@vivo",
    sections: [
      {
        title: "基本调节",
        items: [
          { label: "曝光", value: "+5", span: 1 },
          { label: "亮度", value: "+8", span: 1 },
          { label: "对比度", value: "+10", span: 1 },
          { label: "高光", value: "+8", span: 1 },
          { label: "阴影", value: "+3", span: 1 },
        ],
      },
      {
        title: "色彩调节",
        items: [
          { label: "光感", value: "+11", span: 1 },
          { label: "饱和度", value: "+13", span: 1 },
          { label: "色温", value: "-20", span: 1 },
          { label: "锐度", value: "+70", span: 1 },
        ],
      },
    ],
    tags: ["清新", "通透", "vivo", "蔡司"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】晴天或光线充足\n【场景推荐】户外、风景、人像\n【拍摄要点】极高锐度，冷色调",
    },
    rating: 4.9,
    downloadCount: 8923,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "vivo-5",
    brand: "VIVO",
    name: "拍美食",
    coverUrl: "https://picsum.photos/seed/vivo-food/800/600",
    galleryImages: ["https://picsum.photos/seed/food-1/800/600", "https://picsum.photos/seed/food-2/800/600"],
    author: "@vivo",
    sections: [
      {
        title: "基本调节",
        items: [
          { label: "曝光", value: "-5", span: 1 },
          { label: "亮度", value: "+14", span: 1 },
          { label: "对比度", value: "+5", span: 1 },
          { label: "高光", value: "+5", span: 1 },
          { label: "阴影", value: "-8", span: 1 },
        ],
      },
      {
        title: "色彩调节",
        items: [
          { label: "光感", value: "+10", span: 1 },
          { label: "饱和度", value: "+8", span: 1 },
          { label: "色温", value: "-12", span: 1 },
          { label: "锐度", value: "+5", span: 1 },
        ],
      },
    ],
    tags: ["美食", "静物", "vivo", "蔡司"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】餐厅或室内暖光\n【场景推荐】菜肴、甜品\n【拍摄要点】提亮阴影突出食物细节",
    },
    rating: 4.5,
    downloadCount: 23456,
    isHncsCertified: false,
    isFavorite: false,
  },
  // 荣耀 预设
  {
    id: "honor-1",
    brand: "HONOR",
    name: "拍人物",
    coverUrl: "https://picsum.photos/seed/honor-portrait/800/600",
    galleryImages: ["https://picsum.photos/seed/hp-1/800/600", "https://picsum.photos/seed/hp-2/800/600"],
    author: "@荣耀",
    sections: [
      {
        title: "专业参数",
        items: [
          { label: "ISO感光度", value: "500", span: 1 },
          { label: "快门速度", value: "1/20", span: 1 },
          { label: "AF对焦模式", value: "MF", span: 1 },
          { label: "WB白平衡", value: "4800", span: 1 },
          { label: "M测光模式", value: "中央重点测光", span: 1 },
        ],
      },
    ],
    tags: ["人像", "荣耀", "专业模式"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】室内或弱光环境\n【场景推荐】人物特写\n【拍摄要点】手动对焦，中央重点测光",
    },
    rating: 4.7,
    downloadCount: 67890,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "honor-2",
    brand: "HONOR",
    name: "拍海边",
    coverUrl: "https://picsum.photos/seed/honor-beach/800/600",
    galleryImages: ["https://picsum.photos/seed/beach-1/800/600", "https://picsum.photos/seed/beach-2/800/600"],
    author: "@荣耀",
    sections: [
      {
        title: "专业参数",
        items: [
          { label: "ISO感光度", value: "100", span: 1 },
          { label: "快门速度", value: "1/400", span: 1 },
          { label: "AF对焦模式", value: "AF-S", span: 1 },
          { label: "WB白平衡", value: "7000", span: 1 },
          { label: "M测光模式", value: "矩阵测光", span: 1 },
        ],
      },
    ],
    tags: ["海边", "风光", "荣耀", "专业模式"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】晴天海边\n【场景推荐】沙滩、海浪\n【拍摄要点】低ISO保证画质，高快门凝固浪花",
    },
    rating: 4.8,
    downloadCount: 45678,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "honor-3",
    brand: "HONOR",
    name: "拍日落",
    coverUrl: "https://picsum.photos/seed/honor-sunset/800/600",
    galleryImages: ["https://picsum.photos/seed/sunset-1/800/600", "https://picsum.photos/seed/sunset-2/800/600"],
    author: "@荣耀",
    sections: [
      {
        title: "专业参数",
        items: [
          { label: "ISO感光度", value: "125", span: 1 },
          { label: "快门速度", value: "1/100", span: 1 },
          { label: "AF对焦模式", value: "AF-S", span: 1 },
          { label: "WB白平衡", value: "6300", span: 1 },
          { label: "M测光模式", value: "矩阵测光", span: 1 },
        ],
      },
    ],
    tags: ["日落", "风光", "荣耀", "专业模式"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】黄昏时分\n【场景推荐】夕阳、晚霞\n【拍摄要点】使用2倍变焦，暖色调增强日落氛围",
    },
    rating: 4.9,
    downloadCount: 34567,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "honor-4",
    brand: "HONOR",
    name: "拍演唱会",
    coverUrl: "https://picsum.photos/seed/honor-concert/800/600",
    galleryImages: ["https://picsum.photos/seed/concert-1/800/600", "https://picsum.photos/seed/concert-2/800/600"],
    author: "@荣耀",
    sections: [
      {
        title: "专业参数",
        items: [
          { label: "ISO感光度", value: "1000", span: 1 },
          { label: "快门速度", value: "1/80", span: 1 },
          { label: "AF对焦模式", value: "AF-S", span: 1 },
          { label: "WB白平衡", value: "5000", span: 1 },
          { label: "M测光模式", value: "矩阵测光", span: 1 },
        ],
      },
    ],
    tags: ["演唱会", "夜景", "荣耀", "专业模式"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】暗光舞台\n【场景推荐】演出、灯光\n【拍摄要点】高ISO保证亮度，适当快门捕捉动态",
    },
    rating: 4.6,
    downloadCount: 23456,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "honor-5",
    brand: "HONOR",
    name: "拍风景",
    coverUrl: "https://picsum.photos/seed/honor-landscape/800/600",
    galleryImages: ["https://picsum.photos/seed/landscape-1/800/600", "https://picsum.photos/seed/landscape-2/800/600"],
    author: "@荣耀",
    sections: [
      {
        title: "专业参数",
        items: [
          { label: "ISO感光度", value: "100", span: 1 },
          { label: "快门速度", value: "1/60", span: 1 },
          { label: "AF对焦模式", value: "AF-S", span: 1 },
          { label: "WB白平衡", value: "4000", span: 1 },
          { label: "M测光模式", value: "矩阵测光", span: 1 },
        ],
      },
    ],
    tags: ["风景", "风光", "荣耀", "专业模式"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】白天户外\n【场景推荐】自然风光、建筑\n【拍摄要点】低ISO保证画质，冷白平衡增强通透感",
    },
    rating: 4.8,
    downloadCount: 56789,
    isHncsCertified: false,
    isFavorite: false,
  },
  {
    id: "honor-6",
    brand: "HONOR",
    name: "丁达尔效应",
    coverUrl: "https://picsum.photos/seed/honor-tyndall/800/600",
    galleryImages: ["https://picsum.photos/seed/tyndall-1/800/600", "https://picsum.photos/seed/tyndall-2/800/600"],
    author: "@荣耀",
    sections: [
      {
        title: "专业参数",
        items: [
          { label: "ISO感光度", value: "100", span: 1 },
          { label: "快门速度", value: "1/250", span: 1 },
          { label: "AF对焦模式", value: "AF-S", span: 1 },
          { label: "WB白平衡", value: "5500", span: 1 },
          { label: "M测光模式", value: "点测光", span: 1 },
        ],
      },
    ],
    tags: ["丁达尔", "光影", "荣耀", "专业模式"],
    description: {
      title: "拍摄建议",
      content: "【环境建议】有光束穿透的场景\n【场景推荐】森林、教堂\n【拍摄要点】点测光对准光束区域",
    },
    rating: 4.7,
    downloadCount: 12345,
    isHncsCertified: false,
    isFavorite: false,
  },
];

// 按品牌分组
export const presetsByBrand: Record<BrandType, Preset[]> = {
  OPPO: presets.filter(p => p.brand === "OPPO"),
  REALME: presets.filter(p => p.brand === "REALME"),
  VIVO: presets.filter(p => p.brand === "VIVO"),
  HONOR: presets.filter(p => p.brand === "HONOR"),
};

export const cameraConfigs: CameraConfig[] = [
  {
    id: "c01",
    name: "哈苏人像配置",
    description: "专业人像拍摄参数组合",
    iso: 100,
    shutter: "1/125",
    aperture: "f/1.8",
    ev: "+0.3",
    wb: "5200K",
    isFavorite: true,
    createdAt: Date.now() - 86400000,
    tags: ["人像", "专业"],
  },
  {
    id: "c02",
    name: "夜景拍摄配置",
    description: "低光环境下的最佳参数",
    iso: 3200,
    shutter: "1/30",
    aperture: "f/1.8",
    ev: "+0.7",
    wb: "4000K",
    isFavorite: false,
    createdAt: Date.now() - 172800000,
    tags: ["夜景", "低光"],
  },
  {
    id: "c03",
    name: "风景广角配置",
    description: "风光摄影的参数模板",
    iso: 64,
    shutter: "1/250",
    aperture: "f/8.0",
    ev: "+0.5",
    wb: "6000K",
    isFavorite: true,
    createdAt: Date.now() - 259200000,
    tags: ["风景", "广角"],
  },
];

export const sceneInfo: Record<SceneType, { name: string; color: string; icon: string }> = {
  portrait: { name: "人像", color: "#FF6B00", icon: "user" },
  landscape: { name: "风景", color: "#4ADE80", icon: "mountain" },
  night: { name: "夜景", color: "#3B82F6", icon: "moon" },
  food: { name: "美食", color: "#F59E0B", icon: "utensils" },
  street: { name: "街拍", color: "#9CA3AF", icon: "camera" },
  macro: { name: "微距", color: "#A855F7", icon: "flower" },
  sunset: { name: "日落", color: "#F97316", icon: "sun" },
  cityscape: { name: "城市", color: "#06B6D4", icon: "building" },
};

export function detectSceneFromImage(imageIndex: number, isOffline: boolean = false): SceneDetectionResult {
  const scenes: SceneType[] = ["portrait", "landscape", "night", "food", "street", "macro", "sunset", "cityscape"];
  const scene = scenes[imageIndex % scenes.length];
  const detectionTime = isOffline ? 120 + Math.random() * 80 : 180 + Math.random() * 120;
  const confidence = isOffline ? 0.78 + Math.random() * 0.12 : 0.85 + Math.random() * 0.12;

  return {
    scene,
    sceneName: sceneInfo[scene].name,
    confidence,
    detectionTime: Math.round(detectionTime),
    isOffline,
    recommendedPresetIds: getRecommendedPresetIds(scene),
  };
}

function getRecommendedPresetIds(scene: SceneType): string[] {
  const map: Record<SceneType, string[]> = {
    portrait: ["oppo-1", "vivo-1", "honor-1"],
    landscape: ["oppo-4", "vivo-4", "honor-5"],
    night: ["vivo-3", "honor-4"],
    food: ["vivo-5"],
    street: ["realme-1", "oppo-2"],
    macro: ["vivo-2"],
    sunset: ["honor-3", "oppo-3"],
    cityscape: ["vivo-3", "oppo-1"],
  };
  return map[scene] || [];
}

export const watermarkTemplateInfo: Record<WatermarkTemplate, { name: string; displayText: string; color: string }> = {
  HASSELBLAD: { name: "哈苏经典", displayText: "HASSELBLAD", color: "#FF6B00" },
  OPPO: { name: "OPPO", displayText: "OPPO", color: "#1A8CFF" },
  ONEPLUS: { name: "一加", displayText: "OnePlus", color: "#EB0028" },
  REALME: { name: "真我", displayText: "realme", color: "#FFC901" },
  CUSTOM: { name: "自定义", displayText: "CUSTOM", color: "#D4A574" },
};