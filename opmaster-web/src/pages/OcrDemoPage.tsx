import { motion } from 'framer-motion'
import { ScanText, Upload, Copy, Check } from 'lucide-react'
import { useState } from 'react'

export default function OcrDemoPage() {
  const [imageUrl, setImageUrl] = useState('')
  const [extractedText, setExtractedText] = useState('')
  const [isProcessing, setIsProcessing] = useState(false)
  const [copied, setCopied] = useState(false)

  const handleExtract = () => {
    if (!imageUrl) return
    setIsProcessing(true)
    setTimeout(() => {
      setExtractedText('OPPO Find X7 Pro\n哈苏影像系统\nMaster Parameters\n\nISO: 100\nShutter: 1/500s\nAperture: f/1.8\nWB: Auto\n\nColors: HNCS Natural\nEffect: Pro Mode')
      setIsProcessing(false)
    }, 2000)
  }

  const handleCopy = () => {
    navigator.clipboard.writeText(extractedText)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">OCR 识别</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card-oppo p-4"
        >
          <h2 className="text-sm font-medium text-text-secondary mb-4">输入图片 URL</h2>
          <div className="flex gap-2">
            <input
              type="text"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="输入图片地址或上传图片..."
              className="flex-1 px-4 py-3 bg-white/5 border border-white/10 rounded-oppo text-white placeholder-text-tertiary focus:outline-none focus:border-oppo-sunrise-gold/50 transition-colors duration-200"
              aria-label="图片 URL"
            />
            <button className="btn-secondary px-4 flex items-center gap-2 touch-feedback" aria-label="上传图片">
              <Upload className="w-5 h-5" />
            </button>
          </div>
          <button 
            onClick={handleExtract}
            disabled={!imageUrl || isProcessing}
            className="w-full mt-4 btn-primary flex items-center justify-center gap-2 touch-feedback disabled:opacity-50"
            aria-label="识别文字"
          >
            {isProcessing ? (
              <>
                <div className="w-5 h-5 border-2 border-deep-space/30 border-t-deep-space rounded-full animate-spin" />
                识别中...
              </>
            ) : (
              <>
                <ScanText className="w-5 h-5" />
                开始识别
              </>
            )}
          </button>
        </motion.section>

        {extractedText && (
          <motion.section
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="card-oppo p-4"
          >
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-medium text-text-secondary">识别结果</h2>
              <button 
                onClick={handleCopy}
                className="btn-secondary text-sm py-2 px-3 flex items-center gap-2 touch-feedback"
                aria-label="复制文字"
              >
                {copied ? (
                  <>
                    <Check className="w-4 h-4 text-oppo-green" />
                    已复制
                  </>
                ) : (
                  <>
                    <Copy className="w-4 h-4" />
                    复制
                  </>
                )}
              </button>
            </div>
            <div className="bg-white/5 rounded-oppo p-4">
              <pre className="text-text-secondary text-sm whitespace-pre-wrap font-mono">
                {extractedText}
              </pre>
            </div>
          </motion.section>
        )}

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="card-oppo p-4"
        >
          <h2 className="text-sm font-medium text-text-secondary mb-4">支持的功能</h2>
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">文字识别</p>
              <p className="text-text-tertiary text-xs">支持中英文混合识别</p>
            </div>
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">参数提取</p>
              <p className="text-text-tertiary text-xs">自动识别摄影参数</p>
            </div>
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">水印识别</p>
              <p className="text-text-tertiary text-xs">识别照片水印信息</p>
            </div>
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">一键复制</p>
              <p className="text-text-tertiary text-xs">快速复制识别结果</p>
            </div>
          </div>
        </motion.section>
      </main>
    </div>
  )
}
