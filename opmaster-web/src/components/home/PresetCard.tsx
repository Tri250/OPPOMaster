import { motion } from 'framer-motion';
import { Heart, Star, Sparkles, Camera, Check } from 'lucide-react';
import type { Preset } from '../../data/mockPresets';
import { useAppStore } from '../../store/useAppStore';
import { useNavigate } from 'react-router-dom';

const easeOppoEnter = [0.05, 0.7, 0.1, 1.0] as [number, number, number, number];

interface PresetCardProps {
  preset: Preset;
  index: number;
}

export default function PresetCard({ preset, index }: PresetCardProps) {
  const { toggleFavorite, setSelectedPreset, isFavorite: checkFavorite } = useAppStore();
  const navigate = useNavigate();
  const isFavorite = checkFavorite(preset.id);

  const handleClick = () => {
    setSelectedPreset(preset);
    navigate(`/preset/${preset.id}`);
  };

  const handleApply = (e: React.MouseEvent) => {
    e.stopPropagation();
    alert(`已将 "${preset.name}" 影像参数应用到您的设备`);
  };

  const handleFavorite = (e: React.MouseEvent) => {
    e.stopPropagation();
    toggleFavorite(preset.id);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 25, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ delay: index * 0.06, ease: easeOppoEnter, duration: 0.5 }}
      whileHover={{ y: -6, scale: 1.03 }}
      whileTap={{ scale: 0.98 }}
      className={`${preset.cameraParams?.hncs ? 'card-hncs' : 'card-oppo-interactive'} cursor-pointer group overflow-hidden relative`}
      onClick={handleClick}
    >
      {/* 图片区域 */}
      <div className="relative aspect-[3/4] overflow-hidden">
        <img
          src={preset.coverPath}
          alt={preset.name}
          className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-115"
          loading="lazy"
        />
        
        {/* 渐变遮罩 */}
        <div className="absolute inset-0 bg-gradient-to-t from-bg-primary/95 via-bg-primary/20 to-transparent" />
        
        {/* 顶部标签 */}
        <div className="absolute top-3.5 left-3.5 right-3.5 flex items-center justify-between">
          <div className="flex items-center gap-2">
            {/* 哈苏认证标签 */}
            {preset.cameraParams?.hncs && (
              <motion.div 
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ delay: index * 0.06 + 0.2, type: 'spring', stiffness: 300 }}
                className="glass-effect-light px-3 py-1.5 rounded-full flex items-center gap-1.5 shadow-oppo-elevation-2"
              >
                <Star className="w-3.5 h-3.5 text-hasselblad-orange fill-hasselblad-orange" />
                <span className="text-caption font-bold text-hasselblad-orange">哈苏认证</span>
              </motion.div>
            )}
            {/* 精选标签 */}
            {preset.isFeatured && (
              <div className="glass-effect-light px-2.5 py-1.5 rounded-full">
                <span className="text-caption text-text-primary/90 flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-oppo-orange" />
                  精选
                </span>
              </div>
            )}
          </div>
          {/* NEW标签 */}
          {preset.isNew && (
            <motion.div
              initial={{ scale: 0, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: index * 0.06 + 0.3, type: 'spring', stiffness: 300 }}
              className="bg-gradient-to-r from-oppo-orange to-hasselblad-orange px-3 py-1.5 rounded-full flex items-center gap-1.5 shadow-oppo-elevation-2 animate-breathing"
            >
              <Sparkles className="w-3.5 h-3.5 text-oppo-black" />
              <span className="text-caption font-bold text-oppo-black">NEW</span>
            </motion.div>
          )}
        </div>
        
        {/* 收藏按钮 */}
        <motion.button
          whileHover={{ scale: 1.15 }}
          whileTap={{ scale: 0.9 }}
          onClick={handleFavorite}
          className="absolute top-3.5 right-3.5 w-9 h-9 glass-effect-light rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-300 ease-out-elastic shadow-oppo-elevation-1"
        >
          <Heart
            className={`w-4.5 h-4.5 transition-all duration-300 ${
              isFavorite
                ? 'text-oppo-orange fill-oppo-orange scale-110'
                : 'text-text-primary'
            }`}
          />
        </motion.button>
        
        {/* 内容区域 */}
        <div className="absolute bottom-0 left-0 right-0 p-4.5">
          <h3 className="text-h2 font-bold mb-1.5 group-hover:text-oppo-orange transition-colors line-clamp-2">
            {preset.name}
          </h3>
          
          {/* 作者信息 */}
          {preset.author && (
            <p className="text-caption text-text-tertiary mb-3">
              by {preset.author}
            </p>
          )}
          
          {/* 底部信息栏 */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Camera className="w-4 h-4 text-text-secondary" />
              <span className="text-caption text-text-secondary bg-white/8 px-2.5 py-1 rounded-full backdrop-blur-sm">
                {preset.deviceModel}
              </span>
            </div>
            
            <motion.button
              whileHover={{ scale: 1.08 }}
              whileTap={{ scale: 0.95 }}
              onClick={handleApply}
              className={`text-caption font-bold px-3.5 py-1.5 rounded-full transition-all duration-300 ease-out-elastic flex items-center gap-1.5 ${
                preset.cameraParams?.hncs 
                  ? 'bg-gradient-to-r from-hasselblad-orange to-hasselblad-gold text-oppo-black hover:shadow-oppo-glow-orange' 
                  : 'bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-oppo-black hover:shadow-oppo-glow-orange'
              }`}
            >
              <Check className="w-3.5 h-3.5" />
              应用
            </motion.button>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
