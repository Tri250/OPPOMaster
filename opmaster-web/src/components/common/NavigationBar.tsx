import { motion } from 'framer-motion';
import { Camera, Menu, X, Download } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { path: '/home', label: '首页' },
  { path: '/ai-demo', label: 'AI场景识别' },
  { path: '/tech', label: '影像工具' },
  { path: '/about', label: '关于' }
];

export default function NavigationBar() {
  const [isOpen, setIsOpen] = useState(false);
  const location = useLocation();

  const handleDownload = () => {
    alert('下载功能即将上线，敬请期待！');
  };

  return (
    <motion.nav
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      className="fixed top-0 left-0 right-0 z-50 bg-deep-space/80 backdrop-blur-xl border-b border-white/5"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 md:h-20">
          {/* Logo */}
          <Link to="/home" className="flex items-center space-x-3">
            <motion.div
              whileHover={{ scale: 1.1, rotate: 5 }}
              className="w-10 h-10 md:w-12 md:h-12 bg-gradient-to-br from-hasselblad to-hasselblad rounded-xl flex items-center justify-center shadow-glow-orange"
            >
              <Camera className="w-6 h-6 md:w-7 md:h-7 text-deep-space" />
            </motion.div>
            <span className="text-xl md:text-2xl font-bold text-gradient-hasselblad">
              OPPO Master
            </span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-8">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`relative px-1 py-2 text-body-md font-medium transition-all ${
                  location.pathname === item.path
                    ? 'text-hasselblad'
                    : 'text-neutral-60 hover:text-white'
                }`}
              >
                {location.pathname === item.path && (
                  <motion.div
                    layoutId="nav-indicator"
                    className="absolute left-0 right-0 bottom-0 h-0.5 bg-gradient-to-r from-oppo-coral to-hasselblad"
                    initial={{ scale: 0.8, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    transition={{ type: 'spring', stiffness: 250, damping: 20 }}
                  />
                )}
                {item.label}
              </Link>
            ))}
            
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={handleDownload}
              className="btn-primary text-sm flex items-center gap-2"
            >
              <Download className="w-4 h-4" />
              立即体验
            </motion.button>
          </div>

          {/* Mobile menu button */}
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="md:hidden p-2 text-white"
          >
            {isOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {/* Mobile Navigation */}
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          className="md:hidden bg-deep-space/95 backdrop-blur-xl border-t border-white/10"
        >
          <div className="px-4 py-6 space-y-4">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setIsOpen(false)}
                className={`block px-4 py-3 rounded-xl text-body-lg font-medium transition-all ${
                  location.pathname === item.path
                    ? 'bg-hasselblad/10 text-hasselblad'
                    : 'text-neutral-70 hover:bg-white/5'
                }`}
              >
                {item.label}
              </Link>
            ))}
            
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={handleDownload}
              className="btn-primary w-full text-center mt-6 flex items-center justify-center gap-2"
            >
              <Download className="w-4 h-4" />
              立即体验
            </motion.button>
          </div>
        </motion.div>
      )}
    </motion.nav>
  );
}
