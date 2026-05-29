import { motion } from 'framer-motion';
import { Camera, Menu, X, Download } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { path: '/', label: '首页' },
  { path: '/ai-demo', label: 'AI场景识别' },
  { path: '/tech', label: '影像工具' },
  { path: '/about', label: '关于我' }
];

const MotionLink = motion(Link);

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
          <Link to="/" className="flex items-center space-x-3">
            <motion.div
              whileHover={{ scale: 1.05 }}
              className="w-10 h-10 md:w-12 md:h-12 rounded-oppo-sm bg-gradient-to-br from-oppo-sunrise-gold via-hasselblad-pro to-hasselblad-dark flex items-center justify-center shadow-lg shadow-oppo-sunrise-gold/20"
            >
              <Camera className="w-6 h-6 md:w-7 md:h-7 text-deep-space" />
            </motion.div>
            <span className="text-xl md:text-2xl font-bold gradient-text-oppo">小O帮帮</span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-8">
            {navItems.map((item) => (
              <MotionLink
                key={item.path}
                to={item.path}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className={`text-sm md:text-base font-medium transition-colors ${
                  location.pathname === item.path
                    ? 'text-oppo-sunrise-gold'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                {item.label}
              </MotionLink>
            ))}
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={handleDownload} 
              className="btn-primary flex items-center gap-2"
            >
              <Download className="w-4 h-4" />
              立即下载
            </motion.button>
          </div>

          {/* Mobile menu button */}
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="md:hidden p-2 text-text-primary"
          >
            {isOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {/* Mobile Navigation */}
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ type: 'spring', damping: 30, stiffness: 300 }}
          className="md:hidden fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl pt-20"
        >
          <div className="flex flex-col items-center space-y-6 p-8">
            {navItems.map((item, i) => (
              <MotionLink
                key={item.path}
                to={item.path}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.1 }}
                onClick={() => setIsOpen(false)}
                className={`text-xl font-medium ${
                  location.pathname === item.path
                    ? 'text-oppo-sunrise-gold'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                {item.label}
              </MotionLink>
            ))}
            <motion.button
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              onClick={() => {
                handleDownload();
                setIsOpen(false);
              }}
              className="btn-primary w-full mt-8"
            >
              立即下载
            </motion.button>
          </div>
        </motion.div>
      )}
    </motion.nav>
  );
}
