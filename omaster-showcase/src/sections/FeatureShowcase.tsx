import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Palette, 
  Cloud, 
  Layers, 
  Star, 
  Smartphone, 
  Sparkles,
  Compass,
  X,
  ChevronRight
} from 'lucide-react'

interface Feature {
  id: string
  icon: any
  title: string
  subtitle: string
  description: string
  details: string[]
  color: string
  preview: React.ReactNode
}

const features: Feature[] = [
  {
    id: 'presets',
    icon: Palette,
    title: '丰富的预设库',
    subtitle: '23+ 款专业预设',
    description: '涵盖胶片、复古、清新、黑白、美食等多种风格，满足不同拍摄场景需求。',
    details: [
      '哈苏自然 - 还原哈苏相机自然色彩',
      '胶片复古 - 模拟经典胶片色彩',
      '夜景霓虹 - 城市夜景专用预设',
      '清新日系 - 清新淡雅日系风格',
      '黑白人文 - 经典黑白人文摄影'
    ],
    color: '#FF6B35',
    preview: (
      <div className="grid grid-cols-2 gap-2">
        <div className="bg-[#1C1C1E] rounded-lg p-2">
          <div className="h-16 rounded bg-gradient-to-br from-orange-400 to-red-500 mb-2" />
          <div className="text-white text-xs">哈苏自然</div>
        </div>
        <div className="bg-[#1C1C1E] rounded-lg p-2">
          <div className="h-16 rounded bg-gradient-to-br from-blue-400 to-purple-500 mb-2" />
          <div className="text-white text-xs">胶片复古</div>
        </div>
        <div className="bg-[#1C1C1E] rounded-lg p-2">
          <div className="h-16 rounded bg-gradient-to-br from-green-400 to-teal-500 mb-2" />
          <div className="text-white text-xs">夜景霓虹</div>
        </div>
        <div className="bg-[#1C1C1E] rounded-lg p-2">
          <div className="h-16 rounded bg-gradient-to-br from-pink-400 to-rose-500 mb-2" />
          <div className="text-white text-xs">清新日系</div>
        </div>
      </div>
    )
  },
  {
    id: 'cloud',
    icon: Cloud,
    title: '配置云更新',
    subtitle: '实时获取最新配置',
    description: '支持从云端获取最新配置，支持自定义更新源，随时获取社区分享的优质预设。',
    details: [
      '云端预设实时同步',
      '自定义更新源支持',
      '社区预设库订阅',
      '增量更新节省流量',
      '离线缓存随时使用'
    ],
    color: '#58A6FF',
    preview: (
      <div className="space-y-3">
        <div className="flex items-center gap-3 p-3 bg-[#1C1C1E] rounded-lg">
          <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center">
            <Cloud size={20} className="text-blue-400" />
          </div>
          <div className="flex-1">
            <div className="text-white text-sm">正在同步...</div>
            <div className="w-full h-1.5 bg-[#30363D] rounded-full mt-2 overflow-hidden">
              <motion.div
                className="h-full bg-blue-400 rounded-full"
                animate={{ width: ['0%', '100%'] }}
                transition={{ duration: 2, repeat: Infinity }}
              />
            </div>
          </div>
        </div>
        <div className="flex items-center justify-between text-xs text-gray-400">
          <span>上次更新: 2分钟前</span>
          <span>23 个预设已同步</span>
        </div>
      </div>
    )
  },
  {
    id: 'floating',
    icon: Layers,
    title: '悬浮窗模式',
    subtitle: '拍照时悬浮显示',
    description: '拍照时可悬浮显示参数，支持左右滑动切换预设，半透明设计不遮挡取景。',
    details: [
      '悬浮窗参数实时显示',
      '左右滑动切换预设',
      '无闪动平滑切换',
      '半透明不遮挡取景',
      '可收起为悬浮球'
    ],
    color: '#A371F7',
    preview: (
      <div className="relative h-40 bg-gradient-to-b from-[#1C1C1E] to-[#0D1117] rounded-lg overflow-hidden">
        <div className="absolute top-4 right-4 bg-[#161B22]/95 backdrop-blur-sm rounded-xl p-3 border border-[#30363D] shadow-lg w-32">
          <div className="flex items-center justify-between mb-2">
            <span className="text-white text-xs font-medium">哈苏自然</span>
          </div>
          <div className="space-y-1.5">
            <div className="flex justify-between text-xs">
              <span className="text-gray-500">ISO</span>
              <span className="text-white">100</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-gray-500">S</span>
              <span className="text-white">1/125</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-gray-500">WB</span>
              <span className="text-white">Auto</span>
            </div>
          </div>
        </div>
        <div className="absolute bottom-4 left-4 right-4">
          <div className="bg-black/50 backdrop-blur-sm rounded-lg px-3 py-2 flex items-center justify-between">
            <span className="text-white text-xs">相机界面</span>
            <div className="flex gap-2">
              <div className="w-6 h-6 rounded-full bg-white/20" />
              <div className="w-6 h-6 rounded-full bg-white/20" />
            </div>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'discover',
    icon: Compass,
    title: '发现功能',
    subtitle: 'v1.5.0 全新上线',
    description: '探索热门预设、摄影师作品、精选合集，发现更多摄影灵感。',
    details: [
      '热门预设排行榜',
      '推荐摄影师作品',
      '精选预设合集',
      '新品预设推荐',
      '个性化推荐算法'
    ],
    color: '#F778BA',
    preview: (
      <div className="space-y-3">
        <div className="flex gap-2 overflow-x-auto pb-2">
          <span className="px-3 py-1 bg-[#FF6B35] text-white text-xs rounded-full whitespace-nowrap">热门</span>
          <span className="px-3 py-1 bg-[#30363D] text-gray-400 text-xs rounded-full whitespace-nowrap">新品</span>
          <span className="px-3 py-1 bg-[#30363D] text-gray-400 text-xs rounded-full whitespace-nowrap">摄影师</span>
        </div>
        <div className="flex gap-2">
          <div className="flex-1 bg-[#1C1C1E] rounded-lg p-2">
            <div className="h-12 rounded bg-gradient-to-br from-purple-400 to-pink-500 mb-2" />
            <div className="text-white text-xs">人像精选</div>
          </div>
          <div className="flex-1 bg-[#1C1C1E] rounded-lg p-2">
            <div className="h-12 rounded bg-gradient-to-br from-yellow-400 to-orange-500 mb-2" />
            <div className="text-white text-xs">风景大片</div>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'favorites',
    icon: Star,
    title: '收藏管理',
    subtitle: '一键收藏快速访问',
    description: '一键收藏喜欢的预设，快速访问常用参数，本地存储无需网络，随时可用。',
    details: [
      '一键收藏喜欢的预设',
      '快速访问常用参数',
      '本地存储无需网络',
      '收藏夹分类管理',
      '历史使用记录'
    ],
    color: '#FFD700',
    preview: (
      <div className="space-y-2">
        <div className="flex items-center gap-3 p-3 bg-[#1C1C1E] rounded-lg">
          <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-orange-400 to-red-500" />
          <div className="flex-1">
            <div className="text-white text-sm">哈苏自然</div>
            <div className="text-gray-500 text-xs">小O帮帮官方</div>
          </div>
          <Star size={16} className="text-yellow-400 fill-yellow-400" />
        </div>
        <div className="flex items-center gap-3 p-3 bg-[#1C1C1E] rounded-lg">
          <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-400 to-purple-500" />
          <div className="flex-1">
            <div className="text-white text-sm">胶片复古</div>
            <div className="text-gray-500 text-xs">摄影师小王</div>
          </div>
          <Star size={16} className="text-yellow-400 fill-yellow-400" />
        </div>
      </div>
    )
  },
  {
    id: 'platform',
    icon: Smartphone,
    title: '全平台支持',
    subtitle: '各大品牌相机支持',
    description: '支持创建自定义预设，支持各大主流平台专业相机，OPPO、OnePlus、realme全覆盖。',
    details: [
      'OPPO 专业模式支持',
      'OnePlus 哈苏模式支持',
      'realme 专业模式支持',
      '自定义预设创建',
      '参数一键导入导出'
    ],
    color: '#3FB950',
    preview: (
      <div className="grid grid-cols-3 gap-2">
        <div className="bg-[#1C1C1E] rounded-lg p-3 text-center">
          <div className="w-8 h-8 mx-auto mb-2 rounded-full bg-green-500/20 flex items-center justify-center">
            <span className="text-green-400 text-xs font-bold">O</span>
          </div>
          <div className="text-white text-xs">OPPO</div>
        </div>
        <div className="bg-[#1C1C1E] rounded-lg p-3 text-center">
          <div className="w-8 h-8 mx-auto mb-2 rounded-full bg-red-500/20 flex items-center justify-center">
            <span className="text-red-400 text-xs font-bold">1+</span>
          </div>
          <div className="text-white text-xs">OnePlus</div>
        </div>
        <div className="bg-[#1C1C1E] rounded-lg p-3 text-center">
          <div className="w-8 h-8 mx-auto mb-2 rounded-full bg-yellow-500/20 flex items-center justify-center">
            <span className="text-yellow-400 text-xs font-bold">R</span>
          </div>
          <div className="text-white text-xs">realme</div>
        </div>
      </div>
    )
  }
]

export default function FeatureShowcase() {
  const [selectedFeature, setSelectedFeature] = useState<Feature | null>(null)

  return (
    <section className="py-24 bg-[#0D1117] relative">
      <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-[#30363D] to-transparent" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center mb-16">
          <span className="inline-block px-4 py-1.5 rounded-full bg-[#FF6B35]/10 text-[#FF6B35] text-sm font-medium mb-4">
            功能体验
          </span>
          <h2 className="text-4xl sm:text-5xl font-bold text-white mb-4">
            点击卡片<span className="text-[#FF6B35]">体验功能</span>
          </h2>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            探索 小O帮帮 的强大功能，让摄影更简单
          </p>
        </div>

        {/* Feature grid */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => {
            const Icon = feature.icon
            return (
              <motion.div
                key={feature.id}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
                viewport={{ once: true }}
                whileHover={{ y: -8 }}
                onClick={() => setSelectedFeature(feature)}
                className="group cursor-pointer bg-[#161B22] rounded-2xl p-6 border border-[#30363D] hover:border-[#FF6B35]/50 transition-all duration-300"
              >
                {/* Icon */}
                <div 
                  className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 transition-transform duration-300 group-hover:scale-110"
                  style={{ backgroundColor: `${feature.color}15` }}
                >
                  <Icon size={28} style={{ color: feature.color }} />
                </div>

                {/* Content */}
                <h3 className="text-xl font-bold text-white mb-2 group-hover:text-[#FF6B35] transition-colors">
                  {feature.title}
                </h3>
                <p className="text-sm text-gray-500 mb-3">{feature.subtitle}</p>
                <p className="text-gray-400 text-sm leading-relaxed mb-4">
                  {feature.description}
                </p>

                {/* Preview */}
                <div className="opacity-60 group-hover:opacity-100 transition-opacity">
                  {feature.preview}
                </div>

                {/* CTA */}
                <div className="mt-4 flex items-center text-[#FF6B35] text-sm font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  点击体验
                  <ChevronRight size={16} className="ml-1" />
                </div>
              </motion.div>
            )
          })}
        </div>
      </div>

      {/* Feature detail modal */}
      <AnimatePresence>
        {selectedFeature && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setSelectedFeature(null)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-[#161B22] rounded-3xl p-8 max-w-2xl w-full border border-[#30363D] max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="flex items-start justify-between mb-6">
                <div className="flex items-center gap-4">
                  <div 
                    className="w-16 h-16 rounded-2xl flex items-center justify-center"
                    style={{ backgroundColor: `${selectedFeature.color}15` }}
                  >
                    <selectedFeature.icon size={32} style={{ color: selectedFeature.color }} />
                  </div>
                  <div>
                    <h3 className="text-2xl font-bold text-white">{selectedFeature.title}</h3>
                    <p className="text-gray-400">{selectedFeature.subtitle}</p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedFeature(null)}
                  className="p-2 hover:bg-[#30363D] rounded-full transition-colors"
                >
                  <X size={24} className="text-gray-400" />
                </button>
              </div>

              {/* Description */}
              <p className="text-gray-300 mb-6">{selectedFeature.description}</p>

              {/* Preview */}
              <div className="mb-6 p-4 bg-[#0D1117] rounded-xl">
                {selectedFeature.preview}
              </div>

              {/* Feature list */}
              <div className="space-y-3">
                <h4 className="text-white font-semibold mb-3">功能亮点</h4>
                {selectedFeature.details.map((detail, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <div 
                      className="w-2 h-2 rounded-full"
                      style={{ backgroundColor: selectedFeature.color }}
                    />
                    <span className="text-gray-300">{detail}</span>
                  </div>
                ))}
              </div>

              {/* CTA */}
              <div className="mt-8 pt-6 border-t border-[#30363D]">
                <a
                  href="https://github.com/iCurrer/OMaster/releases"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full py-4 bg-[#FF6B35] hover:bg-[#FF8C42] text-white rounded-xl font-semibold flex items-center justify-center gap-2 transition-colors"
                >
                  <Smartphone size={20} />
                  下载 App 体验完整功能
                </a>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </section>
  )
}
