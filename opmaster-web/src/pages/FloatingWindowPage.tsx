import { motion, AnimatePresence } from 'framer-motion'
import { 
  Layers, Eye, EyeOff, Move, Maximize2, 
  Settings, Camera, ChevronRight, Play, Pause,
  Smartphone, Zap, Check, X, RefreshCw
} from 'lucide-react'
import { useState } from 'react'
import { 
  ColorOSCard, ColorOSButton, ColorOSSwitch,
  ColorOSSectionHeader, ColorOSAnimations, ColorOSSlider
} from '../components/common/ColorOSComponents'

export default function FloatingWindowPage() {
  const [isEnabled, setIsEnabled] = useState(false)
  const [isPreviewVisible, setIsPreviewVisible] = useState(false)
  const [opacity, setOpacity] = useState(90)
  const [size, setSize] = useState(100)
  const [position, setPosition] = useState<'left' | 'center' | 'right'>('right')

  const features = [
    { icon: <Eye className="w-5 h-5" />, title: '实时预览', desc: '相机取景时实时显示预设效果' },
    { icon: <Zap className="w-5 h-5" />, title: '快速切换', desc: '一键切换不同预设参数' },
    { icon: <Move className="w-5 h-5" />, title: '自由拖动', desc: '悬浮窗位置可自由调整' },
    { icon: <Maximize2 className="w-5 h-5" />, title: '尺寸调节', desc: '根据需要调整窗口大小' },
  ]

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-72 h-72 top-20 -right-36 animate-float" />
        <div className="orb-oppo orb-2 w-56 h-56 bottom-40 -left-28 animate-float" style={{ animationDelay: '2s' }} />
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
              <h1 className="text-2xl font-bold text-white">悬浮窗功能</h1>
              <p className="text-text-tertiary text-sm">ColorOS 系统级实时预览</p>
            </div>
          </div>

          <div className="grid lg:grid-cols-2 gap-6">
            <motion.section
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <ColorOSSectionHeader 
                title="功能演示" 
                subtitle="体验悬浮窗实时预览效果"
              />

              <div className="relative aspect-[9/16] max-w-xs mx-auto">
                <ColorOSCard variant="elevated" className="w-full h-full p-3">
                  <div className="w-full h-full rounded-2xl bg-gradient-to-b from-card-surface to-elevated relative overflow-hidden">
                    <div className="absolute top-0 left-0 right-0 h-6 bg-deep-space/80 flex items-center justify-between px-4">
                      <span className="text-text-tertiary text-xs">9:41</span>
                      <div className="flex gap-1">
                        <div className="w-4 h-2 bg-text-tertiary rounded-sm" />
                        <div className="w-3 h-3 bg-text-tertiary rounded-full" />
                      </div>
                    </div>

                    <div className="pt-8 h-full flex flex-col">
                      <div className="flex-1 flex items-center justify-center p-4">
                        <div className="w-full aspect-square rounded-xl bg-gradient-to-br from-oppo-sunrise-gold/20 to-ocean-blue/20 flex items-center justify-center">
                          <Camera className="w-16 h-16 text-oppo-sunrise-gold/50" />
                        </div>
                      </div>

                      <div className="p-4 space-y-3">
                        <div className="flex justify-center gap-6">
                          {[1, 2, 3].map((i) => (
                            <div key={i} className="w-10 h-10 rounded-full bg-white/10" />
                          ))}
                        </div>
                        <div className="flex justify-center">
                          <div className="w-16 h-16 rounded-full border-4 border-white/30 flex items-center justify-center">
                            <div className="w-12 h-12 rounded-full bg-white/20" />
                          </div>
                        </div>
                      </div>
                    </div>

                    <AnimatePresence>
                      {isPreviewVisible && (
                        <motion.div
                          initial={{ opacity: 0, scale: 0.8, x: 20 }}
                          animate={{ opacity: 1, scale: 1, x: 0 }}
                          exit={{ opacity: 0, scale: 0.8, x: 20 }}
                          style={{ 
                            right: position === 'right' ? '8px' : position === 'left' ? 'auto' : '50%',
                            left: position === 'left' ? '8px' : position === 'right' ? 'auto' : '50%',
                            transform: position === 'center' ? 'translateX(-50%)' : 'none',
                            opacity: opacity / 100,
                            width: `${size * 0.8}px`
                          }}
                          className="absolute top-20 bg-card-surface/95 backdrop-blur-md rounded-xl border border-oppo-sunrise-gold/30 shadow-lg overflow-hidden"
                        >
                          <div className="p-2 border-b border-white/10 flex items-center justify-between">
                            <span className="text-xs text-text-secondary">实时预览</span>
                            <div className="flex items-center gap-1">
                              <div className="w-2 h-2 rounded-full bg-oppo-green animate-pulse" />
                              <span className="text-xs text-oppo-green">运行中</span>
                            </div>
                          </div>
                          <div className="p-2">
                            <div className="aspect-square rounded-lg bg-gradient-to-br from-oppo-sunrise-gold/30 to-hasselblad-pro/30 flex items-center justify-center">
                              <Camera className="w-6 h-6 text-oppo-sunrise-gold" />
                            </div>
                            <div className="mt-2 text-center">
                              <p className="text-xs text-white font-medium">城市夜景</p>
                              <p className="text-xs text-text-tertiary">Find X7 Ultra</p>
                            </div>
                          </div>
                          <div className="p-2 border-t border-white/10 flex justify-center gap-2">
                            <button className="w-6 h-6 rounded-lg bg-white/10 flex items-center justify-center">
                              <RefreshCw className="w-3 h-3 text-text-secondary" />
                            </button>
                            <button className="w-6 h-6 rounded-lg bg-white/10 flex items-center justify-center">
                              <Settings className="w-3 h-3 text-text-secondary" />
                            </button>
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </ColorOSCard>
              </div>

              <div className="mt-6 flex justify-center gap-3">
                <ColorOSButton
                  variant={isPreviewVisible ? 'secondary' : 'primary'}
                  onClick={() => setIsPreviewVisible(!isPreviewVisible)}
                  icon={isPreviewVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                >
                  {isPreviewVisible ? '隐藏预览' : '显示预览'}
                </ColorOSButton>
              </div>
            </motion.section>

            <motion.section
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
            >
              <ColorOSSectionHeader 
                title="功能设置" 
                subtitle="自定义悬浮窗行为"
              />

              <ColorOSCard variant="default" className="p-5 space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-white font-medium">启用悬浮窗</p>
                    <p className="text-text-tertiary text-sm">相机打开时自动显示</p>
                  </div>
                  <ColorOSSwitch
                    checked={isEnabled}
                    onChange={setIsEnabled}
                  />
                </div>

                <div className="h-px bg-oppo-border/50" />

                <ColorOSSlider
                  label="透明度"
                  value={opacity}
                  onChange={setOpacity}
                  min={50}
                  max={100}
                  unit="%"
                />

                <ColorOSSlider
                  label="窗口大小"
                  value={size}
                  onChange={setSize}
                  min={60}
                  max={140}
                  unit="px"
                />

                <div className="space-y-3">
                  <p className="text-text-secondary text-sm">显示位置</p>
                  <div className="flex gap-2">
                    {(['left', 'center', 'right'] as const).map((pos) => (
                      <motion.button
                        key={pos}
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => setPosition(pos)}
                        className={`flex-1 py-2 rounded-xl text-sm font-medium transition-all ${
                          position === pos
                            ? 'bg-oppo-sunrise-gold text-deep-space'
                            : 'bg-white/10 text-text-secondary hover:bg-white/15'
                        }`}
                      >
                        {pos === 'left' ? '左侧' : pos === 'center' ? '居中' : '右侧'}
                      </motion.button>
                    ))}
                  </div>
                </div>
              </ColorOSCard>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
                className="mt-6"
              >
                <ColorOSSectionHeader 
                  title="核心功能" 
                  subtitle="悬浮窗提供的功能"
                />

                <div className="space-y-3">
                  {features.map((feature, index) => (
                    <motion.div
                      key={feature.title}
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.4 + index * 0.1 }}
                    >
                      <ColorOSCard variant="default" className="p-4">
                        <div className="flex items-center gap-4">
                          <div className="w-10 h-10 rounded-xl bg-oppo-sunrise-gold/10 flex items-center justify-center text-oppo-sunrise-gold">
                            {feature.icon}
                          </div>
                          <div className="flex-1">
                            <p className="text-white font-medium">{feature.title}</p>
                            <p className="text-text-tertiary text-sm">{feature.desc}</p>
                          </div>
                          <Check className="w-5 h-5 text-oppo-green" />
                        </div>
                      </ColorOSCard>
                    </motion.div>
                  ))}
                </div>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.6 }}
                className="mt-6"
              >
                <ColorOSCard variant="gradient" className="p-5">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-xl bg-oppo-green/20 flex items-center justify-center">
                      <Smartphone className="w-6 h-6 text-oppo-green" />
                    </div>
                    <div className="flex-1">
                      <p className="text-white font-medium">ColorOS 深度集成</p>
                      <p className="text-text-secondary text-sm">需要系统权限支持，请在设置中开启</p>
                    </div>
                    <ChevronRight className="w-5 h-5 text-text-tertiary" />
                  </div>
                </ColorOSCard>
              </motion.div>
            </motion.section>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
