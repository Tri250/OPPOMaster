import { motion } from 'framer-motion';
import { Heart, Star, Sparkles, Camera } from 'lucide-react';
import type { Preset } from '../../data/mockPresets';
import { useAppStore } from '../../store/useAppStore';
import { useNavigate } from 'react-router-dom';

const easeOppoEnter: [number, number, number, number] = [0.05, 0.7, 0.1, 1.0];

interface PresetCardProps {
  preset: Preset;
  index: number;
}

export default function PresetCard({ preset, index }: PresetCardProps) {
  const { toggleFavorite, setSelectedPreset } = useAppStore();
  const navigate = useNavigate();

  const handleClick = () => {
    setSelectedPreset(preset);
    navigate(`/preset/${preset.id}`);
  };

  const handleApply = (e: React.MouseEvent) => {
    e.stopPropagation();
    alert(`已将 "${preset.name}" 影像参数应用到您的设备`);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, ease: easeOppoEnter, duration: 0.4 }}
      whileHover={{ y: -4, scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      className={`preset-card ${preset.cameraParams?.hncs ? 'card-hncs' : 'card-oppo-interactive'} cursor-pointer group overflow-hidden`}
      onClick={handleClick}
    >
      {/* Image */}
      <div className="relative aspect-[3/4] overflow-hidden">
        <img
          src={preset.coverPath}
          alt={preset.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500 ease-out-cubic"
          loading="lazy"
        />
        
        {/* Gradient Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-oppo-black/90 via-oppo-black/20 to-transparent" />
        
        {/* Top Badges */}
        <div className="absolute top-3 left-3 right-3 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            {preset.cameraParams?.hncs && (
              <div className="glass-effect px-2.5 py-1 rounded-full flex items-center space-x-1.5 shadow-oppo-elevation-1">
                <Star className="w-3 h-3 text-hasselblad-orange fill-hasselblad-orange" />
                <span className="text-caption font-bold text-hasselblad-orange">哈苏</span>
              </div>
            )}
            {preset.category && (
              <div className="glass-effect px-2 py-1 rounded-full shadow-oppo-elevation-1">
                <span className="text-caption text-text-primary/90">{preset.category}</span>
              </div>
            )}
          </div>
          {preset.isNew && (
            <div className="bg-gradient-to-r from-oppo-orange to-hasselblad-orange px-2.5 py-1 rounded-full flex items-center space-x-1 shadow-oppo-elevation-2 animate-breathing">
              <Sparkles className="w-3 h-3 text-oppo-black" />
              <span className="text-caption font-bold text-oppo-black">NEW</span>
            </div>
          )}
        </div>
        
        {/* Favorite Button */}
        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          onClick={(e) => {
            e.stopPropagation();
            toggleFavorite(preset.id);
          }}
          className="absolute top-3 right-3 w-8 h-8 glass-effect rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300 shadow-oppo-elevation-1"
        >
          <Heart
            className={`w-4 h-4 transition-colors ${
              preset.isFavorite
                ? 'text-oppo-orange fill-oppo-orange'
                : 'text-text-primary'
            }`}
          />
        </motion.button>
        
        {/* Content Overlay */}
        <div className="absolute bottom-0 left-0 right-0 p-4">
          <h3 className="text-h3 font-bold mb-1.5 group-hover:text-oppo-orange transition-colors line-clamp-2">
            {preset.name}
          </h3>
          
          {preset.author && (
            <p className="text-caption text-text-tertiary mb-2">
              by {preset.author}
            </p>
          )}
          
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Camera className="w-3.5 h-3.5 text-text-secondary" />
              <span className="text-caption text-text-secondary bg-white/10 px-2.5 py-1 rounded-full">
                {preset.deviceModel}
              </span>
            </div>
            
            <button 
              onClick={handleApply}
              className={`text-caption font-semibold px-3 py-1 rounded-full transition-all duration-200 ease-out-cubic ${
                preset.cameraParams?.hncs 
                  ? 'bg-hasselblad-orange text-oppo-black hover:bg-hasselblad-orange/90 shadow-oppo-elevation-1' 
                  : 'bg-oppo-orange text-oppo-black hover:bg-oppo-orange-dark shadow-oppo-elevation-1'
              }`}
            >
              应用
            </button>
          </div>
        </div>
      </div>
    </motion.div>
  );
}

