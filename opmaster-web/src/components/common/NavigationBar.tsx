import { motion } from 'framer-motion'
import { 
  Camera, Menu, X, Download, Sparkles, 
  ChevronRight
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
  { path: '/floating-window', label: '悬浮窗', icon: <Sparkles className="w-4 h-4" /> },
  { path: '/ai-finetune', label: 'AI微调', icon: <Sparkles className="w-4 h-4" /> },
  { path: '/scene-detection', label: '场景识别', icon: <Sparkles className="w-4 h-4" /> },
  { path: '/lut-manager', label: 'LUT滤镜', icon: <Sparkles className="w-4 h-4" /> },
  { path: '/cloud-sync', label: '云同步', icon: <Sparkles className="w-4 h-4" /> },
  { path: '/settings', label: '设置', icon: <Sparkles className="w-4 h-4" /> },
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
      transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
      className="nav-bar"
      role="navigation"
      aria-label="主导航"
    >
      <div className="max-w-7xl mx-auto px-4 h-full">
        <div className="flex items-center justify-between h-full">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
            <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
              <Camera className="w-5 h-5 text-oppo-black" />
            </div>
            <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-8">
            {mainNavItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`text-body2 font-medium transition-all duration-200 ease-out-cubic touch-feedback ${
                  location.pathname === item.path
                    ? 'text-oppo-orange'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                {item.label}
              </Link>
            ))}

            {/* Features Dropdown */}
            <div className="relative">
              <button
                onClick={() => setShowFeatures(!showFeatures)}
                className={`flex items-center gap-1 text-body2 font-medium transition-all duration-200 ease-out-cubic touch-feedback ${
                  featureNavItems.some(item => location.pathname === item.path)
                    ? 'text-oppo-orange'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
                aria-expanded={showFeatures}
                aria-haspopup="true"
              >
                <Sparkles className="w-4 h-4" />
                功能
                <ChevronRight className={`w-3 h-3 transition-transform duration-200 ease-out-cubic ${showFeatures ? 'rotate-90' : ''}`} />
              </button>

              {showFeatures && (
                <motion.div
                  initial={{ opacity: 0, y: 10, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 10, scale: 0.95 }}
                  transition={{ duration: 0.2, ease: 'ease-out-cubic' }}
                  className="absolute top-full right-0 mt-2 w-48 bg-bg-secondary rounded-md border border-border-default shadow-oppo-elevation-2 overflow-hidden z-50"
                  role="menu"
                >
                  {featureNavItems.map((item) => (
                    <Link
                      key={item.path}
                      to={item.path}
                      onClick={() => setShowFeatures(false)}
                      className={`flex items-center gap-3 px-4 py-3 text-body2 transition-all duration-200 ease-out-cubic touch-feedback ${
                        location.pathname === item.path
                          ? 'bg-oppo-orange/10 text-oppo-orange'
                          : 'text-text-secondary hover:bg-white/5 hover:text-text-primary'
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

            {/* Download Button */}
            <button 
              onClick={handleDownload} 
              className="btn-primary text-body2 flex items-center gap-2"
              aria-label="下载应用"
            >
              <Download className="w-4 h-4" />
              立即下载
            </button>
          </div>

          {/* Mobile Menu Button */}
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200"
            aria-expanded={isOpen}
            aria-label={isOpen ? '关闭菜单' : '打开菜单'}
          >
            {isOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          exit={{ opacity: 0, height: 0 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="md:hidden bg-bg-secondary border-t border-border-default"
          role="menu"
        >
          <div className="px-4 py-4 space-y-3">
            <p className="text-text-tertiary text-caption font-medium px-2 mb-2">主要导航</p>
            {mainNavItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setIsOpen(false)}
                className={`block px-3 py-3 rounded-sm text-body1 font-medium touch-feedback transition-all duration-200 ease-out-cubic ${
                  location.pathname === item.path
                    ? 'bg-oppo-orange/10 text-oppo-orange'
                    : 'text-text-secondary hover:bg-white/5 hover:text-text-primary'
                }`}
                role="menuitem"
              >
                {item.label}
              </Link>
            ))}

            <p className="text-text-tertiary text-caption font-medium px-2 mt-4 mb-2">功能模块</p>
            {featureNavItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setIsOpen(false)}
                className={`flex items-center gap-3 px-3 py-3 rounded-sm text-body1 font-medium touch-feedback transition-all duration-200 ease-out-cubic ${
                  location.pathname === item.path
                    ? 'bg-oppo-orange/10 text-oppo-orange'
                    : 'text-text-secondary hover:bg-white/5 hover:text-text-primary'
                }`}
                role="menuitem"
              >
                {item.icon}
                {item.label}
              </Link>
            ))}

            {/* Download Button for Mobile */}
            <button 
              onClick={handleDownload} 
              className="btn-primary-large w-full text-center mt-4 flex items-center justify-center gap-2"
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

