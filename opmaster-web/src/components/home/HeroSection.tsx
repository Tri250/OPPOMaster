import { motion } from 'framer-motion';
import { Camera, Sparkles, Star, Smartphone, Download } from 'lucide-react';
import { Link } from 'react-router-dom';

const easeOppoEnter: [number, number, number, number] = [0.05, 0.7, 0.1, 1.0];

export default function HeroSection() {
  return (
    <section className="relative min-h-[80vh] flex items-center justify-center overflow-hidden pt-16">
      {/* Background Effects - OPPO品牌风格 */}
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-b from-oppo-black via-oppo-black to-oppo-black" />
        <div className="orb-oppo orb-1 top-1/4 left-1/4 w-96 h-96" />
        <div className="orb-oppo orb-2 bottom-1/4 right-1/4 w-64 h-64" style={{ animationDelay: '3s' }} />
        <div className="orb-oppo orb-3 top-1/2 left-1/2 w-48 h-48 -translate-x-1/2 -translate-y-1/2" style={{ animationDelay: '1.5s' }} />
      </div>

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        {/* Logo Animation */}
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 1, type: 'spring', stiffness: 100 }}
          className="mb-8"
        >
          <div className="inline-flex items-center justify-center w-24 h-24 bg-gradient-to-br from-oppo-orange to-hasselblad-orange rounded-2xl shadow-oppo-elevation-3 animate-pulse-glow">
            <Camera className="w-14 h-14 text-oppo-black" />
          </div>
        </motion.div>

        {/* Title */}
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.5, ease: easeOppoEnter }}
          className="text-4xl md:text-6xl font-bold mb-6"
        >
          <span className="gradient-text-oppo">小O帮帮</span>
        </motion.h1>

        {/* Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.5, ease: easeOppoEnter }}
          className="text-lg md:text-xl text-text-secondary mb-8 max-w-3xl mx-auto"
        >
          哈苏影像系统级参数中枢
        </motion.p>

        {/* Feature Tags - OPPO风格 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7, duration: 0.5, ease: easeOppoEnter }}
          className="flex flex-wrap justify-center gap-3 mb-12"
        >
          {[
            { icon: Star, label: '哈苏认证', color: 'text-hasselblad-orange' },
            { icon: Sparkles, label: 'AI智能', color: 'text-oppo-orange' },
            { icon: Smartphone, label: '悬浮窗', color: 'text-text-primary' }
          ].map((feature, idx) => (
            <motion.div
              key={feature.label}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.8 + idx * 0.08, ease: easeOppoEnter }}
              whileHover={{ scale: 1.05, y: -2 }}
              className="glass-effect px-5 py-2.5 rounded-full flex items-center space-x-2 backdrop-blur-md border border-white/10 shadow-oppo-elevation-1"
            >
              <feature.icon className={`w-4.5 h-4.5 ${feature.color}`} />
              <span className="text-body2 font-medium">{feature.label}</span>
            </motion.div>
          ))}
        </motion.div>

        {/* CTA Buttons - OPPO官方标准 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.0, duration: 0.5, ease: easeOppoEnter }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <button className="btn-primary-large flex items-center gap-2 animate-pulse-glow">
            <Download className="w-5 h-5" />
            立即体验
          </button>
          <Link to="/preset/1" className="btn-secondary text-body1 px-8 py-3.5">
            浏览影像推荐
          </Link>
        </motion.div>
      </div>
    </section>
  );
}

