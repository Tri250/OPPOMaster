import HeroSection from '../components/home/HeroSection';
import FeatureCards from '../components/home/FeatureCards';
import PresetGrid from '../components/home/PresetGrid';
import { motion } from 'framer-motion';

export default function HomePage() {
  return (
    <div className="min-h-screen">
      <HeroSection />
      
      {/* 渐变分隔线 */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className="h-px bg-gradient-to-r from-transparent via-hasselblad/30 to-transparent mx-auto max-w-3xl"
      />
      
      <FeatureCards />
      
      <PresetGrid />
      
      {/* 底部信息栏 */}
      <footer className="mt-20 py-12 px-4 border-t border-white/10">
        <div className="max-w-6xl mx-auto text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <span className="text-neutral-50 font-bold text-lg">OPPO Master</span>
            <span className="text-hasselblad text-sm">v1.5.0</span>
          </div>
          
          <p className="text-caption text-neutral-50 mb-6">
            由 OPPO 与 Hasselblad 联合打造 · 专业影像预设管理系统
          </p>
          
          <div className="flex flex-wrap justify-center gap-8">
            <a href="#" className="text-body-md text-neutral-60 hover:text-hasselblad transition-colors">
              隐私政策
            </a>
            <a href="#" className="text-body-md text-neutral-60 hover:text-hasselblad transition-colors">
              用户协议
            </a>
            <a href="#" className="text-body-md text-neutral-60 hover:text-hasselblad transition-colors">
              帮助中心
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
