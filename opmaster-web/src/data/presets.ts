export interface Preset {
  id: string;
  name: string;
  coverPath: string;
  galleryImages: string[];
  author: string;
  isNew?: boolean;
  tags: string[];
  description: {
    title: string;
    content: string;
  };
}

// OPPO Presets
const oppoPresetsData: Preset[] = [
  {
    id: "oppo-1",
    name: "德味预设",
    coverPath: "https://cdn.fky.ltd/dw_01.webp",
    galleryImages: [
      "https://cdn.fky.ltd/dw_02.webp",
      "https://cdn.fky.ltd/dw_03.webp"
    ],
    author: "@波子Booz",
    isNew: true,
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】日间户外或光线充足的室内\n【场景推荐】街拍、建筑、风景、人文\n【拍摄要点】德味风格，影调偏暗，色彩浓郁，适合追求经典德系胶片质感的摄影爱好者"
    }
  },
  {
    id: "oppo-2",
    name: "富士胶片",
    coverPath: "https://picsum.photos/seed/fujifilm/600/450",
    galleryImages: [
      "https://picsum.photos/seed/fujifilm1/600/450",
      "https://picsum.photos/seed/fujifilm2/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】日间户外或光线充足的室内\n【场景推荐】街拍、人像、风景、建筑\n【拍摄要点】适合追求经典胶片质感的场景，色彩浓郁复古，建议寻找有光影对比的场景增强层次感"
    }
  },
  {
    id: "oppo-3",
    name: "胶片感",
    coverPath: "https://picsum.photos/seed/film/600/450",
    galleryImages: [
      "https://picsum.photos/seed/film1/600/450",
      "https://picsum.photos/seed/film2/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】自然光或柔和人工光源\n【场景推荐】人像写真、静物、咖啡馆、文艺场景\n【拍摄要点】柔光效果营造梦幻氛围，适合拍摄情绪感照片，建议对焦主体保持清晰"
    }
  },
  {
    id: "oppo-4",
    name: "童话",
    coverPath: "https://picsum.photos/seed/fairytale/600/450",
    galleryImages: [
      "https://picsum.photos/seed/fairytale1/600/450",
      "https://picsum.photos/seed/fairytale2/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】清晨、黄昏或阴天散射光\n【场景推荐】儿童摄影、花园、公园、浪漫场景\n【拍摄要点】影调偏暗营造神秘感，梦幻柔光适合营造童话氛围，建议寻找色彩丰富的场景"
    }
  },
  {
    id: "oppo-5",
    name: "高对比黑白",
    coverPath: "https://picsum.photos/seed/bw/600/450",
    galleryImages: [
      "https://picsum.photos/seed/bw1/600/450",
      "https://picsum.photos/seed/bw2/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】强烈光影对比场景，如阳光直射或聚光灯\n【场景推荐】建筑、纪实摄影、街头、艺术人像\n【拍摄要点】利用明暗对比突出主体轮廓，适合几何线条和纹理丰富的场景，注意构图简洁有力"
    }
  },
  {
    id: "oppo-6",
    name: "理光绿",
    coverPath: "https://picsum.photos/seed/ricohg/600/450",
    galleryImages: [
      "https://picsum.photos/seed/ricohg1/600/450",
      "https://picsum.photos/seed/ricohg2/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】户外自然光，森林、草地、植物丰富的场景\n【场景推荐】植物摄影、森林漫步、春日户外、清新人像\n【拍摄要点】影调偏亮突出清新感，绿色表现自然通透，适合拍摄植物和户外自然场景"
    }
  },
  {
    id: "oppo-7",
    name: "理光蓝",
    coverPath: "https://picsum.photos/seed/ricohb/600/450",
    galleryImages: [
      "https://picsum.photos/seed/ricohb1/600/450",
      "https://picsum.photos/seed/ricohb2/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】晴朗天气或蓝天背景\n【场景推荐】海边、城市建筑、天空、冷色调场景\n【拍摄要点】偏冷色调增强蓝色表现力，适合拍摄天空、水面和城市建筑，营造通透冷静的氛围"
    }
  },
  {
    id: "oppo-8",
    name: "蓝调时刻",
    coverPath: "https://picsum.photos/seed/bluemoment/600/450",
    galleryImages: [
      "https://picsum.photos/seed/bluemoment1/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】日出前或日落后20分钟的蓝调时刻\n【场景推荐】城市夜景、灯光璀璨的场景、水面倒影\n【拍摄要点】冷暖对比强烈，适合拍摄城市灯光和夜景，建议寻找有水面的场景增强倒影效果"
    }
  },
  {
    id: "oppo-9",
    name: "梦幻黑柔",
    coverPath: "https://picsum.photos/seed/dreamsoft/600/450",
    galleryImages: [
      "https://picsum.photos/seed/dreamsoft1/600/450",
      "https://picsum.photos/seed/dreamsoft2/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】逆光或侧逆光场景\n【场景推荐】人像写真、情绪摄影、艺术场景、柔美人像\n【拍摄要点】黑柔滤镜效果营造梦幻氛围，适合拍摄唯美人像，建议利用逆光创造光晕效果"
    }
  },
  {
    id: "oppo-10",
    name: "富士NC",
    coverPath: "https://picsum.photos/seed/fujinc/600/450",
    galleryImages: [
      "https://picsum.photos/seed/fujinc1/600/450",
      "https://picsum.photos/seed/fujinc2/600/450"
    ],
    author: "@OPPO影像",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】日间户外，光线充足\n【场景推荐】街拍、人文、日常记录\n【拍摄要点】色彩鲜艳自然，适合记录生活中的美好瞬间"
    }
  },
  {
    id: "realme-1",
    name: "理光正片",
    coverPath: "https://cdn.fky.ltd/zwzp_01.webp",
    galleryImages: [
      "https://cdn.fky.ltd/zwzp_02.webp",
      "https://cdn.fky.ltd/zwzp_03.webp"
    ],
    author: "@尼克lin",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】日间户外，光线充足的场景\n【场景推荐】街拍、建筑、人文、日常记录\n【拍摄要点】模拟理光GR正片风格，色彩鲜艳对比度高，适合追求胶片质感的拍摄场景"
    }
  },
  {
    id: "realme-2",
    name: "理光负片",
    coverPath: "https://cdn.fky.ltd/lgfp_01.webp",
    galleryImages: [
      "https://cdn.fky.ltd/lgfp_02.webp",
      "https://cdn.fky.ltd/lgfp_03.webp"
    ],
    author: "@尼克lin",
    tags: ["Auto"],
    description: {
      title: "Shooting Tips",
      content: "【环境建议】日间户外，光线充足的场景\n【场景推荐】街拍、建筑、人文、日常记录\n【拍摄要点】模拟理光GR负片风格，色彩自然略带胶片感，适合追求真实质感的拍摄场景"
    }
  }
];

export const presets = oppoPresetsData;
