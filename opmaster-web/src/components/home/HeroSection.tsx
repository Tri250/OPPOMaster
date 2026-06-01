import { motion } from 'framer-motion';
import { Camera, Sparkles, Star, Smartphone, Download } from 'lucide-react';

export default function HeroSection() {
  return (
    <section className="relative min-h-[70vh] flex items-center justify-center overflow-hidden pt-16">
      {/* Background Effects */}
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-b from-deep-space via-deep-space-light to-deep-space" />
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-hasselblad/10 rounded-full blur-3xl animate-float" />
        <div className="absolute bottom-1/4 right-1/4 w-64 h-64 bg-oppo-green/10 rounded-full blur-3xl animate-float" style={{ animationDelay: '3s' }} />
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
          <span className="gradient-text">小O帮帮</span>
        </motion.h1>

        {/* Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="text-xl md:text-2xl text-white/70 mb-4 max-w-3xl mx-auto"
        >
          OPPO哈苏影像系统级参数中枢
        </motion.p>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
          className="text-lg text-white/50 mb-8"
        >
          专为OPPO/一加Android设备打造的专业摄影助手
        </motion.p>

        {/* Feature Tags - Simplified */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
          className="flex flex-wrap justify-center gap-3 mb-12"
        >
          {[
            { icon: Star, label: '哈苏认证', color: 'text-hasselblad' },
            { icon: Sparkles, label: 'AI智能', color: 'text-purple-400' },
            { icon: Smartphone, label: '悬浮窗', color: 'text-pink-400' },
            { icon: Download, label: 'Android专属', color: 'text-green-400' }
          ].map((feature, idx) => (
            <motion.div
              key={feature.label}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.8 + idx * 0.08 }}
              whileHover={{ scale: 1.05, y: -2 }}
              className="glass-effect px-5 py-2.5 rounded-full flex items-center space-x-2 backdrop-blur-md border border-white/10"
            >
              <feature.icon className={`w-4.5 h-4.5 ${feature.color}`} />
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
          <button onClick={() => alert('APK下载即将上线，敬请期待！')} className="btn-primary text-lg px-8 py-4 animate-pulse-glow inline-flex items-center gap-2">
            <Download className="w-5 h-5" />
            下载Android应用
          </button>
        </motion.div>
      </div>
    </section>
  );
}
