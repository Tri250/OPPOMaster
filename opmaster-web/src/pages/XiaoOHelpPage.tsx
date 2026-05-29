import { motion } from 'framer-motion'
import { 
  Camera, Layers, Zap, 
  Star, ChevronRight, Download, ScrollText, Filter, 
  Cloud, Image, Sliders, FileText
} from 'lucide-react'
import { Link } from 'react-router-dom'

export default function XiaoOHelpPage() {

  const categories = [
    { 
      id: 1,
      icon: Filter,
      title: '滤镜分类搜索',
      description: '按场景、风格快速找到滤镜',
      color: 'text-aurora-purple',
      bgColor: 'bg-aurora-purple/20',
      path: '/filter-library'
    },
    { 
      id: 2,
      icon: Layers,
      title: '悬浮窗',
      description: '实时预览滤镜效果',
      color: 'text-oppo-sunrise-gold',
      bgColor: 'bg-oppo-sunrise-gold/20',
      path: '/floating-window'
    },
    { 
      id: 3,
      icon: ScrollText,
      title: '大师参数',
      description: '哈苏认证参数一键应用',
      color: 'text-ocean-blue',
      bgColor: 'bg-ocean-blue/20',
      path: '/master-params'
    },
    { 
      id: 4,
      icon: Image,
      title: '多格式导入导出',
      description: '支持 JSON、LUT 等多种格式',
      color: 'text-oppo-green',
      bgColor: 'bg-oppo-green/20',
      path: '/lut-manager'
    },
    { 
      id: 5,
      icon: Sliders,
      title: '预设编辑器',
      description: '自定义调整预设参数',
      color: 'text-sakura-pink',
      bgColor: 'bg-sakura-pink/20',
      path: '/preset-ecosystem'
    },
    { 
      id: 6,
      icon: FileText,
      title: '水印生成器',
      description: '自定义照片水印',
      color: 'text-warning-vital',
      bgColor: 'bg-warning-vital/20',
      path: '/watermark'
    }
  ]

  const features = [
    { icon: Filter, label: '48+ 专业滤镜' },
    { icon: Cloud, label: '云端同步' },
    { icon: Zap, label: '毫秒级响应' },
    { icon: Star, label: 'HNCS 认证' }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">小O帮帮</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-8">
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card-oppo p-6 relative overflow-hidden"
        >
          <div className="absolute inset-0 bg-gradient-to-r from-oppo-sunrise-gold/10 to-ocean-blue/10" />
          <div className="relative z-10">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-14 h-14 rounded-oppo bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                <Camera className="w-7 h-7 text-deep-space" />
              </div>
              <div>
                <h2 className="text-xl font-bold gradient-text-oppo">小O帮帮</h2>
                <p className="text-text-secondary text-sm">哈苏影像系统级标定基座</p>
              </div>
            </div>
            <p className="text-text-secondary mb-6">
              专为 OPPO/一加用户设计的专业调色工具，让你的哈苏影像系统发挥全部潜能。
            </p>
            <div className="flex flex-wrap gap-3">
              {features.map((f, i) => (
                <div key={i} className="flex items-center gap-2 bg-white/5 px-3 py-1.5 rounded-full">
                  <f.icon className="w-4 h-4 text-oppo-sunrise-gold" />
                  <span className="text-sm">{f.label}</span>
                </div>
              ))}
            </div>
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">核心功能</h2>
          <div className="grid grid-cols-2 gap-4">
            {categories.map((cat, i) => (
              <motion.div
                key={cat.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.1 + i * 0.05 }}
                whileHover={{ y: -4, scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <Link to={cat.path} className="block">
                  <div className="card-oppo p-4 h-full cursor-pointer touch-feedback">
                    <div className={`w-12 h-12 rounded-oppo ${cat.bgColor} flex items-center justify-center mb-4`}>
                      <cat.icon className={`w-6 h-6 ${cat.color}`} />
                    </div>
                    <h3 className="text-base font-semibold mb-1">{cat.title}</h3>
                    <p className="text-sm text-text-secondary">{cat.description}</p>
                  </div>
                </Link>
              </motion.div>
            ))}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">使用指南</h2>
          <div className="space-y-3">
            {[
              { step: '01', title: '选择滤镜', desc: '从滤镜库中选择喜欢的风格' },
              { step: '02', title: '预览效果', desc: '通过悬浮窗实时预览' },
              { step: '03', title: '应用参数', desc: '一键应用到相机' },
              { step: '04', title: '保存分享', desc: '导出或分享你的作品' }
            ].map((guide, i) => (
              <motion.div
                key={guide.step}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.2 + i * 0.1 }}
                className="card-oppo p-4 flex items-center gap-4"
              >
                <div className="w-12 h-12 rounded-oppo bg-oppo-sunrise-gold/10 flex items-center justify-center flex-shrink-0">
                  <span className="text-oppo-sunrise-gold font-bold">{guide.step}</span>
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold mb-0.5">{guide.title}</h3>
                  <p className="text-text-tertiary text-sm">{guide.desc}</p>
                </div>
                <ChevronRight className="w-5 h-5 text-text-tertiary flex-shrink-0" />
              </motion.div>
            ))}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="card-oppo p-6 text-center"
        >
          <h2 className="text-xl font-bold mb-2">准备好开始了吗？</h2>
          <p className="text-text-secondary mb-6">立即下载 小O帮帮，让你的照片更专业</p>
          <button className="btn-primary flex items-center justify-center gap-2 mx-auto touch-feedback" aria-label="免费下载应用">
            <Download className="w-5 h-5" />
            <span>免费下载</span>
          </button>
        </motion.section>
      </main>
    </div>
  )
}
