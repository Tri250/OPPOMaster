import { Link, useLocation } from 'react-router-dom';
import { Camera, Home, Sparkles, Cpu, User } from 'lucide-react';

const navItems = [
  { path: '/', label: '首页', icon: Home },
  { path: '/ai-demo', label: 'AI场景', icon: Sparkles },
  { path: '/tech', label: '影像工具', icon: Cpu },
  { path: '/about', label: '关于', icon: User },
];

export default function Navbar() {
  const location = useLocation();

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-deep-space/90 backdrop-blur-xl border-b border-border-subtle">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link to="/" className="flex items-center gap-2">
            <div className="w-10 h-10 bg-gradient-to-br from-oppo-orange to-hasselblad rounded-xl flex items-center justify-center">
              <Camera className="w-6 h-6 text-deep-space" />
            </div>
            <span className="text-lg font-bold text-text-primary">
              OPPOMaster
            </span>
          </Link>

          <div className="hidden md:flex items-center gap-1">
            {navItems.map((item) => {
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-oppo-orange/10 text-oppo-orange'
                      : 'text-text-secondary hover:text-text-primary hover:bg-surface-hover'
                  }`}
                >
                  <item.icon className="w-4 h-4" />
                  {item.label}
                </Link>
              );
            })}
          </div>

          <div className="md:hidden flex items-center gap-2">
            {navItems.map((item) => {
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center justify-center w-10 h-10 rounded-xl transition-all ${
                    isActive
                      ? 'bg-oppo-orange/10 text-oppo-orange'
                      : 'text-text-secondary hover:text-text-primary hover:bg-surface-hover'
                  }`}
                >
                  <item.icon className="w-5 h-5" />
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </nav>
  );
}
