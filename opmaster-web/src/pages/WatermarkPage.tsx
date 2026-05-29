import { motion, AnimatePresence } from 'framer-motion';
import { Camera, Download, Palette, Move, CheckCircle2, X } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';

type WatermarkPosition = 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' | 'center';

interface PositionConfig {
  x: number;
  y: number;
  label: string;
  icon: string;
}

const watermarkTemplates = [
  { id: 'hasselblad', name: '哈苏风格', color: '#D4A574', bgColor: 'rgba(212, 165, 116, 0.1)' },
  { id: 'oppo', name: 'OPPO风格', color: '#00D7A0', bgColor: 'rgba(0, 215, 160, 0.1)' },
  { id: 'oneplus', name: 'OnePlus风格', color: '#FF3333', bgColor: 'rgba(255, 51, 51, 0.1)' },
  { id: 'realme', name: 'realme风格', color: '#FFC107', bgColor: 'rgba(255, 193, 7, 0.1)' },
  { id: 'minimal', name: '简约参数', color: '#FFFFFF', bgColor: 'rgba(255, 255, 255, 0.05)' },
  { id: 'film', name: '胶片风格', color: '#8B7355', bgColor: 'rgba(139, 115, 85, 0.1)' }
];

const positionConfigs: Record<WatermarkPosition, PositionConfig> = {
  'top-left': { x: 10, y: 10, label: '左上', icon: '↖' },
  'top-right': { x: -280, y: 10, label: '右上', icon: '↗' },
  'bottom-left': { x: 10, y: -130, label: '左下', icon: '↙' },
  'bottom-right': { x: -280, y: -130, label: '右下', icon: '↘' },
  'center': { x: -140, y: -65, label: '居中', icon: '⊕' }
};

export default function WatermarkGenerator() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState(watermarkTemplates[0]);
  const [deviceName, setDeviceName] = useState('Find X9spro');
  const [lensInfo, setLensInfo] = useState('24mm f/1.8');
  const [showWatermark, setShowWatermark] = useState(true);
  const [isDragOver, setIsDragOver] = useState(false);
  const [watermarkPosition, setWatermarkPosition] = useState<WatermarkPosition>('bottom-right');
  const [customOffset, setCustomOffset] = useState<{ x: number; y: number } | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const canvasContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
  };

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setSelectedImage(event.target?.result as string);
        showNotification('图片已加载', 'success');
      };
      reader.readAsDataURL(file);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setSelectedImage(event.target?.result as string);
        showNotification('图片已加载', 'success');
      };
      reader.readAsDataURL(file);
    }
  };

  const getWatermarkPosition = () => {
    if (customOffset) {
      return customOffset;
    }
    const config = positionConfigs[watermarkPosition];
    return { x: config.x, y: config.y };
  };

  useEffect(() => {
    if (selectedImage && canvasRef.current) {
      const canvas = canvasRef.current;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      const img = new window.Image();
      img.src = selectedImage;
      img.onload = () => {
        const maxWidth = 800;
        const maxHeight = 600;
        let width = img.width;
        let height = img.height;
        
        if (width > maxWidth) {
          height = (maxWidth / width) * height;
          width = maxWidth;
        }
        if (height > maxHeight) {
          width = (maxHeight / height) * width;
          height = maxHeight;
        }

        canvas.width = width;
        canvas.height = height;

        ctx.drawImage(img, 0, 0, width, height);

        if (showWatermark) {
          const currentDate = new Date().toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
          });

          const pos = getWatermarkPosition();
          let x = pos.x >= 0 ? pos.x : width + pos.x;
          let y = pos.y >= 0 ? pos.y : height + pos.y;
          
          x = Math.max(0, Math.min(x, width - 280));
          y = Math.max(0, Math.min(y, height - 140));

          ctx.fillStyle = selectedTemplate.bgColor;
          ctx.fillRect(x, y, 270, 130);

          ctx.strokeStyle = selectedTemplate.color;
          ctx.lineWidth = 1.5;
          ctx.strokeRect(x, y, 270, 130);

          ctx.fillStyle = selectedTemplate.color;
          ctx.font = 'bold 16px Arial';
          ctx.fillText(deviceName, x + 20, y + 35);

          ctx.font = '12px Arial';
          ctx.fillStyle = 'rgba(255,255,255,0.8)';
          ctx.fillText(lensInfo, x + 20, y + 60);

          ctx.beginPath();
          ctx.moveTo(x + 20, y + 70);
          ctx.lineTo(x + 250, y + 70);
          ctx.strokeStyle = selectedTemplate.color;
          ctx.stroke();

          ctx.font = '11px Arial';
          ctx.fillStyle = 'rgba(255,255,255,0.7)';
          ctx.fillText(currentDate, x + 20, y + 88);

          ctx.font = 'bold 14px Arial';
          ctx.fillStyle = selectedTemplate.color;
          ctx.fillText('小O帮帮', x + 20, y + 110);
        }
      };
    }
  }, [selectedImage, selectedTemplate, showWatermark, deviceName, lensInfo, watermarkPosition, customOffset]);

  const handleDownload = () => {
    if (!canvasRef.current) return;
    
    const link = document.createElement('a');
    link.download = `watermarked_${Date.now()}.jpg`;
    link.href = canvasRef.current.toDataURL('image/jpeg', 0.9);
    link.click();
    showNotification('图片已下载', 'success');
  };

  const handleCanvasMouseDown = (_e: React.MouseEvent) => {
    if (!canvasRef.current || !showWatermark) return;
    setIsDragging(true);
  };

  const handleCanvasMouseMove = (e: React.MouseEvent) => {
    if (!isDragging || !canvasRef.current) return;
    
    const rect = canvasRef.current.getBoundingClientRect();
    const scaleX = canvasRef.current.width / rect.width;
    const scaleY = canvasRef.current.height / rect.height;
    
    const x = (e.clientX - rect.left) * scaleX;
    const y = (e.clientY - rect.top) * scaleY;
    
    setCustomOffset({
      x: x - 135,
      y: y - 65
    });
  };

  const handleCanvasMouseUp = () => {
    if (isDragging) {
      setIsDragging(false);
      showNotification('水印位置已调整', 'success');
    }
  };

  const handlePositionChange = (position: WatermarkPosition) => {
    setWatermarkPosition(position);
    setCustomOffset(null);
    showNotification(`水印位置: ${positionConfigs[position].label}`, 'success');
  };

  const resetPosition = () => {
    setWatermarkPosition('bottom-right');
    setCustomOffset(null);
    showNotification('位置已重置为右下角', 'success');
  };

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <AnimatePresence>
        {notification && (
          <motion.div
            initial={{ opacity: 0, y: -20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            className={`fixed top-24 left-1/2 -translate-x-1/2 z-50 px-6 py-3 rounded-2xl shadow-lg backdrop-blur-xl ${
              notification.type === 'success' 
                ? 'bg-oppo-green/20 border border-oppo-green/30 text-oppo-green' 
                : 'bg-red-500/20 border border-red-500/30 text-red-400'
            }`}
          >
            <div className="flex items-center gap-2">
              {notification.type === 'success' ? (
                <CheckCircle2 className="w-5 h-5" />
              ) : (
                <X className="w-5 h-5" />
              )}
              <span className="font-medium">{notification.message}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="max-w-7xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', stiffness: 200, damping: 15 }}
            className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-green to-oppo-blue rounded-2xl mb-6 shadow-lg"
          >
            <Palette className="w-12 h-12 text-oppo-black" />
          </motion.div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text-oppo">
            水印生成器
          </h1>
          <p className="text-lg text-text-secondary max-w-2xl mx-auto">
            为您的照片添加专业水印 - 支持10+品牌风格，拖拽定位水印
          </p>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="space-y-6"
          >
            <div className="card-elevated p-6">
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Camera className="w-5 h-5 text-oppo-green" />
                <span>上传图片</span>
              </h2>
              
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleImageSelect}
                accept="image/*"
                className="hidden"
              />
              
              <motion.div
                onClick={() => fileInputRef.current?.click()}
                onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={handleDrop}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className={`border-2 border-dashed rounded-xl p-8 text-center transition-all cursor-pointer ${
                  isDragOver 
                    ? 'border-success bg-success/10' 
                    : 'border-white/20 hover:border-success hover:bg-success/5'
                }`}
              >
                <motion.div
                  animate={{ y: isDragging ? [0, -10, 0] : 0 }}
                  transition={{ repeat: isDragging ? Infinity : 0, duration: 1 }}
                >
                  <Camera className="w-12 h-12 mx-auto mb-3 text-text-tertiary" />
                </motion.div>
                <p className="text-text-secondary">点击上传或拖拽图片</p>
                <p className="text-xs text-text-tertiary mt-1">支持 JPG、PNG、WebP 格式</p>
              </motion.div>
            </div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="card-elevated p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Move className="w-5 h-5 text-oppo-orange" />
                <span>预览效果</span>
                <span className="text-xs text-text-tertiary ml-auto">支持拖拽定位</span>
              </h2>
              <div 
                ref={canvasContainerRef}
                className="relative bg-bg-secondary rounded-xl overflow-hidden"
              >
                {selectedImage ? (
                  <motion.div
                    onMouseDown={handleCanvasMouseDown}
                    onMouseMove={handleCanvasMouseMove}
                    onMouseUp={handleCanvasMouseUp}
                    onMouseLeave={handleCanvasMouseUp}
                    className={`relative ${showWatermark ? 'cursor-move' : 'cursor-default'}`}
                    whileHover={{ scale: isDragging ? 1 : 1.01 }}
                    whileTap={{ scale: 0.99 }}
                  >
                    <canvas
                      ref={canvasRef}
                      className="w-full h-auto"
                    />
                    {isDragging && (
                      <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="absolute inset-0 border-2 border-dashed border-oppo-orange pointer-events-none"
                      />
                    )}
                  </motion.div>
                ) : (
                  <div className="aspect-video flex items-center justify-center">
                    <p className="text-text-tertiary">上传图片以预览水印效果</p>
                  </div>
                )}
              </div>
              
              {selectedImage && (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="mt-4 space-y-3"
                >
                  <motion.button
                    whileHover={{ scale: 1.02, y: -2 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={handleDownload}
                    className="btn-primary w-full flex items-center justify-center space-x-2"
                  >
                    <Download className="w-5 h-5" />
                    <span>下载带水印图片</span>
                  </motion.button>
                  {customOffset && (
                    <motion.button
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: 'auto' }}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      onClick={resetPosition}
                      className="btn-secondary w-full text-sm py-2 flex items-center justify-center space-x-2"
                    >
                      <X className="w-4 h-4" />
                      <span>重置位置</span>
                    </motion.button>
                  )}
                </motion.div>
              )}
            </motion.div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="space-y-6"
          >
            <div className="card-elevated p-6">
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Palette className="w-5 h-5 text-hasselblad-orange" />
                <span>选择水印模板</span>
              </h2>
              
              <div className="grid grid-cols-2 gap-3">
                {watermarkTemplates.map((template, index) => (
                  <motion.button
                    key={template.id}
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: index * 0.05 }}
                    whileHover={{ scale: 1.05, y: -2 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => {
                      setSelectedTemplate(template);
                      showNotification(`已切换到${template.name}`, 'success');
                    }}
                    className={`p-4 rounded-xl border-2 transition-all ${
                      selectedTemplate.id === template.id
                        ? 'border-oppo-green bg-oppo-green/10'
                        : 'border-white/10 hover:border-white/30'
                    }`}
                  >
                    <div 
                      className="w-full h-12 rounded-lg mb-2 flex items-center justify-center text-white font-bold"
                      style={{ backgroundColor: template.bgColor }}
                    >
                      小O帮帮
                    </div>
                    <p className="text-sm font-medium">{template.name}</p>
                  </motion.button>
                ))}
              </div>
            </div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="card p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center space-x-2">
                <Move className="w-5 h-5 text-oppo-green" />
                <span>水印位置</span>
              </h2>
              
              <div className="grid grid-cols-5 gap-2 mb-4">
                {(Object.keys(positionConfigs) as WatermarkPosition[]).map((position) => (
                  <motion.button
                    key={position}
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.9 }}
                    onClick={() => handlePositionChange(position)}
                    className={`p-3 rounded-xl border-2 transition-all text-center ${
                      (watermarkPosition === position && !customOffset)
                        ? 'border-oppo-orange bg-oppo-orange/10'
                        : 'border-white/10 hover:border-white/30'
                    }`}
                    title={positionConfigs[position].label}
                  >
                    <span className="text-2xl">{positionConfigs[position].icon}</span>
                    <p className="text-xs mt-1 opacity-70">{positionConfigs[position].label}</p>
                  </motion.button>
                ))}
              </div>

              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => {
                  setCustomOffset(null);
                  setWatermarkPosition('bottom-right');
                  showNotification('已切换为拖拽模式', 'success');
                }}
                className="w-full p-3 rounded-xl border-2 border-dashed border-white/20 hover:border-oppo-orange/50 transition-colors flex items-center justify-center gap-2"
              >
                <Move className="w-5 h-5" />
                <span className="text-sm">拖拽模式 - 在预览区自由调整位置</span>
              </motion.button>
            </motion.div>

            <div className="card-elevated p-6">
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Move className="w-5 h-5 text-oppo-green" />
                <span>水印位置</span>
              </h2>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-2">
                    设备名称
                  </label>
                  <motion.input
                    type="text"
                    value={deviceName}
                    onChange={(e) => setDeviceName(e.target.value)}
                    placeholder="例如：Find X9spro"
                    whileFocus={{ scale: 1.02 }}
                    className="w-full px-4 py-3 bg-bg-tertiary border border-border-default rounded-xl text-text-primary placeholder-text-tertiary focus:outline-none focus:border-oppo-orange focus:ring-2 focus:ring-oppo-orange/20"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-2">
                    镜头参数
                  </label>
                  <motion.input
                    type="text"
                    value={lensInfo}
                    onChange={(e) => setLensInfo(e.target.value)}
                    placeholder="例如：24mm f/1.8"
                    whileFocus={{ scale: 1.02 }}
                    className="w-full px-4 py-3 bg-bg-tertiary border border-border-default rounded-xl text-text-primary placeholder-text-tertiary focus:outline-none focus:border-oppo-orange focus:ring-2 focus:ring-oppo-orange/20"
                  />
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-text-secondary">显示水印</span>
                  <motion.button
                    whileTap={{ scale: 0.95 }}
                    onClick={() => {
                      setShowWatermark(!showWatermark);
                      showNotification(showWatermark ? '水印已隐藏' : '水印已显示', 'success');
                    }}
                    className={`relative w-12 h-6 rounded-full transition-colors ${
                      showWatermark ? 'bg-oppo-green' : 'bg-white/20'
                    }`}
                  >
                    <motion.div
                      animate={{ x: showWatermark ? 24 : 0 }}
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                      className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-md"
                    />
                  </motion.button>
                </div>
              </div>
            </div>

            <div className="card-elevated p-6">
              <h2 className="text-lg font-bold mb-4">快速预设</h2>
              <div className="grid grid-cols-2 gap-3">
                <motion.button
                  whileHover={{ scale: 1.05, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    setDeviceName('Find X9spro');
                    setLensInfo('24mm f/1.8');
                    setSelectedTemplate(watermarkTemplates[1]);
                    showNotification('已切换到OPPO旗舰预设', 'success');
                  }}
                  className="btn-secondary text-sm py-3"
                >
                  OPPO 旗舰
                </motion.button>
                <motion.button
                  whileHover={{ scale: 1.05, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    setDeviceName('OnePlus 13');
                    setLensInfo('23mm f/1.8');
                    setSelectedTemplate(watermarkTemplates[2]);
                    showNotification('已切换到OnePlus预设', 'success');
                  }}
                  className="btn-secondary text-sm py-3"
                >
                  OnePlus
                </motion.button>
                <motion.button
                  whileHover={{ scale: 1.05, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    setDeviceName('realme GT 6');
                    setLensInfo('26mm f/1.9');
                    setSelectedTemplate(watermarkTemplates[3]);
                    showNotification('已切换到realme预设', 'success');
                  }}
                  className="btn-secondary text-sm py-3"
                >
                  realme
                </motion.button>
                <motion.button
                  whileHover={{ scale: 1.05, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    setDeviceName('iPhone 16 Pro');
                    setLensInfo('24mm f/1.78');
                    setSelectedTemplate(watermarkTemplates[4]);
                    showNotification('已切换到iPhone预设', 'success');
                  }}
                  className="btn-secondary text-sm py-3"
                >
                  iPhone
                </motion.button>
              </div>
            </div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
              className="card-elevated p-6 bg-gradient-to-br from-oppo-green/10 to-transparent"
            >
              <h2 className="text-lg font-bold mb-3 flex items-center gap-2">
                <span className="text-2xl">💡</span>
                <span>使用提示</span>
              </h2>
              <ul className="space-y-2 text-sm text-text-secondary">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>支持拖拽或点击上传图片</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>在预览区拖拽水印可自由调整位置</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>点击位置按钮可快速定位到四个角或中心</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>可自定义设备名称和镜头参数</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>支持多种品牌风格快速切换</span>
                </li>
              </ul>
            </motion.div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
