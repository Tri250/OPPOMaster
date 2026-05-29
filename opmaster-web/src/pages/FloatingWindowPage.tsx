import { motion } from 'framer-motion'
import { Layers, Eye, EyeOff, GripVertical, Trash2, Smartphone, Shield, Zap, Check } from 'lucide-react'
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

const windowTypes = [
  {
    id: 'standard',
    name: '标准悬浮窗',
    desc: '适用于Android 8.0+，兼容性好，大多数机型可用',
    icon: <Smartphone className="w-6 h-6" />,
    compatible: 'Android 8.0+'
  },
  {
    id: 'accessibility',
    name: '无障碍悬浮窗',
    desc: '针对ColorOS/小米等定制系统，合法绕过相机上层显示限制',
    icon: <Shield className="w-6 h-6" />,
    compatible: 'ColorOS/MIUI等'
  },
  {
    id: 'notification',
    name: '通知栏模式',
    desc: '极端限制场景下的兜底方案，通知栏展示+迷你悬浮球侧滑',
    icon: <Zap className="w-6 h-6" />,
    compatible: '所有机型'
  }
]

const brandPermissions = [
  { brand: 'OPPO/一加/realme', steps: ['设置 → 应用管理 → 小O帮帮 → 悬浮窗', '设置 → 无障碍 → 小O帮帮'] },
  { brand: '小米', steps: ['设置 → 授权管理 → 应用权限管理 → 小O帮帮 → 显示悬浮窗', '设置 → 更多设置 → 无障碍 → 小O帮帮'] },
  { brand: 'vivo', steps: ['设置 → 应用与权限 → 权限管理 → 悬浮窗 → 小O帮帮', '设置 → 快捷与辅助 → 无障碍 → 小O帮帮'] },
  { brand: '华为', steps: ['设置 → 应用和服务 → 权限管理 → 小O帮帮 → 悬浮窗', '设置 → 辅助功能 → 无障碍 → 小O帮帮'] }
]

export default function FloatingWindowPage() {
  const [windows, setWindows] = useState<FloatingWindow[]>([
    { id: 'fuji', name: '富士胶片', intensity: 73, isVisible: true, isLocked: false, x: 20, y: 200 },
    { id: 'leica', name: '徕卡经典', intensity: 50, isVisible: false, isLocked: false, x: 20, y: 350 }
  ])
  const [globalOpacity, setGlobalOpacity] = useState(80)
  const [selectedWindow, setSelectedWindow] = useState<string | null>(null)
  const [selectedWindowType, setSelectedWindowType] = useState('standard')

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
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-orange to-hasselblad-orange rounded-2xl mb-6">
            <Layers className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text-oppo">
            智能悬浮窗
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            多悬浮窗类型兼容方案，支持标准悬浮窗、无障碍悬浮窗和通知栏模式，兼容率达95%+
          </p>
        </motion.div>

        {/* Window Types */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="mb-8"
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">选择悬浮窗类型</h2>
            <div className="grid md:grid-cols-3 gap-4">
              {windowTypes.map((type) => (
                <motion.button
                  key={type.id}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setSelectedWindowType(type.id)}
                  className={`p-6 rounded-xl text-left transition-all duration-200 ${
                    selectedWindowType === type.id
                      ? 'bg-oppo-orange/20 border border-oppo-orange/50'
                      : 'bg-white/5 border border-white/10 hover:bg-white/10'
                  }`}
                >
                  <div className="w-12 h-12 bg-oppo-orange/20 rounded-xl flex items-center justify-center text-oppo-orange mb-4">
                    {type.icon}
                  </div>
                  <h3 className="text-lg font-bold mb-2">{type.name}</h3>
                  <p className="text-white/60 text-sm mb-3">{type.desc}</p>
                  <p className="text-xs text-hasselblad-orange">兼容：{type.compatible}</p>
                </motion.button>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Permission Guide */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-8"
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">分品牌权限引导</h2>
            <div className="grid md:grid-cols-2 gap-4">
              {brandPermissions.map((item, idx) => (
                <motion.div
                  key={idx}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.3 + idx * 0.1 }}
                  className="bg-white/5 rounded-xl p-5"
                >
                  <h3 className="font-bold mb-3">{item.brand}</h3>
                  <ul className="space-y-2">
                    {item.steps.map((step, stepIdx) => (
                      <li key={stepIdx} className="flex items-start gap-2 text-sm text-white/60">
                        <span className="flex-shrink-0 w-5 h-5 bg-oppo-orange/20 rounded-full flex items-center justify-center text-oppo-orange text-xs font-bold">
                          {stepIdx + 1}
                        </span>
                        <span>{step}</span>
                      </li>
                    ))}
                  </ul>
                </motion.div>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Global Settings */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="mb-8"
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">全局设置</h2>
            <div className="space-y-4">
              <ColorOSSlider
                value={globalOpacity}
                onChange={setGlobalOpacity}
                label="全局透明度"
                unit="%"
              />
            </div>
          </div>
        </motion.div>

        {/* Add Filters */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="mb-8"
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">添加滤镜悬浮窗</h2>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              {defaultFilters.filter(f => !windows.find(w => w.id === f.id)).map(filter => (
                <motion.button
                  key={filter.id}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => addWindow(filter.id)}
                  className="p-5 bg-white/5 rounded-xl hover:bg-white/10 transition-colors duration-200 flex flex-col items-center gap-3"
                >
                  <div className={`w-14 h-14 rounded-full bg-gradient-to-br ${filter.color}`} />
                  <span className="text-sm font-medium">{filter.name}</span>
                </motion.button>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Active Windows */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">已添加的悬浮窗 ({windows.length})</h2>
            <div className="space-y-4">
              {windows.map(window => (
                <motion.div
                  key={window.id}
                  layout
                  className={`p-5 bg-white/5 rounded-xl transition-all duration-200 ${
                    selectedWindow === window.id ? 'ring-2 ring-oppo-orange' : ''
                  }`}
                  onClick={() => setSelectedWindow(window.id)}
                >
                  <div className="flex items-center justify-between mb-4">
                    <span className="font-bold">{window.name}</span>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={(e) => { e.stopPropagation(); toggleWindowVisibility(window.id) }}
                        className="p-2 rounded-full bg-white/10 hover:bg-white/20 transition-colors duration-200"
                        aria-label={window.isVisible ? '隐藏' : '显示'}
                      >
                        {window.isVisible ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                      </button>
                      <button
                        onClick={(e) => { e.stopPropagation(); toggleWindowLock(window.id) }}
                        className={`p-2 rounded-full transition-colors duration-200 ${
                          window.isLocked ? 'bg-oppo-orange/20 text-oppo-orange' : 'bg-white/10 hover:bg-white/20'
                        }`}
                        aria-label={window.isLocked ? '解锁' : '锁定'}
                      >
                        <GripVertical className="w-4 h-4" />
                      </button>
                      <button
                        onClick={(e) => { e.stopPropagation(); removeWindow(window.id) }}
                        className="p-2 rounded-full bg-red-500/20 hover:bg-red-500/30 text-red-500 transition-colors duration-200"
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
                  <div className="flex items-center gap-2 mt-3 text-xs text-white/60">
                    <Check className="w-4 h-4 text-green-500" />
                    <span>使用 SurfaceView 绘制，降低帧率损耗</span>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Usage Instructions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
          className="mt-8"
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">使用说明</h2>
            <div className="grid md:grid-cols-3 gap-6">
              <div className="flex items-start gap-3">
                <Eye className="w-5 h-5 mt-0.5 flex-shrink-0 text-oppo-orange" />
                <div>
                  <h3 className="font-medium mb-1">快速显示/隐藏</h3>
                  <p className="text-sm text-white/60">点击眼睛图标可以快速隐藏或显示悬浮窗</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <GripVertical className="w-5 h-5 mt-0.5 flex-shrink-0 text-oppo-orange" />
                <div>
                  <h3 className="font-medium mb-1">锁定位置</h3>
                  <p className="text-sm text-white/60">锁定后悬浮窗将固定在当前位置，防止误触移动</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Layers className="w-5 h-5 mt-0.5 flex-shrink-0 text-oppo-orange" />
                <div>
                  <h3 className="font-medium mb-1">拖动调整</h3>
                  <p className="text-sm text-white/60">拖动调整悬浮窗位置，长按拖动可快速定位</p>
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Compatibility Stats */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.8 }}
          className="mt-8"
        >
          <div className="card p-8">
            <div className="grid md:grid-cols-3 gap-6 text-center">
              <div>
                <div className="text-4xl font-bold text-oppo-orange">95%+</div>
                <div className="text-white/60 text-sm mt-1">机型兼容率</div>
              </div>
              <div>
                <div className="text-4xl font-bold text-oppo-orange">&lt;200ms</div>
                <div className="text-white/60 text-sm mt-1">交互响应时间</div>
              </div>
              <div>
                <div className="text-4xl font-bold text-oppo-orange">3</div>
                <div className="text-white/60 text-sm mt-1">悬浮窗类型</div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
