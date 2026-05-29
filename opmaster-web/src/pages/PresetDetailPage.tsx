import { motion } from 'framer-motion'
import { useParams, useNavigate } from 'react-router-dom'
import { 
  ArrowLeft, 
  Heart, 
  Share2, 
  Download, 
  Copy, 
  Check, 
  Settings,
  Camera,
  Aperture,
  Clock,
  Sun,
  Palette
} from 'lucide-react'
import { useAppStore } from '../store/useAppStore'
import { useState, useCallback } from 'react'
import { 
  ColorOSCard, 
  ColorOSButton, 
  ColorOSSectionHeader,
  easeOppoEnter
} from '../components/common/ColorOSComponents'

// 参数卡片图标映射
const paramIcons: Record<string, React.ReactNode> = {
  iso: <Sun className="w-4 h-4" />,
  shutter_speed: <Clock className="w-4 h-4" />,
  aperture: <Aperture className="w-4 h-4" />,
  saturation: <Palette className="w-4 h-4" />,
  contrast: <Settings className="w-4 h-4" />,
  brightness: <Sun className="w-4 h-4" />
}

// 参数中文标签映射
const paramLabels: Record<string, string> = {
  iso: 'ISO 感光度',
  shutter_speed: '快门速度',
  aperture: '光圈值',
  saturation: '饱和度',
  contrast: '对比度',
  brightness: '亮度',
  filter: '滤镜风格',
  filter_intensity: '滤镜强度',
  soft_light: '柔光效果',
  tone_curve: '色调曲线',
  hncs: '哈苏自然色彩',
  master_hdr: '大师HDR',
  clarity: '锐度',
  vignette: '暗角效果',
  warm_cool: '冷暖色调',
  cyan_magenta: '青品色调',
  custom_wb: '自定义白平衡'
}

export default function PresetDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { presets, toggleFavorite, isFavorite, showToast } = useAppStore()
  const [copiedParams, setCopiedParams] = useState<string | null>(null)
  const [brightness, setBrightness] = useState(50)
  const [contrast, setContrast] = useState(50)
  const [saturation, setSaturation] = useState(50)

  const preset = presets.find(p => p.id === id)

  // 复制单个参数
  const copyParam = useCallback((key: string, value: string | number | boolean) => {
    const text = `${paramLabels[key] || key}: ${typeof value === 'boolean' ? (value ? '开启' : '关闭') : value}`
    navigator.clipboard.writeText(text).then(() => {
      setCopiedParams(key)
      showToast('参数已复制', 'success')
      setTimeout(() => setCopiedParams(null), 2000)
    })
  }, [showToast])

  // 复制所有参数
  const copyAllParams = useCallback(() => {
    if (!preset?.cameraParams) return

    const params = preset.cameraParams
    const paramText = Object.entries(params)
      .filter(([_, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${paramLabels[k] || k}: ${typeof v === 'boolean' ? (v ? '开启' : '关闭') : v}`)
      .join('\n')

    const fullText = `【${preset.name}】\n${preset.deviceModel}\n\n${paramText}`
    
    navigator.clipboard.writeText(fullText).then(() => {
      showToast('所有参数已复制', 'success')
    })
  }, [preset, showToast])

  // 一键应用到相机
  const applyToCamera = useCallback(() => {
    if (!preset) return
    
    // 尝试打开系统相机应用
    const cameraUrls = [
      'cameras://',           // iOS相机
      'oppo.camera://',      // OPPO相机
      'oneplus.camera://',   // OnePlus相机
      'realme.camera://',   // realme相机
      'intent://camera#Intent;scheme=camera;package=com.oppo.camera;end', // Android OPPO
      'intent://#Intent;scheme=camera;package=com.oneplus.camera;end',    // Android OnePlus
      'intent://#Intent;scheme=camera;package=com.oplus.camera;end',      // Android ColorOS
    ]
    
    // 尝试打开相机
    let cameraOpened = false
    for (const url of cameraUrls) {
      try {
        window.location.href = url
        cameraOpened = true
        break
      } catch (e) {
        console.log('无法打开:', url)
      }
    }
    
    // 如果无法打开相机，复制参数到剪贴板
    if (!cameraOpened && preset.cameraParams) {
      const params = preset.cameraParams
      const paramText = Object.entries(params)
        .filter(([_, v]) => v !== undefined && v !== null && v !== '')
        .map(([k, v]) => `${paramLabels[k] || k}: ${typeof v === 'boolean' ? (v ? '开启' : '关闭') : v}`)
        .join('\n')
      
      const fullText = `【${preset.name}】\n${preset.deviceModel}\n\n${paramText}`
      
      navigator.clipboard.writeText(fullText).then(() => {
        showToast('参数已复制，请在相机大师模式中粘贴', 'success')
      }).catch(() => {
        showToast(`已选择 "${preset.name}" 预设`, 'success')
      })
    } else {
      showToast(`已应用 "${preset.name}" 预设参数`, 'success')
    }
    
    // 提供触觉反馈（如果支持）
    if ('vibrate' in navigator) {
      navigator.vibrate(10)
    }
  }, [preset, showToast])

  if (!preset) {
    return (
      <div className="min-h-screen pt-20 flex items-center justify-center bg-[#0F0F0F]">
        <motion.div 
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="text-center p-8"
        >
          <div className="text-6xl mb-4">📷</div>
          <h2 className="text-xl font-bold text-white mb-2">预设不存在</h2>
          <p className="text-white/60 mb-6">该预设可能已被移除或不存在</p>
          <ColorOSButton onClick={() => navigate(-1)} icon={<ArrowLeft className="w-5 h-5" />}>
            返回上一页
          </ColorOSButton>
        </motion.div>
      </div>
    )
  }

  const hasCameraParams = preset.cameraParams && Object.keys(preset.cameraParams).length > 0

  return (
    <div className="min-h-screen pt-20 pb-24 px-4 sm:px-6 lg:px-8 bg-[#0F0F0F]">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between mb-8"
        >
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-2 text-white/70 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span className="text-sm font-medium">返回</span>
          </button>
          
          <div className="flex items-center gap-3">
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={() => toggleFavorite(preset.id)}
              className="p-3 bg-white/5 hover:bg-white/10 rounded-2xl border border-white/10 transition-all"
            >
              <Heart
                className={`w-5 h-5 ${
                  isFavorite(preset.id) ? 'text-red-500 fill-red-500' : 'text-white'
                }`}
              />
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              className="p-3 bg-white/5 hover:bg-white/10 rounded-2xl border border-white/10 transition-all"
            >
              <Share2 className="w-5 h-5 text-white" />
            </motion.button>
          </div>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* 左侧：预览区域 */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4, ease: easeOppoEnter }}
            className="space-y-6"
          >
            {/* 主预览 */}
            <ColorOSCard variant="glass" className="p-6">
              <div className="relative aspect-[4/3] rounded-2xl overflow-hidden shadow-2xl">
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover"
                  style={{
                    filter: `brightness(${brightness / 50}) contrast(${contrast / 50}) saturate(${saturation / 50})`
                  }}
                />
                
                {/* 渐变叠加 */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
                
                {/* 标签 */}
                <div className="absolute top-4 left-4 flex items-center gap-2 flex-wrap">
                  {preset.cameraParams?.hncs && (
                    <div className="bg-[#D4A574]/20 border border-[#D4A574]/30 px-3 py-1.5 rounded-full flex items-center gap-1.5">
                      <span className="text-xs font-bold text-[#D4A574]">HNCS</span>
                    </div>
                  )}
                  {preset.category && (
                    <div className="bg-white/10 backdrop-blur-sm px-3 py-1.5 rounded-full">
                      <span className="text-xs font-medium text-white">{preset.category}</span>
                    </div>
                  )}
                  <div className="bg-white/10 backdrop-blur-sm px-3 py-1.5 rounded-full">
                    <span className="text-xs font-medium text-white">{preset.deviceModel}</span>
                  </div>
                </div>
              </div>

              {/* 实时调节预览 */}
              <div className="mt-6 space-y-4">
                <ColorOSSectionHeader title="实时预览调节" subtitle="拖动滑块查看效果" />
                
                <div className="space-y-5">
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <label className="text-sm font-medium text-white/70">亮度</label>
                      <span className="text-sm text-[#FF6B35]">{brightness}%</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={brightness}
                      onChange={(e) => setBrightness(Number(e.target.value))}
                      className="w-full h-2.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
                    />
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <label className="text-sm font-medium text-white/70">对比度</label>
                      <span className="text-sm text-[#FF6B35]">{contrast}%</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={contrast}
                      onChange={(e) => setContrast(Number(e.target.value))}
                      className="w-full h-2.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
                    />
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <label className="text-sm font-medium text-white/70">饱和度</label>
                      <span className="text-sm text-[#FF6B35]">{saturation}%</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={saturation}
                      onChange={(e) => setSaturation(Number(e.target.value))}
                      className="w-full h-2.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
                    />
                  </div>
                </div>
              </div>
            </ColorOSCard>

            {/* 示例图库 */}
            {preset.galleryImages && preset.galleryImages.length > 0 && (
              <ColorOSCard className="p-6">
                <ColorOSSectionHeader title="示例图库" subtitle={`${preset.galleryImages.length}张效果图`} />
                <div className="grid grid-cols-3 gap-3">
                  {preset.galleryImages.map((img, idx) => (
                    <motion.div
                      key={idx}
                      initial={{ opacity: 0, scale: 0.9 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: 0.2 + idx * 0.05 }}
                      whileHover={{ scale: 1.05 }}
                      className="aspect-square rounded-xl overflow-hidden"
                    >
                      <img
                        src={img}
                        alt={`${preset.name} 示例 ${idx + 1}`}
                        className="w-full h-full object-cover"
                      />
                    </motion.div>
                  ))}
                </div>
              </ColorOSCard>
            )}

            {/* 操作按钮 */}
            <div className="flex gap-3">
              <ColorOSButton
                variant="primary"
                onClick={applyToCamera}
                fullWidth
                icon={<Camera className="w-5 h-5" />}
              >
                一键应用到相机
              </ColorOSButton>
              <ColorOSButton
                variant="secondary"
                onClick={copyAllParams}
                disabled={!hasCameraParams}
                icon={<Download className="w-5 h-5" />}
              >
                复制全部参数
              </ColorOSButton>
            </div>
          </motion.div>

          {/* 右侧：详细信息 */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4, delay: 0.1, ease: easeOppoEnter }}
            className="space-y-6"
          >
            {/* 基本信息 */}
            <ColorOSCard className="p-6">
              <h1 className="text-2xl font-bold text-white mb-2">{preset.name}</h1>
              <p className="text-white/60 mb-4">
                适用于 {preset.deviceModel}
                {preset.author && (
                  <span className="text-[#00D7A0] ml-2">by {preset.author}</span>
                )}
              </p>
              
              {/* 标签 */}
              {preset.tags && preset.tags.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-4">
                  {preset.tags.map((tag, idx) => (
                    <motion.span
                      key={tag}
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: 0.3 + idx * 0.05 }}
                      className="text-xs bg-white/10 px-2.5 py-1 rounded-full text-white/70"
                    >
                      {tag}
                    </motion.span>
                  ))}
                </div>
              )}

              {/* 描述 */}
              {preset.description && (
                <div className="mb-4">
                  <h3 className="text-sm font-bold text-[#D4A574] mb-1">{preset.description.title}</h3>
                  <p className="text-sm text-white/70 leading-relaxed">{preset.description.content}</p>
                </div>
              )}
            </ColorOSCard>

            {/* 相机参数 */}
            {hasCameraParams && (
              <ColorOSCard className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <ColorOSSectionHeader 
                    title="哈苏大师模式参数" 
                    subtitle={`${Object.entries(preset.cameraParams!).filter(([_, v]) => v !== undefined && v !== null).length}个参数`}
                  />
                  <ColorOSButton
                    variant="secondary"
                    size="sm"
                    onClick={copyAllParams}
                    icon={<Copy className="w-4 h-4" />}
                  >
                    全部复制
                  </ColorOSButton>
                </div>
                
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {(Object.entries(preset.cameraParams!) as [string, string | number | boolean | undefined][]).filter(([_, v]) => 
                    v !== undefined && v !== null && v !== ''
                  ).map(([key, value], idx) => (
                    <motion.div
                      key={key}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.3 + idx * 0.03 }}
                      whileHover={{ scale: 1.02 }}
                      className="bg-white/5 rounded-2xl p-4 border border-white/10 hover:border-[#D4A574]/30 transition-all group cursor-pointer"
                      onClick={() => copyParam(key, value as string | number | boolean)}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 rounded-xl bg-[#D4A574]/10 flex items-center justify-center text-[#D4A574]">
                            {paramIcons[key] || <Settings className="w-4 h-4" />}
                          </div>
                          <span className="text-xs text-white/50">
                            {paramLabels[key] || key}
                          </span>
                        </div>
                        <motion.button
                          whileTap={{ scale: 0.95 }}
                          className="p-1.5 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-white/10 transition-all"
                        >
                          {copiedParams === key ? (
                            <Check className="w-4 h-4 text-[#00D7A0]" />
                          ) : (
                            <Copy className="w-4 h-4 text-white/40" />
                          )}
                        </motion.button>
                      </div>
                      <div className="mt-2 text-sm font-bold text-white">
                        {typeof value === 'boolean' ? (value ? '开启' : '关闭') : String(value)}
                        {key === 'custom_wb' && value !== undefined && value !== null && 'K'}
                        {key === 'filter_intensity' && '%'}
                      </div>
                    </motion.div>
                  ))}
                </div>
              </ColorOSCard>
            )}

            {/* 详细说明 */}
            {preset.sections && preset.sections.length > 0 && (
              <ColorOSCard className="p-6">
                <ColorOSSectionHeader title="详细说明" subtitle="使用技巧与注意事项" />
                <div className="space-y-4">
                  {preset.sections.map((section, idx) => (
                    <motion.div
                      key={idx}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.4 + idx * 0.05 }}
                    >
                      <h3 className="text-sm font-bold text-[#D4A574] mb-2">{section.title}</h3>
                      <div className="space-y-2">
                        {section.items.map((item, itemIdx) => (
                          <div key={itemIdx} className="flex justify-between bg-white/5 px-3.5 py-2.5 rounded-xl">
                            <span className="text-sm text-white/60">{item.label}</span>
                            <span className="text-sm font-medium text-white">{item.value}</span>
                          </div>
                        ))}
                      </div>
                    </motion.div>
                  ))}
                </div>
              </ColorOSCard>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  )
}
