import { motion } from 'framer-motion'
import { Star, CheckCircle, Clock, ExternalLink } from 'lucide-react'
import { Link } from 'react-router-dom'

const p0Features = [
  { id: 1, name: '悬浮窗滤镜', status: 'completed', priority: 'P0', path: '/floating-window', description: 'ColorOS 深度集成，实时预览滤镜效果' },
  { id: 2, name: '滤镜库', status: 'completed', priority: 'P0', path: '/filter-library', description: '六大分类，48+ 专业预设' },
  { id: 3, name: '大师参数库', status: 'completed', priority: 'P0', path: '/master-params', description: '哈苏认证参数一键应用' },
  { id: 4, name: '主题切换', status: 'completed', priority: 'P0', path: '/settings', description: '深色/浅色/自适应模式' },
  { id: 5, name: '云同步', status: 'in-progress', priority: 'P0', path: '/cloud-sync', description: '跨设备同步滤镜和参数' },
  { id: 6, name: 'AI 微调', status: 'planned', priority: 'P1', path: '/ai-finetune', description: 'AI 智能参数优化' },
]

export default function P0Overview() {
  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">P0 功能总览</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card-oppo p-6"
        >
          <h2 className="text-xl font-bold mb-4">MVP 功能清单</h2>
          <div className="grid grid-cols-3 gap-4">
            <div className="text-center">
              <div className="text-3xl font-bold text-oppo-green mb-1">4</div>
              <p className="text-text-secondary text-sm">已完成</p>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-warning-vital mb-1">1</div>
              <p className="text-text-secondary text-sm">进行中</p>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-text-tertiary mb-1">1</div>
              <p className="text-text-secondary text-sm">计划中</p>
            </div>
          </div>
        </motion.div>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">P0 核心功能</h2>
          <div className="space-y-4">
            {p0Features.map((feature, i) => (
              <motion.div
                key={feature.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.1 + i * 0.05 }}
                whileHover={{ scale: 1.01 }}
                whileTap={{ scale: 0.99 }}
              >
                <Link to={feature.path} className="block">
                  <div className="card-oppo p-4 cursor-pointer touch-feedback">
                    <div className="flex items-start gap-4">
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                        feature.status === 'completed' 
                          ? 'bg-oppo-green/20' 
                          : feature.status === 'in-progress'
                          ? 'bg-warning-vital/20'
                          : 'bg-text-tertiary/20'
                      }`}>
                        {feature.status === 'completed' ? (
                          <CheckCircle className="w-5 h-5 text-oppo-green" />
                        ) : feature.status === 'in-progress' ? (
                          <Clock className="w-5 h-5 text-warning-vital" />
                        ) : (
                          <Star className="w-5 h-5 text-text-tertiary" />
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <h3 className="font-semibold">{feature.name}</h3>
                          <span className={`px-2 py-0.5 text-xs rounded-full ${
                            feature.priority === 'P0' 
                              ? 'bg-oppo-sunrise-gold/20 text-oppo-sunrise-gold'
                              : 'bg-white/10 text-text-secondary'
                          }`}>
                            {feature.priority}
                          </span>
                        </div>
                        <p className="text-text-secondary text-sm mb-2">{feature.description}</p>
                        <div className="flex items-center gap-2 text-xs">
                          <span className={`${
                            feature.status === 'completed' 
                              ? 'text-oppo-green' 
                              : feature.status === 'in-progress'
                              ? 'text-warning-vital'
                              : 'text-text-tertiary'
                          }`}>
                            {feature.status === 'completed' ? '✓ 已完成' : feature.status === 'in-progress' ? '⟳ 进行中' : '○ 计划中'}
                          </span>
                        </div>
                      </div>
                      <ExternalLink className="w-5 h-5 text-text-tertiary flex-shrink-0" />
                    </div>
                  </div>
                </Link>
              </motion.div>
            ))}
          </div>
        </motion.section>
      </main>
    </div>
  )
}
