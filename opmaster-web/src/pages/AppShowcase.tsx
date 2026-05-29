import { motion } from 'framer-motion'
import { Link, useNavigate } from 'react-router-dom'
import { 
  Moon, Users, Mountain, Coffee, Wind, Sun, 
  Download, Play, Sparkles, Layers, Camera, 
  Heart, Star, ChevronRight, ArrowRight
} from 'lucide-react'
import { useAppStore } from '../store/useAppStore'
import { mockPresets } from '../data/mockPresets'

export default function AppShowcase() {
  const navigate = useNavigate()
  const { setSelectedPreset } = useAppStore()

  const showcasePresets = [
    { name: '德味预设', device: 'OPPO Find X7 Ultra', isHNCS: true, isNew: true, icon: Moon, id: 'hasselblad_dewei' },
    { name: '富士胶片', device: 'OPPO Find X7 Ultra', isHNCS: true, isNew: false, icon: Users, id: 'fujifilm_film' },
    { name: '胶片感', device: 'OPPO Find X7 Ultra', isHNCS: false, isNew: true, icon: Mountain, id: 'film_sense' },
    { name: '童话', device: 'OPPO Find X7 Ultra', isHNCS: false, isNew: false, icon: Coffee, id: 'fairy_tale' },
    { name: '高对比黑白', device: 'OPPO Find X7 Ultra', isHNCS: true, isNew: false, icon: Wind, id: 'high_contrast_bw' },
    { name: '富士NC', device: 'OPPO Find X7 Ultra', isHNCS: true, isNew: true, icon: Sun, id: 'fujifilm_nc' },
  ]

  const features = [
    {
      icon: Sparkles,
      title: '哈苏色彩',
      desc: '源自哈苏的HNCS自然色彩系统，让每一张照片都呈现真实质感',
      bgColor: 'bg-oppo-sunrise-gold/20',
      color: 'text-oppo-sunrise-gold'
    },
    {
      icon: Layers,
      title: '大师预设',
      desc: '专业摄影师精心调校的预设方案，一键提升照片品质',
      bgColor: 'bg-ocean-blue/20',
      color: 'text-ocean-blue'
    },
    {
      icon: Camera,
      title: '专业模式',
      desc: '完整的手动控制功能，满足专业摄影创作需求',
      bgColor: 'bg-hasselblad-pro/20',
      color: 'text-hasselblad-pro'
    }
  ]

  const handlePresetClick = (preset: any) => {
    const fullPreset = mockPresets.find(p => p.id === preset.id)
    if (fullPreset) {
      setSelectedPreset(fullPreset)
      navigate(`/preset/${preset.id}`)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      <section className="relative min-h-screen flex items-center justify-center overflow-hidden pt-20">
        <div className="absolute inset-0 overflow-hidden">
          <motion.div
            className="absolute w-[600px] h-[600px] rounded-full bg-oppo-sunrise-gold/20 blur-[128px]"
            animate={{
              x: [-100, 100, -100],
              y: [-50, 50, -50],
            }}
            transition={{
              duration: 20,
              repeat: Infinity,
              ease: "easeInOut"
            }}
          />
          <motion.div
            className="absolute w-[500px] h-[500px] rounded-full bg-ocean-blue/20 blur-[100px]"
            animate={{
              x: [100, -100, 100],
              y: [50, -50, 50],
            }}
            transition={{
              duration: 15,
              repeat: Infinity,
              ease: "easeInOut"
            }}
          />
        </div>

        <div className="relative z-10 max-w-7xl mx-auto px-4 py-20">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: [0.4, 0, 0.2, 1] }}
            className="text-center"
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.2 }}
              className="inline-flex items-center space-x-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 mb-8"
            >
              <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
              <span className="text-sm text-white/70">全新升级</span>
            </motion.div>

            <h1 className="text-4xl md:text-5xl lg:text-7xl font-bold mb-6">
              <span className="bg-gradient-to-r from-white via-white/80 to-white/60 bg-clip-text text-transparent">
                OMaster
              </span>
              <br />
              <span className="gradient-text-oppo">哈苏大师模式</span>
            </h1>

            <p className="text-lg md:text-xl text-text-secondary max-w-2xl mx-auto mb-12">
              释放你的创造力，用专业预设打造惊艳影像
            </p>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="flex flex-col sm:flex-row items-center justify-center gap-4"
            >
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="group flex items-center space-x-2 px-8 py-4 bg-oppo-sunrise-gold text-slate-900 font-semibold rounded-oppo-lg transition-all hover:shadow-lg hover:shadow-oppo-sunrise-gold/30"
              >
                <Download className="w-5 h-5" />
                <span>立即体验</span>
                <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="group flex items-center space-x-2 px-8 py-4 bg-white/5 text-white font-semibold rounded-oppo-lg border border-white/10 transition-all hover:bg-white/10"
              >
                <Play className="w-5 h-5" />
                <span>观看演示</span>
              </motion.button>
            </motion.div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.6, duration: 0.8 }}
            className="relative mt-20"
          >
            <div className="relative max-w-4xl mx-auto">
              <div className="absolute -inset-4 bg-gradient-to-r from-oppo-sunrise-gold/20 to-ocean-blue/20 rounded-3xl blur-2xl" />
              <div className="relative card-glass p-4 md:p-6 rounded-2xl border border-white/10">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 rounded-full bg-red-500" />
                    <div className="w-3 h-3 rounded-full bg-yellow-500" />
                    <div className="w-3 h-3 rounded-full bg-green-500" />
                  </div>
                  <span className="text-xs text-white/50">OMaster Preview</span>
                </div>
                
                <div className="relative aspect-[4/3] rounded-xl overflow-hidden bg-slate-800">
                  <img
                    src="https://cdn.fky.ltd/dw_01.webp"
                    alt="Preview"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                  <div className="absolute bottom-4 left-4 right-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-xs text-white/60 mb-1">德味预设</p>
                        <p className="text-sm font-medium text-white">OPPO Find X7 Ultra</p>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Heart className="w-5 h-5 text-white/80" />
                        <span className="text-xs text-white/60">1.2k</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <p className="text-oppo-sunrise-gold font-semibold mb-2 text-sm md:text-base tracking-wider uppercase">
              Features
            </p>
            <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-4">专业影像工具</h2>
            <p className="text-text-secondary max-w-2xl mx-auto">
              为专业摄影师打造的强大工具集
            </p>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-6">
            {features.map((feature, i) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -8, scale: 1.02 }}
                className="card-glass p-6 md:p-8 group cursor-pointer"
              >
                <motion.div
                  whileHover={{ scale: 1.1, rotate: 5 }}
                  className={`w-14 h-14 rounded-oppo-md ${feature.bgColor} flex items-center justify-center mb-6 transition-colors`}
                >
                  <feature.icon className={`w-7 h-7 ${feature.color}`} />
                </motion.div>
                <h3 className="text-xl md:text-2xl font-semibold mb-3">{feature.title}</h3>
                <p className="text-text-secondary leading-relaxed">{feature.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      <section className="py-20 px-4 bg-gradient-to-b from-transparent via-card-surface/30 to-transparent">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="text-oppo-sunrise-gold font-semibold mb-2 text-sm md:text-base tracking-wider uppercase"
            >
              Presets
            </motion.p>
            <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold">发现你的灵感</h2>
            <p className="text-text-secondary mt-4 max-w-2xl mx-auto">
              专业摄影师精心调校，覆盖多种拍摄场景
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {showcasePresets.map((preset, i) => {
              const realPreset = mockPresets.find(p => p.id === preset.id);
              return (
                <motion.div
                  key={preset.id}
                  initial={{ opacity: 0, scale: 0.9 }}
                  whileInView={{ opacity: 1, scale: 1 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  whileHover={{ y: -4 }}
                  onClick={() => handlePresetClick(preset)}
                  className="card-oppo overflow-hidden group cursor-pointer"
                >
                  <div className="relative h-48">
                    {realPreset?.coverPath ? (
                      <img
                        src={realPreset.coverPath}
                        alt={realPreset.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                      />
                    ) : (
                      <div className="w-full h-full bg-gradient-to-br from-oppo-sunrise-gold/15 to-ocean-blue/15 flex items-center justify-center">
                        <preset.icon className="w-20 h-20 text-oppo-sunrise-gold/20" />
                      </div>
                    )}
                    
                    <div className="absolute top-4 left-4 flex space-x-2">
                      {preset.isNew && <span className="tag-new">NEW</span>}
                      {preset.isHNCS && <span className="tag-hasselblad">HNCS</span>}
                    </div>

                    <motion.div
                      initial={{ opacity: 0 }}
                      whileHover={{ opacity: 1 }}
                      className="absolute bottom-4 right-4"
                    >
                      <div className="w-10 h-10 rounded-full bg-black/60 backdrop-blur-sm flex items-center justify-center">
                        <Heart className="w-5 h-5 text-white" />
                      </div>
                    </motion.div>

                    <div className="absolute inset-0 gradient-overlay" />
                  </div>

                  <div className="p-6">
                    <h3 className="text-lg md:text-xl font-semibold mb-2">{realPreset?.name || preset.name}</h3>
                    <div className="flex items-center justify-between">
                      <span className="tag-oppo">{realPreset?.deviceModel || ''}</span>
                      <div className="flex items-center space-x-1 text-text-tertiary">
                        <Star className="w-4 h-4 fill-oppo-sunrise-gold text-oppo-sunrise-gold" />
                        <span className="text-sm">4.9</span>
                      </div>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </div>

          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-center mt-12"
          >
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="group inline-flex items-center space-x-2 px-8 py-4 bg-white/5 text-white font-semibold rounded-oppo-lg border border-white/10 transition-all hover:bg-white/10"
            >
              <span>浏览全部预设</span>
              <ChevronRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
            </motion.button>
          </motion.div>
        </div>
      </section>

      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="card-glass p-8 md:p-12 rounded-2xl text-center">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
            >
              <h2 className="text-2xl md:text-3xl lg:text-4xl font-bold mb-4">
                开始你的创作之旅
              </h2>
              <p className="text-text-secondary max-w-xl mx-auto mb-8">
                加入全球数百万摄影师的行列，用OMaster释放你的创意潜力
              </p>
              
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="group inline-flex items-center space-x-2 px-8 py-4 bg-oppo-sunrise-gold text-slate-900 font-semibold rounded-oppo-lg transition-all hover:shadow-lg hover:shadow-oppo-sunrise-gold/30"
              >
                <Download className="w-5 h-5" />
                <span>免费下载</span>
                <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
              </motion.button>
            </motion.div>
          </div>
        </div>
      </section>

      <footer className="py-12 px-4 border-t border-white/5">
        <div className="max-w-7xl mx-auto">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex items-center space-x-2">
              <span className="text-xl font-bold gradient-text-oppo">OMaster</span>
              <span className="text-text-tertiary text-sm">v2.0</span>
            </div>
            <div className="flex items-center space-x-6 text-sm text-text-tertiary">
              <Link to="/privacy" className="hover:text-white transition-colors">隐私政策</Link>
              <Link to="/terms" className="hover:text-white transition-colors">服务条款</Link>
              <Link to="/about" className="hover:text-white transition-colors">关于我们</Link>
            </div>
            <p className="text-text-tertiary text-sm">
              &copy; 2024 OMaster. All rights reserved.
            </p>
          </div>
        </div>
      </footer>
    </div>
  )
}