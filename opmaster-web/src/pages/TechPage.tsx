import { motion, AnimatePresence } from 'framer-motion';
import { 
  Cpu, 
  Smartphone,
  Layers,
  Database,
  Code,
  Palette,
  X,
  CheckCircle,
  Sparkles,
  Zap,
  Search,
  Upload,
  Download
} from 'lucide-react';
import { useState } from 'react';

const features = [
  {
    id: 'ai',
    icon: Cpu,
    title: 'AI场景识别',
    description: '智能识别拍摄场景，自动推荐最佳影像参数',
    color: 'from-purple-500 to-pink-500',
    features: [
      '50+场景类型识别',
      '毫秒级响应速度',
      '自适应参数推荐'
    ],
    detail: {
      title: 'AI场景识别',
      icon: Sparkles,
      color: 'from-purple-500 to-pink-500',
      description: '基于深度学习的图像识别技术，自动分析场景类型、主体，光照条件，为您推荐最佳影像参数。',
      benefits: [
        '智能识别50+种场景类型',
        '毫秒级快速响应',
        '自动适配最佳参数',
        '支持人像、风景、夜景、美食等多种场景'
      ],
      techDetails: [
        'TensorFlow Lite 端侧推理',
        '本地处理保护隐私',
        '实时场景分析',
        '参数智能推荐'
      ],
      action: '体验AI识别',
      actionLink: '/ai-demo'
    }
  },
  {
    id: 'autofill',
    icon: Smartphone,
    title: '原生相机参数自动填入',
    description: '基于安卓无障碍服务，无Root自动填参数',
    color: 'from-oppo-green to-cyan-500',
    features: [
      '支持六大品牌相机',
      '无Root合法合规',
      '10+步简化为2步'
    ],
    detail: {
      title: '原生相机参数自动填入',
      icon: Zap,
      color: 'from-oppo-green to-cyan-500',
      description: '解决用户"手动输入参数"的最高频痛点，从"参数参考工具"升级为"参数执行工具"。',
      benefits: [
        '基于安卓无障碍服务，无Root、合法合规',
        '支持OPPO/一加/Realme/小米/vivo/华为六大品牌',
        '自动填充所有基础/专业参数',
        '操作步骤从10+步压缩到2步'
      ],
      techDetails: [
        'AccessibilityService 无障碍服务',
        'Layout Inspector 控件映射',
        'TYPE_ACCESSIBILITY_OVERLAY',
        '一键复制粘贴兜底方案'
      ],
      action: '了解更多技术实现',
      actionLink: null
    }
  },
  {
    id: 'floating',
    icon: Layers,
    title: '悬浮窗',
    description: '多悬浮窗类型兼容方案，适配率95%+',
    color: 'from-blue-500 to-indigo-500',
    features: [
      '标准悬浮窗类型',
      '相机上层显示',
      'SurfaceView绘制'
    ],
    detail: {
      title: '悬浮窗系统',
      icon: Layers,
      color: 'from-blue-500 to-indigo-500',
      description: '解决ColorOS/小米等系统悬浮窗无法开启、无法显示在相机上层的核心功能失效问题。',
      benefits: [
        '多悬浮窗类型自动切换',
        'Android 8.0+ 标准兼容',
        '国产系统特殊适配',
        '适配率从30%提升到95%+'
      ],
      techDetails: [
        'TYPE_APPLICATION_OVERLAY',
        'TYPE_ACCESSIBILITY_OVERLAY',
        'SurfaceView 低性能损耗',
        'XXPermissions 权限适配'
      ],
      action: '查看权限适配方案',
      actionLink: null
    }
  },
  {
    id: 'search',
    icon: Database,
    title: '预设分类搜索',
    description: '多维度分类体系，快速找到心仪预设',
    color: 'from-green-500 to-emerald-500',
    features: [
      '按风格分类',
      '按场景分类',
      '全文搜索'
    ],
    detail: {
      title: '预设分类与搜索',
      icon: Search,
      color: 'from-green-500 to-emerald-500',
      description: '多维度分类体系，支持全文搜索，快速找到心仪的影像预设。',
      benefits: [
        '按风格分类：胶片/复古/清新/电影感',
        '按场景分类：人像/美食/风光/建筑',
        '按设备分类：Find X/Reno/一加/小米',
        '支持预设名称、参数、标签全文搜索'
      ],
      techDetails: [
        'Jetpack Room FTS',
        'Compose SearchBar',
        'Chip Group 标签筛选',
        '本地全文检索'
      ],
      action: '体验搜索功能',
      actionLink: '/'
    }
  },
  {
    id: 'ecosystem',
    icon: Code,
    title: '预设生态',
    description: '内置预设编辑器+一键社区贡献系统',
    color: 'from-yellow-500 to-orange-500',
    features: [
      '预设编辑器',
      '一键贡献',
      '预设排行榜'
    ],
    detail: {
      title: '预设生态系统',
      icon: Code,
      color: 'from-yellow-500 to-orange-500',
      description: '内置预设编辑器，支持一键贡献到社区，建立UGC内容生态，预设规模从23+提升到1000+。',
      benefits: [
        '1:1复刻原生相机大师模式参数',
        '支持自定义标签和样片上传',
        '一键提交到GitHub PR',
        '基于收藏量的排行榜激励'
      ],
      techDetails: [
        'GitHub API/PR 自动创建',
        'GitHub Actions 自动审核',
        'JSON Schema 格式校验',
        '排行榜权重算法'
      ],
      action: '参与社区贡献',
      actionLink: null
    }
  },
  {
    id: 'import',
    icon: Palette,
    title: '多格式预设导入导出',
    description: '支持主流修图工具预设格式',
    color: 'from-red-500 to-pink-500',
    features: [
      'LUT文件解析',
      '泼辣修图预设',
      'Lightroom预设'
    ],
    detail: {
      title: '多格式预设导入导出',
      icon: Upload,
      color: 'from-red-500 to-pink-500',
      description: '支持主流修图工具预设格式，打通预设生态，实现跨平台预设共享。',
      benefits: [
        'LUT文件(.cube)解析与转换',
        '泼辣修图预设导入',
        'Lightroom手机版预设兼容',
        '支持JSON、二维码、链接分享'
      ],
      techDetails: [
        'Android-LUT-Parser',
        'LrPresetParser Lightroom解析',
        'ZXing Compose 二维码',
        '批量导入导出支持'
      ],
      action: '体验导入功能',
      actionLink: null
    }
  }
];

export default function TechPage() {
  const [selectedFeature, setSelectedFeature] = useState<typeof features[0] | null>(null);

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-16"
        >
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text">
            影像参数
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            核心功能展示 - 点击卡片查看详情
          </p>
        </motion.div>

        {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-16">
          {features.map((feature, idx) => (
            <motion.div
              key={feature.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
              whileHover={{ y: -8, scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => setSelectedFeature(feature)}
              className="card p-6 group cursor-pointer relative overflow-hidden"
            >
              {/* Hover Effect */}
              <div className={`absolute inset-0 bg-gradient-to-br ${feature.color} opacity-0 group-hover:opacity-10 transition-opacity duration-300`} />
              
              <div className={`relative w-16 h-16 rounded-2xl bg-gradient-to-br ${feature.color} p-4 mb-4 group-hover:scale-110 transition-transform duration-300`}>
                <feature.icon className="w-full h-full text-white" />
              </div>
              
              <h3 className="text-xl font-bold mb-2 group-hover:text-hasselblad transition-colors">
                {feature.title}
              </h3>
              
              <p className="text-white/60 text-sm mb-4">
                {feature.description}
              </p>
              
              <ul className="space-y-2">
                {feature.features.map((item, fIdx) => (
                  <li key={fIdx} className="flex items-center text-sm text-white/50">
                    <div className="w-1.5 h-1.5 bg-hasselblad rounded-full mr-2" />
                    {item}
                  </li>
                ))}
              </ul>

              {/* Click hint */}
              <div className="absolute bottom-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity">
                <span className="text-xs text-hasselblad">点击查看详情 →</span>
              </div>
            </motion.div>
          ))}
        </div>

        {/* Feature Detail Modal */}
        <AnimatePresence>
          {selectedFeature && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
              onClick={() => setSelectedFeature(null)}
            >
              <motion.div
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.9, opacity: 0 }}
                onClick={(e) => e.stopPropagation()}
                className="bg-deep-space-light rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto"
              >
                {/* Modal Header */}
                <div className={`sticky top-0 bg-gradient-to-r ${selectedFeature.detail.color} p-6 rounded-t-2xl`}>
                  <div className="flex items-start justify-between">
                    <div className="flex items-center space-x-4">
                      <div className="w-16 h-16 bg-white/20 rounded-2xl flex items-center justify-center">
                        <selectedFeature.detail.icon className="w-10 h-10 text-white" />
                      </div>
                      <div>
                        <h2 className="text-2xl font-bold text-white">{selectedFeature.detail.title}</h2>
                        <p className="text-white/80 text-sm mt-1">点击卡片查看功能详情</p>
                      </div>
                    </div>
                    <button
                      onClick={() => setSelectedFeature(null)}
                      className="w-10 h-10 bg-white/20 hover:bg-white/30 rounded-full flex items-center justify-center transition-colors"
                    >
                      <X className="w-6 h-6 text-white" />
                    </button>
                  </div>
                </div>

                {/* Modal Content */}
                <div className="p-6 space-y-6">
                  {/* Description */}
                  <div>
                    <h3 className="text-lg font-bold mb-2 gradient-text">功能介绍</h3>
                    <p className="text-white/70 leading-relaxed">
                      {selectedFeature.detail.description}
                    </p>
                  </div>

                  {/* Benefits */}
                  <div>
                    <h3 className="text-lg font-bold mb-3 flex items-center">
                      <CheckCircle className="w-5 h-5 text-oppo-green mr-2" />
                      核心优势
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {selectedFeature.detail.benefits.map((benefit, idx) => (
                        <motion.div
                          key={idx}
                          initial={{ opacity: 0, x: -20 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: idx * 0.1 }}
                          className="flex items-start space-x-2 bg-white/5 p-3 rounded-lg"
                        >
                          <CheckCircle className="w-5 h-5 text-hasselblad flex-shrink-0 mt-0.5" />
                          <span className="text-sm text-white/80">{benefit}</span>
                        </motion.div>
                      ))}
                    </div>
                  </div>

                  {/* Tech Details */}
                  <div>
                    <h3 className="text-lg font-bold mb-3">技术实现</h3>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                      {selectedFeature.detail.techDetails.map((tech, idx) => (
                        <motion.div
                          key={idx}
                          initial={{ opacity: 0, scale: 0.9 }}
                          animate={{ opacity: 1, scale: 1 }}
                          transition={{ delay: idx * 0.05 }}
                          className="bg-white/5 p-3 rounded-lg text-center"
                        >
                          <span className="text-xs text-white/60">{tech}</span>
                        </motion.div>
                      ))}
                    </div>
                  </div>

                  {/* Action */}
                  {selectedFeature.detail.actionLink && (
                    <div className="text-center pt-4">
                      <a
                        href={selectedFeature.detail.actionLink}
                        className="btn-primary inline-flex items-center space-x-2"
                      >
                        <Download className="w-5 h-5" />
                        <span>{selectedFeature.detail.action}</span>
                      </a>
                    </div>
                  )}
                </div>
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Tools Quick Access */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="card p-8"
        >
          <h2 className="text-2xl font-bold mb-6 text-center gradient-text">
            影像工具
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
            <a href="/watermark" className="btn-primary text-center py-6 flex flex-col items-center space-y-2">
              <Palette className="w-8 h-8" />
              <span className="font-bold">水印生成器</span>
              <span className="text-xs opacity-80">10+品牌水印模板</span>
            </a>
            <a href="/editor" className="btn-secondary text-center py-6 flex flex-col items-center space-y-2">
              <Code className="w-8 h-8" />
              <span className="font-bold">预设编辑器</span>
              <span className="text-xs opacity-80">创建专属预设</span>
            </a>
          </div>
        </motion.div>

        {/* Tech Summary */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="card p-8"
        >
          <h2 className="text-2xl font-bold mb-6 text-center gradient-text">
            快速操作入口
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <a href="/ai-demo" className="btn-primary text-center py-4">
              <Cpu className="w-6 h-6 mx-auto mb-2" />
              体验AI场景识别
            </a>
            <a href="/" className="btn-secondary text-center py-4">
              <Search className="w-6 h-6 mx-auto mb-2" />
              浏览预设库
            </a>
            <button className="btn-secondary text-center py-4">
              <Download className="w-6 h-6 mx-auto mb-2" />
              下载APP
            </button>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
