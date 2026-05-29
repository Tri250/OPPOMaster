import { motion, AnimatePresence } from 'framer-motion'
import { 
  Sparkles, Camera, ImagePlus, RefreshCw, 
  Check, Save, ArrowRight, Wand2,
  Sun, Droplets, Palette, Focus, Contrast,
  CircleDot, Layers, Download, Share2
} from 'lucide-react'
import { useState } from 'react'
import { 
  ColorOSCard, ColorOSButton, ColorOSSlider,
  ColorOSSectionHeader, ColorOSAnimations, ColorOSChip
} from '../components/common/ColorOSComponents'

interface AdjustmentParams {
  contrast: number
  saturation: number
  temperature: number
  exposure: number
  highlights: number
  shadows: number
  vibrance: number
  sharpness: number
}

const defaultParams: AdjustmentParams = {
  contrast: 0,
  saturation: 0,
  temperature: 0,
  exposure: 0,
  highlights: 0,
  shadows: 0,
  vibrance: 0,
  sharpness: 0
}

const presetPresets = [
  { id: 'portrait', name: '人像优化', icon: '👤', desc: '优化肤色和细节' },
  { id: 'landscape', name: '风景增强', icon: '🏔️', desc: '增强天空和绿色' },
  { id: 'night', name: '夜景降噪', icon: '🌙', desc: '降低噪点提亮暗部' },
  { id: 'food', name: '美食鲜艳', icon: '🍜', desc: '提升色彩饱和度' },
]

export default function AiFineTunePage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null)
  const [isProcessing, setIsProcessing] = useState(false)
  const [showResult, setShowResult] = useState(false)
  const [adjustmentParams, setAdjustmentParams] = useState<AdjustmentParams>(defaultParams)
  const [activePreset, setActivePreset] = useState<string | null>(null)
  const [compareMode, setCompareMode] = useState(false)

  const handleSelectImage = () => {
    const seed = Math.random().toString(36).substring(7)
    setSelectedImage(`https://picsum.photos/seed/${seed}/800/600`)
    setShowResult(false)
    setAdjustmentParams(defaultParams)
    setActivePreset(null)
  }

  const handleAiFineTune = async () => {
    if (!selectedImage) return
    setIsProcessing(true)
    await new Promise(resolve => setTimeout(resolve, 2000))
    setAdjustmentParams({
      contrast: 12,
      saturation: 8,
      temperature: -5,
      exposure: 3,
      highlights: -15,
      shadows: 20,
      vibrance: 10,
      sharpness: 5
    })
    setIsProcessing(false)
    setShowResult(true)
  }

  const handlePresetSelect = (presetId: string) => {
    setActivePreset(presetId)
    const presetAdjustments: Record<string, AdjustmentParams> = {
      portrait: { contrast: 8, saturation: 5, temperature: 3, exposure: 2, highlights: -10, shadows: 15, vibrance: 8, sharpness: 3 },
      landscape: { contrast: 15, saturation: 12, temperature: -8, exposure: 5, highlights: -20, shadows: 25, vibrance: 15, sharpness: 8 },
      night: { contrast: 5, saturation: -3, temperature: -10, exposure: 10, highlights: 5, shadows: 30, vibrance: 0, sharpness: -5 },
      food: { contrast: 10, saturation: 20, temperature: 5, exposure: 5, highlights: -5, shadows: 10, vibrance: 25, sharpness: 10 },
    }
    setAdjustmentParams(presetAdjustments[presetId] || defaultParams)
    setShowResult(true)
  }

  const updateParam = (key: keyof AdjustmentParams, value: number) => {
    setAdjustmentParams(prev => ({ ...prev, [key]: value }))
    setShowResult(true)
  }

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-80 h-80 top-1/4 -left-40 animate-float" />
        <div className="orb-oppo orb-2 w-64 h-64 bottom-1/4 -right-32 animate-float" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative max-w-4xl mx-auto px-4 py-8">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
        >
          <div className="flex items-center gap-3 mb-8">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-aurora-purple to-ocean-blue flex items-center justify-center">
              <Wand2 className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">AI 样张微调</h1>
              <p className="text-text-tertiary text-sm">智能优化您的照片参数</p>
            </div>
          </div>

          <div className="grid lg:grid-cols-2 gap-6">
            <motion.section
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <ColorOSSectionHeader title="样张预览" subtitle="选择需要优化的照片" />
              
              <ColorOSCard 
                variant="default" 
                interactive={!selectedImage}
                className={`aspect-[4/3] relative overflow-hidden ${!selectedImage ? 'cursor-pointer' : ''}`}
              >
                {selectedImage ? (
                  <div className="relative w-full h-full">
                    <img 
                      src={selectedImage} 
                      alt="预览" 
                      className="w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-deep-space/80 via-transparent to-transparent" />
                    
                    <div className="absolute top-4 right-4 flex gap-2">
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={handleSelectImage}
                        className="w-10 h-10 rounded-xl bg-black/50 backdrop-blur-sm flex items-center justify-center text-white"
                      >
                        <RefreshCw className="w-5 h-5" />
                      </motion.button>
                    </div>

                    {showResult && (
                      <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="absolute bottom-4 left-4 right-4"
                      >
                        <div className="flex gap-2">
                          <ColorOSChip 
                            label="原图" 
                            selected={!compareMode}
                            onClick={() => setCompareMode(false)}
                          />
                          <ColorOSChip 
                            label="优化后" 
                            selected={compareMode}
                            onClick={() => setCompareMode(true)}
                          />
                        </div>
                      </motion.div>
                    )}
                  </div>
                ) : (
                  <div 
                    onClick={handleSelectImage}
                    className="w-full h-full flex flex-col items-center justify-center gap-4 p-8"
                  >
                    <div className="w-20 h-20 rounded-full bg-white/5 flex items-center justify-center">
                      <ImagePlus className="w-10 h-10 text-text-tertiary" />
                    </div>
                    <div className="text-center">
                      <p className="text-white font-medium">点击选择样张</p>
                      <p className="text-text-tertiary text-sm mt-1">支持 JPG、PNG、HEIC 格式</p>
                    </div>
                    <div className="flex gap-4 mt-2">
                      <div className="flex items-center gap-2 text-text-secondary text-sm">
                        <Camera className="w-4 h-4" />
                        <span>拍照</span>
                      </div>
                      <div className="flex items-center gap-2 text-text-secondary text-sm">
                        <Layers className="w-4 h-4" />
                        <span>相册</span>
                      </div>
                    </div>
                  </div>
                )}
              </ColorOSCard>

              {selectedImage && (
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.2 }}
                  className="mt-4"
                >
                  <ColorOSSectionHeader title="快速预设" subtitle="一键应用优化方案" />
                  <div className="grid grid-cols-2 gap-3">
                    {presetPresets.map((preset) => (
                      <motion.button
                        key={preset.id}
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => handlePresetSelect(preset.id)}
                        className={`p-4 rounded-oppo-sm text-left transition-all ${
                          activePreset === preset.id
                            ? 'bg-oppo-sunrise-gold/20 border border-oppo-sunrise-gold/50'
                            : 'bg-white/5 border border-transparent hover:bg-white/10'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <span className="text-2xl">{preset.icon}</span>
                          <div>
                            <p className="text-white font-medium">{preset.name}</p>
                            <p className="text-text-tertiary text-xs">{preset.desc}</p>
                          </div>
                        </div>
                      </motion.button>
                    ))}
                  </div>
                </motion.div>
              )}
            </motion.section>

            <motion.section
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
            >
              <ColorOSSectionHeader title="AI 微调" subtitle="智能分析并优化参数" />

              <ColorOSCard variant="default" className="p-6">
                <motion.button
                  whileHover={{ scale: selectedImage ? 1.02 : 1 }}
                  whileTap={{ scale: selectedImage ? 0.98 : 1 }}
                  onClick={handleAiFineTune}
                  disabled={!selectedImage || isProcessing}
                  className={`w-full py-4 rounded-oppo-sm font-semibold flex items-center justify-center gap-3 transition-all ${
                    selectedImage && !isProcessing
                      ? 'bg-gradient-to-r from-aurora-purple to-ocean-blue text-white'
                      : 'bg-white/10 text-text-tertiary cursor-not-allowed'
                  }`}
                >
                  {isProcessing ? (
                    <>
                      <motion.div
                        animate={{ rotate: 360 }}
                        transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                      >
                        <Sparkles className="w-5 h-5" />
                      </motion.div>
                      <span>AI 分析中...</span>
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-5 h-5" />
                      <span>{selectedImage ? '开始 AI 智能微调' : '请先选择样张'}</span>
                    </>
                  )}
                </motion.button>
              </ColorOSCard>

              <AnimatePresence>
                {showResult && (
                  <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                    className="mt-6 space-y-4"
                  >
                    <ColorOSSectionHeader 
                      title="调整参数" 
                      subtitle="AI 推荐的优化方案"
                      action={
                        <button className="text-oppo-sunrise-gold text-sm font-medium flex items-center gap-1">
                          重置 <RefreshCw className="w-3 h-3" />
                        </button>
                      }
                    />

                    <ColorOSCard variant="default" className="p-5 space-y-5">
                      <ColorOSSlider
                        label="对比度"
                        value={adjustmentParams.contrast}
                        onChange={(v) => updateParam('contrast', v)}
                        min={-50}
                        max={50}
                        unit=""
                      />
                      <ColorOSSlider
                        label="饱和度"
                        value={adjustmentParams.saturation}
                        onChange={(v) => updateParam('saturation', v)}
                        min={-50}
                        max={50}
                        unit=""
                      />
                      <ColorOSSlider
                        label="色温"
                        value={adjustmentParams.temperature}
                        onChange={(v) => updateParam('temperature', v)}
                        min={-50}
                        max={50}
                        unit="K"
                      />
                      <ColorOSSlider
                        label="曝光"
                        value={adjustmentParams.exposure}
                        onChange={(v) => updateParam('exposure', v)}
                        min={-50}
                        max={50}
                        unit="EV"
                      />
                      <ColorOSSlider
                        label="高光"
                        value={adjustmentParams.highlights}
                        onChange={(v) => updateParam('highlights', v)}
                        min={-100}
                        max={100}
                        unit=""
                      />
                      <ColorOSSlider
                        label="阴影"
                        value={adjustmentParams.shadows}
                        onChange={(v) => updateParam('shadows', v)}
                        min={-100}
                        max={100}
                        unit=""
                      />
                    </ColorOSCard>

                    <div className="grid grid-cols-2 gap-3">
                      <ColorOSButton variant="secondary" size="lg" className="w-full">
                        <Save className="w-5 h-5" />
                        保存参数
                      </ColorOSButton>
                      <ColorOSButton variant="primary" size="lg" className="w-full">
                        <Check className="w-5 h-5" />
                        应用到相机
                      </ColorOSButton>
                    </div>

                    <ColorOSCard variant="glass" className="p-4">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-oppo-green/20 flex items-center justify-center">
                          <Check className="w-5 h-5 text-oppo-green" />
                        </div>
                        <div className="flex-1">
                          <p className="text-white font-medium">AI 优化完成</p>
                          <p className="text-text-tertiary text-sm">已为您智能调整 6 项参数</p>
                        </div>
                      </div>
                    </ColorOSCard>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.section>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
