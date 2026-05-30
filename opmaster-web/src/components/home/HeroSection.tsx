import { motion } from 'framer-motion';
import { Camera, Sparkles, Star, Smartphone } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function HeroSection() {
  return (
    <section className="relative min-h-[70vh] flex items-center justify-center overflow-hidden pt-16">
      {/* Background Effects */}
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-b from-page-bg via-card-bg to-page-bg" />
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-oppo-primary/10 rounded-full blur-3xl animate-float" />
        <div className="absolute bottom-1/4 right-1/4 w-64 h-64 bg-oppo-green/10 rounded-full blur-3xl animate-float" style={{ animationDelay: '3s' }} />
      </div>

      <div className="relative z-10 max-w-7xl mx-auto px-page sm:px-page lg:px-page text-center pt-nav-content">
        {/* Logo Animation */}
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 1, type: 'spring', stiffness: 100 }}
          className="mb-8"
        >
          <div className="inline-flex items-center justify-center w-24 h-24 bg-gradient-to-br from-oppo-primary to-oppo-primary/60 rounded-card shadow-2xl shadow-oppo-primary/30 animate-pulse-glow">
            <Camera className="w-14 h-14 text-text-primary" />
          </div>
        </motion.div>

        {/* Title - 一级标题使用哈苏橙 */}
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="text-5xl md:text-7xl font-bold mb-6 text-hasselblad"
        >
          小O帮帮
        </motion.h1>

        {/* Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="text-xl md:text-2xl text-text-secondary mb-8 max-w-3xl mx-auto"
        >
          哈苏影像系统级参数中枢
        </motion.p>

        {/* Feature Tags - Simplified */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
          className="flex flex-wrap justify-center gap-component mb-12"
        >
          {[
            { icon: Star, label: '哈苏认证', color: 'text-hasselblad' },
            { icon: Sparkles, label: 'AI智能', color: 'text-purple-400' },
            { icon: Smartphone, label: '悬浮窗', color: 'text-pink-400' }
          ].map((feature, idx) => (
            <motion.div
              key={feature.label}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.8 + idx * 0.08 }}
              whileHover={{ scale: 1.05, y: -2 }}
              className="glass-effect px-5 py-2.5 rounded-small flex items-center gap-2 backdrop-blur-md border border-white/10"
            >
              <feature.icon className={`w-4.5 h-4.5 ${feature.color}`} />
              <span className="text-sm font-medium text-text-primary">{feature.label}</span>
            </motion.div>
          ))}
        </motion.div>

        {/* CTA Buttons */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.0 }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <Link to="/ai-demo" className="btn-primary text-lg px-8 py-4 animate-pulse-glow">
            立即体验AI功能
          </Link>
          <Link to="/preset/1" className="btn-secondary text-lg px-8 py-4">
            浏览影像推荐
          </Link>
        </motion.div>
      </div>
    </section>
  );
}
