import { motion } from 'framer-motion'
import { Copy, Check } from 'lucide-react'
import { useState } from 'react'

const cameraBrands = [
  {
    id: 'oppo',
    name: 'OPPO Find X7',
    icon: '📱',
    params: {
      '色彩模式': 'HNCS 自然',
      '专业模式': '开启',
      'HDR': '自动',
      'AI 场景增强': '开启',
      '夜景模式': '智能'
    }
  },
  {
    id: 'oneplus',
    name: 'OnePlus 12',
    icon: '📱',
    params: {
      '色彩模式': '自然',
      '专业模式': '开启',
      'HDR': '自动',
      'AI 增强': '开启',
      ' Nightscape': '开启'
    }
  },
  {
    id: 'realme',
    name: 'realme GT5 Pro',
    icon: '📱',
    params: {
      '色彩模式': '鲜明',
      '专业模式': '开启',
      'HDR': '自动',
      'AI 场景': '开启',
      '星空模式': '开启'
    }
  }
]

export default function NativeCameraPage() {
  const [selectedBrand, setSelectedBrand] = useState('oppo')
  const [copied, setCopied] = useState(false)

  const currentBrand = cameraBrands.find(b => b.id === selectedBrand)

  const handleCopy = () => {
    const text = Object.entries(currentBrand!.params)
      .map(([key, value]) => `${key}: ${value}`)
      .join('\n')
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">原生相机参数</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card-oppo p-4"
        >
          <h2 className="text-sm font-medium text-text-secondary mb-4">选择机型</h2>
          <div className="grid grid-cols-3 gap-3">
            {cameraBrands.map((brand) => (
              <motion.button
                key={brand.id}
                whileTap={{ scale: 0.95 }}
                onClick={() => setSelectedBrand(brand.id)}
                className={`p-4 rounded-oppo transition-all duration-200 touch-feedback ${
                  selectedBrand === brand.id
                    ? 'bg-oppo-sunrise-gold/10 border border-oppo-sunrise-gold/30'
                    : 'bg-white/5 border border-transparent hover:bg-white/10'
                }`}
              >
                <div className="text-3xl mb-2">{brand.icon}</div>
                <p className={`text-sm font-medium ${
                  selectedBrand === brand.id ? 'text-oppo-sunrise-gold' : ''
                }`}>{brand.name}</p>
              </motion.button>
            ))}
          </div>
        </motion.section>

        {currentBrand && (
          <motion.section
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="card-oppo p-4"
          >
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-medium text-text-secondary">相机参数</h2>
              <button 
                onClick={handleCopy}
                className="btn-secondary text-sm py-2 px-3 flex items-center gap-2 touch-feedback"
                aria-label="复制参数"
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
            <div className="space-y-3">
              {Object.entries(currentBrand.params).map(([key, value]) => (
                <div key={key} className="flex items-center justify-between py-2 border-b border-white/5 last:border-0">
                  <span className="text-text-secondary text-sm">{key}</span>
                  <span className="text-white font-medium text-sm">{value}</span>
                </div>
              ))}
            </div>
          </motion.section>
        )}

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="card-oppo p-4"
        >
          <h2 className="text-sm font-medium text-text-secondary mb-4">使用说明</h2>
          <div className="space-y-3 text-sm text-text-tertiary">
            <p>1. 选择您的手机品牌和型号</p>
            <p>2. 查看推荐的专业相机参数设置</p>
            <p>3. 点击复制按钮将参数复制到剪贴板</p>
            <p>4. 打开原生相机应用，手动设置相应参数</p>
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="card-oppo p-4"
        >
          <h2 className="text-sm font-medium text-text-secondary mb-4">支持的机型</h2>
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">OPPO</p>
              <p className="text-text-tertiary text-xs">Find X7 系列</p>
            </div>
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">OnePlus</p>
              <p className="text-text-tertiary text-xs">OnePlus 12</p>
            </div>
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">realme</p>
              <p className="text-text-tertiary text-xs">GT5 Pro</p>
            </div>
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">更多</p>
              <p className="text-text-tertiary text-xs">持续更新中</p>
            </div>
          </div>
        </motion.section>
      </main>
    </div>
  )
}
