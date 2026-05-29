import { motion, AnimatePresence } from 'framer-motion';
import { 
  Camera, Sparkles, Star, Download, Search, TrendingUp, Heart, X, Menu, 
  Palette, Layers, Zap, Settings, Edit3, Sliders, Share2, Award, ChevronRight,
  CheckCircle, CloudSync, Globe, Users
} from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAppStore } from '../store/useAppStore';
import PresetCard from '../components/home/PresetCard';

const easeOppoEnter = [0.05, 0.7, 0.1, 1.0] as [number, number, number, number];
const easeOppoBounce = [0.175, 0.885, 0.32, 1.275] as [number, number, number, number];

// ============================================
// ColorOS 16 快捷入口配置 - 专家级设计
// ============================================
const quickActions = [
  { 
    id: 'ai', 
    icon: Sparkles, 
    label: 'AI场景', 
    path: '/ai-demo', 
    color: 'from-oppo-orange to-hasselblad-orange',
    desc: '智能识别'
  },
  { 
    id: 'preset', 
    icon: Palette, 
    label: '预设库', 
    path: '/filter-library', 
    color: 'from-oppo-blue to-oppo-purple',
    desc: '大师参数'
  },
  { 
    id: 'community', 
    icon: Users, 
    label: '社区', 
    path: '/community', 
    color: 'from-oppo-green to-oppo-blue',
    desc: '分享交流'
  },
  { 
    id: 'sync', 
    icon: CloudSync, 
    label: '云同步', 
    path: '/cloud-sync', 
    color: 'from-oppo-purple to-oppo-pink',
    desc: '数据备份'
  }
];

// ============================================
// 分类标签 - ColorOS 16 风格
// ============================================
const categories = [
  { id: 'all', label: '全部', icon: Sparkles },
  { id: 'hncs', label: '哈苏认证', icon: Star },
  { id: 'popular', label: '热门', icon: TrendingUp },
  { id: 'favorites', label: '收藏', icon: Heart }
];

// ============================================
// 核心功能展示卡片 - 专家级设计
// ============================================
const featureCards = [
  { 
    id: 'presets', 
    title: '大师预设', 
    desc: '专业摄影师精心调校，还原哈苏影像质感', 
    icon: Palette, 
    color: 'from-oppo-orange to-hasselblad-orange',
    path: '/filter-library'
  },
  { 
    id: 'ai', 
    title: 'AI场景识别', 
    desc: '666种场景智能识别，一键匹配最佳参数', 
    icon: Sparkles, 
    color: 'from-oppo-blue to-oppo-purple',
    path: '/ai-demo'
  },
  { 
    id: 'floating', 
    title: '悬浮窗助手', 
    desc: '相机实时调整，专业参数即时可见', 
    icon: Layers, 
    color: 'from-oppo-green to-oppo-blue',
    path: '/floating-window'
  },
  { 
    id: 'subscription', 
    title: '订阅管理', 
    desc: '多源订阅，预设自动更新', 
    icon: Globe, 
    color: 'from-oppo-purple to-oppo-pink',
    path: '/subscription'
  }
];

// ============================================
// 精选照片展示 - 旗舰级设计
// ============================================
const samplePhotos = [
  { id: 1, url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&h=600&fit=crop', label: '风景' },
  { id: 2, url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&h=600&fit=crop', label: '人像' },
  { id: 3, url: 'https://images.unsplash.com/photo-1518895949257-7621c3c78fc0?w=600&h=600&fit=crop', label: '夜景' },
  { id: 4, url: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600&h=600&fit=crop', label: '美食' }
];

export default function HomePage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const navigate = useNavigate();
  const { getFilteredPresets } = useAppStore();
  
  const presets = getFilteredPresets().slice(0, 8);

  return (
    <div className="min-h-screen bg-bg-primary text-text-primary overflow-x-hidden relative">
      {/* ============================================
           ColorOS 16 背景光效 - 旗舰级
           ============================================ */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <motion.div
          animate={{ 
            x: [0, 80, 0], 
            y: [0, 40, 0],
          }}
          transition={{ 
            duration: 25, 
            repeat: Infinity, 
            ease: 'easeInOut' 
          }}
          className="absolute -top-52 -left-52 w-[500px] h-[500px] orb-oppo orb-orange"
        />
        <motion.div
          animate={{ 
            x: [0, -60, 0], 
            y: [0, -50, 0],
          }}
          transition={{ 
            duration: 30, 
            repeat: Infinity, 
            ease: 'easeInOut' 
          }}
          className="absolute -bottom-52 -right-52 w-[450px] h-[450px] orb-oppo orb-blue"
        />
      </div>

      {/* ============================================
           ColorOS 16 顶部导航
           ============================================ */}
      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link 
              to="/" 
              className="flex items-center gap-3 touch-feedback"
              aria-label="小O帮帮"
            >
              <motion.div
                whileHover={{ scale: 1.08, rotate: 5 }}
                whileTap={{ scale: 0.95 }}
                className="w-10 h-10 rounded-2xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-2"
              >
                <Camera className="w-5.5 h-5.5 text-oppo-black" />
              </motion.div>
              <span className="text-h2 font-bold gradient-text-oppo hidden sm:block">小O帮帮</span>
            </Link>

            {/* Desktop Navigation */}
            <div className="hidden md:flex items-center gap-1">
              {[
                { name: '首页', path: '/' },
                { name: '预设库', path: '/filter-library' },
                { name: '社区', path: '/community' },
                { name: '设置', path: '/settings' }
              ].map((item) => (
                <Link
                  key={item.path}
                  to={item.path}
                  className="px-4 py-2 rounded-xl text-body2 font-medium text-text-secondary hover:text-text-primary hover:bg-white/8 transition-all duration-200 ease-out-elastic"
                >
                  {item.name}
                </Link>
              ))}
            </div>

            <div className="flex items-center gap-2">
              <button 
                className="btn-primary text-sm px-5 py-2.5 hidden sm:flex items-center gap-2"
                onClick={() => alert('小O帮帮 APP 下载页面即将上线！\n\n我们正在努力开发中，敬请期待！')}
              >
                <Download className="w-4 h-4" />
                <span>下载 APP</span>
              </button>
              
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="md:hidden p-2.5 min-h-[48px] min-w-[48px] flex items-center justify-center rounded-2xl hover:bg-white/10 transition-colors duration-200"
                aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
              >
                <AnimatePresence mode="wait">
                  <motion.div
                    key={mobileMenuOpen ? 'close' : 'menu'}
                    initial={{ scale: 0.6, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.6, opacity: 0 }}
                    transition={{ duration: 0.2, ease: easeOppoBounce }}
                  >
                    {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
                  </motion.div>
                </AnimatePresence>
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* ============================================
           移动端全屏菜单 - ColorOS 16风格
           ============================================ */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0, scale: 0.96, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: -20 }}
            transition={{ duration: 0.3, ease: easeOppoEnter }}
            className="fixed inset-0 z-40 bg-bg-primary/98 backdrop-blur-2xl md:hidden"
          >
            <div className="pt-20 px-6 pb-24">
              <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="space-y-2"
              >
                {[
                  { name: '首页', path: '/', icon: Camera },
                  { name: '预设库', path: '/filter-library', icon: Palette },
                  { name: 'AI场景识别', path: '/ai-demo', icon: Sparkles },
                  { name: '社区', path: '/community', icon: Users },
                  { name: '云同步', path: '/cloud-sync', icon: CloudSync },
                  { name: '订阅管理', path: '/subscription', icon: Globe },
                  { name: '设置', path: '/settings', icon: Settings },
                ].map((item, index) => (
                  <motion.button
                    key={item.path}
                    initial={{ opacity: 0, x: -30 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.15 + index * 0.06 }}
                    onClick={() => {
                      navigate(item.path);
                      setMobileMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-4 py-3.5 px-4 rounded-2xl hover:bg-white/8 transition-all duration-200 text-left"
                  >
                    <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-oppo-orange/20 to-hasselblad-orange/20 flex items-center justify-center">
                      <item.icon className="w-5.5 h-5.5 text-oppo-orange" />
                    </div>
                    <span className="text-h3 font-semibold">{item.name}</span>
                  </motion.button>
                ))}
              </motion.div>
              
              <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.6 }}
                className="mt-8"
              >
                <button 
                  className="btn-primary-large w-full flex items-center justify-center gap-2"
                  onClick={() => { 
                    setMobileMenuOpen(false); 
                    alert('小O帮帮 APP 下载页面即将上线！');
                  }}
                >
                  <Download className="w-5 h-5" />
                  立即下载 APP
                </button>
              </motion.div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ============================================
           主内容区
           ============================================ */}
      <main className="relative pt-14 pb-24 md:pb-12 z-10">
        {/* ============================================
             Hero 区域 - 专家级设计
             ============================================ */}
        <section className="section-padding pt-16">
          <div className="max-w-7xl mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.7, ease: easeOppoEnter }}
              className="text-center mb-12"
            >
              {/* ColorOS 16 风格标签 */}
              <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ delay: 0.2, type: 'spring', stiffness: 200, damping: 15 }}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-full glass-effect-light mb-6"
              >
                <span className="relative flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-oppo-green opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-oppo-green"></span>
                </span>
                <span className="text-caption text-text-secondary font-semibold">ColorOS 16 • 哈苏影像系统</span>
              </motion.div>

              {/* 主标题 */}
              <motion.h1
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3, duration: 0.6 }}
                className="text-3xl sm:text-4xl md:text-5xl font-bold mb-4 leading-tight"
              >
                <span className="gradient-text-oppo">小O帮帮</span>
                <br />
                <span className="text-text-primary">让每一张照片都成为大片</span>
              </motion.h1>
              
              {/* 副标题 */}
              <motion.p
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.45, duration: 0.6 }}
                className="text-body1 text-text-secondary max-w-2xl mx-auto leading-relaxed mb-8"
              >
                专业摄影师精心调校，AI智能场景识别，哈苏影像质感
                <br />
                让你的OPPO手机发挥全部摄影潜力
              </motion.p>

              {/* CTA按钮 */}
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.6, duration: 0.5 }}
                className="flex flex-col sm:flex-row items-center gap-4 justify-center"
              >
                <button 
                  className="btn-primary-large flex items-center gap-2"
                  onClick={() => navigate('/filter-library')}
                >
                  <Palette className="w-5 h-5" />
                  探索预设库
                </button>
                <button 
                  className="btn-secondary flex items-center gap-2"
                  onClick={() => navigate('/ai-demo')}
                >
                  <Sparkles className="w-5 h-5" />
                  体验AI识别
                </button>
              </motion.div>
            </motion.div>

            {/* ============================================
                 精选照片展示 - 旗舰级设计
                 ============================================ */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.75, duration: 0.6 }}
              className="grid grid-cols-4 gap-2.5 sm:gap-3.5 max-w-2xl mx-auto mb-14"
            >
              {samplePhotos.map((photo, index) => (
                <motion.div
                  key={photo.id}
                  initial={{ opacity: 0, scale: 0.8, y: 25 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  transition={{ delay: 0.85 + index * 0.1, duration: 0.5, ease: easeOppoEnter }}
                  whileHover={{ scale: 1.06, y: -4 }}
                  className="aspect-square rounded-2xl overflow-hidden relative group cursor-pointer shadow-oppo-elevation-1"
                >
                  <img
                    src={photo.url}
                    alt={photo.label}
                    className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-115"
                  />
                  <div className="absolute inset-0 gradient-overlay-subtle opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                  <div className="absolute bottom-2.5 left-2.5 right-2.5 text-center opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                    <span className="text-caption font-bold text-white bg-black/40 px-2 py-1 rounded-full backdrop-blur-sm">
                      {photo.label}
                    </span>
                  </div>
                </motion.div>
              ))}
            </motion.div>

            {/* ============================================
                 快捷入口 - ColorOS 16 玻璃卡片风格
                 ============================================ */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 1.0, duration: 0.6 }}
              className="grid grid-cols-2 sm:grid-cols-4 gap-3.5 sm:gap-4 mb-16"
            >
              {quickActions.map((action, index) => (
                <motion.button
                  key={action.id}
                  initial={{ opacity: 0, scale: 0.8, y: 25 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  transition={{ delay: 1.1 + index * 0.1, duration: 0.5, ease: easeOppoEnter }}
                  whileHover={{ scale: 1.05, y: -5 }}
                  whileTap={{ scale: 0.97 }}
                  onClick={() => navigate(action.path)}
                  className="card-glass p-4.5 sm:p-5 flex flex-col items-center gap-3.5 hover:bg-white/10 transition-all duration-300 ease-out-elastic"
                  aria-label={action.label}
                >
                  <motion.div
                    whileHover={{ rotate: 8, scale: 1.1 }}
                    className={`w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-gradient-to-br ${action.color} flex items-center justify-center shadow-oppo-elevation-2`}
                  >
                    <action.icon className="w-6 h-6 sm:w-7 sm:h-7 text-oppo-black" />
                  </motion.div>
                  <div className="text-center">
                    <span className="text-body2 font-bold block">{action.label}</span>
                    <span className="text-caption text-text-tertiary block mt-0.5">{action.desc}</span>
                  </div>
                </motion.button>
              ))}
            </motion.div>
          </div>
        </section>

        {/* ============================================
             精选预设区域 - 专家级设计
             ============================================ */}
        <section className="section-padding">
          <div className="max-w-7xl mx-auto">
            {/* 区域标题 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="flex items-end justify-between mb-8"
            >
              <div>
                <h2 className="text-h1 font-bold mb-1.5 flex items-center gap-2">
                  <Star className="w-6 h-6 text-hasselblad-orange fill-hasselblad-orange" />
                  精选预设
                </h2>
                <p className="text-body2 text-text-secondary">专业摄影师精心调校，还原真实色彩</p>
              </div>
              <Link 
                to="/filter-library" 
                className="hidden sm:flex items-center gap-2 text-oppo-orange text-body2 font-semibold hover:gap-3 transition-all duration-200 group"
              >
                查看全部
                <ChevronRight className="w-4.5 h-4.5 transition-transform duration-200 group-hover:translate-x-1" />
              </Link>
            </motion.div>

            {/* 分类标签栏 */}
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 }}
              className="flex items-center gap-2 overflow-x-auto pb-4 scrollbar-hide -mx-4 px-4 sm:mx-0 sm:px-0 mb-6"
            >
              {categories.map((category) => (
                <button
                  key={category.id}
                  onClick={() => setActiveCategory(category.id)}
                  className={`flex items-center gap-1.5 px-4 py-2.5 rounded-full text-body2 font-medium whitespace-nowrap transition-all duration-300 ease-out-elastic ${
                    activeCategory === category.id
                      ? 'bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-oppo-black shadow-oppo-elevation-2'
                      : 'bg-bg-secondary text-text-secondary hover:text-text-primary hover:bg-bg-tertiary'
                  }`}
                >
                  <category.icon className="w-4.5 h-4.5" />
                  {category.label}
                </button>
              ))}
            </motion.div>

            {/* 预设卡片网格 */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5 sm:gap-4">
              {presets.map((preset, index) => (
                <motion.div
                  key={preset.id}
                  initial={{ opacity: 0, y: 25 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.06, duration: 0.5, ease: easeOppoEnter }}
                >
                  <PresetCard preset={preset} index={index} />
                </motion.div>
              ))}
            </div>

            {/* 查看更多 - 移动端 */}
            <motion.div
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
              transition={{ delay: 0.4 }}
              className="text-center mt-8 sm:hidden"
            >
              <Link 
                to="/filter-library" 
                className="btn-secondary inline-flex items-center gap-2"
              >
                查看全部预设
                <ChevronRight className="w-4.5 h-4.5" />
              </Link>
            </motion.div>
          </div>
        </section>

        {/* ============================================
             核心功能展示区 - 专家级设计
             ============================================ */}
        <section className="section-padding">
          <div className="max-w-7xl mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="text-center mb-10"
            >
              <h2 className="text-display font-bold mb-3">核心功能</h2>
              <p className="text-body1 text-text-secondary max-w-2xl mx-auto">全方位提升你的摄影体验，专业级功能轻松上手</p>
            </motion.div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 md:gap-5">
              {featureCards.map((card, index) => (
                <motion.div
                  key={card.id}
                  initial={{ opacity: 0, y: 30 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.12, duration: 0.5 }}
                  whileHover={{ y: -6, scale: 1.02 }}
                  onClick={() => navigate(card.path)}
                  className="card-elevated p-6 sm:p-7 cursor-pointer hover:border-oppo-orange/20 transition-all duration-300 ease-out-elastic"
                >
                  <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${card.color} flex items-center justify-center shadow-oppo-elevation-2 mb-5`}>
                    <card.icon className="w-7 h-7 text-oppo-black" />
                  </div>
                  <h3 className="text-h2 font-bold mb-2">{card.title}</h3>
                  <p className="text-body2 text-text-secondary mb-4">{card.desc}</p>
                  <div className="flex items-center gap-1.5 text-oppo-orange text-body2 font-semibold">
                    了解更多
                    <ChevronRight className="w-4.5 h-4.5" />
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        </section>

        {/* ============================================
             AI功能展示区 - 旗舰级设计
             ============================================ */}
        <section className="section-padding">
          <div className="max-w-7xl mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="relative overflow-hidden rounded-3xl card-glass p-7 sm:p-9"
            >
              {/* 背景渐变 */}
              <div className="absolute inset-0 bg-gradient-to-br from-oppo-orange/15 via-transparent to-oppo-blue/10" />
              
              <div className="relative z-10 flex flex-col lg:flex-row items-center gap-8 lg:gap-12">
                <div className="flex-1 text-center lg:text-left">
                  <motion.div
                    initial={{ scale: 0, opacity: 0 }}
                    whileInView={{ scale: 1, opacity: 1 }}
                    viewport={{ once: true }}
                    transition={{ type: 'spring', stiffness: 200, damping: 15 }}
                    className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange mb-6 shadow-oppo-elevation-3"
                  >
                    <Sparkles className="w-8 h-8 text-oppo-black" />
                  </motion.div>
                  
                  <h2 className="text-h1 sm:text-display font-bold mb-4">
                    AI场景识别 · 666种
                  </h2>
                  <p className="text-body1 text-text-secondary mb-7 max-w-md mx-auto lg:mx-0">
                    上传照片，AI自动识别场景类型，智能匹配最佳预设参数，一秒还原哈苏色彩
                  </p>
                  
                  <div className="flex flex-col sm:flex-row items-center gap-4 justify-center lg:justify-start">
                    <Link to="/ai-demo" className="btn-primary flex items-center gap-2">
                      立即体验
                      <Zap className="w-4.5 h-4.5" />
                    </Link>
                    <div className="flex items-center gap-6 text-left">
                      <div>
                        <p className="text-number-lg font-bold gradient-text-oppo">666</p>
                        <p className="text-caption text-text-tertiary">场景类型</p>
                      </div>
                      <div>
                        <p className="text-number-lg font-bold gradient-text-oppo">&lt;350ms</p>
                        <p className="text-caption text-text-tertiary">极速响应</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </section>

        {/* ============================================
             底部下载引导
             ============================================ */}
        <section className="section-padding">
          <div className="max-w-xl mx-auto text-center">
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
            >
              <h2 className="text-display font-bold mb-4">准备好开始了吗？</h2>
              <p className="text-body1 text-text-secondary mb-8">
                立即下载小O帮帮，让你的哈苏影像系统发挥全部潜能
              </p>
              <button 
                className="btn-primary-large inline-flex items-center gap-2"
                onClick={() => alert('小O帮帮 APP 下载页面即将上线！')}
              >
                <Download className="w-5 h-5" />
                <span>免费下载 APP</span>
              </button>
            </motion.div>
          </div>
        </section>
      </main>

      {/* ============================================
           ColorOS 16 底部导航 - 移动端
           ============================================ */}
      <nav className="bottom-nav-bar md:hidden">
        <div className="flex items-center justify-around h-full max-w-md mx-auto">
          {[
            { icon: Camera, label: '首页', path: '/', active: true },
            { icon: Palette, label: '预设', path: '/filter-library', active: false },
            { icon: Sparkles, label: 'AI', path: '/ai-demo', active: false },
            { icon: Settings, label: '我的', path: '/settings', active: false },
          ].map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className="flex flex-col items-center gap-1.5 px-4 py-2.5 touch-feedback-strong"
            >
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-300 ease-out-elastic ${
                item.active 
                  ? 'bg-gradient-to-br from-oppo-orange/20 to-hasselblad-orange/20 text-oppo-orange' 
                  : 'text-text-tertiary hover:text-text-secondary hover:bg-white/8'
              }`}>
                <item.icon className={`w-5.5 h-5.5 ${item.active ? 'text-oppo-orange' : 'text-text-tertiary'}`} />
              </div>
              <span className={`text-caption font-semibold ${
                item.active ? 'text-oppo-orange' : 'text-text-tertiary'
              }`}>
                {item.label}
              </span>
            </Link>
          ))}
        </div>
      </nav>

      {/* 隐藏滚动条 */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar {
          display: none;
        }
        .scrollbar-hide {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
      `}</style>
    </div>
  );
}
