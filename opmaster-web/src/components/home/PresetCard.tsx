import { motion, AnimatePresence } from 'framer-motion'
import { Heart, Star, Sparkles, Camera, Share, Eye, MoreHorizontal } from 'lucide-react';
import type { Preset } from '../../data/mockPresets';
import { useAppStore } from '../../store/useAppStore';
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';

interface PresetCardProps {
  preset: Preset;
  index: number;
}

export default function PresetCard({ preset, index }: PresetCardProps) {
  const { toggleFavorite, setSelectedPreset } = useAppStore();
  const navigate = useNavigate();
  const [showQuickMenu, setShowQuickMenu] = useState(false);

  const handleClick = () => {
    setSelectedPreset(preset);
    navigate(`/preset/${preset.id}`);
  };

  const handleApply = (e: React.MouseEvent) => {
    e.stopPropagation();
    alert(`已将 "${preset.name}" 影像参数应用到您的设备`);
  };

  const handleQuickMenuToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShowQuickMenu(!showQuickMenu);
  };

  const handleShare = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShowQuickMenu(false);
    if (navigator.share) {
      navigator.share({
        title: preset.name,
        text: `Check out this OPPO camera preset: ${preset.name}`,
        url: window.location.href,
      });
    } else {
      alert(`分享 "${preset.name}" 预设`);
    }
  };

  const handleViewDetails = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShowQuickMenu(false);
    handleClick();
  };

  const handleFavoriteToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    toggleFavorite(preset.id);
    if (showQuickMenu) {
      setShowQuickMenu(false);
    }
  };

  return (
    <>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: index * 0.05, ease: [0.4, 0, 0.2, 1] }}
        whileHover={{ y: -6, scale: 1.02 }}
        className="card-oppo cursor-pointer group overflow-hidden relative"
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
          <div className="flex items-center space-x-2">
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
            {/* Quick Menu Button */}
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={handleQuickMenuToggle}
              className="w-8 h-8 glass-effect rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-300"
            >
              <MoreHorizontal className="w-4 h-4 text-white" />
            </motion.button>
          </div>
        </div>
        
        {/* Favorite Button */}
        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          onClick={handleFavoriteToggle}
          className="absolute bottom-3 right-3 w-8 h-8 glass-effect rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-300"
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
    
    {/* Quick Menu Overlay */}
    <AnimatePresence>
      {showQuickMenu && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={(e) => {
              e.stopPropagation();
              setShowQuickMenu(false);
            }}
            className="fixed inset-0 bg-black/50 z-40"
          />
          
          {/* Menu */}
          <motion.div
            initial={{ opacity: 0, scale: 0.85, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.85, y: 20 }}
            transition={{ type: 'spring', damping: 30, stiffness: 400 }}
            className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-[90%] max-w-md"
          >
            <div className="card-glass rounded-oppo-lg overflow-hidden">
              {/* Menu Header */}
              <div className="p-6 border-b border-white/10">
                <h3 className="text-lg font-semibold text-text-primary mb-1">{preset.name}</h3>
                <p className="text-sm text-text-secondary">快捷操作</p>
              </div>
              
              {/* Menu Items */}
              <div className="divide-y divide-white/10">
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleFavoriteToggle}
                  className="w-full flex items-center space-x-4 p-5 hover:bg-white/5 transition-colors"
                >
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center ${preset.isFavorite ? 'bg-red-500/20' : 'bg-white/5'}`}>
                    <Heart className={`w-5 h-5 ${preset.isFavorite ? 'text-red-500 fill-red-500' : 'text-text-primary'}`} />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="font-medium text-text-primary">
                      {preset.isFavorite ? '取消收藏' : '快速收藏'}
                    </p>
                  </div>
                </motion.button>
                
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleShare}
                  className="w-full flex items-center space-x-4 p-5 hover:bg-white/5 transition-colors"
                >
                  <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
                    <Share className="w-5 h-5 text-text-primary" />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="font-medium text-text-primary">分享预设</p>
                  </div>
                </motion.button>
                
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleViewDetails}
                  className="w-full flex items-center space-x-4 p-5 hover:bg-white/5 transition-colors"
                >
                  <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
                    <Eye className="w-5 h-5 text-text-primary" />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="font-medium text-text-primary">查看详细参数</p>
                  </div>
                </motion.button>
                
                {preset.cameraParams?.hncs && (
                  <div className="flex items-center space-x-4 p-5 bg-oppo-sunrise-gold/5">
                    <div className="w-10 h-10 rounded-full bg-oppo-sunrise-gold/20 flex items-center justify-center">
                      <Star className="w-5 h-5 text-oppo-sunrise-gold fill-oppo-sunrise-gold" />
                    </div>
                    <div className="flex-1 text-left">
                      <p className="font-medium text-oppo-sunrise-gold">哈苏认证</p>
                    </div>
                  </div>
                )}
              </div>
              
              {/* Close Button */}
              <div className="p-4 border-t border-white/10">
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowQuickMenu(false);
                  }}
                  className="w-full py-3 rounded-full bg-white/10 text-text-primary font-medium hover:bg-white/15 transition-colors"
                >
                  关闭
                </motion.button>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
    </>
  );
}
