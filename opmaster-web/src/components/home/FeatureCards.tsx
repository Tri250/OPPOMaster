import { motion } from 'framer-motion';
import { Camera, Sparkles, Search, Zap, Upload, Palette, Droplets, Sparkles as PenTool, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

interface Feature {
  id: number;
  title: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  href: string;
  color: string;
}

const baseFeatures: Feature[] = [
  { id: 1, title: 'AI场景识别', description: '智能识别20+拍摄场景，自动优化参数', icon: Sparkles, href: '/ai-demo', color: 'from-oppo-coral to-oppo-coralLight' },
  { id: 2, title: '原生相机参数', description: '一键填入哈苏官方认证的最佳参数', icon: Camera, href: '/tech', color: 'from-hasselblad to-hasselblad-light' },
  { id: 3, title: '悬浮窗预览', description: '全局悬浮窗，实时预览滤镜效果', icon: PenTool, href: '/tech', color: 'from-oppo-green to-oppo-greenLight' },
  { id: 4, title: '预设搜索', description: '智能搜索，快速找到所需滤镜', icon: Search, href: '/home', color: 'from-status-info to-blue-400' },
];

const advancedFeatures: Feature[] = [
  { id: 5, title: '预设社区', description: '百万级预设库，分享与下载', icon: Zap, href: '/home', color: 'from-purple-500 to-pink-500' },
  { id: 6, title: '导入导出', description: '支持LUT、XMP等多种格式', icon: Upload, href: '/editor', color: 'from-indigo-500 to-blue-500' },
  { id: 7, title: '水印生成器', description: '专业哈苏风格水印制作', icon: Droplets, href: '/watermark', color: 'from-cyan-500 to-teal-500' },
  { id: 8, title: '预设编辑器', description: '全参数自定义编辑', icon: Palette, href: '/editor', color: 'from-orange-500 to-red-500' },
];

interface FeatureCardProps {
  feature: Feature;
  index: number;
}

const FeatureCard = ({ feature, index }: FeatureCardProps) => {
  return (
    <Link to={feature.href}>
      <motion.div
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: '-100px' }}
        transition={{ duration: 0.6, delay: index * 0.1, type: 'spring', stiffness: 120 }}
        whileHover={{ y: -8, scale: 1.02 }}
        whileTap={{ scale: 0.97 }}
        className="glass-card glass-card-hover relative overflow-hidden group ripple"
      >
        {/* 渐变光边 */}
        <div className={`absolute inset-0 bg-gradient-to-br ${feature.color} opacity-0 group-hover:opacity-8 transition-opacity duration-500 pointer-events-none`} />
        
        {/* 图标背景 */}
        <div className="p-6">
          <motion.div
            whileHover={{ scale: 1.05, rotate: 2 }}
            className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${feature.color} flex items-center justify-center mb-4 shadow-lg group-hover:shadow-xl transition-all duration-300`}
          >
            <feature.icon className="w-7 h-7 text-white" />
          </motion.div>
          
          {/* 文字内容 */}
          <h3 className="text-display-sm font-medium text-white mb-2">
            {feature.title}
          </h3>
          <p className="text-body-md text-neutral-60 mb-4">
            {feature.description}
          </p>
          
          {/* 操作按钮 */}
          <div className="flex items-center gap-2 text-hasselblad group-hover:text-oppo-coral transition-colors">
            <span className="text-body-md font-medium">了解更多</span>
            <motion.div
              initial={{ x: 0 }}
              whileHover={{ x: 6 }}
              transition={{ type: 'spring', stiffness: 400 }}
            >
              <ArrowRight className="w-4 h-4" />
            </motion.div>
          </div>
        </div>
      </motion.div>
    </Link>
  );
};

interface FeatureSectionProps {
  title: string;
  subtitle: string;
  features: Feature[];
  showViewAll?: boolean;
}

const FeatureSection = ({ title, subtitle, features, showViewAll = true }: FeatureSectionProps) => {
  return (
    <div className="mb-16">
      {/* 标题区域 */}
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between mb-8 gap-4">
        <div>
          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-display-lg font-semibold text-white mb-2"
          >
            {title}
          </motion.h2>
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="text-body-md text-neutral-70"
          >
            {subtitle}
          </motion.p>
        </div>
        
        {showViewAll && (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.2 }}
          >
            <Link
              to="/home"
              className="flex items-center gap-2 text-hasselblad hover:text-oppo-coral transition-colors"
            >
              <span className="font-medium">查看全部</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </motion.div>
        )}
      </div>
      
      {/* 卡片网格 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
        {features.map((feature, index) => (
          <FeatureCard key={feature.id} feature={feature} index={index} />
        ))}
      </div>
    </div>
  );
};

export default function FeatureCards() {
  return (
    <section className="px-4 sm:px-6 py-12">
      <div className="max-w-6xl mx-auto">
        {/* 基础功能区 */}
        <FeatureSection
          title="核心功能"
          subtitle="哈苏认证技术，让每一张照片都成为佳作"
          features={baseFeatures}
        />
        
        {/* 高级功能区 */}
        <FeatureSection
          title="专业工具"
          subtitle="为专业摄影师打造的强大功能集"
          features={advancedFeatures}
          showViewAll={false}
        />
      </div>
    </section>
  );
}
