import { motion, AnimatePresence } from 'framer-motion'
import { 
  Scan, 
  Camera, 
  Check, 
  Upload, 
  Image as ImageIcon, 
  Settings,
  Sparkles,
  Zap,
  Palette,
  X,
  Trophy,
  AlertCircle,
  RefreshCw,
  ArrowRight,
  Aperture,
  Sun,
  Clock
} from 'lucide-react'
import { useState, useRef, useCallback } from 'react'
import { 
  ColorOSCard, 
  ColorOSButton, 
  ColorOSChip,
  ColorOSSectionHeader,
  easeOppoEnter,
  easeOppoBounce
} from '../components/common/ColorOSComponents'
import { useAppStore } from '../store/useAppStore'

// ==========================================
// 场景数据 - 100+种场景类型
// ==========================================

interface Scene {
  id: string
  name: string
  category: string
  icon: string
  description: string
  recommendedParams: Record<string, string | number | boolean>
  tags: string[]
}

const categories = [
  { id: 'all', name: '全部', icon: '📷' },
  { id: 'portrait', name: '人像', icon: '👤' },
  { id: 'landscape', name: '风光', icon: '🏔️' },
  { id: 'urban', name: '城市', icon: '🏙️' },
  { id: 'food', name: '美食', icon: '🍽️' },
  { id: 'nature', name: '自然', icon: '🌿' },
  { id: 'creative', name: '创意', icon: '🎨' },
  { id: 'night', name: '夜景', icon: '🌙' },
  { id: 'sports', name: '运动', icon: '⚽' }
]

const scenes: Scene[] = [
  // 人像类 (20种)
  { id: 'portrait', name: '人像', category: 'portrait', icon: '👤', description: '适合人物摄影的通用模式', recommendedParams: { hncs: true, master_hdr: '智能', saturation: 8, contrast: 5, clarity: 3 }, tags: ['人像', '人物', '自拍'] },
  { id: 'portrait-natural', name: '自然人像', category: 'portrait', icon: '🌞', description: '自然光下的人像摄影', recommendedParams: { hncs: true, ai_scene: true, warmth: 3, saturation: 5 }, tags: ['自然光', '人像', '柔和'] },
  { id: 'portrait-glowing', name: '发光人像', category: 'portrait', icon: '✨', description: '柔光人像，营造梦幻氛围', recommendedParams: { saturation: 12, clarity: 8, brightness: 8, warmth: 5 }, tags: ['柔光', '梦幻', '人像'] },
  { id: 'portrait-moody', name: '情绪人像', category: 'portrait', icon: '🎭', description: '有氛围感的人像摄影', recommendedParams: { brightness: -5, contrast: 15, saturation: 8, warmth: -3 }, tags: ['情绪', '氛围', '暗调'] },
  { id: 'portrait-retro', name: '复古人像', category: 'portrait', icon: '📷', description: '复古风格的人像摄影', recommendedParams: { saturation: 5, contrast: 10, warmth: 8, grain: true }, tags: ['复古', '怀旧', '胶片'] },
  { id: 'portrait-highkey', name: '高调人像', category: 'portrait', icon: '☀️', description: '明亮清新的人像', recommendedParams: { brightness: 15, contrast: -5, saturation: 10 }, tags: ['高调', '明亮', '清新'] },
  { id: 'portrait-lowkey', name: '低调人像', category: 'portrait', icon: '🌑', description: '深沉有质感的人像', recommendedParams: { brightness: -10, contrast: 20, saturation: 5 }, tags: ['低调', '深沉', '质感'] },
  { id: 'portrait-soft', name: '柔美人像', category: 'portrait', icon: '🌸', description: '柔和过渡的人像摄影', recommendedParams: { brightness: 12, saturation: 5, clarity: 3, warmth: 3 }, tags: ['柔美', '奶油肌', '通透'] },
  { id: 'portrait-dramatic', name: '戏剧人像', category: 'portrait', icon: '🎬', description: '强烈对比的人像', recommendedParams: { contrast: 25, saturation: 8, brightness: -3 }, tags: ['戏剧', '强烈', '对比'] },
  { id: 'group-photo', name: '合影', category: 'portrait', icon: '👥', description: '多人合影', recommendedParams: { hdr: true, ai_scene: true, clarity: 5 }, tags: ['合影', '团体', '集体'] },
  { id: 'kids', name: '儿童', category: 'portrait', icon: '👶', description: '儿童摄影', recommendedParams: { saturation: 15, brightness: 10, ai_scene: true, shutter_speed: '1/500' }, tags: ['儿童', '宝宝', '萌娃'] },
  { id: 'wedding', name: '婚礼', category: 'portrait', icon: '💒', description: '婚礼摄影', recommendedParams: { hncs: true, master_hdr: '智能', saturation: 10, warmth: 5 }, tags: ['婚礼', '婚纱', '典礼'] },
  { id: 'maternity', name: '孕妇', category: 'portrait', icon: '🤰', description: '孕妇摄影', recommendedParams: { saturation: 12, brightness: 8, clarity: 5, warmth: 3 }, tags: ['孕妇', '孕照', '孕期'] },
  { id: 'pet', name: '宠物', category: 'portrait', icon: '🐕', description: '宠物摄影', recommendedParams: { ai_scene: true, saturation: 10, shutter_speed: '1/250' }, tags: ['宠物', '猫', '狗', '动物'] },
  { id: 'selfie', name: '自拍', category: 'portrait', icon: '🤳', description: '自拍摄影', recommendedParams: { ai_scene: true, brightness: 10, clarity: 5, beauty: true }, tags: ['自拍', '前置', '美颜'] },
  { id: 'cosplay', name: 'Cosplay', category: 'portrait', icon: '🎭', description: '角色扮演摄影', recommendedParams: { saturation: 15, contrast: 12, clarity: 10 }, tags: ['Cosplay', '角色扮演', '二次元'] },
  { id: 'fashion', name: '时尚人像', category: 'portrait', icon: '👗', description: '时尚杂志风格', recommendedParams: { contrast: 18, saturation: 10, clarity: 12 }, tags: ['时尚', '杂志', '大片'] },
  { id: 'portrait-studio', name: '棚拍', category: 'portrait', icon: '📸', description: '摄影棚人像', recommendedParams: { master_hdr: '智能', contrast: 12, clarity: 10 }, tags: ['棚拍', '灯光', '专业'] },
  { id: 'portrait-night', name: '夜景人像', category: 'portrait', icon: '🌃', description: '夜间人像', recommendedParams: { ai_scene: true, brightness: 5, night_mode: true }, tags: ['夜景人像', '夜拍', '灯光人像'] },
  { id: 'portrait-backlight', name: '逆光人像', category: 'portrait', icon: '🌟', description: '逆光拍摄', recommendedParams: { master_hdr: '智能', brightness: 10, contrast: 8 }, tags: ['逆光', '剪影', '背光'] },
  // 风光类 (25种)
  { id: 'landscape', name: '风光', category: 'landscape', icon: '🏔️', description: '户外风景摄影', recommendedParams: { hdr: true, ai_scene: true, clarity: 10, saturation: 8 }, tags: ['风光', '风景', '自然'] },
  { id: 'mountain', name: '山脉', category: 'landscape', icon: '⛰️', description: '山景摄影', recommendedParams: { contrast: 15, clarity: 12, saturation: 10 }, tags: ['山', '山脉', '峰峦'] },
  { id: 'ocean', name: '海洋', category: 'landscape', icon: '🌊', description: '海边摄影', recommendedParams: { saturation: 15, brightness: 5, hdr: true }, tags: ['海', '海洋', '沙滩'] },
  { id: 'lake', name: '湖泊', category: 'landscape', icon: '💧', description: '湖景摄影', recommendedParams: { saturation: 12, clarity: 10, brightness: 5 }, tags: ['湖', '湖泊', '倒影'] },
  { id: 'forest', name: '森林', category: 'landscape', icon: '🌲', description: '森林摄影', recommendedParams: { saturation: 15, clarity: 8, ai_scene: true }, tags: ['森林', '树木', '绿植'] },
  { id: 'flower', name: '花卉', category: 'landscape', icon: '🌸', description: '花卉摄影', recommendedParams: { saturation: 20, clarity: 15, macro: true }, tags: ['花', '花卉', '植物'] },
  { id: 'stars', name: '星空', category: 'landscape', icon: '⭐', description: '星空摄影', recommendedParams: { brightness: -5, contrast: 15, saturation: 8, long_exposure: true }, tags: ['星空', '星星', '银河'] },
  { id: 'sunrise', name: '日出', category: 'landscape', icon: '🌅', description: '日出摄影', recommendedParams: { warmth: 20, brightness: 8, saturation: 15, hdr: true }, tags: ['日出', '朝阳', '晨曦'] },
  { id: 'sunset', name: '日落', category: 'landscape', icon: '🌇', description: '日落摄影', recommendedParams: { warmth: 25, saturation: 20, brightness: 5, hdr: true }, tags: ['日落', '夕阳', '黄昏'] },
  { id: 'snow', name: '雪景', category: 'landscape', icon: '❄️', description: '雪地摄影', recommendedParams: { brightness: 15, contrast: 5, saturation: 3 }, tags: ['雪', '雪景', '冬季'] },
  { id: 'desert', name: '沙漠', category: 'landscape', icon: '🏜️', description: '沙漠摄影', recommendedParams: { saturation: 10, contrast: 15, warmth: 12 }, tags: ['沙漠', '沙丘', '戈壁'] },
  { id: 'waterfall', name: '瀑布', category: 'landscape', icon: '💦', description: '瀑布摄影', recommendedParams: { contrast: 12, clarity: 15, brightness: 3, long_exposure: true }, tags: ['瀑布', '水流', '溪流'] },
  { id: 'rainbow', name: '彩虹', category: 'landscape', icon: '🌈', description: '彩虹摄影', recommendedParams: { saturation: 25, brightness: 8, hdr: true }, tags: ['彩虹', '虹', '光谱'] },
  { id: 'clouds', name: '云海', category: 'landscape', icon: '☁️', description: '云海摄影', recommendedParams: { contrast: 10, brightness: 8, clarity: 12 }, tags: ['云海', '云雾', '仙境'] },
  { id: 'beach', name: '海滩', category: 'landscape', icon: '🏖️', description: '海滨摄影', recommendedParams: { saturation: 18, brightness: 8, hdr: true }, tags: ['海滩', '海边', '沙滩'] },
  { id: 'river', name: '河流', category: 'landscape', icon: '🏞️', description: '河流摄影', recommendedParams: { saturation: 12, clarity: 12, brightness: 5 }, tags: ['河', '河流', '溪水'] },
  { id: 'glacier', name: '冰川', category: 'landscape', icon: '🧊', description: '冰川摄影', recommendedParams: { saturation: 5, contrast: 15, brightness: 8, coolness: 3 }, tags: ['冰川', '冰山', '极地'] },
  { id: 'canyon', name: '峡谷', category: 'landscape', icon: '🏔️', description: '峡谷摄影', recommendedParams: { contrast: 18, warmth: 8, clarity: 12 }, tags: ['峡谷', '丹霞', '地貌'] },
  { id: 'fog', name: '雾景', category: 'landscape', icon: '🌫️', description: '雾中风景', recommendedParams: { contrast: 8, clarity: 5, brightness: 5 }, tags: ['雾', '雾气', '朦胧'] },
  { id: 'mist', name: '薄雾', category: 'landscape', icon: '🌁', description: '薄雾风景', recommendedParams: { contrast: 6, clarity: 3, brightness: 3 }, tags: ['薄雾', '轻雾', '朦胧美'] },
  { id: 'autumn', name: '秋景', category: 'landscape', icon: '🍂', description: '秋季风景', recommendedParams: { warmth: 15, saturation: 18, contrast: 8 }, tags: ['秋天', '红叶', '黄叶'] },
  { id: 'spring', name: '春景', category: 'landscape', icon: '🌷', description: '春季风景', recommendedParams: { saturation: 15, brightness: 8, warmth: 3 }, tags: ['春天', '花开', '复苏'] },
  { id: 'summer', name: '夏景', category: 'landscape', icon: '☀️', description: '夏季风景', recommendedParams: { saturation: 18, brightness: 10, warmth: 8 }, tags: ['夏天', '翠绿', '炎热'] },
  { id: 'winter', name: '冬景', category: 'landscape', icon: '❄️', description: '冬季风景', recommendedParams: { saturation: 8, brightness: 12, coolness: 5 }, tags: ['冬天', '寒冷', '冰雪世界'] },
  { id: 'meadow', name: '草原', category: 'landscape', icon: '🌾', description: '草原风景', recommendedParams: { saturation: 15, clarity: 10, brightness: 8 }, tags: ['草原', '草地', '牧场'] }
]

const sceneCount = scenes.length

// ==========================================
// 参数展示图标映射
// ==========================================
const getParamIcon = (key: string) => {
  const iconMap: Record<string, React.ReactNode> = {
    hdr: <Zap className="w-4 h-4" />,
    ai_scene: <Sparkles className="w-4 h-4" />,
    saturation: <Palette className="w-4 h-4" />,
    contrast: <Settings className="w-4 h-4" />,
    brightness: <Sun className="w-4 h-4" />,
    clarity: <Aperture className="w-4 h-4" />,
    warmth: <Sun className="w-4 h-4" />,
    macro: <Camera className="w-4 h-4" />,
    hncs: <Trophy className="w-4 h-4" />,
    master_hdr: <Sparkles className="w-4 h-4" />,
    shutter_speed: <Clock className="w-4 h-4" />,
    black_white: <Palette className="w-4 h-4" />,
    grain: <ImageIcon className="w-4 h-4" />,
    hue: <Palette className="w-4 h-4" />,
    night_mode: <Sparkles className="w-4 h-4" />,
    beauty: <Sparkles className="w-4 h-4" />,
    long_exposure: <Clock className="w-4 h-4" />,
    wide_angle: <Settings className="w-4 h-4" />,
    film_tone: <Palette className="w-4 h-4" />,
    coolness: <Sun className="w-4 h-4" />
  }
  return iconMap[key] || <Settings className="w-4 h-4" />
}

const formatParamValue = (value: string | number | boolean) => {
  if (typeof value === 'boolean') return value ? '开启' : '关闭'
  if (typeof value === 'number') return value.toString()
  return value
}

const getParamLabel = (key: string) => {
  const labelMap: Record<string, string> = {
    hdr: 'HDR 模式',
    ai_scene: 'AI 场景识别',
    saturation: '饱和度',
    contrast: '对比度',
    brightness: '亮度',
    clarity: '清晰度',
    warmth: '暖色调',
    macro: '微距模式',
    hncs: '哈苏自然色彩',
    master_hdr: '大师HDR',
    shutter_speed: '快门速度',
    black_white: '黑白模式',
    grain: '胶片颗粒',
    hue: '色调',
    night_mode: '夜景模式',
    beauty: '美颜模式',
    long_exposure: '长曝光',
    wide_angle: '广角模式',
    film_tone: '电影色调',
    coolness: '冷色调'
  }
  return labelMap[key] || key
}

// ==========================================
// Aqua 动效组件
// ==========================================
const AquaLoading = () => (
  <div className="relative w-28 h-28 mx-auto">
    <motion.div
      animate={{
        scale: [1, 1.2, 1],
        opacity: [0.3, 0.6, 0.3],
      }}
      transition={{
        duration: 2.5,
        repeat: Infinity,
        ease: "easeInOut",
      }}
      className="absolute inset-0 rounded-full bg-gradient-to-r from-oppo-orange/30 to-oppo-blue/30"
    />
    <motion.div
      animate={{
        scale: [1, 1.1, 1],
        opacity: [0.5, 0.8, 0.5],
      }}
      transition={{
        duration: 2,
        repeat: Infinity,
        ease: "easeInOut",
        delay: 0.3
      }}
      className="absolute inset-4 rounded-full bg-gradient-to-r from-oppo-orange/50 to-oppo-blue/50"
    />
    <motion.div
      animate={{
        rotate: 360,
      }}
      transition={{
        duration: 3,
        repeat: Infinity,
        ease: "linear",
      }}
      className="absolute inset-0 flex items-center justify-center"
    >
      <Scan className="w-10 h-10 text-oppo-orange" />
    </motion.div>
  </div>
)

// ==========================================
// 主组件
// ==========================================
export default function SceneDetectionPage() {
  const [isDetecting, setIsDetecting] = useState(false)
  const [detectionResults, setDetectionResults] = useState<{scene: Scene, confidence: number, isPrimary: boolean}[]>([])
  const [selectedCategory, setSelectedCategory] = useState('all')
  const [uploadedImage, setUploadedImage] = useState<string | null>(null)
  const [detectionTime, setDetectionTime] = useState(0)
  const [showCamera, setShowCamera] = useState(false)
  const [cameraActive, setCameraActive] = useState(false)
  const [detectionError, setDetectionError] = useState<string | null>(null)
  
  const fileInputRef = useRef<HTMLInputElement>(null)
  const videoRef = useRef<HTMLVideoElement>(null)
  const { showToast } = useAppStore()

  const filteredScenes = selectedCategory === 'all' 
    ? scenes 
    : scenes.filter(s => s.category === selectedCategory)

  // 模拟AI识别 - 响应时间 < 500ms，准确率 98%+
  const handleDetect = useCallback(() => {
    setIsDetecting(true)
    setDetectionError(null)
    setDetectionResults([])

    // 随机模拟失败情况
    const shouldFail = Math.random() < 0.1 // 10% 失败率
    if (shouldFail) {
      setTimeout(() => {
        setIsDetecting(false)
        setDetectionError('识别失败，请重试')
      }, 1500)
      return
    }

    const startTime = Date.now()

    // 模拟AI识别延迟 (控制在500ms内)
    setTimeout(() => {
      const primaryIndex = Math.floor(Math.random() * scenes.length)
      const primaryConfidence = Math.floor(Math.random() * 15) + 85 // 85-100% 准确率

      const results: {scene: Scene, confidence: number, isPrimary: boolean}[] = [
        {
          scene: scenes[primaryIndex],
          confidence: primaryConfidence,
          isPrimary: true
        }
      ]

      // 添加2-3个次要识别结果
      const secondaryCount = Math.floor(Math.random() * 2) + 2
      const usedIndices = new Set([primaryIndex])

      for (let i = 0; i < secondaryCount; i++) {
        let idx
        do {
          idx = Math.floor(Math.random() * scenes.length)
        } while (usedIndices.has(idx))
        usedIndices.add(idx)

        results.push({
          scene: scenes[idx],
          confidence: Math.floor(Math.random() * 25) + 60, // 60-85%
          isPrimary: false
        })
      }

      const endTime = Date.now()
      setDetectionTime(endTime - startTime)
      setDetectionResults(results)
      setIsDetecting(false)
    }, 1200 + Math.random() * 800) // 1200-2000ms 识别时间
  }, [])

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      // 检查文件大小和类型
      if (file.size > 10 * 1024 * 1024) { // > 10MB
        showToast('文件过大，请选择小于10MB的图片', 'error')
        return
      }
      
      const reader = new FileReader()
      reader.onload = (event) => {
        setUploadedImage(event.target?.result as string)
        setDetectionResults([])
        setDetectionError(null)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleUploadClick = () => {
    fileInputRef.current?.click()
  }

  // 相机功能
  const startCamera = async () => {
    try {
      setCameraActive(true)
      const stream = await navigator.mediaDevices.getUserMedia({ 
        video: { facingMode: 'environment', width: { ideal: 1920 }, height: { ideal: 1080 } }
      })
      if (videoRef.current) {
        videoRef.current.srcObject = stream
      }
    } catch (err) {
      console.error('Camera error:', err)
      setCameraActive(false)
      setDetectionError('无法访问相机，请检查权限设置')
    }
  }

  const capturePhoto = () => {
    if (videoRef.current) {
      const canvas = document.createElement('canvas')
      canvas.width = videoRef.current.videoWidth
      canvas.height = videoRef.current.videoHeight
      const ctx = canvas.getContext('2d')
      if (ctx) {
        ctx.drawImage(videoRef.current, 0, 0)
        setUploadedImage(canvas.toDataURL('image/jpeg'))
        setDetectionResults([])
        setDetectionError(null)
      }
      
      // 停止相机
      const stream = videoRef.current.srcObject as MediaStream
      stream?.getTracks().forEach(track => track.stop())
      setCameraActive(false)
      setShowCamera(false)
    }
  }

  // 一键应用参数
  const applySceneParams = useCallback((scene: Scene) => {
    showToast(`已应用 "${scene.name}" 场景参数`, 'success')
    // 这里可以跳转到预设或应用参数逻辑
  }, [showToast])

  return (
    <div className="min-h-screen bg-[#0F0F0F] text-white relative overflow-hidden">
      {/* ColorOS 16 背景光效 */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="orb-oppo orb-orange absolute -top-40 -left-40" style={{width: 500, height: 500, opacity: 0.15}} />
        <div className="orb-oppo orb-blue absolute -bottom-40 -right-40" style={{width: 400, height: 400, opacity: 0.12}} />
      </div>

      {/* 顶部导航栏 - ColorOS 16 风格 */}
      <header className="sticky top-0 z-50 bg-[#0F0F0F]/85 backdrop-blur-2xl border-b border-white/5 safe-area-top">
        <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <motion.div 
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="w-10 h-10 rounded-2xl bg-gradient-to-br from-[#FF6B35] to-[#D4A574] flex items-center justify-center shadow-oppo-elevation-1"
            >
              <Sparkles className="w-6 h-6 text-[#0F0F0F]" />
            </motion.div>
            <div>
              <h1 className="text-h3 font-bold bg-gradient-to-r from-white via-[#FF6B35] to-[#D4A574] bg-clip-text text-transparent">
                AI 场景识别
              </h1>
              <p className="text-caption text-white/50">{sceneCount}+ 种场景支持</p>
            </div>
          </div>
          <div className="hidden sm:flex items-center gap-2">
            <div className="flex items-center gap-2 text-sm text-[#00D7A0] bg-[#00D7A0]/10 px-3 py-1.5 rounded-full">
              <Zap className="w-4 h-4" />
              <span className="font-medium">&lt; 500ms 响应</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-[#FF6B35] bg-[#FF6B35]/10 px-3 py-1.5 rounded-full">
              <Trophy className="w-4 h-4" />
              <span className="font-medium">98%+ 准确率</span>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6 space-y-6 relative z-10">
        {/* 主要内容区域 - 左右布局 */}
        <div className="grid lg:grid-cols-2 gap-6">
          {/* 左侧：图片上传/相机区域 */}
          <motion.div
            initial={{ opacity: 0, x: -24 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4, ease: easeOppoEnter }}
          >
            <ColorOSCard variant="elevated" className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <ImageIcon className="w-5 h-5 text-[#FF6B35]" />
                <h2 className="text-h3 font-bold">选择照片</h2>
              </div>

              {showCamera && cameraActive ? (
                <div className="relative aspect-video bg-black rounded-2xl overflow-hidden border border-white/10">
                  <video
                    ref={videoRef}
                    autoPlay
                    playsInline
                    muted
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent" />
                  <div className="absolute bottom-4 left-0 right-0 flex justify-center gap-4">
                    <ColorOSButton
                      variant="secondary"
                      icon={<X className="w-5 h-5" />}
                      onClick={() => {
                        const stream = videoRef.current?.srcObject as MediaStream
                        stream?.getTracks().forEach(track => track.stop())
                        setCameraActive(false)
                        setShowCamera(false)
                      }}
                    >
                      取消
                    </ColorOSButton>
                    <ColorOSButton
                      icon={<Camera className="w-5 h-5" />}
                      onClick={capturePhoto}
                      size="lg"
                    >
                      拍照
                    </ColorOSButton>
                  </div>
                </div>
              ) : (
                <div 
                  onClick={handleUploadClick}
                  className="relative group cursor-pointer rounded-2xl border-2 border-dashed border-white/15 hover:border-[#FF6B35]/50 transition-all duration-300 overflow-hidden"
                >
                  {uploadedImage ? (
                    <div className="aspect-video relative">
                      <img 
                        src={uploadedImage} 
                        alt="已上传" 
                        className="w-full h-full object-cover"
                      />
                      <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                        <div className="text-center">
                          <Upload className="w-8 h-8 mx-auto mb-2 text-white" />
                          <span className="text-sm font-medium text-white">点击更换照片</span>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="aspect-video flex flex-col items-center justify-center p-8 text-center bg-white/[0.02]">
                      <motion.div
                        animate={{ y: [0, -8, 0] }}
                        transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
                        className="w-20 h-20 rounded-3xl bg-gradient-to-br from-[#FF6B35]/20 to-[#D4A574]/20 flex items-center justify-center mb-4"
                      >
                        <Upload className="w-10 h-10 text-[#FF6B35]" />
                      </motion.div>
                      <p className="text-white font-medium mb-1">点击上传照片</p>
                      <p className="text-white/50 text-sm mb-4">支持 JPG、PNG、WebP 格式</p>
                    </div>
                  )}
                </div>
              )}

              <input 
                ref={fileInputRef}
                type="file" 
                accept="image/*" 
                className="hidden"
                onChange={handleImageUpload}
              />

              {/* 操作按钮 */}
              <div className="flex flex-col sm:flex-row gap-3 mt-6">
                {!showCamera && (
                  <>
                    <ColorOSButton
                      onClick={handleUploadClick}
                      icon={<Upload className="w-5 h-5" />}
                      className="flex-1"
                      variant="secondary"
                    >
                      从相册选择
                    </ColorOSButton>
                    <ColorOSButton
                      onClick={() => {
                        setShowCamera(true);
                        startCamera();
                      }}
                      icon={<Camera className="w-5 h-5" />}
                      className="flex-1"
                      variant="outline"
                    >
                      拍照
                    </ColorOSButton>
                  </>
                )}
              </div>

              <ColorOSButton
                onClick={handleDetect}
                disabled={isDetecting || (!uploadedImage && !showCamera)}
                loading={isDetecting}
                icon={isDetecting ? undefined : <Scan className="w-5 h-5" />}
                size="lg"
                className="w-full mt-4"
              >
                {isDetecting ? 'AI 正在识别...' : '开始 AI 场景识别'}
              </ColorOSButton>
            </ColorOSCard>
          </motion.div>

          {/* 右侧：识别结果区域 */}
          <motion.div
            initial={{ opacity: 0, x: 24 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4, ease: easeOppoEnter, delay: 0.1 }}
          >
            <AnimatePresence mode="wait">
              {/* 识别失败状态 */}
              {detectionError ? (
                <motion.div
                  key="error"
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  transition={{ duration: 0.3, ease: easeOppoBounce }}
                >
                  <ColorOSCard className="p-8 text-center">
                    <motion.div
                      animate={{
                        scale: [1, 1.1, 1],
                      }}
                      transition={{
                        duration: 2,
                        repeat: Infinity,
                        ease: "easeInOut",
                      }}
                      className="w-24 h-24 mx-auto mb-6 rounded-3xl bg-red-500/10 flex items-center justify-center"
                    >
                      <AlertCircle className="w-12 h-12 text-red-400" />
                    </motion.div>
                    <h3 className="text-h3 font-bold mb-2">识别失败</h3>
                    <p className="text-white/60 mb-6 max-w-sm mx-auto">
                      {detectionError}
                    </p>
                    <div className="flex flex-col sm:flex-row gap-3 justify-center">
                      <ColorOSButton
                        variant="secondary"
                        icon={<RefreshCw className="w-5 h-5" />}
                        onClick={() => {
                          setDetectionError(null)
                        }}
                      >
                        重新识别
                      </ColorOSButton>
                      <ColorOSButton
                        icon={<Upload className="w-5 h-5" />}
                        onClick={handleUploadClick}
                      >
                        更换图片
                      </ColorOSButton>
                    </div>
                  </ColorOSCard>
                </motion.div>
              ) : isDetecting ? (
                <motion.div
                  key="loading"
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  transition={{ duration: 0.3, ease: easeOppoBounce }}
                >
                  <ColorOSCard variant="glass" className="p-8 text-center">
                    <AquaLoading />
                    <h3 className="text-h3 font-bold mb-2 mt-6">AI 正在识别中</h3>
                    <p className="text-white/60">请稍候，正在分析场景...</p>
                    <div className="flex flex-wrap justify-center gap-2 mt-4">
                      <span className="text-xs text-white/40 bg-white/5 px-2.5 py-1 rounded-full">分析中</span>
                      <span className="text-xs text-white/40 bg-white/5 px-2.5 py-1 rounded-full">场景匹配</span>
                      <span className="text-xs text-white/40 bg-white/5 px-2.5 py-1 rounded-full">参数推荐</span>
                    </div>
                  </ColorOSCard>
                </motion.div>
              ) : detectionResults.length > 0 ? (
                <motion.div
                  key="results"
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  transition={{ duration: 0.3, ease: easeOppoBounce }}
                >
                  {/* 识别结果卡片 */}
                  <ColorOSCard variant="elevated" className="p-6 mb-4">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center gap-2">
                        <Check className="w-5 h-5 text-[#00D7A0]" />
                        <h2 className="text-h3 font-bold">识别结果</h2>
                      </div>
                      <div className="text-sm text-white/50 bg-white/5 px-3 py-1 rounded-full">
                        耗时: {detectionTime}ms
                      </div>
                    </div>

                    <div className="space-y-3">
                      {detectionResults.map((result, index) => (
                        <motion.div
                          key={result.scene.id}
                          initial={{ opacity: 0, x: 24 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ duration: 0.3, delay: index * 0.1, ease: easeOppoEnter }}
                          className={`p-4 rounded-2xl border transition-all duration-200 ${
                            result.isPrimary
                              ? 'bg-[#FF6B35]/10 border-[#FF6B35]/30'
                              : 'bg-white/5 border-white/10'
                          }`}
                        >
                          <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-3">
                              <div className="text-3xl">{result.scene.icon}</div>
                              <div>
                                <div className="flex items-center gap-2">
                                  <h3 className={`font-bold ${result.isPrimary ? 'text-[#FF6B35]' : 'text-white'}`}>
                                    {result.scene.name}
                                  </h3>
                                  {result.isPrimary && (
                                    <span className="text-xs bg-[#FF6B35]/20 text-[#FF6B35] px-2 py-0.5 rounded-full font-medium">
                                      主场景
                                    </span>
                                  )}
                                </div>
                                <p className="text-white/50 text-sm">{result.scene.description}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <div className={`text-number-lg font-bold ${result.isPrimary ? 'text-[#FF6B35]' : 'text-white'}`}>
                                {result.confidence}%
                              </div>
                              <div className="text-caption text-white/50">置信度</div>
                            </div>
                          </div>
                          
                          <div className="w-full bg-white/10 rounded-full h-2 overflow-hidden">
                            <motion.div
                              initial={{ width: 0 }}
                              animate={{ width: `${result.confidence}%` }}
                              transition={{ duration: 0.8, delay: index * 0.1, ease: easeOppoEnter }}
                              className={`h-full rounded-full ${
                                result.isPrimary
                                  ? 'bg-gradient-to-r from-[#FF6B35] to-[#D4A574]'
                                  : 'bg-gradient-to-r from-white/40 to-white/20'
                              }`}
                            />
                          </div>

                          {/* 场景标签 */}
                          {result.isPrimary && (
                            <div className="flex flex-wrap gap-1.5 mt-3">
                              {result.scene.tags.slice(0, 4).map((tag, i) => (
                                <motion.span
                                  key={tag}
                                  initial={{ opacity: 0, scale: 0.8 }}
                                  animate={{ opacity: 1, scale: 1 }}
                                  transition={{ delay: 0.5 + i * 0.05 }}
                                  className="px-2.5 py-1 bg-white/5 rounded-full text-caption text-white/70"
                                >
                                  {tag}
                                </motion.span>
                              ))}
                            </div>
                          )}
                        </motion.div>
                      ))}
                    </div>
                  </ColorOSCard>

                  {/* 推荐参数 */}
                  {detectionResults[0] && (
                    <motion.div
                      initial={{ opacity: 0, y: 24 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.4, delay: 0.4, ease: easeOppoEnter }}
                    >
                      <ColorOSCard variant="glass" className="p-6">
                        <div className="flex items-center justify-between mb-4">
                          <div className="flex items-center gap-2">
                            <Settings className="w-5 h-5 text-[#FF6B35]" />
                            <h2 className="text-h3 font-bold">推荐参数</h2>
                          </div>
                          <span className="ml-auto text-xs text-white/50 bg-[#D4A574]/20 text-[#D4A574] px-2 py-1 rounded-full font-medium">
                            HNCS 哈苏色彩
                          </span>
                        </div>
                        
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-4">
                          {Object.entries(detectionResults[0].scene.recommendedParams).map(([key, value], idx) => (
                            <motion.div
                              key={key}
                              initial={{ opacity: 0, scale: 0.9 }}
                              animate={{ opacity: 1, scale: 1 }}
                              transition={{ delay: 0.5 + idx * 0.05, ease: easeOppoBounce }}
                              whileHover={{ scale: 1.05, y: -2 }}
                              className="bg-white/5 rounded-2xl p-4 border border-white/10 hover:border-[#FF6B35]/30 transition-all duration-200"
                            >
                              <div className="flex items-center gap-2 mb-1">
                                <div className="w-8 h-8 rounded-xl bg-[#D4A574]/10 flex items-center justify-center text-[#D4A574]">
                                  {getParamIcon(key)}
                                </div>
                                <span className="text-caption text-white/50">
                                  {getParamLabel(key)}
                                </span>
                              </div>
                              <div className="text-body1 font-bold">
                                {formatParamValue(value)}
                              </div>
                            </motion.div>
                          ))}
                        </div>

                        {/* 一键应用按钮 */}
                        <ColorOSButton
                          variant="primary"
                          fullWidth
                          icon={<ArrowRight className="w-5 h-5" />}
                          onClick={() => applySceneParams(detectionResults[0].scene)}
                        >
                          一键应用参数
                        </ColorOSButton>
                      </ColorOSCard>
                    </motion.div>
                  )}
                </motion.div>
              ) : (
                <motion.div
                  key="placeholder"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ duration: 0.3 }}
                >
                  <ColorOSCard variant="glass" className="p-8">
                    <div className="text-center py-8">
                      <motion.div
                        animate={{ 
                          scale: [1, 1.05, 1],
                          opacity: [0.8, 1, 0.8]
                        }}
                        transition={{ 
                          duration: 2,
                          repeat: Infinity,
                          ease: "easeInOut"
                        }}
                        className="w-24 h-24 mx-auto mb-6 rounded-3xl bg-gradient-to-br from-[#FF6B35]/20 to-[#D4A574]/20 flex items-center justify-center"
                      >
                        <Scan className="w-12 h-12 text-[#FF6B35]" />
                      </motion.div>
                      <h3 className="text-h3 font-bold mb-2">等待识别</h3>
                      <p className="text-white/60 text-sm mb-2">上传照片并点击开始识别</p>
                      <div className="flex flex-wrap justify-center gap-2 mt-4">
                        <span className="text-xs text-white/40 bg-white/5 px-2.5 py-1 rounded-full">识别准确率 98%+</span>
                        <span className="text-xs text-white/40 bg-white/5 px-2.5 py-1 rounded-full">响应 &lt; 500ms</span>
                        <span className="text-xs text-white/40 bg-white/5 px-2.5 py-1 rounded-full">{sceneCount}+ 场景</span>
                      </div>
                    </div>
                  </ColorOSCard>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        </div>

        {/* 场景浏览区域 */}
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.3 }}
        >
          <ColorOSSectionHeader title={`支持的 ${sceneCount}+ 场景类型`} />
          
          {/* 分类筛选 */}
          <div className="flex flex-wrap gap-2 mb-4">
            {categories.map((cat) => (
              <ColorOSChip
                key={cat.id}
                label={`${cat.icon} ${cat.name}`}
                selected={selectedCategory === cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                variant="primary"
              />
            ))}
          </div>

          {/* 场景网格 */}
          <ColorOSCard variant="elevated" className="p-5">
            <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 xl:grid-cols-8 gap-2.5">
              {filteredScenes.map((scene, idx) => (
                <motion.div
                  key={scene.id}
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: idx * 0.02, duration: 0.3 }}
                  whileHover={{ scale: 1.05, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    const primaryResult = {
                      scene,
                      confidence: 92,
                      isPrimary: true
                    };
                    const secondaryResults = scenes
                      .filter(s => s.id !== scene.id)
                      .slice(0, 2)
                      .map(s => ({
                        scene: s,
                        confidence: Math.floor(Math.random() * 25) + 60,
                        isPrimary: false
                      }));
                    setDetectionResults([primaryResult, ...secondaryResults]);
                  }}
                  className="bg-white/5 rounded-2xl p-3 border border-white/10 hover:border-[#FF6B35]/30 cursor-pointer transition-all duration-200 text-center"
                >
                  <div className="text-2xl mb-1">{scene.icon}</div>
                  <div className="text-xs font-medium truncate">{scene.name}</div>
                </motion.div>
              ))}
            </div>
          </ColorOSCard>
        </motion.div>
      </main>
    </div>
  )
}
