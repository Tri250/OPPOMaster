import { motion } from 'framer-motion'
import { useInView } from 'framer-motion'
import { useRef, useState } from 'react'
import { ChevronLeft, ChevronRight, Smartphone, Layers, Sliders } from 'lucide-react'

const screenshots = [
  {
    id: 1,
    title: '首页浏览',
    description: '瀑布流展示所有预设，支持分类筛选',
    icon: Smartphone,
    content: (
      <div className="space-y-3">
        <div className="flex items-center gap-2 mb-4">
          <div className="w-8 h-8 rounded-lg bg-[#FF6B35] flex items-center justify-center">
            <span className="text-white font-bold text-sm">O</span>
          </div>
          <span className="text-white font-semibold">小O帮帮</span>
        </div>
        <div className="flex gap-2 mb-3">
          <span className="px-3 py-1 bg-[#FF6B35] text-white text-xs rounded-full">全部</span>
          <span className="px-3 py-1 bg-[#30363D] text-gray-400 text-xs rounded-full">风景</span>
          <span className="px-3 py-1 bg-[#30363D] text-gray-400 text-xs rounded-full">人像</span>
        </div>
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
      </div>
    )
  },
  {
    id: 2,
    title: '预设详情',
    description: '查看完整参数和样片效果',
    icon: Sliders,
    content: (
      <div className="space-y-3">
        <div className="h-24 rounded-xl bg-gradient-to-br from-orange-400 via-red-500 to-pink-500 mb-3" />
        <div className="text-white font-semibold">哈苏自然</div>
        <div className="text-gray-500 text-xs mb-3">还原哈苏相机自然色彩</div>
        <div className="space-y-2">
          <div className="flex justify-between text-xs">
            <span className="text-gray-400">ISO</span>
            <span className="text-white">100</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-gray-400">快门</span>
            <span className="text-white">1/125</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-gray-400">白平衡</span>
            <span className="text-white">Auto</span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-gray-400">滤镜</span>
            <span className="text-white">Natural</span>
          </div>
        </div>
        <button className="w-full py-2 bg-[#FF6B35] text-white text-xs rounded-lg mt-3">应用预设</button>
      </div>
    )
  },
  {
    id: 3,
    title: '悬浮窗模式',
    description: '拍照时随时参考参数',
    icon: Layers,
    content: (
      <div className="relative h-full">
        <div className="absolute top-4 right-4 bg-[#1C1C1E]/95 backdrop-blur-sm rounded-xl p-3 border border-[#30363D] shadow-lg w-32">
          <div className="flex items-center justify-between mb-2">
            <span className="text-white text-xs font-medium">哈苏自然</span>
            <button className="text-gray-400 hover:text-white">
              <ChevronLeft size={14} />
            </button>
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
            <div className="flex justify-between text-xs">
              <span className="text-gray-500">EV</span>
              <span className="text-white">0</span>
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
  }
]

export default function Preview() {
  const sectionRef = useRef(null)
  const isInView = useInView(sectionRef, { once: true, margin: "-100px" })
  const [currentIndex, setCurrentIndex] = useState(0)

  const nextSlide = () => {
    setCurrentIndex((prev) => (prev + 1) % screenshots.length)
  }

  const prevSlide = () => {
    setCurrentIndex((prev) => (prev - 1 + screenshots.length) % screenshots.length)
  }

  const current = screenshots[currentIndex]
  const Icon = current.icon

  return (
    <section ref={sectionRef} className="py-24 bg-[#0D1117] relative overflow-hidden">
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
            界面预览
          </span>
          <h2 className="text-4xl sm:text-5xl font-bold text-white mb-4">
            简洁优雅，<span className="text-[#FF6B35]">极致体验</span>
          </h2>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            精心设计的界面，让每一次操作都流畅自然
          </p>
        </motion.div>

        {/* Preview carousel */}
        <div className="relative max-w-4xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-8 items-center">
            {/* Phone mockup */}
            <motion.div
              initial={{ opacity: 0, x: -50 }}
              animate={isInView ? { opacity: 1, x: 0 } : {}}
              transition={{ duration: 0.6 }}
              className="relative"
            >
              <div className="relative mx-auto w-64">
                {/* Phone frame */}
                <div className="relative bg-[#1C1C1E] rounded-[2.5rem] p-2 shadow-2xl shadow-black/50 border border-gray-800">
                  <div className="relative bg-[#0D1117] rounded-[2rem] overflow-hidden aspect-[9/19]">
                    {/* Dynamic notch */}
                    <div className="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-6 bg-black rounded-b-2xl z-10" />
                    
                    {/* Screen content */}
                    <motion.div
                      key={currentIndex}
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -20 }}
                      transition={{ duration: 0.3 }}
                      className="absolute inset-0 p-4 pt-10"
                    >
                      {current.content}
                    </motion.div>
                  </div>
                </div>
                
                {/* Glow */}
                <div className="absolute -inset-4 bg-[#FF6B35]/10 rounded-[3rem] blur-2xl -z-10" />
              </div>

              {/* Navigation arrows */}
              <button
                onClick={prevSlide}
                className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-4 lg:-translate-x-12 w-10 h-10 rounded-full bg-[#30363D] hover:bg-[#484F58] flex items-center justify-center text-white transition-colors"
              >
                <ChevronLeft size={20} />
              </button>
              <button
                onClick={nextSlide}
                className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-4 lg:translate-x-12 w-10 h-10 rounded-full bg-[#30363D] hover:bg-[#484F58] flex items-center justify-center text-white transition-colors"
              >
                <ChevronRight size={20} />
              </button>
            </motion.div>

            {/* Info panel */}
            <motion.div
              initial={{ opacity: 0, x: 50 }}
              animate={isInView ? { opacity: 1, x: 0 } : {}}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="text-center lg:text-left"
            >
              <motion.div
                key={currentIndex}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
              >
                <div 
                  className="w-16 h-16 rounded-2xl flex items-center justify-center mb-6 mx-auto lg:mx-0"
                  style={{ backgroundColor: '#FF6B3515' }}
                >
                  <Icon size={32} className="text-[#FF6B35]" />
                </div>
                <h3 className="text-3xl font-bold text-white mb-4">{current.title}</h3>
                <p className="text-xl text-gray-400 mb-8">{current.description}</p>
              </motion.div>

              {/* Indicators */}
              <div className="flex gap-2 justify-center lg:justify-start">
                {screenshots.map((_, index) => (
                  <button
                    key={index}
                    onClick={() => setCurrentIndex(index)}
                    className={`w-3 h-3 rounded-full transition-all duration-300 ${
                      index === currentIndex 
                        ? 'bg-[#FF6B35] w-8' 
                        : 'bg-[#30363D] hover:bg-[#484F58]'
                    }`}
                  />
                ))}
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </section>
  )
}
