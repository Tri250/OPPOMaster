import { Link } from 'react-router-dom';
import { Layers, Sparkles, Camera, Image, Share2, Palette } from 'lucide-react';
import { motion } from 'framer-motion';

const coreFeatures = [
  {
    icon: <Layers className="w-10 h-10" />,
    title: '预设管理',
    description: 'HNCS认证预设，专业色彩调校',
    color: 'text-hasselblad'
  },
  {
    icon: <Sparkles className="w-10 h-10" />,
    title: 'AI智能',
    description: '24种场景识别，智能参数推荐',
    color: 'text-purple-400'
  },
  {
    icon: <Camera className="w-10 h-10" />,
    title: '参数显示',
    description: '悬浮窗实时显示，专业拍摄辅助',
    color: 'text-blue-400'
  },
  {
    icon: <Image className="w-10 h-10" />,
    title: '水印编辑',
    description: '专业模板，批量处理',
    color: 'text-green-400'
  },
  {
    icon: <Share2 className="w-10 h-10" />,
    title: '社交分享',
    description: '9+分享渠道，原图分享',
    color: 'text-pink-400'
  },
  {
    icon: <Palette className="w-10 h-10" />,
    title: '主题系统',
    description: '深色/浅色/跟随系统',
    color: 'text-yellow-400'
  }
];

export default function FeaturesOverview() {
  return (
    <section className="py-20">
      <div className="max-w-6xl mx-auto px-6">
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">
            核心功能
            <span className="gradient-text"> 预览</span>
          </h2>
          <p className="text-white/60 text-lg max-w-2xl mx-auto">
            探索OPPO哈苏影像系统的强大功能，专业摄影从此更简单
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
          {coreFeatures.map((feature, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              whileHover={{ y: -8, scale: 1.02 }}
              className="card p-6 group cursor-pointer"
            >
              <div className={`${feature.color} mb-4 group-hover:scale-110 transition-transform duration-300`}>
                {feature.icon}
              </div>
              <h3 className="text-xl font-semibold mb-2 text-white">
                {feature.title}
              </h3>
              <p className="text-white/60 text-sm">
                {feature.description}
              </p>
            </motion.div>
          ))}
        </div>

        <div className="text-center">
          <Link to="/features" className="btn-primary inline-flex items-center gap-2">
            查看全部功能
            <span>→</span>
          </Link>
        </div>
      </div>
    </section>
  );
}
