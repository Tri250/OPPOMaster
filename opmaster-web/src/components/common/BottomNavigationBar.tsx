import { motion } from 'framer-motion';
import { 
  Home, 
  Palette, 
  Sparkles, 
  Layers, 
  User 
} from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';

const easeOppoEnter = [0.05, 0.7, 0.1, 1.0] as [number, number, number, number];
const easeOppoBounce = [0.175, 0.885, 0.32, 1.275] as [number, number, number, number];

const navItems = [
  { path: '/', label: '首页', icon: Home, id: 'home' },
  { path: '/filter-library', label: '预设', icon: Palette, id: 'presets' },
  { path: '/ai-demo', label: 'AI', icon: Sparkles, id: 'ai' },
  { path: '/floating-window', label: '悬浮窗', icon: Layers, id: 'floating' },
  { path: '/community', label: '我的', icon: User, id: 'profile' }
];

export default function BottomNavigationBar() {
  const location = useLocation();

  return (
    <motion.nav
      initial={{ y: 100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.4, ease: easeOppoEnter }}
      className="bottom-nav-bar"
      role="navigation"
      aria-label="底部导航"
    >
      <div className="h-full max-w-lg mx-auto px-4 flex items-center justify-around">
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          
          return (
            <Link
              key={item.id}
              to={item.path}
              className="relative flex flex-col items-center justify-center py-2 px-2 min-w-[72px] min-h-[56px] touch-feedback group"
              aria-label={item.label}
              aria-current={isActive ? 'page' : undefined}
            >
              {/* ============================================
                   水下光影效果 - ColorOS 16 金标规范
                   ============================================ */}
              {isActive && (
                <motion.div
                  layoutId="nav-indicator"
                  transition={{ type: 'spring', duration: 0.6, bounce: 0.25 }}
                  className="absolute inset-x-1 top-0 h-[3px] rounded-b-full bg-gradient-to-r from-oppo-orange to-hasselblad-orange shadow-oppo-glow-orange"
                />
              )}
              
              {/* ============================================
                   图标 - 动态缩放和颜色变化
                   ============================================ */}
              <motion.div
                animate={{
                  scale: isActive ? 1.15 : 1,
                  y: isActive ? -2 : 0
                }}
                transition={{ duration: 0.25, ease: easeOppoBounce }}
                className="relative z-10"
              >
                <item.icon
                  className={`w-6 h-6 transition-all duration-300 ${
                    isActive 
                      ? 'text-oppo-orange drop-shadow-lg' 
                      : 'text-text-tertiary group-hover:text-text-secondary'
                  }`}
                  strokeWidth={isActive ? 2.5 : 2}
                />
                
                {/* 选中时的光晕效果 */}
                {isActive && (
                  <motion.div
                    layoutId="nav-glow"
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 0.4, scale: 1.5 }}
                    transition={{ duration: 0.3, ease: easeOppoEnter }}
                    className="absolute inset-0 bg-oppo-orange rounded-full blur-md -z-10"
                  />
                )}
              </motion.div>
              
              {/* ============================================
                   标签 - ColorOS 16 规范
                   ============================================ */}
              <motion.span
                animate={{
                  scale: isActive ? 1 : 0.95,
                  color: isActive ? '#FF6B35' : '#757575'
                }}
                transition={{ duration: 0.25, ease: easeOppoEnter }}
                className={`mt-1 text-[11px] font-medium ${
                  isActive ? 'text-oppo-orange' : 'text-text-tertiary'
                }`}
              >
                {item.label}
              </motion.span>
            </Link>
          );
        })}
      </div>
    </motion.nav>
  );
}
