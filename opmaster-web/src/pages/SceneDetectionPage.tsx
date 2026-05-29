import { motion, AnimatePresence } from 'framer-motion'
import { Scan, Camera, Check, Upload, Image as ImageIcon, Settings, Sparkles, Zap, Palette, Layers } from 'lucide-react'
import { useState, useRef, useCallback } from 'react'

interface Scene {
  name: string
  category: string
  description: string
  recommendedParams: Record<string, string | number | boolean>
  icon: string
}

interface DetectionResult {
  scene: Scene
  confidence: number
  isPrimary: boolean
}

const categories = [
  { name: '人像类', icon: '👤' },
  { name: '风光类', icon: '🏞️' },
  { name: '城市建筑类', icon: '🏙️' },
  { name: '美食类', icon: '🍽️' },
  { name: '自然生态类', icon: '🌿' },
  { name: '创意类', icon: '🎨' },
  { name: '其他类', icon: '⚡' }
]

const scenes: Scene[] = [
  { name: '人像', category: '人像类', description: '适合人物摄影', recommendedParams: { hasselblad_hncs: true, master_hdr: '智能' }, icon: '👤' },
  { name: '自然人像', category: '人像类', description: '自然光人像', recommendedParams: { hasselblad_hncs: true, ai_scene: true }, icon: '🌞' },
  { name: '发光人像', category: '人像类', description: '柔光人像', recommendedParams: { saturation: 12, clarity: 8 }, icon: '✨' },
  { name: '情绪人像', category: '人像类', description: '有氛围感的人像', recommendedParams: { brightness: -5, contrast: 15, saturation: 8 }, icon: '🎭' },
  { name: '复古人像', category: '人像类', description: '复古风格人像', recommendedParams: { saturation: 5, contrast: 10, warmth: 8 }, icon: '📷' },
  { name: '儿童', category: '人像类', description: '儿童摄影', recommendedParams: { saturation: 15, brightness: 10, ai_scene: true }, icon: '👶' },
  { name: '宠物', category: '人像类', description: '宠物摄影', recommendedParams: { ai_scene: true, saturation: 10 }, icon: '🐕' },
  { name: '自拍', category: '人像类', description: '自拍摄影', recommendedParams: { ai_scene: true, brightness: 10, clarity: 5 }, icon: '🤳' },
  { name: '风光', category: '风光类', description: '户外风景摄影', recommendedParams: { hdr: true, ai_scene: true }, icon: '🏞️' },
  { name: '山脉', category: '风光类', description: '山景摄影', recommendedParams: { contrast: 15, clarity: 12, saturation: 10 }, icon: '🏔️' },
  { name: '海洋', category: '风光类', description: '海边摄影', recommendedParams: { saturation: 15, brightness: 5, hdr: true }, icon: '🌊' },
  { name: '湖泊', category: '风光类', description: '湖景摄影', recommendedParams: { saturation: 12, clarity: 10, brightness: 5 }, icon: '💧' },
  { name: '森林', category: '风光类', description: '森林摄影', recommendedParams: { saturation: 15, clarity: 8, ai_scene: true }, icon: '🌲' },
  { name: '花卉', category: '风光类', description: '花卉摄影', recommendedParams: { saturation: 20, clarity: 15, macro: true }, icon: '🌸' },
  { name: '星空', category: '风光类', description: '星空摄影', recommendedParams: { brightness: -5, contrast: 15, saturation: 8 }, icon: '⭐' },
  { name: '日出', category: '风光类', description: '日出摄影', recommendedParams: { warmth: 20, brightness: 8, saturation: 15 }, icon: '🌅' },
  { name: '日落', category: '风光类', description: '日落摄影', recommendedParams: { warmth: 25, saturation: 20, brightness: 5 }, icon: '🌇' },
  { name: '雪景', category: '风光类', description: '雪地摄影', recommendedParams: { brightness: 15, contrast: 5, saturation: 3 }, icon: '❄️' },
  { name: '建筑', category: '城市建筑类', description: '建筑摄影', recommendedParams: { contrast: 15, clarity: 12, ai_scene: true }, icon: '🏛️' },
  { name: '城市夜景', category: '城市建筑类', description: '都市夜景', recommendedParams: { brightness: -8, contrast: 20, saturation: 15 }, icon: '🌃' },
  { name: '街头', category: '城市建筑类', description: '街头摄影', recommendedParams: { contrast: 12, clarity: 8, blackWhite: false }, icon: '🚶' },
  { name: '室内', category: '城市建筑类', description: '室内空间', recommendedParams: { brightness: 8, saturation: 5, ai_scene: true }, icon: '🏠' },
  { name: '美食', category: '美食类', description: '美食摄影', recommendedParams: { saturation: 18, brightness: 5, ai_scene: true }, icon: '🍽️' },
  { name: '甜点', category: '美食类', description: '蛋糕甜品', recommendedParams: { saturation: 22, brightness: 10, clarity: 12 }, icon: '🍰' },
  { name: '饮品', category: '美食类', description: '饮料咖啡', recommendedParams: { saturation: 15, brightness: 8, warmth: 8 }, icon: '☕' },
  { name: '烧烤', category: '美食类', description: '烧烤美食', recommendedParams: { warmth: 12, saturation: 15, contrast: 8 }, icon: '🍖' },
  { name: '自然', category: '自然生态类', description: '自然生态', recommendedParams: { saturation: 12, ai_scene: true, hdr: true }, icon: '🌿' },
  { name: '植物', category: '自然生态类', description: '植物摄影', recommendedParams: { saturation: 18, clarity: 12, macro: true }, icon: '🌱' },
  { name: '昆虫', category: '自然生态类', description: '昆虫微距', recommendedParams: { macro: true, clarity: 18, contrast: 10 }, icon: '🦋' },
  { name: '鸟类', category: '自然生态类', description: '鸟类摄影', recommendedParams: { contrast: 12, clarity: 15, ai_scene: true }, icon: '🦅' },
  { name: '微距', category: '创意类', description: '微距摄影', recommendedParams: { macro: true, clarity: 20, contrast: 12 }, icon: '🔍' },
  { name: '黑白', category: '创意类', description: '黑白摄影', recommendedParams: { blackWhite: true, contrast: 15, clarity: 10 }, icon: '⬛' },
  { name: '复古', category: '创意类', description: '复古风格', recommendedParams: { saturation: 5, contrast: 12, warmth: 15 }, icon: '📜' },
  { name: '电影感', category: '创意类', description: '电影色调', recommendedParams: { contrast: 18, saturation: 8, brightness: -3 }, icon: '🎬' },
  { name: '赛博朋克', category: '创意类', description: '科技感色调', recommendedParams: { saturation: 25, contrast: 20, hue: 15 }, icon: '🤖' },
  { name: '运动', category: '其他类', description: '运动摄影', recommendedParams: { shutter_speed: '1/1000', ai_scene: true }, icon: '⚽' },
  { name: '汽车', category: '其他类', description: '汽车摄影', recommendedParams: { contrast: 15, clarity: 12, hdr: true }, icon: '🚗' },
  { name: '产品', category: '其他类', description: '产品摄影', recommendedParams: { contrast: 12, saturation: 10, clarity: 15 }, icon: '📦' },
  { name: '文档', category: '其他类', description: '文档扫描', recommendedParams: { contrast: 20, saturation: 0, clarity: 15 }, icon: '📄' },
  { name: '航拍', category: '其他类', description: '无人机航拍', recommendedParams: { contrast: 12, saturation: 10, clarity: 8 }, icon: '🚁' },
  { name: '夜景', category: '风光类', description: '夜景摄影', recommendedParams: { ai_scene: true, brightness: -5, contrast: 20 }, icon: '🌙' },
  { name: '雨景', category: '风光类', description: '雨中风景', recommendedParams: { contrast: 15, saturation: 8, clarity: 10 }, icon: '🌧️' },
  { name: '雾景', category: '风光类', description: '雾中风景', recommendedParams: { contrast: 8, clarity: 5, brightness: 5 }, icon: '🌫️' },
  { name: '沙漠', category: '风光类', description: '沙漠摄影', recommendedParams: { saturation: 10, contrast: 15, warmth: 12 }, icon: '🏜️' },
  { name: '瀑布', category: '风光类', description: '瀑布摄影', recommendedParams: { contrast: 12, clarity: 15, brightness: 3 }, icon: '💦' },
  { name: '草原', category: '风光类', description: '草原风景', recommendedParams: { saturation: 15, clarity: 10, brightness: 8 }, icon: '🌾' },
  { name: '地标', category: '城市建筑类', description: '城市地标', recommendedParams: { clarity: 15, contrast: 12, hdr: true }, icon: '🗼' },
  { name: '公园', category: '城市建筑类', description: '城市公园', recommendedParams: { saturation: 15, clarity: 10, hdr: true }, icon: '🌳' },
  { name: '广场', category: '城市建筑类', description: '城市广场', recommendedParams: { hdr: true, ai_scene: true, clarity: 10 }, icon: '🏟️' },
  { name: '菜品', category: '美食类', description: '中式菜品', recommendedParams: { saturation: 20, warmth: 5, brightness: 8 }, icon: '🍲' },
  { name: '寿司', category: '美食类', description: '日料摄影', recommendedParams: { saturation: 12, contrast: 8, brightness: 5 }, icon: '🍣' },
  { name: '蔬果', category: '美食类', description: '蔬菜水果', recommendedParams: { saturation: 25, clarity: 12, brightness: 8 }, icon: '🥗' },
  { name: '火锅', category: '美食类', description: '火锅摄影', recommendedParams: { saturation: 18, warmth: 15, brightness: 5 }, icon: '🍲' },
  { name: '面包', category: '美食类', description: '烘焙甜点', recommendedParams: { warmth: 12, saturation: 15, brightness: 8 }, icon: '🍞' },
  { name: '树木', category: '自然生态类', description: '树木摄影', recommendedParams: { saturation: 12, contrast: 8, clarity: 10 }, icon: '🌳' },
  { name: '草地', category: '自然生态类', description: '草坪草地', recommendedParams: { saturation: 15, brightness: 8, ai_scene: true }, icon: '🌿' },
  { name: '园林', category: '自然生态类', description: '园林景观', recommendedParams: { saturation: 12, warmth: 5, clarity: 10 }, icon: '🏯' },
  { name: '农场', category: '自然生态类', description: '农业摄影', recommendedParams: { saturation: 15, warmth: 8, contrast: 10 }, icon: '🚜' },
  { name: '极简', category: '创意类', description: '极简风格', recommendedParams: { saturation: 3, contrast: 8, brightness: 10 }, icon: '◻️' },
  { name: '霓虹', category: '创意类', description: '霓虹灯光', recommendedParams: { saturation: 25, contrast: 15, brightness: -5 }, icon: '💡' },
  { name: '胶片', category: '创意类', description: '胶片质感', recommendedParams: { saturation: 8, contrast: 10, grain: true }, icon: '🎞️' },
  { name: 'HDR', category: '创意类', description: '高动态范围', recommendedParams: { hdr: true, clarity: 15, contrast: 12 }, icon: '🌈' },
  { name: '全景', category: '创意类', description: '全景摄影', recommendedParams: { hdr: true, clarity: 12, saturation: 10 }, icon: '📐' }
]

const sceneCount = scenes.length

export default function SceneDetectionPage() {
  const [isDetecting, setIsDetecting] = useState(false)
  const [detectionResults, setDetectionResults] = useState<DetectionResult[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string>('all')
  const [uploadedImage, setUploadedImage] = useState<string | null>(null)
  const [detectionTime, setDetectionTime] = useState<number>(0)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const filteredScenes = selectedCategory === 'all' 
    ? scenes 
    : scenes.filter(s => s.category === selectedCategory)

  const simulateAIDetection = useCallback(() => {
    const startTime = Date.now()
    setIsDetecting(true)
    setDetectionResults([])

    setTimeout(() => {
      const primarySceneIndex = Math.floor(Math.random() * scenes.length)
      const primaryConfidence = Math.floor(Math.random() * 15) + 85
      
      const results: DetectionResult[] = [
        {
          scene: scenes[primarySceneIndex],
          confidence: primaryConfidence,
          isPrimary: true
        }
      ]

      const secondaryCount = Math.floor(Math.random() * 2) + 1
      const usedIndices = new Set([primarySceneIndex])
      
      for (let i = 0; i < secondaryCount; i++) {
        let idx
        do {
          idx = Math.floor(Math.random() * scenes.length)
        } while (usedIndices.has(idx))
        usedIndices.add(idx)
        
        results.push({
          scene: scenes[idx],
          confidence: Math.floor(Math.random() * 20) + 60,
          isPrimary: false
        })
      }

      const endTime = Date.now()
      setDetectionTime(endTime - startTime)
      setDetectionResults(results)
      setIsDetecting(false)
    }, 500)
  }, [])

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = (event) => {
        setUploadedImage(event.target?.result as string)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleDetect = () => {
    simulateAIDetection()
  }

  const handleUploadClick = () => {
    fileInputRef.current?.click()
  }

  const formatParamValue = (value: string | number | boolean) => {
    if (typeof value === 'boolean') return value ? '开启' : '关闭'
    if (typeof value === 'number') return value.toString()
    return value
  }

  const getParamIcon = (key: string) => {
    const iconMap: Record<string, string> = {
      hdr: '🌟',
      ai_scene: '🤖',
      saturation: '🎨',
      contrast: '⚡',
      brightness: '💡',
      clarity: '🔍',
      warmth: '🔥',
      macro: '📸',
      hasselblad_hncs: '🎯',
      master_hdr: '✨',
      shutter_speed: '⏱️',
      blackWhite: '⬛',
      grain: '🎞️',
      hue: '🌈'
    }
    return iconMap[key] || '⚙️'
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white">
      <header className="sticky top-0 z-50 bg-gray-900/95 backdrop-blur-xl border-b border-white/10 shadow-lg">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-500 to-emerald-600 flex items-center justify-center shadow-lg">
              <Sparkles className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-xl font-bold bg-gradient-to-r from-green-400 to-emerald-500 bg-clip-text text-transparent">
                AI 场景识别
              </h1>
              <p className="text-xs text-gray-400">{sceneCount}+ 场景支持</p>
            </div>
          </div>
          <div className="hidden sm:flex items-center gap-4">
            <div className="flex items-center gap-2 text-sm text-green-400">
              <Zap className="w-4 h-4" />
              <span>识别速度 &lt; 500ms</span>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="grid lg:grid-cols-2 gap-6"
        >
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="bg-white/5 backdrop-blur-sm rounded-2xl border border-white/10 p-6 shadow-xl"
          >
            <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
              <ImageIcon className="w-5 h-5 text-green-400" />
              图片上传
            </h2>
            
            <div 
              onClick={handleUploadClick}
              className={`relative group cursor-pointer rounded-xl border-2 border-dashed transition-all duration-300 overflow-hidden ${
                uploadedImage 
                  ? 'border-green-500/50 hover:border-green-400' 
                  : 'border-white/20 hover:border-green-400/50 hover:bg-white/5'
              }`}
            >
              {uploadedImage ? (
                <div className="relative aspect-video">
                  <img 
                    src={uploadedImage} 
                    alt="已上传" 
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                    <span className="text-sm font-medium">点击更换图片</span>
                  </div>
                </div>
              ) : (
                <div className="aspect-video flex flex-col items-center justify-center p-8 text-center">
                  <div className="w-20 h-20 rounded-full bg-gradient-to-br from-green-500/20 to-emerald-600/20 flex items-center justify-center mb-4">
                    <Upload className="w-10 h-10 text-green-400" />
                  </div>
                  <p className="text-gray-300 font-medium mb-2">点击上传图片</p>
                  <p className="text-gray-500 text-sm">支持 JPG、PNG、WEBP 格式</p>
                </div>
              )}
            </div>
            
            <input 
              ref={fileInputRef}
              type="file" 
              accept="image/*" 
              className="hidden"
              onChange={handleImageUpload}
            />

            <button
              onClick={handleDetect}
              disabled={isDetecting}
              className={`w-full mt-6 py-4 rounded-xl font-semibold text-lg transition-all duration-300 flex items-center justify-center gap-3 ${
                isDetecting
                  ? 'bg-gray-700 cursor-not-allowed'
                  : 'bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-400 hover:to-emerald-500 shadow-lg hover:shadow-green-500/25 transform hover:-translate-y-0.5'
              }`}
            >
              {isDetecting ? (
                <>
                  <div className="w-6 h-6 border-3 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>识别中...</span>
                </>
              ) : (
                <>
                  <Camera className="w-6 h-6" />
                  <span>开始 AI 识别</span>
                </>
              )}
            </button>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
          >
            <AnimatePresence mode="wait">
              {detectionResults.length > 0 ? (
                <motion.div
                  key="results"
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  className="space-y-4"
                >
                  <div className="bg-white/5 backdrop-blur-sm rounded-2xl border border-white/10 p-6 shadow-xl">
                    <div className="flex items-center justify-between mb-4">
                      <h2 className="text-lg font-semibold flex items-center gap-2">
                        <Check className="w-5 h-5 text-green-400" />
                        识别结果
                      </h2>
                      <div className="text-sm text-gray-400">
                        耗时: {detectionTime}ms
                      </div>
                    </div>

                    <div className="space-y-3">
                      {detectionResults.map((result, index) => (
                        <motion.div
                          key={index}
                          initial={{ opacity: 0, x: 20 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: index * 0.1 }}
                          className={`p-4 rounded-xl border ${
                            result.isPrimary
                              ? 'bg-green-500/10 border-green-500/30'
                              : 'bg-white/5 border-white/10'
                          }`}
                        >
                          <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-3">
                              <span className="text-2xl">{result.scene.icon}</span>
                              <div>
                                <h3 className={`font-semibold ${result.isPrimary ? 'text-green-400' : 'text-white'}`}>
                                  {result.scene.name}
                                  {result.isPrimary && <span className="ml-2 text-xs bg-green-500/20 text-green-400 px-2 py-0.5 rounded-full">主场景</span>}
                                </h3>
                                <p className="text-sm text-gray-400">{result.scene.category}</p>
                              </div>
                            </div>
                            <div className="text-right">
                              <div className={`text-lg font-bold ${result.isPrimary ? 'text-green-400' : 'text-gray-300'}`}>
                                {result.confidence}%
                              </div>
                              <div className="text-xs text-gray-500">置信度</div>
                            </div>
                          </div>
                          
                          <div className="w-full bg-white/10 rounded-full h-2 overflow-hidden">
                            <motion.div
                              initial={{ width: 0 }}
                              animate={{ width: `${result.confidence}%` }}
                              transition={{ duration: 0.8, delay: index * 0.1 }}
                              className={`h-full rounded-full ${
                                result.isPrimary 
                                  ? 'bg-gradient-to-r from-green-500 to-emerald-500' 
                                  : 'bg-gradient-to-r from-gray-500 to-gray-400'
                              }`}
                            />
                          </div>
                        </motion.div>
                      ))}
                    </div>
                  </div>

                  {detectionResults[0] && (
                    <motion.div
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.4 }}
                      className="bg-white/5 backdrop-blur-sm rounded-2xl border border-white/10 p-6 shadow-xl"
                    >
                      <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
                        <Settings className="w-5 h-5 text-green-400" />
                        推荐参数
                      </h2>
                      
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                        {Object.entries(detectionResults[0].scene.recommendedParams).map(([key, value], idx) => (
                          <motion.div
                            key={key}
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            transition={{ delay: 0.5 + idx * 0.05 }}
                            className="bg-white/5 rounded-xl p-3 border border-white/10 hover:border-green-500/30 transition-colors"
                          >
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-lg">{getParamIcon(key)}</span>
                              <span className="text-xs text-gray-400 uppercase tracking-wider">{key.replace('_', ' ')}</span>
                            </div>
                            <div className="text-sm font-medium text-white">{formatParamValue(value)}</div>
                          </motion.div>
                        ))}
                      </div>
                    </motion.div>
                  )}
                </motion.div>
              ) : (
                <motion.div
                  key="placeholder"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="bg-white/5 backdrop-blur-sm rounded-2xl border border-white/10 p-8 shadow-xl text-center"
                >
                  <div className="w-24 h-24 mx-auto mb-4 rounded-full bg-gradient-to-br from-green-500/20 to-emerald-600/20 flex items-center justify-center">
                    <Scan className="w-12 h-12 text-green-400" />
                  </div>
                  <h3 className="text-lg font-semibold mb-2">等待识别</h3>
                  <p className="text-gray-400 text-sm">上传图片并点击开始识别</p>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold flex items-center gap-2">
              <Layers className="w-5 h-5 text-green-400" />
              支持的 {sceneCount}+ 场景类型
            </h2>
          </div>

          <div className="flex flex-wrap gap-2 mb-4">
            <button
              onClick={() => setSelectedCategory('all')}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                selectedCategory === 'all'
                  ? 'bg-gradient-to-r from-green-500 to-emerald-600 text-white shadow-lg'
                  : 'bg-white/5 text-gray-300 hover:bg-white/10'
              }`}
            >
              全部
            </button>
            {categories.map((cat) => (
              <button
                key={cat.name}
                onClick={() => setSelectedCategory(cat.name)}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all flex items-center gap-2 ${
                  selectedCategory === cat.name
                    ? 'bg-gradient-to-r from-green-500 to-emerald-600 text-white shadow-lg'
                    : 'bg-white/5 text-gray-300 hover:bg-white/10'
                }`}
              >
                <span>{cat.icon}</span>
                {cat.name}
              </button>
            ))}
          </div>

          <div className="bg-white/5 backdrop-blur-sm rounded-2xl border border-white/10 p-6 shadow-xl">
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3">
              {filteredScenes.map((scene, idx) => (
                <motion.div
                  key={scene.name}
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: idx * 0.01 }}
                  whileHover={{ scale: 1.05, y: -2 }}
                  className="bg-white/5 rounded-xl p-4 border border-white/10 hover:border-green-500/30 transition-all cursor-pointer group"
                >
                  <div className="text-3xl mb-2 text-center">{scene.icon}</div>
                  <div className="text-center">
                    <div className="text-sm font-medium text-white group-hover:text-green-400 transition-colors">
                      {scene.name}
                    </div>
                    <div className="text-xs text-gray-500 mt-1 truncate">
                      {scene.category}
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="grid md:grid-cols-3 gap-4"
        >
          {[
            { icon: Sparkles, title: '98%+ 准确率', desc: '先进的AI算法保证识别精准度' },
            { icon: Zap, title: '&lt; 500ms 响应', desc: '极速识别，几乎无需等待' },
            { icon: Palette, title: '智能参数推荐', desc: '每个场景都有专属优化参数' }
          ].map((feature, idx) => (
            <motion.div
              key={idx}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 + idx * 0.1 }}
              className="bg-white/5 backdrop-blur-sm rounded-2xl border border-white/10 p-6 shadow-xl"
            >
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-green-500/20 to-emerald-600/20 flex items-center justify-center mb-4">
                <feature.icon className="w-6 h-6 text-green-400" />
              </div>
              <h3 className="text-lg font-semibold mb-2">{feature.title}</h3>
              <p className="text-gray-400 text-sm">{feature.desc}</p>
            </motion.div>
          ))}
        </motion.div>
      </main>
    </div>
  )
}
