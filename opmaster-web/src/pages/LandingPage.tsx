import { motion } from 'framer-motion';
import { Sparkles, Camera, Palette, Zap, Shield, Smartphone, Globe, Cpu, Sun, Moon, Mountain, Users, Star, Check, ArrowRight } from 'lucide-react';

const features = [
  {
    icon: <Sparkles className="w-8 h-8" />,
    title: 'DeepSeek AI 智能识别',
    description: '基于先进的DeepSeek AI技术，智能识别15种拍摄场景，一键推荐最佳影像参数',
    color: 'from-cyan-400 to-blue-500',
    stats: '准确率 ≥90%'
  },
  {
    icon: <Camera className="w-8 h-8" />,
    title: '哈苏大师预设',
    description: '专业级影像参数预设，覆盖人像、风景、夜景、美食等多种场景，一键应用大师级效果',
    color: 'from-amber-400 to-orange-500',
    stats: '50+ 精选预设'
  },
  {
    icon: <Palette className="w-8 h-8" />,
    title: 'ColorOS 16 设计',
    description: '遵循OPPO最新设计规范，日落金配色、流畅动画，打造原生级用户体验',
    color: 'from-pink-400 to-rose-500',
    stats: '原生体验'
  },
  {
    icon: <Zap className="w-8 h-8" />,
    title: '极速响应',
    description: 'AI识别响应时间≤500ms，预设应用即时生效，无需等待即刻呈现',
    color: 'from-yellow-400 to-amber-500',
    stats: '≤500ms'
  },
  {
    icon: <Shield className="w-8 h-8" />,
    title: '隐私安全',
    description: '本地处理优先，图片不上传云端，遵循ColorOS隐私规范，保护用户数据',
    color: 'from-emerald-400 to-teal-500',
    stats: '100% 安全'
  },
  {
    icon: <Globe className="w-8 h-8" />,
    title: '跨平台同步',
    description: 'Android与Web端功能完全一致，随时随地访问您的影像预设库',
    color: 'from-violet-400 to-purple-500',
    stats: '双端同步'
  }
];

const sceneTypes = [
  { name: '人像', icon: <Users className="w-6 h-6" />, color: 'bg-pink-500' },
  { name: '风景', icon: <Mountain className="w-6 h-6" />, color: 'bg-green-500' },
  { name: '夜景', icon: <Moon className="w-6 h-6" />, color: 'bg-indigo-500' },
  { name: '日落', icon: <Sun className="w-6 h-6" />, color: 'bg-orange-500' },
  { name: '美食', icon: <Palette className="w-6 h-6" />, color: 'bg-red-500' },
  { name: '微距', icon: <Cpu className="w-6 h-6" />, color: 'bg-purple-500' },
  { name: '运动', icon: <Sparkles className="w-6 h-6" />, color: 'bg-cyan-500' },
  { name: '建筑', icon: <Globe className="w-6 h-6" />, color: 'bg-blue-500' }
];

const techStack = [
  { name: 'DeepSeek AI', role: '智能场景识别' },
  { name: 'Android 16', role: '原生应用' },
  { name: 'React 19', role: 'Web端框架' },
  { name: 'ColorOS 16', role: '设计规范' },
  { name: 'Jetpack Compose', role: 'Android UI' },
  { name: '哈苏 HNCS', role: '专业色彩' }
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.15,
      delayChildren: 0.3
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, y: 30 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.6,
      ease: [0.25, 0.46, 0.45, 0.94]
    }
  }
};

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-slate-950 text-white overflow-x-hidden">
      {/* Animated Background */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-gradient-to-br from-amber-500/20 to-transparent rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-gradient-to-br from-orange-500/15 to-transparent rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />
        <div className="absolute top-1/2 right-0 w-64 h-64 bg-gradient-to-bl from-cyan-500/10 to-transparent rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
      </div>

      {/* Hero Section */}
      <section className="relative min-h-screen flex items-center justify-center px-4 py-20">
        <div className="max-w-6xl mx-auto text-center relative z-10">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.8 }}
            className="mb-8"
          >
            <div className="inline-flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-amber-500/20 to-orange-500/20 border border-amber-500/30 rounded-full mb-8">
              <Sparkles className="w-4 h-4 text-amber-400" />
              <span className="text-sm text-amber-300 font-medium">Powered by DeepSeek AI</span>
            </div>
            
            <h1 className="text-5xl md:text-7xl font-bold mb-6 bg-gradient-to-r from-white via-amber-100 to-amber-200 bg-clip-text text-transparent">
              OPPOMaster
            </h1>
            
            <p className="text-xl md:text-2xl text-slate-400 mb-4 max-w-3xl mx-auto">
              小O帮帮 · AI智能影像专家
            </p>
            
            <p className="text-lg text-slate-500 mb-12 max-w-2xl mx-auto leading-relaxed">
              融合DeepSeek AI智能技术与哈苏专业影像系统，一键识别拍摄场景，智能推荐最佳影像参数，让每一张照片都呈现大师级效果。
            </p>

            <div className="flex flex-wrap gap-4 justify-center mb-16">
              <motion.button
                whileHover={{ scale: 1.05, boxShadow: '0 20px 40px rgba(245, 158, 11, 0.3)' }}
                whileTap={{ scale: 0.95 }}
                className="px-8 py-4 bg-gradient-to-r from-amber-500 to-orange-500 text-slate-900 font-bold rounded-2xl flex items-center gap-2 shadow-lg shadow-amber-500/20"
              >
                <Smartphone className="w-5 h-5" />
                下载Android版
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="px-8 py-4 bg-slate-800/50 border border-slate-700 text-white font-bold rounded-2xl flex items-center gap-2 hover:bg-slate-700/50 transition-colors"
              >
                <Globe className="w-5 h-5" />
                在线体验Web版
              </motion.button>
            </div>

            {/* Stats */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5, duration: 0.6 }}
              className="grid grid-cols-2 md:grid-cols-4 gap-6 max-w-3xl mx-auto"
            >
              {[
                { value: '15+', label: '智能场景' },
                { value: '50+', label: '哈苏预设' },
                { value: '≤500ms', label: '识别速度' },
                { value: '≥90%', label: '识别准确率' }
              ].map((stat, index) => (
                <motion.div
                  key={index}
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.6 + index * 0.1 }}
                  className="bg-slate-800/30 backdrop-blur-sm border border-slate-700/50 rounded-2xl p-6"
                >
                  <div className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent mb-2">
                    {stat.value}
                  </div>
                  <div className="text-sm text-slate-400">{stat.label}</div>
                </motion.div>
              ))}
            </motion.div>
          </motion.div>
        </div>

        {/* Scroll Indicator */}
        <motion.div
          animate={{ y: [0, 10, 0] }}
          transition={{ duration: 2, repeat: Infinity }}
          className="absolute bottom-8 left-1/2 -translate-x-1/2"
        >
          <div className="w-6 h-10 border-2 border-slate-600 rounded-full flex justify-center pt-2">
            <motion.div
              animate={{ y: [0, 12, 0] }}
              transition={{ duration: 2, repeat: Infinity }}
              className="w-1.5 h-1.5 bg-slate-400 rounded-full"
            />
          </div>
        </motion.div>
      </section>

      {/* Features Section */}
      <section className="relative py-32 px-4">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-20"
          >
            <h2 className="text-4xl md:text-5xl font-bold mb-6 bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
              核心功能
            </h2>
            <p className="text-lg text-slate-400 max-w-2xl mx-auto">
              融合前沿AI技术与专业影像系统，打造极致的移动影像体验
            </p>
          </motion.div>

          <motion.div
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8"
          >
            {features.map((feature, index) => (
              <motion.div
                key={index}
                variants={itemVariants}
                whileHover={{ y: -8, transition: { duration: 0.3 } }}
                className="relative group"
              >
                <div className="absolute inset-0 bg-gradient-to-br from-slate-800/50 to-slate-900/50 backdrop-blur-xl border border-slate-700/50 rounded-3xl p-8 h-full group-hover:border-amber-500/30 transition-all duration-300">
                  <div className={`inline-flex p-3 rounded-2xl bg-gradient-to-br ${feature.color} mb-6`}>
                    <div className="text-white">
                      {feature.icon}
                    </div>
                  </div>
                  
                  <h3 className="text-2xl font-bold mb-4 text-white group-hover:text-amber-300 transition-colors">
                    {feature.title}
                  </h3>
                  
                  <p className="text-slate-400 leading-relaxed mb-6">
                    {feature.description}
                  </p>
                  
                  <div className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span className="text-sm text-emerald-400 font-medium">{feature.stats}</span>
                  </div>
                </div>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* Scene Types Section */}
      <section className="relative py-32 px-4 bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-20"
          >
            <h2 className="text-4xl md:text-5xl font-bold mb-6 bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
              智能场景识别
            </h2>
            <p className="text-lg text-slate-400 max-w-2xl mx-auto">
              DeepSeek AI 驱动的智能识别系统，支持15+种拍摄场景自动检测
            </p>
          </motion.div>

          <motion.div
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            className="flex flex-wrap justify-center gap-4 mb-16"
          >
            {sceneTypes.map((scene, index) => (
              <motion.div
                key={index}
                variants={itemVariants}
                whileHover={{ scale: 1.1, y: -4 }}
                className={`${scene.color} px-6 py-4 rounded-2xl flex items-center gap-3 text-white shadow-lg`}
              >
                {scene.icon}
                <span className="font-semibold">{scene.name}</span>
              </motion.div>
            ))}
          </motion.div>

          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            transition={{ delay: 0.3 }}
            className="relative bg-gradient-to-br from-slate-800/30 to-slate-900/30 backdrop-blur-xl border border-slate-700/50 rounded-3xl p-12 text-center"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-amber-500/5 to-orange-500/5 rounded-3xl" />
            <div className="relative z-10">
              <div className="inline-flex p-4 rounded-full bg-gradient-to-br from-amber-500/20 to-orange-500/20 mb-6">
                <Sparkles className="w-12 h-12 text-amber-400" />
              </div>
              <h3 className="text-3xl font-bold mb-4 text-white">
                一键识别 · 智能推荐
              </h3>
              <p className="text-lg text-slate-400 mb-8 max-w-2xl mx-auto">
                选择照片后，AI自动分析场景内容，智能匹配最合适的哈苏大师预设，无需手动调整参数，即可获得专业级影像效果。
              </p>
              <div className="flex flex-wrap gap-3 justify-center">
                {['智能检测', '自动匹配', '一键应用'].map((tag, index) => (
                  <span key={index} className="px-4 py-2 bg-slate-800/50 border border-slate-700 rounded-full text-sm text-slate-300">
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Tech Stack Section */}
      <section className="relative py-32 px-4">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-20"
          >
            <h2 className="text-4xl md:text-5xl font-bold mb-6 bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
              技术架构
            </h2>
            <p className="text-lg text-slate-400 max-w-2xl mx-auto">
              采用最新技术栈，打造高性能、高可用的跨平台应用
            </p>
          </motion.div>

          <motion.div
            variants={containerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6"
          >
            {techStack.map((tech, index) => (
              <motion.div
                key={index}
                variants={itemVariants}
                whileHover={{ scale: 1.05, borderColor: 'rgba(245, 158, 11, 0.5)' }}
                className="bg-slate-800/30 backdrop-blur-sm border border-slate-700/50 rounded-2xl p-6 text-center hover:border-amber-500/30 transition-all"
              >
                <div className="text-2xl font-bold text-amber-400 mb-2">{tech.name}</div>
                <div className="text-sm text-slate-400">{tech.role}</div>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="relative py-32 px-4">
        <div className="max-w-4xl mx-auto">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="relative bg-gradient-to-br from-amber-500/10 via-orange-500/10 to-amber-500/5 border border-amber-500/20 rounded-3xl p-16 text-center overflow-hidden"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-amber-500/5 to-transparent" />
            <div className="absolute -top-20 -right-20 w-64 h-64 bg-gradient-to-br from-amber-500/20 to-transparent rounded-full blur-3xl" />
            <div className="absolute -bottom-20 -left-20 w-64 h-64 bg-gradient-to-br from-orange-500/20 to-transparent rounded-full blur-3xl" />
            
            <div className="relative z-10">
              <h2 className="text-4xl md:text-5xl font-bold mb-6 text-white">
                立即体验 AI 影像魅力
              </h2>
              <p className="text-xl text-slate-400 mb-10 max-w-2xl mx-auto">
                下载Android应用或在线体验Web版本，让DeepSeek AI为您的每一张照片增添大师级光彩
              </p>
              
              <div className="flex flex-wrap gap-4 justify-center">
                <motion.button
                  whileHover={{ scale: 1.05, boxShadow: '0 20px 40px rgba(245, 158, 11, 0.4)' }}
                  whileTap={{ scale: 0.95 }}
                  className="px-10 py-5 bg-gradient-to-r from-amber-500 to-orange-500 text-slate-900 font-bold text-lg rounded-2xl flex items-center gap-3 shadow-lg shadow-amber-500/30"
                >
                  <Smartphone className="w-6 h-6" />
                  立即下载
                  <ArrowRight className="w-5 h-5" />
                </motion.button>
              </div>

              <div className="mt-12 flex items-center justify-center gap-6 text-sm text-slate-500">
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-emerald-400" />
                  <span>免费使用</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-emerald-400" />
                  <span>无需注册</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-emerald-400" />
                  <span>隐私保护</span>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-slate-800 py-12 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-gradient-to-br from-amber-500 to-orange-500 rounded-xl flex items-center justify-center">
                <Camera className="w-5 h-5 text-white" />
              </div>
              <div>
                <div className="font-bold text-white">OPPOMaster</div>
                <div className="text-sm text-slate-500">小O帮帮 · AI影像专家</div>
              </div>
            </div>
            
            <div className="text-center md:text-right">
              <div className="text-sm text-slate-500">
                © 2026 OPPOMaster. All rights reserved.
              </div>
              <div className="text-sm text-slate-600 mt-1">
                Powered by DeepSeek AI · ColorOS 16 Design
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
