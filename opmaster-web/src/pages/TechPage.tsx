import { motion } from 'framer-motion';
import { 
  Cpu, 
  Cloud, 
  Zap, 
  Smartphone,
  Upload,
  Palette,
  Code,
  Share2,
  Database,
  Layers
} from 'lucide-react';

const features = [
  {
    icon: Cpu,
    title: 'AI场景识别',
    description: '智能识别拍摄场景，自动推荐最佳影像参数',
    color: 'from-purple-500 to-pink-500',
    features: [
      '50+场景类型识别',
      '毫秒级响应速度',
      '自适应参数推荐'
    ]
  },
  {
    icon: Smartphone,
    title: '原生相机参数自动填入',
    description: '基于安卓无障碍服务，无Root自动填参数',
    color: 'from-oppo-green to-cyan-500',
    features: [
      '支持六大品牌相机',
      '无Root合法合规',
      '10+步简化为2步'
    ]
  },
  {
    icon: Layers,
    title: '悬浮窗',
    description: '多悬浮窗类型兼容方案，适配率95%+',
    color: 'from-blue-500 to-indigo-500',
    features: [
      '标准悬浮窗类型',
      '相机上层显示',
      'SurfaceView绘制'
    ]
  },
  {
    icon: Database,
    title: '预设分类搜索',
    description: '多维度分类体系，快速找到心仪预设',
    color: 'from-green-500 to-emerald-500',
    features: [
      '按风格分类',
      '按场景分类',
      '全文搜索'
    ]
  },
  {
    icon: Code,
    title: '预设生态',
    description: '内置预设编辑器+一键社区贡献系统',
    color: 'from-yellow-500 to-orange-500',
    features: [
      '预设编辑器',
      '一键贡献',
      '预设排行榜'
    ]
  },
  {
    icon: Palette,
    title: '多格式预设导入导出',
    description: '支持主流修图工具预设格式',
    color: 'from-red-500 to-pink-500',
    features: [
      'LUT文件解析',
      '泼辣修图预设',
      'Lightroom预设'
    ]
  }
];

export default function TechPage() {
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
            核心功能展示
          </p>
        </motion.div>

        {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-16">
          {features.map((feature, idx) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
              whileHover={{ y: -8 }}
              className="card p-6 group"
            >
              <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${feature.color} p-4 mb-4 group-hover:scale-110 transition-transform duration-300`}>
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
            </motion.div>
          ))}
        </div>

        {/* Tech Detail */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="card p-8"
        >
          <h2 className="text-2xl font-bold mb-6 text-center gradient-text">
            技术亮点
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <h3 className="text-lg font-bold text-hasselblad">原生相机参数自动填入</h3>
              <p className="text-white/70 text-sm">解决用户"手动输入参数"的最高频痛点，从"参数参考工具"升级为"参数执行工具"</p>
              <ul className="text-white/50 text-sm space-y-2">
                <li>• 基于安卓无障碍服务，无Root、合法合规</li>
                <li>• 针对六大品牌原生相机大师模式</li>
                <li>• 自动填充所有基础/专业参数</li>
              </ul>
            </div>
            <div className="space-y-4">
              <h3 className="text-lg font-bold text-hasselblad">悬浮窗</h3>
              <p className="text-white/70 text-sm">解决ColorOS/小米等系统悬浮窗无法开启、无法显示在相机上层的核心功能失效问题</p>
              <ul className="text-white/50 text-sm space-y-2">
                <li>• 多悬浮窗类型兼容方案</li>
                <li>• 针对四大厂商定制权限逻辑</li>
                <li>• 适配率从30%提升到95%+</li>
              </ul>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
