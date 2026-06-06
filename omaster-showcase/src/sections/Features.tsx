import { motion } from 'framer-motion'
import { useInView } from 'framer-motion'
import { useRef } from 'react'
import { 
  Palette, 
  Cloud, 
  Layers, 
  Star, 
  Smartphone, 
  Sparkles 
} from 'lucide-react'

const features = [
  {
    icon: Palette,
    title: '丰富的预设库',
    description: '23+ 款专业预设，涵盖胶片、复古、清新、黑白、美食等多种风格，满足不同拍摄场景需求。',
    color: '#FF6B35'
  },
  {
    icon: Cloud,
    title: '配置云更新',
    description: '支持从云端获取最新配置，支持自定义更新源，随时获取社区分享的优质预设。',
    color: '#58A6FF'
  },
  {
    icon: Layers,
    title: '悬浮窗模式',
    description: '拍照时可悬浮显示参数，支持左右滑动切换预设，半透明设计不遮挡取景。',
    color: '#A371F7'
  },
  {
    icon: Star,
    title: '收藏管理',
    description: '一键收藏喜欢的预设，快速访问常用参数，本地存储无需网络，随时可用。',
    color: '#FFD700'
  },
  {
    icon: Smartphone,
    title: '全平台支持',
    description: '支持创建自定义预设，支持各大主流平台专业相机，OPPO、OnePlus、realme全覆盖。',
    color: '#3FB950'
  },
  {
    icon: Sparkles,
    title: '简洁优雅的界面',
    description: '纯黑背景 + 各大摄影品牌配色，流畅的动画过渡，瀑布流卡片布局，极致视觉体验。',
    color: '#F778BA'
  }
]

function FeatureCard({ feature, index }: { feature: typeof features[0]; index: number }) {
  const ref = useRef(null)
  const isInView = useInView(ref, { once: true, margin: "-100px" })
  const Icon = feature.icon

  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 50 }}
      animate={isInView ? { opacity: 1, y: 0 } : {}}
      transition={{ duration: 0.6, delay: index * 0.1 }}
      whileHover={{ y: -8, transition: { duration: 0.3 } }}
      className="group relative bg-[#161B22] rounded-2xl p-6 border border-[#30363D] hover:border-[#FF6B35]/50 transition-all duration-300"
    >
      {/* Glow effect on hover */}
      <div 
        className="absolute inset-0 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-300 -z-10 blur-xl"
        style={{ backgroundColor: `${feature.color}10` }}
      />
      
      {/* Icon */}
      <div 
        className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 transition-transform duration-300 group-hover:scale-110"
        style={{ backgroundColor: `${feature.color}15` }}
      >
        <Icon size={28} style={{ color: feature.color }} />
      </div>

      {/* Content */}
      <h3 className="text-xl font-bold text-white mb-3 group-hover:text-[#FF6B35] transition-colors">
        {feature.title}
      </h3>
      <p className="text-gray-400 leading-relaxed">
        {feature.description}
      </p>
    </motion.div>
  )
}

export default function Features() {
  const sectionRef = useRef(null)
  const isInView = useInView(sectionRef, { once: true, margin: "-100px" })

  return (
    <section ref={sectionRef} className="py-24 bg-[#0D1117] relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-[#30363D] to-transparent" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={isInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <span className="inline-block px-4 py-1.5 rounded-full bg-[#FF6B35]/10 text-[#FF6B35] text-sm font-medium mb-4">
            核心功能
          </span>
          <h2 className="text-4xl sm:text-5xl font-bold text-white mb-4">
            强大的功能，<span className="text-[#FF6B35]">简单的操作</span>
          </h2>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            为摄影爱好者打造的专业工具，让每一次拍摄都能获得完美的色彩
          </p>
        </motion.div>

        {/* Features grid */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <FeatureCard key={feature.title} feature={feature} index={index} />
          ))}
        </div>
      </div>
    </section>
  )
}
