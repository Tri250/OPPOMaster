import { motion, AnimatePresence } from 'framer-motion'
import { 
  ScanText, Upload, Camera, Check, AlertCircle,
  Copy, Save, RefreshCw, FileText, Zap,
  Eye, ChevronRight, Info
} from 'lucide-react'
import { useState } from 'react'
import { 
  ColorOSCard, ColorOSButton, ColorOSSectionHeader,
  ColorOSAnimations, ColorOSChip
} from '../components/common/ColorOSComponents'

interface RecognizedParams {
  iso: string
  shutter: string
  aperture: string
  ev: string
  wb: string
  focal: string
  mode: string
}

const mockRecognizedParams: RecognizedParams = {
  iso: 'ISO 400',
  shutter: '1/250s',
  aperture: 'f/1.8',
  ev: '+0.3 EV',
  wb: '5600K',
  focal: '24mm',
  mode: '专业模式'
}

export default function OcrDemoPage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null)
  const [isRecognizing, setIsRecognizing] = useState(false)
  const [recognizedParams, setRecognizedParams] = useState<RecognizedParams | null>(null)
  const [confidence, setConfidence] = useState(0)

  const handleSelectImage = () => {
    const seed = Math.random().toString(36).substring(7)
    setSelectedImage(`https://picsum.photos/seed/${seed}/800/600`)
    setRecognizedParams(null)
    setConfidence(0)
  }

  const handleRecognize = async () => {
    if (!selectedImage) return
    setIsRecognizing(true)
    await new Promise(resolve => setTimeout(resolve, 2500))
    setRecognizedParams(mockRecognizedParams)
    setConfidence(Math.floor(Math.random() * 10) + 90)
    setIsRecognizing(false)
  }

  const paramGroups = [
    { title: '曝光参数', params: ['iso', 'shutter', 'aperture', 'ev'] },
    { title: '白平衡', params: ['wb'] },
    { title: '镜头参数', params: ['focal'] },
  ]

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-80 h-80 top-1/3 -left-40 animate-float" />
        <div className="orb-oppo orb-3 w-64 h-64 bottom-1/4 -right-32 animate-float" style={{ animationDelay: '3s' }} />
      </div>

      <div className="relative max-w-4xl mx-auto px-4 py-8">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
        >
          <div className="flex items-center gap-3 mb-8">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
              <ScanText className="w-6 h-6 text-deep-space" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">OCR 参数识别</h1>
              <p className="text-text-tertiary text-sm">从照片中识别相机参数信息</p>
            </div>
          </div>

          <div className="grid lg:grid-cols-2 gap-6">
            <motion.section
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <ColorOSSectionHeader 
                title="上传图片" 
                subtitle="选择包含相机参数的照片"
              />

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
                    
                    <div className="absolute top-4 right-4">
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={handleSelectImage}
                        className="w-10 h-10 rounded-xl bg-black/50 backdrop-blur-sm flex items-center justify-center text-white"
                      >
                        <RefreshCw className="w-5 h-5" />
                      </motion.button>
                    </div>

                    <div className="absolute bottom-4 left-4 right-4">
                      <p className="text-white text-sm font-medium">已选择图片</p>
                      <p className="text-text-secondary text-xs">点击右上角更换图片</p>
                    </div>
                  </div>
                ) : (
                  <div 
                    onClick={handleSelectImage}
                    className="w-full h-full flex flex-col items-center justify-center gap-6 p-8"
                  >
                    <div className="w-24 h-24 rounded-full bg-gradient-to-br from-oppo-sunrise-gold/20 to-ocean-blue/20 flex items-center justify-center">
                      <Upload className="w-12 h-12 text-oppo-sunrise-gold" />
                    </div>
                    <div className="text-center">
                      <p className="text-white font-medium text-lg">点击上传图片</p>
                      <p className="text-text-tertiary text-sm mt-2">支持 JPG、PNG、HEIC 格式</p>
                    </div>
                    <div className="flex gap-6 mt-4">
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={(e) => { e.stopPropagation(); handleSelectImage() }}
                        className="flex flex-col items-center gap-2"
                      >
                        <div className="w-14 h-14 rounded-2xl bg-oppo-sunrise-gold/20 flex items-center justify-center">
                          <Camera className="w-7 h-7 text-oppo-sunrise-gold" />
                        </div>
                        <span className="text-text-secondary text-sm">拍照</span>
                      </motion.button>
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={(e) => { e.stopPropagation(); handleSelectImage() }}
                        className="flex flex-col items-center gap-2"
                      >
                        <div className="w-14 h-14 rounded-2xl bg-ocean-blue/20 flex items-center justify-center">
                          <FileText className="w-7 h-7 text-ocean-blue" />
                        </div>
                        <span className="text-text-secondary text-sm">相册</span>
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
                  loading={isRecognizing}
                  disabled={!selectedImage || isRecognizing}
                  onClick={handleRecognize}
                  className="w-full"
                  icon={isRecognizing ? undefined : <Eye className="w-5 h-5" />}
                >
                  {isRecognizing ? '正在识别参数...' : selectedImage ? '开始 OCR 识别' : '请先上传图片'}
                </ColorOSButton>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
                className="mt-6"
              >
                <ColorOSCard variant="glass" className="p-4">
                  <div className="flex items-start gap-3">
                    <Info className="w-5 h-5 text-ocean-blue flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="text-white text-sm font-medium">使用提示</p>
                      <p className="text-text-tertiary text-xs mt-1">
                        上传包含 EXIF 信息或屏幕截图的照片，AI 将自动识别相机参数
                      </p>
                    </div>
                  </div>
                </ColorOSCard>
              </motion.div>
            </motion.section>

            <motion.section
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
            >
              <ColorOSSectionHeader 
                title="识别结果" 
                subtitle="提取的相机参数"
              />

              <AnimatePresence mode="wait">
                {recognizedParams ? (
                  <motion.div
                    key="result"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                    className="space-y-4"
                  >
                    <ColorOSCard variant="gradient" className="p-5">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-xl bg-oppo-green/20 flex items-center justify-center">
                            <Check className="w-5 h-5 text-oppo-green" />
                          </div>
                          <div>
                            <p className="text-white font-medium">识别成功</p>
                            <p className="text-text-secondary text-sm">已提取 7 项参数</p>
                          </div>
                        </div>
                        <div className="text-right">
                          <p className="text-oppo-sunrise-gold font-bold">{confidence}%</p>
                          <p className="text-text-tertiary text-xs">置信度</p>
                        </div>
                      </div>
                      <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                        <motion.div
                          initial={{ width: 0 }}
                          animate={{ width: `${confidence}%` }}
                          transition={{ duration: 0.5 }}
                          className="h-full bg-gradient-to-r from-oppo-sunrise-gold to-oppo-sunrise-gold-light rounded-full"
                        />
                      </div>
                    </ColorOSCard>

                    {paramGroups.map((group, groupIndex) => (
                      <motion.div
                        key={group.title}
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.1 + groupIndex * 0.1 }}
                      >
                        <p className="text-text-secondary text-sm mb-2">{group.title}</p>
                        <div className="grid grid-cols-2 gap-2">
                          {group.params.map((param) => (
                            <ColorOSCard key={param} variant="default" className="p-3">
                              <p className="text-text-tertiary text-xs uppercase">{param}</p>
                              <p className="text-white font-medium mt-1">
                                {recognizedParams[param as keyof RecognizedParams]}
                              </p>
                            </ColorOSCard>
                          ))}
                        </div>
                      </motion.div>
                    ))}

                    <motion.div
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.4 }}
                      className="grid grid-cols-2 gap-3 pt-4"
                    >
                      <ColorOSButton variant="secondary" icon={<Copy className="w-4 h-4" />}>
                        复制参数
                      </ColorOSButton>
                      <ColorOSButton variant="primary" icon={<Save className="w-4 h-4" />}>
                        保存为预设
                      </ColorOSButton>
                    </motion.div>
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
                        <ScanText className="w-8 h-8 text-text-tertiary" />
                      </div>
                      <p className="text-text-secondary">上传图片后开始识别</p>
                      <p className="text-text-tertiary text-sm mt-2">AI 将自动提取相机参数</p>
                    </ColorOSCard>
                  </motion.div>
                )}
              </AnimatePresence>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 }}
                className="mt-6"
              >
                <ColorOSSectionHeader 
                  title="支持识别" 
                  subtitle="可识别的参数类型"
                />

                <div className="flex flex-wrap gap-2">
                  {['ISO', '快门', '光圈', 'EV', '白平衡', '焦距', '拍摄模式'].map((tag) => (
                    <ColorOSChip key={tag} label={tag} />
                  ))}
                </div>
              </motion.div>
            </motion.section>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
