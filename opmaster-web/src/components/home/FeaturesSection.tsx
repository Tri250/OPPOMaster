import { motion } from 'framer-motion';
import { 
  Sparkles, 
  Camera, 
  Cloud, 
  Zap, 
  Users, 
  Shield 
} from 'lucide-react';

const features = [
  {
    icon: Sparkles,
    title: 'AI智能推荐',
    description: '智能识别场景，自动匹配最佳预设参数',
    color: 'from-purple-500 to-pink-500'
  },
  {
    icon: Camera,
    title: '哈苏认证',
    description: 'HNCS专业色彩认证，还原真实自然色彩',
    color: 'from-hasselblad to-orange-500'
  },
  {
    icon: Cloud,
    title: '流体云胶囊',
    description: 'ColorOS深度集成，一键应用预设参数',
    color: 'from-blue-500 to-cyan-500'
  },
  {
    icon: Zap,
    title: '实时预览',
    description: 'GPU加速渲染，所见即所得的编辑体验',
    color: 'from-green-500 to-emerald-500'
  },
  {
    icon: Users,
    title: '社区生态',
    description: 'UGC预设分享，发现更多创作灵感',
    color: 'from-pink-500 to-rose-500'
  },
  {
    icon: Shield,
    title: '隐私保护',
    description: '本地优先处理，数据安全有保障',
    color: 'from-gray-600 to-gray-800'
  }
];

export default function FeaturesSection() {
  return (
    <section className="py-20 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="text-center mb-16"
      >
        <h2 className="text-4xl md:text-5xl font-bold mb-4 gradient-text">
          核心功能特性
        </h2>
        <p className="text-lg text-white/60 max-w-2xl mx-auto">
          融合专业摄影技术与AI智能，为您带来前所未有的移动摄影体验
        </p>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {features.map((feature, idx) => (
          <motion.div
            key={feature.title}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: idx * 0.1 }}
            whileHover={{ y: -8 }}
            className="card p-6 group cursor-pointer"
          >
            <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${feature.color} p-4 mb-4 group-hover:scale-110 transition-transform duration-300`}>
              <feature.icon className="w-full h-full text-white" />
            </div>
            
            <h3 className="text-xl font-bold mb-2 group-hover:text-hasselblad transition-colors">
              {feature.title}
            </h3>
            
            <p className="text-white/60 group-hover:text-white/80 transition-colors">
              {feature.description}
            </p>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
