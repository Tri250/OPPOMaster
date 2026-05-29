import { motion } from 'framer-motion'
import { Layers, Eye, EyeOff, GripVertical, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { ColorOSSlider } from '../components/common/ColorOSComponents'

interface FloatingWindow {
  id: string
  name: string
  intensity: number
  isVisible: boolean
  isLocked: boolean
  x: number
  y: number
}

const defaultFilters = [
  { id: 'fuji', name: '富士胶片', color: 'from-orange-500 to-red-500' },
  { id: 'leica', name: '徕卡经典', color: 'from-gray-600 to-gray-800' },
  { id: 'hasselblad', name: '哈苏自然', color: 'from-amber-600 to-yellow-500' },
  { id: 'cyberpunk', name: '赛博朋克', color: 'from-purple-600 to-pink-500' },
  { id: 'portrait', name: '人像暖色', color: 'from-rose-400 to-orange-400' },
  { id: 'night', name: '夜景大师', color: 'from-blue-600 to-indigo-800' }
]

export default function FloatingWindowPage() {
  const [windows, setWindows] = useState<FloatingWindow[]>([
    { id: 'fuji', name: '富士胶片', intensity: 73, isVisible: true, isLocked: false, x: 20, y: 200 },
    { id: 'leica', name: '徕卡经典', intensity: 50, isVisible: false, isLocked: false, x: 20, y: 350 }
  ])
  const [globalOpacity, setGlobalOpacity] = useState(80)
  const [selectedWindow, setSelectedWindow] = useState<string | null>(null)

  const toggleWindowVisibility = (id: string) => {
    setWindows(prev => prev.map(w => 
      w.id === id ? { ...w, isVisible: !w.isVisible } : w
    ))
  }

  const toggleWindowLock = (id: string) => {
    setWindows(prev => prev.map(w => 
      w.id === id ? { ...w, isLocked: !w.isLocked } : w
    ))
  }

  const updateIntensity = (id: string, intensity: number) => {
    setWindows(prev => prev.map(w => 
      w.id === id ? { ...w, intensity } : w
    ))
  }

  const removeWindow = (id: string) => {
    setWindows(prev => prev.filter(w => w.id !== id))
    if (selectedWindow === id) setSelectedWindow(null)
  }

  const addWindow = (filterId: string) => {
    const filter = defaultFilters.find(f => f.id === filterId)
    if (!filter || windows.find(w => w.id === filterId)) return
    
    setWindows(prev => [...prev, {
      id: filter.id,
      name: filter.name,
      intensity: 50,
      isVisible: true,
      isLocked: false,
      x: 20,
      y: 100 + prev.length * 150
    }])
  }

  return (
    <div className="min-h-screen bg-oppo-black text-text-primary">
      <header className="sticky top-0 z-40 bg-oppo-black/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
          <h1 className="text-h3 font-semibold">悬浮窗</h1>
          <div className="flex items-center gap-2">
            <button className="btn-primary text-sm py-2 px-4 touch-feedback">
              保存布局
            </button>
          </div>
        </div>
      </header>

      <main className="p-4 space-y-6">
        <section className="card-oppo p-4">
          <h2 className="text-sm font-medium text-text-secondary mb-4">全局设置</h2>
          <div className="space-y-4">
            <ColorOSSlider
              value={globalOpacity}
              onChange={setGlobalOpacity}
              label="全局透明度"
              unit="%"
            />
          </div>
        </section>

        <section className="card-oppo p-4">
          <h2 className="text-sm font-medium text-text-secondary mb-4">添加滤镜</h2>
          <div className="grid grid-cols-3 gap-2">
            {defaultFilters.filter(f => !windows.find(w => w.id === f.id)).map(filter => (
              <motion.button
                key={filter.id}
                whileTap={{ scale: 0.95 }}
                onClick={() => addWindow(filter.id)}
                className="p-3 bg-white/5 rounded-xl hover:bg-white/10 transition-colors duration-200 touch-feedback flex flex-col items-center gap-2"
              >
                <div className={`w-10 h-10 rounded-full bg-gradient-to-br ${filter.color}`} />
                <span className="text-xs text-text-secondary">{filter.name}</span>
              </motion.button>
            ))}
          </div>
        </section>

        <section className="card-oppo p-4">
          <h2 className="text-sm font-medium text-text-secondary mb-4">已添加的悬浮窗 ({windows.length})</h2>
          <div className="space-y-3">
            {windows.map(window => (
              <motion.div
                key={window.id}
                layout
                className={`p-4 bg-white/5 rounded-xl transition-all duration-200 ${
                  selectedWindow === window.id ? 'ring-2 ring-oppo-orange' : ''
                }`}
                onClick={() => setSelectedWindow(window.id)}
              >
                <div className="flex items-center justify-between mb-3">
                  <span className="font-medium text-text-primary">{window.name}</span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={(e) => { e.stopPropagation(); toggleWindowVisibility(window.id) }}
                      className="p-2 rounded-full bg-white/10 hover:bg-white/20 transition-colors duration-200 touch-feedback"
                      aria-label={window.isVisible ? '隐藏' : '显示'}
                    >
                      {window.isVisible ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                    </button>
                    <button
                      onClick={(e) => { e.stopPropagation(); toggleWindowLock(window.id) }}
                      className={`p-2 rounded-full transition-colors duration-200 touch-feedback ${
                        window.isLocked ? 'bg-oppo-orange/20 text-oppo-orange' : 'bg-white/10 hover:bg-white/20'
                      }`}
                      aria-label={window.isLocked ? '解锁' : '锁定'}
                    >
                      <GripVertical className="w-4 h-4" />
                    </button>
                    <button
                      onClick={(e) => { e.stopPropagation(); removeWindow(window.id) }}
                      className="p-2 rounded-full bg-error/20 hover:bg-error/30 text-error transition-colors duration-200 touch-feedback"
                      aria-label="删除"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
                <ColorOSSlider
                  value={window.intensity}
                  onChange={(val) => updateIntensity(window.id, val)}
                  label="滤镜强度"
                  unit="%"
                />
              </motion.div>
            ))}
          </div>
        </section>

        <section className="card-oppo p-4">
          <h2 className="text-sm font-medium text-text-secondary mb-4">操作说明</h2>
          <div className="space-y-3 text-sm text-text-tertiary">
            <div className="flex items-start gap-3">
              <Eye className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <p>点击眼睛图标可以快速隐藏/显示悬浮窗</p>
            </div>
            <div className="flex items-start gap-3">
              <GripVertical className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <p>锁定后悬浮窗将固定在当前位置，防止误触移动</p>
            </div>
            <div className="flex items-start gap-3">
              <Layers className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <p>拖动调整悬浮窗位置，长按拖动可快速定位</p>
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}
