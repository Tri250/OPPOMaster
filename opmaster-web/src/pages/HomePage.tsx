import { motion, AnimatePresence } from 'framer-motion';
import { 
  Camera, Sparkles, Star, Download, Search, TrendingUp, Heart, X, Menu, 
  Palette, Layers, Upload, Zap, Image as ImageIcon, Settings, Photo, 
  Magic, ColorPalette, Edit3, Sliders 
} from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAppStore } from '../store/useAppStore';
import PresetCard from '../components/home/PresetCard';

// ColorOS 16 标准快捷入口配置
const quickActions = [
  { id: 'ai', icon: Magic, label: 'AI场景', path: '/ai-demo', color: 'from-oppo-orange to-hasselblad-orange', desc: '智能识别' },
  { id: 'preset', icon: Palette, label: '预设', path: '/filter-library', color: 'from-info to-neutral-600', desc: '滤镜库' },
  { id: 'watermark', icon: Edit3, label: '水印', path: '/watermark', color: 'from-success to-info', desc: '水印工具' },
  { id: 'editor', icon: Sliders, label: '编辑', path: '/preset-editor', color: 'from-warning to-error', desc: '参数调节' },
];

// 分类标签
const categories = [
  { id: 'all', label: '全部', icon: Sparkles },
  { id: 'hncs', label: '哈苏认证', icon: Star },
  { id: 'popular', label: '热门', icon: TrendingUp },
  { id: 'favorites', label: '收藏', icon: Heart },
];

// 核心功能入口
const featureCards = [
  { 
    id: 'presets', 
    title: '大师预设', 
    desc: '专业摄影师精心调校', 
    icon: Palette, 
    color: 'from-oppo-orange to-hasselblad-orange',
    path: '/filter-library'
  },
  { 
    id: 'ai', 
    title: 'AI智能识别', 
    desc: '50+场景自动检测', 
    icon: Sparkles, 
    color: 'from-info to-neutral-600',
    path: '/ai-demo'
  },
  { 
    id: 'watermark', 
    title: '专业水印', 
    desc: '品牌水印一键添加', 
    icon: Photo, 
    color: 'from-success to-info',
    path: '/watermark'
  },
  { 
    id: 'floating', 
    title: '悬浮窗', 
    desc: '相机实时调整', 
    icon: Layers, 
    color: 'from-warning to-error',
    path: '/floating-window'
  },
];

// 精选照片展示
const samplePhotos = [
  { id: 1, url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=400&fit=crop', label: '风景' },
  { id: 2, url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=400&fit=crop', label: '人像' },
  { id: 3, url: 'https://images.unsplash.com/photo-1518895949257-7621c3c78fc0?w=400&h=400&fit=crop', label: '夜景' },
  { id: 4, url: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=400&fit=crop', label: '美食' },
];

export default function HomePage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const navigate = useNavigate();
  const { getFilteredPresets } = useAppStore();
  
  const presets = getFilteredPresets().slice(0, 8);

  return (
    <div className="min-h-screen bg-oppo-black text-text-primary overflow-x-hidden">
      {/* ColorOS 16 风格背景光效 */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <motion.div
          animate={{ 
            x: [0, 50, 0], 
            y: [0, 30, 0],
          }}
          transition={{ 
            duration: 20, 
            repeat: Infinity, 
            ease: 'easeInOut' 
          }}
          className="absolute -top-40 -left-40 w-[400px] h-[400px] bg-oppo-orange/10 rounded-full blur-[120px]"
        />
        <motion.div
          animate={{ 
            x: [0, -50, 0], 
            y: [0, -30, 0],
          }}
          transition={{ 
            duration: 25, 
            repeat: Infinity, 
            ease: 'easeInOut' 
          }}
          className="absolute -bottom-40 -right-40 w-[350px] h-[350px] bg-hasselblad-orange/10 rounded-full blur-[100px]"
        />
      </div>

      {/* ColorOS 16 顶部导航 */}
      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation border-b border-white/5">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center gap-3 touch-feedback" aria-label="OPPO Master">
              <motion.div
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="w-9 h-9 rounded-lg bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1"
              >
                <Camera className="w-5 h-5 text-oppo-black" />
              </motion.div>
              <span className="text-h2 font-bold gradient-text-oppo hidden sm:block">OPPO Master</span>
            </Link>

            {/* Desktop Navigation */}
            <div className="hidden md:flex items-center gap-1">
              {[
                { name: '首页', path: '/' },
                { name: '预设', path: '/filter-library' },
                { name: 'AI识别', path: '/ai-demo' },
                { name: '水印', path: '/watermark' },
                { name: '设置', path: '/settings' },
              ].map((item) => (
                <Link
                  key={item.path}
                  to={item.path}
                  className="px-4 py-2 rounded-lg text-body2 font-medium text-text-secondary hover:text-text-primary hover:bg-white/5 transition-all duration-200 ease-out-cubic touch-feedback"
                >
                  {item.name}
                </Link>
              ))}
            </div>

            <div className="flex items-center gap-2">
              <button className="btn-primary text-xs px-4 py-2.5 hidden sm:flex items-center gap-2 touch-feedback" aria-label="下载应用">
                <Download className="w-4 h-4" />
                <span>下载</span>
              </button>
              
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="md:hidden p-2 min-h-[48px] min-w-[48px] flex items-center justify-center rounded-lg hover:bg-white/10 transition-colors duration-200 touch-feedback"
                aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
              >
                <AnimatePresence mode="wait">
                  <motion.div
                    key={mobileMenuOpen ? 'close' : 'menu'}
                    initial={{ scale: 0.5, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.5, opacity: 0 }}
                    transition={{ duration: 0.2 }}
                  >
                    {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
                  </motion.div>
                </AnimatePresence>
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* 移动端全屏菜单 */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.3, ease: 'easeOutCubic' }}
            className="fixed inset-0 z-40 bg-oppo-black/98 backdrop-blur-2xl md:hidden"
          >
            <div className="pt-20 px-6 pb-24">
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="space-y-1"
              >
                {[
                  { name: '首页', path: '/', icon: Camera },
                  { name: '预设库', path: '/filter-library', icon: Palette },
                  { name: 'AI场景识别', path: '/ai-demo', icon: Sparkles },
                  { name: '水印工具', path: '/watermark', icon: Edit3 },
                  { name: '悬浮窗', path: '/floating-window', icon: Layers },
                  { name: '预设编辑器', path: '/preset-editor', icon: Sliders },
                  { name: '设置', path: '/settings', icon: Settings },
                ].map((item, index) => (
                  <motion.button
                    key={item.path}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.15 + index * 0.05 }}
                    onClick={() => {
                      navigate(item.path);
                      setMobileMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-4 py-4 px-4 rounded-2xl hover:bg-white/5 transition-all duration-200 text-left"
                  >
                    <div className="w-10 h-10 rounded-lg bg-oppo-orange/10 flex items-center justify-center">
                      <item.icon className="w-5 h-5 text-oppo-orange" />
                    </div>
                    <span className="text-h3 font-medium">{item.name}</span>
                  </motion.button>
                ))}
              </motion.div>
              
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.5 }}
                className="mt-8"
              >
                <button className="btn-primary-large w-full" onClick={() => setMobileMenuOpen(false)}>
                  立即下载
                </button>
              </motion.div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* 主内容区 */}
      <main className="relative pt-14 pb-24 md:pb-12">
        {/* Hero 区域 */}
        <section className="px-4 sm:px-6 lg:px-8 pt-12 pb-6">
          <div className="max-w-7xl mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, ease: 'easeOutCubic' }}
              className="text-center mb-8"
            >
              {/* ColorOS 16 风格标签 */}
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: 'spring', stiffness: 200, damping: 15 }}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-full glass-effect mb-6"
              >
                <span className="relative flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-success opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-success"></span>
                </span>
                <span className="text-caption text-text-secondary font-medium">ColorOS 16 • 哈苏影像</span>
              </motion.div>

              {/* 主标题 */}
              <motion.h1
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3, duration: 0.5 }}
                className="text-3xl sm:text-4xl md:text-5xl font-bold mb-4 leading-tight"
              >
                <span className="gradient-text-oppo">OPPO Master</span>
              </motion.h1>
              
              {/* 副标题 */}
              <motion.p
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4, duration: 0.5 }}
                className="text-body1 text-text-secondary max-w-2xl mx-auto leading-relaxed"
              >
                专业摄影师精心调校，让每一次按下快门都充满惊喜
              </motion.p>
            </motion.div>

            {/* ColorOS 16 风格搜索框 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5, duration: 0.5 }}
              className="max-w-xl mx-auto mb-10"
            >
              <div className="relative group">
                <div className="absolute inset-0 rounded-2xl bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 opacity-0 group-hover:opacity-100 transition-opacity duration-300 blur-lg" />
                <div className="relative">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="搜索预设、标签或相机型号..."
                    className="w-full pl-12 pr-12 py-3.5 bg-white/5 border border-white/10 rounded-2xl text-body1 placeholder:text-text-tertiary focus:outline-none focus:border-oppo-orange/50 focus:bg-white/7 transition-all duration-200 ease-out-cubic"
                  />
                  {searchQuery && (
                    <button
                      onClick={() => setSearchQuery('')}
                      className="absolute right-4 top-1/2 -translate-y-1/2 text-text-tertiary hover:text-text-primary transition-colors p-1 rounded-full hover:bg-white/10"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  )}
                </div>
              </div>
            </motion.div>

            {/* 精选照片展示 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6, duration: 0.5 }}
              className="grid grid-cols-4 gap-2 sm:gap-3 max-w-2xl mx-auto mb-10"
            >
              {samplePhotos.map((photo, index) => (
                <motion.div
                  key={photo.id}
                  initial={{ opacity: 0, scale: 0.8, y: 20 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  transition={{ delay: 0.7 + index * 0.08, duration: 0.4, ease: 'easeOutCubic' }}
                  whileHover={{ scale: 1.05, y: -2 }}
                  className="aspect-square rounded-2xl overflow-hidden relative group cursor-pointer"
                >
                  <img
                    src={photo.url}
                    alt={photo.label}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                  <div className="absolute bottom-2 left-2 right-2 text-center opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                    <span className="text-caption font-medium text-white">{photo.label}</span>
                  </div>
                </motion.div>
              ))}
            </motion.div>

            {/* 快捷入口 - ColorOS 16 玻璃卡片风格 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.8, duration: 0.5 }}
              className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 mb-12"
            >
              {quickActions.map((action, index) => (
                <motion.button
                  key={action.id}
                  initial={{ opacity: 0, scale: 0.8, y: 20 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  transition={{ delay: 0.9 + index * 0.08, duration: 0.4, ease: 'easeOutCubic' }}
                  whileHover={{ scale: 1.03, y: -4 }}
                  whileTap={{ scale: 0.97 }}
                  onClick={() => navigate(action.path)}
                  className="glass-effect rounded-2xl p-4 sm:p-5 flex flex-col items-center gap-3 hover:bg-white/10 transition-all duration-200 ease-out-cubic"
                  aria-label={action.label}
                >
                  <motion.div
                    whileHover={{ rotate: 5 }}
                    className={`w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-gradient-to-br ${action.color} flex items-center justify-center shadow-oppo-elevation-1`}
                  >
                    <action.icon className="w-6 h-6 sm:w-7 sm:h-7 text-oppo-black" />
                  </motion.div>
                  <div className="text-center">
                    <span className="text-body2 font-semibold block">{action.label}</span>
                    <span className="text-caption text-text-tertiary block">{action.desc}</span>
                  </div>
                </motion.button>
              ))}
            </motion.div>
          </div>
        </section>

        {/* 精选预设区域 */}
        <section className="px-4 sm:px-6 lg:px-8 py-6">
          <div className="max-w-7xl mx-auto">
            {/* 分类标签栏 */}
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="flex items-center justify-between mb-6"
            >
              <div>
                <h2 className="text-h2 font-bold mb-1">精选预设</h2>
                <p className="text-caption text-text-secondary">由专业摄影师精心调校</p>
              </div>
              <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-hide -mr-4 pr-4">
                {categories.map((category) => (
                  <button
                    key={category.id}
                    onClick={() => setActiveCategory(category.id)}
                    className={`flex items-center gap-1.5 px-4 py-2 rounded-full text-body2 font-medium whitespace-nowrap transition-all duration-200 ease-out-cubic ${
                      activeCategory === category.id
                        ? 'bg-oppo-orange text-oppo-black shadow-oppo-elevation-1'
                        : 'bg-white/5 text-text-secondary hover:text-text-primary hover:bg-white/10'
                    }`}
                  >
                    <category.icon className="w-4 h-4" />
                    {category.label}
                  </button>
                ))}
              </div>
            </motion.div>

            {/* 预设卡片网格 */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
              {presets.map((preset, index) => (
                <motion.div
                  key={preset.id}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.05, duration: 0.4 }}
                >
                  <PresetCard preset={preset} index={index} />
                </motion.div>
              ))}
            </div>

            {/* 查看更多 */}
            <motion.div
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
              transition={{ delay: 0.5 }}
              className="text-center mt-8"
            >
              <Link 
                to="/filter-library" 
                className="inline-flex items-center gap-2 text-oppo-orange text-body2 font-semibold hover:gap-3 transition-all duration-200 group"
              >
                查看全部预设
                <span className="w-7 h-7 rounded-full bg-oppo-orange/10 flex items-center justify-center group-hover:bg-oppo-orange/20 transition-colors">
                  <Zap className="w-4 h-4" />
                </span>
              </Link>
            </motion.div>
          </div>
        </section>

        {/* 核心功能展示区 */}
        <section className="px-4 sm:px-6 lg:px-8 py-12">
          <div className="max-w-7xl mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="text-center mb-10"
            >
              <h2 className="text-h1 font-bold mb-3">核心功能</h2>
              <p className="text-body1 text-text-secondary max-w-2xl mx-auto">全方位提升您的摄影体验</p>
            </motion.div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {featureCards.map((card, index) => (
                <motion.div
                  key={card.id}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.1, duration: 0.4 }}
                  whileHover={{ y: -4 }}
                  onClick={() => navigate(card.path)}
                  className="glass-effect rounded-3xl p-6 sm:p-8 cursor-pointer hover:bg-white/10 transition-all duration-200 ease-out-cubic"
                >
                  <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${card.color} flex items-center justify-center shadow-oppo-elevation-1 mb-4`}>
                    <card.icon className="w-7 h-7 text-oppo-black" />
                  </div>
                  <h3 className="text-h3 font-bold mb-2">{card.title}</h3>
                  <p className="text-body2 text-text-secondary">{card.desc}</p>
                </motion.div>
              ))}
            </div>
          </div>
        </section>

        {/* AI功能展示区 */}
        <section className="px-4 sm:px-6 lg:px-8 py-12">
          <div className="max-w-7xl mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="relative overflow-hidden rounded-3xl glass-effect p-8 sm:p-12"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-oppo-orange/10 to-hasselblad-orange/5" />
              
              <div className="relative z-10 flex flex-col lg:flex-row items-center gap-8">
                <div className="flex-1 text-center lg:text-left">
                  <motion.div
                    initial={{ scale: 0 }}
                    whileInView={{ scale: 1 }}
                    viewport={{ once: true }}
                    transition={{ type: 'spring', stiffness: 200, damping: 15 }}
                    className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange mb-6 shadow-oppo-elevation-2"
                  >
                    <Sparkles className="w-8 h-8 text-oppo-black" />
                  </motion.div>
                  
                  <h2 className="text-2xl sm:text-3xl font-bold mb-4">AI智能场景识别</h2>
                  <p className="text-body1 text-text-secondary mb-6 max-w-md mx-auto lg:mx-0">
                    上传照片，AI自动识别场景类型，智能推荐最匹配的预设参数
                  </p>
                  
                  <div className="flex flex-col sm:flex-row items-center gap-4 justify-center lg:justify-start">
                    <Link to="/ai-demo" className="btn-primary text-body2 px-6 py-3 flex items-center gap-2">
                      立即体验
                      <Zap className="w-4 h-4" />
                    </Link>
                  </div>
                </div>
                
                <div className="flex gap-6 lg:gap-10">
                  {[
                    { label: '50+', desc: '场景类型' },
                    { label: '毫秒', desc: '极速响应' },
                    { label: '百万', desc: '训练样本' },
                  ].map((stat, index) => (
                    <motion.div
                      key={stat.label}
                      initial={{ opacity: 0, y: 20 }}
                      whileInView={{ opacity: 1, y: 0 }}
                      viewport={{ once: true }}
                      transition={{ delay: 0.2 + index * 0.1 }}
                      className="text-center"
                    >
                      <p className="text-h1 font-bold text-oppo-orange">{stat.label}</p>
                      <p className="text-caption text-text-tertiary">{stat.desc}</p>
                    </motion.div>
                  ))}
                </div>
              </div>
            </motion.div>
          </div>
        </section>

        {/* 底部下载引导 */}
        <section className="px-4 sm:px-6 lg:px-8 py-16">
          <div className="max-w-xl mx-auto text-center">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
            >
              <h2 className="text-h1 font-bold mb-4">准备好开始了吗？</h2>
              <p className="text-body1 text-text-secondary mb-8">
                立即下载 OPPO Master，让您的哈苏影像系统发挥全部潜能
              </p>
              <button className="btn-primary-large inline-flex items-center gap-2">
                <Download className="w-5 h-5" />
                <span>免费下载</span>
              </button>
            </motion.div>
          </div>
        </section>
      </main>

      {/* ColorOS 16 底部导航 - 移动端 */}
      <nav className="fixed bottom-0 left-0 right-0 z-50 md:hidden h-16 glass-navigation border-t border-white/5 safe-area-bottom">
        <div className="flex items-center justify-around h-full max-w-md mx-auto">
          {[
            { icon: Camera, label: '首页', path: '/', active: true },
            { icon: Sparkles, label: '滤镜', path: '/filter-library', active: false },
            { icon: Edit3, label: '水印', path: '/watermark', active: false },
            { icon: Heart, label: '我的', path: '/settings', active: false },
          ].map((item, index) => (
            <Link
              key={item.path}
              to={item.path}
              className="flex flex-col items-center gap-1 px-4 py-2"
            >
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-200 ${
                item.active 
                  ? 'bg-oppo-orange/20 text-oppo-orange' 
                  : 'text-text-tertiary hover:text-text-secondary hover:bg-white/5'
              }`}>
                <item.icon className={`w-5 h-5 ${item.active ? 'text-oppo-orange' : 'text-text-tertiary'}`} />
              </div>
              <span className={`text-caption font-medium ${
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
