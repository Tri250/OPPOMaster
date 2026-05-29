import { motion } from 'framer-motion';
import { Link, useLocation } from 'react-router-dom';
import { 
  Home,
  Palette,
  Sparkles,
  Layers,
  Users,
  Settings,
  Camera,
  Award,
  Scan,
  Filter,
  Edit3,
  UploadCloud,
  Smartphone,
  Star,
} from 'lucide-react';
import { featureEntries } from '../../data/featureEntries';

interface NavItem {
  id: string;
  label: string;
  icon: any;
  path: string;
}

const mainNavItems: NavItem[] = [
  { id: 'home', label: '首页', icon: Home, path: '/' },
  { id: 'photography', label: '摄影', icon: Camera, path: '/filter-library' },
  { id: 'ai', label: 'AI智能', icon: Sparkles, path: '/ai-demo' },
  { id: 'tools', label: '工具', icon: Layers, path: '/floating-window' },
  { id: 'community', label: '社区', icon: Users, path: '/community' },
  { id: 'settings', label: '设置', icon: Settings, path: '/settings' },
];

export default function SidebarNav() {
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(path);
  };

  return (
    <motion.aside
      initial={{ x: -100, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      className="hidden lg:flex flex-col w-64 h-screen fixed left-0 top-0 z-40 
        bg-bg-primary border-r border-white/5"
    >
      {/* Logo区域 */}
      <div className="p-6 border-b border-white/5">
        <Link to="/" className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange 
            flex items-center justify-center shadow-oppo-elevation-2">
            <Camera className="w-5 h-5 text-oppo-black" />
          </div>
          <span className="text-h2 font-bold gradient-text-oppo">小O帮帮</span>
        </Link>
      </div>

      {/* 主导航 */}
      <nav className="flex-1 py-6 px-4 overflow-y-auto">
        <div className="mb-6">
          <p className="text-caption text-text-tertiary font-semibold px-3 mb-2">
            主要导航
          </p>
          <div className="space-y-1">
            {mainNavItems.map((item) => (
              <Link
                key={item.id}
                to={item.path}
                className={`
                  flex items-center gap-3 px-3 py-3 rounded-xl
                  transition-all duration-200
                  ${isActive(item.path)
                    ? 'bg-oppo-orange/10 text-oppo-orange'
                    : 'text-text-secondary hover:text-text-primary hover:bg-white/5'
                  }
                `}
              >
                <item.icon className="w-5 h-5" />
                <span className="font-medium text-body2">{item.label}</span>
              </Link>
            ))}
          </div>
        </div>

        {/* 快速入口 */}
        <div>
          <p className="text-caption text-text-tertiary font-semibold px-3 mb-2">
            快速入口
          </p>
          <div className="space-y-1">
            {featureEntries.filter(f => f.isFeatured).map((feature) => {
              const Icon = feature.icon;
              return (
                <Link
                  key={feature.id}
                  to={feature.path}
                  className={`
                    flex items-center gap-3 px-3 py-3 rounded-xl
                    transition-all duration-200
                    ${location.pathname === feature.path
                      ? 'bg-oppo-orange/10 text-oppo-orange'
                      : 'text-text-secondary hover:text-text-primary hover:bg-white/5'
                    }
                  `}
                >
                  <Icon className="w-5 h-5" />
                  <span className="font-medium text-body2">{feature.title}</span>
                </Link>
              );
            })}
          </div>
        </div>
      </nav>

      {/* 底部信息 */}
      <div className="p-4 border-t border-white/5">
        <div className="card-glass rounded-xl p-4">
          <p className="text-body2 text-text-secondary">
            ColorOS 16 • 哈苏影像
          </p>
          <p className="text-micro text-text-tertiary mt-1">
            专业摄影，极致体验
          </p>
        </div>
      </div>
    </motion.aside>
  );
}
