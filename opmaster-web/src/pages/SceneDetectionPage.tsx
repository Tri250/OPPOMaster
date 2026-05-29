import { motion, AnimatePresence } from 'framer-motion'
import { 
  Scan, Camera, Image, Mountain, User, Moon,
  Sunset, UtensilsCrossed, Building2, Leaf,
  Focus, HelpCircle, Check, Sparkles, Heart,
  ChevronRight, Upload, ImagePlus
} from 'lucide-react'
import { useState } from 'react'
import { 
  ColorOSCard, ColorOSButton, ColorOSSectionHeader,
  ColorOSAnimations, ColorOSChip
} from '../components/common/ColorOSComponents'

interface SceneType {
  id: string
  name: string
  icon: React.ReactNode
  color: string
  description: string
  confidence: number
}

interface Preset {
  id: string
  name: string
  device: string
  isHNCS: boolean
  rating: number
}

const sceneTypes: SceneType[] = [
  { id: 'landscape', name: '风景', icon: <Mountain className="w-5 h-5" />, color: 'text-oppo-green', description: '自然风光、山川湖海', confidence: 0 },
  { id: 'portrait', name: '人像', icon: <User className="w-5 h-5" />, color: 'text-sakura-pink', description: '人物肖像、自拍', confidence: 0 },
  { id: 'night', name: '夜景', icon: <Moon className="w-5 h-5" />, color: 'text-aurora-purple', description: '夜间场景、城市灯光', confidence: 0 },
  { id: 'sunset', name: '日落', icon: <Sunset className="w-5 h-5" />, color: 'text-oppo-sunrise-gold', description: '日出日落、黄金时刻', confidence: 0 },
  { id: 'food', name: '美食', icon: <UtensilsCrossed className="w-5 h-5" />, color: 'text-warning-vital', description: '美食摄影、餐饮', confidence: 0 },
  { id: 'street', name: '街拍', icon: <Building2 className="w-5 h-5" />, color: 'text-ocean-blue', description: '街头摄影、城市人文', confidence: 0 },
  { id: 'nature', name: '自然', icon: <Leaf className="w-5 h-5" />, color: 'text-oppo-green-light', description: '植物花卉、生态', confidence: 0 },
  { id: 'architecture', name: '建筑', icon: <Building2 className="w-5 h-5" />, color: 'text-text-secondary', description: '建筑摄影、室内', confidence: 0 },
  { id: 'macro', name: '微距', icon: <Focus className="w-5 h-5" />, color: 'text-hasselblad-pro', description: '微距特写、细节', confidence: 0 },
]

const mockPresets: Preset[] = [
  { id: '1', name: '城市夜景大师', device: 'Find X7 Ultra', isHNCS: true, rating: 4.9 },
  { id: '2', name: '人像柔光模式', device: 'Reno 12 Pro', isHNCS: true, rating: 4.8 },
  { id: '3', name: '风光 HDR', device: 'Find X6 Pro', isHNCS: false, rating: 4.7 },
  { id: '4', name: '美食鲜艳', device: '一加 12', isHNCS: false, rating: 4.6 },
]

export default function SceneDetectionPage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null)
  const [isDetecting, setIsDetecting] = useState(false)
  const [detectedScene, setDetectedScene] = useState<SceneType | null>(null)
  const [recommendedPresets, setRecommendedPresets] = useState<Preset[]>([])
  const [imageSource, setImageSource] = useState<'camera' | 'gallery' | null>(null)

  const handleSelectImage = (source: 'camera' | 'gallery') => {
    setImageSource(source)
    const seed = Math.random().toString(36).substring(7)
    setSelectedImage(`https://picsum.photos/seed/${seed}/800/600`)
    setDetectedScene(null)
    setRecommendedPresets([])
  }

  const handleDetectScene = async () => {
    if (!selectedImage) return
    setIsDetecting(true)
    await new Promise(resolve => setTimeout(resolve, 2500))
    
    const randomScene = sceneTypes[Math.floor(Math.random() * sceneTypes.length)]
    randomScene.confidence = Math.floor(Math.random() * 15) + 85
    setDetectedScene(randomScene)
    setRecommendedPresets(mockPresets.slice(0, 3))
    setIsDetecting(false)
  }

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/3 -left-48 animate-float" />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 right-0 animate-float" style={{ animationDelay: '3s' }} />
      </div>

      <div className="relative max-w-4xl mx-auto px-4 sm:px-6 py-6 md:py-8">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
        >
          <div className="flex items-center gap-3 mb-6 md:mb-8">
            <div className="w-11 h-11 md:w-12 md:h-12 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
              <Scan className="w-5 md:w-6 h-5 md:h-6 text-deep-space" />
            </div>
            <div>
              <h1 className="text-xl md:text-2xl font-bold text-white">AI 场景识别</h1>
              <p className="text-text-tertiary text-xs md:text-sm">智能识别场景，推荐最佳预设</p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-5 gap-4 md:gap-6">
            <motion.div 
              className="lg:col-span-3"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <ColorOSSectionHeader title="选择图片" subtitle="拍照或从相册选择" />

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
                    
                    <div className="absolute top-3 md:top-4 right-3 md:right-4 flex gap-2">
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => handleSelectImage(imageSource || 'gallery')}
                        className="w-11 h-11 md:w-10 md:h-10 rounded-xl bg-black/50 backdrop-blur-sm flex items-center justify-center text-white min-h-[44px] min-w-[44px]"
                      >
                        <ImagePlus className="w-5 h-5" />
                      </motion.button>
                    </div>

                    <div className="absolute bottom-3 md:bottom-4 left-3 md:left-4 right-3 md:right-4 flex gap-2 flex-wrap">
                      <ColorOSChip 
                        icon={<Camera className="w-4 h-4" />}
                        label="拍照" 
                        selected={imageSource === 'camera'}
                        onClick={() => handleSelectImage('camera')}
                      />
                      <ColorOSChip 
                        icon={<Image className="w-4 h-4" />}
                        label="相册" 
                        selected={imageSource === 'gallery'}
                        onClick={() => handleSelectImage('gallery')}
                      />
                    </div>
                  </div>
                ) : (
                  <div className="w-full h-full flex flex-col items-center justify-center gap-4 md:gap-6 p-4 md:p-8">
                    <div className="w-20 h-20 md:w-24 md:h-24 rounded-full bg-gradient-to-br from-oppo-sunrise-gold/20 to-ocean-blue/20 flex items-center justify-center">
                      <Upload className="w-10 h-10 md:w-12 md:h-12 text-oppo-sunrise-gold" />
                    </div>
                    <div className="text-center">
                      <p className="text-white font-medium text-base md:text-lg">点击选择拍摄的样张</p>
                      <p className="text-text-tertiary text-xs md:text-sm mt-1 md:mt-2">AI 将自动识别场景并推荐预设</p>
                    </div>
                    <div className="flex gap-4 md:gap-6 mt-4">
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => handleSelectImage('camera')}
                        className="flex flex-col items-center gap-2 min-h-[80px] min-w-[80px] p-2"
                      >
                        <div className="w-13 h-13 md:w-14 md:h-14 rounded-2xl bg-oppo-sunrise-gold/20 flex items-center justify-center">
                          <Camera className="w-6 h-6 md:w-7 md:h-7 text-oppo-sunrise-gold" />
                        </div>
                        <span className="text-text-secondary text-xs md:text-sm">拍照</span>
                      </motion.button>
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => handleSelectImage('gallery')}
                        className="flex flex-col items-center gap-2 min-h-[80px] min-w-[80px] p-2"
                      >
                        <div className="w-13 h-13 md:w-14 md:h-14 rounded-2xl bg-ocean-blue/20 flex items-center justify-center">
                          <Image className="w-6 h-6 md:w-7 md:h-7 text-ocean-blue" />
                        </div>
                        <span className="text-text-secondary text-xs md:text-sm">相册</span>
                      </motion.button>
                    </div>
                  </div>
                )}
              </ColorOSCard>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="mt-6"
              >
                <ColorOSButton
                  variant="primary"
                  size="lg"
                  loading={isDetecting}
                  disabled={!selectedImage || isDetecting}
                  onClick={handleDetectScene}
                  className="w-full"
                  icon={isDetecting ? undefined : <Sparkles className="w-5 h-5" />}
                >
                  {isDetecting ? '正在识别场景...' : selectedImage ? '开始 AI 场景识别' : '请先选择图片'}
                </ColorOSButton>
              </motion.div>
            </motion.div>

            <motion.div 
              className="lg:col-span-2"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
            >
              <ColorOSSectionHeader title="识别结果" subtitle="AI 分析的场景类型" />

              <AnimatePresence mode="wait">
                {detectedScene ? (
                  <motion.div
                    key="result"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                    className="space-y-4"
                  >
                    <ColorOSCard variant="gradient" className="p-5">
                      <div className="flex items-center gap-4">
                        <div className={`w-14 h-14 rounded-2xl bg-white/10 flex items-center justify-center ${detectedScene.color}`}>
                          {detectedScene.icon}
                        </div>
                        <div className="flex-1">
                          <p className="text-text-tertiary text-sm">识别结果</p>
                          <p className="text-white text-xl font-bold">{detectedScene.name}</p>
                          <p className="text-text-secondary text-sm">{detectedScene.description}</p>
                        </div>
                      </div>
                      <div className="mt-4 pt-4 border-t border-white/10">
                        <div className="flex items-center justify-between">
                          <span className="text-text-tertiary text-sm">置信度</span>
                          <span className="text-oppo-sunrise-gold font-bold">{detectedScene.confidence}%</span>
                        </div>
                        <div className="mt-2 h-2 bg-white/10 rounded-full overflow-hidden">
                          <motion.div
                            initial={{ width: 0 }}
                            animate={{ width: `${detectedScene.confidence}%` }}
                            transition={{ duration: 0.5, ease: 'easeOut' }}
                            className="h-full bg-gradient-to-r from-oppo-sunrise-gold to-oppo-sunrise-gold-light rounded-full"
                          />
                        </div>
                      </div>
                    </ColorOSCard>

                    <ColorOSSectionHeader 
                      title="推荐预设" 
                      subtitle="为您匹配的哈苏大师预设"
                    />

                    <div className="space-y-3">
                      {recommendedPresets.map((preset, index) => (
                        <motion.div
                          key={preset.id}
                          initial={{ opacity: 0, x: 20 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: index * 0.1 }}
                        >
                          <ColorOSCard variant="default" interactive className="p-4">
                            <div className="flex items-center gap-3">
                              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex items-center justify-center">
                                <Camera className="w-6 h-6 text-oppo-sunrise-gold" />
                              </div>
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2">
                                  <p className="text-white font-medium truncate">{preset.name}</p>
                                  {preset.isHNCS && (
                                    <span className="px-2 py-0.5 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">
                                      HNCS
                                    </span>
                                  )}
                                </div>
                                <p className="text-text-tertiary text-sm">{preset.device}</p>
                              </div>
                              <div className="flex items-center gap-1 text-oppo-sunrise-gold">
                                <Heart className="w-4 h-4 fill-current" />
                                <span className="text-sm font-medium">{preset.rating}</span>
                              </div>
                            </div>
                          </ColorOSCard>
                        </motion.div>
                      ))}
                    </div>
                  </motion.div>
                ) : (
                  <motion.div
                    key="empty"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                  >
                    <ColorOSCard variant="default" className="p-8 text-center">
                      <div className="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-4">
                        <HelpCircle className="w-8 h-8 text-text-tertiary" />
                      </div>
                      <p className="text-text-secondary">选择图片后开始识别</p>
                      <p className="text-text-tertiary text-sm mt-2">AI 将自动分析场景类型</p>
                    </ColorOSCard>
                  </motion.div>
                )}
              </AnimatePresence>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
                className="mt-6"
              >
                <ColorOSSectionHeader title="支持的场景" subtitle="可识别 9 种场景类型" />
                <div className="grid grid-cols-3 sm:grid-cols-3 gap-2 sm:gap-3">
                  {sceneTypes.map((scene) => (
                    <div
                      key={scene.id}
                      className={`p-2.5 md:p-3 rounded-xl text-center transition-all min-h-[70px] flex flex-col items-center justify-center ${
                        detectedScene?.id === scene.id
                          ? 'bg-oppo-sunrise-gold/20 border border-oppo-sunrise-gold/50'
                          : 'bg-white/5 hover:bg-white/10'
                      }`}
                    >
                      <div className={`${scene.color} flex justify-center mb-1`}>
                        {scene.icon}
                      </div>
                      <p className="text-text-secondary text-xs">{scene.name}</p>
                    </div>
                  ))}
                </div>
              </motion.div>
            </motion.div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
