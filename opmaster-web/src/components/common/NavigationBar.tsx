import { motion } from 'framer-motion';
import { Camera, Menu, X } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { path: '/', label: '首页' },
  { path: '/ai-demo', label: 'AI演示' },
  { path: '/tech', label: '技术特性' },
  { path: '/about', label: '关于我们' }
];

export default function NavigationBar() {
  const [isOpen, setIsOpen] = useState(false);
  const location = useLocation();

  return (
    <motion.nav
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      className="fixed top-0 left-0 right-0 z-50 glass-effect"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-2">
            <div className="w-10 h-10 bg-hasselblad rounded-xl flex items-center justify-center">
              <Camera className="w-6 h-6 text-deep-space" />
            </div>
            <span className="text-xl font-bold gradient-text">OMaster</span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-8">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`text-sm font-medium transition-colors ${
                  location.pathname === item.path
                    ? 'text-hasselblad'
                    : 'text-white/70 hover:text-white'
                }`}
              >
                {item.label}
              </Link>
            ))}
            <button className="btn-primary text-sm">立即下载</button>
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
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          className="md:hidden glass-effect border-t border-white/10"
        >
          <div className="px-4 py-4 space-y-3">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setIsOpen(false)}
                className={`block text-base font-medium ${
                  location.pathname === item.path
                    ? 'text-hasselblad'
                    : 'text-white/70'
                }`}
              >
                {item.label}
              </Link>
            ))}
            <button className="btn-primary w-full text-center mt-4">立即下载</button>
          </div>
        </motion.div>
      )}
    </motion.nav>
  );
}
