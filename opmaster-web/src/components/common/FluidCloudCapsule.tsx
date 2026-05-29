import { motion, AnimatePresence } from 'framer-motion'
import { Sparkles, Camera, Layers, Settings, Zap, Wifi, Bluetooth, Volume2, X } from 'lucide-react'
import { useState } from 'react'

interface CapsuleItem {
  id: string
  icon: React.ReactNode
  label: string
  color: string
  onClick: () => void
}

export default function FluidCloudCapsule() {
  const [isExpanded, setIsExpanded] = useState(false)
  const [isVisible, setIsVisible] = useState(true)
  const [isActive, setIsActive] = useState(false)

  const capsuleItems: CapsuleItem[] = [
    { id: 'camera', icon: <Camera className="w-5 h-5" />, label: '相机', color: 'from-oppo-orange to-hasselblad-orange', onClick: () => console.log('打开相机') },
    { id: 'ai', icon: <Sparkles className="w-5 h-5" />, label: 'AI识别', color: 'from-oppo-blue to-oppo-purple', onClick: () => console.log('AI场景识别') },
    { id: 'preset', icon: <Layers className="w-5 h-5" />, label: '预设', color: 'from-oppo-green to-oppo-blue', onClick: () => console.log('切换预设') },
    { id: 'settings', icon: <Settings className="w-5 h-5" />, label: '设置', color: 'from-oppo-gray to-oppo-dark', onClick: () => console.log('打开设置') }
  ]

  const systemItems = [
    { id: 'wifi', icon: <Wifi className="w-4 h-4" />, label: 'WiFi', active: true },
    { id: 'bluetooth', icon: <Bluetooth className="w-4 h-4" />, label: '蓝牙', active: false },
    { id: 'volume', icon: <Volume2 className="w-4 h-4" />, label: '音量', active: true }
  ]

  const currentItem = capsuleItems[0]

  return (
    <>
      {/* 流体云胶囊入口 */}
      <AnimatePresence>
        {isVisible && (
          <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: [0.05, 0.7, 0.1, 1.0] }}
            className="fixed right-4 bottom-24 z-50"
          >
            {isExpanded ? (
              /* 展开模式 */
              <motion.div
                initial={{ opacity: 0, y: 20, scale: 0.9 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 20, scale: 0.9 }}
                className="glass-effect rounded-3xl p-2 shadow-2xl min-w-[200px]"
              >
                {/* 标题栏 */}
                <div className="flex items-center justify-between mb-3 px-2">
                  <div className="flex items-center gap-2">
                    <Zap className="w-4 h-4 text-oppo-orange" />
                    <span className="text-sm font-bold">OMaster 快捷入口</span>
                  </div>
                  <button
                    onClick={() => setIsExpanded(false)}
                    className="p-1 hover:bg-white/10 rounded-lg transition-colors"
                  >
                    <X className="w-4 h-4 text-white/60" />
                  </button>
                </div>

                {/* 快捷功能列表 */}
                <div className="space-y-1">
                  {capsuleItems.map((item) => (
                    <motion.button
                      key={item.id}
                      whileHover={{ scale: 1.02, x: -4 }}
                      whileTap={{ scale: 0.98 }}
                      onClick={() => {
                        item.onClick()
                        setIsExpanded(false)
                      }}
                      className={`w-full flex items-center gap-3 p-3 rounded-2xl transition-all bg-gradient-to-r ${item.color} bg-opacity-10 hover:bg-opacity-20`}
                    >
                      <div className={`p-2 rounded-xl bg-gradient-to-r ${item.color} text-white`}>
                        {item.icon}
                      </div>
                      <span className="text-sm font-medium text-white">{item.label}</span>
                    </motion.button>
                  ))}
                </div>

                {/* 系统快捷设置 */}
                <div className="mt-3 pt-3 border-t border-white/10">
                  <div className="flex items-center justify-around">
                    {systemItems.map((item) => (
                      <button
                        key={item.id}
                        className={`p-2 rounded-xl transition-all ${
                          item.active ? 'bg-oppo-orange/20 text-oppo-orange' : 'text-white/40'
                        }`}
                      >
                        {item.icon}
                      </button>
                    ))}
                  </div>
                </div>

                {/* 隐藏胶囊按钮 */}
                <button
                  onClick={() => setIsVisible(false)}
                  className="mt-2 w-full py-2 text-xs text-white/40 hover:text-white/60 transition-colors"
                >
                  隐藏胶囊
                </button>
              </motion.div>
            ) : (
              /* 收起模式 - 胶囊形状 */
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => {
                  setIsExpanded(true)
                  setIsActive(true)
                }}
                className={`glass-effect rounded-full px-4 py-2.5 flex items-center gap-2 shadow-2xl transition-all ${
                  isActive ? 'ring-2 ring-oppo-orange' : ''
                }`}
              >
                <div className={`p-1.5 rounded-xl bg-gradient-to-r ${currentItem.color}`}>
                  {currentItem.icon}
                </div>
                <span className="text-sm font-medium text-white">OMaster</span>
                <div className="w-2 h-2 rounded-full bg-oppo-green animate-pulse" />
              </motion.button>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* 显示胶囊按钮（当胶囊被隐藏时） */}
      {!isVisible && (
        <motion.button
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          onClick={() => setIsVisible(true)}
          className="fixed right-4 bottom-24 z-50 glass-effect rounded-full px-4 py-2 flex items-center gap-2 shadow-2xl"
        >
          <Sparkles className="w-4 h-4 text-oppo-orange" />
          <span className="text-sm font-medium">显示胶囊</span>
        </motion.button>
      )}
    </>
  )
}
