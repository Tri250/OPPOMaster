import { motion } from 'framer-motion';
import { Camera, Sparkles, Star, Smartphone, Aperture, Zap, Palette } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function HeroSection() {
  return (
    <section className="relative min-h-[80vh] flex items-center justify-center overflow-hidden pt-16">
      {/* Advanced Background Effects - Photography Theme */}
      <div className="absolute inset-0">
        {/* Deep space gradient with photography atmosphere */}
        <div className="absolute inset-0 bg-gradient-to-br from-surface-800 via-surface-700 to-surface-900" />
        
        {/* Hasselblad gold accent glow */}
        <div className="absolute top-1/3 left-1/4 w-[500px] h-[500px] bg-gradient-to-br from-hasselblad/20 to-transparent rounded-full blur-[120px] animate-pulse" />
        
        {/* Aqua blue accent */}
        <div className="absolute bottom-1/3 right-1/4 w-[400px] h-[400px] bg-gradient-to-br from-aqua-primary/10 to-transparent rounded-full blur-[100px] animate-pulse" style={{ animationDelay: '1.5s' }} />
        
        {/* Subtle grid pattern for professional feel */}
        <div className="absolute inset-0 opacity-5" 
          style={{
            backgroundImage: `linear-gradient(to right, rgba(201, 168, 108, 0.1) 1px, transparent 1px), linear-gradient(to bottom, rgba(201, 168, 108, 0.1) 1px, transparent 1px)`,
            backgroundSize: '40px 40px'
          }}
        />
        
        {/* Floating particles for depth */}
        <div className="absolute inset-0 overflow-hidden">
          {[...Array(20)].map((_, i) => (
            <motion.div
              key={i}
              className="absolute w-1 h-1 bg-hasselblad/30 rounded-full"
              initial={{
                x: Math.random() * (typeof window !== 'undefined' ? window.innerWidth : 1200),
                y: Math.random() * (typeof window !== 'undefined' ? window.innerHeight : 800),
              }}
              animate={{
                y: [null, Math.random() * -200 - 100],
                opacity: [0.3, 0.8, 0.3],
              }}
              transition={{
                duration: Math.random() * 10 + 10,
                repeat: Infinity,
                ease: 'linear',
              }}
            />
          ))}
        </div>
      </div>

      <div className="relative z-10 max-w-6xl mx-auto px-6 text-center">
        {/* Premium Logo with Hasselblad Glow */}
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 1.2, type: 'spring', stiffness: 80, damping: 15 }}
          className="mb-10"
        >
          <div className="relative inline-block">
            {/* Outer glow ring */}
            <div className="absolute inset-0 bg-gradient-to-br from-hasselblad to-hasselblad-dark rounded-3xl blur-2xl opacity-50 animate-pulse" />
            
            {/* Main logo container */}
            <motion.div
              whileHover={{ scale: 1.05 }}
              className="relative w-28 h-28 bg-gradient-to-br from-hasselblad via-hasselblad-light to-hasselblad rounded-3xl flex items-center justify-center shadow-2xl"
              style={{
                boxShadow: '0 20px 60px rgba(201, 168, 108, 0.4), 0 0 80px rgba(201, 168, 108, 0.2)',
              }}
            >
              <Camera className="w-16 h-16 text-surface-900" />
              
              {/* Inner shimmer effect */}
              <div className="absolute inset-0 rounded-3xl overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-br from-white/20 to-transparent" />
              </div>
            </motion.div>
          </div>
        </motion.div>

        {/* Premium Title with Gradient */}
        <motion.h1
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4, duration: 0.8, ease: 'easeOut' }}
          className="text-display-xl md:text-display-lg mb-6"
        >
          <span className="gradient-text">小O帮帮</span>
        </motion.h1>

        {/* Professional Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6, duration: 0.6 }}
          className="text-headline-lg text-surface-100/80 mb-4 max-w-2xl mx-auto"
        >
          哈苏影像系统级参数中枢
        </motion.p>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7, duration: 0.6 }}
          className="text-body-xl text-surface-200/60 mb-10 max-w-3xl mx-auto"
        >
          OPPO × Hasselblad 联合调校 | AI智能场景识别 | 专业摄影师推荐
        </motion.p>

        {/* Premium Feature Tags - Photography Oriented */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.8 }}
          className="flex flex-wrap justify-center gap-4 mb-14"
        >
          {[
            { icon: Aperture, label: '哈苏认证', color: 'text-hasselblad', gradient: 'from-hasselblad/20 to-hasselblad/5', border: 'border-hasselblad/30' },
            { icon: Sparkles, label: 'AI场景识别', color: 'text-aqua-primary', gradient: 'from-aqua-primary/20 to-aqua-primary/5', border: 'border-aqua-primary/30' },
            { icon: Palette, label: '专业调色', color: 'text-oppo-coral', gradient: 'from-oppo-coral/20 to-oppo-coral/5', border: 'border-oppo-coral/30' },
            { icon: Zap, label: '一键应用', color: 'text-oppo-green', gradient: 'from-oppo-green/20 to-oppo-green/5', border: 'border-oppo-green/30' },
          ].map((feature, idx) => (
            <motion.div
              key={feature.label}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.9 + idx * 0.1, type: 'spring', stiffness: 200 }}
              whileHover={{ scale: 1.05, y: -3 }}
              className={`px-5 py-3 bg-gradient-to-br ${feature.gradient} border ${feature.border} rounded-full flex items-center gap-2.5 backdrop-blur-sm`}
            >
              <feature.icon className={`w-5 h-5 ${feature.color}`} />
              <span className="text-body-md font-medium text-surface-0">{feature.label}</span>
            </motion.div>
          ))}
        </motion.div>

        {/* Premium CTA Buttons - Photography Theme */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.3 }}
          className="flex flex-col sm:flex-row items-center justify-center gap-5"
        >
          <Link to="/ai-demo">
            <motion.button
              whileHover={{ scale: 1.02, y: -2 }}
              whileTap={{ scale: 0.98 }}
              className="group relative px-10 py-4 text-body-lg font-semibold rounded-radius-md overflow-hidden"
              style={{
                background: 'linear-gradient(135deg, #C9A86C 0%, #D4B87A 50%, #C9A86C 100%)',
                boxShadow: '0 8px 30px rgba(201, 168, 108, 0.4)',
              }}
            >
              {/* Shimmer effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-700" />
              
              <span className="relative text-surface-900 flex items-center gap-2">
                <Aperture className="w-5 h-5" />
                立即体验AI功能
              </span>
            </motion.button>
          </Link>

          <Link to="/home">
            <motion.button
              whileHover={{ scale: 1.02, y: -2 }}
              whileTap={{ scale: 0.98 }}
              className="px-10 py-4 text-body-lg font-semibold rounded-radius-md border-2 border-hasselblad/50 text-hasselblad hover:bg-hasselblad/10 transition-all duration-normal"
            >
              <span className="flex items-center gap-2">
                <Palette className="w-5 h-5" />
                浏览影像推荐
              </span>
            </motion.button>
          </Link>
        </motion.div>

        {/* Professional Stats - Photography Metrics */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.5 }}
          className="mt-16 grid grid-cols-3 gap-8 max-w-2xl mx-auto"
        >
          {[
            { value: '50+', label: '哈苏认证预设', icon: Star },
            { value: '<1.5s', label: 'AI识别速度', icon: Zap },
            { value: '15+', label: '场景类型覆盖', icon: Aperture },
          ].map((stat, idx) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 1.6 + idx * 0.1 }}
              className="text-center"
            >
              <div className="text-display-md text-hasselblad mb-2 font-bold">{stat.value}</div>
              <div className="text-body-sm text-surface-200/60">{stat.label}</div>
            </motion.div>
          ))}
        </motion.div>
      </div>

      {/* Bottom gradient fade */}
      <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-surface-800 to-transparent" />
    </section>
  );
}