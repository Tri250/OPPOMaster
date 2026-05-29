import { motion } from 'framer-motion';
import { Camera, Sparkles, Zap, Palette } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function OppoHero() {
  return (
    <section className="relative min-h-screen flex flex-col items-center justify-center overflow-hidden px-4 pt-16 pb-8">
      {/* Background Effects */}
      <div className="absolute inset-0 bg-gradient-to-b from-deep-space via-deep-space-100 to-deep-space" />
      <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-oppo-orange/10 rounded-full blur-3xl animate-float" />
      <div className="absolute bottom-1/4 right-1/4 w-48 h-48 bg-oppo-green/10 rounded-full blur-3xl animate-float" style={{ animationDelay: '2s' }} />
      
      <div className="relative z-10 max-w-4xl mx-auto text-center">
        {/* Logo */}
        <motion.div
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.8, type: 'spring' }}
          className="mb-8"
        >
          <div className="inline-flex items-center justify-center w-24 h-24 bg-gradient-to-br from-oppo-orange to-hasselblad rounded-3xl shadow-lg shadow-oppo-orange/30 animate-breathing">
            <Camera className="w-14 h-14 text-deep-space" />
          </div>
        </motion.div>

        {/* Title */}
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2, duration: 0.6 }}
          className="text-5xl md:text-6xl font-bold mb-4 tracking-tight"
        >
          <span className="bg-gradient-to-r from-white via-oppo-orange to-hasselblad bg-clip-text text-transparent">
            小O帮帮
          </span>
        </motion.h1>

        {/* Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4, duration: 0.6 }}
          className="text-xl text-text-secondary mb-8"
        >
          哈苏影像系统级参数中枢
        </motion.p>

        {/* Feature Tags */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6, duration: 0.6 }}
          className="flex flex-wrap justify-center gap-3 mb-12"
        >
          {[
            { icon: Sparkles, label: '哈苏认证', color: 'text-oppo-orange' },
            { icon: Zap, label: 'AI 智能', color: 'text-oppo-green' },
            { icon: Palette, label: '精选预设', color: 'text-hasselblad' },
          ].map((feature, idx) => (
            <motion.div
              key={feature.label}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.7 + idx * 0.1 }}
              whileHover={{ scale: 1.05, y: -2 }}
              className="bg-surface border border-border-subtle px-5 py-2.5 rounded-full flex items-center gap-2 transition-all hover:border-oppo-orange/40"
            >
              <feature.icon className={`w-4 h-4 ${feature.color}`} />
              <span className="text-sm font-medium text-text-primary">{feature.label}</span>
            </motion.div>
          ))}
        </motion.div>

        {/* CTA Buttons */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1, duration: 0.6 }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <Link
            to="/ai-demo"
            className="w-full sm:w-auto bg-gradient-to-r from-oppo-orange to-hasselblad text-deep-space font-semibold px-8 py-4 rounded-2xl hover:shadow-oppo-hover transition-all hover:-translate-y-0.5"
          >
            立即体验 AI 功能
          </Link>
          <Link
            to="/about"
            className="w-full sm:w-auto bg-surface border border-border-subtle text-text-primary font-medium px-8 py-4 rounded-2xl hover:border-oppo-orange/40 hover:bg-surface-hover transition-all"
          >
            了解更多
          </Link>
        </motion.div>
      </div>

      {/* Quick Action Indicator */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1.5 }}
        className="absolute bottom-8 left-1/2 -translate-x-1/2"
      >
        <div className="text-center">
          <motion.div
            animate={{ y: [0, 6, 0] }}
            transition={{ duration: 2, repeat: Infinity }}
            className="flex flex-col items-center gap-1"
          >
            <span className="text-xs text-text-tertiary">向下滑动</span>
            <div className="w-5 h-8 border-2 border-text-tertiary/30 rounded-full flex justify-center pt-1">
              <motion.div
                animate={{ y: [0, 10, 0] }}
                transition={{ duration: 2, repeat: Infinity }}
                className="w-1.5 h-1.5 bg-text-tertiary rounded-full"
              />
            </div>
          </motion.div>
        </div>
      </motion.div>
    </section>
  );
}
