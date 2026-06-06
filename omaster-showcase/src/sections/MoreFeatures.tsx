import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  SlidersHorizontal, 
  Import, 
  History, 
  Share2, 
  Layers,
  Search,
  Filter,
  X,
  ChevronRight,
  Download,
  Upload,
  Clock,
  Send,
  Sparkles,
  Image as ImageIcon,
  Plus,
  Camera
} from 'lucide-react'

interface MoreFeature {
  id: string
  icon: any
  title: string
  subtitle: string
  description: string
  details: string[]
  color: string
  preview: React.ReactNode
}

const moreFeatures: MoreFeature[] = [
  {
    id: 'params',
    icon: SlidersHorizontal,
    title: '参数精细调节',
    subtitle: '专业级参数控制',
    description: '支持ISO、快门速度、白平衡、曝光补偿等专业参数的精细调节，满足摄影师的个性化需求。',
    details: [
      'ISO 感光度调节 (50-12800)',
      '快门速度控制 (1/8000-30s)',
      '白平衡色温调节 (2000K-10000K)',
      '曝光补偿 (-5EV 到 +5EV)',
      '对焦模式切换'
    ],
    color: '#FF6B35',
    preview: (
      <div className="space-y-3">
        <div className="flex items-center justify-between p-2 bg-[#1C1C1E] rounded-lg">
          <span className="text-gray-400 text-xs">ISO</span>
          <div className="flex items-center gap-2">
            <div className="w-20 h-1.5 bg-[#30363D] rounded-full overflow-hidden">
              <div className="w-3/5 h-full bg-[#FF6B35] rounded-full" />
            </div>
            <span className="text-white text-xs w-12 text-right">400</span>
          </div>
        </div>
        <div className="flex items-center justify-between p-2 bg-[#1C1C1E] rounded-lg">
          <span className="text-gray-400 text-xs">快门</span>
          <div className="flex items-center gap-2">
            <div className="w-20 h-1.5 bg-[#30363D] rounded-full overflow-hidden">
              <div className="w-1/2 h-full bg-[#58A6FF] rounded-full" />
            </div>
            <span className="text-white text-xs w-12 text-right">1/125</span>
          </div>
        </div>
        <div className="flex items-center justify-between p-2 bg-[#1C1C1E] rounded-lg">
          <span className="text-gray-400 text-xs">WB</span>
          <div className="flex items-center gap-2">
            <div className="w-20 h-1.5 bg-[#30363D] rounded-full overflow-hidden">
              <div className="w-2/3 h-full bg-[#A371F7] rounded-full" />
            </div>
            <span className="text-white text-xs w-12 text-right">5500K</span>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'import-export',
    icon: Import,
    title: '预设导入导出',
    subtitle: '支持多种格式',
    description: '支持导入导出预设配置，兼容多种格式，方便备份和分享你的专属预设。',
    details: [
      'JSON 格式预设导入导出',
      '二维码分享预设',
      '批量导入预设包',
      '云端备份与恢复',
      '兼容第三方预设格式'
    ],
    color: '#58A6FF',
    preview: (
      <div className="grid grid-cols-2 gap-3">
        <div className="bg-[#1C1C1E] rounded-lg p-3 text-center">
          <div className="w-10 h-10 mx-auto mb-2 rounded-full bg-blue-500/20 flex items-center justify-center">
            <Upload size={18} className="text-blue-400" />
          </div>
          <div className="text-white text-xs">导入预设</div>
          <div className="text-gray-500 text-[10px] mt-1">JSON / QR</div>
        </div>
        <div className="bg-[#1C1C1E] rounded-lg p-3 text-center">
          <div className="w-10 h-10 mx-auto mb-2 rounded-full bg-green-500/20 flex items-center justify-center">
            <Download size={18} className="text-green-400" />
          </div>
          <div className="text-white text-xs">导出预设</div>
          <div className="text-gray-500 text-[10px] mt-1">分享 / 备份</div>
        </div>
        <div className="col-span-2 bg-[#1C1C1E] rounded-lg p-2 flex items-center gap-2">
          <div className="w-8 h-8 rounded bg-gradient-to-br from-orange-400 to-red-500" />
          <div className="flex-1">
            <div className="text-white text-xs">哈苏自然.omaster</div>
            <div className="text-gray-500 text-[10px]">2.3 KB</div>
          </div>
          <div className="w-4 h-4 rounded-full bg-green-500 flex items-center justify-center">
            <div className="w-2 h-2 bg-white rounded-sm rotate-45" />
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'history',
    icon: History,
    title: '使用历史记录',
    subtitle: '追踪预设使用',
    description: '自动记录预设使用历史，快速找回之前使用的参数配置，支持按时间筛选。',
    details: [
      '自动记录使用历史',
      '按日期筛选查看',
      '快速重新应用预设',
      '历史数据统计分析',
      '云端同步历史记录'
    ],
    color: '#A371F7',
    preview: (
      <div className="space-y-2">
        <div className="flex items-center gap-3 p-2 bg-[#1C1C1E] rounded-lg">
          <Clock size={14} className="text-purple-400" />
          <div className="flex-1">
            <div className="text-white text-xs">哈苏自然</div>
            <div className="text-gray-500 text-[10px]">今天 14:32</div>
          </div>
        </div>
        <div className="flex items-center gap-3 p-2 bg-[#1C1C1E] rounded-lg">
          <Clock size={14} className="text-purple-400" />
          <div className="flex-1">
            <div className="text-white text-xs">胶片复古</div>
            <div className="text-gray-500 text-[10px]">今天 10:15</div>
          </div>
        </div>
        <div className="flex items-center gap-3 p-2 bg-[#1C1C1E] rounded-lg">
          <Clock size={14} className="text-purple-400" />
          <div className="flex-1">
            <div className="text-white text-xs">夜景霓虹</div>
            <div className="text-gray-500 text-[10px]">昨天 20:48</div>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'share',
    icon: Share2,
    title: '一键分享',
    subtitle: '多平台分享',
    description: '支持将预设通过二维码、链接、文件等多种形式分享给好友，支持分享到微信、QQ、微博等社交平台。',
    details: [
      '生成预设分享二维码',
      '复制分享链接',
      '导出预设文件',
      '分享到社交平台',
      '批量分享预设包'
    ],
    color: '#F778BA',
    preview: (
      <div className="space-y-3">
        <div className="flex justify-center">
          <div className="w-20 h-20 bg-white rounded-lg p-2">
            <div className="w-full h-full grid grid-cols-5 grid-rows-5 gap-0.5">
              {Array.from({ length: 25 }).map((_, i) => (
                <div 
                  key={i} 
                  className={`rounded-sm ${Math.random() > 0.5 ? 'bg-black' : 'bg-white'}`}
                />
              ))}
            </div>
          </div>
        </div>
        <div className="flex justify-center gap-2">
          <div className="w-8 h-8 rounded-full bg-green-500/20 flex items-center justify-center">
            <span className="text-green-400 text-[10px]">微</span>
          </div>
          <div className="w-8 h-8 rounded-full bg-blue-500/20 flex items-center justify-center">
            <span className="text-blue-400 text-[10px]">Q</span>
          </div>
          <div className="w-8 h-8 rounded-full bg-red-500/20 flex items-center justify-center">
            <span className="text-red-400 text-[10px]">博</span>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'batch',
    icon: Layers,
    title: '批量操作',
    subtitle: '高效管理预设',
    description: '支持批量导入、导出、删除预设，一键应用预设到多个场景，大幅提升工作效率。',
    details: [
      '批量导入预设包',
      '批量导出分享',
      '批量删除整理',
      '一键应用多预设',
      '批量收藏管理'
    ],
    color: '#FF6B6B',
    preview: (
      <div className="space-y-2">
        <div className="flex items-center gap-2 p-2 bg-[#1C1C1E] rounded-lg">
          <div className="w-4 h-4 rounded bg-[#FF6B35] flex items-center justify-center">
            <div className="w-2 h-2 bg-white rounded-sm rotate-45" />
          </div>
          <div className="w-6 h-6 rounded bg-gradient-to-br from-orange-400 to-red-500" />
          <span className="text-white text-xs flex-1">哈苏自然</span>
        </div>
        <div className="flex items-center gap-2 p-2 bg-[#1C1C1E] rounded-lg">
          <div className="w-4 h-4 rounded bg-[#FF6B35] flex items-center justify-center">
            <div className="w-2 h-2 bg-white rounded-sm rotate-45" />
          </div>
          <div className="w-6 h-6 rounded bg-gradient-to-br from-blue-400 to-purple-500" />
          <span className="text-white text-xs flex-1">胶片复古</span>
        </div>
        <div className="flex items-center gap-2 p-2 bg-[#1C1C1E] rounded-lg">
          <div className="w-4 h-4 rounded border border-gray-600" />
          <div className="w-6 h-6 rounded bg-gradient-to-br from-green-400 to-teal-500" />
          <span className="text-white text-xs flex-1">夜景霓虹</span>
        </div>
        <div className="flex gap-2 mt-3">
          <button className="flex-1 py-1.5 bg-[#FF6B35] text-white text-xs rounded">导出选中</button>
          <button className="flex-1 py-1.5 bg-[#30363D] text-white text-xs rounded">删除选中</button>
        </div>
      </div>
    )
  },
  {
    id: 'search',
    icon: Search,
    title: '智能搜索',
    subtitle: '快速找到预设',
    description: '强大的搜索功能，支持按名称、风格、摄影师等多维度搜索，快速找到你需要的预设。',
    details: [
      '关键词模糊搜索',
      '按风格筛选',
      '按摄影师搜索',
      '搜索历史记录',
      '热门搜索推荐'
    ],
    color: '#00D9FF',
    preview: (
      <div className="space-y-3">
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
          <input 
            type="text" 
            placeholder="搜索预设..." 
            className="w-full bg-[#1C1C1E] rounded-lg py-2 pl-9 pr-3 text-white text-xs placeholder-gray-500"
            readOnly
          />
        </div>
        <div className="flex flex-wrap gap-1.5">
          <span className="px-2 py-0.5 bg-[#FF6B35]/20 text-[#FF6B35] text-[10px] rounded-full">哈苏</span>
          <span className="px-2 py-0.5 bg-[#30363D] text-gray-400 text-[10px] rounded-full">胶片</span>
          <span className="px-2 py-0.5 bg-[#30363D] text-gray-400 text-[10px] rounded-full">夜景</span>
          <span className="px-2 py-0.5 bg-[#30363D] text-gray-400 text-[10px] rounded-full">人像</span>
        </div>
      </div>
    )
  },
  {
    id: 'filter',
    icon: Filter,
    title: '分类筛选',
    subtitle: '多维度筛选',
    description: '支持按风格、品牌、场景等多维度筛选预设，快速定位到想要的预设类型。',
    details: [
      '按风格分类筛选',
      '按相机品牌筛选',
      '按拍摄场景筛选',
      '按发布时间排序',
      '自定义筛选条件'
    ],
    color: '#9B59B6',
    preview: (
      <div className="space-y-2">
        <div className="flex gap-2 overflow-x-auto pb-1">
          <span className="px-3 py-1 bg-[#FF6B35] text-white text-xs rounded-full whitespace-nowrap">全部</span>
          <span className="px-3 py-1 bg-[#30363D] text-gray-400 text-xs rounded-full whitespace-nowrap">胶片</span>
          <span className="px-3 py-1 bg-[#30363D] text-gray-400 text-xs rounded-full whitespace-nowrap">黑白</span>
          <span className="px-3 py-1 bg-[#30363D] text-gray-400 text-xs rounded-full whitespace-nowrap">风景</span>
        </div>
        <div className="grid grid-cols-2 gap-2">
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-[#FF6B35] text-lg font-bold">23</div>
            <div className="text-gray-500 text-[10px]">胶片风格</div>
          </div>
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-[#58A6FF] text-lg font-bold">15</div>
            <div className="text-gray-500 text-[10px]">人像风格</div>
          </div>
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-[#3FB950] text-lg font-bold">12</div>
            <div className="text-gray-500 text-[10px]">风景风格</div>
          </div>
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-[#A371F7] text-lg font-bold">8</div>
            <div className="text-gray-500 text-[10px]">夜景风格</div>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'ai-fine-tune',
    icon: Sparkles,
    title: 'AI精细调整',
    subtitle: '智能参数优化',
    description: '基于AI算法分析当前环境，自动微调预设参数，获得最佳拍摄效果。',
    details: [
      'AI自动分析环境光线',
      '智能优化预设参数',
      '实时预览调整效果',
      '一键应用优化方案',
      '保存自定义AI参数'
    ],
    color: '#FF6B35',
    preview: (
      <div className="space-y-3">
        <div className="bg-[#1C1C1E] rounded-lg p-3">
          <div className="flex items-center gap-2 mb-2">
            <Sparkles size={14} className="text-[#FF6B35]" />
            <span className="text-white text-xs font-medium">AI 优化中...</span>
          </div>
          <div className="w-full h-1.5 bg-[#30363D] rounded-full overflow-hidden">
            <motion.div 
              className="h-full bg-[#FF6B35] rounded-full"
              animate={{ width: ['0%', '75%'] }}
              transition={{ duration: 2, repeat: Infinity }}
            />
          </div>
        </div>
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-[#58A6FF] text-xs font-bold">ISO</div>
            <div className="text-white text-xs">↓ 100</div>
          </div>
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-[#3FB950] text-xs font-bold">快门</div>
            <div className="text-white text-xs">↑ 1/60</div>
          </div>
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-[#A371F7] text-xs font-bold">白平衡</div>
            <div className="text-white text-xs">5200K</div>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'watermark',
    icon: ImageIcon,
    title: '水印编辑器',
    subtitle: '专业水印功能',
    description: '强大的水印编辑功能，支持自定义水印、位置、透明度、大小，打造个人品牌。',
    details: [
      '文字/图片水印支持',
      '水印位置自由调整',
      '透明度/大小设置',
      '多水印叠加',
      '水印模板保存'
    ],
    color: '#3FB950',
    preview: (
      <div className="space-y-2">
        <div className="bg-[#1C1C1E] rounded-lg p-3 relative overflow-hidden">
          <div className="w-full h-20 bg-gradient-to-br from-gray-700 to-gray-900 rounded" />
          <div className="absolute bottom-2 right-2 text-white text-[10px] bg-black/50 px-2 py-1 rounded">
            @摄影师
          </div>
        </div>
        <div className="flex gap-2">
          <div className="flex-1 bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-gray-400 text-[10px]">透明度</div>
            <div className="text-white text-xs">75%</div>
          </div>
          <div className="flex-1 bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-gray-400 text-[10px]">大小</div>
            <div className="text-white text-xs">中</div>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'submit',
    icon: Plus,
    title: '预设提交',
    subtitle: '分享你的创作',
    description: '将你创作的预设提交到社区，与其他摄影师分享，获得认可和交流。',
    details: [
      '预设提交审核',
      '预设详情编辑',
      '预览图片上传',
      '使用说明撰写',
      '社区反馈收集'
    ],
    color: '#FF6B35',
    preview: (
      <div className="space-y-2">
        <div className="flex items-center gap-2 p-2 bg-[#1C1C1E] rounded-lg">
          <Plus size={14} className="text-[#FF6B35]" />
          <span className="text-white text-xs">提交预设</span>
        </div>
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-gray-400 text-[10px]">编辑</div>
            <div className="text-white text-xs">详情</div>
          </div>
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-gray-400 text-[10px]">上传</div>
            <div className="text-white text-xs">预览</div>
          </div>
          <div className="bg-[#1C1C1E] rounded-lg p-2 text-center">
            <div className="text-gray-400 text-[10px]">等待</div>
            <div className="text-white text-xs">审核</div>
          </div>
        </div>
      </div>
    )
  },
  {
    id: 'scene-detection',
    icon: Camera,
    title: '场景检测',
    subtitle: '智能识别场景',
    description: '智能识别当前拍摄场景（人像、风景、夜景、美食等），自动推荐最适合的预设方案。',
    details: [
      '自动识别拍摄场景',
      '智能推荐预设方案',
      '实时场景切换提示',
      '场景匹配度显示',
      '自定义场景预设'
    ],
    color: '#58A6FF',
    preview: (
      <div className="space-y-2">
        <div className="flex items-center gap-3 p-3 bg-[#1C1C1E] rounded-lg">
          <Camera size={18} className="text-[#58A6FF]" />
          <div className="flex-1">
            <div className="text-white text-xs font-medium">检测到: 人像</div>
            <div className="text-gray-500 text-[10px]">匹配度: 92%</div>
          </div>
        </div>
        <div className="flex gap-2">
          <span className="px-2 py-1 bg-[#FF6B35]/20 text-[#FF6B35] text-[10px] rounded-full">人像</span>
          <span className="px-2 py-1 bg-[#30363D] text-gray-400 text-[10px] rounded-full">风景</span>
          <span className="px-2 py-1 bg-[#30363D] text-gray-400 text-[10px] rounded-full">夜景</span>
          <span className="px-2 py-1 bg-[#30363D] text-gray-400 text-[10px] rounded-full">美食</span>
        </div>
      </div>
    )
  }
]

export default function MoreFeatures() {
  const [selectedFeature, setSelectedFeature] = useState<MoreFeature | null>(null)

  return (
    <section className="py-24 bg-[#0D1117] relative">
      <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-[#30363D] to-transparent" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center mb-16">
          <span className="inline-block px-4 py-1.5 rounded-full bg-[#58A6FF]/10 text-[#58A6FF] text-sm font-medium mb-4">
            更多功能
          </span>
          <h2 className="text-4xl sm:text-5xl font-bold text-white mb-4">
            探索更多<span className="text-[#58A6FF]">强大功能</span>
          </h2>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            小O帮帮 还有更多实用功能等待你发现
          </p>
        </div>

        {/* Features grid */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {moreFeatures.map((feature, index) => {
            const Icon = feature.icon
            return (
              <motion.div
                key={feature.id}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: (index % 9) * 0.08 }}
                viewport={{ once: true }}
                whileHover={{ y: -8 }}
                onClick={() => setSelectedFeature(feature)}
                className="group cursor-pointer bg-[#161B22] rounded-2xl p-6 border border-[#30363D] hover:border-[#58A6FF]/50 transition-all duration-300"
              >
                {/* Icon */}
                <div 
                  className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 transition-transform duration-300 group-hover:scale-110"
                  style={{ backgroundColor: `${feature.color}15` }}
                >
                  <Icon size={28} style={{ color: feature.color }} />
                </div>

                {/* Content */}
                <h3 className="text-xl font-bold text-white mb-2 group-hover:text-[#58A6FF] transition-colors">
                  {feature.title}
                </h3>
                <p className="text-sm text-gray-500 mb-3">{feature.subtitle}</p>
                <p className="text-gray-400 text-sm leading-relaxed mb-4">
                  {feature.description}
                </p>

                {/* Preview */}
                <div className="opacity-60 group-hover:opacity-100 transition-opacity">
                  {feature.preview}
                </div>

                {/* CTA */}
                <div className="mt-4 flex items-center text-[#58A6FF] text-sm font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  点击体验
                  <ChevronRight size={16} className="ml-1" />
                </div>
              </motion.div>
            )
          })}
        </div>
      </div>

      {/* Feature detail modal */}
      <AnimatePresence>
        {selectedFeature && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setSelectedFeature(null)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-[#161B22] rounded-3xl p-8 max-w-2xl w-full border border-[#30363D] max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="flex items-start justify-between mb-6">
                <div className="flex items-center gap-4">
                  <div 
                    className="w-16 h-16 rounded-2xl flex items-center justify-center"
                    style={{ backgroundColor: `${selectedFeature.color}15` }}
                  >
                    <selectedFeature.icon size={32} style={{ color: selectedFeature.color }} />
                  </div>
                  <div>
                    <h3 className="text-2xl font-bold text-white">{selectedFeature.title}</h3>
                    <p className="text-gray-400">{selectedFeature.subtitle}</p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedFeature(null)}
                  className="p-2 hover:bg-[#30363D] rounded-full transition-colors"
                >
                  <X size={24} className="text-gray-400" />
                </button>
              </div>

              {/* Description */}
              <p className="text-gray-300 mb-6">{selectedFeature.description}</p>

              {/* Preview */}
              <div className="mb-6 p-4 bg-[#0D1117] rounded-xl">
                {selectedFeature.preview}
              </div>

              {/* Feature list */}
              <div className="space-y-3">
                <h4 className="text-white font-semibold mb-3">功能亮点</h4>
                {selectedFeature.details.map((detail, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <div 
                      className="w-2 h-2 rounded-full"
                      style={{ backgroundColor: selectedFeature.color }}
                    />
                    <span className="text-gray-300">{detail}</span>
                  </div>
                ))}
              </div>

              {/* CTA */}
              <div className="mt-8 pt-6 border-t border-[#30363D]">
                <a
                  href="https://github.com/iCurrer/OMaster/releases"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full py-4 bg-[#58A6FF] hover:bg-[#79B8FF] text-white rounded-xl font-semibold flex items-center justify-center gap-2 transition-colors"
                >
                  <Send size={20} />
                  下载 App 体验完整功能
                </a>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </section>
  )
}
