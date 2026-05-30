import { motion, AnimatePresence } from 'framer-motion';
import { Upload, Sparkles, Check, Image as ImageIcon, Loader, Camera } from 'lucide-react';
import { useState, useRef, useCallback } from 'react';
import { mockPresets } from '../data/mockPresets';

const sampleImages = [
  { id: 1, label: '人像', seed: 'portrait' },
  { id: 2, label: '风景', seed: 'landscape' },
  { id: 3, label: '夜景', seed: 'night' },
  { id: 4, label: '美食', seed: 'food' },
  { id: 5, label: '建筑', seed: 'architecture' }
];

export default function AIDemoPage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisComplete, setAnalysisComplete] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);
  const [detectedScene, setDetectedScene] = useState<any>(null);
  const [recommendedPresets, setRecommendedPresets] = useState<any[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageSelect = useCallback((imageUrl: string) => {
    setSelectedImage(imageUrl);
    setSelectedFile(null);
    setAnalysisComplete(false);
    setDetectedScene(null);
    setRecommendedPresets([]);
  }, []);

  const handleFileSelect = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      const reader = new FileReader();
      reader.onload = (event) => {
        setSelectedImage(event.target?.result as string);
        setAnalysisComplete(false);
        setDetectedScene(null);
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
        setDetectedScene(null);
        setRecommendedPresets([]);
      };
      reader.readAsDataURL(file);
    }
  }, []);

  const handleAnalyze = useCallback(() => {
    if (!selectedImage) return;

    setIsAnalyzing(true);
    setAnalysisComplete(false);

    setTimeout(() => {
      const scenes = ['人像', '风景', '夜景', '美食', '建筑'];
      const randomScene = scenes[Math.floor(Math.random() * scenes.length)];
      
      setDetectedScene({
        label: randomScene,
        confidence: 0.85 + Math.random() * 0.15,
        description: `适合${randomScene}摄影的哈苏参数`
      });

      setRecommendedPresets(mockPresets.slice(0, 3).map(preset => ({
        ...preset,
        matchScore: 0.7 + Math.random() * 0.3
      })));

      setAnalysisComplete(true);
      setIsAnalyzing(false);
    }, 1500);
  }, [selectedImage]);

  const handleClear = useCallback(() => {
    setSelectedImage(null);
    setSelectedFile(null);
    setAnalysisComplete(false);
    setDetectedScene(null);
    setRecommendedPresets([]);
  }, []);

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8 bg-gray-900">
      <div className="max-w-7xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-yellow-600 to-yellow-500 rounded-2xl mb-6 shadow-lg">
            <Sparkles className="w-12 h-12 text-gray-900" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 text-white">
            AI 智能场景识别
          </h1>
          <p className="text-lg text-gray-400 max-w-2xl mx-auto">
            基于 DeepSeek AI 技术，智能识别场景并推荐最佳哈苏大师影像预设
          </p>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="space-y-6"
          >
            <div className="bg-gray-800/50 backdrop-blur-sm rounded-3xl p-8 border border-gray-700/50">
              <h2 className="text-xl font-bold mb-6 text-white flex items-center gap-2">
                <ImageIcon className="w-5 h-5 text-yellow-500" />
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
                    <div className="absolute inset-0 bg-gradient-to-t from-gray-900/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                    <button
                      onClick={handleClear}
                      className="absolute top-4 right-4 p-2 bg-black/50 hover:bg-black/70 rounded-full text-white transition-colors"
                    >
                      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
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
                        ? 'border-yellow-500 bg-yellow-500/10' 
                        : 'border-gray-600 hover:border-yellow-500 hover:bg-yellow-500/5'
                    }`}
                  >
                    <Upload className={`w-16 h-16 mx-auto mb-4 transition-colors ${
                      isDragOver ? 'text-yellow-500' : 'text-gray-500'
                    }`} />
                    <p className="text-gray-400 mb-2">点击上传照片，或拖拽到此处</p>
                    <p className="text-sm text-gray-500">支持 JPG、PNG 格式</p>
                  </motion.div>
                )}
              </AnimatePresence>

              <div className="mt-6">
                <p className="text-sm text-gray-400 mb-3">基础场景测试</p>
                <div className="grid grid-cols-5 gap-3">
                  {sampleImages.map((img) => (
                    <motion.button
                      key={img.id}
                      whileHover={{ scale: 1.05, y: -2 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => handleImageSelect(`https://picsum.photos/seed/${img.seed}/400/300`)}
                      className={`relative aspect-square rounded-xl overflow-hidden transition-all ${
                        selectedImage?.includes(img.seed)
                          ? 'ring-2 ring-yellow-500 ring-offset-2 ring-offset-gray-900'
                          : 'hover:ring-2 hover:ring-gray-600'
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
                    ? 'bg-gradient-to-r from-yellow-600 to-yellow-500 text-gray-900 hover:shadow-lg'
                    : 'bg-gray-700 text-gray-500 cursor-not-allowed'
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
            <div className="bg-gray-800/50 backdrop-blur-sm rounded-3xl p-8 border border-gray-700/50">
              <h2 className="text-xl font-bold mb-6 text-white flex items-center gap-2">
                <Check className="w-5 h-5 text-yellow-500" />
                识别结果
              </h2>

              <AnimatePresence>
                {analysisComplete && detectedScene ? (
                  <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="space-y-6"
                  >
                    <div className="bg-gray-700/50 rounded-2xl p-6 border border-gray-600/50">
                      <div className="flex items-start justify-between mb-4">
                        <div>
                          <h3 className="text-2xl font-bold text-white mb-1">
                            {detectedScene.label}
                          </h3>
                          <p className="text-gray-400">
                            {detectedScene.description}
                          </p>
                        </div>
                        <div className="text-right">
                          <div className="text-3xl font-bold text-yellow-500">
                            {Math.round(detectedScene.confidence * 100)}%
                          </div>
                          <div className="text-sm text-gray-500">置信度</div>
                        </div>
                      </div>
                      <div className="w-full bg-gray-700 rounded-full h-2 overflow-hidden">
                        <motion.div
                          initial={{ width: 0 }}
                          animate={{ width: `${detectedScene.confidence * 100}%` }}
                          transition={{ delay: 0.3, duration: 0.8 }}
                          className="h-full bg-gradient-to-r from-yellow-600 to-yellow-500 rounded-full"
                        />
                      </div>
                    </div>

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
                              className="bg-gray-700/50 rounded-xl p-4 border border-gray-600/50 flex items-center gap-4"
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
                                  <span className="text-xs px-2 py-0.5 bg-yellow-500/20 text-yellow-500 rounded-full">
                                    {Math.round(preset.matchScore * 100)}% 匹配
                                  </span>
                                </div>
                                <div className="flex flex-wrap gap-1">
                                  {preset.tags.slice(0, 3).map((tag: string) => (
                                    <span key={tag} className="text-xs text-gray-400">
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
                    <div className="w-20 h-20 mx-auto mb-4 bg-gray-700/50 rounded-2xl flex items-center justify-center">
                      <Camera className="w-10 h-10 text-gray-500" />
                    </div>
                    <p className="text-gray-500">选择照片后开始识别</p>
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
