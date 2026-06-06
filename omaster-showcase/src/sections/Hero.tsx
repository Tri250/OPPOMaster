import { motion } from 'framer-motion'
import { Download, Github, ExternalLink, ChevronDown } from 'lucide-react'

export default function Hero() {
  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden bg-[#0D1117]">
      {/* Background gradient */}
      <div className="absolute inset-0 bg-gradient-to-br from-[#0D1117] via-[#161B22] to-[#0D1117]" />
      
      {/* Animated background orbs */}
      <motion.div
        className="absolute top-20 left-10 w-96 h-96 bg-[#FF6B35]/20 rounded-full blur-[120px]"
        animate={{
          x: [0, 50, 0],
          y: [0, 30, 0],
        }}
        transition={{
          duration: 8,
          repeat: Infinity,
          ease: "easeInOut"
        }}
      />
      <motion.div
        className="absolute bottom-20 right-10 w-80 h-80 bg-[#58A6FF]/10 rounded-full blur-[100px]"
        animate={{
          x: [0, -30, 0],
          y: [0, -50, 0],
        }}
        transition={{
          duration: 10,
          repeat: Infinity,
          ease: "easeInOut"
        }}
      />

      {/* Content */}
      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left: Text content */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.2, duration: 0.5 }}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-[#FF6B35]/10 border border-[#FF6B35]/20 mb-6"
            >
              <span className="w-2 h-2 rounded-full bg-[#FF6B35] animate-pulse" />
              <span className="text-[#FF6B35] text-sm font-medium">v1.5.0 现已发布</span>
            </motion.div>

            <h1 className="text-5xl sm:text-6xl lg:text-7xl font-bold text-white leading-tight mb-6">
              <span className="block">大师模式</span>
              <span className="block text-[#FF6B35]">调色参数库</span>
            </h1>

            <p className="text-xl text-gray-400 mb-8 max-w-lg">
              为各品牌手机打造的摄影调色参数管理工具。告别参数焦虑，一键获取专业摄影预设。
            </p>

            <div className="flex flex-wrap gap-4 mb-8">
              <motion.a
                href="https://github.com/iCurrer/OMaster/releases"
                target="_blank"
                rel="noopener noreferrer"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="inline-flex items-center gap-2 px-8 py-4 bg-[#FF6B35] hover:bg-[#FF8C42] text-white rounded-xl font-semibold transition-colors shadow-lg shadow-[#FF6B35]/25"
              >
                <Download size={20} />
                立即下载
              </motion.a>
              <motion.a
                href="https://github.com/iCurrer/OMaster"
                target="_blank"
                rel="noopener noreferrer"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="inline-flex items-center gap-2 px-8 py-4 bg-[#30363D] hover:bg-[#484F58] text-white rounded-xl font-semibold transition-colors"
              >
                <Github size={20} />
                GitHub
              </motion.a>
            </div>

            <div className="flex items-center gap-6 text-sm text-gray-500">
              <span className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-green-500" />
                免费开源
              </span>
              <span className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-blue-500" />
                23+ 专业预设
              </span>
              <span className="flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-purple-500" />
                全平台支持
              </span>
            </div>
          </motion.div>

          {/* Right: Phone mockup */}
          <motion.div
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3, duration: 0.8 }}
            className="relative"
          >
            <motion.div
              animate={{ y: [0, -20, 0] }}
              transition={{
                duration: 4,
                repeat: Infinity,
                ease: "easeInOut"
              }}
              className="relative z-10"
            >
              {/* Phone frame */}
              <div className="relative mx-auto w-72 sm:w-80">
                <div className="relative bg-[#1C1C1E] rounded-[3rem] p-3 shadow-2xl shadow-black/50 border border-gray-800">
                  {/* Screen */}
                  <div className="relative bg-[#0D1117] rounded-[2.5rem] overflow-hidden aspect-[9/19]">
                    {/* App UI mockup */}
                    <div className="absolute inset-0 bg-gradient-to-b from-[#161B22] to-[#0D1117]">
                      {/* Status bar */}
                      <div className="h-12 flex items-center justify-between px-6 pt-2">
                        <span className="text-white text-sm font-medium">9:41</span>
                        <div className="flex gap-1">
                          <div className="w-4 h-4 rounded-full bg-white/20" />
                          <div className="w-4 h-4 rounded-full bg-white/20" />
                        </div>
                      </div>
                      
                      {/* App header */}
                      <div className="px-4 py-3">
                        <div className="flex items-center gap-2 mb-4">
                          <div className="w-8 h-8 rounded-lg bg-[#FF6B35] flex items-center justify-center">
                            <span className="text-white font-bold text-sm">O</span>
                          </div>
                          <span className="text-white font-semibold">小O帮帮</span>
                        </div>
                        
                        {/* Preset cards */}
                        <div className="space-y-3">
                          <div className="bg-[#1C1C1E] rounded-xl p-3">
                            <div className="flex gap-3">
                              <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-orange-400 to-red-500" />
                              <div className="flex-1">
                                <div className="text-white text-sm font-medium mb-1">哈苏自然</div>
                                <div className="text-gray-500 text-xs">还原哈苏相机自然色彩</div>
                              </div>
                            </div>
                          </div>
                          <div className="bg-[#1C1C1E] rounded-xl p-3">
                            <div className="flex gap-3">
                              <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-blue-400 to-purple-500" />
                              <div className="flex-1">
                                <div className="text-white text-sm font-medium mb-1">胶片复古</div>
                                <div className="text-gray-500 text-xs">模拟经典胶片色彩</div>
                              </div>
                            </div>
                          </div>
                          <div className="bg-[#1C1C1E] rounded-xl p-3">
                            <div className="flex gap-3">
                              <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-green-400 to-teal-500" />
                              <div className="flex-1">
                                <div className="text-white text-sm font-medium mb-1">夜景霓虹</div>
                                <div className="text-gray-500 text-xs">城市夜景专用预设</div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                
                {/* Glow effect */}
                <div className="absolute -inset-4 bg-[#FF6B35]/20 rounded-[4rem] blur-2xl -z-10" />
              </div>
            </motion.div>
          </motion.div>
        </div>
      </div>

      {/* Scroll indicator */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1 }}
        className="absolute bottom-8 left-1/2 -translate-x-1/2"
      >
        <motion.div
          animate={{ y: [0, 10, 0] }}
          transition={{ duration: 1.5, repeat: Infinity }}
          className="flex flex-col items-center gap-2 text-gray-500"
        >
          <span className="text-sm">向下滚动</span>
          <ChevronDown size={20} />
        </motion.div>
      </motion.div>
    </section>
  )
}
