import { motion } from 'framer-motion';
import { Camera, Download, Palette, Type } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';

const watermarkTemplates = [
  { id: 'hasselblad', name: '哈苏风格', color: '#D4A574', bgColor: 'rgba(212, 165, 116, 0.1)' },
  { id: 'oppo', name: 'OPPO风格', color: '#00D7A0', bgColor: 'rgba(0, 215, 160, 0.1)' },
  { id: 'oneplus', name: 'OnePlus风格', color: '#FF3333', bgColor: 'rgba(255, 51, 51, 0.1)' },
  { id: 'realme', name: 'realme风格', color: '#FFC107', bgColor: 'rgba(255, 193, 7, 0.1)' },
  { id: 'minimal', name: '简约参数', color: '#FFFFFF', bgColor: 'rgba(255, 255, 255, 0.05)' },
  { id: 'film', name: '胶片风格', color: '#8B7355', bgColor: 'rgba(139, 115, 85, 0.1)' }
];

export default function WatermarkGenerator() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState(watermarkTemplates[0]);
  const [deviceName, setDeviceName] = useState('Find X9spro');
  const [lensInfo, setLensInfo] = useState('24mm f/1.8');
  const [showWatermark, setShowWatermark] = useState(true);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setSelectedImage(event.target?.result as string);
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
      };
      reader.readAsDataURL(file);
    }
  };

  useEffect(() => {
    if (selectedImage && canvasRef.current) {
      const canvas = canvasRef.current;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      const img = new window.Image();
      img.src = selectedImage;
      img.onload = () => {
        // 设置canvas大小（限制最大尺寸）
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

        // 绘制图片
        ctx.drawImage(img, 0, 0, width, height);

        // 如果显示水印
        if (showWatermark) {
          const currentDate = new Date().toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
          });

          // 水印背景
          ctx.fillStyle = selectedTemplate.bgColor;
          ctx.fillRect(width - 280, height - 140, 270, 130);

          // 水印边框
          ctx.strokeStyle = selectedTemplate.color;
          ctx.lineWidth = 1;
          ctx.strokeRect(width - 280, height - 140, 270, 130);

          // 设备名称
          ctx.fillStyle = selectedTemplate.color;
          ctx.font = 'bold 16px Arial';
          ctx.fillText(deviceName, width - 260, height - 105);

          // 镜头参数
          ctx.font = '12px Arial';
          ctx.fillStyle = 'rgba(255,255,255,0.8)';
          ctx.fillText(lensInfo, width - 260, height - 80);

          // 分隔线
          ctx.beginPath();
          ctx.moveTo(width - 260, height - 70);
          ctx.lineTo(width - 20, height - 70);
          ctx.strokeStyle = selectedTemplate.color;
          ctx.stroke();

          // 日期
          ctx.font = '11px Arial';
          ctx.fillStyle = 'rgba(255,255,255,0.7)';
          ctx.fillText(currentDate, width - 260, height - 52);

          // 品牌标识
          ctx.font = 'bold 14px Arial';
          ctx.fillStyle = selectedTemplate.color;
          ctx.fillText('小O帮帮', width - 260, height - 30);
        }
      };
    }
  }, [selectedImage, selectedTemplate, showWatermark, deviceName, lensInfo]);

  const handleDownload = () => {
    if (!canvasRef.current) return;
    
    const link = document.createElement('a');
    link.download = `watermarked_${Date.now()}.jpg`;
    link.href = canvasRef.current.toDataURL('image/jpeg', 0.9);
    link.click();
  };

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-green to-cyan-500 rounded-2xl mb-6">
            <Palette className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text">
            水印生成器
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            为您的照片添加专业水印 - 支持10+品牌风格
          </p>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Left: Upload & Preview */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            className="space-y-6"
          >
            {/* Upload Area */}
            <div className="card p-6">
              <h2 className="text-lg font-bold mb-4 flex items-center space-x-2">
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
              
              <div
                onClick={() => fileInputRef.current?.click()}
                onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-xl p-8 text-center transition-all cursor-pointer ${
                  isDragOver 
                    ? 'border-oppo-green bg-oppo-green/10' 
                    : 'border-white/20 hover:border-oppo-green hover:bg-oppo-green/5'
                }`}
              >
                <Camera className="w-12 h-12 mx-auto mb-3 text-white/40" />
                <p className="text-white/60">点击上传或拖拽图片</p>
                <p className="text-xs text-white/40 mt-1">支持 JPG、PNG、WebP 格式</p>
              </div>
            </div>

            {/* Preview Canvas */}
            <div className="card p-6">
              <h2 className="text-lg font-bold mb-4">预览效果</h2>
              <div className="relative bg-black/20 rounded-xl overflow-hidden">
                {selectedImage ? (
                  <canvas
                    ref={canvasRef}
                    className="w-full h-auto"
                  />
                ) : (
                  <div className="aspect-video flex items-center justify-center">
                    <p className="text-white/40">上传图片以预览水印效果</p>
                  </div>
                )}
              </div>
              
              {selectedImage && (
                <motion.button
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleDownload}
                  className="btn-primary w-full mt-4 flex items-center justify-center space-x-2"
                >
                  <Download className="w-5 h-5" />
                  <span>下载带水印图片</span>
                </motion.button>
              )}
            </div>
          </motion.div>

          {/* Right: Settings */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="space-y-6"
          >
            {/* Template Selection */}
            <div className="card p-6">
              <h2 className="text-lg font-bold mb-4 flex items-center space-x-2">
                <Palette className="w-5 h-5 text-hasselblad" />
                <span>选择水印模板</span>
              </h2>
              
              <div className="grid grid-cols-2 gap-3">
                {watermarkTemplates.map((template) => (
                  <motion.button
                    key={template.id}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setSelectedTemplate(template)}
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

            {/* Custom Settings */}
            <div className="card p-6">
              <h2 className="text-lg font-bold mb-4 flex items-center space-x-2">
                <Type className="w-5 h-5 text-oppo-green" />
                <span>自定义参数</span>
              </h2>
              
              <div className="space-y-4">
                {/* Device Name */}
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-2">
                    设备名称
                  </label>
                  <input
                    type="text"
                    value={deviceName}
                    onChange={(e) => setDeviceName(e.target.value)}
                    placeholder="例如：Find X9spro"
                    className="input"
                  />
                </div>

                {/* Lens Info */}
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-2">
                    镜头参数
                  </label>
                  <input
                    type="text"
                    value={lensInfo}
                    onChange={(e) => setLensInfo(e.target.value)}
                    placeholder="例如：24mm f/1.8"
                    className="input"
                  />
                </div>

                {/* Show Watermark Toggle */}
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-white/70">显示水印</span>
                  <button
                    onClick={() => setShowWatermark(!showWatermark)}
                    className={`relative w-12 h-6 rounded-full transition-colors ${
                      showWatermark ? 'bg-oppo-green' : 'bg-white/20'
                    }`}
                  >
                    <div
                      className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-transform ${
                        showWatermark ? 'translate-x-7' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              </div>
            </div>

            {/* Quick Presets */}
            <div className="card p-6">
              <h2 className="text-lg font-bold mb-4">快速预设</h2>
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => {
                    setDeviceName('Find X9spro');
                    setLensInfo('24mm f/1.8');
                    setSelectedTemplate(watermarkTemplates[0]);
                  }}
                  className="btn-secondary text-sm py-3"
                >
                  OPPO 旗舰
                </button>
                <button
                  onClick={() => {
                    setDeviceName('OnePlus 13');
                    setLensInfo('23mm f/1.8');
                    setSelectedTemplate(watermarkTemplates[2]);
                  }}
                  className="btn-secondary text-sm py-3"
                >
                  OnePlus
                </button>
                <button
                  onClick={() => {
                    setDeviceName('realme GT 6');
                    setLensInfo('26mm f/1.9');
                    setSelectedTemplate(watermarkTemplates[3]);
                  }}
                  className="btn-secondary text-sm py-3"
                >
                  realme
                </button>
                <button
                  onClick={() => {
                    setDeviceName('iPhone 16 Pro');
                    setLensInfo('24mm f/1.78');
                    setSelectedTemplate(watermarkTemplates[4]);
                  }}
                  className="btn-secondary text-sm py-3"
                >
                  iPhone
                </button>
              </div>
            </div>

            {/* Usage Tips */}
            <div className="card p-6 bg-gradient-to-br from-oppo-green/10 to-transparent">
              <h2 className="text-lg font-bold mb-3 flex items-center space-x-2">
                <span className="text-2xl">💡</span>
                <span>使用提示</span>
              </h2>
              <ul className="space-y-2 text-sm text-white/70">
                <li>• 支持拖拽或点击上传图片</li>
                <li>• 水印位置固定在右下角</li>
                <li>• 可自定义设备名称和镜头参数</li>
                <li>• 点击下载保存到本地</li>
                <li>• 支持多种品牌风格切换</li>
              </ul>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
