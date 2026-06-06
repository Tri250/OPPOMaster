import { motion } from 'framer-motion'
import { useInView } from 'framer-motion'
import { useRef, useState } from 'react'
import { Download, Github, ExternalLink, QrCode, X, Check } from 'lucide-react'

const downloadOptions = [
  {
    id: 'github',
    name: 'GitHub Releases',
    description: '官方下载渠道，获取最新版本',
    icon: Github,
    url: 'https://github.com/iCurrer/OMaster/releases',
    color: '#FFFFFF'
  },
  {
    id: 'pgyer',
    name: '蒲公英',
    description: '国内快速下载，无需翻墙',
    icon: ExternalLink,
    url: 'https://www.pgyer.com/omaster-android',
    color: '#3B82F6'
  },
  {
    id: 'lanzou',
    name: '蓝奏云',
    description: '备用下载渠道，稳定可靠',
    icon: ExternalLink,
    url: 'https://wwbwy.lanzouu.com/b016klqmib',
    color: '#10B981'
  }
]

const requirements = [
  'Android 13 (API 33) 及以上',
  '支持悬浮窗权限',
  '无需Root权限',
  '完全免费开源'
]

export default function DownloadSection() {
  const sectionRef = useRef(null)
  const isInView = useInView(sectionRef, { once: true, margin: "-100px" })
  const [showQR, setShowQR] = useState(false)

  return (
    <section ref={sectionRef} className="py-24 bg-[#0D1117] relative overflow-hidden">
      <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-[#30363D] to-transparent" />
      
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden">
        <motion.div
          className="absolute -top-40 -right-40 w-80 h-80 bg-[#FF6B35]/10 rounded-full blur-[100px]"
          animate={{
            scale: [1, 1.2, 1],
            opacity: [0.3, 0.5, 0.3]
          }}
          transition={{
            duration: 4,
            repeat: Infinity,
            ease: "easeInOut"
          }}
        />
        <motion.div
          className="absolute -bottom-40 -left-40 w-80 h-80 bg-[#58A6FF]/10 rounded-full blur-[100px]"
          animate={{
            scale: [1.2, 1, 1.2],
            opacity: [0.3, 0.5, 0.3]
          }}
          transition={{
            duration: 4,
            repeat: Infinity,
            ease: "easeInOut"
          }}
        />
      </div>
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left: Content */}
          <motion.div
            initial={{ opacity: 0, x: -30 }}
            animate={isInView ? { opacity: 1, x: 0 } : {}}
            transition={{ duration: 0.6 }}
          >
            <span className="inline-block px-4 py-1.5 rounded-full bg-[#FF6B35]/10 text-[#FF6B35] text-sm font-medium mb-4">
              立即下载
            </span>
            <h2 className="text-4xl sm:text-5xl font-bold text-white mb-6">
              开始你的<span className="text-[#FF6B35]">摄影之旅</span>
            </h2>
            <p className="text-xl text-gray-400 mb-8">
              免费下载 OMaster，告别参数焦虑，让每一次拍摄都能获得完美的色彩。
            </p>

            {/* Requirements */}
            <div className="space-y-3 mb-8">
              {requirements.map((req, index) => (
                <div key={index} className="flex items-center gap-3 text-gray-300">
                  <div className="w-5 h-5 rounded-full bg-green-500/20 flex items-center justify-center">
                    <Check size={12} className="text-green-400" />
                  </div>
                  {req}
                </div>
              ))}
            </div>

            {/* QR Code button */}
            <motion.button
              onClick={() => setShowQR(true)}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="flex items-center gap-2 px-6 py-3 bg-[#30363D] hover:bg-[#484F58] text-white rounded-xl font-medium transition-colors"
            >
              <QrCode size={20} />
              扫码下载
            </motion.button>
          </motion.div>

          {/* Right: Download cards */}
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            animate={isInView ? { opacity: 1, x: 0 } : {}}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="space-y-4"
          >
            {downloadOptions.map((option, index) => {
              const Icon = option.icon
              return (
                <motion.a
                  key={option.id}
                  href={option.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  initial={{ opacity: 0, y: 20 }}
                  animate={isInView ? { opacity: 1, y: 0 } : {}}
                  transition={{ duration: 0.4, delay: 0.3 + index * 0.1 }}
                  whileHover={{ scale: 1.02, x: 8 }}
                  className="group flex items-center gap-4 p-5 bg-[#161B22] rounded-xl border border-[#30363D] hover:border-[#FF6B35]/50 transition-all duration-300"
                >
                  <div 
                    className="w-12 h-12 rounded-xl flex items-center justify-center transition-colors"
                    style={{ backgroundColor: `${option.color}15` }}
                  >
                    <Icon size={24} style={{ color: option.color }} />
                  </div>
                  <div className="flex-1">
                    <h3 className="text-white font-semibold group-hover:text-[#FF6B35] transition-colors">
                      {option.name}
                    </h3>
                    <p className="text-gray-500 text-sm">{option.description}</p>
                  </div>
                  <Download size={20} className="text-gray-400 group-hover:text-[#FF6B35] transition-colors" />
                </motion.a>
              )
            })}
          </motion.div>
        </div>

        {/* Footer */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={isInView ? { opacity: 1 } : {}}
          transition={{ duration: 0.6, delay: 0.6 }}
          className="mt-20 pt-8 border-t border-[#30363D] text-center"
        >
          <p className="text-gray-500 text-sm">
            Made with ❤️ by OMaster Team | 
            <a href="https://github.com/iCurrer/OMaster" target="_blank" rel="noopener noreferrer" className="text-[#FF6B35] hover:underline ml-1">
              GitHub
            </a>
          </p>
        </motion.div>
      </div>

      {/* QR Code Modal */}
      {showQR && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          onClick={() => setShowQR(false)}
        >
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            className="bg-[#161B22] rounded-2xl p-8 max-w-sm w-full border border-[#30363D]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-white">扫码下载</h3>
              <button 
                onClick={() => setShowQR(false)}
                className="text-gray-400 hover:text-white transition-colors"
              >
                <X size={24} />
              </button>
            </div>
            <div className="aspect-square bg-white rounded-xl flex items-center justify-center mb-4">
              <div className="text-center text-gray-800">
                <QrCode size={120} className="mx-auto mb-2" />
                <p className="text-sm">扫描二维码下载</p>
              </div>
            </div>
            <p className="text-center text-gray-400 text-sm">
              或使用上方下载链接
            </p>
          </motion.div>
        </motion.div>
      )}
    </section>
  )
}
