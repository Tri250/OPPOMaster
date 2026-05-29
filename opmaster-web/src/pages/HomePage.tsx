import { motion } from 'framer-motion';
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import HeroSection from '../components/home/HeroSection';
import PresetGrid from '../components/home/PresetGrid';
import AIDemoBanner from '../components/home/AIDemoBanner';

export default function HomePage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-oppo-black text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-oppo-black/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-14">
        <HeroSection />
        <PresetGrid />
        <AIDemoBanner />
      </main>
    </div>
  );
}
