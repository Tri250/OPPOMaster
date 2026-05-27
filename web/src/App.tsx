import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Camera, Sparkles, Cloud, Share2, Zap, Palette, Layers, Star, Heart, ChevronRight, Settings, Search } from 'lucide-react';
import './index.css';

const PRESETS = [
  {
    id: 1,
    name: "哈苏 X2D · 慵懒午后的佛罗伦萨",
    cover: "https://images.unsplash.com/photo-1543883268-09b4233b28e8?w=400&h=400&fit=crop",
    tags: ["风景", "哈苏", "暖调"],
    device: "Find X8 Pro",
    hasselblad: true,
    favorite: true
  },
  {
    id: 2,
    name: "京都夜色 · 霓虹光斑",
    cover: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=400&h=400&fit=crop",
    tags: ["夜景", "城市"],
    device: "Find X8 Ultra",
    hasselblad: false,
    favorite: false
  },
  {
    id: 3,
    name: "北欧森林 · 自然清新",
    cover: "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400&h=400&fit=crop",
    tags: ["风景", "自然"],
    device: "Reno 12 Pro",
    hasselblad: true,
    favorite: false
  },
  {
    id: 4,
    name: "人像大师 · 柔肤磨皮",
    cover: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=400&fit=crop",
    tags: ["人像", "哈苏"],
    device: "Find X8 Ultra",
    hasselblad: true,
    favorite: true
  }
];

const FEATURES = [
  {
    icon: <Sparkles className="w-6 h-6" />,
    title: "智能场景检测",
    description: "AI 自动识别拍摄场景，智能推荐最佳参数预设"
  },
  {
    icon: <Layers className="w-6 h-6" />,
    title: "哈苏自然色彩",
    description: "专业级 HNCS 哈苏自然色彩解决方案"
  },
  {
    icon: <Cloud className="w-6 h-6" />,
    title: "云端同步",
    description: "多设备预设同步，随时随地拍摄创作"
  },
  {
    icon: <Share2 className="w-6 h-6" />,
    title: "社区分享",
    description: "发现全球摄影师的创作灵感，分享你的预设"
  },
  {
    icon: <Zap className="w-6 h-6" />,
    title: "一键应用",
    description: "快速将预设应用到系统相机，告别复杂设置"
  },
  {
    icon: <Palette className="w-6 h-6" />,
    title: "自定义预设",
    description: "创建并管理属于你的专属摄影风格"
  }
];

function PresetCard({ preset }) {
  return (
    <motion.div
      whileHover={{ scale: 1.02 }}
      className="preset-card cursor-pointer group"
    >
      <div className="relative aspect-[4/3] overflow-hidden">
        <img
          src={preset.cover}
          alt={preset.name}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
        {preset.hasselblad && (
          <div className="absolute top-3 right-3">
            <span className="hasselblad-badge">HNCS</span>
          </div>
        )}
        <button className="absolute top-3 left-3 p-2 rounded-full bg-black/30 backdrop-blur-sm hover:bg-black/50 transition-colors">
          <Heart className={`w-4 h-4 ${preset.favorite ? 'fill-red-500 text-red-500' : 'text-white'}`} />
        </button>
      </div>
      <div className="p-4">
        <h3 className="font-semibold text-white mb-2 line-clamp-2 leading-tight">
          {preset.name}
        </h3>
        <div className="flex items-center gap-2 mb-3">
          <span className="text-xs text-gray-400 bg-white/5 px-2 py-1 rounded-full">
            {preset.device}
          </span>
        </div>
        <div className="flex flex-wrap gap-1">
          {preset.tags.map(tag => (
            <span key={tag} className="text-xs text-cyan-300 bg-cyan-500/10 px-2 py-0.5 rounded-full">
              {tag}
            </span>
          ))}
        </div>
      </div>
    </motion.div>
  );
}

function PhoneScreen({ activeTab }) {
  return (
    <div className="phone-screen flex flex-col">
      <div className="h-12 px-6 flex items-center justify-between bg-gradient-to-b from-black/30 to-transparent">
        <span className="text-xs text-white/70">9:41</span>
        <div className="flex gap-1">
          <div className="w-4 h-2 rounded-sm border border-white/70" />
          <div className="w-3 h-2 rounded-sm bg-white/70" />
          <div className="w-5 h-2 rounded-sm bg-white/70" />
        </div>
      </div>
      <div className="flex-1 overflow-y-auto bg-gradient-to-b from-[#12121a] via-[#12121a] to-[#0a0a10]">
        {activeTab === 'home' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-4">
            <div className="mb-6">
              <h1 className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent">
                OMaster
              </h1>
              <p className="text-gray-400 text-sm mt-1">OPPO 哈苏影像 · 大师级参数中枢</p>
            </div>
            <div className="flex items-center gap-3 bg-white/5 rounded-xl px-4 py-3 mb-6 border border-white/10">
              <Search className="w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="搜索预设..."
                className="bg-transparent border-none outline-none text-sm flex-1 text-white placeholder-gray-500"
              />
            </div>
            <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
              {['全部', '收藏', 'HNCS', 'Find X', 'Reno'].map((chip, i) => (
                <button
                  key={chip}
                  className={`px-4 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-all ${
                    i === 0 ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30' : 'bg-white/5 text-gray-400 hover:bg-white/10'
                  }`}
                >
                  {chip}
                </button>
              ))}
            </div>
            <div className="grid grid-cols-2 gap-3">
              {PRESETS.map(preset => (
                <PresetCard key={preset.id} preset={preset} />
              ))}
            </div>
          </motion.div>
        )}
        {activeTab === 'scene' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-4">
            <h2 className="text-lg font-bold text-white mb-4">场景检测</h2>
            <div className="grid grid-cols-2 gap-3">
              {['风景', '人像', '夜景', '美食', '街拍', '建筑'].map((scene, i) => {
                const emojis = ['🏔️', '👤', '🌃', '🍜', '📷', '🏛️'];
                return (
                  <button key={scene} className="bg-white/5 border border-white/10 rounded-2xl p-4 hover:bg-white/10 transition-colors">
                    <div className="text-2xl mb-2">{emojis[i]}</div>
                    <span className="text-white text-sm font-medium">{scene}</span>
                  </button>
                );
              })}
            </div>
          </motion.div>
        )}
        {activeTab === 'community' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-4">
            <h2 className="text-lg font-bold text-white mb-4">社区发现</h2>
            <div className="space-y-4">
              {PRESETS.slice(0, 3).map((preset) => (
                <div key={preset.id} className="bg-white/5 rounded-2xl p-4 border border-white/10">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-500 to-purple-500" />
                    <div>
                      <p className="text-white text-sm font-medium">摄影师{preset.id}</p>
                      <p className="text-gray-500 text-xs">专业摄影</p>
                    </div>
                  </div>
                  <img src={preset.cover} alt="" className="w-full h-40 object-cover rounded-xl" />
                  <p className="text-white text-sm mt-3">{preset.name}</p>
                </div>
              ))}
            </div>
          </motion.div>
        )}
        {activeTab === 'settings' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-4">
            <h2 className="text-lg font-bold text-white mb-4">设置</h2>
            <div className="space-y-3">
              {[
                { label: '主题模式', value: '跟随系统' },
                { label: '流体云胶囊', value: '已开启' },
                { label: '云端同步', value: '已启用' },
                { label: '数据备份', value: '立即备份' }
              ].map((item, i) => (
                <div key={i} className="bg-white/5 border border-white/10 rounded-xl p-4 flex items-center justify-between">
                  <span className="text-white text-sm">{item.label}</span>
                  <span className="text-cyan-400 text-sm">{item.value}</span>
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </div>
      <div className="bg-[#12121a]/95 backdrop-blur-xl border-t border-white/5 px-6 py-3">
        <div className="flex items-center justify-around">
          {[
            { id: 'home', icon: Camera, label: '首页' },
            { id: 'scene', icon: Sparkles, label: '场景' },
            { id: 'community', icon: Layers, label: '社区' },
            { id: 'settings', icon: Settings, label: '设置' }
          ].map((tab) => {
            const IconComponent = tab.icon;
            return (
              <button key={tab.id} className={`flex flex-col items-center gap-1 ${activeTab === tab.id ? 'text-cyan-400' : 'text-gray-500'}`}>
                <IconComponent className="w-6 h-6" />
                <span className="text-[10px]">{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function Hero() {
  const [activeTab, setActiveTab] = useState('home');
  const [isDemoMode, setIsDemoMode] = useState(false);

  return (
    <section className="relative z-10 min-h-screen flex items-center">
      <div className="max-w-7xl mx-auto px-6 lg:px-8 w-full">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          <motion.div
            initial={{ opacity: 0, x: -50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8 }}
          >
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 mb-6">
              <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
              <span className="text-sm text-gray-300">v1.0 正式发布</span>
            </div>
            <h1 className="text-5xl lg:text-7xl font-bold font-display leading-tight mb-6">
              <span className="glow-text">OPPO</span>
              <br />
              <span className="bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-500 bg-clip-text text-transparent">
                哈苏影像 · 大师级
              </span>
              <br />
              <span className="text-white">参数中枢</span>
            </h1>
            <p className="text-lg text-gray-400 mb-8 max-w-lg leading-relaxed">
              专为 OPPO 和一加设备打造的专业相机参数管理应用，
              发现来自全球摄影师的哈苏风格预设，轻松一键应用。
            </p>
            <div className="flex flex-wrap gap-4 mb-12">
              <button className="button-primary px-8 py-4 rounded-2xl text-deep-space font-semibold text-lg flex items-center gap-2">
                <span>下载 OMaster</span>
                <ChevronRight className="w-5 h-5" />
              </button>
              <button
                onClick={() => setIsDemoMode(!isDemoMode)}
                className="px-8 py-4 rounded-2xl border border-white/20 text-white font-semibold text-lg hover:bg-white/5 transition-colors flex items-center gap-2"
              >
                <span>交互演示</span>
              </button>
            </div>
            <div className="grid grid-cols-3 gap-8 max-w-md">
              <div>
                <p className="text-3xl font-bold text-cyan-400">100+</p>
                <p className="text-gray-500 text-sm">预设模板</p>
              </div>
              <div>
                <p className="text-3xl font-bold text-orange-400">10K+</p>
                <p className="text-gray-500 text-sm">用户喜爱</p>
              </div>
              <div>
                <p className="text-3xl font-bold text-purple-400">4.9</p>
                <p className="text-gray-500 text-sm">用户评分</p>
              </div>
            </div>
          </motion.div>
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 50 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="flex justify-center lg:justify-end"
          >
            <div className={`phone-frame ${isDemoMode ? 'fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 scale-110' : ''}`}>
              <div className="phone-notch" />
              <PhoneScreen activeTab={activeTab} />
            </div>
          </motion.div>
        </div>
      </div>
      {isDemoMode && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/70 backdrop-blur-sm z-40"
          onClick={() => setIsDemoMode(false)}
        />
      )}
    </section>
  );
}

function Features() {
  return (
    <section className="relative z-10 py-24">
      <div className="max-w-7xl mx-auto px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <h2 className="text-4xl lg:text-5xl font-bold font-display mb-4">
            强大功能，<span className="text-cyan-400">专业体验</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            从基础参数调整到 AI 智能辅助，OMaster 为你提供全方位的摄影创作支持
          </p>
        </motion.div>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {FEATURES.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
            >
              <div className="feature-card p-6 rounded-3xl h-full">
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-blue-500/20 flex items-center justify-center text-cyan-400 mb-5">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold text-white mb-3">{feature.title}</h3>
                <p className="text-gray-400 leading-relaxed">{feature.description}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}

function PresetShowcase() {
  return (
    <section className="relative z-10 py-24 overflow-hidden">
      <div className="max-w-7xl mx-auto px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="mb-12"
        >
          <div className="flex items-end justify-between mb-2">
            <h2 className="text-4xl font-bold font-display">精选预设</h2>
            <button className="text-cyan-400 hover:text-cyan-300 flex items-center gap-2 font-medium">
              查看全部
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
          <p className="text-gray-400">来自全球摄影师的专业创作</p>
        </motion.div>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {PRESETS.map((preset, i) => (
            <motion.div
              key={preset.id}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
            >
              <PresetCard preset={preset} />
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}

function CTA() {
  return (
    <section className="relative z-10 py-24">
      <div className="max-w-4xl mx-auto px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-cyan-900/30 via-purple-900/20 to-orange-900/30 border border-white/10 p-12 text-center"
        >
          <h2 className="text-4xl lg:text-5xl font-bold font-display mb-6">
            准备好开始你的摄影之旅了吗？
          </h2>
          <p className="text-gray-300 text-lg mb-10 max-w-xl mx-auto">
            现在下载 OMaster，发现属于你的摄影风格，让每一张照片都成为佳作
          </p>
          <div className="flex flex-wrap justify-center gap-4">
            <button className="button-primary px-10 py-5 rounded-2xl text-deep-space font-bold text-lg flex items-center gap-3">
              <Star className="w-6 h-6" />
              立即下载
            </button>
            <button className="px-10 py-5 rounded-2xl border border-white/20 text-white font-semibold text-lg hover:bg-white/5 transition-colors">
              查看源代码
            </button>
          </div>
        </motion.div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="relative z-10 border-t border-white/5 py-12">
      <div className="max-w-7xl mx-auto px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center">
              <Camera className="w-5 h-5 text-white" />
            </div>
            <span className="font-bold text-xl font-display">OMaster</span>
          </div>
          <div className="flex items-center gap-8 text-gray-400 text-sm">
            <a href="#" className="hover:text-white transition-colors">GitHub</a>
            <a href="#" className="hover:text-white transition-colors">文档</a>
            <a href="#" className="hover:text-white transition-colors">隐私政策</a>
            <a href="#" className="hover:text-white transition-colors">联系我们</a>
          </div>
          <p className="text-gray-600 text-sm">© 2024 OMaster Team. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}

export default function App() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 50);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen relative">
      <div className="gradient-mesh" />
      <div className="grain-overlay" />
      <motion.header
        initial={{ y: -100 }}
        animate={{ y: 0 }}
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          scrolled ? 'bg-[#0F0F23]/80 backdrop-blur-xl border-b border-white/5' : 'bg-transparent'
        }`}
      >
        <div className="max-w-7xl mx-auto px-6 lg:px-8 h-20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center glow-box">
              <Camera className="w-6 h-6 text-white" />
            </div>
            <span className="text-xl font-bold font-display">OMaster</span>
          </div>
          <nav className="hidden md:flex items-center gap-8">
            {['功能', '预设', '社区', '文档'].map((item) => (
              <a key={item} href="#" className="nav-tab text-gray-300 hover:text-white transition-colors font-medium">
                {item}
              </a>
            ))}
          </nav>
          <button className="button-primary px-6 py-2.5 rounded-xl text-deep-space font-semibold">
            下载
          </button>
        </div>
      </motion.header>
      <main className="pt-20">
        <Hero />
        <Features />
        <PresetShowcase />
        <CTA />
      </main>
      <Footer />
    </div>
  );
}
