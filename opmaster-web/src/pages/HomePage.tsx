import { motion, AnimatePresence } from 'framer-motion';
import { Camera, Sparkles, Star, Download, Search, TrendingUp, Heart, X, Menu } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAppStore } from '../store/useAppStore';
import PresetCard from '../components/home/PresetCard';

// 快捷入口配置
const quickActions = [
  { id: 'ai', icon: Sparkles, label: 'AI场景', path: '/ai-demo', color: 'from-oppo-orange to-hasselblad-orange' },
  { id: 'watermark', icon: Star, label: '水印', path: '/watermark', color: 'from-info to-neutral-600' },
  { id: 'preset', icon: Camera, label: '预设', path: '/filter-library', color: 'from-success to-info' },
  { id: 'editor', icon: Download, label: '编辑器', path: '/preset-editor', color: 'from-warning to-error' },
];

// 分类标签
const categories = [
  { id: 'all', label: '全部', icon: Sparkles },
  { id: 'hncs', label: '哈苏认证', icon: Star },
  { id: 'popular', label: '热门', icon: TrendingUp },
  { id: 'favorites', label: '收藏', icon: Heart },
];

export default function HomePage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const navigate = useNavigate();
  const { getFilteredPresets } = useAppStore();
  
  const presets = getFilteredPresets().slice(0, 6);

  return (
    <div className="min-h-screen bg-oppo-black text-text-primary">
      {/* 背景光效 */}
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute top-0 left-1/4 w-[400px] h-[400px] bg-oppo-orange/5 rounded-full blur-[120px]" />
        <div className="absolute bottom-0 right-1/4 w-[300px] h-[300px] bg-hasselblad-orange/5 rounded-full blur-[100px]" />
      </div>

      {/* 顶部导航 - 简洁透明 */}
      <nav className="fixed top-0 left-0 right-0 z-50 h-14 bg-oppo-black/60 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center gap-3" aria-label="OPPO Master">
              <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo hidden sm:block">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center gap-6">
              <Link to="/filter-library" className="text-body2 text-text-secondary hover:text-text-primary transition-colors">滤镜库</Link>
              <Link to="/master-params" className="text-body2 text-text-secondary hover:text-text-primary transition-colors">大师参数</Link>
              <Link to="/settings" className="text-body2 text-text-secondary hover:text-text-primary transition-colors">设置</Link>
            </div>

            <div className="flex items-center gap-3">
              <button className="btn-primary text-xs px-4 py-2.5 hidden sm:flex items-center gap-2" aria-label="下载应用">
                <Download className="w-4 h-4" />
                <span>下载</span>
              </button>
              
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="md:hidden p-2 min-h-[48px] min-w-[48px] flex items-center justify-center rounded-lg hover:bg-white/10 transition-colors"
                aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
              >
                {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* 移动端菜单 */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40 bg-oppo-black/95 backdrop-blur-xl md:hidden"
          >
            <div className="pt-20 px-6 pb-6">
              <div className="space-y-2">
                <Link to="/" onClick={() => setMobileMenuOpen(false)} className="flex items-center gap-3 py-3 text-h3">首页</Link>
                <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="flex items-center gap-3 py-3 text-body1 text-text-secondary">滤镜库</Link>
                <Link to="/master-params" onClick={() => setMobileMenuOpen(false)} className="flex items-center gap-3 py-3 text-body1 text-text-secondary">大师参数</Link>
                <Link to="/ai-demo" onClick={() => setMobileMenuOpen(false)} className="flex items-center gap-3 py-3 text-body1 text-text-secondary">AI场景识别</Link>
                <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="flex items-center gap-3 py-3 text-body1 text-text-secondary">水印工具</Link>
                <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="flex items-center gap-3 py-3 text-body1 text-text-secondary">设置</Link>
              </div>
              <button className="btn-primary-large w-full mt-6" onClick={() => setMobileMenuOpen(false)}>立即下载</button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <main className="relative pt-14">
        {/* Hero区域 - 简洁大气 */}
        <section className="px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
          <div className="max-w-7xl mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, ease: 'ease-out-cubic' }}
              className="text-center mb-10"
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: 'spring', stiffness: 100 }}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-full glass-effect mb-6"
              >
                <span className="w-2 h-2 bg-success rounded-full animate-pulse" />
                <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
              </motion.div>

              <h1 className="text-3xl sm:text-4xl md:text-5xl font-bold mb-4">
                <span className="gradient-text-oppo">哈苏影像</span>
              </h1>
              <p className="text-body1 text-text-secondary max-w-xl mx-auto">
                专业摄影师精心调校，让每一次按下快门都充满惊喜
              </p>
            </motion.div>

            {/* 搜索框 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3, duration: 0.5 }}
              className="max-w-xl mx-auto mb-10"
            >
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="搜索预设、标签或相机型号..."
                  className="w-full pl-12 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl text-body1 placeholder:text-text-tertiary focus:outline-none focus:border-oppo-orange/50 transition-colors"
                />
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery('')}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-text-tertiary hover:text-text-primary"
                  >
                    <X className="w-5 h-5" />
                  </button>
                )}
              </div>
            </motion.div>

            {/* 快捷入口 - 玻璃卡片风格 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4, duration: 0.5 }}
              className="grid grid-cols-4 gap-3 mb-12"
            >
              {quickActions.map((action, index) => (
                <motion.button
                  key={action.id}
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.5 + index * 0.1 }}
                  whileHover={{ scale: 1.05, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => navigate(action.path)}
                  className="flex flex-col items-center gap-2 p-4 rounded-2xl glass-effect hover:bg-white/10 transition-colors"
                  aria-label={action.label}
                >
                  <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${action.color} flex items-center justify-center shadow-oppo-elevation-1`}>
                    <action.icon className="w-6 h-6 text-oppo-black" />
                  </div>
                  <span className="text-caption font-medium">{action.label}</span>
                </motion.button>
              ))}
            </motion.div>

            {/* 分类标签 */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.6 }}
              className="flex items-center justify-between mb-6"
            >
              <h2 className="text-h3 font-semibold">精选预设</h2>
              <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-hide">
                {categories.map((category) => (
                  <button
                    key={category.id}
                    onClick={() => setActiveCategory(category.id)}
                    className={`flex items-center gap-1.5 px-4 py-2 rounded-full text-body2 font-medium whitespace-nowrap transition-all ${
                      activeCategory === category.id
                        ? 'bg-oppo-orange text-oppo-black'
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
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
              {presets.map((preset, index) => (
                <motion.div
                  key={preset.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.7 + index * 0.08 }}
                >
                  <PresetCard preset={preset} index={index} />
                </motion.div>
              ))}
            </div>

            {/* 查看更多 */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 1.2 }}
              className="text-center mt-10"
            >
              <Link to="/filter-library" className="inline-flex items-center gap-2 text-oppo-orange text-body2 font-medium hover:gap-3 transition-all">
                查看全部预设
                <span className="w-6 h-6 rounded-full bg-oppo-orange/10 flex items-center justify-center">
                  <Download className="w-3.5 h-3.5" />
                </span>
              </Link>
            </motion.div>
          </div>
        </section>

        {/* AI功能展示区 */}
        <section className="px-4 sm:px-6 lg:px-8 py-16">
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
                    className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange mb-6 shadow-oppo-elevation-2"
                  >
                    <Sparkles className="w-8 h-8 text-oppo-black" />
                  </motion.div>
                  
                  <h2 className="text-2xl sm:text-3xl font-bold mb-4">AI智能场景识别</h2>
                  <p className="text-body1 text-text-secondary mb-6">
                    上传照片，AI自动识别场景类型，智能推荐最匹配的预设参数
                  </p>
                  
                  <div className="flex flex-col sm:flex-row items-center gap-4 justify-center lg:justify-start">
                    <Link to="/ai-demo" className="btn-primary text-body2 px-6 py-3">
                      立即体验
                    </Link>
                  </div>
                </div>
                
                <div className="flex gap-4">
                  {[
                    { label: '50+场景', desc: '自动识别' },
                    { label: '毫秒级', desc: '响应速度' },
                    { label: '百万级', desc: '训练样本' },
                  ].map((stat, index) => (
                    <motion.div
                      key={stat.label}
                      initial={{ opacity: 0, y: 20 }}
                      whileInView={{ opacity: 1, y: 0 }}
                      viewport={{ once: true }}
                      transition={{ delay: index * 0.1 }}
                      className="text-center px-4"
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
        <section className="px-4 sm:px-6 lg:px-8 py-12">
          <div className="max-w-xl mx-auto text-center">
            <h2 className="text-h1 font-bold mb-4">准备好开始了吗？</h2>
            <p className="text-body1 text-text-secondary mb-8">
              立即下载 OPPO Master，让您的哈苏影像系统发挥全部潜能
            </p>
            <button className="btn-primary-large inline-flex items-center gap-2">
              <Download className="w-5 h-5" />
              <span>免费下载</span>
            </button>
          </div>
        </section>
      </main>

      {/* 底部导航 - 移动端 */}
      <nav className="fixed bottom-0 left-0 right-0 z-50 md:hidden h-16 bg-oppo-black/80 backdrop-blur-xl border-t border-white/5 safe-area-bottom">
        <div className="flex items-center justify-around h-full">
          <Link to="/" className="flex flex-col items-center gap-1 px-6">
            <div className="w-8 h-8 rounded-lg bg-oppo-orange/20 flex items-center justify-center">
              <Camera className="w-5 h-5 text-oppo-orange" />
            </div>
            <span className="text-caption text-oppo-orange">首页</span>
          </Link>
          <Link to="/filter-library" className="flex flex-col items-center gap-1 px-6">
            <Sparkles className="w-5 h-5 text-text-tertiary" />
            <span className="text-caption text-text-tertiary">滤镜</span>
          </Link>
          <Link to="/watermark" className="flex flex-col items-center gap-1 px-6">
            <Star className="w-5 h-5 text-text-tertiary" />
            <span className="text-caption text-text-tertiary">水印</span>
          </Link>
          <Link to="/settings" className="flex flex-col items-center gap-1 px-6">
            <Heart className="w-5 h-5 text-text-tertiary" />
            <span className="text-caption text-text-tertiary">我的</span>
          </Link>
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