import { motion } from 'framer-motion'
import { Scan, Camera, Check } from 'lucide-react'
import { useState } from 'react'

const sceneTypes = [
  '人像', '风光', '建筑', '美食', '夜景', '星空',
  '微距', '运动', '宠物', '儿童', '花卉', '树木',
  '海洋', '湖泊', '山脉', '日落', '日出', '城市',
  '街拍', '室内', '逆光', '阴天', '晴天', '雪景',
  '雨景', '雾景', '沙漠', '森林', '草原', '瀑布'
]

export default function SceneDetectionPage() {
  const [isDetecting, setIsDetecting] = useState(false)
  const [detectedScenes, setDetectedScenes] = useState<string[]>([])
  const [confidence, setConfidence] = useState(0)

  const handleDetect = () => {
    setIsDetecting(true)
    setDetectedScenes([])
    setConfidence(0)

    setTimeout(() => {
      const randomScenes = sceneTypes
        .sort(() => Math.random() - 0.5)
        .slice(0, Math.floor(Math.random() * 3) + 1)
      setDetectedScenes(randomScenes)
      setConfidence(Math.floor(Math.random() * 20) + 80)
      setIsDetecting(false)
    }, 2000)
  }

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">AI 场景识别</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card-oppo p-6 text-center"
        >
          <div className="w-24 h-24 mx-auto mb-6 rounded-full bg-gradient-to-br from-ocean-blue/30 to-aurora-purple/30 flex items-center justify-center">
            {isDetecting ? (
              <div className="w-12 h-12 border-4 border-ocean-blue/30 border-t-ocean-blue rounded-full animate-spin" />
            ) : (
              <Scan className="w-12 h-12 text-ocean-blue" />
            )}
          </div>
          
          <h2 className="text-xl font-bold mb-2">
            {isDetecting ? '正在识别场景...' : 'AI 智能场景识别'}
          </h2>
          <p className="text-text-secondary mb-6">
            {isDetecting 
              ? '正在分析当前画面，请稍候'
              : '点击按钮开始识别当前场景类型'}
          </p>

          {detectedScenes.length > 0 && (
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="mb-6"
            >
              <div className="text-sm text-text-secondary mb-2">
                识别置信度: <span className="text-oppo-green font-semibold">{confidence}%</span>
              </div>
              <div className="flex flex-wrap justify-center gap-2">
                {detectedScenes.map((scene) => (
                  <motion.span
                    key={scene}
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="px-4 py-2 bg-oppo-green/20 border border-oppo-green/30 rounded-full text-sm font-medium text-oppo-green"
                  >
                    {scene}
                  </motion.span>
                ))}
              </div>
            </motion.div>
          )}

          <button
            onClick={handleDetect}
            disabled={isDetecting}
            className="btn-primary px-8 py-3 flex items-center justify-center gap-2 mx-auto touch-feedback disabled:opacity-50"
            aria-label="开始场景识别"
          >
            <Camera className="w-5 h-5" />
            {isDetecting ? '识别中...' : '开始识别'}
          </button>
        </motion.div>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">支持的场景类型</h2>
          <div className="card-oppo p-4">
            <div className="flex flex-wrap gap-2">
              {sceneTypes.map((scene) => (
                <span
                  key={scene}
                  className="px-3 py-1.5 bg-white/5 rounded-full text-sm text-text-secondary"
                >
                  {scene}
                </span>
              ))}
            </div>
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="card-oppo p-4"
        >
          <h2 className="text-sm font-medium text-text-secondary mb-4">功能特点</h2>
          <div className="space-y-3">
            <div className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-oppo-green/20 flex items-center justify-center flex-shrink-0">
                <Check className="w-4 h-4 text-oppo-green" />
              </div>
              <div>
                <p className="font-medium text-sm mb-0.5">50+ 场景识别</p>
                <p className="text-text-tertiary text-xs">覆盖日常拍摄的大部分场景类型</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-oppo-green/20 flex items-center justify-center flex-shrink-0">
                <Check className="w-4 h-4 text-oppo-green" />
              </div>
              <div>
                <p className="font-medium text-sm mb-0.5">智能参数推荐</p>
                <p className="text-text-tertiary text-xs">根据场景自动推荐最佳滤镜和参数</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-oppo-green/20 flex items-center justify-center flex-shrink-0">
                <Check className="w-4 h-4 text-oppo-green" />
              </div>
              <div>
                <p className="font-medium text-sm mb-0.5">实时识别</p>
                <p className="text-text-tertiary text-xs">支持拍照时实时场景识别</p>
              </div>
            </div>
          </div>
        </motion.section>
      </main>
    </div>
  )
}
