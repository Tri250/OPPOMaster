import { motion } from 'framer-motion'
import { Camera, Sparkles, Smartphone, Layers, Palette, Zap, Heart, Star, ArrowRight, Download, Menu, X, ChevronRight, Check } from 'lucide-react'
import { useState } from 'react'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [activeSection, setActiveSection] = useState(0)

  const features = [
    { icon: Layers, title: '哈苏认证预设', desc: '专业摄影师精心调校，官方认证品质', color: 'text-oppo-sunrise-gold' },
    { icon: Sparkles, title: 'AI 智能推荐', desc: '场景识别，自动匹配最佳参数', color: 'text-aurora-purple' },
    { icon: Smartphone, title: '系统级悬浮窗', desc: 'ColorOS深度集成，实时预览', color: 'text-ocean-blue' },
    { icon: Palette, title: '可视化调节', desc: '直观的参数面板，所见即所得', color: 'text-sakura-pink' },
    { icon: Zap, title: '极速应用', desc: '毫秒级响应，瞬间优化照片', color: 'text-oppo-green' },
    { icon: Camera, title: '全设备支持', desc: '覆盖全系列OPPO/一加机型', color: 'text-hasselblad-pro' },
  ]

  const presets = [
    { name: '城市夜景', device: 'Find X7 Ultra', isHNCS: true, isNew: true },
    { name: '人像大师', device: 'Reno 12 Pro', isHNCS: true, isNew: false },
    { name: '风光摄影', device: 'Find X6 Pro', isHNCS: false, isNew: true },
    { name: '美食探店', device: 'Reno 11', isHNCS: false, isNew: false },
    { name: '街头抓拍', device: 'Find X7', isHNCS: true, isNew: false },
    { name: '自然风光', device: '一加 12', isHNCS: true, isNew: false },
  ]

  const screenshots = [
    { title: '首页探索', desc: '发现精选预设' },
    { title: '参数面板', desc: '专业级调节' },
    { title: '悬浮窗', desc: '实时预览' },
  ]

  const sectionVariants = {
    hidden: { opacity: 0, y: 60 },
    visible: (i: number) => ({
      opacity: 1,
      y: 0,
      transition: {
        delay: i * 0.1,
        duration: 0.6,
        ease: 'easeOut',
      },
    }),
  }

  return (
    <div className="min-h-screen bg-deep-space text-white overflow-x-hidden">
      {/* 背景光效 */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      {/* 导航栏 */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-deep-space/80 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                <Camera className="w-6 h-6 text-deep-space" />
              </div>
              <span className="text-xl font-bold gradient-text-oppo">OMaster</span>
            </div>

            <div className="hidden md:flex items-center space-x-8">
              {['首页', '功能', '预设', '下载'].map((item, i) => (
                <button
                  key={item}
                  onClick={() => setActiveSection(i)}
                  className={`text-sm font-medium transition-colors ${
                    activeSection === i ? 'text-oppo-sunrise-gold' : 'text-text-secondary hover:text-white'
                  }`}
                >
                  {item}
                </button>
              ))}
            </div>

            <div className="hidden md:flex items-center space-x-4">
              <button className="btn-secondary">登录</button>
              <button className="btn-primary flex items-center space-x-2">
                <Download className="w-4 h-4" />
                <span>立即下载</span>
              </button>
            </div>

            <button className="md:hidden" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
              {mobileMenuOpen ? <X /> : <Menu />}
            </button>
          </div>
        </div>
      </nav>

      {/* 移动端菜单 */}
      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
        >
          <div className="flex flex-col items-center space-y-6 p-8">
            {['首页', '功能', '预设', '下载'].map((item) => (
              <button key={item} className="text-xl font-medium text-text-secondary hover:text-white">
                {item}
              </button>
            ))}
            <button className="btn-primary w-full mt-8">立即下载</button>
          </div>
        </motion.div>
      )}

      {/* Hero 区域 */}
      <section className="relative pt-32 pb-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <motion.div
              initial="hidden"
              animate="visible"
              className="space-y-8"
            >
              <motion.div custom={0} variants={sectionVariants}>
                <div className="inline-flex items-center space-x-2 bg-white/5 border border-white/10 rounded-full px-4 py-2 mb-6">
                  <span className="w-2 h-2 bg-oppo-sunrise-gold rounded-full animate-pulse" />
                  <span className="text-sm text-text-secondary">全新 ColorOS 16 优化</span>
                </div>
              </motion.div>

              <motion.h1
                custom={1}
                variants={sectionVariants}
                className="text-5xl md:text-7xl font-bold leading-tight"
              >
                <span className="gradient-text-oppo">哈苏影像</span>
                <br />
                <span className="text-white">触手可及</span>
              </motion.h1>

              <motion.p
                custom={2}
                variants={sectionVariants}
                className="text-xl text-text-secondary max-w-lg"
              >
                专为 OPPO 哈苏影像系统打造的专业调色参数库。AI 智能推荐，系统级悬浮窗，让每一次按下快门都充满惊喜。
              </motion.p>

              <motion.div
                custom={3}
                variants={sectionVariants}
                className="flex flex-col sm:flex-row gap-4"
              >
                <button className="btn-primary text-lg px-8 py-4 flex items-center justify-center space-x-2 animate-pulse-glow">
                  <Download className="w-5 h-5" />
                  <span>免费下载</span>
                </button>
                <button className="btn-secondary text-lg px-8 py-4 flex items-center justify-center space-x-2">
                  <span>查看演示</span>
                  <ArrowRight className="w-5 h-5" />
                </button>
              </motion.div>

              <motion.div
                custom={4}
                variants={sectionVariants}
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

            {/* 手机预览图 */}
            <motion.div
              initial={{ opacity: 0, scale: 0.8, rotate: -5 }}
              animate={{ opacity: 1, scale: 1, rotate: 0 }}
              transition={{ duration: 0.8, delay: 0.3 }}
              className="relative flex justify-center"
            >
              <div className="relative">
                {/* 手机外框 */}
                <div className="w-72 md:w-80 h-[580px] md:h-[640px] bg-gradient-to-b from-gray-800 to-gray-900 rounded-[3rem] p-3 shadow-2xl shadow-oppo-sunrise-gold/10">
                  <div className="w-full h-full bg-card-surface rounded-[2.5rem] overflow-hidden relative">
                    {/* 手机状态栏 */}
                    <div className="absolute top-0 left-0 right-0 h-8 bg-deep-space flex items-center justify-between px-6 z-10">
                      <span className="text-xs text-text-secondary">9:41</span>
                      <div className="flex items-center space-x-1">
                        <div className="w-4 h-2 bg-text-secondary rounded-sm" />
                        <div className="w-3 h-3 bg-text-secondary rounded-full" />
                        <div className="w-5 h-2 bg-text-secondary rounded-sm" />
                      </div>
                    </div>

                    {/* 手机内容 */}
                    <div className="pt-12 h-full bg-gradient-to-b from-deep-space to-card-surface">
                      {/* 模拟App界面 */}
                      <div className="px-4 space-y-4">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-text-tertiary text-sm">欢迎回来</p>
                            <p className="text-lg font-semibold">OMaster</p>
                          </div>
                          <div className="flex space-x-2">
                            <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
                              <div className="w-5 h-5 rounded-full border-2 border-text-secondary" />
                            </div>
                          </div>
                        </div>

                        {/* 搜索框 */}
                        <div className="bg-white/5 rounded-2xl px-4 py-3 flex items-center space-x-3">
                          <div className="w-5 h-5 rounded-full border-2 border-text-tertiary" />
                          <span className="text-text-tertiary text-sm">搜索预设、场景...</span>
                        </div>

                        {/* 预设卡片 */}
                        <div className="space-y-3 mt-4">
                          {presets.slice(0, 3).map((preset, i) => (
                            <motion.div
                              key={preset.name}
                              initial={{ opacity: 0, x: 20 }}
                              animate={{ opacity: 1, x: 0 }}
                              transition={{ delay: 0.8 + i * 0.1 }}
                              className="bg-elevated rounded-2xl p-4 flex items-center space-x-4"
                            >
                              <div className="w-16 h-16 rounded-xl bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex items-center justify-center">
                                <Camera className="w-6 h-6 text-oppo-sunrise-gold" />
                              </div>
                              <div className="flex-1">
                                <div className="flex items-center space-x-2">
                                  <p className="font-medium">{preset.name}</p>
                                  {preset.isHNCS && <span className="tag-hasselblad text-xs">HNCS</span>}
                                </div>
                                <p className="text-text-tertiary text-sm">{preset.device}</p>
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
                <div className="absolute -top-4 -right-4 w-24 h-24 bg-oppo-sunrise-gold/20 rounded-full blur-2xl" />
                <div className="absolute -bottom-8 -left-8 w-32 h-32 bg-ocean-blue/20 rounded-full blur-2xl" />
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
            <p className="text-oppo-sunrise-gold font-medium mb-2">核心功能</p>
            <h2 className="text-3xl md:text-5xl font-bold">ColorOS 16 深度集成</h2>
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
                className="card-oppo p-8 group cursor-pointer"
              >
                <div className={`w-14 h-14 rounded-2xl bg-white/5 flex items-center justify-center mb-6 group-hover:bg-oppo-sunrise-gold/10 transition-colors`}>
                  <feature.icon className={`w-7 h-7 ${feature.color}`} />
                </div>
                <h3 className="text-xl font-semibold mb-3">{feature.title}</h3>
                <p className="text-text-secondary">{feature.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* 预设展示 */}
      <section className="py-20 px-4 bg-gradient-to-b from-transparent via-card-surface/50 to-transparent">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <p className="text-oppo-sunrise-gold font-medium mb-2">精选预设</p>
            <h2 className="text-3xl md:text-5xl font-bold">发现你的灵感</h2>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {presets.map((preset, i) => (
              <motion.div
                key={preset.name}
                initial={{ opacity: 0, scale: 0.9 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -4 }}
                className="card-oppo overflow-hidden group cursor-pointer"
              >
                <div className="relative h-48 bg-gradient-to-br from-oppo-sunrise-gold/20 to-ocean-blue/20 group-hover:from-oppo-sunrise-gold/30 group-hover:to-ocean-blue/30 transition-all">
                  <div className="absolute inset-0 flex items-center justify-center">
                    <Camera className="w-16 h-16 text-oppo-sunrise-gold/30" />
                  </div>
                  
                  <div className="absolute top-4 left-4 flex space-x-2">
                    {preset.isNew && <span className="tag-new">NEW</span>}
                    {preset.isHNCS && <span className="tag-hasselblad">HNCS</span>}
                  </div>

                  <div className="absolute bottom-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity">
                    <div className="w-10 h-10 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center">
                      <Heart className="w-5 h-5 text-white" />
                    </div>
                  </div>

                  <div className="absolute inset-0 gradient-overlay" />
                </div>

                <div className="p-6">
                  <h3 className="text-lg font-semibold mb-2">{preset.name}</h3>
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
            <button className="btn-secondary">
              查看全部预设
              <ChevronRight className="w-4 h-4 ml-2" />
            </button>
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
            <p className="text-oppo-sunrise-gold font-medium mb-2">用户界面</p>
            <h2 className="text-3xl md:text-5xl font-bold">简约而不简单</h2>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-8">
            {screenshots.map((screen, i) => (
              <motion.div
                key={screen.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.15 }}
                className="text-center"
              >
                <div className="card-elevated p-4 mb-6">
                  <div className="aspect-[9/16] rounded-2xl bg-gradient-to-b from-deep-space to-card-surface flex items-center justify-center">
                    <div className="text-center">
                      <div className="w-16 h-16 rounded-2xl bg-oppo-sunrise-gold/10 mx-auto mb-4 flex items-center justify-center">
                        <Smartphone className="w-8 h-8 text-oppo-sunrise-gold" />
                      </div>
                      <p className="text-text-tertiary text-sm">{screen.title}</p>
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
            className="card-oppo p-8 md:p-16 text-center relative overflow-hidden"
          >
            <div className="absolute inset-0 bg-gradient-to-r from-oppo-sunrise-gold/10 to-ocean-blue/10" />
            <div className="relative z-10">
              <h2 className="text-3xl md:text-5xl font-bold mb-6">
                准备好开始了吗？
              </h2>
              <p className="text-xl text-text-secondary mb-10 max-w-2xl mx-auto">
                立即下载 OMaster，让你的哈苏影像系统发挥全部潜能
              </p>
              
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-12">
                <button className="btn-primary text-lg px-10 py-5 flex items-center space-x-3 animate-pulse-glow">
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
              <p className="text-text-secondary mb-6 max-w-sm">
                哈苏影像系统级参数中枢，让每一次按下快门都充满惊喜。
              </p>
            </div>

            <div>
              <h4 className="font-semibold mb-4">产品</h4>
              <ul className="space-y-3 text-text-secondary">
                <li><a href="#" className="hover:text-white transition-colors">功能介绍</a></li>
                <li><a href="#" className="hover:text-white transition-colors">预设库</a></li>
                <li><a href="#" className="hover:text-white transition-colors">更新日志</a></li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold mb-4">支持</h4>
              <ul className="space-y-3 text-text-secondary">
                <li><a href="#" className="hover:text-white transition-colors">帮助中心</a></li>
                <li><a href="#" className="hover:text-white transition-colors">联系我们</a></li>
                <li><a href="#" className="hover:text-white transition-colors">隐私政策</a></li>
              </ul>
            </div>
          </div>

          <div className="pt-8 border-t border-white/5 flex flex-col md:flex-row items-center justify-between text-text-tertiary text-sm">
            <p>© 2024 OMaster. All rights reserved.</p>
            <div className="flex items-center space-x-6 mt-4 md:mt-0">
              <span>Made with ❤️ for ColorOS</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
