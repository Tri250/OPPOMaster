import { motion } from 'framer-motion'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { 
  Layers, 
  Filter, 
  ScrollText, 
  Palette, 
  Eye, 
  Heart, 
  Clock, 
  Zap,
  ChevronRight,
  CheckCircle2
} from 'lucide-react'
import { 
  ColorOSCard, 
  ColorOSButton, 
  ColorOSChip,
  ColorOSAnimations 
} from '../components/common/ColorOSComponents'

const p0Features = [
  {
    id: 'floating-window',
    icon: Layers,
    title: '悬浮窗滤镜渲染引擎',
    description: '实时预览滤镜效果，支持透明度调节、尺寸缩放、显示/隐藏',
    status: 'completed',
    path: '/floating-window',
    points: [
      '悬浮窗显示与拖拽',
      '透明度调节',
      '尺寸缩放',
      '一键隐藏/恢复',
      '毛玻璃效果'
    ]
  },
  {
    id: 'filter-library',
    icon: Filter,
    title: '滤镜库',
    description: '六大分类，专业预设，支持收藏、最近使用记录',
    status: 'completed',
    path: '/filter-library',
    points: [
      '滤镜分类浏览（6大类）',
      '滤镜收藏管理',
      '最近使用记录',
      '搜索与筛选',
      '网格/列表视图'
    ]
  },
  {
    id: 'master-params',
    icon: ScrollText,
    title: '大师参数库',
    description: '哈苏认证参数，一键应用，HNCS品质保障',
    status: 'completed',
    path: '/master-params',
    points: [
      '大师参数卡片浏览',
      '一键应用参数',
      'HNCS认证标识',
      '热门排序',
      '收藏置顶'
    ]
  },
  {
    id: 'theme',
    icon: Palette,
    title: '主题切换',
    description: '深色/浅色/自适应，完美匹配ColorOS设计',
    status: 'completed',
    path: '/settings',
    points: [
      '深色主题',
      '浅色主题',
      '系统自适应',
      '主题动画过渡'
    ]
  }
]

const timeline = [
  { week: '第1周', features: ['悬浮窗渲染引擎', '滤镜参数解析器', '悬浮窗权限引导', '悬浮窗显示与拖拽', '实时取景预览', '透明度调节', '尺寸缩放'] },
  { week: '第2周', features: ['滤镜分类浏览', '最近使用记录', '滤镜收藏', '大师参数卡片浏览', '一键应用大师参数'] },
  { week: '第3周', features: ['悬浮窗一键隐藏/恢复', '主题色彩切换', 'UI打磨', '性能优化', '用户体验提升'] }
]

export default function P0Overview() {
  const [activeTab, setActiveTab] = useState('all')

  return (
    <div className="min-h-screen bg-deep-space pb-20">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-72 h-72 top-1/4 -left-36 animate-float" />
        <div className="orb-oppo orb-2 w-56 h-56 bottom-1/4 -right-28 animate-float" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative max-w-6xl mx-auto px-4 pt-24 pb-12">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-oppo-green/20 border border-oppo-green/30 mb-6">
            <CheckCircle2 className="w-5 h-5 text-oppo-green" />
            <span className="text-oppo-green font-medium">P0 阶段已完成</span>
          </div>
          
          <h1 className="text-4xl md:text-5xl font-bold text-white mb-4">
            MVP 核心体验
          </h1>
          <p className="text-xl text-text-secondary max-w-2xl mx-auto">
            14项核心功能，3周完成，打造OPPO摄影大师体验
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-12"
        >
          {[
            { label: '功能总数', value: '14', color: 'text-oppo-sunrise-gold' },
            { label: '完成周数', value: '3', color: 'text-oppo-green' },
            { label: '核心模块', value: '4', color: 'text-ocean-blue' },
            { label: '用户价值', value: '★★★★★', color: 'text-sakura-pink' }
          ].map((stat, i) => (
            <ColorOSCard key={i} className="p-6 text-center">
              <p className={`text-3xl font-bold ${stat.color} mb-2`}>{stat.value}</p>
              <p className="text-text-tertiary text-sm">{stat.label}</p>
            </ColorOSCard>
          ))}
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="mb-12"
        >
          <h2 className="text-2xl font-semibold text-white mb-6">功能模块</h2>
          <div className="grid md:grid-cols-2 gap-6">
            {p0Features.map((feature, i) => (
              <motion.div
                key={feature.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 + i * 0.1 }}
              >
                <Link to={feature.path} className="block">
                  <ColorOSCard className="p-6 hover:bg-white/5 transition-colors group">
                    <div className="flex items-start gap-4">
                      <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold/20 to-oppo-sunrise-gold/5 flex items-center justify-center flex-shrink-0">
                        <feature.icon className="w-7 h-7 text-oppo-sunrise-gold" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-2">
                          <h3 className="text-xl font-semibold text-white group-hover:text-oppo-sunrise-gold transition-colors">
                            {feature.title}
                          </h3>
                          <ColorOSChip label="已完成" selected />
                        </div>
                        <p className="text-text-secondary mb-4">{feature.description}</p>
                        <div className="flex flex-wrap gap-2">
                          {feature.points.slice(0, 3).map((point, j) => (
                            <span key={j} className="text-xs text-text-tertiary bg-white/5 px-2 py-1 rounded-full">
                              {point}
                            </span>
                          ))}
                          {feature.points.length > 3 && (
                            <span className="text-xs text-text-tertiary">
                              +{feature.points.length - 3} 更多
                            </span>
                          )}
                        </div>
                        <div className="mt-4 flex items-center text-oppo-sunrise-gold text-sm font-medium">
                          <span>查看详情</span>
                          <ChevronRight className="w-4 h-4 ml-1" />
                        </div>
                      </div>
                    </div>
                  </ColorOSCard>
                </Link>
              </motion.div>
            ))}
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="mb-12"
        >
          <h2 className="text-2xl font-semibold text-white mb-6">开发时间线</h2>
          <div className="space-y-4">
            {timeline.map((item, i) => (
              <ColorOSCard key={i} className="p-6">
                <div className="flex items-start gap-4">
                  <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex items-center justify-center flex-shrink-0">
                    <span className="text-xl font-bold text-white">{item.week}</span>
                  </div>
                  <div className="flex-1">
                    <div className="flex flex-wrap gap-2">
                      {item.features.map((feat, j) => (
                        <span key={j} className="px-3 py-1.5 bg-white/5 rounded-full text-text-secondary text-sm">
                          {feat}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </ColorOSCard>
            ))}
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
          className="text-center"
        >
          <ColorOSCard className="p-8 bg-gradient-to-br from-oppo-sunrise-gold/10 to-hasselblad-pro/10 border border-oppo-sunrise-gold/20">
            <Zap className="w-12 h-12 text-oppo-sunrise-gold mx-auto mb-4" />
            <h3 className="text-2xl font-semibold text-white mb-4">准备好体验了吗？</h3>
            <p className="text-text-secondary mb-6">
              点击下方按钮，开始体验OPPO Master的所有功能
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link to="/filter-library">
                <ColorOSButton variant="primary" size="lg">
                  体验滤镜库
                </ColorOSButton>
              </Link>
              <Link to="/floating-window">
                <ColorOSButton variant="secondary" size="lg">
                  查看悬浮窗
                </ColorOSButton>
              </Link>
            </div>
          </ColorOSCard>
        </motion.div>
      </div>
    </div>
  )
}
