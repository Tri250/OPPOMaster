import { motion } from 'framer-motion';
import { Upload, Sparkles, Check, Image as ImageIcon, Loader } from 'lucide-react';
import { useState, useRef, useCallback } from 'react';

const sampleImages = [
  { id: 1, label: '人像', seed: 'portrait' },
  { id: 2, label: '风景', seed: 'landscape' },
  { id: 3, label: '夜景', seed: 'night-city' },
  { id: 4, label: '美食', seed: 'food' }
];

const sceneResults = [
  { label: '人像', confidence: 0.95, color: 'from-pink-500 to-rose-500' },
  { label: '逆光', confidence: 0.87, color: 'from-yellow-500 to-orange-500' },
  { label: '户外', confidence: 0.82, color: 'from-green-500 to-emerald-500' }
];

export default function AIDemoPage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisComplete, setAnalysisComplete] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageSelect = (imageUrl: string) => {
    setSelectedImage(imageUrl);
    setAnalysisComplete(false);
  };

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setSelectedImage(event.target?.result as string);
        setAnalysisComplete(false);
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
      };
      reader.readAsDataURL(file);
    }
  }, []);

  const handleAnalyze = useCallback(() => {
    if (!selectedImage) return;
    
    setIsAnalyzing(true);
    setTimeout(() => {
      setIsAnalyzing(false);
      setAnalysisComplete(true);
    }, 2000);
  }, [selectedImage]);

  return (
    <div className="min-h-screen pt-20 pb-12 px-page sm:px-page lg:px-page">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-purple-500 to-pink-500 rounded-card mb-6">
            <Sparkles className="w-12 h-12 text-text-primary" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 text-hasselblad">
            AI智能场景识别
          </h1>
          <p className="text-lg text-text-tertiary max-w-2xl mx-auto">
            上传您的照片，体验AI智能识别场景并推荐最佳影像参数
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
            <div className="card p-8">
              <h2 className="text-xl font-bold mb-6">上传图片</h2>
              
              {/* Hidden File Input */}
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileSelect}
                accept="image/jpeg,image/png,image/jpg"
                className="hidden"
              />
              
              {/* Upload Area */}
              <motion.div
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleUploadClick}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-card p-12 text-center transition-all cursor-pointer group ${
                  isDragOver 
                    ? 'border-oppo-primary bg-oppo-primary/10' 
                    : 'border-white/20 hover:border-oppo-primary hover:bg-oppo-primary/5'
                }`}
              >
                <Upload className={`w-16 h-16 mx-auto mb-4 transition-colors ${
                  isDragOver ? 'text-oppo-primary' : 'text-text-tertiary group-hover:text-oppo-primary'
                }`} />
                <p className="text-text-tertiary mb-2">点击上传图片，或拖拽到此处</p>
                <p className="text-sm text-text-tertiary">支持 JPG、PNG 格式</p>
              </motion.div>

              {/* Sample Images */}
              <div className="mt-6">
                <p className="text-sm text-text-tertiary mb-3">或选择示例图片：</p>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {sampleImages.map((img) => (
                    <motion.button
                      key={img.id}
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => handleImageSelect(`https://picsum.photos/seed/${img.seed}/400/300`)}
                      className={`relative aspect-square rounded-button overflow-hidden border-2 transition-all ${
                        selectedImage?.includes(img.seed)
                          ? 'border-oppo-primary'
                          : 'border-transparent hover:border-white/20'
                      }`}
                    >
                      <img
                        src={`https://picsum.photos/seed/${img.seed}/200/200`}
                        alt={img.label}
                        className="w-full h-full object-cover"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex items-end p-2">
                        <span className="text-xs text-text-primary font-medium">{img.label}</span>
                      </div>
                    </motion.button>
                  ))}
                </div>
              </div>

              {/* Analyze Button */}
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleAnalyze}
                disabled={!selectedImage || isAnalyzing}
                className={`w-full mt-6 py-4 rounded-button font-semibold text-lg transition-all ${
                  selectedImage
                    ? 'btn-primary'
                    : 'bg-white/10 text-text-tertiary cursor-not-allowed'
                }`}
              >
                {isAnalyzing ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader className="w-5 h-5 animate-spin" />
                    <span>分析中...</span>
                  </span>
                ) : (
                  '开始分析'
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
            {/* Selected Image Preview */}
            {selectedImage && (
              <div className="card p-6">
                <h2 className="text-xl font-bold mb-4">图片预览</h2>
                <div className="relative aspect-video rounded-button overflow-hidden">
                  <img
                    src={selectedImage}
                    alt="Selected"
                    className="w-full h-full object-cover"
                  />
                  {isAnalyzing && (
                    <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                      <div className="text-center">
                        <Loader className="w-12 h-12 text-oppo-primary animate-spin mx-auto mb-2" />
                        <p className="text-text-primary">AI正在分析中...</p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Analysis Results */}
            {analysisComplete && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="card p-6 space-y-6"
              >
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold">识别结果</h2>
                  <div className="flex items-center gap-2 text-oppo-green">
                    <Check className="w-5 h-5" />
                    <span className="text-sm font-medium">分析完成</span>
                  </div>
                </div>

                {/* Scene Labels */}
                <div className="space-y-4">
                  {sceneResults.map((result, idx) => (
                    <motion.div
                      key={result.label}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: idx * 0.2 }}
                      className="space-y-2"
                    >
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-medium">{result.label}</span>
                        <span className="text-text-tertiary">
                          {Math.round(result.confidence * 100)}%
                        </span>
                      </div>
                      <div className="h-2 bg-white/10 rounded-small overflow-hidden">
                        <motion.div
                          initial={{ width: 0 }}
                          animate={{ width: `${result.confidence * 100}%` }}
                          transition={{ delay: 0.3 + idx * 0.2, duration: 0.5 }}
                          className={`h-full bg-gradient-to-r ${result.color} rounded-small`}
                        />
                      </div>
                    </motion.div>
                  ))}
                </div>

                {/* Recommended Presets */}
                <div>
                  <h3 className="text-sm font-bold text-text-tertiary mb-3">推荐影像参数</h3>
                  <div className="space-y-2">
                    {[1, 2, 3].map((i) => (
                      <motion.div
                        key={i}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.6 + i * 0.1 }}
                        className="flex items-center gap-3 p-3 bg-white/5 rounded-button hover:bg-white/10 transition-colors cursor-pointer"
                      >
                        <ImageIcon className="w-8 h-8 text-oppo-primary" />
                        <div className="flex-1">
                          <p className="font-medium text-sm">推荐影像参数 {i}</p>
                          <p className="text-xs text-text-tertiary">匹配度 {95 - i * 5}%</p>
                        </div>
                        <button className="btn-primary text-sm px-4 py-2">
                          应用
                        </button>
                      </motion.div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}

            {/* Empty State */}
            {!selectedImage && (
              <div className="card p-12 text-center">
                <ImageIcon className="w-16 h-16 text-text-tertiary/30 mx-auto mb-4" />
                <p className="text-text-tertiary">上传图片以开始AI分析</p>
              </div>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
}
