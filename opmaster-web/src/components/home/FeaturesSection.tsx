import { motion } from 'framer-motion';
import { 
  Sparkles, 
  Smartphone, 
  Monitor,
  Search,
  Zap,
  FileText 
} from 'lucide-react';
import FeatureCard from './FeatureCard';
import ToolCard from './ToolCard';
import { Palette, Code } from 'lucide-react';

const features = [
  {
    icon: <Sparkles className="w-8 h-8 text-white" />,
    title: 'AI场景识别',
    description: '智能识别拍摄场景，自动推荐最佳影像参数',
    features: [
      '支持50+场景类型自动识别',
      '基于百万级样本AI模型训练',
      '毫秒级响应，即时查看效果'
    ],
    gradient: 'linear-gradient(135deg, #FF6B35 0%, #9C27B0 100%)',
    color: '#FF6B35'
  },
  {
    icon: <Smartphone className="w-8 h-8 text-white" />,
    title: '原生相机参数自动填入',
    description: '基于安卓无障碍服务，无Root自动填参数',
    features: [
      '支持OPPO/一加/Realme等品牌',
      '无需Root权限，安全合规',
      '操作步骤从10+步简化到2步'
    ],
    gradient: '',
    color: '#00C853'
  },
  {
    icon: <Monitor className="w-8 h-8 text-white" />,
    title: '悬浮窗',
    description: '多悬浮窗类型兼容方案，适配率95%+',
    features: [
      '全局参数显示',
      '快捷操作入口',
      'ColorOS Fluid Capsule支持'
    ],
    gradient: '',
    color: '#2962FF'
  },
  {
    icon: <Search className="w-8 h-8 text-white" />,
    title: '预设分类搜索',
    description: '多维度分类体系，快速找到心仪预设',
    features: [
      '按品牌/场景/风格分类',
      '智能搜索推荐',
      '收藏管理功能'
    ],
    gradient: '',
    color: '#00C853'
  },
  {
    icon: <Zap className="w-8 h-8 text-white" />,
    title: '预设生态',
    description: '内置预设编辑器+一键社区贡献系统',
    features: [
      '创建专属预设',
      '分享到社区',
      '使用他人优秀预设'
    ],
    gradient: '',
    color: '#FF9800'
  },
  {
    icon: <FileText className="w-8 h-8 text-white" />,
    title: '多格式预设导入导出',
    description: '支持主流修图工具预设格式',
    features: [
      'LUT文件(.cube)支持',
      '泼辣/Lightroom预设',
      'JSON格式兼容'
    ],
    gradient: '',
    color: '#E91E63'
  }
];

const tools = [
  {
    icon: <Palette className="w-8 h-8" />,
    title: '水印生成器',
    subtitle: '10+品牌水印模板',
    isPrimary: true
  },
  {
    icon: <Code className="w-8 h-8" />,
    title: '预设编辑器',
    subtitle: '创建专属预设',
    isPrimary: false
  }
];

export default function FeaturesSection() {
  return (
    <section className="py-6 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      {/* 影像参数区 - 占页面约70%高度 */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="mb-16"
      >
        {/* 区域标题 - 哈苏橙，水平居中 */}
        <div className="text-center mb-10">
          <h2 className="text-3xl md:text-4xl font-bold text-[#D4A574] mb-3">
            影像参数
          </h2>
          {/* 分隔线 - 高度1dp，颜色#333333 */}
          <div className="w-full h-px bg-[#333333] mt-6" />
        </div>

        {/* 6个功能卡片网格 - 左对齐 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <FeatureCard
              key={index}
              {...feature}
              index={index}
            />
          ))}
        </div>
      </motion.div>

      {/* 影像工具区 - 占页面约30%高度 */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ delay: 0.3 }}
        className="relative"
      >
        {/* 区域标题 - 哈苏橙，水平居中 */}
        <div className="text-center mb-10">
          <h2 className="text-3xl md:text-4xl font-bold text-[#D4A574] mb-3">
            影像工具
          </h2>
          {/* 分隔线 - 高度1dp，颜色#333333 */}
          <div className="w-full h-px bg-[#333333] mt-6" />
        </div>

        {/* 2个工具卡片 - 上下叠加设计 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 relative">
          {tools.map((tool, index) => (
            <ToolCard
              key={index}
              {...tool}
              index={index}
            />
          ))}
          
          {/* 叠加效果 - 使用绝对定位创建上下叠加 */}
          <div className="hidden md:block absolute top-1/2 left-0 right-0 h-5 bg-transparent -translate-y-1/2 z-10 pointer-events-none">
            <div className="absolute top-0 left-1/4 right-1/4 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          </div>
        </div>
      </motion.div>
    </section>
  );
}
