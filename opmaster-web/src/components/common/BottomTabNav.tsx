import { motion } from 'framer-motion';
import { Link, useLocation } from 'react-router-dom';
import { Home, Camera, Sparkles, Layers, Settings } from 'lucide-react';

interface TabItem {
  id: string;
  label: string;
  icon: any;
  path: string;
}

const tabItems: TabItem[] = [
  { id: 'home', label: '首页', icon: Home, path: '/' },
  { id: 'photography', label: '摄影', icon: Camera, path: '/filter-library' },
  { id: 'ai', label: 'AI', icon: Sparkles, path: '/ai-demo' },
  { id: 'tools', label: '工具', icon: Layers, path: '/floating-window' },
  { id: 'settings', label: '设置', icon: Settings, path: '/settings' },
];

export default function BottomTabNav() {
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(path);
  };

  return (
    <motion.nav
      initial={{ y: 100, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="lg:hidden fixed bottom-0 left-0 right-0 z-50 
        bg-bg-primary/85 backdrop-blur-2xl border-t border-white/5
        safe-area-bottom"
    >
      <div className="flex items-center justify-around h-16 max-w-md mx-auto">
        {tabItems.map((item, index) => {
          const active = isActive(item.path);
          return (
            <Link
              key={item.id}
              to={item.path}
              className="flex flex-col items-center gap-1 px-2 py-2 min-w-0
                touch-feedback-strong"
            >
              <motion.div
                animate={{
                  scale: active ? 1.15 : 1,
                }}
                transition={{ type: 'spring', stiffness: 400, damping: 20 }}
                className={`
                  w-9 h-9 rounded-xl flex items-center justify-center
                  transition-all duration-300
                  ${active
                    ? 'bg-gradient-to-br from-oppo-orange/20 to-hasselblad-orange/20'
                    : ''
                  }
                `}
              >
                <item.icon
                  className={`w-5.5 h-5.5 transition-colors duration-200
                    ${active ? 'text-oppo-orange' : 'text-text-tertiary'
                  }`}
                />
              </motion.div>
              <span className={`
                text-caption font-semibold transition-colors duration-200
                ${active ? 'text-oppo-orange' : 'text-text-tertiary'}
              `}>
                {item.label}
              </span>
              
              {/* 选中指示器 */}
              {active && (
                <motion.div
                  layoutId="activeTabIndicator"
                  className="absolute -top-3 w-1.5 h-1.5 bg-oppo-orange rounded-full"
                />
              )}
            </Link>
          );
        })}
      </div>
    </motion.nav>
  );
}
