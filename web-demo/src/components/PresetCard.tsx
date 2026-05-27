import React from 'react';
import { Link } from 'react-router-dom';
import { Heart, Eye, Star, Download } from 'lucide-react';
import { motion } from 'framer-motion';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { Preset } from '../types';
import { usePresetStore } from '../store/usePresetStore';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface PresetCardProps {
  preset: Preset;
  index: number;
}

const PresetCard: React.FC<PresetCardProps> = ({ preset, index }) => {
  const toggleFavorite = usePresetStore((state) => state.toggleFavorite);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1 }}
      whileHover={{ y: -8 }}
      className="group"
    >
      <div className="bg-white rounded-3xl overflow-hidden shadow-lg shadow-gray-200/50 transition-all duration-300 group-hover:shadow-2xl group-hover:shadow-purple-200/50 border border-gray-100">
        <div className="relative aspect-[4/3] overflow-hidden">
          <img
            src={preset.coverPath}
            alt={preset.name}
            className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
          
          <button
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              toggleFavorite(preset.id);
            }}
            className={cn(
              'absolute top-4 right-4 p-3 rounded-2xl backdrop-blur-md transition-all duration-300',
              preset.isFavorite
                ? 'bg-pink-500 text-white shadow-lg shadow-pink-200'
                : 'bg-white/80 text-gray-600 hover:bg-white'
            )}
          >
            <Heart
              className={cn('w-5 h-5', preset.isFavorite && 'fill-current')}
            />
          </button>

          <Link
            to={`/preset/${preset.id}`}
            className="absolute bottom-4 left-4 right-4 flex items-center justify-center space-x-2 bg-white/95 backdrop-blur-sm py-3 px-4 rounded-2xl font-semibold text-gray-800 opacity-0 group-hover:opacity-100 transition-all duration-300 transform translate-y-4 group-hover:translate-y-0 hover:bg-white hover:shadow-lg"
          >
            <Eye className="w-5 h-5" />
            <span>查看详情</span>
          </Link>
        </div>

        <div className="p-5">
          <h3 className="text-lg font-bold text-gray-900 mb-2 line-clamp-2">
            {preset.name}
          </h3>
          
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-1">
              <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />
              <span className="text-sm font-semibold text-gray-700">{preset.rating}</span>
            </div>
            <div className="flex items-center space-x-1 text-gray-500 text-sm">
              <Download className="w-4 h-4" />
              <span>{preset.usageCount}</span>
            </div>
          </div>

          <div className="flex flex-wrap gap-2 mb-4">
            {preset.cameraParams.sceneTags.slice(0, 2).map((tag) => (
              <span
                key={tag}
                className="px-3 py-1 bg-gradient-to-r from-blue-50 to-purple-50 text-blue-700 text-xs font-semibold rounded-full"
              >
                {tag}
              </span>
            ))}
          </div>

          <div className="flex items-center justify-between text-xs text-gray-500">
            <span className="flex items-center space-x-1">
              <div className="w-5 h-5 rounded-full bg-gradient-to-r from-blue-500 to-purple-500" />
              <span>{preset.author}</span>
            </span>
            <span>{preset.deviceModel}</span>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

export default PresetCard;
