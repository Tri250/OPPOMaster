import { motion } from 'framer-motion';
import { Sparkles, ArrowRight, Camera } from 'lucide-react';
import { Link } from 'react-router-dom';

// 光粒子组件
const LightParticles = () => {
  const particles = Array.from({ length: 60 }, (_, i) => ({
    id: i,
    x: Math.random() * 100,
    y: Math.random() * 100,
    size: Math.random() * 2 + 1,
    duration: Math.random() * 20 + 10,
    delay: Math.random() * 2,
  }));

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none">
      {particles.map((particle) => (
        <motion.div
          key={particle.id}
          className="absolute rounded-full bg-hasselblad/30"
          style={{
            left: `${particle.x}%`,
            top: `${particle.y}%`,
            width: `${particle.size}px`,
            height: `${particle.size}px`,
          }}
          animate={{
            y: [0, -20, 0],
            opacity: [0.2, 0.6, 0.2],
          }}
          transition={{
            duration: particle.duration,
            repeat: Infinity,
            delay: particle.delay,
            ease: 'easeInOut',
          }}
        />
      ))}
    </div>
  );
};

export default function HeroSection() {
  return (
    <section className="relative min-h-[60vh] md:min-h-[70vh] flex items-center justify-center overflow-hidden pt-20 pb-12">
      {/* 背景渐变 */}
      <div className="absolute inset-0 bg-gradient-to-b from-deep-space via-deep-spaceLight to-deep-space">
        {/* 光效模糊区域 */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-oppo-coral/10 rounded-full blur-[120px] opacity-60 animate-pulse" />
        <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-hasselblad/10 rounded-full blur-[100px] opacity-60 animate-pulse" style={{ animationDelay: '1.5s' }} />
      </div>
      
      {/* 光粒子效果 */}
      <LightParticles />

      <div className="relative z-10 max-w-5xl mx-auto px-4 sm:px-6 text-center">
        {/* Logo区域 */}
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8, type: 'spring', stiffness: 80 }}
          className="mb-10"
        >
          <div className="relative inline-flex">
            {/* 光晕背景 */}
            <div className="absolute inset-0 bg-gradient-to-br from-oppo-coral/30 to-hasselblad/30 rounded-3xl blur-xl opacity-60 animate-pulse-glow" />
            
            {/* 主Logo容器 */}
            <motion.div
              whileHover={{ scale: 1.05, rotate: 2 }}
              whileTap={{ scale: 0.95 }}
              className="relative w-28 h-28 bg-gradient-to-br from-deep-spaceLight via-surface-700 to-deep-spaceLight rounded-3xl flex items-center justify-center border border-hasselblad/30 shadow-glow-orange"
            >
              <Camera className="w-16 h-16 text-hasselblad" />
              {/* 光泽效果 */}
              <div className="absolute inset-0 rounded-3xl bg-gradient-to-br from-white/20 via-transparent to-transparent pointer-events-none" />
            </motion.div>
          </div>
        </motion.div>

        {/* 主标题 */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.6, ease: 'easeOut' }}
          className="mb-4"
        >
          <h1 className="text-[clamp(32px,6vw,44px)] font-bold leading-tight">
            <span className="text-gradient-hasselblad">OPPO Master</span>
          </h1>
        </motion.div>

        {/* 副标题和哈苏标识 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.5 }}
          className="mb-3"
        >
          <div className="flex items-center justify-center gap-4 flex-wrap">
            <p className="text-[clamp(16px,3vw,20px)] text-neutral-70">
              哈苏影像系统级参数库
            </p>
            <span className="text-hasselblad font-bold text-lg animate-pulse-glow">
              Hasselblad
            </span>
          </div>
        </motion.div>

        {/* 描述文字 */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7, duration: 0.5 }}
          className="text-body-lg text-neutral-60 mb-12 max-w-2xl mx-auto"
        >
          哈苏认证预设 · AI智能场景识别 · 专业参数管理
        </motion.p>

        {/* 按钮区域 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.9, duration: 0.5 }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <Link to="/ai-demo">
            <motion.button
              whileHover={{ scale: 1.03 }}
              whileTap={{ scale: 0.97 }}
              className="relative px-8 py-4 bg-gradient-to-r from-oppo-coral to-oppo-coralLight text-white font-medium rounded-lg shadow-glow-coral hover:shadow-glow-coral transition-all overflow-hidden ripple"
            >
              <span className="relative flex items-center gap-2 text-display-sm">
                <Sparkles className="w-5 h-5" />
                立即体验
              </span>
            </motion.button>
          </Link>

          <Link to="/tech">
            <motion.button
              whileHover={{ scale: 1.03 }}
              whileTap={{ scale: 0.97 }}
              className="px-8 py-4 bg-white/10 text-white font-medium rounded-lg border border-white/20 hover:bg-white/20 transition-all flex items-center gap-2 ripple"
            >
              <span className="text-display-sm">了解更多</span>
              <ArrowRight className="w-5 h-5" />
            </motion.button>
          </Link>
        </motion.div>

        {/* 快速入口标签 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.2, duration: 0.6 }}
          className="mt-14 flex flex-wrap items-center justify-center gap-4"
        >
          {[
            { label: '哈苏认证', color: 'text-hasselblad', bg: 'bg-hasselblad/10', border: 'border-hasselblad/30' },
            { label: 'AI智能', color: 'text-oppo-coral', bg: 'bg-oppo-coral/10', border: 'border-oppo-coral/30' },
            { label: '专业预设', color: 'text-oppo-green', bg: 'bg-oppo-green/10', border: 'border-oppo-green/30' },
          ].map((tag, index) => (
            <motion.div
              key={tag.label}
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 1.3 + index * 0.1, type: 'spring', stiffness: 150 }}
              whileHover={{ y: -3, scale: 1.05 }}
              className={`px-5 py-2 ${tag.bg} border ${tag.border} rounded-full backdrop-blur-sm`}
            >
              <span className={`font-medium ${tag.color} text-body-md`}>
                {tag.label}
              </span>
            </motion.div>
          ))}
        </motion.div>
      </div>

      {/* 底部渐变光边 */}
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-oppo-coral/50 to-transparent opacity-60" />
    </section>
  );
}
