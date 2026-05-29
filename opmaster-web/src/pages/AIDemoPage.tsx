import { motion } from 'framer-motion';
import { Upload, Sparkles, Check, Image as ImageIcon, Loader, X } from 'lucide-react';
import { useState, useRef } from 'react';

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
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        setSelectedImage(e.target?.result as string);
        setAnalysisComplete(false);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleSampleSelect = (seed: string) => {
    setSelectedImage(`https://picsum.photos/seed/${seed}/600/450`);
    setAnalysisComplete(false);
  };

  const handleAnalyze = () => {
    if (!selectedImage) return;
    
    setIsAnalyzing(true);
    setTimeout(() => {
      setIsAnalyzing(false);
      setAnalysisComplete(true);
    }, 2000);
  };

  const handleClear = () => {
    setSelectedImage(null);
    setAnalysisComplete(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="min-h-screen bg-deep-space pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-10"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-orange to-hasselblad rounded-2xl mb-6">
            <Sparkles className="w-12 h-12 text-deep-space" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4">
            <span className="bg-gradient-to-r from-white via-oppo-orange to-hasselblad bg-clip-text text-transparent">
              AI 智能场景识别
            </span>
          </h1>
          <p className="text-lg text-text-secondary max-w-2xl mx-auto">
            上传您的照片，体验AI智能识别场景并推荐最佳预设
          </p>
        </motion.div>

        {/* Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Upload Section */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="space-y-5"
          >
            <div className="bg-surface border border-border-subtle rounded-3xl p-6">
              <h2 className="text-xl font-bold mb-5 text-text-primary">
                上传图片
              </h2>
              
              {/* Upload Area */}
              {!selectedImage ? (
                <div
                  onClick={handleUploadClick}
                  className="border-2 border-dashed border-border-light rounded-2xl p-12 text-center hover:border-oppo-orange/60 transition-colors cursor-pointer group"
                >
                  <Upload className="w-16 h-16 text-text-tertiary mx-auto mb-4 group-hover:text-oppo-orange transition-colors" />
                  <p className="text-text-secondary mb-2">
                    点击或拖拽上传
                  </p>
                  <p className="text-sm text-text-tertiary">
                    支持 JPG、PNG 格式，最大 10MB
                  </p>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </div>
              ) : (
                <div className="relative">
                  <div className="relative aspect-video rounded-2xl overflow-hidden">
                    <img
                      src={selectedImage}
                      alt="Selected"
                      className="w-full h-full object-cover"
                    />
                    {isAnalyzing && (
                      <div className="absolute inset-0 bg-deep-space/80 flex items-center justify-center">
                        <div className="text-center">
                          <Loader className="w-12 h-12 text-oppo-orange animate-spin mx-auto mb-2" />
                          <p className="text-text-primary">AI 正在分析中...</p>
                        </div>
                      </div>
                    )}
                  </div>
                  <button
                    onClick={handleClear}
                    className="absolute top-3 right-3 w-8 h-8 bg-surface border border-border-subtle rounded-full flex items-center justify-center hover:bg-surface-hover transition-all"
                  >
                    <X className="w-4 h-4 text-text-secondary" />
                  </button>
                </div>
              )}

              {/* Sample Images */}
              {!selectedImage && (
                <div className="mt-5">
                  <p className="text-sm text-text-tertiary mb-3">
                    或选择示例图片：
                  </p>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    {sampleImages.map((img) => (
                      <motion.button
                        key={img.id}
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => handleSampleSelect(img.seed)}
                        className="relative aspect-square rounded-xl overflow-hidden border-2 border-transparent hover:border-oppo-orange/50 transition-all"
                      >
                        <img
                          src={`https://picsum.photos/seed/${img.seed}/200/200`}
                          alt={img.label}
                          className="w-full h-full object-cover"
                        />
                        <div className="absolute inset-0 bg-gradient-to-t from-deep-space/80 to-transparent flex items-end p-2">
                          <span className="text-xs text-white font-medium">
                            {img.label}
                          </span>
                        </div>
                      </motion.button>
                    ))}
                  </div>
                </div>
              )}

              {/* Analyze Button */}
              {selectedImage && !analysisComplete && (
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleAnalyze}
                  disabled={isAnalyzing}
                  className="w-full mt-5 py-4 rounded-2xl font-semibold text-lg transition-all bg-gradient-to-r from-oppo-orange to-hasselblad text-deep-space hover:shadow-oppo-hover disabled:opacity-50 disabled:cursor-not-allowed"
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
              )}
            </div>
          </motion.div>

          {/* Results Section */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="space-y-5"
          >
            {/* Analysis Results */}
            {analysisComplete && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-surface border border-border-subtle rounded-3xl p-6 space-y-5"
              >
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold text-text-primary">
                    识别结果
                  </h2>
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
                        <span className="font-medium text-text-primary">
                          {result.label}
                        </span>
                        <span className="text-text-tertiary">
                          {Math.round(result.confidence * 100)}%
                        </span>
                      </div>
                      <div className="h-2 bg-surface-hover rounded-full overflow-hidden">
                        <motion.div
                          initial={{ width: 0 }}
                          animate={{ width: `${result.confidence * 100}%` }}
                          transition={{ delay: 0.3 + idx * 0.2, duration: 0.6 }}
                          className={`h-full bg-gradient-to-r ${result.color} rounded-full`}
                        />
                      </div>
                    </motion.div>
                  ))}
                </div>

                {/* Recommended Presets */}
                <div>
                  <h3 className="text-sm font-bold text-text-tertiary mb-3">
                    推荐预设
                  </h3>
                  <div className="space-y-2">
                    {[
                      { name: '德味预设', match: 95 },
                      { name: '富士胶片', match: 87 },
                      { name: '胶片感', match: 82 }
                    ].map((preset, i) => (
                      <motion.div
                        key={i}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.6 + i * 0.1 }}
                        className="flex items-center gap-3 p-3.5 bg-surface-hover rounded-xl hover:bg-surface-hover/80 transition-colors cursor-pointer"
                      >
                        <div className="w-8 h-8 bg-oppo-orange/20 rounded-lg flex items-center justify-center">
                          <ImageIcon className="w-4 h-4 text-oppo-orange" />
                        </div>
                        <div className="flex-1">
                          <p className="font-medium text-sm text-text-primary">
                            {preset.name}
                          </p>
                          <p className="text-xs text-text-tertiary">
                            匹配度 {preset.match}%
                          </p>
                        </div>
                        <button className="bg-oppo-orange text-deep-space text-sm px-4 py-2 rounded-lg font-medium">
                          应用
                        </button>
                      </motion.div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}

            {/* Empty State */}
            {!analysisComplete && (
              <div className="bg-surface border border-border-subtle rounded-3xl p-12 text-center">
                <ImageIcon className="w-16 h-16 text-text-tertiary mx-auto mb-4" />
                <p className="text-text-secondary">
                  上传图片以开始AI分析
                </p>
              </div>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
}
