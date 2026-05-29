import { motion } from 'framer-motion'
import { 
  Camera, Menu, X, Download, Sparkles, Scan, 
  Layers, Filter, Cloud, ScanText, Settings,
  Wand2, ChevronRight, ScrollText
} from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'

const mainNavItems = [
  { path: '/', label: '首页' },
  { path: '/filter-library', label: '滤镜库' },
  { path: '/master-params', label: '大师参数' },
  { path: '/tech', label: '技术' },
  { path: '/about', label: '关于' }
]

const featureNavItems = [
  { path: '/floating-window', label: '悬浮窗', icon: <Layers className="w-4 h-4" /> },
  { path: '/ai-finetune', label: 'AI 微调', icon: <Wand2 className="w-4 h-4" /> },
  { path: '/scene-detection', label: '场景识别', icon: <Scan className="w-4 h-4" /> },
  { path: '/lut-manager', label: 'LUT 滤镜', icon: <Filter className="w-4 h-4" /> },
  { path: '/cloud-sync', label: '云同步', icon: <Cloud className="w-4 h-4" /> },
  { path: '/ocr-demo', label: 'OCR 识别', icon: <ScanText className="w-4 h-4" /> },
  { path: '/settings', label: '设置', icon: <Settings className="w-4 h-4" /> },
]

export default function NavigationBar() {
  const [isOpen, setIsOpen] = useState(false)
  const [showFeatures, setShowFeatures] = useState(false)
  const location = useLocation()

  const handleDownload = () => {
    alert('下载功能即将上线，敬请期待！')
  }

  return (
    <motion.nav
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.3, ease: [0.05, 0.7, 0.1, 1.0] }}
      className="fixed top-0 left-0 right-0 z-50 h-14 bg-deep-space/90 backdrop-blur-xl border-b border-white/5"
      role="navigation"
      aria-label="主导航"
    >
      <div className="max-w-7xl mx-auto px-4 h-full">
        <div className="flex items-center justify-between h-full">
          <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
            <div className="w-10 h-10 rounded-oppo bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
              <Camera className="w-5 h-5 text-deep-space" />
            </div>
            <span className="text-lg font-bold gradient-text-oppo">OPPO Master</span>
          </Link>

          <div className="hidden md:flex items-center space-x-6">
            {mainNavItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`text-sm font-medium transition-colors duration-200 touch-feedback ${
                  location.pathname === item.path
                    ? 'text-oppo-sunrise-gold'
                    : 'text-text-secondary hover:text-white'
                }`}
              >
                {item.label}
              </Link>
            ))}

            <div className="relative">
              <button
                onClick={() => setShowFeatures(!showFeatures)}
                className={`flex items-center gap-1 text-sm font-medium transition-colors duration-200 touch-feedback ${
                  featureNavItems.some(item => location.pathname === item.path)
                    ? 'text-oppo-sunrise-gold'
                    : 'text-text-secondary hover:text-white'
                }`}
                aria-expanded={showFeatures}
                aria-haspopup="true"
              >
                <Sparkles className="w-4 h-4" />
                功能
                <ChevronRight className={`w-3 h-3 transition-transform duration-200 ${showFeatures ? 'rotate-90' : ''}`} />
              </button>

              {showFeatures && (
                <motion.div
                  initial={{ opacity: 0, y: 10, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 10, scale: 0.95 }}
                  transition={{ duration: 0.2 }}
                  className="absolute top-full right-0 mt-2 w-48 bg-card-surface rounded-oppo border border-oppo-border shadow-oppo overflow-hidden"
                  role="menu"
                >
                  {featureNavItems.map((item) => (
                    <Link
                      key={item.path}
                      to={item.path}
                      onClick={() => setShowFeatures(false)}
                      className={`flex items-center gap-3 px-4 py-3 text-sm transition-colors duration-200 touch-feedback ${
                        location.pathname === item.path
                          ? 'bg-oppo-sunrise-gold/10 text-oppo-sunrise-gold'
                          : 'text-text-secondary hover:bg-white/5 hover:text-white'
                      }`}
                      role="menuitem"
                    >
                      {item.icon}
                      {item.label}
                    </Link>
                  ))}
                </motion.div>
              )}
            </div>

            <button onClick={handleDownload} className="btn-primary text-sm flex items-center gap-2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              立即下载
            </button>
          </div>

          <button
            onClick={() => setIsOpen(!isOpen)}
            className="md:hidden p-2 text-white touch-feedback min-h-[44px] min-w-[44px] flex items-center justify-center rounded-oppo hover:bg-white/10 transition-colors duration-200"
            aria-expanded={isOpen}
            aria-label={isOpen ? '关闭菜单' : '打开菜单'}
          >
            {isOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {isOpen && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          exit={{ opacity: 0, height: 0 }}
          transition={{ duration: 0.3 }}
          className="md:hidden bg-card-surface border-t border-oppo-border"
          role="menu"
        >
          <div className="px-4 py-4 space-y-2">
            <p className="text-text-tertiary text-xs uppercase font-medium px-2 mb-2">主要导航</p>
            {mainNavItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setIsOpen(false)}
                className={`block px-3 py-3 rounded-oppo text-base font-medium touch-feedback transition-colors duration-200 ${
                  location.pathname === item.path
                    ? 'bg-oppo-sunrise-gold/10 text-oppo-sunrise-gold'
                    : 'text-text-secondary hover:bg-white/5 hover:text-white'
                }`}
                role="menuitem"
              >
                {item.label}
              </Link>
            ))}

            <p className="text-text-tertiary text-xs uppercase font-medium px-2 mt-4 mb-2">功能模块</p>
            {featureNavItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setIsOpen(false)}
                className={`flex items-center gap-3 px-3 py-3 rounded-oppo text-base font-medium touch-feedback transition-colors duration-200 ${
                  location.pathname === item.path
                    ? 'bg-oppo-sunrise-gold/10 text-oppo-sunrise-gold'
                    : 'text-text-secondary hover:bg-white/5 hover:text-white'
                }`}
                role="menuitem"
              >
                {item.icon}
                {item.label}
              </Link>
            ))}

            <button 
              onClick={handleDownload} 
              className="btn-primary w-full text-center mt-4 flex items-center justify-center gap-2"
              aria-label="下载应用"
            >
              <Download className="w-4 h-4" />
              立即下载
            </button>
          </div>
        </motion.div>
      )}
    </motion.nav>
  )
}
