import { motion } from 'framer-motion';
import { Upload, Sparkles, Check, Image as ImageIcon, Loader, ArrowLeft, Smartphone, Palette, Zap, User } from 'lucide-react';
import { useState, useRef, useCallback } from 'react';
import { Link } from 'react-router-dom';

const sampleImages = [
  { id: 1, label: '人像', seed: 'portrait' },
  { id: 2, label: '风景', seed: 'landscape' },
  { id: 3, label: '夜景', seed: 'night-city' },
  { id: 4, label: '美食', seed: 'food' }
];

const sceneResults = [
  { label: '人像', confidence: 0.95, color: 'from-oppo-pink to-oppo-purple' },
  { label: '逆光', confidence: 0.87, color: 'from-oppo-orange to-hasselblad-orange' },
  { label: '户外', confidence: 0.82, color: 'from-oppo-green to-oppo-blue' }
];

const easeOppoEnter = [0.05, 0.7, 0.1, 1.0] as [number, number, number, number];

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
    <div className="min-h-screen bg-bg-primary text-text-primary">
      {/* ColorOS 16 背景光效 */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <motion.div
          animate={{ 
            x: [0, 80, 0], 
            y: [0, 40, 0],
          }}
          transition={{ 
            duration: 25, 
            repeat: Infinity, 
            ease: 'easeInOut' 
          }}
          className="absolute -top-52 -left-52 w-[500px] h-[500px] orb-oppo orb-purple"
        />
      </div>

      {/* 顶部导航栏 */}
      <header className="sticky top-0 z-40 glass-navigation">
        <div className="max-w-7xl mx-auto px-4 h-14 flex items-center">
          <Link to="/" className="flex items-center gap-2 touch-feedback mr-4">
            <div className="p-1.5 rounded-xl hover:bg-white/10 transition-colors">
              <ArrowLeft className="w-5 h-5 text-text-primary" />
            </div>
          </Link>
          <h1 className="text-h2 font-bold">AI智能场景识别</h1>
        </div>
      </header>

      <main className="relative z-10">
        <div className="max-w-7xl mx-auto px-4 py-6">
          {/* Hero 区域 */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ease: easeOppoEnter }}
            className="mb-8"
          >
            <div className="card-glass p-6 sm:p-8 text-center">
              <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-purple to-oppo-pink rounded-2xl mb-4 shadow-oppo-elevation-2">
                <Sparkles className="w-12 h-12 text-text-primary" />
              </div>
              <h2 className="text-h1 font-bold mb-3 gradient-text-oppo">
                AI智能场景识别
              </h2>
              <p className="text-body1 text-text-secondary max-w-2xl mx-auto">
                上传您的照片，体验AI智能识别场景并推荐最佳影像参数
              </p>
            </div>
          </motion.div>

          {/* Main Content */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Upload Section */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1, ease: easeOppoEnter }}
              className="space-y-6"
            >
              <div className="card-elevated p-6">
                <h2 className="text-body1 font-bold mb-6">上传图片</h2>
                
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
                  className={`border-2 border-dashed rounded-2xl p-12 text-center transition-all cursor-pointer group ${
                    isDragOver 
                      ? 'border-oppo-orange bg-oppo-orange/10' 
                      : 'border-border-default hover:border-oppo-orange hover:bg-oppo-orange/5'
                  }`}
                >
                  <Upload className={`w-16 h-16 mx-auto mb-4 transition-colors ${
                    isDragOver ? 'text-oppo-orange' : 'text-text-tertiary group-hover:text-oppo-orange'
                  }`} />
                  <p className="text-text-secondary mb-2">点击上传图片，或拖拽到此处</p>
                  <p className="text-sm text-text-tertiary">支持 JPG、PNG 格式</p>
                </motion.div>

                {/* Sample Images */}
                <div className="mt-6">
                  <p className="text-body2 text-text-tertiary mb-3">或选择示例图片：</p>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    {sampleImages.map((img) => (
                      <motion.button
                        key={img.id}
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => handleImageSelect(`https://picsum.photos/seed/${img.seed}/400/300`)}
                        className={`relative aspect-square rounded-2xl overflow-hidden border-2 transition-all ${
                          selectedImage?.includes(img.seed)
                            ? 'border-oppo-orange shadow-oppo-elevation-1'
                            : 'border-border-default hover:border-oppo-orange/50'
                        }`}
                      >
                        <img
                          src={`https://picsum.photos/seed/${img.seed}/200/200`}
                          alt={img.label}
                          className="w-full h-full object-cover"
                        />
                        <div className="absolute inset-0 bg-gradient-to-t from-bg-primary/80 to-transparent flex items-end p-3">
                          <span className="text-caption text-text-primary font-medium">{img.label}</span>
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
                  className={`w-full mt-6 py-4 rounded-2xl font-semibold text-body1 transition-all ${
                    selectedImage
                      ? 'btn-primary'
                      : 'bg-bg-secondary text-text-tertiary cursor-not-allowed'
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
              transition={{ delay: 0.2, ease: easeOppoEnter }}
              className="space-y-6"
            >
              {/* Selected Image Preview */}
              {selectedImage && (
                <div className="card-elevated p-6">
                  <h2 className="text-body1 font-bold mb-4">图片预览</h2>
                  <div className="relative aspect-video rounded-2xl overflow-hidden">
                    <img
                      src={selectedImage}
                      alt="Selected"
                      className="w-full h-full object-cover"
                    />
                    {isAnalyzing && (
                      <div className="absolute inset-0 bg-bg-primary/70 flex items-center justify-center">
                        <div className="text-center">
                          <Loader className="w-12 h-12 text-oppo-orange animate-spin mx-auto mb-2" />
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
                  transition={{ ease: easeOppoEnter }}
                  className="card-elevated p-6 space-y-6"
                >
                  <div className="flex items-center justify-between">
                    <h2 className="text-body1 font-bold">识别结果</h2>
                    <div className="flex items-center gap-2 text-oppo-green">
                      <Check className="w-5 h-5" />
                      <span className="text-body2 font-medium">分析完成</span>
                    </div>
                  </div>

                  {/* Scene Labels */}
                  <div className="space-y-4">
                    {sceneResults.map((result, idx) => (
                      <motion.div
                        key={result.label}
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: idx * 0.15, ease: easeOppoEnter }}
                        className="space-y-2"
                      >
                        <div className="flex items-center justify-between text-body2">
                          <span className="font-medium">{result.label}</span>
                          <span className="text-text-secondary">
                            {Math.round(result.confidence * 100)}%
                          </span>
                        </div>
                        <div className="h-2 bg-bg-secondary rounded-full overflow-hidden">
                          <motion.div
                            initial={{ width: 0 }}
                            animate={{ width: `${result.confidence * 100}%` }}
                            transition={{ delay: 0.2 + idx * 0.15, duration: 0.5, ease: easeOppoEnter }}
                            className={`h-full bg-gradient-to-r ${result.color} rounded-full`}
                          />
                        </div>
                      </motion.div>
                    ))}
                  </div>

                  {/* Recommended Presets */}
                  <div>
                    <h3 className="text-body2 font-bold text-text-secondary mb-3">推荐影像参数</h3>
                    <div className="space-y-2">
                      {[1, 2, 3].map((i) => (
                        <motion.div
                          key={i}
                          initial={{ opacity: 0, y: 10 }}
                          animate={{ opacity: 1, y: 0 }}
                          transition={{ delay: 0.5 + i * 0.1, ease: easeOppoEnter }}
                          className="flex items-center gap-3 p-4 bg-bg-secondary rounded-2xl hover:bg-oppo-orange/10 transition-colors cursor-pointer"
                        >
                          <ImageIcon className="w-8 h-8 text-oppo-orange" />
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-body2 truncate">推荐影像参数 {i}</p>
                            <p className="text-caption text-text-tertiary">匹配度 {95 - i * 5}%</p>
                          </div>
                          <button className="btn-primary text-sm px-4 py-2 shrink-0">
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
                <div className="card-elevated p-12 text-center">
                  <ImageIcon className="w-16 h-16 text-text-tertiary mx-auto mb-4" />
                  <p className="text-text-secondary">上传图片以开始AI分析</p>
                </div>
              )}
            </motion.div>
          </div>
        </div>
      </main>

      {/* 底部导航栏 - 移动端 */}
      <nav className="bottom-nav-bar md:hidden">
        <div className="flex items-center justify-around h-full max-w-md mx-auto">
          {[
            { icon: Smartphone, label: '首页', path: '/', active: false },
            { icon: Palette, label: '预设', path: '/filter-library', active: false },
            { icon: Zap, label: 'AI', path: '/ai-demo', active: true },
            { icon: User, label: '我的', path: '/settings', active: false },
          ].map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className="flex flex-col items-center gap-1.5 px-4 py-2.5 touch-feedback-strong"
            >
              <div className={`p-2 rounded-xl transition-all duration-300 ease-out-elastic ${
                item.active 
                  ? 'bg-gradient-to-br from-oppo-orange/20 to-hasselblad-orange/20' 
                  : 'hover:bg-white/8'
              }`}>
                <item.icon className={`w-5.5 h-5.5 ${item.active ? 'text-oppo-orange' : 'text-text-tertiary'}`} />
              </div>
              <span className={`text-caption font-semibold ${
                item.active ? 'text-oppo-orange' : 'text-text-tertiary'
              }`}>
                {item.label}
              </span>
            </Link>
          ))}
        </div>
      </nav>
    </div>
  );
}
