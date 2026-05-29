import { motion } from 'framer-motion'
import { Copy, Check, Smartphone, Zap, Shield } from 'lucide-react'
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
  },
  {
    id: 'xiaomi',
    name: '小米 14 Ultra',
    icon: '📱',
    params: {
      '色彩模式': '徕卡自然',
      '专业模式': '开启',
      'HDR': '自动',
      'AI 魔法分身': '关闭',
      '徕卡水印': '开启'
    }
  },
  {
    id: 'vivo',
    name: 'vivo X100 Pro',
    icon: '📱',
    params: {
      '色彩模式': '蔡司自然',
      '专业模式': '开启',
      'HDR': '自动',
      '蔡司光学镜头': '启用',
      'V1 芯片': '开启'
    }
  },
  {
    id: 'huawei',
    name: '华为 Mate 60 Pro',
    icon: '📱',
    params: {
      '色彩模式': '徕卡标准',
      '专业模式': '开启',
      'HDR': '智能',
      'AI 摄影大师': '开启',
      'XMAGE': '启用'
    }
  }
]

const accessibilitySteps = [
  {
    title: '开启无障碍服务',
    desc: '在系统设置中找到"无障碍"，找到小O帮帮服务并开启',
    icon: <Shield className="w-6 h-6" />
  },
  {
    title: '选择相机品牌',
    desc: '选择您的手机品牌，系统会自动适配相应的相机控件',
    icon: <Smartphone className="w-6 h-6" />
  },
  {
    title: '一键自动填入',
    desc: '打开原生相机大师模式，点击悬浮窗即可自动填入所有参数',
    icon: <Zap className="w-6 h-6" />
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
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-orange to-hasselblad-orange rounded-2xl mb-6">
            <Smartphone className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text-oppo">
            原生相机参数自动填入
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            基于安卓无障碍服务，无需Root权限，一键自动填入相机参数，让摄影更简单
          </p>
        </motion.div>

        {/* Main Features */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="mb-12"
        >
          <div className="grid md:grid-cols-3 gap-6">
            {accessibilitySteps.map((step, idx) => (
              <motion.div
                key={idx}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 + idx * 0.1 }}
                className="card p-6"
              >
                <div className="w-12 h-12 bg-oppo-orange/20 rounded-xl flex items-center justify-center text-oppo-orange mb-4">
                  {step.icon}
                </div>
                <h3 className="text-lg font-bold mb-2">{step.title}</h3>
                <p className="text-white/60 text-sm">{step.desc}</p>
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* Brand Selection */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="mb-8"
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">选择您的手机品牌</h2>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              {cameraBrands.map((brand) => (
                <motion.button
                  key={brand.id}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setSelectedBrand(brand.id)}
                  className={`p-6 rounded-xl transition-all duration-200 ${
                    selectedBrand === brand.id
                      ? 'bg-oppo-orange/20 border border-oppo-orange/50'
                      : 'bg-white/5 border border-white/10 hover:bg-white/10'
                  }`}
                >
                  <div className="text-4xl mb-3">{brand.icon}</div>
                  <p className={`font-medium ${
                    selectedBrand === brand.id ? 'text-oppo-orange' : 'text-white'
                  }`}>{brand.name}</p>
                </motion.button>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Parameters Display */}
        {currentBrand && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="mb-8"
          >
            <div className="card p-8">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-bold">相机参数配置</h2>
                <button 
                  onClick={handleCopy}
                  className="btn-primary flex items-center gap-2"
                  aria-label="复制参数"
                >
                  {copied ? (
                    <>
                      <Check className="w-4 h-4" />
                      已复制
                    </>
                  ) : (
                    <>
                      <Copy className="w-4 h-4" />
                      复制参数
                    </>
                  )}
                </button>
              </div>
              
              <div className="grid md:grid-cols-2 gap-4">
                {Object.entries(currentBrand.params).map(([key, value]) => (
                  <motion.div
                    key={key}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="flex items-center justify-between p-4 bg-white/5 rounded-xl"
                  >
                    <span className="text-white/60">{key}</span>
                    <span className="text-white font-medium">{value}</span>
                  </motion.div>
                ))}
              </div>
            </div>
          </motion.div>
        )}

        {/* Instructions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">使用说明</h2>
            
            <div className="space-y-6">
              <div className="flex gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-oppo-orange rounded-full flex items-center justify-center text-oppo-black font-bold">1</div>
                <div>
                  <h3 className="font-medium mb-1">安装小O帮帮APP</h3>
                  <p className="text-white/60 text-sm">从官网下载并安装小O帮帮APP到您的手机</p>
                </div>
              </div>

              <div className="flex gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-oppo-orange rounded-full flex items-center justify-center text-oppo-black font-bold">2</div>
                <div>
                  <h3 className="font-medium mb-1">开启无障碍服务</h3>
                  <p className="text-white/60 text-sm">进入设置 → 无障碍 → 找到"小O帮帮"并开启</p>
                </div>
              </div>

              <div className="flex gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-oppo-orange rounded-full flex items-center justify-center text-oppo-black font-bold">3</div>
                <div>
                  <h3 className="font-medium mb-1">选择您的机型</h3>
                  <p className="text-white/60 text-sm">在APP中选择您的手机品牌和型号</p>
                </div>
              </div>

              <div className="flex gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-oppo-orange rounded-full flex items-center justify-center text-oppo-black font-bold">4</div>
                <div>
                  <h3 className="font-medium mb-1">一键自动填入</h3>
                  <p className="text-white/60 text-sm">打开原生相机大师模式，点击悬浮窗即可自动填入所有参数</p>
                </div>
              </div>
            </div>

            {/* Backup Option */}
            <div className="mt-8 p-6 bg-yellow-500/10 border border-yellow-500/20 rounded-xl">
              <h3 className="text-lg font-bold mb-3 text-yellow-400">兜底方案：悬浮窗复制粘贴</h3>
              <p className="text-white/60 text-sm">
                对于无法自动适配的机型，您可以使用悬浮窗一键复制参数，然后在相机中手动粘贴。
                我们会持续更新支持更多机型！
              </p>
            </div>
          </div>
        </motion.div>

        {/* Supported Brands */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
          className="mt-8"
        >
          <div className="card p-8">
            <h2 className="text-xl font-bold mb-6">已支持的品牌</h2>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              {['OPPO', 'OnePlus', 'realme', '小米', 'vivo', '华为'].map((brand) => (
                <div key={brand} className="p-4 bg-white/5 rounded-xl text-center">
                  <p className="font-medium">{brand}</p>
                  <div className="flex items-center justify-center gap-1 mt-2">
                    <Check className="w-4 h-4 text-green-500" />
                    <span className="text-sm text-green-500">已支持</span>
                  </div>
                </div>
              ))}
            </div>
            <p className="text-center text-white/40 mt-4 text-sm">更多品牌持续更新中...</p>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
