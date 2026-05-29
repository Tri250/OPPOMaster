import { motion } from 'framer-motion'
import { Wand2, Eye, RefreshCw, Check } from 'lucide-react'
import { useState } from 'react'
import { ColorOSSlider } from '../components/common/ColorOSComponents'

export default function AiFineTunePage() {
  const [isProcessing, setIsProcessing] = useState(false)
  const [applied, setApplied] = useState(false)
  const [params, setParams] = useState({
    portraitDetail: 50,
    skinTone: 50,
    lighting: 50,
    background: 50
  })

  const handleApply = () => {
    setIsProcessing(true)
    setApplied(false)
    setTimeout(() => {
      setIsProcessing(false)
      setApplied(true)
      setTimeout(() => setApplied(false), 2000)
    }, 2000)
  }

  const handleReset = () => {
    setParams({
      portraitDetail: 50,
      skinTone: 50,
      lighting: 50,
      background: 50
    })
  }

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">AI 微调</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card-oppo p-6 text-center"
        >
          <div className="w-24 h-24 mx-auto mb-6 rounded-full bg-gradient-to-br from-aurora-purple/30 to-ocean-blue/30 flex items-center justify-center">
            {isProcessing ? (
              <div className="w-12 h-12 border-4 border-aurora-purple/30 border-t-aurora-purple rounded-full animate-spin" />
            ) : (
              <Wand2 className="w-12 h-12 text-aurora-purple" />
            )}
          </div>
          
          <h2 className="text-xl font-bold mb-2">
            {isProcessing ? '正在智能优化...' : 'AI 人像细节调整'}
          </h2>
          <p className="text-text-secondary mb-6">
            {isProcessing 
              ? 'AI 正在分析并优化人像细节'
              : '智能调节人像细节，打造完美效果'}
          </p>

          <div className="flex gap-3 justify-center">
            <button
              onClick={handleApply}
              disabled={isProcessing}
              className="btn-primary px-6 py-3 flex items-center gap-2 touch-feedback disabled:opacity-50"
              aria-label="应用 AI 微调"
            >
              {isProcessing ? (
                <>
                  <div className="w-5 h-5 border-2 border-deep-space/30 border-t-deep-space rounded-full animate-spin" />
                  处理中...
                </>
              ) : applied ? (
                <>
                  <Check className="w-5 h-5" />
                  已应用
                </>
              ) : (
                <>
                  <Wand2 className="w-5 h-5" />
                  一键优化
                </>
              )}
            </button>
            <button
              onClick={handleReset}
              className="btn-secondary px-6 py-3 flex items-center gap-2 touch-feedback"
              aria-label="重置参数"
            >
              <RefreshCw className="w-5 h-5" />
              重置
            </button>
          </div>
        </motion.div>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="card-oppo p-4"
        >
          <h2 className="text-sm font-medium text-text-secondary mb-4">微调参数</h2>
          <div className="space-y-5">
            <ColorOSSlider
              value={params.portraitDetail}
              onChange={(val) => setParams(p => ({ ...p, portraitDetail: val }))}
              label="人像细节"
              unit="%"
            />
            <ColorOSSlider
              value={params.skinTone}
              onChange={(val) => setParams(p => ({ ...p, skinTone: val }))}
              label="肤色优化"
              unit="%"
            />
            <ColorOSSlider
              value={params.lighting}
              onChange={(val) => setParams(p => ({ ...p, lighting: val }))}
              label="光影调节"
              unit="%"
            />
            <ColorOSSlider
              value={params.background}
              onChange={(val) => setParams(p => ({ ...p, background: val }))}
              label="背景虚化"
              unit="%"
            />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="card-oppo p-4"
        >
          <h2 className="text-sm font-medium text-text-secondary mb-4">功能说明</h2>
          <div className="space-y-3 text-sm text-text-tertiary">
            <div className="flex items-start gap-3">
              <Eye className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <p><span className="text-white font-medium">人像细节</span>：智能识别并优化面部特征，保留自然纹理</p>
            </div>
            <div className="flex items-start gap-3">
              <Eye className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <p><span className="text-white font-medium">肤色优化</span>：自动调整肤色，使皮肤更加通透自然</p>
            </div>
            <div className="flex items-start gap-3">
              <Eye className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <p><span className="text-white font-medium">光影调节</span>：智能补光，塑造立体五官轮廓</p>
            </div>
            <div className="flex items-start gap-3">
              <Eye className="w-4 h-4 mt-0.5 flex-shrink-0" />
              <p><span className="text-white font-medium">背景虚化</span>：模拟大光圈效果，突出人物主体</p>
            </div>
          </div>
        </motion.section>
      </main>
    </div>
  )
}
