import { motion } from 'framer-motion';
import { Heart, Star, Sparkles, Camera } from 'lucide-react';
import type { Preset } from '../../data/mockPresets';
import { useAppStore } from '../../store/useAppStore';
import { useNavigate } from 'react-router-dom';

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
      transition={{ delay: index * 0.05, ease: [0.4, 0, 0.2, 1] }}
      whileHover={{ y: -6, scale: 1.02 }}
      className="card-oppo cursor-pointer group overflow-hidden"
      onClick={handleClick}
    >
      {/* Image */}
      <div className="relative aspect-[3/4] overflow-hidden">
        <motion.img
          src={preset.coverPath}
          alt={preset.name}
          className="w-full h-full object-cover"
          loading="lazy"
          whileHover={{ scale: 1.1 }}
          transition={{ duration: 0.5 }}
        />
        
        {/* Gradient Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent" />
        
        {/* Top Badges */}
        <div className="absolute top-3 left-3 right-3 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            {preset.cameraParams?.hncs && (
              <motion.div
                whileHover={{ scale: 1.05 }}
                className="tag-hasselblad px-2.5 py-1 flex items-center space-x-1.5"
              >
                <Star className="w-3 h-3 text-deep-space fill-deep-space" />
                <span className="text-xs font-bold text-deep-space">HNCS</span>
              </motion.div>
            )}
            {preset.category && (
              <div className="tag-oppo px-2 py-1">
                <span className="text-xs text-text-primary">{preset.category}</span>
              </div>
            )}
          </div>
          {preset.isNew && (
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', delay: 0.5 }}
              className="tag-new px-2.5 py-1 flex items-center space-x-1"
            >
              <Sparkles className="w-3 h-3 text-white" />
              <span className="text-xs font-bold text-white">NEW</span>
            </motion.div>
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
          className="absolute top-3 right-3 w-8 h-8 glass-effect rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-300"
        >
          <motion.div
            animate={preset.isFavorite ? { scale: [1, 1.2, 1] } : {}}
            transition={{ duration: 0.3 }}
          >
            <Heart
              className={`w-4 h-4 transition-colors ${
                preset.isFavorite
                  ? 'text-red-500 fill-red-500'
                  : 'text-text-primary'
              }`}
            />
          </motion.div>
        </motion.button>
        
        {/* Content Overlay */}
        <div className="absolute bottom-0 left-0 right-0 p-4">
          <h3 className="text-base font-bold mb-1.5 group-hover:text-oppo-sunrise-gold transition-colors line-clamp-2">
            {preset.name}
          </h3>
          
          {preset.author && (
            <p className="text-xs text-text-secondary mb-2">
              by {preset.author}
            </p>
          )}
          
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Camera className="w-3.5 h-3.5 text-text-secondary" />
              <span className="text-xs text-text-primary bg-white/8 px-2.5 py-1 rounded-full">
                {preset.deviceModel}
              </span>
            </div>
            
            <motion.button 
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={handleApply}
              className="bg-gradient-to-r from-oppo-sunrise-gold to-hasselblad-pro text-deep-space text-xs font-semibold px-3 py-1 rounded-full transition-all hover:shadow-lg"
            >
              应用
            </motion.button>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
