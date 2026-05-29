import { motion, AnimatePresence } from 'framer-motion'
import { Eye, EyeOff, ChevronRight, Download, Filter, Layers, Maximize2, Minimize2, Move, Check, Heart, Star } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ColorOSCard, ColorOSButton, ColorOSSwitch, ColorOSChip, ColorOSAnimations } from '../components/common/ColorOSComponents'

const filters = [
  { name: '富士胶片', intensity: 73, isHNCS: true, isFavorite: true },
  { name: '徕卡经典', intensity: 65, isHNCS: true, isFavorite: false },
  { name: '哈苏自然', intensity: 58, isHNCS: true, isFavorite: true },
  { name: '赛博朋克', intensity: 85, isHNCS: false, isFavorite: false },
  { name: '人像暖色', intensity: 62, isHNCS: false, isFavorite: true },
  { name: '风光HDR', intensity: 78, isHNCS: true, isFavorite: false },
  { name: '夜景大师', intensity: 80, isHNCS: true, isFavorite: true },
  { name: '美食鲜艳', intensity: 70, isHNCS: false, isFavorite: false },
]

export default function FloatingWindowPage() {
  const [isEnabled, setIsEnabled] = useState(true)
  const [isVisible, setIsVisible] = useState(true)
  const [isLocked, setIsLocked] = useState(false)
  const [selectedFilter, setSelectedFilter] = useState(filters[0])
  const [transparency, setTransparency] = useState(85)
  const [windowSize, setWindowSize] = useState(100)

  return (
    <div className="min-h-screen bg-deep-space pb-20">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-72 h-72 top-1/4 -left-36 animate-float" />
        <div className="orb-oppo orb-2 w-56 h-56 bottom-1/4 -right-28 animate-float" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative max-w-4xl mx-auto px-4 py-8">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
        >
          <div className="flex items-center gap-3 mb-8">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-ocean-blue to-aurora-purple flex items-center justify-center">
              <Layers className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">悬浮窗滤镜</h1>
              <p className="text-text-tertiary text-sm">实时预览，触手可及</p>
            </div>
          </div>

          <div className="grid lg:grid-cols-2 gap-8">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <ColorOSSectionHeader title="演示" subtitle="体验悬浮窗效果" />
              <ColorOSCard variant="glass" className="p-6">
                <div className="relative aspect-[9/16] bg-gradient-to-b from-gray-800 to-gray-900 rounded-[3rem] p-3 mx-auto max-w-xs">
                  <div className="w-full h-full bg-deep-space rounded-[2.5rem] overflow-hidden relative">
                    <div className="absolute top-0 left-0 right-0 h-8 bg-black/50 flex items-center justify-between px-6 z-10">
                      <span className="text-xs text-text-tertiary">9:41</span>
                      <div className="flex items-center space-x-1">
                        <div className="w-4 h-2 bg-text-tertiary rounded-sm" />
                        <div className="w-3 h-3 bg-text-tertiary rounded-full" />
                      </div>
                    </div>

                    <div className="pt-8 h-full flex flex-col">
                      <div className="flex-1 bg-gradient-to-br from-oppo-sunrise-gold/20 to-ocean-blue/20 mx-4 mt-4 rounded-2xl flex items-center justify-center relative overflow-hidden">
                        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
                        <div className="text-center">
                          <div className="w-16 h-16 rounded-full bg-white/10 flex items-center justify-center mx-auto mb-2">
                            <div className="w-8 h-8 rounded-full border-2 border-oppo-sunrise-gold/50" />
                          </div>
                          <p className="text-text-tertiary text-sm">取景预览</p>
                        </div>
                      </div>

                      <div className="p-4">
                        <div className="flex items-center justify-between mb-6">
                          <div className="flex gap-3">
                            {[1, 2, 3].map((i) => (
                              <div key={i} className="w-10 h-10 rounded-xl bg-white/5" />
                            ))}
                          </div>
                          <div className="w-16 h-16 rounded-full border-4 border-white/30 flex items-center justify-center">
                            <div className="w-12 h-12 rounded-full bg-white/20" />
                          </div>
                        </div>
                      </div>
                    </div>

                    <AnimatePresence>
                      {isVisible && isEnabled && (
                        <motion.div
                          initial={{ opacity: 0, scale: 0.8, x: 20 }}
                          animate={{ opacity: 1, scale: 1, x: 0 }}
                          exit={{ opacity: 0, scale: 0.8, x: 20 }}
                          className="absolute top-24 right-4 w-64 p-4 bg-black/85 backdrop-blur-xl border border-white/15 rounded-[16px] shadow-lg z-20"
                          style={{ opacity: transparency / 100, transform: `scale(${windowSize / 100})` }}
                        >
                          <div className="flex items-center justify-between mb-2">
                            <div>
                              <p className="text-white font-medium text-sm">{selectedFilter.name}</p>
                              <p className="text-text-tertiary text-xs">强度 {selectedFilter.intensity}%</p>
                            </div>
                            <div className="flex items-center gap-2">
                              <button
                                onClick={(e) => { e.stopPropagation(); setIsLocked(!isLocked) }}
                                className="w-6 h-6 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors"
                              >
                                <Move className="w-3 h-3 text-text-tertiary" />
                              </button>
                              <button
                                onClick={(e) => { e.stopPropagation(); setIsVisible(false) }}
                                className="w-6 h-6 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors"
                              >
                                <EyeOff className="w-3 h-3 text-text-tertiary" />
                              </button>
                            </div>
                          </div>

                          <div className="h-1.5 bg-white/20 rounded-full overflow-hidden mb-3">
                            <div className="h-full bg-gradient-to-r from-oppo-sunrise-gold to-oppo-sunrise-gold-light rounded-full" style={{ width: `${selectedFilter.intensity}%` }} />
                          </div>

                          <div className="flex gap-2">
                            <ColorOSChip label="HNCS" selected={selectedFilter.isHNCS} />
                            <ColorOSChip label="73%" selected />
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </div>

                <div className="mt-6 flex justify-center gap-4">
                  <ColorOSButton
                    variant={isVisible ? "secondary" : "primary"}
                    size="sm"
                    icon={isVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    onClick={() => setIsVisible(!isVisible)}
                  >
                    {isVisible ? "隐藏" : "显示"}
                  </ColorOSButton>
                </div>
              </ColorOSCard>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
              className="space-y-6"
            >
              <ColorOSSectionHeader title="设置" subtitle="自定义悬浮窗" />
              <ColorOSCard variant="default" className="p-6 space-y-6">
                <ColorOSSwitch
                  checked={isEnabled}
                  onChange={setIsEnabled}
                  label="启用悬浮窗"
                  description="在相机取景时显示"
                />
                <div className="h-px bg-white/10" />
                <ColorOSSwitch
                  checked={isLocked}
                  onChange={setIsLocked}
                  label="固定位置"
                  description="防止意外移动"
                />
                <div className="h-px bg-white/10" />

                <div className="space-y-4">
                  <div>
                    <div className="flex justify-between mb-2">
                      <span className="text-text-secondary text-sm">透明度</span>
                      <span className="text-white font-medium">{transparency}%</span>
                    </div>
                    <input
                      type="range"
                      min="20"
                      max="100"
                      value={transparency}
                      onChange={(e) => setTransparency(Number(e.target.value))}
                      className="w-full h-2 bg-white/10 rounded-full cursor-pointer accent-oppo-sunrise-gold"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between mb-2">
                      <span className="text-text-secondary text-sm">窗口大小</span>
                      <span className="text-white font-medium">{windowSize}%</span>
                    </div>
                    <input
                      type="range"
                      min="70"
                      max="150"
                      value={windowSize}
                      onChange={(e) => setWindowSize(Number(e.target.value))}
                      className="w-full h-2 bg-white/10 rounded-full cursor-pointer accent-oppo-sunrise-gold"
                    />
                  </div>
                </div>
              </ColorOSCard>

              <ColorOSSectionHeader title="选择滤镜" subtitle="点击应用" />
              <div className="grid grid-cols-2 gap-3">
                {filters.map((filter, i) => (
                  <motion.div
                    key={filter.name}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.3 + i * 0.05 }}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                  >
                    <ColorOSCard
                      variant={selectedFilter.name === filter.name ? "gradient" : "default"}
                      onClick={() => setSelectedFilter(filter)}
                      className="p-4"
                    >
                      <div className="flex items-start justify-between mb-2">
                        <p className="text-white font-medium text-sm">{filter.name}</p>
                        {filter.isHNCS && (
                          <span className="px-2 py-0.5 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">HNCS</span>
                        )}
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-text-tertiary text-xs">{filter.intensity}%</span>
                        <Heart
                          className={`w-4 h-4 ${filter.isFavorite ? 'fill-sakura-pink text-sakura-pink' : 'text-text-tertiary'}`}
                        />
                      </div>
                    </ColorOSCard>
                  </motion.div>
                ))}
              </div>
            </motion.div>
          </div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="mt-12"
          >
            <ColorOSSectionHeader title="功能特性" subtitle="ColorOS 深度集成" />
            <div className="grid md:grid-cols-3 gap-4">
              {[
                { icon: Eye, title: "实时预览", desc: "取景即见效果" },
                { icon: Layers, title: "一键切换", desc: "悬浮窗内滑动换滤镜" },
                { icon: Maximize2, title: "自由调整", desc: "大小位置透明度" },
              ].map((feature, i) => (
                <ColorOSCard key={feature.title} className="p-6">
                  <div className="w-12 h-12 rounded-xl bg-white/5 flex items-center justify-center mb-4">
                    <feature.icon className="w-6 h-6 text-oppo-sunrise-gold" />
                  </div>
                  <h3 className="text-white font-semibold mb-2">{feature.title}</h3>
                  <p className="text-text-tertiary text-sm">{feature.desc}</p>
                </ColorOSCard>
              ))}
            </div>
          </motion.div>
        </motion.div>
      </div>
    </div>
  )
}

function ColorOSSectionHeader({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="mb-4">
      <h2 className="text-lg font-semibold text-white">{title}</h2>
      {subtitle && <p className="text-text-tertiary text-sm mt-1">{subtitle}</p>}
    </div>
  )
}
