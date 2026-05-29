import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Heart, Star, ChevronRight, Download, X, Menu, Upload, Filter, Scan } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  // 功能卡片数据 - 按测试用例要求的顺序
  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-accent-primary to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-accent-primary to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  // 影像工具卡片
  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-accent-primary to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      {/* 背景光效 */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      {/* 顶部导航栏 */}
      <nav className="fixed top-0 left-0 right-0 z-50 h-14 bg-deep-space/90 backdrop-blur-xl border-b border-white/5" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-oppo bg-gradient-to-br from-accent-primary to-hasselblad-orange flex items-center justify-center">
                <Camera className="w-5 h-5 text-deep-space" />
              </div>
              <span className="text-lg font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-sm font-medium text-accent-primary touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-sm font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-sm font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-sm font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 touch-feedback">设置</Link>
            </div>

            <button
              className="hidden md:flex items-center space-x-2">
              <button className="btn-primary text-base px-6 py-3 flex items-center space-x-2 touch-feedback" aria-label="下载应用">
                <Download className="w-5 h-5" />
                <span>立即下载</span>
              </button>
            </button>

            <button
              className="md:hidden p-2 rounded-oppo hover:bg-white/5 transition-colors duration-200 min-h-[44px] min-w-[44px] flex items-center justify-center touch-feedback"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {/* 移动端菜单 */}
      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'easeOut' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-accent-primary min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-lg font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary w-full mt-4 min-h-[48px] touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      {/* 主内容区域 */}
      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          {/* 头部区域 */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'easeOut' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5 }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-accent-primary/20 to-hasselblad-orange/20 border border-accent-primary/30 rounded-full px-3 py-1.5 mb-6"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-xs text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5 }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5 }}
              className="text-base md:text-lg text-text-secondary mb-8"
            >
              核心功能展示 - 点击卡片查看详情
            </motion.p>
          </motion.div>

          {/* 功能卡片区域 */}
          <section className="mb-16">
            <div className="space-y-5">
              {featureCards.map((card, index) => (
                <motion.div
                  key={card.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.4 + index * 0.1, duration: 0.5, ease: 'easeOut' }}
                  whileHover={{ y: -4, transition: { duration: 0.2 } }
                  whileTap={{ scale: 0.98 }
                  className="card-oppo-interactive"
                  onClick={() => navigate(card.path)}
                >
                  <div className="p-5">
                    <div className="flex items-start gap-4 mb-3">
                      {/* 卡片图标 */}
                      <div className={`w-12 h-12 rounded-oppo bg-gradient-to-br ${card.gradient} flex items-center justify-center flex-shrink-0 shadow-lg`}>
                        <card.icon className="w-6 h-6 text-deep-space" />
                      </div>

                      {/* 卡片标题和描述 */}
                      <div className="flex-1">
                        <h3 className="text-lg font-semibold text-text-primary mb-1.5">
                          {card.title}
                        </h3>
                        <p className="text-sm text-text-secondary">
                          {card.description}
                        </p>
                      </div>
                    </div>

                    {/* 要点列表 */}
                    <div className="mt-4">
                      <ul className="space-y-1.5">
                        {card.points.map((point, i) => (
                          <li key={i} className="flex items-center gap-2">
                            <span className="w-1.5 h-1.5 rounded-full bg-text-tertiary flex-shrink-0" />
                            <span className="text-xs text-text-tertiary">{point}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>
          </section>

          {/* 影像工具区域 */}
          <motion.section
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 1.0, duration: 0.5 }}
          >
            <div className="mb-6">
              <h2 className="text-xl font-semibold text-text-primary mb-1">
                影像工具
              </h2>
              <p className="text-sm text-text-secondary">
                专业工具，提升创作效率
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {toolCards.map((card, index) => (
                <motion.div
                  key={card.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 1.1 + index * 0.1, duration: 0.5 }}
                  whileHover={{ y: -4, transition: { duration: 0.2 } }
                  whileTap={{ scale: 0.98 }
                  onClick={() => navigate(card.path)}
                  className="card-oppo-interactive"
                >
                  <div className="p-5">
                    <div className={`w-12 h-12 rounded-oppo bg-gradient-to-br ${card.gradient} flex items-center justify-center flex-shrink-0 mb-4 shadow-lg`}>
                      <card.icon className="w-6 h-6 text-deep-space" />
                    </div>

                    <h3 className="text-base font-semibold text-text-primary mb-1">
                      {card.title}
                    </h3>
                    <p className="text-sm text-text-secondary">
                      {card.description}
                    </p>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.section>

          {/* 底部操作区域 */}
          <motion.section
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 1.3, duration: 0.5 }}
            className="mt-16"
          >
            <div className="card-oppo p-6 md:p-8 text-center relative overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-r from-accent-primary/10 to-ocean-blue/10" />
              <div className="relative z-10">
                <h2 className="text-2xl md:text-3xl font-bold mb-4">
                  准备好开始了吗？
                </h2>
                <p className="text-base md:text-lg text-text-secondary mb-6 max-w-md mx-auto">
                  立即下载 OPPO Master，让你的哈苏影像系统发挥全部潜能
                </p>

                <button className="btn-primary text-base px-8 py-4 flex items-center justify-center space-x-2 mx-auto mb-6 touch-feedback" aria-label="免费下载应用">
                  <Download className="w-5 h-5" />
                  <span>免费下载</span>
                </button>
              </div>
            </div>
          </motion.section>
        </div>
      </main>

      {/* 页脚 */}
      <footer className="py-8 px-4 border-t border-white/5" role="contentinfo">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-6 mb-8">
            <div className="md:col-span-2">
              <div className="flex items-center space-x-3 mb-4">
                <div className="w-10 h-10 rounded-oppo bg-gradient-to-br from-accent-primary to-hasselblad-orange flex items-center justify-center">
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
                <li><Link to="/filter-library" className="hover:text-text-primary transition-colors duration-200">滤镜库</Link></li>
                <li><Link to="/master-params" className="hover:text-text-primary transition-colors duration-200">大师参数</Link></li>
                <li><Link to="/about" className="hover:text-text-primary transition-colors duration-200">关于我们</Link></li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold mb-3">功能</h4>
              <ul className="space-y-2 text-text-secondary">
                <li><Link to="/floating-window" className="hover:text-text-primary transition-colors duration-200">悬浮窗</Link></li>
                <li><Link to="/lut-manager" className="hover:text-text-primary transition-colors duration-200">LUT 滤镜</Link></li>
                <li><Link to="/settings" className="hover:text-text-primary transition-colors duration-200">设置</Link></li>
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
