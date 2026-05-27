import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Heart, Eye, Star, Download, AlertCircle } from 'lucide-react';
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
  const [imageError, setImageError] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // 获取来源标签
  const getSourceLabel = () => {
    if (preset.source === 'oppo') return 'OPPO';
    if (preset.source === 'realme') return 'Realme';
    return preset.source;
  };

  // 获取来源标签颜色
  const getSourceColor = () => {
    if (preset.source === 'oppo') return 'from-green-500 to-emerald-600';
    if (preset.source === 'realme') return 'from-orange-500 to-red-600';
    return 'from-blue-500 to-purple-600';
  };

  // 获取备用图片
  const getFallbackImage = () => {
    return `https://picsum.photos/seed/${encodeURIComponent(preset.name)}-${preset.source}/600/400`;
  };

  // 处理图片加载
  const handleImageError = () => {
    console.log('[PresetCard] Image failed to load:', preset.coverPath);
    setImageError(true);
  };

  const handleImageLoad = () => {
    setIsLoading(false);
  };

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
          {/* 加载状态 */}
          {isLoading && (
            <div className="absolute inset-0 bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center z-10">
              <div className="flex flex-col items-center space-y-2">
                <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                <span className="text-gray-500 text-sm">加载中...</span>
              </div>
            </div>
          )}

          {/* 图片 */}
          <img
            src={imageError ? getFallbackImage() : preset.coverPath}
            alt={preset.name}
            className={cn(
              "w-full h-full object-cover transition-transform duration-700 group-hover:scale-110",
              isLoading && "opacity-0"
            )}
            loading="lazy"
            onError={handleImageError}
            onLoad={handleImageLoad}
          />

          {/* 图片加载失败提示 */}
          {imageError && !isLoading && (
            <div className="absolute inset-0 bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center">
              <div className="flex flex-col items-center space-y-2 text-gray-500">
                <AlertCircle className="w-8 h-8" />
                <span className="text-sm">图片加载中</span>
              </div>
            </div>
          )}

          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
          
          {/* 来源标签 - 左上角 */}
          <div className="absolute top-4 left-4 z-20">
            <span className={cn(
              "px-3 py-1.5 rounded-xl text-xs font-bold text-white shadow-lg bg-gradient-to-r",
              getSourceColor()
            )}>
              {getSourceLabel()}
            </span>
          </div>
          
          {/* 收藏按钮 - 右上角 */}
          <button
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              toggleFavorite(preset.id);
            }}
            className={cn(
              "absolute top-4 right-4 p-3 rounded-2xl backdrop-blur-md transition-all duration-300 z-20",
              preset.isFavorite
                ? "bg-pink-500 text-white shadow-lg shadow-pink-200"
                : "bg-white/80 text-gray-600 hover:bg-white"
            )}
          >
            <Heart
              className={cn("w-5 h-5", preset.isFavorite && "fill-current")}
            />
          </button>

          {/* 查看详情按钮 - 底部 */}
          <Link
            to={`/preset/${preset.id}`}
            className="absolute bottom-4 left-4 right-4 flex items-center justify-center space-x-2 bg-white/95 backdrop-blur-sm py-3 px-4 rounded-2xl font-semibold text-gray-800 opacity-0 group-hover:opacity-100 transition-all duration-300 transform translate-y-4 group-hover:translate-y-0 hover:bg-white hover:shadow-lg z-20"
          >
            <Eye className="w-5 h-5" />
            <span>查看详情</span>
          </Link>
        </div>

        <div className="p-5">
          {/* 预设名称 */}
          <h3 className="text-lg font-bold text-gray-900 mb-2 line-clamp-2">
            {preset.name}
          </h3>
          
          {/* 评分和使用次数 */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-1">
              <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />
              <span className="text-sm font-semibold text-gray-700">{preset.rating.toFixed(1)}</span>
            </div>
            <div className="flex items-center space-x-1 text-gray-500 text-sm">
              <Download className="w-4 h-4" />
              <span>{preset.usageCount.toLocaleString()}</span>
            </div>
          </div>

          {/* 标签 */}
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

          {/* 作者和机型信息 */}
          <div className="flex flex-col space-y-2">
            <div className="flex items-center justify-between text-xs text-gray-500">
              <span className="flex items-center space-x-1">
                <div className="w-5 h-5 rounded-full bg-gradient-to-r from-blue-500 to-purple-500" />
                <span>{preset.author}</span>
              </span>
            </div>
            {/* 机型标签 - 更清晰的显示 */}
            <div className="flex items-center space-x-2">
              <span className="px-3 py-1 bg-gradient-to-r from-gray-100 to-gray-200 text-gray-700 text-xs font-semibold rounded-full">
                📱 {preset.deviceModel}
              </span>
            </div>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

export default PresetCard;
