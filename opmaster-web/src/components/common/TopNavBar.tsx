import { motion, AnimatePresence } from 'framer-motion';
import { Link, useLocation } from 'react-router-dom';
import { Camera, Menu, X, Download } from 'lucide-react';
import { useState } from 'react';

export default function TopNavBar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const location = useLocation();

  return (
    <>
      <motion.header
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      className="fixed top-0 left-0 right-0 z-40 h-14
        bg-bg-primary/85 backdrop-blur-2xl
        border-b border-white/5
        lg:pl-64"
    >
      <div className="h-full max-w-7xl mx-auto px-4 flex items-center justify-between
        lg:px-8">
        {/* Logo - 仅移动端显示 */}
        <Link to="/" className="flex items-center gap-3 lg:hidden">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange 
            flex items-center justify-center shadow-oppo-elevation-2">
            <Camera className="w-5 h-5 text-oppo-black" />
          </div>
          <span className="text-h2 font-bold gradient-text-oppo">小O帮帮</span>
        </Link>

        {/* 移动端菜单按钮 */}
        <button
          onClick={() => setIsMenuOpen(!isMenuOpen)}
          className="lg:hidden p-2 min-h-[44px] min-w-[44px] 
            flex items-center justify-center rounded-xl
            hover:bg-white/5 transition-colors"
        >
          {isMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>

        {/* 桌面端操作区 */}
        <div className="hidden lg:flex items-center gap-4">
          <button 
            onClick={() => alert('下载功能即将上线')}
            className="btn-primary flex items-center gap-2 text-body2"
          >
            <Download className="w-4 h-4" />
            下载 APP
          </button>
        </div>
      </div>
    </motion.header>

    {/* 移动端全屏菜单 */}
    <AnimatePresence>
      {isMenuOpen && (
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: -20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: -20 }}
          className="fixed inset-0 z-30 bg-bg-primary pt-14 lg:hidden"
          onClick={() => setIsMenuOpen(false)}
        >
          <div className="p-6" onClick={e => e.stopPropagation()}>
            {/* 菜单内容会在后面完善 */}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
    </>
  );
}
