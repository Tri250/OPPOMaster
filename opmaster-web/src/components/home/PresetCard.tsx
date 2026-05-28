import { motion } from 'framer-motion';
import { Heart, Star, Sparkles } from 'lucide-react';
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
      transition={{ delay: index * 0.05 }}
      whileHover={{ y: -4, scale: 1.02 }}
      className="card cursor-pointer group overflow-hidden"
      onClick={handleClick}
    >
      {/* Image */}
      <div className="relative aspect-[3/4] overflow-hidden">
        <img
          src={`https://picsum.photos/seed/${preset.coverPath}/400/533`}
          alt={preset.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
          loading="lazy"
        />
        
        {/* Gradient Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent" />
        
        {/* Top Badges */}
        <div className="absolute top-3 left-3 right-3 flex items-center justify-between">
          {preset.cameraParams?.hasselblad_hncs && (
            <div className="glass-effect px-2.5 py-1 rounded-full flex items-center space-x-1.5">
              <Star className="w-3 h-3 text-hasselblad fill-hasselblad" />
              <span className="text-xs font-bold text-hasselblad">哈苏</span>
            </div>
          )}
          {preset.isNew && (
            <div className="bg-gradient-to-r from-oppo-green to-hasselblad px-2.5 py-1 rounded-full flex items-center space-x-1">
              <Sparkles className="w-3 h-3 text-white" />
              <span className="text-xs font-bold text-white">NEW</span>
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
          className="absolute top-3 right-3 w-8 h-8 glass-effect rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300"
        >
          <Heart
            className={`w-4 h-4 transition-colors ${
              preset.isFavorite
                ? 'text-red-500 fill-red-500'
                : 'text-white'
            }`}
          />
        </motion.button>
        
        {/* Content Overlay */}
        <div className="absolute bottom-0 left-0 right-0 p-4">
          <h3 className="text-base font-bold mb-1.5 group-hover:text-hasselblad transition-colors line-clamp-2">
            {preset.name}
          </h3>
          
          <div className="flex items-center justify-between">
            <span className="text-xs text-white/80 bg-white/10 px-2.5 py-1 rounded-full">
              {preset.deviceModel}
            </span>
            
            <button 
              onClick={handleApply}
              className="bg-hasselblad/90 hover:bg-hasselblad text-deep-space text-xs font-semibold px-3 py-1 rounded-full transition-colors"
            >
              应用
            </button>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
