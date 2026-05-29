import { motion } from 'framer-motion';
import { ArrowRight } from 'lucide-react';
import { presets } from '../../data/presets';

export default function OppoFeaturedPresets() {
  const featuredPresets = presets.slice(0, 6);

  return (
    <section className="py-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      {/* Section Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="mb-10"
      >
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl md:text-3xl font-bold text-text-primary mb-2">
              精选影像推荐
            </h2>
            <p className="text-text-secondary">
              专业摄影师精心调校的预设参数
            </p>
          </div>
          <button className="hidden md:flex items-center gap-1 text-oppo-orange text-sm font-medium hover:gap-2 transition-all">
            查看全部 <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </motion.div>

      {/* Presets Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4 md:gap-5">
        {featuredPresets.map((preset, index) => (
          <motion.div
            key={preset.id}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: index * 0.1 }}
            whileHover={{ y: -4 }}
            className="group cursor-pointer"
          >
            {/* Card */}
            <div className="bg-surface rounded-2xl overflow-hidden border border-border-subtle hover:border-oppo-orange/30 transition-all duration-300 shadow-card">
              {/* Image Container */}
              <div className="relative aspect-[3/4] overflow-hidden">
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  loading="lazy"
                />
                
                {/* Gradient Overlay */}
                <div className="absolute inset-0 bg-gradient-to-t from-deep-space via-transparent to-transparent opacity-70" />
                
                {/* New Badge */}
                {preset.isNew && (
                  <div className="absolute top-3 left-3">
                    <span className="bg-oppo-orange text-deep-space text-xs font-bold px-3 py-1 rounded-full">
                      NEW
                    </span>
                  </div>
                )}
                
                {/* Info */}
                <div className="absolute bottom-0 left-0 right-0 p-4">
                  <h3 className="text-lg font-bold text-white mb-1 line-clamp-1">
                    {preset.name}
                  </h3>
                  <p className="text-xs text-white/70">
                    {preset.author}
                  </p>
                </div>
              </div>
              
              {/* Footer */}
              <div className="p-3.5">
                <button className="w-full bg-surface-hover text-text-primary text-sm font-medium py-2.5 rounded-xl hover:bg-oppo-orange/10 hover:text-oppo-orange transition-all duration-200">
                  查看预设
                </button>
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {/* View All Button (Mobile) */}
      <motion.div
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true }}
        className="mt-8 md:hidden"
      >
        <button className="w-full bg-surface border border-border-subtle text-text-primary font-medium py-3.5 rounded-2xl hover:border-oppo-orange/40 transition-all">
          查看全部预设
        </button>
      </motion.div>
    </section>
  );
}
