import { motion } from 'framer-motion'
import { Camera, Sparkles, Smartphone, Layers, Palette, Zap, Heart, Star, ArrowRight, Download, Menu, X, ChevronRight, Check, Sun, Moon, Wind, Mountain, Coffee, Users } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppStore } from '../store/useAppStore'
import { mockPresets } from '../data/mockPresets'

export default function AppShowcase() {
  const navigate = useNavigate()
  const { setSelectedPreset } = useAppStore()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [activeSection, setActiveSection] = useState(0)
  const [currentScreenshot, setCurrentScreenshot] = useState(0)

  const features = [
    { icon: Layers, title: '哈苏认证预设', desc: '专业摄影师精心调校，官方认证品质', color: 'text-oppo-sunrise-gold', bgColor: 'bg-oppo-sunrise-gold/10' },
    { icon: Sparkles, title: 'AI 智能推荐', desc: '场景识别，自动匹配最佳参数', color: 'text-aurora-purple', bgColor: 'bg-aurora-purple/10' },
    { icon: Smartphone, title: '系统级悬浮窗', desc: 'ColorOS深度集成，实时预览', color: 'text-ocean-blue', bgColor: 'bg-ocean-blue/10' },
    { icon: Palette, title: '可视化调节', desc: '直观的参数面板，所见即所得', color: 'text-sakura-pink', bgColor: 'bg-sakura-pink/10' },
    { icon: Zap, title: '极速应用', desc: '毫秒级响应，瞬间优化照片', color: 'text-oppo-green', bgColor: 'bg-oppo-green/10' },
    { icon: Camera, title: '全设备支持', desc: '覆盖全系列OPPO/一加机型', color: 'text-hasselblad-pro', bgColor: 'bg-hasselblad/10' },
  ]

  const showcasePresets = [
    { name: '德味预设', device: 'OPPO Find X7 Ultra', isHNCS: true, isNew: true, icon: Moon, id: 'hasselblad_dewei' },
    { name: '富士胶片', device: 'OPPO Find X7 Ultra', isHNCS: true, isNew: false, icon: Users, id: 'fujifilm_film' },
    { name: '胶片感', device: 'OPPO Find X7 Ultra', isHNCS: false, isNew: true, icon: Mountain, id: 'film_sense' },
    { name: '童话', device: 'OPPO Find X7 Ultra', isHNCS: false, isNew: false, icon: Coffee, id: 'fairy_tale' },
    { name: '高对比黑白', device: 'OPPO Find X7 Ultra', isHNCS: true, isNew: false, icon: Wind, id: 'high_contrast_bw' },
    { name: '富士NC', device: 'OPPO Find X7 Ultra', isHNCS: true, isNew: true, icon: Sun, id: 'fujifilm_nc' },
  ]

  const handlePresetClick = (preset: any) => {
    // 找到匹配的预设数据
    const fullPreset = mockPresets.find(p => p.id === preset.id)
    if (fullPreset) {
      setSelectedPreset(fullPreset)
      navigate(`/preset/${preset.id}`)
    }
  }

  const screenshots = [
    { title: '首页探索', desc: '发现精选预设', gradient: 'from-oppo-sunrise-gold/20 to-hasselblad/20' },
    { title: '参数面板', desc: '专业级调节', gradient: 'from-aurora-purple/20 to-ocean-blue/20' },
    { title: '悬浮窗', desc: '实时预览', gradient: 'from-oppo-green/20 to-cyber-teal/20' },
  ]

  const stats = [
    { value: '50,000+', label: '活跃用户' },
    { value: '100+', label: '精选预设' },
    { value: '98%', label: '好评率' },
    { value: '50+', label: '支持机型' },
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      {/* 背景光效 */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-[500px] h-[500px] top-1/4 -left-32 animate-float" />
        <div className="orb-oppo orb-2 w-[400px] h-[400px] top-2/3 -right-32 animate-float-slow" />
        <div className="orb-oppo orb-3 w-[350px] h-[350px] bottom-1/4 left-1/2 animate-float-fast" />
        <div className="orb-oppo orb-4 w-[300px] h-[300px] top-1/2 left-1/4 animate-float" style={{ animationDelay: '3s' }} />
      </div>

      {/* 导航栏 */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-deep-space/80 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16 md:h-20">
            <div className="flex items-center space-x-3">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', stiffness: 200, delay: 0.2 }}
                className="w-10 h-10 md:w-12 md:h-12 rounded-oppo-sm bg-gradient-to-br from-oppo-sunrise-gold via-hasselblad-pro to-hasselblad-dark flex items-center justify-center shadow-lg shadow-oppo-sunrise-gold/20"
              >
                <Camera className="w-6 h-6 md:w-7 md:h-7 text-deep-space" />
              </motion.div>
              <span className="text-xl md:text-2xl font-bold gradient-text-oppo tracking-tight">OMaster</span>
            </div>

            <div className="hidden md:flex items-center space-x-8">
              {['首页', '功能', '预设', '下载'].map((item, i) => (
                <motion.button
                  key={item}
                  onClick={() => setActiveSection(i)}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  className={`text-sm md:text-base font-medium transition-colors ${
                    activeSection === i ? 'text-oppo-sunrise-gold' : 'text-text-secondary hover:text-text-primary'
                  }`}
                >
                  {item}
                </motion.button>
              ))}
            </div>

            <div className="hidden md:flex items-center space-x-4">
              <button className="btn-secondary">登录</button>
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="btn-primary flex items-center space-x-2"
              >
                <Download className="w-4 h-4" />
                <span>立即下载</span>
              </motion.button>
            </div>

            <button className="md:hidden p-2" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {/* 移动端菜单 */}
      <motion.div
        initial={false}
        animate={mobileMenuOpen ? { opacity: 1, x: 0 } : { opacity: 0, x: 300 }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
        className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
      >
        <div className="flex flex-col items-center space-y-6 p-8">
          {['首页', '功能', '预设', '下载'].map((item, i) => (
            <motion.button
              key={item}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
              onClick={() => {
                setActiveSection(i)
                setMobileMenuOpen(false)
              }}
              className="text-xl font-medium text-text-secondary hover:text-text-primary"
            >
              {item}
            </motion.button>
          ))}
          <motion.button
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="btn-primary w-full mt-8"
            onClick={() => setMobileMenuOpen(false)}
          >
            立即下载
          </motion.button>
        </div>
      </motion.div>

      {/* Hero 区域 */}
      <section className="relative pt-24 md:pt-32 pb-20 px-4 overflow-hidden">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">
            <motion.div
              initial="hidden"
              animate="visible"
              className="space-y-8"
            >
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0 }}
              >
                <div className="inline-flex items-center space-x-2 glass-effect rounded-oppo-pill px-4 py-2 mb-6">
                  <span className="w-2 h-2 bg-oppo-green rounded-full animate-pulse" />
                  <span className="text-sm text-text-secondary">全新 ColorOS 16 深度优化</span>
                </div>
              </motion.div>

              <motion.h1
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="text-5xl md:text-6xl lg:text-7xl font-extrabold leading-tight tracking-tight"
              >
                <span className="gradient-text-oppo">哈苏影像</span>
                <br />
                <span className="text-text-primary">触手可及</span>
              </motion.h1>

              <motion.p
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="text-lg md:text-xl text-text-secondary max-w-lg leading-relaxed"
              >
                专为 OPPO 哈苏影像系统打造的专业调色参数库。AI 智能推荐，系统级悬浮窗，让每一次按下快门都充满惊喜。
              </motion.p>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
                className="flex flex-col sm:flex-row gap-4"
              >
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  className="btn-primary text-lg px-8 py-4 flex items-center justify-center space-x-2 animate-pulse-glow"
                >
                  <Download className="w-5 h-5" />
                  <span>免费下载</span>
                </motion.button>
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  className="btn-outline text-lg px-8 py-4 flex items-center justify-center space-x-2"
                >
                  <span>查看演示</span>
                  <ArrowRight className="w-5 h-5" />
                </motion.button>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 }}
                className="grid grid-cols-2 md:grid-cols-4 gap-6 pt-4"
              >
                {stats.map((stat, i) => (
                  <motion.div
                    key={stat.label}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.8 + i * 0.1 }}
                    className="text-center"
                  >
                    <div className="text-2xl md:text-3xl font-bold gradient-text-gold">{stat.value}</div>
                    <div className="text-xs md:text-sm text-text-tertiary mt-1">{stat.label}</div>
                  </motion.div>
                ))}
              </motion.div>
            </motion.div>

            {/* 手机预览图 */}
            <motion.div
              initial={{ opacity: 0, scale: 0.9, rotateY: -15 }}
              animate={{ opacity: 1, scale: 1, rotateY: 0 }}
              transition={{ duration: 0.8, delay: 0.3, type: 'spring', stiffness: 100 }}
              className="relative flex justify-center perspective-1000"
            >
              <div className="relative">
                {/* 手机外框 */}
                <div className="w-72 md:w-80 lg:w-88 h-[580px] md:h-[640px] lg:h-[700px] bg-gradient-to-b from-gray-800 via-gray-900 to-gray-950 rounded-[3rem] p-3 shadow-2xl shadow-black/40">
                  <div className="w-full h-full bg-card-surface rounded-[2.5rem] overflow-hidden relative">
                    {/* 手机状态栏 */}
                    <div className="absolute top-0 left-0 right-0 h-8 md:h-10 bg-deep-space flex items-center justify-between px-6 z-10">
                      <span className="text-xs text-text-secondary">9:41</span>
                      <div className="flex items-center space-x-1">
                        <div className="w-4 h-2 bg-text-secondary rounded-sm" />
                        <div className="w-3 h-3 bg-text-secondary rounded-full" />
                        <div className="w-5 h-2 bg-text-secondary rounded-sm" />
                      </div>
                    </div>

                    {/* 手机内容 */}
                    <div className="pt-12 md:pt-14 h-full bg-gradient-to-b from-deep-space-light to-card-surface">
                      {/* 模拟App界面 */}
                      <div className="px-4 space-y-4">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-text-tertiary text-xs md:text-sm">欢迎回来</p>
                            <p className="text-lg md:text-xl font-semibold">OMaster</p>
                          </div>
                          <div className="flex space-x-2">
                            <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
                              <div className="w-5 h-5 rounded-full border-2 border-text-secondary" />
                            </div>
                          </div>
                        </div>

                        {/* 搜索框 */}
                        <div className="glass-effect rounded-oppo-md px-4 py-3 flex items-center space-x-3">
                          <div className="w-5 h-5 rounded-full border-2 border-text-tertiary" />
                          <span className="text-text-tertiary text-sm">搜索预设、场景...</span>
                        </div>

                        {/* 预设卡片 */}
                        <div className="space-y-3 mt-4">
                          {showcasePresets.slice(0, 3).map((preset, i) => (
                            <motion.div
                              key={preset.name}
                              initial={{ opacity: 0, x: 20 }}
                              animate={{ opacity: 1, x: 0 }}
                              transition={{ delay: 1 + i * 0.1 }}
                              whileHover={{ scale: 1.02, x: 4 }}
                              className="card-glass p-4 flex items-center space-x-4 cursor-pointer"
                            >
                              <div className="w-14 h-14 md:w-16 md:h-16 rounded-xl bg-gradient-to-br from-oppo-sunrise-gold/20 to-ocean-blue/20 flex items-center justify-center">
                                <preset.icon className="w-6 h-6 md:w-7 md:h-7 text-oppo-sunrise-gold" />
                              </div>
                              <div className="flex-1">
                                <div className="flex items-center space-x-2">
                                  <p className="font-medium text-sm md:text-base">{preset.name}</p>
                                  {preset.isHNCS && <span className="tag-hasselblad text-xs">HNCS</span>}
                                </div>
                                <p className="text-text-tertiary text-xs md:text-sm">{preset.device}</p>
                              </div>
                              <Heart className="w-5 h-5 text-text-tertiary" />
                            </motion.div>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* 装饰光效 */}
                <motion.div
                  animate={{ scale: [1, 1.2, 1], opacity: [0.3, 0.5, 0.3] }}
                  transition={{ duration: 3, repeat: Infinity }}
                  className="absolute -top-4 -right-4 w-24 h-24 bg-oppo-sunrise-gold/20 rounded-full blur-2xl"
                />
                <motion.div
                  animate={{ scale: [1, 1.3, 1], opacity: [0.2, 0.4, 0.2] }}
                  transition={{ duration: 4, repeat: Infinity, delay: 1 }}
                  className="absolute -bottom-8 -left-8 w-32 h-32 bg-ocean-blue/20 rounded-full blur-2xl"
                />
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* 功能特性 */}
      <section className="py-20 px-4 relative">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="text-oppo-sunrise-gold font-semibold mb-2 text-sm md:text-base tracking-wider uppercase"
            >
              Core Features
            </motion.p>
            <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold">ColorOS 16 深度集成</h2>
            <p className="text-text-secondary mt-4 max-w-2xl mx-auto">
              与 ColorOS 系统无缝衔接，带来原生级别的流畅体验
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature, i) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -8, scale: 1.02 }}
                className="card-glass p-6 md:p-8 group cursor-pointer"
              >
                <motion.div
                  whileHover={{ scale: 1.1, rotate: 5 }}
                  className={`w-14 h-14 rounded-oppo-md ${feature.bgColor} flex items-center justify-center mb-6 transition-colors`}
                >
                  <feature.icon className={`w-7 h-7 ${feature.color}`} />
                </motion.div>
                <h3 className="text-xl md:text-2xl font-semibold mb-3">{feature.title}</h3>
                <p className="text-text-secondary leading-relaxed">{feature.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* 预设展示 */}
      <section className="py-20 px-4 bg-gradient-to-b from-transparent via-card-surface/30 to-transparent">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="text-oppo-sunrise-gold font-semibold mb-2 text-sm md:text-base tracking-wider uppercase"
            >
              Presets
            </motion.p>
            <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold">发现你的灵感</h2>
            <p className="text-text-secondary mt-4 max-w-2xl mx-auto">
              专业摄影师精心调校，覆盖多种拍摄场景
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {showcasePresets.map((preset, i) => (
              <motion.div
                key={preset.name}
                initial={{ opacity: 0, scale: 0.9 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -4 }}
                onClick={() => handlePresetClick(preset)}
                className="card-oppo overflow-hidden group cursor-pointer"
              >
                <div className="relative h-48 bg-gradient-to-br from-oppo-sunrise-gold/15 to-ocean-blue/15 group-hover:from-oppo-sunrise-gold/25 group-hover:to-ocean-blue/25 transition-all duration-500">
                  <div className="absolute inset-0 flex items-center justify-center">
                    <preset.icon className="w-20 h-20 text-oppo-sunrise-gold/20 group-hover:text-oppo-sunrise-gold/30 transition-colors" />
                  </div>
                  
                  <div className="absolute top-4 left-4 flex space-x-2">
                    {preset.isNew && <span className="tag-new">NEW</span>}
                    {preset.isHNCS && <span className="tag-hasselblad">HNCS</span>}
                  </div>

                  <motion.div
                    initial={{ opacity: 0 }}
                    whileHover={{ opacity: 1 }}
                    className="absolute bottom-4 right-4"
                  >
                    <div className="w-10 h-10 rounded-full bg-black/60 backdrop-blur-sm flex items-center justify-center">
                      <Heart className="w-5 h-5 text-white" />
                    </div>
                  </motion.div>

                  <div className="absolute inset-0 gradient-overlay" />
                </div>

                <div className="p-6">
                  <h3 className="text-lg md:text-xl font-semibold mb-2">{preset.name}</h3>
                  <div className="flex items-center justify-between">
                    <span className="tag-oppo">{preset.device}</span>
                    <div className="flex items-center space-x-1 text-text-tertiary">
                      <Star className="w-4 h-4 fill-oppo-sunrise-gold text-oppo-sunrise-gold" />
                      <span className="text-sm">4.9</span>
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
            className="text-center mt-12"
          >
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="btn-secondary flex items-center space-x-2 mx-auto"
            >
              <span>查看全部预设</span>
              <ChevronRight className="w-4 h-4" />
            </motion.button>
          </motion.div>
        </div>
      </section>

      {/* 界面截图 */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="text-oppo-sunrise-gold font-semibold mb-2 text-sm md:text-base tracking-wider uppercase"
            >
              Interface
            </motion.p>
            <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold">简约而不简单</h2>
            <p className="text-text-secondary mt-4 max-w-2xl mx-auto">
              精心设计的界面，让操作更加直观高效
            </p>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-8">
            {screenshots.map((screen, i) => (
              <motion.div
                key={screen.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.15 }}
                onClick={() => setCurrentScreenshot(i)}
                className={`text-center cursor-pointer transition-all ${currentScreenshot === i ? 'scale-105' : ''}`}
              >
                <div className={`card-elevated p-4 mb-6 ${currentScreenshot === i ? 'ring-2 ring-oppo-sunrise-gold/50' : ''}`}>
                  <div className={`aspect-[9/16] rounded-oppo-lg bg-gradient-to-b ${screen.gradient} flex items-center justify-center`}>
                    <div className="text-center p-8">
                      <div className={`w-20 h-20 rounded-oppo-lg bg-white/10 mx-auto mb-4 flex items-center justify-center ${currentScreenshot === i ? 'animate-pulse-glow' : ''}`}>
                        <Smartphone className="w-10 h-10 text-oppo-sunrise-gold" />
                      </div>
                      <p className="text-text-secondary text-sm">{screen.title}</p>
                    </div>
                  </div>
                </div>
                <h3 className="text-xl font-semibold mb-2">{screen.title}</h3>
                <p className="text-text-secondary">{screen.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA 区域 */}
      <section className="py-24 px-4">
        <div className="max-w-4xl mx-auto">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="card-glass p-8 md:p-16 text-center relative overflow-hidden"
          >
            <div className="absolute inset-0 bg-gradient-to-r from-oppo-sunrise-gold/5 via-transparent to-ocean-blue/5" />
            <div className="relative z-10">
              <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-6">
                准备好开始了吗？
              </h2>
              <p className="text-lg md:text-xl text-text-secondary mb-10 max-w-2xl mx-auto">
                立即下载 OMaster，让你的哈苏影像系统发挥全部潜能
              </p>
              
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-12">
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  className="btn-primary text-lg px-10 py-5 flex items-center space-x-3 animate-pulse-glow"
                >
                  <Download className="w-6 h-6" />
                  <span>免费下载</span>
                </motion.button>
              </div>

              <div className="flex flex-wrap items-center justify-center gap-8 text-sm text-text-secondary">
                <motion.div
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  className="flex items-center space-x-2"
                >
                  <Check className="w-5 h-5 text-oppo-green" />
                  <span>完全免费</span>
                </motion.div>
                <motion.div
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: 0.1 }}
                  className="flex items-center space-x-2"
                >
                  <Check className="w-5 h-5 text-oppo-green" />
                  <span>无需登录</span>
                </motion.div>
                <motion.div
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: 0.2 }}
                  className="flex items-center space-x-2"
                >
                  <Check className="w-5 h-5 text-oppo-green" />
                  <span>无广告</span>
                </motion.div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* 页脚 */}
      <footer className="py-12 px-4 border-t border-white/5">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 mb-12">
            <div className="md:col-span-2">
              <div className="flex items-center space-x-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                  <Camera className="w-6 h-6 text-deep-space" />
                </div>
                <span className="text-xl font-bold gradient-text-oppo">OMaster</span>
              </div>
              <p className="text-text-secondary mb-6 max-w-sm leading-relaxed">
                哈苏影像系统级参数中枢，让每一次按下快门都充满惊喜。
              </p>
            </div>

            <div>
              <h4 className="font-semibold mb-4">产品</h4>
              <ul className="space-y-3 text-text-secondary">
                <li><a href="#" className="hover:text-text-primary transition-colors">功能介绍</a></li>
                <li><a href="#" className="hover:text-text-primary transition-colors">预设库</a></li>
                <li><a href="#" className="hover:text-text-primary transition-colors">更新日志</a></li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold mb-4">支持</h4>
              <ul className="space-y-3 text-text-secondary">
                <li><a href="#" className="hover:text-text-primary transition-colors">帮助中心</a></li>
                <li><a href="#" className="hover:text-text-primary transition-colors">联系我们</a></li>
                <li><a href="#" className="hover:text-text-primary transition-colors">隐私政策</a></li>
              </ul>
            </div>
          </div>

          <div className="pt-8 border-t border-white/5 flex flex-col md:flex-row items-center justify-between text-text-tertiary text-sm">
            <p>© 2024 OMaster. All rights reserved.</p>
            <div className="flex items-center space-x-6 mt-4 md:mt-0">
              <span className="flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-oppo-green animate-pulse" />
                <span>Made with ❤️ for ColorOS</span>
              </span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}