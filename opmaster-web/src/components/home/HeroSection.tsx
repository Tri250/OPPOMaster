import { motion } from 'framer-motion';
import { Camera, Sparkles, Star, Zap } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function HeroSection() {
  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden pt-16">
      {/* Background Effects */}
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-b from-deep-space via-deep-space-light to-deep-space" />
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-hasselblad/10 rounded-full blur-3xl animate-float" />
        <div className="absolute bottom-1/4 right-1/4 w-64 h-64 bg-oppo-green/10 rounded-full blur-3xl animate-float" style={{ animationDelay: '3s' }} />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,transparent_0%,rgba(0,0,0,0.5)_100%)]" />
      </div>

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        {/* Logo Animation */}
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 1, type: 'spring', stiffness: 100 }}
          className="mb-8"
        >
          <div className="inline-flex items-center justify-center w-24 h-24 bg-gradient-to-br from-hasselblad to-hasselblad/60 rounded-3xl shadow-2xl shadow-hasselblad/30 animate-pulse-glow">
            <Camera className="w-14 h-14 text-deep-space" />
          </div>
        </motion.div>

        {/* Title */}
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="text-5xl md:text-7xl font-bold mb-6"
        >
          <span className="gradient-text">OPPOMaster</span>
        </motion.h1>

        {/* Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="text-xl md:text-2xl text-white/70 mb-4 max-w-3xl mx-auto"
        >
          哈苏影像系统级参数中枢
        </motion.p>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
          className="text-lg text-white/50 mb-12 max-w-2xl mx-auto"
        >
          深度集成 ColorOS 16 · HNCS 认证预设 · AI 智能推荐
        </motion.p>

        {/* Feature Tags */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
          className="flex flex-wrap justify-center gap-4 mb-12"
        >
          {[
            { icon: Star, label: '哈苏认证', color: 'text-hasselblad' },
            { icon: Sparkles, label: 'AI智能', color: 'text-purple-400' },
            { icon: Zap, label: '实时预览', color: 'text-oppo-green' }
          ].map((feature, idx) => (
            <motion.div
              key={feature.label}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.8 + idx * 0.1 }}
              className="glass-effect px-6 py-3 rounded-full flex items-center space-x-2"
            >
              <feature.icon className={`w-5 h-5 ${feature.color}`} />
              <span className="text-sm font-medium">{feature.label}</span>
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
          <button className="btn-secondary text-lg px-8 py-4">
            了解更多
          </button>
        </motion.div>
      </div>

      {/* Scroll Indicator */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1.5 }}
        className="absolute bottom-8 left-1/2 -translate-x-1/2"
      >
        <motion.div
          animate={{ y: [0, 10, 0] }}
          transition={{ duration: 2, repeat: Infinity }}
          className="w-6 h-10 border-2 border-white/30 rounded-full flex justify-center pt-2"
        >
          <motion.div
            animate={{ y: [0, 12, 0] }}
            transition={{ duration: 2, repeat: Infinity }}
            className="w-1.5 h-1.5 bg-white rounded-full"
          />
        </motion.div>
      </motion.div>
    </section>
  );
}
