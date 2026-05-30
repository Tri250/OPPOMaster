import { motion } from 'framer-motion';
import { Camera, Sparkles, Star, Smartphone } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function HeroSection() {
  return (
    <section className="relative min-h-[80vh] flex items-center justify-center overflow-hidden pt-16">
      {/* Background effects */}
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-br from-deep-space via-deep-space-light to-[#1a1a1a]" />
        <div className="absolute top-1/4 left-1/4 w-[300px] h-[300px] bg-hasselblad/20 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/4 right-1/4 w-[250px] h-[250px] bg-white/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />
      </div>

      <div className="relative z-10 max-w-4xl mx-auto px-6 text-center">
        {/* Logo */}
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ duration: 1, type: 'spring', stiffness: 100 }}
          className="mb-8"
        >
          <div className="w-24 h-24 bg-gradient-to-br from-hasselblad to-hasselblad/80 rounded-2xl flex items-center justify-center mx-auto shadow-lg shadow-hasselblad/30">
            <Camera className="w-14 h-14 text-deep-space" />
          </div>
        </motion.div>

        {/* Title */}
        <motion.h1
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.6 }}
          className="text-5xl md:text-6xl font-bold mb-4"
        >
          <span className="gradient-text">小O帮帮</span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.5 }}
          className="text-xl text-white/80 mb-6"
        >
          哈苏影像系统级参数中枢
        </motion.p>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6, duration: 0.5 }}
          className="text-white/60 mb-10 max-w-2xl mx-auto"
        >
          OPPO × Hasselblad 联合调校 | AI智能场景识别 | 专业摄影师推荐
        </motion.p>

        {/* Feature tags */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
          className="flex flex-wrap justify-center gap-3 mb-10"
        >
          {[
            { icon: Sparkles, label: 'AI智能', color: 'text-hasselblad' },
            { icon: Star, label: '哈苏认证', color: 'text-white' },
            { icon: Smartphone, label: 'OPPO生态', color: 'text-white' },
          ].map((feature, idx) => (
            <motion.div
              key={feature.label}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.8 + idx * 0.1 }}
              className="px-4 py-2 bg-white/10 rounded-full flex items-center gap-2"
            >
              <feature.icon className={`w-4 h-4 ${feature.color}`} />
              <span className="text-sm">{feature.label}</span>
            </motion.div>
          ))}
        </motion.div>

        {/* CTA Buttons */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.1 }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <Link to="/ai-demo">
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="btn-primary"
            >
              立即体验AI功能
            </motion.button>
          </Link>
          <Link to="/home">
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="btn-secondary"
            >
              浏览影像推荐
            </motion.button>
          </Link>
        </motion.div>
      </div>
    </section>
  );
}