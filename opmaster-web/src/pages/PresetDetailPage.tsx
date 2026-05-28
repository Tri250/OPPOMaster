import { motion } from 'framer-motion';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Heart, Share2, Star, Camera, Sliders, Download, Tag } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { useState } from 'react';

export default function PresetDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { presets, toggleFavorite } = useAppStore();
  const [brightness, setBrightness] = useState(50);
  const [contrast, setContrast] = useState(50);
  const [saturation, setSaturation] = useState(50);

  const preset = presets.find(p => p.id === id);

  if (!preset) {
    return (
      <div className="min-h-screen pt-20 flex items-center justify-center">
        <div className="text-center">
          <p className="text-white/60 text-xl mb-4">预设不存在</p>
          <button onClick={() => navigate('/')} className="btn-primary">
            返回首页
          </button>
        </div>
      </div>
    );
  }

  const getParamDisplayValue = (value: string | number | undefined): string => {
    if (value === undefined || value === null) return '-';
    return String(value);
  };

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between mb-8"
        >
          <button
            onClick={() => navigate(-1)}
            className="flex items-center space-x-2 text-white/70 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span>返回</span>
          </button>
          
          <div className="flex items-center space-x-3">
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={() => toggleFavorite(preset.id)}
              className="p-3 glass-effect rounded-full"
            >
              <Heart
                className={`w-5 h-5 ${
                  preset.isFavorite ? 'text-red-500 fill-red-500' : 'text-white'
                }`}
              />
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              className="p-3 glass-effect rounded-full"
            >
              <Share2 className="w-5 h-5 text-white" />
            </motion.button>
          </div>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Preview Section */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="space-y-6"
          >
            {/* Main Preview */}
            <div className="relative aspect-[4/3] rounded-2xl overflow-hidden shadow-2xl">
              <img
                src={preset.coverPath}
                alt={preset.name}
                className="w-full h-full object-cover"
                style={{
                  filter: `brightness(${brightness / 50}) contrast(${contrast / 50}) saturate(${saturation / 50})`
                }}
              />
              
              {/* Overlay */}
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
              
              {/* Badges */}
              <div className="absolute top-4 left-4 flex items-center space-x-2 flex-wrap gap-2">
                {preset.cameraParams?.hncs && (
                  <div className="glass-effect px-3 py-1 rounded-full flex items-center space-x-1">
                    <Star className="w-4 h-4 text-hasselblad fill-hasselblad" />
                    <span className="text-sm font-bold text-hasselblad">HNCS</span>
                  </div>
                )}
                {preset.category && (
                  <div className="glass-effect px-3 py-1 rounded-full">
                    <span className="text-sm font-medium">{preset.category}</span>
                  </div>
                )}
                <div className="glass-effect px-3 py-1 rounded-full">
                  <span className="text-sm font-medium">{preset.deviceModel}</span>
                </div>
              </div>
            </div>

            {/* Gallery Images */}
            {preset.galleryImages && preset.galleryImages.length > 0 && (
              <div className="grid grid-cols-3 gap-3">
                {preset.galleryImages.map((img, idx) => (
                  <motion.div
                    key={idx}
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: 0.2 + idx * 0.05 }}
                    className="aspect-square rounded-xl overflow-hidden"
                  >
                    <img
                      src={img}
                      alt={`${preset.name} sample ${idx + 1}`}
                      className="w-full h-full object-cover"
                    />
                  </motion.div>
                ))}
              </div>
            )}

            {/* Tags */}
            {preset.tags && preset.tags.length > 0 && (
              <div className="flex flex-wrap gap-2">
                <Tag className="w-4 h-4 text-white/50" />
                {preset.tags.map((tag, idx) => (
                  <span
                    key={idx}
                    className="text-xs bg-white/10 px-2 py-1 rounded-full text-white/70"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            )}

            {/* Actions */}
            <div className="flex gap-4">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="btn-primary flex-1 flex items-center justify-center space-x-2"
              >
                <Download className="w-5 h-5" />
                <span>应用预设</span>
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="btn-secondary flex-1 flex items-center justify-center space-x-2"
              >
                <Camera className="w-5 h-5" />
                <span>保存到相机</span>
              </motion.button>
            </div>
          </motion.div>

          {/* Details Section */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="space-y-6"
          >
            {/* Title */}
            <div>
              <h1 className="text-3xl font-bold mb-2">{preset.name}</h1>
              <p className="text-white/60">
                适用于 {preset.deviceModel}
                {preset.author && (
                  <span className="text-oppo-green ml-2">by {preset.author}</span>
                )}
              </p>
            </div>

            {/* Camera Parameters */}
            {preset.cameraParams && (
              <div className="card p-6">
                <h2 className="text-lg font-bold mb-4 flex items-center space-x-2">
                  <Sliders className="w-5 h-5 text-hasselblad" />
                  <span>哈苏大师模式参数</span>
                </h2>
                
                <div className="grid grid-cols-2 gap-4">
                  {[
                    { label: '模式', value: preset.cameraParams.mode },
                    { label: '滤镜', value: `${preset.cameraParams.filter} ${preset.cameraParams.filter_intensity}%` },
                    { label: '柔光', value: preset.cameraParams.soft_light },
                    { label: '色调曲线', value: preset.cameraParams.tone_curve },
                    { label: '饱和度', value: `${preset.cameraParams.saturation}%` },
                    { label: '冷暖调', value: preset.cameraParams.warm_cool },
                    { label: '青红调', value: preset.cameraParams.cyan_magenta },
                    { label: '锐度', value: preset.cameraParams.sharpness },
                    { label: '暗角', value: preset.cameraParams.vignette ? '开启' : '关闭' },
                    { label: 'ISO', value: getParamDisplayValue(preset.cameraParams.iso) },
                    { label: '快门', value: getParamDisplayValue(preset.cameraParams.shutter_speed) },
                    { label: '曝光补偿', value: getParamDisplayValue(preset.cameraParams.exposure_compensation) },
                    { label: '自定义白平衡', value: getParamDisplayValue(preset.cameraParams.custom_wb ? `${preset.cameraParams.custom_wb}K` : undefined) }
                  ].filter(p => p.value !== '-').map((param) => (
                    <div key={param.label} className="bg-white/5 rounded-lg p-3">
                      <span className="text-xs text-white/50 block">{param.label}</span>
                      <span className="text-sm font-medium">{param.value}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Adjustment Sliders */}
            <div className="card p-6">
              <h2 className="text-lg font-bold mb-4">实时预览调节</h2>
              
              <div className="space-y-6">
                {/* Brightness */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium">亮度</span>
                    <span className="text-sm text-white/60">{brightness}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={brightness}
                    onChange={(e) => setBrightness(Number(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer slider"
                    style={{
                      background: `linear-gradient(to right, #D4A574 ${brightness}%, rgba(255,255,255,0.1) ${brightness}%)`
                    }}
                  />
                </div>

                {/* Contrast */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium">对比度</span>
                    <span className="text-sm text-white/60">{contrast}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={contrast}
                    onChange={(e) => setContrast(Number(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer slider"
                    style={{
                      background: `linear-gradient(to right, #D4A574 ${contrast}%, rgba(255,255,255,0.1) ${contrast}%)`
                    }}
                  />
                </div>

                {/* Saturation */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium">饱和度</span>
                    <span className="text-sm text-white/60">{saturation}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={saturation}
                    onChange={(e) => setSaturation(Number(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer slider"
                    style={{
                      background: `linear-gradient(to right, #D4A574 ${saturation}%, rgba(255,255,255,0.1) ${saturation}%)`
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Description */}
            {(preset.sections.length > 0 || preset.description) && (
              <div className="card p-6">
                <h2 className="text-lg font-bold mb-4">使用说明</h2>
                <div className="space-y-4">
                  {preset.description && (
                    <div>
                      <h3 className="text-sm font-bold text-hasselblad mb-1">{preset.description.title}</h3>
                      <p className="text-sm text-white/70 whitespace-pre-wrap">{preset.description.content}</p>
                    </div>
                  )}
                  {preset.sections.map((section, idx) => (
                    <div key={idx}>
                      <h3 className="text-sm font-bold text-hasselblad mb-2">{section.title}</h3>
                      <div className="space-y-2">
                        {section.items.map((item, itemIdx) => (
                          <div key={itemIdx} className="flex justify-between bg-white/5 px-3 py-2 rounded">
                            <span className="text-sm text-white/60">{item.label}</span>
                            <span className="text-sm font-medium">{item.value}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </motion.div>
        </div>
      </div>

      <style>{`
        .slider::-webkit-slider-thumb {
          -webkit-appearance: none;
          width: 16px;
          height: 16px;
          background: #D4A574;
          border-radius: 50%;
          cursor: pointer;
          box-shadow: 0 2px 8px rgba(212, 165, 116, 0.4);
        }
        
        .slider::-moz-range-thumb {
          width: 16px;
          height: 16px;
          background: #D4A574;
          border-radius: 50%;
          cursor: pointer;
          border: none;
          box-shadow: 0 2px 8px rgba(212, 165, 116, 0.4);
        }
      `}</style>
    </div>
  );
}
