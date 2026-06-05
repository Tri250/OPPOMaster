import { Link, useLocation } from "react-router-dom";
import { Camera, Menu, X } from "lucide-react";
import { useState } from "react";

const navItems = [
  { path: "/", label: "首页" },
  { path: "/presets", label: "预设库" },
  { path: "/scene-detection", label: "AI 场景检测" },
  { path: "/watermark", label: "水印编辑" },
  { path: "/camera-config", label: "相机配置" },
];

export default function Navbar() {
  const location = useLocation();
  const [open, setOpen] = useState(false);

  return (
    <header className="fixed top-0 inset-x-0 z-50 glass-strong border-b border-white/[0.06]">
      <nav className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2.5 group">
          <div className="w-9 h-9 rounded-xl bg-hasselblad-500 flex items-center justify-center group-hover:rotate-12 transition-transform">
            <Camera className="w-5 h-5 text-ink-900" strokeWidth={2.2} />
          </div>
          <div className="flex flex-col leading-none">
            <span className="font-display text-lg font-bold tracking-wide text-ink-50">
              OMaster
            </span>
            <span className="text-[10px] text-ink-300 tracking-[0.18em] uppercase mt-0.5">
              Hasselblad × OPPO
            </span>
          </div>
        </Link>

        {/* 桌面端菜单 */}
        <div className="hidden md:flex items-center gap-1">
          {navItems.map((item) => {
            const active = location.pathname === item.path ||
              (item.path !== "/" && location.pathname.startsWith(item.path));
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`relative px-4 py-2 text-sm font-medium transition-colors ${
                  active ? "text-hasselblad-400" : "text-ink-200 hover:text-ink-50"
                }`}
              >
                {item.label}
                {active && (
                  <span className="absolute -bottom-px left-1/2 -translate-x-1/2 w-6 h-0.5 bg-hasselblad-500 rounded-full" />
                )}
              </Link>
            );
          })}
        </div>

        <div className="hidden md:flex items-center gap-3">
          <span className="inline-flex items-center gap-1.5 text-xs text-ink-300">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            v3.0 · 哈苏认证
          </span>
        </div>

        {/* 移动端按钮 */}
        <button
          onClick={() => setOpen(!open)}
          className="md:hidden p-2 rounded-lg hover:bg-white/[0.05]"
          aria-label="切换菜单"
        >
          {open ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </button>
      </nav>

      {/* 移动端菜单 */}
      {open && (
        <div className="md:hidden glass-strong border-t border-white/[0.06]">
          <div className="px-6 py-4 flex flex-col gap-1">
            {navItems.map((item) => {
              const active = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setOpen(false)}
                  className={`px-4 py-3 rounded-lg text-sm font-medium transition-colors ${
                    active
                      ? "bg-hasselblad-500/15 text-hasselblad-400"
                      : "text-ink-200 hover:bg-white/[0.04]"
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </header>
  );
}
