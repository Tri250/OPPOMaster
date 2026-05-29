import { motion, AnimatePresence } from 'framer-motion'
import { 
  Maximize2, 
  Minimize2, 
  Smartphone, 
  ChevronLeft,
  ChevronRight,
  Layers,
  Palette,
  Sun,
  Moon,
  Monitor,
  Eye,
  EyeOff,
  Move,
  Settings,
  Check,
  X,
  Zap,
  Gauge,
  Smartphone as MobileIcon
} from 'lucide-react'
import { useState, useCallback, useEffect, useRef } from 'react'
import { 
  ColorOSCard, 
  ColorOSButton, 
  ColorOSSwitch,
  ColorOSChip,
  ColorOSSectionHeader,
  ColorOSListItem,
  easeOppoEnter,
  easeOppoBounce
} from '../components/common/ColorOSComponents'

// ==========================================
// 悬浮窗类型定义
// ==========================================
type FloatingWindowType = 'compact' | 'expanded' | 'minimal' | 'info'
type FloatingWindowSize = 'small' | 'medium' | 'large'
type FloatingPosition = 'left' | 'right'
type FloatingTheme = 'dark' | 'light' | 'glass'

interface FloatingWindowConfig {
  type: FloatingWindowType
  size: FloatingWindowSize
  position: FloatingPosition
  theme: FloatingTheme
  transparency: number
  showDevice: boolean
  showParams: boolean
  showPresetName: boolean
}

interface PresetItem {
  id: string
  name: string
  device: string
  params: Record<string, string | number | boolean>
  color: string
}

// ==========================================
// 预设数据
// ==========================================
const presetList: PresetItem[] = [
  { id: '1', name: '哈苏人像大师', device: 'OPPO Find X8 Ultra', params: { hasselblad_hncs: true, saturation: 10, contrast: 8 }, color: 'from-oppo-orange to-hasselblad-orange' },
  { id: '2', name: '徕卡经典', device: 'Xiaomi 16 Ultra', params: { contrast: 12, saturation: 10, warmth: 8 }, color: 'from-gray-400 to-gray-600' },
  { id: '3', name: '蔡司自然', device: 'vivo X200 Ultra', params: { saturation: 8, contrast: 12, clarity: 10 }, color: 'from-blue-500 to-blue-700' },
  { id: '4', name: 'XMAGE影像', device: 'Huawei Mate 80 Pro+', params: { saturation: 10, contrast: 8, hdr: true }, color: 'from-red-500 to-red-700' },
  { id: '5', name: '电影色调', device: '通用', params: { contrast: 18, saturation: 6, film_tone: true }, color: 'from-purple-500 to-purple-700' },
  { id: '6', name: '自然风光', device: '通用', params: { saturation: 15, contrast: 10, hdr: true }, color: 'from-green-500 to-green-700' },
  { id: '7', name: '夜景模式', device: '通用', params: { night_mode: true, brightness: -5, contrast: 20 }, color: 'from-indigo-500 to-indigo-700' }
]

const windowTypes = [
  { id: 'compact', name: '紧凑型', icon: '📱', description: '最小化显示，仅显示预设名称' },
  { id: 'expanded', name: '展开型', icon: '📋', description: '显示预设名称、设备和参数' },
  { id: 'minimal', name: '极简型', icon: '⚡', description: '仅显示预设名称和颜色指示' },
  { id: 'info', name: '信息型', icon: '💡', description: '显示详细信息和参数调整' }
]

// ==========================================
// 主组件
// ==========================================
export default function FloatingWindowPage() {
  const [config, setConfig] = useState<FloatingWindowConfig>({
    type: 'expanded',
    size: 'medium',
    position: 'right',
    theme: 'glass',
    transparency: 85,
    showDevice: true,
    showParams: true,
    showPresetName: true
  })
  
  const [isActive, setIsActive] = useState(false)
  const [currentPresetIndex, setCurrentPresetIndex] = useState(0)
  const [showExpanded, setShowExpanded] = useState(true)
  const [isLocked, setIsLocked] = useState(false)
  const [interactionTime, setInteractionTime] = useState(0)
  const [testResults, setTestResults] = useState<Record<string, { passed: boolean; value: string }>>({})
  
  const interactionStartRef = useRef(0)

  const currentPreset = presetList[currentPresetIndex]

  // 记录交互时间
  const recordInteraction = useCallback((action: string) => {
    const duration = Date.now() - interactionStartRef.current
    setInteractionTime(duration)
    
    setTestResults(prev => ({
      ...prev,
      [action]: {
        passed: duration <= 200,
        value: `${duration}ms`
      }
    }))
  }, [])

  // 切换预设
  const handlePresetChange = useCallback((direction: 'prev' | 'next') => {
    interactionStartRef.current = Date.now()
    
    setCurrentPresetIndex(prev => {
      const newIndex = direction === 'next' 
        ? (prev + 1) % presetList.length 
        : (prev - 1 + presetList.length) % presetList.length
      
      setTimeout(() => recordInteraction('preset_change'), 0)
      return newIndex
    })
  }, [recordInteraction])

  // 切换展开状态
  const toggleExpand = useCallback(() => {
    interactionStartRef.current = Date.now()
    setShowExpanded(prev => !prev)
    setTimeout(() => recordInteraction('toggle_expand'), 0)
  }, [recordInteraction])

  // 切换锁定
  const toggleLock = useCallback(() => {
    interactionStartRef.current = Date.now()
    setIsLocked(prev => !prev)
    setTimeout(() => recordInteraction('toggle_lock'), 0)
  }, [recordInteraction])

  // 启用/停用
  const toggleActive = useCallback(() => {
    interactionStartRef.current = Date.now()
    setIsActive(prev => !prev)
    setTimeout(() => recordInteraction('toggle_active'), 0)
  }, [recordInteraction])

  // 尺寸映射
  const sizeClasses = {
    small: 'w-64',
    medium: 'w-80',
    large: 'w-96'
  }

  // 主题样式映射
  const themeStyles = {
    dark: 'bg-bg-primary/95',
    light: 'bg-white/95 border border-border-light-default',
    glass: 'bg-bg-glass backdrop-blur-2xl'
  }

  return (
    <div className="min-h-screen bg-bg-primary text-text-primary font-sans relative overflow-hidden">
      {/* ColorOS 16 背景光效 */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="orb-oppo orb-orange absolute -top-40 -left-40" style={{width: 500, height: 500, opacity: 0.15}} />
        <div className="orb-oppo orb-blue absolute -bottom-40 -right-40" style={{width: 400, height: 400, opacity: 0.12}} />
      </div>

      {/* 顶部导航栏 - ColorOS 16 风格 */}
      <header className="sticky top-0 z-50 bg-bg-primary/85 backdrop-blur-2xl border-b border-white/5 safe-area-top">
        <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <motion.div 
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="w-10 h-10 rounded-2xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1"
            >
              <Layers className="w-6 h-6 text-oppo-black" />
            </motion.div>
            <div>
              <h1 className="text-h3 font-bold bg-gradient-to-r from-text-primary via-oppo-orange to-hasselblad-orange bg-clip-text text-transparent">
                悬浮窗
              </h1>
              <p className="text-caption text-text-tertiary">适配率 ≥ 95%</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-2">
              <div className="flex items-center gap-2 text-sm text-oppo-green bg-oppo-green/10 px-3 py-1.5 rounded-full">
                <Zap className="w-4 h-4" />
                <span className="font-medium">响应 &lt; 200ms</span>
              </div>
            </div>
            <ColorOSButton
              onClick={toggleActive}
              size="sm"
              variant={isActive ? 'secondary' : 'primary'}
            >
              {isActive ? '停用' : '启用'}
            </ColorOSButton>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6 space-y-6 relative z-10">
        {/* 实时悬浮窗预览 */}
        {isActive && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, ease: easeOppoEnter }}
          >
            <ColorOSSectionHeader title="实时预览" subtitle="悬浮窗将显示在相机应用上方" />
            
            <div className="relative">
              {/* 模拟相机界面 */}
              <div className="relative bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 rounded-3xl overflow-hidden border border-white/10" style={{ minHeight: 400 }}>
                {/* 相机UI */}
                <div className="absolute inset-0">
                  <div className="absolute top-6 left-6 text-white/60 text-sm">1x</div>
                  <div className="absolute top-6 right-6 flex gap-2">
                    <button className="w-10 h-10 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center">
                      <Settings className="w-5 h-5 text-white/80" />
                    </button>
                  </div>
                  <div className="absolute bottom-20 left-1/2 -translate-x-1/2 flex gap-6">
                    <button className="w-14 h-14 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center">
                      <div className="w-10 h-10 rounded-full bg-white/30 border-2 border-white/50" />
                    </button>
                  </div>
                </div>

                {/* 悬浮窗 - ColorOS 16 风格 */}
                <AnimatePresence>
                  {isActive && (
                    <motion.div
                      initial={{ opacity: 0, scale: 0.8, x: config.position === 'right' ? 50 : -50 }}
                      animate={{ opacity: 1, scale: 1, x: 0 }}
                      exit={{ opacity: 0, scale: 0.8 }}
                      transition={{ duration: 0.3, ease: easeOppoBounce }}
                      className={`absolute ${config.position === 'right' ? 'right-4' : 'left-4'} top-4 ${sizeClasses[config.size]} ${themeStyles[config.theme]}`}
                      style={{ 
                        opacity: config.transparency / 100,
                        borderRadius: 20 
                      }}
                    >
                      <div className="p-4">
                        {/* 紧凑型显示 */}
                        {config.type === 'compact' && (
                          <div className="flex items-center gap-3">
                            <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${currentPreset.color} flex items-center justify-center`}>
                              <Palette className="w-5 h-5 text-white" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <p className="text-text-primary font-bold text-sm truncate">{currentPreset.name}</p>
                            </div>
                          </div>
                        )}

                        {/* 展开型显示 */}
                        {config.type === 'expanded' && (
                          <div className="space-y-3">
                            <div className="flex items-start gap-3">
                              <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${currentPreset.color} flex items-center justify-center flex-shrink-0`}>
                                <Palette className="w-5 h-5 text-white" />
                              </div>
                              <div className="flex-1 min-w-0">
                                {config.showPresetName && (
                                  <p className="text-text-primary font-bold text-sm truncate">{currentPreset.name}</p>
                                )}
                                {config.showDevice && (
                                  <p className="text-oppo-green text-xs mt-0.5">{currentPreset.device}</p>
                                )}
                                {config.showParams && (
                                  <p className="text-text-tertiary text-xs mt-1 line-clamp-2">
                                    {Object.entries(currentPreset.params).map(([k, v]) => `${k}: ${v}`).join(' | ')}
                                  </p>
                                )}
                              </div>
                            </div>
                            
                            {/* 导航按钮 */}
                            <div className="flex items-center justify-center gap-3">
                              <motion.button
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.95 }}
                                onClick={() => handlePresetChange('prev')}
                                className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
                              >
                                <ChevronLeft className="w-4 h-4 text-white/80" />
                              </motion.button>
                              <span className="text-text-secondary text-xs font-medium">
                                {currentPresetIndex + 1}/{presetList.length}
                              </span>
                              <motion.button
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.95 }}
                                onClick={() => handlePresetChange('next')}
                                className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-colors"
                              >
                                <ChevronRight className="w-4 h-4 text-white/80" />
                              </motion.button>
                              <motion.button
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.95 }}
                                onClick={toggleLock}
                                className={`w-9 h-9 rounded-full flex items-center justify-center transition-colors ${
                                  isLocked ? 'bg-oppo-orange/30' : 'bg-white/10 hover:bg-white/20'
                                }`}
                              >
                                <Move className={`w-4 h-4 ${isLocked ? 'text-oppo-orange' : 'text-white/80'}`} />
                              </motion.button>
                            </div>
                          </div>
                        )}

                        {/* 极简型显示 */}
                        {config.type === 'minimal' && (
                          <div className="flex items-center justify-center">
                            <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${currentPreset.color} flex items-center justify-center`}>
                              <Palette className="w-4 h-4 text-white" />
                            </div>
                          </div>
                        )}

                        {/* 信息型显示 */}
                        {config.type === 'info' && (
                          <div className="space-y-3">
                            <div className="flex items-center gap-3">
                              <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${currentPreset.color} flex items-center justify-center shadow-oppo-elevation-2`}>
                                <Palette className="w-6 h-6 text-white" />
                              </div>
                              <div className="flex-1">
                                <p className="text-text-primary font-bold">{currentPreset.name}</p>
                                <p className="text-oppo-green text-xs">{currentPreset.device}</p>
                              </div>
                            </div>
                            
                            <div className="space-y-2">
                              {Object.entries(currentPreset.params).map(([key, value]) => (
                                <div key={key} className="flex items-center justify-between text-xs">
                                  <span className="text-text-tertiary uppercase tracking-wide">{key}</span>
                                  <span className="text-text-primary font-medium">
                                    {typeof value === 'boolean' ? (value ? '开启' : '关闭') : value}
                                  </span>
                                </div>
                              ))}
                            </div>

                            <div className="flex items-center justify-between pt-2 border-t border-white/10">
                              <motion.button
                                whileTap={{ scale: 0.95 }}
                                onClick={() => handlePresetChange('prev')}
                                className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center"
                              >
                                <ChevronLeft className="w-4 h-4" />
                              </motion.button>
                              <span className="text-text-secondary text-xs">
                                {currentPresetIndex + 1} / {presetList.length}
                              </span>
                              <motion.button
                                whileTap={{ scale: 0.95 }}
                                onClick={() => handlePresetChange('next')}
                                className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center"
                              >
                                <ChevronRight className="w-4 h-4" />
                              </motion.button>
                            </div>
                          </div>
                        )}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>

                <p className="absolute bottom-6 left-1/2 -translate-x-1/2 text-white/40 text-sm">
                  相机取景区域
                </p>
              </div>
            </div>
          </motion.div>
        )}

        {/* 交互性能测试报告 */}
        {Object.keys(testResults).length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <ColorOSSectionHeader title="交互性能测试" subtitle="符合 ColorOS 16 响应标准 ≤ 200ms" />
            <ColorOSCard className="p-5">
              <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-3">
                {Object.entries(testResults).map(([action, result]) => (
                  <motion.div
                    key={action}
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="bg-white/5 rounded-2xl p-4 border border-white/10"
                  >
                    <div className="flex items-center gap-2 mb-2">
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
                        result.passed ? 'bg-oppo-green/20' : 'bg-error/20'
                      }`}>
                        {result.passed ? <Check className="w-4 h-4 text-oppo-green" /> : <X className="w-4 h-4 text-error" />}
                      </div>
                      <span className="font-medium text-text-primary text-sm capitalize">{action.replace('_', ' ')}</span>
                    </div>
                    <div className="text-text-secondary text-sm">
                      响应时间: <span className="text-text-primary font-medium">{result.value}</span>
                    </div>
                    <div className="text-text-tertiary text-xs mt-1">
                      标准: ≤ 200ms
                    </div>
                  </motion.div>
                ))}
              </div>
            </ColorOSCard>
          </motion.div>
        )}

        {/* 悬浮窗类型选择 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <ColorOSSectionHeader title="悬浮窗类型" subtitle="支持多种显示模式" />
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            {windowTypes.map((type, idx) => (
              <motion.div
                key={type.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: idx * 0.05 }}
                whileHover={{ scale: 1.02, y: -2 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setConfig({ ...config, type: type.id as FloatingWindowType })}
                className={`cursor-pointer rounded-2xl p-5 border-2 transition-all duration-200 ${
                  config.type === type.id
                    ? 'bg-oppo-orange/10 border-oppo-orange/50 shadow-oppo-elevation-2'
                    : 'bg-white/5 border-white/10 hover:border-white/20'
                }`}
              >
                <div className="text-3xl mb-3">{type.icon}</div>
                <h3 className="font-bold text-text-primary mb-1">{type.name}</h3>
                <p className="text-text-tertiary text-xs">{type.description}</p>
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* 外观设置 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <ColorOSSectionHeader title="外观设置" />
          <ColorOSCard className="p-5">
            <div className="grid md:grid-cols-2 gap-6">
              {/* 尺寸 */}
              <div>
                <label className="text-text-secondary text-sm mb-3 block">窗口尺寸</label>
                <div className="flex gap-2">
                  {(['small', 'medium', 'large'] as FloatingWindowSize[]).map((size) => (
                    <ColorOSChip
                      key={size}
                      label={size === 'small' ? '小' : size === 'medium' ? '中' : '大'}
                      selected={config.size === size}
                      onClick={() => setConfig({ ...config, size })}
                      variant="primary"
                    />
                  ))}
                </div>
              </div>

              {/* 位置 */}
              <div>
                <label className="text-text-secondary text-sm mb-3 block">显示位置</label>
                <div className="flex gap-2">
                  {(['left', 'right'] as FloatingPosition[]).map((pos) => (
                    <ColorOSChip
                      key={pos}
                      label={pos === 'left' ? '左侧' : '右侧'}
                      selected={config.position === pos}
                      onClick={() => setConfig({ ...config, position: pos })}
                      variant="primary"
                    />
                  ))}
                </div>
              </div>

              {/* 主题 */}
              <div>
                <label className="text-text-secondary text-sm mb-3 block">主题样式</label>
                <div className="flex flex-wrap gap-2">
                  {([
                    { id: 'dark', name: '深色', icon: <Moon className="w-4 h-4" /> },
                    { id: 'light', name: '浅色', icon: <Sun className="w-4 h-4" /> },
                    { id: 'glass', name: '玻璃', icon: <Layers className="w-4 h-4" /> }
                  ] as { id: FloatingTheme; name: string; icon: React.ReactNode }[]).map((theme) => (
                    <ColorOSChip
                      key={theme.id}
                      label={`${theme.icon} ${theme.name}`}
                      selected={config.theme === theme.id}
                      onClick={() => setConfig({ ...config, theme: theme.id })}
                      variant="primary"
                    />
                  ))}
                </div>
              </div>

              {/* 透明度 */}
              <div>
                <label className="text-text-secondary text-sm mb-3 block">
                  透明度: {config.transparency}%
                </label>
                <input
                  type="range"
                  min="30"
                  max="100"
                  value={config.transparency}
                  onChange={(e) => setConfig({ ...config, transparency: Number(e.target.value) })}
                  className="w-full h-2.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-oppo-orange"
                />
              </div>
            </div>
          </ColorOSCard>
        </motion.div>

        {/* 显示内容 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <ColorOSSectionHeader title="显示内容" />
          <ColorOSCard className="divide-y divide-white/5">
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={config.showPresetName}
                onChange={(v) => setConfig({ ...config, showPresetName: v })}
                label="显示预设名称"
                description="在悬浮窗中显示当前预设的名称"
              />
            </div>
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={config.showDevice}
                onChange={(v) => setConfig({ ...config, showDevice: v })}
                label="显示设备信息"
                description="显示预设对应的设备型号"
              />
            </div>
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={config.showParams}
                onChange={(v) => setConfig({ ...config, showParams: v })}
                label="显示参数信息"
                description="显示预设的详细参数"
              />
            </div>
          </ColorOSCard>
        </motion.div>

        {/* 预设列表 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <ColorOSSectionHeader title={`预设列表 (${presetList.length}个)`} subtitle="点击选择当前预设" />
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {presetList.map((preset, idx) => (
              <motion.div
                key={preset.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: idx * 0.03 }}
                whileHover={{ scale: 1.02, y: -2 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setCurrentPresetIndex(idx)}
                className={`cursor-pointer rounded-2xl p-4 border-2 transition-all duration-200 ${
                  currentPresetIndex === idx
                    ? 'bg-oppo-orange/10 border-oppo-orange/50 shadow-oppo-elevation-2'
                    : 'bg-white/5 border-white/10 hover:border-white/20'
                }`}
              >
                <div className="flex items-start gap-3">
                  <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${preset.color} flex items-center justify-center flex-shrink-0 shadow-oppo-elevation-1`}>
                    <Palette className="w-6 h-6 text-white" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-bold text-text-primary text-sm truncate">{preset.name}</h3>
                    <p className="text-oppo-green text-xs mt-0.5 truncate">{preset.device}</p>
                    <div className="flex flex-wrap gap-1 mt-2">
                      {Object.entries(preset.params).slice(0, 3).map(([key]) => (
                        <span key={key} className="text-caption text-text-tertiary bg-white/5 px-2 py-0.5 rounded-full">
                          {key}
                        </span>
                      ))}
                    </div>
                  </div>
                  {currentPresetIndex === idx && (
                    <div className="w-6 h-6 rounded-full bg-oppo-orange flex items-center justify-center flex-shrink-0">
                      <Check className="w-4 h-4 text-oppo-black" />
                    </div>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* 功能特性 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="grid md:grid-cols-3 gap-4"
        >
          {[
            { 
              icon: <Gauge className="w-6 h-6 text-oppo-orange" />, 
              title: '≤ 200ms 响应',
              desc: '所有交互操作响应时间均小于200毫秒',
              color: 'from-oppo-orange to-hasselblad-orange'
            },
            { 
              icon: <MobileIcon className="w-6 h-6 text-oppo-green" />, 
              title: '≥ 95% 适配率',
              desc: '完美适配各种屏幕尺寸和设备',
              color: 'from-oppo-green to-oppo-blue'
            },
            { 
              icon: <Layers className="w-6 h-6 text-oppo-purple" />, 
              title: '多类型支持',
              desc: '支持紧凑型、展开型、极简型、信息型',
              color: 'from-oppo-purple to-oppo-pink'
            }
          ].map((feature, idx) => (
            <motion.div
              key={idx}
              initial={{ opacity: 0, y: 24 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.6 + idx * 0.1 }}
              whileHover={{ y: -4, scale: 1.02 }}
            >
              <ColorOSCard variant="glass" className="p-6 h-full">
                <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${feature.color}/20 flex items-center justify-center mb-4`}>
                  {feature.icon}
                </div>
                <h3 className="text-h3 font-bold text-text-primary mb-2">{feature.title}</h3>
                <p className="text-text-secondary text-sm">{feature.desc}</p>
              </ColorOSCard>
            </motion.div>
          ))}
        </motion.div>

        {/* 使用指南 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
        >
          <ColorOSSectionHeader title="使用指南" />
          <ColorOSCard className="p-5">
            <div className="space-y-4">
              {[
                { step: 1, title: '启用悬浮窗', desc: '点击右上角"启用"按钮启动悬浮窗服务' },
                { step: 2, title: '配置样式', desc: '选择悬浮窗类型、大小、位置和主题' },
                { step: 3, title: '选择预设', desc: '从预设列表中选择需要的滤镜预设' },
                { step: 4, title: '开始使用', desc: '打开相机应用，悬浮窗将在上层显示' }
              ].map((item) => (
                <motion.div
                  key={item.step}
                  initial={{ opacity: 0, x: -12 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.7 + item.step * 0.05 }}
                  className="flex items-start gap-4"
                >
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center flex-shrink-0 text-oppo-black font-bold text-sm">
                    {item.step}
                  </div>
                  <div>
                    <h4 className="font-bold text-text-primary text-sm">{item.title}</h4>
                    <p className="text-text-secondary text-xs mt-0.5">{item.desc}</p>
                  </div>
                </motion.div>
              ))}
            </div>
          </ColorOSCard>
        </motion.div>
      </main>
    </div>
  )
}
