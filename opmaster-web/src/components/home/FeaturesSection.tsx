import { motion } from 'framer-motion';
import { useAppStore } from '../../store/useAppStore';
import { appFeatures, watermarkTemplates, securityFeatures, buildFeatures } from '../../data/mockPresets';
import { Smartphone, ShieldCheck, Sparkles, Code2, ChevronRight, Eye, Palette, Lock, Zap, Layers, ArrowUpRight } from 'lucide-react';

export default function FeaturesSection() {
  const { 
    activeFeatureTab, 
    setActiveFeatureTab, 
    selectedWatermarkTemplate, 
    setSelectedWatermarkTemplate,
    showFloatingWindowDemo,
    setShowFloatingWindowDemo,
    floatingWindowOpacity,
    setFloatingWindowOpacity,
    showSecurityDemo,
    setShowSecurityDemo,
    showBuildDemo,
    setShowBuildDemo
  } = useAppStore();

  const tabs = [
    { id: 'core', label: '核心功能', icon: Smartphone },
    { id: 'security', label: '安全隐私', icon: ShieldCheck },
    { id: 'ux', label: '交互体验', icon: Sparkles },
    { id: 'build', label: '构建发布', icon: Code2 }
  ];

  const filteredFeatures = appFeatures.filter(f => f.category === activeFeatureTab);

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1
      }
    }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 }
  };

  return (
    <section className="py-24 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <h2 className="text-4xl md:text-5xl font-bold mb-4">
            <span className="gradient-text">全面功能</span>展示
          </h2>
          <p className="text-white/60 text-lg max-w-2xl mx-auto">
            OPPOMaster 提供专业级别的调色功能、安全的隐私保护、流畅的用户体验
          </p>
        </motion.div>

        {/* Tabs */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="flex justify-center mb-12"
        >
          <div className="glass-effect rounded-2xl p-2 flex gap-2">
            {tabs.map((tab, idx) => (
              <button
                key={tab.id}
                onClick={() => setActiveFeatureTab(tab.id as any)}
                className={`px-6 py-3 rounded-xl transition-all flex items-center gap-2 ${
                  activeFeatureTab === tab.id
                    ? 'bg-gradient-to-r from-hasselblad to-hasselblad/60 text-deep-space font-medium'
                    : 'text-white/60 hover:text-white hover:bg-white/5'
                }`}
              >
                <tab.icon className="w-4.5 h-4.5" />
                {tab.label}
              </button>
            ))}
          </div>
        </motion.div>

        {/* Features Grid */}
        <motion.div
          variants={containerVariants}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
        >
          {filteredFeatures.map((feature, idx) => (
            <motion.div
              key={feature.id}
              variants={itemVariants}
              whileHover={{ y: -8, scale: 1.02 }}
              className="glass-effect rounded-3xl p-6 border border-white/5 hover:border-hasselblad/30 transition-all cursor-pointer"
              onClick={() => {
                // 根据功能类型触发对应的演示
                if (feature.id === 'floating-window') setShowFloatingWindowDemo(!showFloatingWindowDemo);
                if (feature.id.startsWith('encrypted') || feature.id.startsWith('https')) setShowSecurityDemo(!showSecurityDemo);
                if (feature.id.startsWith('dependency') || feature.id.startsWith('code')) setShowBuildDemo(!showBuildDemo);
              }}
            >
              <div className="text-4xl mb-4">{feature.icon}</div>
              <h3 className="text-xl font-bold mb-2">{feature.title}</h3>
              <p className="text-white/50 mb-4">{feature.description}</p>
              {feature.demoAvailable && (
                <div className="flex items-center text-hasselblad text-sm font-medium">
                  <span>体验演示</span>
                  <ChevronRight className="w-4 h-4 ml-1" />
                </div>
              )}
            </motion.div>
          ))}
        </motion.div>

        {/* Interactive Demos Area */}
        {(showFloatingWindowDemo || showSecurityDemo || showBuildDemo) && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="mt-12 overflow-hidden"
          >
            {showFloatingWindowDemo && <FloatingWindowDemo opacity={floatingWindowOpacity} onOpacityChange={setFloatingWindowOpacity} />}
            {showSecurityDemo && <SecurityDemo />}
            {showBuildDemo && <BuildDemo />}
          </motion.div>
        )}

        {/* Watermark Templates Showcase */}
        {activeFeatureTab === 'core' && (
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.3 }}
            className="mt-24"
          >
            <h3 className="text-2xl font-bold mb-8 text-center">
              <span className="text-hasselblad">10种</span>水印模板
            </h3>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
              {watermarkTemplates.map((template, idx) => (
                <motion.div
                  key={template.id}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: idx * 0.1 }}
                  whileHover={{ scale: 1.05 }}
                  onClick={() => setSelectedWatermarkTemplate(template.id === selectedWatermarkTemplate ? null : template.id)}
                  className={`glass-effect rounded-2xl p-4 cursor-pointer transition-all ${
                    selectedWatermarkTemplate === template.id
                      ? 'border-2 border-hasselblad shadow-lg shadow-hasselblad/20'
                      : 'border border-white/5 hover:border-white/15'
                  }`}
                >
                  <div className="text-2xl mb-2">
                    {template.id === 'hasselblad' && '📷'}
                    {template.id === 'oppo' && '🟠'}
                    {template.id === 'oneplus' && '🔴'}
                    {template.id === 'realme' && '🟡'}
                    {template.id === 'minimal' && '📊'}
                    {template.id === 'timestamp' && '⏰'}
                    {template.id === 'location' && '📍'}
                    {template.id === 'custom' && '🎨'}
                    {template.id === 'brand-simple' && '🏷️'}
                    {template.id === 'film-style' && '🎞️'}
                  </div>
                  <h4 className="font-medium mb-1">{template.name}</h4>
                  <p className="text-xs text-white/40">{template.description}</p>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}
      </div>
    </section>
  );
}

function FloatingWindowDemo({ opacity, onOpacityChange }: { opacity: number; onOpacityChange: (val: number) => void }) {
  return (
    <div className="glass-effect rounded-3xl p-8 border border-hasselblad/20">
      <div className="flex items-center gap-3 mb-6">
        <Smartphone className="w-6 h-6 text-hasselblad" />
        <h4 className="text-xl font-bold">悬浮窗演示</h4>
      </div>
      <div className="grid md:grid-cols-2 gap-8">
        <div className="relative bg-deep-space/50 rounded-2xl p-6 min-h-[300px] border border-white/10">
          {/* Mock Floating Window */}
          <motion.div
            className="absolute right-4 top-4 bg-deep-space border border-hasselblad/30 rounded-2xl p-4 shadow-2xl"
            style={{ opacity: opacity / 100 }}
            animate={{ x: [0, 4, 0], y: [0, -4, 0] }}
            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
          >
            <div className="text-xs text-white/60 mb-2">实时相机参数</div>
            <div className="space-y-1">
              <div className="flex justify-between text-sm">
                <span className="text-white/50">ISO</span>
                <span className="text-hasselblad font-mono">200</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-white/50">快门</span>
                <span className="text-hasselblad font-mono">1/250s</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-white/50">EV</span>
                <span className="text-hasselblad font-mono">+0.3</span>
              </div>
            </div>
            <div className="flex gap-2 mt-3">
              <div className="w-2 h-2 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-xs text-oppo-green">监控中</span>
            </div>
          </motion.div>
          <div className="text-white/30 text-sm pt-20">
            悬浮窗可自由拖拽、调整透明度、收起为悬浮球
          </div>
        </div>
        <div className="flex flex-col justify-center">
          <h5 className="font-bold mb-4">透明度调节</h5>
          <input
            type="range"
            min="30"
            max="100"
            value={opacity}
            onChange={(e) => onOpacityChange(parseInt(e.target.value))}
            className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-hasselblad"
          />
          <div className="flex justify-between text-sm text-white/40 mt-2">
            <span>30%</span>
            <span className="text-hasselblad font-medium">{opacity}%</span>
            <span>100%</span>
          </div>
          <div className="mt-6 space-y-2">
            <div className="flex items-center gap-2 text-sm text-white/60">
              <div className="w-2 h-2 bg-hasselblad rounded-full" />
              支持悬浮球收起展开
            </div>
            <div className="flex items-center gap-2 text-sm text-white/60">
              <div className="w-2 h-2 bg-oppo-green rounded-full" />
              边缘自动吸附
            </div>
            <div className="flex items-center gap-2 text-sm text-white/60">
              <div className="w-2 h-2 bg-purple-400 rounded-full" />
              左右滑动切换预设
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function SecurityDemo() {
  return (
    <div className="glass-effect rounded-3xl p-8 border border-blue-500/20">
      <div className="flex items-center gap-3 mb-6">
        <ShieldCheck className="w-6 h-6 text-blue-400" />
        <h4 className="text-xl font-bold">安全隐私特性</h4>
      </div>
      <div className="grid md:grid-cols-2 gap-6">
        {securityFeatures.map((group, idx) => (
          <div key={idx} className="bg-deep-space/30 rounded-xl p-4 border border-white/5">
            <h5 className="font-bold text-white/80 mb-3">{group.title}</h5>
            <ul className="space-y-2">
              {group.items.map((item, itemIdx) => (
                <li key={itemIdx} className="flex items-center gap-2 text-sm text-white/60">
                  <div className="w-1.5 h-1.5 bg-blue-400/60 rounded-full" />
                  {item}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="mt-6 flex items-center gap-2 text-xs text-white/40">
        <Lock className="w-4 h-4" />
        <span>所有数据本地存储，不上传服务器</span>
      </div>
    </div>
  );
}

function BuildDemo() {
  return (
    <div className="glass-effect rounded-3xl p-8 border border-green-500/20">
      <div className="flex items-center gap-3 mb-6">
        <Code2 className="w-6 h-6 text-green-400" />
        <h4 className="text-xl font-bold">构建与发布</h4>
      </div>
      <div className="grid md:grid-cols-3 gap-6">
        {buildFeatures.map((group, idx) => (
          <div key={idx} className="bg-deep-space/30 rounded-xl p-4 border border-white/5">
            <h5 className="font-bold text-white/80 mb-3">{group.title}</h5>
            <ul className="space-y-2">
              {group.items.map((item, itemIdx) => (
                <li key={itemIdx} className="flex items-center gap-2 text-sm text-white/60">
                  <div className="w-1.5 h-1.5 bg-green-400/60 rounded-full" />
                  {item}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: '版本', value: 'v1.2.1' },
          { label: 'Gradle', value: '8.7' },
          { label: 'Android', value: '14' },
          { label: '打包', value: 'Release' }
        ].map((stat, idx) => (
          <div key={idx} className="bg-deep-space/30 rounded-xl p-3 text-center border border-white/5">
            <div className="text-green-400 font-mono font-bold">{stat.value}</div>
            <div className="text-xs text-white/40">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
