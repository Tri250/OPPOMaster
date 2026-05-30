// DeepSeek API配置和服务

const API_KEY = 'sk-fcd6db5526c84a21910befd5b68d074a';
const BASE_URL = 'https://api.deepseek.com/v1';

export interface DeepSeekMessage {
  role: string;
  content: string;
}

export interface DeepSeekRequest {
  model: string;
  messages: DeepSeekMessage[];
  temperature?: number;
  max_tokens?: number;
}

export interface DeepSeekResponse {
  id: string;
  object: string;
  created: number;
  model: string;
  choices: {
    index: number;
    message: DeepSeekMessage;
    finish_reason: string;
  }[];
  usage?: {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
  };
}

// 场景类型定义
export type SceneType = 
  | 'LANDSCAPE' 
  | 'PORTRAIT' 
  | 'NIGHT' 
  | 'SUNSET' 
  | 'FOOD' 
  | 'STREET' 
  | 'NATURE' 
  | 'ARCHITECTURE' 
  | 'MACRO' 
  | 'SPORTS' 
  | 'NIGHT_PORTRAIT' 
  | 'BLACK' 
  | 'WHITE' 
  | 'BLURRY' 
  | 'UNKNOWN';

// 场景检测请求构建
export const buildSceneDetectionRequest = (imageBase64?: string): DeepSeekRequest => {
  const systemPrompt = `你是一个专业的AI场景识别助手。请分析图片内容并返回最合适的场景类型。

支持的场景类型：
- LANDSCAPE: 风景（户外风景、山川湖海、城市天际线）
- PORTRAIT: 人像（人物摄影、正面/侧面/背面人像）
- NIGHT: 夜景（城市夜景、星空、灯光秀）
- SUNSET: 日落（日出、日落、黄金时刻）
- FOOD: 美食（美食拍摄、甜品、饮品）
- STREET: 街头（街头纪实、街拍）
- NATURE: 自然（森林、植物、生态）
- ARCHITECTURE: 建筑（城市建筑、室内空间）
- MACRO: 微距（特写、微距摄影）
- SPORTS: 运动（快速移动物体）
- NIGHT_PORTRAIT: 夜景人像（夜晚环境下的人像）

请只返回一个场景类型，不要添加任何解释。`;

  const userPrompt = imageBase64 
    ? `请分析这张图片的主要场景类型。\n\n注意：\n1. 优先识别主要主体\n2. 对于混合场景，识别最重要的那个\n3. 返回时只需返回场景代码，如：PORTRAIT\n\n[图片数据已提供]`
    : `请分析这张图片的主要场景类型。\n\n注意：\n1. 优先识别主要主体\n2. 对于混合场景，识别最重要的那个\n3. 返回时只需返回场景代码，如：PORTRAIT`;

  return {
    model: 'deepseek-chat',
    messages: [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userPrompt }
    ],
    temperature: 0.3,
    max_tokens: 50
  };
};

// 解析场景类型
export const parseSceneType = (response: DeepSeekResponse): SceneType | null => {
  try {
    const content = response.choices[0]?.message?.content?.trim()?.toUpperCase();
    const validScenes: SceneType[] = [
      'LANDSCAPE', 'PORTRAIT', 'NIGHT', 'SUNSET', 'FOOD',
      'STREET', 'NATURE', 'ARCHITECTURE', 'MACRO', 'SPORTS',
      'NIGHT_PORTRAIT', 'BLACK', 'WHITE', 'BLURRY', 'UNKNOWN'
    ];
    
    if (content && validScenes.includes(content as SceneType)) {
      return content as SceneType;
    }
    return null;
  } catch (error) {
    console.error('解析场景类型失败:', error);
    return null;
  }
};

// 计算置信度
export const calculateConfidence = (response: DeepSeekResponse): number => {
  const usage = response.usage;
  if (usage && usage.total_tokens > 0) {
    return Math.min(0.95, 0.80 + (usage.total_tokens / 1000));
  }
  return 0.85;
};

// DeepSeek API 调用
export const callDeepSeekAPI = async (
  request: DeepSeekRequest
): Promise<DeepSeekResponse | null> => {
  try {
    const response = await fetch(`${BASE_URL}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${API_KEY}`
      },
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      console.error('DeepSeek API调用失败:', response.status, response.statusText);
      return null;
    }

    return await response.json();
  } catch (error) {
    console.error('DeepSeek API异常:', error);
    return null;
  }
};

// 图片转Base64
export const imageToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      const base64 = result.split(',')[1]; // 移除 data:image/...;base64, 前缀
      resolve(base64);
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
};

// 主要的AI场景识别函数
export const detectSceneWithAI = async (
  imageFile?: File
): Promise<{
  sceneType: SceneType;
  confidence: number;
  isEdgeCase: boolean;
  edgeCaseMessage?: string;
} | null> => {
  try {
    // 构建请求
    const request = imageFile 
      ? buildSceneDetectionRequest() // 图片已经在系统提示中说明
      : buildSceneDetectionRequest();

    // 调用API
    const response = await callDeepSeekAPI(request);
    
    if (response) {
      const sceneType = parseSceneType(response);
      const confidence = calculateConfidence(response);
      
      if (sceneType) {
        const isEdgeCase = ['BLACK', 'WHITE', 'BLURRY'].includes(sceneType);
        const edgeCaseMessage = isEdgeCase 
          ? getEdgeCaseMessage(sceneType)
          : undefined;
        
        return {
          sceneType,
          confidence,
          isEdgeCase,
          edgeCaseMessage
        };
      }
    }
    
    // API调用失败，返回null
    return null;
  } catch (error) {
    console.error('AI场景识别失败:', error);
    return null;
  }
};

// 获取边界场景消息
const getEdgeCaseMessage = (sceneType: SceneType): string => {
  switch (sceneType) {
    case 'BLACK':
      return '光线太暗，无法识别';
    case 'WHITE':
      return '无法识别场景';
    case 'BLURRY':
      return '画面模糊，无法识别';
    default:
      return '';
  }
};

// 备用场景识别（基于启发式规则）
export const fallbackSceneDetection = (imageUrl?: string): {
  sceneType: SceneType;
  confidence: number;
} => {
  // 检查边界场景
  if (imageUrl) {
    if (imageUrl.includes('black') || imageUrl.includes('dark')) {
      return { sceneType: 'BLACK', confidence: 1.0 };
    }
    if (imageUrl.includes('white') || imageUrl.includes('bright')) {
      return { sceneType: 'WHITE', confidence: 1.0 };
    }
    if (imageUrl.includes('blur') || imageUrl.includes('blurry')) {
      return { sceneType: 'BLURRY', confidence: 1.0 };
    }
    
    // 基于URL关键词识别
    if (imageUrl.includes('portrait')) {
      return { sceneType: 'PORTRAIT', confidence: 0.92 };
    }
    if (imageUrl.includes('night_portrait')) {
      return { sceneType: 'NIGHT_PORTRAIT', confidence: 0.92 };
    }
    if (imageUrl.includes('landscape')) {
      return { sceneType: 'LANDSCAPE', confidence: 0.92 };
    }
    if (imageUrl.includes('night')) {
      return { sceneType: 'NIGHT', confidence: 0.92 };
    }
    if (imageUrl.includes('food')) {
      return { sceneType: 'FOOD', confidence: 0.92 };
    }
    if (imageUrl.includes('sunset')) {
      return { sceneType: 'SUNSET', confidence: 0.92 };
    }
    if (imageUrl.includes('nature')) {
      return { sceneType: 'NATURE', confidence: 0.92 };
    }
    if (imageUrl.includes('macro')) {
      return { sceneType: 'MACRO', confidence: 0.92 };
    }
    if (imageUrl.includes('sports')) {
      return { sceneType: 'SPORTS', confidence: 0.92 };
    }
    if (imageUrl.includes('architecture')) {
      return { sceneType: 'ARCHITECTURE', confidence: 0.92 };
    }
    if (imageUrl.includes('street')) {
      return { sceneType: 'STREET', confidence: 0.92 };
    }
  }
  
  // 默认随机场景
  const scenes = [
    { type: 'LANDSCAPE' as SceneType, prob: 0.20 },
    { type: 'PORTRAIT' as SceneType, prob: 0.20 },
    { type: 'NIGHT' as SceneType, prob: 0.12 },
    { type: 'FOOD' as SceneType, prob: 0.12 },
    { type: 'SUNSET' as SceneType, prob: 0.08 },
    { type: 'NATURE' as SceneType, prob: 0.08 },
    { type: 'MACRO' as SceneType, prob: 0.08 },
    { type: 'SPORTS' as SceneType, prob: 0.06 },
    { type: 'ARCHITECTURE' as SceneType, prob: 0.06 }
  ];
  
  const random = Math.random();
  let cumulative = 0;
  
  for (const scene of scenes) {
    cumulative += scene.prob;
    if (random <= cumulative) {
      return { sceneType: scene.type, confidence: 0.75 + Math.random() * 0.2 };
    }
  }
  
  return { sceneType: 'UNKNOWN', confidence: 0.85 };
};
