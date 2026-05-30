// AI 场景识别服务 - 使用 Web Speech API + 启发式识别
// 注意：Web端无法使用Google ML Kit，改为使用Web Speech API进行语音搜索 + 智能降级策略

import type { Preset } from '../data/mockPresets';
import { mockPresets } from '../data/mockPresets';

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
  | 'VINTAGE'
  | 'CINEMATIC'
  | 'BLACK_WHITE'
  | 'BLACK' 
  | 'WHITE' 
  | 'BLURRY' 
  | 'UNKNOWN';

// 场景分类映射
const sceneKeywordMap: Record<SceneType, string[]> = {
  LANDSCAPE: ['风景', '自然', '森林', '海边', '风光', 'landscape', 'nature', 'sky', 'mountain'],
  PORTRAIT: ['人像', '柔焦', '童话', '梦幻', '黑柔', 'portrait', 'person', 'face'],
  NIGHT_PORTRAIT: ['夜景人像', '夜晚人像', 'night_portrait'],
  NIGHT: ['夜景', '夜色', '霓虹', '蓝调', '城市夜景', 'night', 'dark'],
  SUNSET: ['日落', '橙调', '佛罗伦萨', '金色时刻', '夕阳暖调', '暖调', 'sunset', 'sunrise'],
  FOOD: ['美食', '清新', '食欲', '诱人', 'food', 'meal'],
  STREET: ['街头', '纪实', '黑白', '街拍', '故事', 'street', 'urban'],
  NATURE: ['自然', '森林', '清新', '微距', '植物', '生态', 'nature', 'plant'],
  ARCHITECTURE: ['建筑', '城市', '纪实', '空间', '结构', 'architecture', 'building'],
  MACRO: ['微距', '特写', '细节', '微观', 'macro', 'close-up'],
  SPORTS: ['运动', '快速', '动感', '抓拍', 'sports', 'football', 'basketball'],
  VINTAGE: ['复古', '胶片', '经典', 'vintage', 'retro'],
  CINEMATIC: ['电影', '宽幅', '质感', 'cinematic', 'movie'],
  BLACK_WHITE: ['黑白', '单色', 'black_white', 'bw'],
  BLACK: ['black', 'dark', 'darkness'],
  WHITE: ['white', 'snow', 'bright'],
  BLURRY: ['blur', 'blurry', 'focus'],
  UNKNOWN: []
};

// 场景优先级
const scenePriority: Record<SceneType, number> = {
  PORTRAIT: 10,
  NIGHT_PORTRAIT: 9,
  FOOD: 8,
  SUNSET: 7,
  LANDSCAPE: 6,
  NATURE: 5,
  ARCHITECTURE: 4,
  NIGHT: 3,
  MACRO: 2,
  SPORTS: 1,
  STREET: 0,
  VINTAGE: 0,
  CINEMATIC: 0,
  BLACK_WHITE: 0,
  BLACK: -100,
  WHITE: -100,
  BLURRY: -100,
  UNKNOWN: -1000
};

// 场景识别结果
export interface SceneDetectionResult {
  sceneType: SceneType;
  confidence: number;
  isEdgeCase: boolean;
  edgeCaseMessage?: string;
  secondaryScene?: SceneType;
}

// 智能场景识别（基于关键词分析）
export const detectSceneByKeywords = (
  searchText: string,
  presetName?: string
): SceneDetectionResult => {
  const normalizedText = `${searchText} ${presetName || ''}`.toLowerCase();
  
  const scores: Record<SceneType, number> = {} as Record<SceneType, number>;
  
  // 计算每个场景的匹配分数
  for (const [scene, keywords] of Object.entries(sceneKeywordMap)) {
    let score = 0;
    for (const keyword of keywords) {
      if (normalizedText.includes(keyword.toLowerCase())) {
        score += 1;
      }
    }
    if (score > 0) {
      scores[scene as SceneType] = score;
    }
  }
  
  // 检测边界场景
  if (scores['BLACK']) {
    return {
      sceneType: 'BLACK',
      confidence: 1.0,
      isEdgeCase: true,
      edgeCaseMessage: '光线太暗，建议增加曝光或使用闪光灯'
    };
  }
  
  if (scores['WHITE']) {
    return {
      sceneType: 'WHITE',
      confidence: 1.0,
      isEdgeCase: true,
      edgeCaseMessage: '画面过亮，可能过曝'
    };
  }
  
  if (scores['BLURRY']) {
    return {
      sceneType: 'BLURRY',
      confidence: 1.0,
      isEdgeCase: true,
      edgeCaseMessage: '画面模糊，建议稳定手机'
    };
  }
  
  // 排序并返回最高分的场景
  const sortedScenes = Object.entries(scores)
    .sort((a, b) => b[1] - a[1])
    .filter(([scene]) => !['BLACK', 'WHITE', 'BLURRY'].includes(scene));
  
  if (sortedScenes.length > 0) {
    const primaryScene = sortedScenes[0][0] as SceneType;
    const secondaryScene = sortedScenes[1]?.[0] as SceneType | undefined;
    
    return {
      sceneType: primaryScene,
      confidence: Math.min(0.95, 0.8 + sortedScenes[0][1] * 0.1),
      isEdgeCase: false,
      secondaryScene
    };
  }
  
  return {
    sceneType: 'UNKNOWN',
    confidence: 0.85,
    isEdgeCase: false
  };
};

// 基于语音输入的场景识别
export const detectSceneByVoice = async (): Promise<SceneDetectionResult> => {
  return new Promise((resolve) => {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      // 浏览器不支持语音识别，使用默认结果
      resolve({
        sceneType: 'LANDSCAPE',
        confidence: 0.75,
        isEdgeCase: false
      });
      return;
    }
    
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    
    recognition.lang = 'zh-CN';
    recognition.continuous = false;
    recognition.interimResults = false;
    
    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      resolve(detectSceneByKeywords(transcript));
    };
    
    recognition.onerror = () => {
      resolve({
        sceneType: 'UNKNOWN',
        confidence: 0.85,
        isEdgeCase: false
      });
    };
    
    recognition.onend = () => {
      resolve({
        sceneType: 'UNKNOWN',
        confidence: 0.85,
        isEdgeCase: false
      });
    };
    
    recognition.start();
  });
};

// 获取推荐预设
export const getRecommendedPresets = (
  detectionResult: SceneDetectionResult,
  allPresets: Preset[]
): Preset[] => {
  const { sceneType, secondaryScene } = detectionResult;
  
  if (detectionResult.isEdgeCase) {
    return [];
  }
  
  const sceneKeywords = getSceneKeywords(sceneType);
  const secondaryKeywords = secondaryScene ? getSceneKeywords(secondaryScene) : [];
  
  const scoredPresets = allPresets.map(preset => {
    let score = 0;
    
    // 名称匹配
    score += sceneKeywords.filter(k => preset.name.includes(k)).length * 3;
    score += secondaryKeywords.filter(k => preset.name.includes(k)).length * 1;
    
    // 分类匹配
    score += sceneKeywords.filter(k => preset.category?.includes(k)).length * 2;
    
    // 标签匹配
    if (preset.tags) {
      score += preset.tags.filter(t => sceneKeywords.some(k => t.includes(k))).length * 2;
    }
    
    // 哈苏加分
    if (preset.cameraParams?.hncs) {
      score += 1;
    }
    
    return { preset, score };
  });
  
  const sorted = scoredPresets
    .filter(s => s.score > 0)
    .sort((a, b) => b.score - a.score);
  
  return sorted.slice(0, 4).map(s => s.preset);
};

// 获取场景关键词
const getSceneKeywords = (scene: SceneType): string[] => {
  return sceneKeywordMap[scene] || [];
};

// 主要的AI场景识别函数（整合所有策略）
export const detectScene = async (
  input?: string | File
): Promise<SceneDetectionResult> => {
  try {
    // 1. 如果有文本输入，使用关键词分析
    if (typeof input === 'string' && input.trim()) {
      return detectSceneByKeywords(input);
    }
    
    // 2. 如果是文件，使用启发式分析
    if (input instanceof File) {
      // Web端无法直接分析图片，使用文件名或备用逻辑
      const name = input.name.toLowerCase();
      return detectSceneByKeywords(name);
    }
    
    // 3. 默认返回未知
    return {
      sceneType: 'UNKNOWN',
      confidence: 0.85,
      isEdgeCase: false
    };
  } catch (error) {
    console.error('场景识别失败:', error);
    return {
      sceneType: 'UNKNOWN',
      confidence: 0.85,
      isEdgeCase: false
    };
  }
};

// 导出常量供其他模块使用
export const SCENE_TYPES = Object.keys(sceneKeywordMap) as SceneType[];
export const SCENE_LABELS: Record<SceneType, string> = {
  LANDSCAPE: '风景',
  PORTRAIT: '人像',
  NIGHT_PORTRAIT: '夜景人像',
  NIGHT: '夜景',
  SUNSET: '日落',
  FOOD: '美食',
  STREET: '街拍',
  NATURE: '自然',
  ARCHITECTURE: '建筑',
  MACRO: '微距',
  SPORTS: '运动',
  VINTAGE: '复古',
  CINEMATIC: '电影感',
  BLACK_WHITE: '黑白',
  BLACK: '过暗',
  WHITE: '过亮',
  BLURRY: '模糊',
  UNKNOWN: '未知'
};
