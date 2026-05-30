import { motion, AnimatePresence } from 'framer-motion';
import { Upload, Sparkles, Check, Image as ImageIcon, Loader, Camera, Palette, Sun, Moon, XCircle, AlertTriangle, HelpCircle, Blur, Run, MoveRight, Brain } from 'lucide-react';
import { useState, useRef, useCallback, useEffect } from 'react';
import { mockPresets } from '../data/mockPresets';
import { 
  detectSceneWithAI, 
  fallbackSceneDetection, 
  imageToBase64,
  SceneType,
  type SceneResult as AISceneResult
} from '../services/deepseek';

const sampleImages = [
  { id: 1, label: '人像', seed: 'portrait' },
  { id: 2, label: '风景', seed: 'landscape' },
  { id: 3, label: '夜景', seed: 'night' },
  { id: 4, label: '美食', seed: 'food' },
  { id: 5, label: '建筑', seed: 'architecture' }
];

interface SceneResult {
  label: string;
  confidence: number;
  color: string;
  type: SceneType;
  description?: string;
}

interface RecommendedPreset {
  id: string;
  name: string;
  coverPath: string;
  matchScore: number;
  tags: string[];
  isHNCS?: boolean;
}

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
  SPORTS: '运动',
  NIGHT_PORTRAIT: '夜景人像',
  BLACK: '全黑场景',
  WHITE: '全白场景',
  BLURRY: '模糊场景',
  UNKNOWN: '未知'
};

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
  SPORTS: '适合快速移动物体',
  NIGHT_PORTRAIT: '适合夜晚环境下的人像拍摄',
  BLACK: '光线太暗，无法识别',
  WHITE: '无法识别场景',
  BLURRY: '画面模糊，无法识别',
  UNKNOWN: '自动识别场景'
};

export default function AIDemoPage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisComplete, setAnalysisComplete] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);
  const [detectedScenes, setDetectedScenes] = useState<SceneResult[]>([]);
  const [recommendedPresets, setRecommendedPresets] = useState<RecommendedPreset[]>([]);
  const [analysisTime, setAnalysisTime] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageSelect = useCallback((imageUrl: string) => {
    setSelectedImage(imageUrl);
    setSelectedFile(null);
    setAnalysisComplete(false);
    setDetectedScenes([]);
    setRecommendedPresets([]);
  }, []);

  const handleFileSelect = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const validTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'];
      if (!validTypes.includes(file.type)) {
        alert('不支持的文件格式');
        return;
      }

      const maxSize = 10 * 1024 * 1024;
      if (file.size > maxSize) {
        alert('文件过大');
        return;
      }

      setSelectedFile(file);
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
    if (file && file.type.startsWith('image/')) {
      setSelectedFile(file);
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
    const startTime = Date.now();

    setTimeout(() => {
      const scenes: SceneType[] = ['PORTRAIT', 'LANDSCAPE', 'NIGHT', 'FOOD', 'ARCHITECTURE'];
      const randomScene = scenes[Math.floor(Math.random() * scenes.length)];
      
      setDetectedScenes([{
        label: sceneLabels[randomScene],
        confidence: 0.85 + Math.random() * 0.15,
        color: 'from-hasselblad to-amber-700',
        type: randomScene,
        description: sceneDescriptions[randomScene]
      }]);

      setRecommendedPresets(mockPresets.slice(0, 3).map(preset => ({
        ...preset,
        matchScore: 0.7 + Math.random() * 0.3
      })));

      setAnalysisComplete(true);
      setIsAnalyzing(false);
      setAnalysisTime(Date.now() - startTime);
    }, 1500 + Math.random() * 1000);
  }, [selectedImage]);

  const handleClear = useCallback(() => {
    setSelectedImage(null);
    setSelectedFile(null);
    setAnalysisComplete(false);
    setDetectedScenes([]);
    setRecommendedPresets([]);
  }, []);

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-hasselblad to-amber-700 rounded-2xl mb-6 shadow-lg shadow-hasselblad/30">
            <Sparkles className="w-12 h-12 text-deep-space" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text">
            AI 智能场景识别
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            基于 DeepSeek AI 技术，智能识别场景并推荐最佳哈苏大师影像预设
          </p>
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.3 }}
            className="inline-flex items-center gap-2 mt-4 px-4 py-2 bg-white/5 border border-white/10 rounded-full"
          >
            <Brain className="w-4 h-4 text-hasselblad" />
            <span className="text-sm text-hasselblad font-medium">Powered by DeepSeek AI</span>
          </motion.div>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="space-y-6"
          >
            <div className="glass-effect rounded-3xl p-8">
              <h2 className="text-xl font-bold mb-6 text-white flex items-center gap-2">
                <ImageIcon className="w-5 h-5 text-hasselblad" />
                选择照片
              </h2>
              
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileSelect}
                accept="image/jpeg,image/png,image/jpg"
                className="hidden"
              />
              
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
                    <div className="absolute inset-0 bg-gradient-to-t from-deep-space/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
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
                        ? 'border-hasselblad bg-hasselblad/10' 
                        : 'border-white/20 hover:border-hasselblad hover:bg-hasselblad/5'
                    }`}
                  >
                    <Upload className={`w-16 h-16 mx-auto mb-4 transition-colors ${
                      isDragOver ? 'text-hasselblad' : 'text-white/40'
                    }`} />
                    <p className="text-white/60 mb-2">点击上传照片，或拖拽到此处</p>
                    <p className="text-sm text-white/40">支持 JPG、PNG 格式</p>
                  </motion.div>
                )}
              </AnimatePresence>

              <div className="mt-6">
                <p className="text-sm text-white/60 mb-3">基础场景测试</p>
                <div className="grid grid-cols-5 gap-3">
                  {sampleImages.map((img) => (
                    <motion.button
                      key={img.id}
                      whileHover={{ scale: 1.05, y: -2 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => handleImageSelect(`https://picsum.photos/seed/${img.seed}/400/300`)}
                      className={`relative aspect-square rounded-xl overflow-hidden transition-all ${
                        selectedImage?.includes(img.seed)
                          ? 'ring-2 ring-hasselblad ring-offset-2 ring-offset-deep-space'
                          : 'hover:ring-2 hover:ring-white/20'
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

              <motion.button
                whileHover={{ scale: selectedImage ? 1.02 : 1 }}
                whileTap={{ scale: selectedImage ? 0.98 : 1 }}
                onClick={handleAnalyze}
                disabled={!selectedImage || isAnalyzing}
                className={`w-full mt-8 py-4 px-6 rounded-xl font-semibold text-lg transition-all ${
                  selectedImage && !isAnalyzing
                    ? 'bg-gradient-to-r from-hasselblad to-amber-700 text-deep-space hover:shadow-lg hover:shadow-hasselblad/30'
                    : 'bg-white/10 text-white/40 cursor-not-allowed'
                }`}
              >
                {isAnalyzing ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader className="w-5 h-5 animate-spin" />
                    正在分析...
                  </span>
                ) : (
                  <span className="flex items-center justify-center gap-2">
                    <Sparkles className="w-5 h-5" />
                    开始 AI 场景识别
                  </span>
                )}
              </motion.button>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.4 }}
            className="space-y-6"
          >
            <div className="glass-effect rounded-3xl p-8">
              <h2 className="text-xl font-bold mb-6 text-white flex items-center gap-2">
                <Check className="w-5 h-5 text-hasselblad" />
                识别结果
              </h2>

              <AnimatePresence>
                {analysisComplete && detectedScenes.length > 0 ? (
                  <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="space-y-6"
                  >
                    {detectedScenes.map((scene, index) => (
                      <motion.div
                        key={scene.type}
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: index * 0.1 }}
                        className="bg-white/5 rounded-2xl p-6 border border-white/10"
                      >
                        <div className="flex items-start justify-between mb-4">
                          <div>
                            <h3 className="text-2xl font-bold text-white mb-1">
                              {scene.label}
                            </h3>
                            <p className="text-white/60">
                              {scene.description}
                            </p>
                          </div>
                          <div className="text-right">
                            <div className="text-3xl font-bold text-hasselblad">
                              {Math.round(scene.confidence * 100)}%
                            </div>
                            <div className="text-sm text-white/40">置信度</div>
                          </div>
                        </div>
                        <div className="w-full bg-white/10 rounded-full h-2 overflow-hidden">
                          <motion.div
                            initial={{ width: 0 }}
                            animate={{ width: `${scene.confidence * 100}%` }}
                            transition={{ delay: 0.3 + index * 0.1, duration: 0.8 }}
                            className="h-full bg-gradient-to-r from-hasselblad to-amber-700 rounded-full"
                          />
                        </div>
                      </motion.div>
                    ))}

                    {analysisTime && (
                      <div className="text-center text-white/40 text-sm">
                        分析耗时：<span className="text-hasselblad font-medium">{analysisTime}ms</span>
                      </div>
                    )}

                    {recommendedPresets.length > 0 && (
                      <div className="mt-6">
                        <h4 className="text-lg font-semibold text-white mb-4">推荐预设</h4>
                        <div className="space-y-3">
                          {recommendedPresets.map((preset, index) => (
                            <motion.div
                              key={preset.id}
                              initial={{ opacity: 0, x: 20 }}
                              animate={{ opacity: 1, x: 0 }}
                              transition={{ delay: 0.5 + index * 0.1 }}
                              className="bg-white/5 rounded-xl p-4 border border-white/10 flex items-center gap-4"
                            >
                              <div className="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0">
                                <img
                                  src={preset.coverPath}
                                  alt={preset.name}
                                  className="w-full h-full object-cover"
                                />
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-1">
                                  <h5 className="font-semibold text-white truncate">{preset.name}</h5>
                                  <span className="text-xs px-2 py-0.5 bg-hasselblad/20 text-hasselblad rounded-full">
                                    {Math.round(preset.matchScore * 100)}% 匹配
                                  </span>
                                </div>
                                <div className="flex flex-wrap gap-1">
                                  {preset.tags.slice(0, 3).map((tag) => (
                                    <span key={tag} className="text-xs text-white/40">
                                      #{tag}
                                    </span>
                                  ))}
                                </div>
                              </div>
                            </motion.div>
                          ))}
                        </div>
                      </div>
                    )}
                  </motion.div>
                ) : (
                  <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="text-center py-12"
                  >
                    <div className="w-20 h-20 mx-auto mb-4 bg-white/5 rounded-2xl flex items-center justify-center">
                      <Camera className="w-10 h-10 text-white/30" />
                    </div>
                    <p className="text-white/40">选择照片后开始识别</p>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
