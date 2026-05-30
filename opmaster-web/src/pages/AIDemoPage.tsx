import { motion, AnimatePresence } from 'framer-motion';
import { Upload, Sparkles, Check, Image as ImageIcon, Loader, Camera, Palette, Sun, Moon, XCircle } from 'lucide-react';
import { useState, useRef, useCallback, useMemo } from 'react';
import { mockPresets } from '../data/mockPresets';

const sampleImages = [
  { id: 1, label: '人像', seed: 'portrait' },
  { id: 2, label: '风景', seed: 'landscape' },
  { id: 3, label: '夜景', seed: 'night' },
  { id: 4, label: '美食', seed: 'food' }
];

// 场景类型定义，与 Android 端保持一致
type SceneType = 'LANDSCAPE' | 'PORTRAIT' | 'NIGHT' | 'SUNSET' | 'FOOD' | 'STREET' | 'NATURE' | 'ARCHITECTURE' | 'MACRO' | 'UNKNOWN';

interface SceneResult {
  label: string;
  confidence: number;
  color: string;
  type: SceneType;
}

interface RecommendedPreset {
  id: string;
  name: string;
  coverPath: string;
  matchScore: number;
  tags: string[];
  isHNCS?: boolean;
}

// 场景标签映射
const sceneLabels: Record<SceneType, string> = {
  LANDSCAPE: '风景',
  PORTRAIT: '人像',
  NIGHT: '夜景',
  SUNSET: '日落',
  FOOD: '美食',
  STREET: '街拍',
  NATURE: '自然',
  ARCHITECTURE: '建筑',
  MACRO: '微距',
  UNKNOWN: '未知'
};

// 场景描述
const sceneDescriptions: Record<SceneType, string> = {
  LANDSCAPE: '适合户外风景、山川湖海',
  PORTRAIT: '适合人物摄影',
  NIGHT: '适合夜间城市、星空',
  SUNSET: '适合日落、金色时刻',
  FOOD: '适合美食拍摄',
  STREET: '适合街头纪实',
  NATURE: '适合自然生态、植物',
  ARCHITECTURE: '适合城市建筑、室内空间',
  MACRO: '适合特写、微距摄影',
  UNKNOWN: '自动识别场景'
};

// 场景颜色映射
const sceneColors: Record<SceneType, string> = {
  LANDSCAPE: 'from-green-500 to-emerald-500',
  PORTRAIT: 'from-pink-500 to-rose-500',
  NIGHT: 'from-indigo-500 to-purple-500',
  SUNSET: 'from-yellow-500 to-orange-500',
  FOOD: 'from-orange-500 to-red-500',
  STREET: 'from-gray-500 to-slate-500',
  NATURE: 'from-teal-500 to-cyan-500',
  ARCHITECTURE: 'from-blue-500 to-indigo-500',
  MACRO: 'from-violet-500 to-purple-500',
  UNKNOWN: 'from-gray-400 to-gray-500'
};

// 场景关键词
const sceneKeywords: Record<SceneType, string[]> = {
  LANDSCAPE: ['风景', '自然', '森林', '海边', '风光', '蓝调', '理光绿', '清新'],
  PORTRAIT: ['人像', '柔焦', '童话', '梦幻', '黑柔', '经典'],
  NIGHT: ['夜景', '夜色', '霓虹', '蓝调', '城市夜景', '赛博'],
  SUNSET: ['日落', '橙调', '佛罗伦萨', '金色时刻', '夕阳暖调', '暖调'],
  FOOD: ['美食', '清新', '食欲', '诱人'],
  STREET: ['街头', '纪实', '黑白', '街拍', '故事'],
  NATURE: ['自然', '森林', '清新', '微距', '植物', '生态'],
  ARCHITECTURE: ['建筑', '城市', '纪实', '空间', '结构'],
  MACRO: ['微距', '特写', '细节', '微观'],
  UNKNOWN: []
};

export default function AIDemoPage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisComplete, setAnalysisComplete] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);
  const [detectedScenes, setDetectedScenes] = useState<SceneResult[]>([]);
  const [recommendedPresets, setRecommendedPresets] = useState<RecommendedPreset[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 根据图片 URL 模拟场景识别 - 与 Android 端保持一致的逻辑
  const detectScene = useCallback((imageUrl: string): SceneResult[] => {
    let primaryScene: SceneType = 'UNKNOWN';
    let primaryConfidence = 0.85;
    let secondaryScene: SceneType = 'UNKNOWN';
    let secondaryConfidence = 0.65;
    
    // 基于 URL 关键词的启发式规则
    if (imageUrl.includes('portrait')) {
      primaryScene = 'PORTRAIT';
      secondaryScene = 'NATURE';
      primaryConfidence = 0.92;
      secondaryConfidence = 0.58;
    } else if (imageUrl.includes('landscape')) {
      primaryScene = 'LANDSCAPE';
      secondaryScene = 'NATURE';
      primaryConfidence = 0.90;
      secondaryConfidence = 0.62;
    } else if (imageUrl.includes('night')) {
      primaryScene = 'NIGHT';
      secondaryScene = 'ARCHITECTURE';
      primaryConfidence = 0.88;
      secondaryConfidence = 0.55;
    } else if (imageUrl.includes('food')) {
      primaryScene = 'FOOD';
      secondaryScene = 'MACRO';
      primaryConfidence = 0.91;
      secondaryConfidence = 0.60;
    } else {
      // 基于预定义概率的场景识别
      const scenes = [
        { type: 'LANDSCAPE' as SceneType, prob: 0.25 },
        { type: 'PORTRAIT' as SceneType, prob: 0.25 },
        { type: 'NIGHT' as SceneType, prob: 0.15 },
        { type: 'FOOD' as SceneType, prob: 0.15 },
        { type: 'SUNSET' as SceneType, prob: 0.10 },
        { type: 'NATURE' as SceneType, prob: 0.10 }
      ];
      
      const random = Math.random();
      let cumulative = 0;
      for (const scene of scenes) {
        cumulative += scene.prob;
        if (random <= cumulative) {
          primaryScene = scene.type;
          primaryConfidence = 0.75 + Math.random() * 0.2;
          break;
        }
      }
      
      // 选择次要场景
      const remainingScenes = scenes.filter(s => s.type !== primaryScene);
      cumulative = 0;
      const remainingProb = remainingScenes.reduce((sum, s) => sum + s.prob, 0);
      const adjustedRandom = Math.random() * remainingProb;
      
      for (const scene of remainingScenes) {
        cumulative += scene.prob;
        if (adjustedRandom <= cumulative) {
          secondaryScene = scene.type;
          secondaryConfidence = 0.55 + Math.random() * 0.15;
          break;
        }
      }
    }
    
    return [
      { 
        label: sceneLabels[primaryScene], 
        confidence: primaryConfidence, 
        color: sceneColors[primaryScene],
        type: primaryScene 
      },
      { 
        label: sceneLabels[secondaryScene], 
        confidence: secondaryConfidence, 
        color: sceneColors[secondaryScene],
        type: secondaryScene 
      }
    ];
  }, []);

  // 根据场景推荐预设 - 与 Android 端保持一致的逻辑
  const getRecommendedPresets = useCallback((scene: SceneType): RecommendedPreset[] => {
    const keywords = sceneKeywords[scene] || [];
    
    // 带评分的匹配算法
    const scoredPresets = mockPresets.map(preset => {
      let score = 0;
      
      // 名称匹配评分
      score += keywords.reduce((sum, keyword) => {
        return sum + (preset.name.includes(keyword) ? 3 : 0);
      }, 0);
      
      // 标签匹配评分
      score += keywords.reduce((sum, keyword) => {
        return sum + (preset.tags?.some(tag => tag.includes(keyword)) ? 2 : 0);
      }, 0);
      
      // 相机参数匹配评分
      if (preset.cameraParams?.filter) {
        score += keywords.some(keyword => preset.cameraParams!.filter.includes(keyword)) ? 1.5 : 0;
      }
      
      // 特殊场景的相机参数匹配
      if (scene === 'PORTRAIT' && preset.cameraParams?.portrait_mode) {
        score += 2;
      }
      if (scene === 'NIGHT' && preset.cameraParams?.night_mode) {
        score += 2;
      }
      
      // HNCS 认证加分
      if (preset.cameraParams?.hncs) {
        score += 1;
      }
      
      return { preset, score };
    }).sort((a, b) => b.score - a.score);
    
    // 提取高评分预设
    const highScorePresets = scoredPresets.filter(item => item.score > 0).map(item => item.preset);
    
    let resultPresets: typeof mockPresets;
    
    if (highScorePresets.length >= 4) {
      resultPresets = highScorePresets.slice(0, 4);
    } else if (highScorePresets.length > 0) {
      // 补充一些相关场景的预设
      const fallbackPresets = getFallbackPresets(scene, mockPresets.filter(p => !highScorePresets.includes(p)));
      resultPresets = [...highScorePresets, ...fallbackPresets].slice(0, 4);
    } else {
      // 完全没有匹配，使用场景特定的回退策略
      resultPresets = getFallbackPresets(scene, mockPresets).slice(0, 4);
    }
    
    // 转换格式
    return resultPresets.map((preset, index) => ({
      id: preset.id,
      name: preset.name,
      coverPath: preset.coverPath,
      matchScore: Math.max(70, 95 - index * 10 + Math.random() * 10),
      tags: preset.tags || [],
      isHNCS: preset.cameraParams?.hncs
    }));
  }, []);
  
  // 回退预设选择
  const getFallbackPresets = (scene: SceneType, presets: typeof mockPresets): typeof mockPresets => {
    let fallback: typeof mockPresets;
    
    switch (scene) {
      case 'LANDSCAPE':
      case 'NATURE':
        fallback = presets.filter(p => 
          p.name.includes('风景') || p.name.includes('自然') || p.name.includes('清新')
        );
        break;
      case 'PORTRAIT':
        fallback = presets.filter(p => 
          p.name.includes('人像') || p.cameraParams?.portrait_mode
        );
        break;
      case 'NIGHT':
        fallback = presets.filter(p => 
          p.name.includes('夜景') || p.cameraParams?.night_mode
        );
        break;
      case 'FOOD':
        fallback = presets.filter(p => 
          p.name.includes('美食') || p.name.includes('诱人')
        );
        break;
      case 'SUNSET':
        fallback = presets.filter(p => 
          p.name.includes('日落') || p.name.includes('暖调')
        );
        break;
      case 'STREET':
        fallback = presets.filter(p => 
          p.name.includes('街拍') || p.name.includes('纪实')
        );
        break;
      case 'ARCHITECTURE':
        fallback = presets.filter(p => 
          p.name.includes('建筑')
        );
        break;
      case 'MACRO':
        fallback = presets.filter(p => 
          p.name.includes('微距')
        );
        break;
      default:
        fallback = presets;
    }
    
    return fallback.length > 0 ? fallback : [...presets].sort(() => Math.random() - 0.5);
  };

  const handleImageSelect = useCallback((imageUrl: string) => {
    setSelectedImage(imageUrl);
    setAnalysisComplete(false);
    setDetectedScenes([]);
    setRecommendedPresets([]);
  }, []);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setSelectedImage(event.target?.result as string);
        setAnalysisComplete(false);
        setDetectedScenes([]);
        setRecommendedPresets([]);
      };
      reader.readAsDataURL(file);
    }
  }, []);

  const handleUploadClick = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setSelectedImage(event.target?.result as string);
        setAnalysisComplete(false);
        setDetectedScenes([]);
        setRecommendedPresets([]);
      };
      reader.readAsDataURL(file);
    }
  }, []);

  const handleAnalyze = useCallback(() => {
    if (!selectedImage) return;
    
    setIsAnalyzing(true);
    setAnalysisComplete(false);
    
    // 模拟分析过程
    setTimeout(() => {
      const scenes = detectScene(selectedImage);
      setDetectedScenes(scenes);
      
      // 使用主要场景推荐预设
      if (scenes.length > 0) {
        const presets = getRecommendedPresets(scenes[0].type);
        setRecommendedPresets(presets);
      }
      
      setIsAnalyzing(false);
      setAnalysisComplete(true);
    }, 1600); // 与 Android 端保持类似的延迟
  }, [selectedImage, detectScene, getRecommendedPresets]);
  
  const handleClear = useCallback(() => {
    setSelectedImage(null);
    setAnalysisComplete(false);
    setDetectedScenes([]);
    setRecommendedPresets([]);
  }, []);

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8 bg-gradient-to-br from-slate-900 via-slate-900 to-slate-800">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-amber-500 to-orange-500 rounded-2xl mb-6 shadow-lg shadow-amber-500/20">
            <Sparkles className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent">
            AI 智能场景识别
          </h1>
          <p className="text-lg text-slate-400 max-w-2xl mx-auto">
            上传您的照片，体验 AI 智能识别场景并推荐最佳哈苏大师影像预设
          </p>
        </motion.div>

        {/* Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Upload Section */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="space-y-6"
          >
            <div className="bg-slate-800/50 backdrop-blur-sm rounded-3xl p-8 border border-slate-700/50">
              <h2 className="text-xl font-bold mb-6 text-slate-100 flex items-center gap-2">
                <ImageIcon className="w-5 h-5 text-amber-400" />
                选择照片
              </h2>
              
              {/* Hidden File Input */}
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileSelect}
                accept="image/jpeg,image/png,image/jpg"
                className="hidden"
              />
              
              {/* Upload Area */}
              <AnimatePresence mode="wait">
                {selectedImage ? (
                  <motion.div
                    key="preview"
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    className="relative aspect-video rounded-2xl overflow-hidden group"
                  >
                    <img
                      src={selectedImage}
                      alt="Selected"
                      className="w-full h-full object-cover"
                    />
                    {/* Gradient Overlay */}
                    <div className="absolute inset-0 bg-gradient-to-t from-slate-900/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                    {/* Replace Button */}
                    <button
                      onClick={handleClear}
                      className="absolute top-4 right-4 p-2 bg-black/50 hover:bg-black/70 rounded-full text-white transition-colors"
                    >
                      <XCircle className="w-6 h-6" />
                    </button>
                    <div className="absolute bottom-4 left-4 right-4 text-white opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                      <p className="font-medium">点击右上角更换照片</p>
                    </div>
                  </motion.div>
                ) : (
                  <motion.div
                    key="upload"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={handleUploadClick}
                    onDragOver={handleDragOver}
                    onDragLeave={handleDragLeave}
                    onDrop={handleDrop}
                    className={`border-2 border-dashed rounded-2xl p-12 text-center transition-all cursor-pointer ${
                      isDragOver 
                        ? 'border-amber-500 bg-amber-500/10' 
                        : 'border-slate-600 hover:border-amber-500 hover:bg-amber-500/5'
                    }`}
                  >
                    <Upload className={`w-16 h-16 mx-auto mb-4 transition-colors ${
                      isDragOver ? 'text-amber-500' : 'text-slate-500 group-hover:text-amber-500'
                    }`} />
                    <p className="text-slate-400 mb-2">点击上传照片，或拖拽到此处</p>
                    <p className="text-sm text-slate-500">支持 JPG、PNG 格式</p>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Sample Images */}
              <div className="mt-6">
                <p className="text-sm text-slate-400 mb-3">或选择示例照片</p>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {sampleImages.map((img) => (
                    <motion.button
                      key={img.id}
                      whileHover={{ scale: 1.05, y: -2 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => handleImageSelect(`https://picsum.photos/seed/${img.seed}/400/300`)}
                      className={`relative aspect-square rounded-xl overflow-hidden transition-all ${
                        selectedImage?.includes(img.seed)
                          ? 'ring-2 ring-amber-500 ring-offset-2 ring-offset-slate-900'
                          : 'hover:ring-2 hover:ring-slate-600'
                      }`}
                    >
                      <img
                        src={`https://picsum.photos/seed/${img.seed}/200/200`}
                        alt={img.label}
                        className="w-full h-full object-cover"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/70 to-transparent flex items-end p-2">
                        <span className="text-xs text-white font-medium">{img.label}</span>
                      </div>
                    </motion.button>
                  ))}
                </div>
              </div>

              {/* Analyze Button */}
              <motion.button
                whileHover={{ scale: selectedImage ? 1.02 : 1 }}
                whileTap={{ scale: selectedImage ? 0.98 : 1 }}
                onClick={handleAnalyze}
                disabled={!selectedImage || isAnalyzing}
                className={`w-full mt-6 py-4 rounded-2xl font-semibold text-lg transition-all flex items-center justify-center gap-3 ${
                  selectedImage
                    ? 'bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-slate-900 shadow-lg shadow-amber-500/20'
                    : 'bg-slate-700 text-slate-500 cursor-not-allowed'
                }`}
              >
                {isAnalyzing ? (
                  <>
                    <Loader className="w-5 h-5 animate-spin" />
                    <span>AI 正在分析中...</span>
                  </>
                ) : (
                  <>
                    <Sparkles className="w-5 h-5" />
                    <span>开始 AI 场景识别</span>
                  </>
                )}
              </motion.button>
            </div>
          </motion.div>

          {/* Results Section */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="space-y-6"
          >
            {/* Skeleton Loading State */}
            <AnimatePresence>
              {isAnalyzing && (
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -20 }}
                  className="bg-slate-800/50 backdrop-blur-sm rounded-3xl p-6 border border-slate-700/50"
                >
                  <div className="flex items-center justify-between mb-6">
                    <div className="h-6 w-32 bg-slate-700 rounded animate-pulse" />
                    <div className="flex items-center gap-2">
                      <div className="h-5 w-5 bg-amber-500/30 rounded-full animate-pulse" />
                      <div className="h-4 w-20 bg-slate-700 rounded animate-pulse" />
                    </div>
                  </div>
                  
                  {/* Skeleton Scenes */}
                  <div className="space-y-4 mb-6">
                    {[1, 2].map(i => (
                      <div key={i} className="space-y-2">
                        <div className="flex justify-between">
                          <div className="h-4 w-16 bg-slate-700 rounded animate-pulse" />
                          <div className="h-4 w-12 bg-slate-700 rounded animate-pulse" />
                        </div>
                        <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                          <div className="h-full w-2/3 bg-slate-600 rounded-full animate-pulse" />
                        </div>
                      </div>
                    ))}
                  </div>
                  
                  {/* Skeleton Presets */}
                  <div className="h-6 w-40 bg-slate-700 rounded animate-pulse mb-3" />
                  <div className="space-y-3">
                    {[1, 2, 3, 4].map(i => (
                      <div key={i} className="flex items-center gap-3 p-3 bg-slate-700/50 rounded-xl">
                        <div className="w-12 h-12 bg-slate-600 rounded-lg animate-pulse" />
                        <div className="flex-1 space-y-2">
                          <div className="h-4 w-2/3 bg-slate-600 rounded animate-pulse" />
                          <div className="h-3 w-1/2 bg-slate-600 rounded animate-pulse" />
                        </div>
                        <div className="h-9 w-20 bg-slate-600 rounded-lg animate-pulse" />
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Analysis Results */}
            <AnimatePresence>
              {analysisComplete && !isAnalyzing && (
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="bg-slate-800/50 backdrop-blur-sm rounded-3xl p-6 border border-slate-700/50 space-y-6"
                >
                  <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-slate-100">识别结果</h2>
                    <div className="flex items-center gap-2 text-emerald-400">
                      <Check className="w-5 h-5" />
                      <span className="text-sm font-medium">分析完成</span>
                    </div>
                  </div>

                  {/* Scene Labels */}
                  {detectedScenes.length > 0 && (
                    <div className="space-y-4">
                      {detectedScenes.map((result, index) => (
                        <motion.div
                          key={result.label}
                          initial={{ opacity: 0, x: -20 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: index * 0.15 }}
                          className="space-y-2"
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-medium text-slate-200">{result.label}</span>
                            <span className="text-slate-400">
                              {Math.round(result.confidence * 100)}%
                            </span>
                          </div>
                          <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                            <motion.div
                              initial={{ width: 0 }}
                              animate={{ width: `${result.confidence * 100}%` }}
                              transition={{ delay: 0.3 + index * 0.15, duration: 0.6, ease: "easeOut" }}
                              className={`h-full bg-gradient-to-r ${result.color} rounded-full`}
                            />
                          </div>
                          <p className="text-sm text-slate-500">{sceneDescriptions[result.type]}</p>
                        </motion.div>
                      ))}
                    </div>
                  )}

                  {/* Recommended Presets */}
                  {recommendedPresets.length > 0 && (
                    <div>
                      <h3 className="text-sm font-bold text-slate-400 mb-3 flex items-center gap-2">
                        <Palette className="w-4 h-4" />
                        推荐哈苏大师预设
                      </h3>
                      <div className="space-y-3">
                        {recommendedPresets.map((preset, index) => (
                          <motion.div
                            key={preset.id}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: 0.6 + index * 0.1 }}
                            className="flex items-center gap-3 p-3 bg-slate-700/30 rounded-xl hover:bg-slate-700/50 transition-colors cursor-pointer"
                          >
                            {/* Preset Image */}
                            <div className="w-12 h-12 rounded-lg overflow-hidden bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex-shrink-0">
                              <img
                                src={preset.coverPath.startsWith('http') 
                                  ? preset.coverPath 
                                  : `https://picsum.photos/seed/${preset.id}/100/100`}
                                alt={preset.name}
                                className="w-full h-full object-cover"
                                onError={(e) => {
                                  const target = e.target as HTMLImageElement;
                                  target.src = `https://picsum.photos/seed/${preset.id}/100/100`;
                                }}
                              />
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2">
                                <p className="font-medium text-slate-200 truncate">{preset.name}</p>
                                {preset.isHNCS && (
                                  <span className="px-2 py-0.5 bg-amber-500/20 text-amber-400 text-xs font-bold rounded-full">
                                    HNCS
                                  </span>
                                )}
                              </div>
                              <p className="text-xs text-slate-500">
                                匹配度 {Math.round(preset.matchScore)}%
                                {preset.tags.length > 0 && ` · ${preset.tags.slice(0, 2).join(' · ')}`}
                              </p>
                            </div>
                            <button className="bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-slate-900 text-sm px-4 py-2 rounded-lg font-medium whitespace-nowrap transition-colors">
                              应用
                            </button>
                          </motion.div>
                        ))}
                      </div>
                    </div>
                  )}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Empty State */}
            <AnimatePresence>
              {!selectedImage && !isAnalyzing && !analysisComplete && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="bg-slate-800/50 backdrop-blur-sm rounded-3xl p-12 text-center border border-slate-700/50"
                >
                  <ImageIcon className="w-16 h-16 text-slate-600 mx-auto mb-4" />
                  <p className="text-slate-500">上传照片以开始 AI 分析</p>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
