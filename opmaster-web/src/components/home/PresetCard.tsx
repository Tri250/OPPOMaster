import { motion } from 'framer-motion';
import { Heart, Star } from 'lucide-react';
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

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1 }}
      whileHover={{ y: -8 }}
      className="card cursor-pointer group"
      onClick={handleClick}
    >
      {/* Image */}
      <div className="relative aspect-[4/3] overflow-hidden">
        <img
          src={`https://picsum.photos/seed/${preset.coverPath}/600/450`}
          alt={preset.name}
          className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
          loading="lazy"
        />
        
        {/* Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
        
        {/* HNCS Badge */}
        {preset.cameraParams?.hasselblad_hncs && (
          <div className="absolute top-3 right-3 glass-effect px-3 py-1 rounded-full flex items-center space-x-1">
            <Star className="w-3 h-3 text-hasselblad fill-hasselblad" />
            <span className="text-xs font-bold text-hasselblad">HNCS</span>
          </div>
        )}
        
        {/* Favorite Button */}
        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          onClick={(e) => {
            e.stopPropagation();
            toggleFavorite(preset.id);
          }}
          className="absolute bottom-3 right-3 w-10 h-10 glass-effect rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300"
        >
          <Heart
            className={`w-5 h-5 transition-colors ${
              preset.isFavorite
                ? 'text-red-500 fill-red-500'
                : 'text-white'
            }`}
          />
        </motion.button>
      </div>

      {/* Content */}
      <div className="p-4">
        <h3 className="text-lg font-bold mb-2 group-hover:text-hasselblad transition-colors line-clamp-2">
          {preset.name}
        </h3>
        
        <div className="flex items-center justify-between">
          <span className="text-sm text-white/60 bg-white/5 px-3 py-1 rounded-full">
            {preset.deviceModel}
          </span>
          
          {preset.cameraParams && (
            <span className="text-xs text-white/40">
              ISO {preset.cameraParams.iso}
            </span>
          )}
        </div>
      </div>
    </motion.div>
  );
}
