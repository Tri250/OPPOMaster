import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Heart, Star, ChevronRight, Download, Eye, ScrollText, Filter, Cloud, ScanText, X, Menu, Clock, Check } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ColorOSAnimations } from '../components/common/ColorOSComponents'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const p0Features = [
    {
      icon: Layers,
      title: '悬浮窗滤镜',
      description: 'ColorOS深度集成，实时预览效果',
      color: 'text-oppo-sunrise-gold',
      path: '/floating-window'
    },
    {
      icon: Filter,
      title: '滤镜库',
      description: '六大分类，专业预设',
      color: 'text-aurora-purple',
      path: '/filter-library'
    },
    {
      icon: ScrollText,
      title: '大师参数库',
      description: '哈苏认证，一键应用',
      color: 'text-ocean-blue',
      path: '/master-params'
    },
    {
      icon: Palette,
      title: '主题切换',
      description: '深色/浅色/自适应，完美匹配',
      color: 'text-sakura-pink',
      path: '/settings'
    }
  ]

  const allFeatures = [
    { icon: Layers, title: '悬浮窗滤镜', desc: 'ColorOS深度集成，实时预览', color: 'text-oppo-sunrise-gold', path: '/floating-window' },
    { icon: Filter, title: '滤镜分类', desc: '6大分类，快速找到', color: 'text-aurora-purple', path: '/filter-library' },
    { icon: Heart, title: '收藏管理', desc: '快速收藏，随时调用', color: 'text-sakura-pink', path: '/filter-library' },
    { icon: Clock, title: '最近使用', desc: '历史记录，随时回顾', color: 'text-ocean-blue', path: '/filter-library' },
    { icon: ScrollText, title: '大师参数', desc: 'HNCS认证，专业品质', color: 'text-hasselblad-pro', path: '/master-params' },
    { icon: Download, title: '参数导入', desc: '支持JSON和LUT文件', color: 'text-oppo-green', path: '/lut-manager' },
    { icon: Zap, title: '一键应用', desc: '毫秒级响应，立即生效', color: 'text-warning-vital', path: '/floating-window' },
    { icon: Palette, title: '主题切换', desc: '完美适配ColorOS', color: 'text-sakura-pink', path: '/settings' }
  ]

  const filterCategories = [
    { name: '全部', count: 48 },
    { name: '人像', count: 12 },
    { name: '风光', count: 15 },
    { name: '美食', count: 8 },
    { name: '夜景', count: 7 },
    { name: '胶片', count: 6 }
  ]

  const masterPresets = [
    { name: '城市夜景大师', author: '摄影阿东', rating: 4.9, isHNCS: true, isNew: true, category: '夜景' },
    { name: '人像柔光', author: '光影猎人', rating: 4.8, isHNCS: true, isNew: false, category: '人像' },
    { name: '风光HDR', author: '山水之间', rating: 4.7, isHNCS: false, isNew: true, category: '风光' },
    { name: '富士胶片', author: '色彩实验室', rating: 4.9, isHNCS: true, isNew: false, category: '胶片' }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-white overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 bg-deep-space/90 backdrop-blur-xl border-b border-white/5" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-oppo bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                <Camera className="w-5 h-5 text-deep-space" />
              </div>
              <span className="text-lg font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-sm font-medium text-oppo-sunrise-gold touch-feedback">首页</Link>
              <Link to="/xiao-o-help" className="text-sm font-medium text-text-secondary hover:text-white transition-colors duration-200 touch-feedback">小O帮帮</Link>
              <Link to="/filter-library" className="text-sm font-medium text-text-secondary hover:text-white transition-colors duration-200 touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-sm font-medium text-text-secondary hover:text-white transition-colors duration-200 touch-feedback">大师参数</Link>
              <Link to="/floating-window" className="text-sm font-medium text-text-secondary hover:text-white transition-colors duration-200 touch-feedback">悬浮窗</Link>
              <Link to="/settings" className="text-sm font-medium text-text-secondary hover:text-white transition-colors duration-200 touch-feedback">设置</Link>
            </div>

            <div className="hidden md:flex items-center space-x-4">
              <button className="btn-primary text-base px-6 py-3 flex items-center space-x-2 animate-pulse-glow touch-feedback" aria-label="下载应用">
                <Download className="w-5 h-5" />
                <span>立即下载</span>
              </button>
            </div>

            <button 
              className="md:hidden p-2 rounded-oppo hover:bg-white/10 transition-colors duration-200 min-h-[44px] min-w-[44px] flex items-center justify-center touch-feedback" 
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3 }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-oppo-sunrise-gold min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/xiao-o-help" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-white min-h-[48px] flex items-center touch-feedback">小O帮帮</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">滤镜库</Link>
            <Link to="/master-params" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">大师参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">AI场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/editor" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary w-full mt-4 min-h-[48px] touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <section className="relative pt-20 md:pt-24 pb-12 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-8 md:gap-12 items-center">
            <motion.div
              initial="hidden"
              animate="animate"
              variants={ColorOSAnimations.fadeIn}
              className="space-y-6"
            >
              <motion.div variants={ColorOSAnimations.fadeIn}>
                <div className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-sunrise-gold/20 to-hasselblad-pro/20 border border-oppo-sunrise-gold/30 rounded-full px-3 py-1.5 mb-4">
                  <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
                  <span className="text-xs text-text-secondary">ColorOS 16 专业摄影增强</span>
                </div>
              </motion.div>

              <motion.h1
                variants={ColorOSAnimations.fadeIn}
                className="text-4xl sm:text-5xl md:text-6xl font-bold leading-tight"
              >
                <span className="gradient-text-oppo">哈苏影像</span>
                <br />
                <span className="text-white">触手可及</span>
              </motion.h1>

              <motion.p
                variants={ColorOSAnimations.fadeIn}
                className="text-base md:text-lg text-text-secondary max-w-lg"
              >
                专为OPPO/一加设计的专业调色工具。
                系统级悬浮窗，实时预览滤镜效果，
                让每一次按下快门都充满惊喜。
              </motion.p>

              <motion.div
                variants={ColorOSAnimations.fadeIn}
                className="flex flex-col sm:flex-row gap-3"
              >
                <button className="btn-primary text-base px-6 py-3 flex items-center justify-center space-x-2 animate-pulse-glow touch-feedback" aria-label="免费下载应用">
                  <Download className="w-5 h-5" />
                  <span>免费下载</span>
                </button>
                <Link to="/filter-library" className="btn-secondary text-base px-6 py-3 flex items-center justify-center space-x-2 touch-feedback">
                  <span>体验滤镜库</span>
                  <ChevronRight className="w-5 h-5" />
                </Link>
              </motion.div>

              <motion.div
                variants={ColorOSAnimations.fadeIn}
                className="flex items-center space-x-6 pt-4"
              >
                <div className="flex -space-x-3">
                  {[1, 2, 3, 4].map((i) => (
                    <div key={i} className="w-10 h-10 rounded-full border-2 border-deep-space bg-gradient-to-br from-oppo-sunrise-gold/50 to-ocean-blue/50" />
                  ))}
                </div>
                <div className="text-sm text-text-secondary">
                  <span className="text-white font-semibold">50,000+</span> 用户正在使用
                </div>
              </motion.div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, scale: 0.8, rotate: -5 }}
              animate={{ opacity: 1, scale: 1, rotate: 0 }}
              transition={{ duration: 0.8, delay: 0.3 }}
              className="relative flex justify-center"
            >
              <div className="relative">
                <div className="w-72 md:w-80 h-[520px] md:h-[580px] bg-gradient-to-b from-gray-800 to-gray-900 rounded-[2.5rem] p-2 shadow-2xl shadow-oppo-sunrise-gold/10">
                  <div className="w-full h-full bg-card-surface rounded-[2rem] overflow-hidden relative">
                    <div className="absolute top-0 left-0 right-0 h-7 bg-deep-space flex items-center justify-between px-5 z-10">
                      <span className="text-xs text-text-tertiary">9:41</span>
                      <div className="flex items-center space-x-1">
                        <div className="w-4 h-2 bg-text-tertiary rounded-sm" />
                        <div className="w-3 h-3 bg-text-tertiary rounded-full" />
                      </div>
                    </div>

                    <div className="pt-7 h-full bg-gradient-to-b from-deep-space to-card-surface">
                      <div className="px-4 space-y-4">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-text-tertiary text-sm">哈苏影像大师</p>
                            <p className="text-base font-semibold text-white">OPPO Master</p>
                          </div>
                          <div className="flex space-x-2">
                            <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
                              <div className="w-5 h-5 rounded-full border-2 border-text-secondary" />
                            </div>
                          </div>
                        </div>

                        <div className="absolute top-24 right-4 z-20">
                          <div className="w-60 p-4 bg-black/85 backdrop-blur-xl border border-white/15 rounded-oppo shadow-oppo-card">
                            <div className="flex items-center justify-between mb-2">
                              <p className="text-white font-medium text-sm">富士胶片</p>
                              <div className="flex items-center gap-2">
                                <button className="w-6 h-6 rounded-full bg-white/5 flex items-center justify-center" aria-label="锁定">
                                  <div className="w-3 h-3 rounded-full border border-text-tertiary" />
                                </button>
                                <button className="w-6 h-6 rounded-full bg-white/5 flex items-center justify-center" aria-label="隐藏">
                                  <div className="w-3 h-3 rounded-full border border-text-tertiary" />
                                </button>
                              </div>
                            </div>
                            <div className="space-y-1">
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-text-tertiary">强度</span>
                                <span className="text-white font-medium">73%</span>
                              </div>
                              <div className="h-1.5 bg-white/20 rounded-full overflow-hidden">
                                <div className="h-full bg-gradient-to-r from-oppo-sunrise-gold to-oppo-sunrise-gold-light rounded-full" style={{ width: '73%' }} />
                              </div>
                            </div>
                          </div>
                        </div>

                        <div className="bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 rounded-2xl aspect-[4/3] mt-12 mb-4 flex items-center justify-center">
                          <Camera className="w-12 h-12 text-oppo-sunrise-gold/50" />
                        </div>

                        <div className="flex items-center justify-between">
                          <div className="flex gap-3">
                            {[1, 2, 3].map((i) => (
                              <div key={i} className="w-12 h-12 rounded-oppo bg-white/5" />
                            ))}
                          </div>
                          <div className="w-20 h-20 rounded-full border-4 border-white/30 flex items-center justify-center">
                            <div className="w-14 h-14 rounded-full bg-white/20" />
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="absolute -top-4 -right-4 w-24 h-24 bg-oppo-sunrise-gold/20 rounded-full blur-2xl" />
                <div className="absolute -bottom-8 -left-8 w-32 h-32 bg-ocean-blue/20 rounded-full blur-2xl" />
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      <section className="py-16 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">P0 核心体验</p>
            <h2 className="text-3xl md:text-4xl font-bold">MVP 必备功能</h2>
            <p className="text-text-secondary mt-4 max-w-2xl mx-auto">
              第1-3周优先实现，打造核心产品体验
            </p>
          </motion.div>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {p0Features.map((feature, i) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -4, scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="card-oppo group cursor-pointer touch-feedback"
              >
                <Link to={feature.path} className="block p-5">
                  <div className="w-12 h-12 rounded-oppo bg-white/5 flex items-center justify-center mb-4 group-hover:bg-oppo-sunrise-gold/10 transition-colors duration-200">
                    <feature.icon className={`w-6 h-6 ${feature.color}`} />
                  </div>
                  <h3 className="text-base font-semibold mb-2">{feature.title}</h3>
                  <p className="text-sm text-text-secondary">{feature.description}</p>
                </Link>
              </motion.div>
            ))}
          </div>
          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="mt-6 text-center"
          >
            <Link to="/p0-overview">
              <button className="btn-secondary flex items-center gap-2 mx-auto touch-feedback">
                <span>查看全部 P0 功能总览</span>
                <ChevronRight className="w-5 h-5" />
              </button>
            </Link>
          </motion.div>
        </div>
      </section>

      <section className="py-16 px-4 bg-gradient-to-b from-transparent via-card-surface/50 to-transparent safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">滤镜库</p>
            <h2 className="text-3xl md:text-4xl font-bold">专业调色预设</h2>
          </motion.div>

          <div className="flex flex-wrap justify-center gap-2 mb-8">
            {filterCategories.map((cat, i) => (
              <button
                key={cat.name}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 touch-feedback ${
                  i === 0
                    ? 'bg-oppo-sunrise-gold text-deep-space'
                    : 'bg-white/10 text-text-secondary hover:bg-white/20 hover:text-white'
                }`}
              >
                {cat.name} <span className="ml-1 opacity-60">({cat.count})</span>
              </button>
            ))}
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { name: '富士胶片', isNew: true, isHNCS: true, isFavorite: false },
              { name: '徕卡经典', isNew: false, isHNCS: true, isFavorite: true },
              { name: '哈苏自然', isNew: true, isHNCS: true, isFavorite: false },
              { name: '赛博朋克', isNew: false, isHNCS: false, isFavorite: true },
              { name: '人像暖色', isNew: true, isHNCS: false, isFavorite: false },
              { name: '风光HDR', isNew: false, isHNCS: true, isFavorite: true },
              { name: '夜景大师', isNew: true, isHNCS: true, isFavorite: false },
              { name: '美食鲜艳', isNew: false, isHNCS: false, isFavorite: true }
            ].map((filter, i) => (
              <motion.div
                key={filter.name}
                initial={{ opacity: 0, scale: 0.9 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.05 }}
                whileHover={{ y: -4, scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="relative overflow-hidden rounded-oppo cursor-pointer touch-feedback"
              >
                <div className="aspect-square bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex items-center justify-center relative">
                  <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
                  
                  <div className="absolute top-2 left-2 flex gap-1.5 z-10">
                    {filter.isNew && <span className="px-2 py-1 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>}
                    {filter.isHNCS && <span className="px-2 py-1 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">HNCS</span>}
                  </div>

                  {filter.isFavorite && (
                    <div className="absolute top-2 right-2 z-10">
                      <Heart className="w-4 h-4 fill-sakura-pink text-sakura-pink" />
                    </div>
                  )}

                  <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center">
                    <div className="w-5 h-5 rounded-full border-2 border-oppo-sunrise-gold/50" />
                  </div>
                </div>
                
                <div className="p-4 bg-card-surface">
                  <p className="text-white font-medium text-sm truncate">{filter.name}</p>
                </div>
              </motion.div>
            ))}
          </div>

          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-center mt-8"
          >
            <Link to="/filter-library" className="btn-secondary inline-flex items-center touch-feedback">
              查看全部滤镜
              <ChevronRight className="w-4 h-4 ml-2" />
            </Link>
          </motion.div>
        </div>
      </section>

      <section className="py-16 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">大师参数库</p>
            <h2 className="text-3xl md:text-4xl font-bold">专业摄影师作品</h2>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
            {masterPresets.map((preset, i) => (
              <motion.div
                key={preset.name}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -4 }}
                whileTap={{ scale: 0.98 }}
                className="card-oppo overflow-hidden group cursor-pointer touch-feedback"
              >
                <div className="aspect-[4/3] bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 relative">
                  <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
                  
                  <div className="absolute top-2 left-2 flex gap-1.5 z-10">
                    {preset.isNew && <span className="px-2 py-1 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>}
                    {preset.isHNCS && <span className="px-2 py-1 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">HNCS</span>}
                  </div>

                  <div className="absolute bottom-2 left-2 right-2 z-10">
                    <span className="px-2 py-1 bg-black/50 backdrop-blur-sm text-white text-xs rounded-full">
                      {preset.category}
                    </span>
                  </div>

                  <div className="absolute inset-0 gradient-overlay opacity-0 group-hover:opacity-100 transition-opacity duration-200" />
                </div>

                <div className="p-4">
                  <h3 className="text-base font-semibold mb-2">{preset.name}</h3>
                  <div className="flex items-center justify-between">
                    <span className="text-text-tertiary text-sm">@{preset.author}</span>
                    <div className="flex items-center space-x-1">
                      <Star className="w-4 h-4 fill-oppo-sunrise-gold text-oppo-sunrise-gold" />
                      <span className="text-sm text-white">{preset.rating}</span>
                    </div>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="mt-6 text-center"
          >
            <Link to="/master-params" className="btn-secondary inline-flex items-center touch-feedback">
              查看全部大师参数
              <ChevronRight className="w-4 h-4 ml-2" />
            </Link>
          </motion.div>
        </div>
      </section>

      <section className="py-16 px-4 bg-gradient-to-b from-transparent via-card-surface/50 to-transparent safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">完整功能</p>
            <h2 className="text-3xl md:text-4xl font-bold">全方位摄影增强</h2>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
            {allFeatures.map((feature, i) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.05 }}
                whileHover={{ y: -2 }}
                whileTap={{ scale: 0.98 }}
                className="card-oppo p-4 group cursor-pointer touch-feedback"
              >
                <Link to={feature.path} className="block">
                  <div className="w-10 h-10 rounded-oppo bg-white/5 flex items-center justify-center mb-4 group-hover:bg-oppo-sunrise-gold/10 transition-colors duration-200">
                    <feature.icon className={`w-5 h-5 ${feature.color}`} />
                  </div>
                  <h3 className="font-semibold mb-1">{feature.title}</h3>
                  <p className="text-text-tertiary text-sm">{feature.desc}</p>
                </Link>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      <section className="py-20 px-4 safe-area-bottom">
        <div className="max-w-4xl mx-auto">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="card-oppo p-6 md:p-12 text-center relative overflow-hidden"
          >
            <div className="absolute inset-0 bg-gradient-to-r from-oppo-sunrise-gold/10 to-ocean-blue/10" />
            <div className="relative z-10">
              <h2 className="text-3xl md:text-4xl font-bold mb-4">
                准备好开始了吗？
              </h2>
              <p className="text-lg text-text-secondary mb-8 max-w-2xl mx-auto">
                立即下载 OPPO Master，让你的哈苏影像系统发挥全部潜能
              </p>
              
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-8">
                <button className="btn-primary text-lg px-8 py-4 flex items-center justify-center space-x-2 animate-pulse-glow touch-feedback" aria-label="免费下载应用">
                  <Download className="w-6 h-6" />
                  <span>免费下载</span>
                </button>
              </div>

              <div className="flex flex-wrap items-center justify-center gap-6 text-sm text-text-secondary">
                <div className="flex items-center space-x-2">
                  <Check className="w-5 h-5 text-oppo-green" />
                  <span>完全免费</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Check className="w-5 h-5 text-oppo-green" />
                  <span>无需登录</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Check className="w-5 h-5 text-oppo-green" />
                  <span>无广告</span>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      <footer className="py-8 px-4 border-t border-white/5" role="contentinfo">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-6 mb-8">
            <div className="md:col-span-2">
              <div className="flex items-center space-x-3 mb-4">
                <div className="w-10 h-10 rounded-oppo bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                  <Camera className="w-5 h-5 text-deep-space" />
                </div>
                <span className="text-lg font-bold gradient-text-oppo">OPPO Master</span>
              </div>
              <p className="text-text-secondary mb-4 max-w-sm">
                哈苏影像系统级参数库，让每一次按下快门都充满惊喜。
              </p>
            </div>

            <div>
              <h4 className="font-semibold mb-3">产品</h4>
              <ul className="space-y-2 text-text-secondary">
                <li><Link to="/filter-library" className="hover:text-white transition-colors duration-200">滤镜库</Link></li>
                <li><Link to="/master-params" className="hover:text-white transition-colors duration-200">大师参数</Link></li>
                <li><Link to="/about" className="hover:text-white transition-colors duration-200">关于我们</Link></li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold mb-3">功能</h4>
              <ul className="space-y-2 text-text-secondary">
                <li><Link to="/floating-window" className="hover:text-white transition-colors duration-200">悬浮窗</Link></li>
                <li><Link to="/lut-manager" className="hover:text-white transition-colors duration-200">LUT滤镜</Link></li>
                <li><Link to="/settings" className="hover:text-white transition-colors duration-200">设置</Link></li>
              </ul>
            </div>
          </div>

          <div className="pt-6 border-t border-white/5 flex flex-col md:flex-row items-center justify-between text-text-tertiary text-sm">
            <p>© 2026 OPPO Master. All rights reserved.</p>
            <div className="flex items-center space-x-4 mt-4 md:mt-0">
              <span>Made with ❤️ for ColorOS 16</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
