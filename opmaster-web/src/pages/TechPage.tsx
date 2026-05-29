import { motion } from 'framer-motion';
import { 
  Cpu, 
  Zap, 
  Shield,
  Sparkles
} from 'lucide-react';

const techStack = [
  {
    icon: Cpu,
    title: 'AI场景识别',
    description: '基于深度学习的图像识别技术，自动分析场景类型、主体、光照条件',
    color: 'from-purple-500 to-pink-500',
    features: [
      '50+场景类型识别',
      '毫秒级响应速度',
      '自适应参数推荐'
    ]
  },
  {
    icon: Sparkles,
    title: '实时渲染引擎',
    description: 'GPU加速的图像处理管线，支持LUT实时应用和参数调整',
    color: 'from-blue-500 to-cyan-500',
    features: [
      'OpenGL ES 3.0加速',
      '3D LUT实时预览',
      '零延迟参数调节'
    ]
  },
  {
    icon: Shield,
    title: '安全隐私',
    description: '本地处理优先，敏感数据不上传，保护用户隐私安全',
    color: 'from-gray-600 to-gray-800',
    features: [
      '端侧AI处理',
      '隐私计算技术',
      'GDPR合规'
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
          <h1 className="text-4xl md:text-5xl font-bold mb-4 bg-gradient-to-r from-white via-oppo-orange to-hasselblad bg-clip-text text-transparent">
            影像工具
          </h1>
          <p className="text-lg text-text-secondary max-w-2xl mx-auto">
            采用业界领先的技术方案，打造高性能、高可用的移动应用
          </p>
        </motion.div>

        {/* Tech Stack Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-16">
          {techStack.map((tech, idx) => (
            <motion.div
              key={tech.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
              whileHover={{ y: -8 }}
              className="bg-surface border border-border-subtle rounded-2xl p-6 group hover:border-oppo-orange/30 transition-all duration-300"
            >
              <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${tech.color} p-4 mb-4 group-hover:scale-110 transition-transform duration-300`}>
                <tech.icon className="w-full h-full text-white" />
              </div>
              
              <h3 className="text-xl font-bold mb-2 group-hover:text-oppo-orange transition-colors text-text-primary">
                {tech.title}
              </h3>
              
              <p className="text-text-secondary text-sm mb-4">
                {tech.description}
              </p>
              
              <ul className="space-y-2">
                {tech.features.map((feature, fIdx) => (
                  <li key={fIdx} className="flex items-center text-sm text-text-tertiary">
                    <div className="w-1.5 h-1.5 bg-oppo-orange rounded-full mr-2" />
                    {feature}
                  </li>
                ))}
              </ul>
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  );
}
