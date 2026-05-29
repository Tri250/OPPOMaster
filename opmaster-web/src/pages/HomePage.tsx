import { motion } from 'framer-motion';
import { Camera, Sparkles, Zap, Star } from 'lucide-react';
import { useState } from 'react';
import SidebarNav from '../components/common/SidebarNav';
import TopNavBar from '../components/common/TopNavBar';
import BottomTabNav from '../components/common/BottomTabNav';
import FeatureEntryCard from '../components/common/FeatureEntryCard';
import { featureEntries, categories } from '../data/featureEntries';

export default function HomePage() {
  const [activeCategory, setActiveCategory] = useState('all');

  const filteredFeatures = activeCategory === 'all'
    ? featureEntries
    : featureEntries.filter(f => f.category === activeCategory);

  const featuredFeatures = featureEntries.filter(f => f.isFeatured);

  return (
    <div className="min-h-screen bg-bg-primary text-text-primary overflow-x-hidden">
      {/* 背景光效 */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <motion.div
          animate={{ x: [0, 80, 0], y: [0, 40, 0] }}
          transition={{ duration: 25, repeat: Infinity, ease: 'easeInOut' }}
          className="absolute -top-52 -left-52 w-[500px] h-[500px] orb-oppo orb-orange"
        />
        <motion.div
          animate={{ x: [0, -60, 0], y: [0, -50, 0] }}
          transition={{ duration: 30, repeat: Infinity, ease: 'easeInOut' }}
          className="absolute -bottom-52 -right-52 w-[450px] h-[450px] orb-oppo orb-blue"
        />
      </div>

      {/* 导航组件 */}
      <SidebarNav />
      <TopNavBar />

      {/* 主内容区域 */}
      <main className="relative pt-14 pb-24 lg:pb-12 lg:pl-64 z-10">
        <div className="max-w-7xl mx-auto px-4 py-8 lg:px-8 lg:py-12">
          
          {/* Hero 区域 */}
          <section className="mb-12 lg:mb-16">
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.7, ease: [0.05, 0.7, 0.1, 1.0] }}
              className="text-center lg:text-left"
            >
              {/* 品牌标签 */}
              <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ delay: 0.2, type: 'spring', stiffness: 200, damping: 15 }}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-full glass-effect mb-6"
              >
                <span className="relative flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-oppo-green opacity-75" />
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-oppo-green" />
                </span>
                <span className="text-caption text-text-secondary font-semibold">
                  ColorOS 16 • 哈苏影像系统
                </span>
              </motion.div>

              {/* 主标题 */}
              <motion.h1
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3, duration: 0.6 }}
                className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4 leading-tight"
              >
                <span className="gradient-text-oppo">小O帮帮</span>
                <br />
                <span className="text-text-primary">让每一张照片都成为大片</span>
              </motion.h1>

              {/* 副标题 */}
              <motion.p
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.45, duration: 0.6 }}
                className="text-body1 text-text-secondary max-w-2xl mx-auto lg:mx-0 mb-8 leading-relaxed"
              >
                专业摄影师精心调校，AI智能场景识别，哈苏影像质感
                <br className="hidden sm:block" />
                让你的OPPO手机发挥全部摄影潜力
              </motion.p>

              {/* 统计数据 */}
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.6, duration: 0.5 }}
                className="flex items-center justify-center lg:justify-start gap-8"
              >
                <div className="text-center">
                  <p className="text-number-lg font-bold gradient-text-oppo">666</p>
                  <p className="text-caption text-text-tertiary">场景识别</p>
                </div>
                <div className="text-center">
                  <p className="text-number-lg font-bold gradient-text-oppo">50+</p>
                  <p className="text-caption text-text-tertiary">预设参数</p>
                </div>
                <div className="text-center">
                  <p className="text-number-lg font-bold gradient-text-oppo">&lt;350ms</p>
                  <p className="text-caption text-text-tertiary">极速响应</p>
                </div>
              </motion.div>
            </motion.div>
          </section>

          {/* 精选功能区域 */}
          <section className="mb-12 lg:mb-16">
            <div className="flex items-end justify-between mb-6">
              <div>
                <h2 className="text-h1 font-bold mb-1.5 flex items-center gap-2">
                  <Star className="w-6 h-6 text-hasselblad-orange fill-hasselblad-orange" />
                  精选功能
                </h2>
                <p className="text-body2 text-text-secondary">
                  最受欢迎的功能，快速上手
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-5">
              {featuredFeatures.map((feature, index) => (
                <FeatureEntryCard
                  key={feature.id}
                  feature={feature}
                  index={index}
                  size="large"
                />
              ))}
            </div>
          </section>

          {/* 分类筛选 */}
          <section className="mb-6">
            <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-hide -mx-4 px-4 lg:mx-0 lg:px-0">
              {categories.map((category) => (
                <button
                  key={category.id}
                  onClick={() => setActiveCategory(category.id)}
                  className={`
                    flex-shrink-0 px-4 py-2.5 rounded-full text-body2 font-medium
                    transition-all duration-300 ease-out-elastic
                    ${activeCategory === category.id
                      ? 'bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-oppo-black shadow-oppo-elevation-2'
                      : 'bg-bg-secondary text-text-secondary hover:text-text-primary hover:bg-bg-tertiary'
                    }
                  `}
                >
                  {category.label}
                </button>
              ))}
            </div>
          </section>

          {/* 所有功能网格 */}
          <section>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5 lg:gap-4">
              {filteredFeatures.map((feature, index) => (
                <FeatureEntryCard
                  key={feature.id}
                  feature={feature}
                  index={index}
                />
              ))}
            </div>
          </section>
        </div>
      </main>

      {/* 底部导航 */}
      <BottomTabNav />
    </div>
  );
}
