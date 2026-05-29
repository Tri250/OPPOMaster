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

      <nav className="fixed top-0 left-0 right-0 z-50 bg-deep-space/80 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <Link to="/" className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                <Camera className="w-6 h-6 text-deep-space" />
              </div>
              <span className="text-xl font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-sm font-medium text-oppo-sunrise-gold">首页</Link>
              <Link to="/filter-library" className="text-sm font-medium text-text-secondary hover:text-white transition-colors">滤镜库</Link>
              <Link to="/master-params" className="text-sm font-medium text-text-secondary hover:text-white transition-colors">大师参数</Link>
              <Link to="/floating-window" className="text-sm font-medium text-text-secondary hover:text-white transition-colors">悬浮窗</Link>
              <Link to="/settings" className="text-sm font-medium text-text-secondary hover:text-white transition-colors">设置</Link>
            </div>

            <div className="hidden md:flex items-center space-x-4">
              <button className="btn-primary text-lg px-8 py-4 flex items-center space-x-2 animate-pulse-glow">
                <Download className="w-5 h-5" />
                <span>立即下载</span>
              </button>
            </div>

            <button className="md:hidden" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
              {mobileMenuOpen ? <X /> : <Menu />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
        >
          <div className="flex flex-col items-center space-y-6 p-8">
            <Link to="/" className="text-xl font-medium text-oppo-sunrise-gold">首页</Link>
            <Link to="/filter-library" className="text-xl font-medium text-text-secondary">滤镜库</Link>
            <Link to="/master-params" className="text-xl font-medium text-text-secondary">大师参数</Link>
            <Link to="/floating-window" className="text-xl font-medium text-text-secondary">悬浮窗</Link>
            <Link to="/settings" className="text-xl font-medium text-text-secondary">设置</Link>
            <button className="btn-primary w-full mt-8">立即下载</button>
          </div>
        </motion.div>
      )}

      <section className="relative pt-32 pb-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <motion.div
              initial="hidden"
              animate="animate"
              variants={ColorOSAnimations.fadeIn}
              className="space-y-8"
            >
              <motion.div variants={ColorOSAnimations.fadeIn}>
                <div className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-sunrise-gold/20 to-hasselblad-pro/20 border border-oppo-sunrise-gold/30 rounded-full px-4 py-2 mb-6">
                  <span className="w-2 h-2 bg-oppo-green rounded-full animate-pulse" />
                  <span className="text-sm text-text-secondary">ColorOS 16 专业摄影增强</span>
                </div>
              </motion.div>

              <motion.h1
                variants={ColorOSAnimations.fadeIn}
                className="text-5xl md:text-7xl font-bold leading-tight"
              >
                <span className="gradient-text-oppo">哈苏影像</span>
                <br />
                <span className="text-white">触手可及</span>
              </motion.h1>

              <motion.p
                variants={ColorOSAnimations.fadeIn}
                className="text-xl text-text-secondary max-w-lg"
              >
                专为OPPO/一加设计的专业调色工具。
                系统级悬浮窗，实时预览滤镜效果，
                让每一次按下快门都充满惊喜。
              </motion.p>

              <motion.div
                variants={ColorOSAnimations.fadeIn}
                className="flex flex-col sm:flex-row gap-4"
              >
                <button className="btn-primary text-lg px-8 py-4 flex items-center justify-center space-x-2 animate-pulse-glow">
                  <Download className="w-5 h-5" />
                  <span>免费下载</span>
                </button>
                <Link to="/filter-library" className="btn-secondary text-lg px-8 py-4 flex items-center justify-center space-x-2">
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
                <div className="w-72 md:w-80 h-[580px] md:h-[640px] bg-gradient-to-b from-gray-800 to-gray-900 rounded-[3rem] p-3 shadow-2xl shadow-oppo-sunrise-gold/10">
                  <div className="w-full h-full bg-card-surface rounded-[2.5rem] overflow-hidden relative">
                    <div className="absolute top-0 left-0 right-0 h-8 bg-deep-space flex items-center justify-between px-6 z-10">
                      <span className="text-xs text-text-tertiary">9:41</span>
                      <div className="flex items-center space-x-1">
                        <div className="w-4 h-2 bg-text-tertiary rounded-sm" />
                        <div className="w-3 h-3 bg-text-tertiary rounded-full" />
                      </div>
                    </div>

                    <div className="pt-8 h-full bg-gradient-to-b from-deep-space to-card-surface">
                      <div className="px-4 space-y-4">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-text-tertiary text-sm">哈苏影像大师</p>
                            <p className="text-lg font-semibold text-white">OPPO Master</p>
                          </div>
                          <div className="flex space-x-2">
                            <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
                              <div className="w-5 h-5 rounded-full border-2 border-text-secondary" />
                            </div>
                          </div>
                        </div>

                        <div className="absolute top-28 right-4 z-20">
                          <div className="w-64 p-4 bg-black/85 backdrop-blur-xl border border-white/15 rounded-[16px] shadow-lg">
                            <div className="flex items-center justify-between mb-2">
                              <p className="text-white font-medium text-sm">富士胶片</p>
                              <div className="flex items-center gap-2">
                                <button className="w-6 h-6 rounded-full bg-white/5 flex items-center justify-center">
                                  <div className="w-3 h-3 rounded-full border border-text-tertiary" />
                                </button>
                                <button className="w-6 h-6 rounded-full bg-white/5 flex items-center justify-center">
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

                        <div className="bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 rounded-2xl aspect-[4/3] mt-16 mb-6 flex items-center justify-center">
                          <Camera className="w-16 h-16 text-oppo-sunrise-gold/50" />
                        </div>

                        <div className="flex items-center justify-between">
                          <div className="flex gap-3">
                            {[1, 2, 3].map((i) => (
                              <div key={i} className="w-12 h-12 rounded-xl bg-white/5" />
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

      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">P0 核心体验</p>
            <h2 className="text-3xl md:text-5xl font-bold">MVP 必备功能</h2>
            <p className="text-text-secondary mt-4 max-w-2xl mx-auto">
              第1-3周优先实现，打造核心产品体验
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {p0Features.map((feature, i) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -4, scale: 1.02 }}
                className="card-oppo group cursor-pointer"
              >
                <Link to={feature.path}>
                  <div className="w-14 h-14 rounded-2xl bg-white/5 flex items-center justify-center mb-6 group-hover:bg-oppo-sunrise-gold/10 transition-colors">
                    <feature.icon className={`w-7 h-7 ${feature.color}`} />
                  </div>
                  <h3 className="text-xl font-semibold mb-3">{feature.title}</h3>
                  <p className="text-text-secondary">{feature.description}</p>
                </Link>
              </motion.div>
            ))}
          </div>
          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="mt-8 text-center"
          >
            <Link to="/p0-overview">
              <button className="btn-secondary flex items-center gap-2 mx-auto">
                <span>查看全部 P0 功能总览</span>
                <ChevronRight className="w-5 h-5" />
              </button>
            </Link>
          </motion.div>
        </div>
      </section>

      <section className="py-20 px-4 bg-gradient-to-b from-transparent via-card-surface/50 to-transparent">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">滤镜库</p>
            <h2 className="text-3xl md:text-5xl font-bold">专业调色预设</h2>
          </motion.div>

          <div className="flex flex-wrap justify-center gap-2 mb-10">
            {filterCategories.map((cat, i) => (
              <button
                key={cat.name}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 ${
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
                className="relative overflow-hidden rounded-xl cursor-pointer"
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
                
                <div className="p-3 bg-card-surface">
                  <p className="text-white font-medium text-sm truncate">{filter.name}</p>
                </div>
              </motion.div>
            ))}
          </div>

          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-center mt-12"
          >
            <Link to="/filter-library" className="btn-secondary inline-flex items-center">
              查看全部滤镜
              <ChevronRight className="w-4 h-4 ml-2" />
            </Link>
          </motion.div>
        </div>
      </section>

      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">大师参数库</p>
            <h2 className="text-3xl md:text-5xl font-bold">专业摄影师作品</h2>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {masterPresets.map((preset, i) => (
              <motion.div
                key={preset.name}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -4 }}
                className="card-oppo overflow-hidden group cursor-pointer"
              >
                <div className="aspect-[4/3] bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 relative">
                  <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
                  
                  <div className="absolute top-3 left-3 flex gap-1.5 z-10">
                    {preset.isNew && <span className="px-2 py-1 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>}
                    {preset.isHNCS && <span className="px-2 py-1 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">HNCS</span>}
                  </div>

                  <div className="absolute bottom-3 left-3 right-3 z-10">
                    <span className="px-2 py-1 bg-black/50 backdrop-blur-sm text-white text-xs rounded-full">
                      {preset.category}
                    </span>
                  </div>

                  <div className="absolute inset-0 gradient-overlay opacity-0 group-hover:opacity-100 transition-opacity" />
                </div>

                <div className="p-5">
                  <h3 className="text-lg font-semibold mb-2">{preset.name}</h3>
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
            className="mt-8 text-center"
          >
            <Link to="/master-params" className="btn-secondary inline-flex items-center">
              查看全部大师参数
              <ChevronRight className="w-4 h-4 ml-2" />
            </Link>
          </motion.div>
        </div>
      </section>

      <section className="py-20 px-4 bg-gradient-to-b from-transparent via-card-surface/50 to-transparent">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">完整功能</p>
            <h2 className="text-3xl md:text-5xl font-bold">全方位摄影增强</h2>
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
                className="card-oppo p-5 group cursor-pointer"
              >
                <Link to={feature.path}>
                  <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center mb-4 group-hover:bg-oppo-sunrise-gold/10 transition-colors">
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

      <section className="py-24 px-4">
        <div className="max-w-4xl mx-auto">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="card-oppo p-8 md:p-16 text-center relative overflow-hidden"
          >
            <div className="absolute inset-0 bg-gradient-to-r from-oppo-sunrise-gold/10 to-ocean-blue/10" />
            <div className="relative z-10">
              <h2 className="text-3xl md:text-5xl font-bold mb-6">
                准备好开始了吗？
              </h2>
              <p className="text-xl text-text-secondary mb-10 max-w-2xl mx-auto">
                立即下载 OPPO Master，让你的哈苏影像系统发挥全部潜能
              </p>
              
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-12">
                <button className="btn-primary text-lg px-10 py-5 flex items-center justify-center space-x-2 animate-pulse-glow">
                  <Download className="w-6 h-6" />
                  <span>免费下载</span>
                </button>
              </div>

              <div className="flex flex-wrap items-center justify-center gap-8 text-sm text-text-secondary">
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

      <footer className="py-12 px-4 border-t border-white/5">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 mb-12">
            <div className="md:col-span-2">
              <div className="flex items-center space-x-3 mb-4">
                <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                  <Camera className="w-6 h-6 text-deep-space" />
                </div>
                <span className="text-xl font-bold gradient-text-oppo">OPPO Master</span>
              </div>
              <p className="text-text-secondary mb-6 max-w-sm">
                哈苏影像系统级参数库，让每一次按下快门都充满惊喜。
              </p>
            </div>

            <div>
              <h4 className="font-semibold mb-4">产品</h4>
              <ul className="space-y-3 text-text-secondary">
                <li><Link to="/filter-library" className="hover:text-white transition-colors">滤镜库</Link></li>
                <li><Link to="/master-params" className="hover:text-white transition-colors">大师参数</Link></li>
                <li><Link to="/about" className="hover:text-white transition-colors">关于我们</Link></li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold mb-4">功能</h4>
              <ul className="space-y-3 text-text-secondary">
                <li><Link to="/floating-window" className="hover:text-white transition-colors">悬浮窗</Link></li>
                <li><Link to="/lut-manager" className="hover:text-white transition-colors">LUT滤镜</Link></li>
                <li><Link to="/settings" className="hover:text-white transition-colors">设置</Link></li>
              </ul>
            </div>
          </div>

          <div className="pt-8 border-t border-white/5 flex flex-col md:flex-row items-center justify-between text-text-tertiary text-sm">
            <p>© 2026 OPPO Master. All rights reserved.</p>
            <div className="flex items-center space-x-6 mt-4 md:mt-0">
              <span>Made with ❤️ for ColorOS 16</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
