import { motion } from 'framer-motion';
import { Upload, Sparkles, Check, Image as ImageIcon, Loader, Camera, Image, X } from 'lucide-react';
import { useState, useRef, useCallback } from 'react';
import { 
  SceneType, 
  SceneDisplayNames, 
  SceneDescriptions,
  AiService,
  AiAdjustmentParams,
  createAiParams
} from '../types/ai';
import { mockPresets } from '../data/mockPresets';

// 示例图片，覆盖更多场景
const sampleImages = [
  { id: 1, label: '人像', seed: 'portrait', scene: SceneType.PORTRAIT },
  { id: 2, label: '风景', seed: 'landscape', scene: SceneType.LANDSCAPE },
  { id: 3, label: '夜景', seed: 'night-city', scene: SceneType.NIGHT },
  { id: 4, label: '美食', seed: 'food', scene: SceneType.FOOD },
  { id: 5, label: '街头', seed: 'street', scene: SceneType.STREET },
  { id: 6, label: '微距', seed: 'macro', scene: SceneType.MACRO },
  { id: 7, label: '日落', seed: 'sunset', scene: SceneType.SUNSET },
  { id: 8, label: '建筑', seed: 'architecture', scene: SceneType.ARCHITECTURE },
  { id: 9, label: '自然', seed: 'nature', scene: SceneType.NATURE }
];

export default function AIDemoPage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisComplete, setAnalysisComplete] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);
  const [detectedScene, setDetectedScene] = useState<SceneType>(SceneType.UNKNOWN);
  const [aiParams, setAiParams] = useState<AiAdjustmentParams | null>(null);
  const [recommendedPresets, setRecommendedPresets] = useState<any[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 处理图片选择
  const handleImageSelect = (imageUrl: string) => {
    setSelectedImage(imageUrl);
    setAnalysisComplete(false);
    setDetectedScene(SceneType.UNKNOWN);
    setAiParams(null);
    setRecommendedPresets([]);
  };

  // 处理文件选择
  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        handleImageSelect(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  }, []);

  // 上传按钮点击
  const handleUploadClick = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  // 拖拽事件处理
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
        handleImageSelect(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  }, []);

  // AI场景识别主函数
  const handleAnalyze = useCallback(async () => {
    if (!selectedImage) return;
    
    setIsAnalyzing(true);
    setAnalysisComplete(false);
    
    try {
      // 步骤1: 识别场景
      const scene = await AiService.detectScene(selectedImage);
      setDetectedScene(scene);
      
      // 步骤2: 获取推荐预设
      const presets = await AiService.getRecommendedPresets(scene, mockPresets as any[]);
      setRecommendedPresets(presets);
      
      // 步骤3: 获取AI参数调整
      const params = await AiService.fineTuneImage(selectedImage, presets[0] || null);
      setAiParams(params);
      
      // 完成分析
      setTimeout(() => {
        setIsAnalyzing(false);
        setAnalysisComplete(true);
      }, 800);
      
    } catch (error) {
      console.error('Analysis failed:', error);
      setIsAnalyzing(false);
    }
  }, [selectedImage]);

  // 清除选择
  const handleClear = () => {
    setSelectedImage(null);
    setAnalysisComplete(false);
    setDetectedScene(SceneType.UNKNOWN);
    setAiParams(null);
    setRecommendedPresets([]);
  };

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8 bg-gradient-to-br from-[#0f1419] via-[#0a0f14] to-[#0a0f14]">
      <div className="max-w-7xl mx-auto">
        {/* 头部区域 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-[#9fa8a3] to-[#1f2630] rounded-2xl mb-6 shadow-lg shadow-hasselblad/20">
            <Sparkles className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 bg-gradient-to-r from-white via-white/90 to-white/70 bg-clip-text text-transparent">
            AI智能场景识别
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            上传您的照片，体验AI智能识别场景并推荐最佳影像参数，与OPPO手机端功能同步
          </p>
        </motion.div>

        {/* 主要内容区域 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* 上传区域 */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="space-y-6"
          >
            <div className="bg-white/[0.03] backdrop-blur-sm border border-white/10 rounded-3xl p-8 shadow-xl">
              <h2 className="text-xl font-bold mb-6 text-white">上传图片</h2>
              
              {/* 隐藏的文件输入 */}
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileSelect}
                accept="image/jpeg,image/png,image/jpg,image/webp"
                className="hidden"
              />
              
              {/* 上传区域 */}
              <motion.div
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleUploadClick}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-2xl p-12 text-center transition-all cursor-pointer group ${
                  isDragOver 
                    ? 'border-[#9fa8a3] bg-[#9fa8a3]/10' 
                    : 'border-white/20 hover:border-[#9fa8a3] hover:bg-[#9fa8a3]/5'
                }`}
              >
                <Upload className={`w-16 h-16 mx-auto mb-4 transition-colors ${
                  isDragOver ? 'text-[#9fa8a3]' : 'text-white/40 group-hover:text-[#9fa8a3]'
                }`} />
                <p className="text-white/60 mb-2">点击上传图片，或拖拽到此处</p>
                <p className="text-sm text-white/40">支持 JPG、PNG、WebP 格式</p>
              </motion.div>

              {/* 已选择图片预览 */}
              {selectedImage && (
                <div className="mt-6">
                  <div className="flex items-center justify-between mb-3">
                    <p className="text-sm text-white/60">已选择图片：</p>
                    <button 
                      onClick={handleClear}
                      className="text-sm text-red-400 hover:text-red-300 flex items-center gap-1"
                    >
                      <X size={16} />
                      清除
                    </button>
                  </div>
                  <div className="relative aspect-video rounded-xl overflow-hidden border border-white/10">
                    <img
                      src={selectedImage}
                      alt="Selected"
                      className="w-full h-full object-cover"
                    />
                    {isAnalyzing && (
                      <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                        <div className="text-center">
                          <Loader className="w-12 h-12 text-[#9fa8a3] animate-spin mx-auto mb-2" />
                          <p className="text-white">AI正在分析中...</p>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* 示例图片选择 */}
              <div className="mt-6">
                <p className="text-sm text-white/60 mb-3">或选择示例图片：</p>
                <div className="grid grid-cols-3 sm:grid-cols-5 lg:grid-cols-3 xl:grid-cols-5 gap-3">
                  {sampleImages.map((img) => (
                    <motion.button
                      key={img.id}
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => handleImageSelect(`https://picsum.photos/seed/${img.seed}/400/300`)}
                      className={`relative aspect-square rounded-xl overflow-hidden border-2 transition-all ${
                        selectedImage?.includes(img.seed)
                          ? 'border-[#9fa8a3] shadow-lg shadow-[#9fa8a3]/20'
                          : 'border-transparent hover:border-white/20'
                      }`}
                    >
                      <img
                        src={`https://picsum.photos/seed/${img.seed}/200/200`}
                        alt={img.label}
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex items-end p-2">
                        <span className="text-xs text-white font-medium">{img.label}</span>
                      </div>
                    </motion.button>
                  ))}
                </div>
              </div>

              {/* 分析按钮 */}
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleAnalyze}
                disabled={!selectedImage || isAnalyzing}
                className={`w-full mt-6 py-4 rounded-xl font-semibold text-lg transition-all flex items-center justify-center gap-2 ${
                  selectedImage
                    ? 'bg-gradient-to-r from-[#9fa8a3] to-[#1f2630] text-white shadow-lg shadow-[#9fa8a3]/25 hover:shadow-[#9fa8a3]/40'
                    : 'bg-white/10 text-white/40 cursor-not-allowed'
                }`}
              >
                {isAnalyzing ? (
                  <>
                    <Loader className="w-5 h-5 animate-spin" />
                    <span>分析中...</span>
                  </>
                ) : (
                  <>
                    <Sparkles className="w-5 h-5" />
                    <span>开始AI分析</span>
                  </>
                )}
              </motion.button>
            </div>
          </motion.div>

          {/* 结果展示区域 */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="space-y-6"
          >
            {/* 识别结果卡片 */}
            {analysisComplete && detectedScene !== SceneType.UNKNOWN && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-white/[0.03] backdrop-blur-sm border border-white/10 rounded-3xl p-6 shadow-xl"
              >
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-xl font-bold text-white">识别结果</h2>
                  <div className="flex items-center space-x-2 text-green-400">
                    <Check className="w-5 h-5" />
                    <span className="text-sm font-medium">分析完成</span>
                  </div>
                </div>

                {/* 场景信息 */}
                <motion.div 
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.2 }}
                  className="flex items-start gap-4 p-4 bg-gradient-to-r from-[#9fa8a3]/10 to-transparent rounded-2xl border border-[#9fa8a3]/20"
                >
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-[#9fa8a3] to-[#1f2630] flex items-center justify-center flex-shrink-0">
                    <Camera className="w-6 h-6 text-white" />
                  </div>
                  <div className="flex-1">
                    <h3 className="text-lg font-bold text-white mb-1">
                      {SceneDisplayNames[detectedScene]}
                    </h3>
                    <p className="text-sm text-white/60">
                      {SceneDescriptions[detectedScene]}
                    </p>
                  </div>
                </motion.div>

                {/* AI参数建议 */}
                {aiParams && (
                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.4 }}
                    className="mt-6"
                  >
                    <h3 className="text-sm font-bold text-white/60 mb-3">AI推荐参数</h3>
                    <div className="grid grid-cols-3 gap-3">
                      {aiParams.toDisplayMap && Object.entries(aiParams.toDisplayMap()).slice(0, 9).map(([key, value], index) => (
                        <motion.div
                          key={key}
                          initial={{ opacity: 0, scale: 0.9 }}
                          animate={{ opacity: 1, scale: 1 }}
                          transition={{ delay: 0.5 + index * 0.05 }}
                          className="bg-white/5 rounded-xl p-3 text-center"
                        >
                          <p className="text-xs text-white/50 mb-1">{key}</p>
                          <p className="text-lg font-bold text-white">{value}</p>
                        </motion.div>
                      ))}
                    </div>
                  </motion.div>
                )}
              </motion.div>
            )}

            {/* 推荐预设 */}
            {analysisComplete && recommendedPresets.length > 0 && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.5 }}
                className="bg-white/[0.03] backdrop-blur-sm border border-white/10 rounded-3xl p-6 shadow-xl"
              >
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-bold text-white">推荐预设</h2>
                  <span className="text-sm text-white/50">
                    {recommendedPresets.length} 个结果
                  </span>
                </div>
                
                <div className="space-y-3 max-h-96 overflow-y-auto pr-2">
                  {recommendedPresets.slice(0, 5).map((preset: any, index: number) => (
                    <motion.div
                      key={preset.id}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.6 + index * 0.1 }}
                      className="flex items-center gap-4 p-4 bg-white/5 rounded-xl hover:bg-white/10 transition-colors cursor-pointer group"
                    >
                      <div className="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0">
                        <img
                          src={preset.coverPath}
                          alt={preset.name}
                          className="w-full h-full object-cover"
                          loading="lazy"
                          onError={(e) => {
                            (e.target as HTMLImageElement).src = `https://picsum.photos/seed/${preset.id}/200/200`;
                          }}
                        />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-white truncate">{preset.name}</p>
                        <p className="text-xs text-white/50 truncate">
                          {preset.deviceModel} · {preset.source === 'omaster_cloud' ? '官方' : '社区'}
                        </p>
                      </div>
                      <div className="flex flex-col items-end gap-1">
                        {preset.cameraParams?.hncs && (
                          <span className="text-xs px-2 py-0.5 bg-[#9fa8a3]/20 text-[#9fa8a3] rounded-full">
                            HNCS
                          </span>
                        )}
                        <button className="px-4 py-1.5 bg-gradient-to-r from-[#9fa8a3] to-[#1f2630] text-white text-sm rounded-lg opacity-0 group-hover:opacity-100 transition-opacity">
                          应用
                        </button>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            )}

            {/* 空状态 */}
            {!selectedImage && (
              <div className="bg-white/[0.03] backdrop-blur-sm border border-white/10 rounded-3xl p-12 text-center shadow-xl">
                <ImageIcon className="w-16 h-16 text-white/20 mx-auto mb-4" />
                <p className="text-white/60">上传图片以开始AI分析</p>
                <p className="text-sm text-white/40 mt-2">体验与OPPO手机端同步的AI场景识别</p>
              </div>
            )}
          </motion.div>
        </div>

        {/* 功能特性说明 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.8 }}
          className="mt-12"
        >
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-white/[0.03] backdrop-blur-sm border border-white/10 rounded-2xl p-6">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[#9fa8a3]/20 to-[#1f2630]/20 flex items-center justify-center mb-4">
                <Sparkles className="w-6 h-6 text-[#9fa8a3]" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">覆盖9种场景</h3>
              <p className="text-white/60 text-sm">人像、风景、夜景、美食、街头、微距、日落、建筑、自然</p>
            </div>
            <div className="bg-white/[0.03] backdrop-blur-sm border border-white/10 rounded-2xl p-6">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[#9fa8a3]/20 to-[#1f2630]/20 flex items-center justify-center mb-4">
                <Camera className="w-6 h-6 text-[#9fa8a3]" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">智能参数推荐</h3>
              <p className="text-white/60 text-sm">根据识别结果，自动推荐最佳影像参数</p>
            </div>
            <div className="bg-white/[0.03] backdrop-blur-sm border border-white/10 rounded-2xl p-6">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[#9fa8a3]/20 to-[#1f2630]/20 flex items-center justify-center mb-4">
                <Image className="w-6 h-6 text-[#9fa8a3]" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">与手机端同步</h3>
              <p className="text-white/60 text-sm">与OPPO手机端AI场景识别功能保持一致</p>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
