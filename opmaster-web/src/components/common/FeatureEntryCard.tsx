import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import type { FeatureEntry } from '../../data/featureEntries';

interface FeatureEntryCardProps {
  feature: FeatureEntry;
  index?: number;
  size?: 'default' | 'large' | 'compact';
}

export default function FeatureEntryCard({ 
  feature, 
  index = 0,
  size = 'default' 
}: FeatureEntryCardProps) {
  const Icon = feature.icon;
  
  const sizeClasses = {
    default: 'p-6',
    large: 'p-7',
    compact: 'p-5',
  };

  const iconSize = size === 'large' ? 'w-14 h-14' : size === 'compact' ? 'w-10 h-10' : 'w-12 h-12';

  return (
    <motion.div
      initial={{ opacity: 0, y: 24, scale: 0.95 }}
      whileInView={{ opacity: 1, y: 0, scale: 1 }}
      viewport={{ once: true, margin: '-50px' }}
      transition={{ 
        delay: index * 0.06, 
        duration: 0.5, 
        ease: [0.05, 0.7, 0.1, 1.0]
      }}
      whileHover={{ 
        y: -6, scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
    >
      <Link to={feature.path} className="block">
        <div className={`
          card-glass ${sizeClasses[size]}
          rounded-2xl overflow-hidden
          transition-all duration-300
          hover:shadow-oppo-elevation-3
          border border-white/8
          hover:border-oppo-orange/30
          relative group
        `}>
          {/* 背景光效 */}
          <div className={`
            absolute inset-0 bg-gradient-to-br ${feature.color} opacity-0 
            group-hover:opacity-5 transition-opacity duration-500
          `} />
          
          <div className="relative z-10">
            {/* 顶部区域 */}
            <div className="flex items-start justify-between mb-4">
              {/* 图标容器 */}
              <motion.div
                whileHover={{ rotate: 6, scale: 1.1 }}
                transition={{ type: 'spring', stiffness: 400, damping: 15 }}
                className={`${iconSize} rounded-xl bg-gradient-to-br ${feature.color} 
                  flex items-center justify-center
                  shadow-oppo-elevation-2`}
              >
                <Icon className={`${size === 'large' ? 'w-7 h-7' : 'w-6 h-6'} text-oppo-black`} />
              </motion.div>

              {/* 徽章 */}
              {feature.badge && (
                <span className={`
                  px-3 py-1 rounded-full text-micro font-bold
                  bg-gradient-to-r ${feature.color}
                  text-oppo-black
                `}>
                  {feature.badge}
                </span>
              )}

              {/* 精选标识 */}
              {feature.isFeatured && !feature.badge && (
                <span className="px-3 py-1 rounded-full text-micro font-bold bg-oppo-orange text-oppo-black">
                  精选
                </span>
              )}
            </div>

            {/* 文字内容 */}
            <div className="mb-4">
              <h3 className={`
                font-bold text-text-primary mb-1.5
                ${size === 'large' ? 'text-h3' : 'text-h3'}
              `}>
                {feature.title}
              </h3>
              <p className="text-body2 text-text-secondary leading-relaxed">
                {feature.description}
              </p>
            </div>

            {/* 操作提示 */}
            <div className="flex items-center text-oppo-orange text-body2 font-semibold 
              group-hover:gap-2 transition-all">
              <span>立即体验</span>
              <ArrowRight className="w-4.5 h-4.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>
        </div>
      </Link>
    </motion.div>
  );
}
