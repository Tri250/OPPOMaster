import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Sparkles, Palette, Camera, Cpu } from 'lucide-react';

export default function OppoQuickActions() {
  const actions = [
    {
      id: 'ai',
      title: 'AI 场景识别',
      description: '智能识别拍摄场景',
      icon: Sparkles,
      color: 'from-oppo-orange to-hasselblad',
      route: '/ai-demo',
    },
    {
      id: 'presets',
      title: '预设推荐',
      description: '精选哈苏认证预设',
      icon: Palette,
      color: 'from-oppo-green to-oppo-green-light',
      route: '/about',
    },
    {
      id: 'camera',
      title: '相机参数',
      description: '实时相机参数调节',
      icon: Camera,
      color: 'from-ocean-blue to-ocean-blue-light',
      route: '/tech',
    },
    {
      id: 'tools',
      title: '影像工具',
      description: '专业后期工具集',
      icon: Cpu,
      color: 'from-purple-500 to-pink-500',
      route: '/about',
    },
  ];

  return (
    <section className="py-12 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="mb-8"
      >
        <h2 className="text-2xl font-bold text-text-primary">
          快捷操作
        </h2>
      </motion.div>

      <div className="grid grid-cols-2 gap-4">
        {actions.map((action, index) => (
          <motion.div
            key={action.id}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: index * 0.1 }}
            whileHover={{ scale: 1.02, y: -2 }}
            whileTap={{ scale: 0.98 }}
          >
            <Link to={action.route}>
              <div className="bg-surface border border-border-subtle rounded-2xl p-5 hover:border-oppo-orange/30 transition-all duration-300 h-full">
                <div className={`w-12 h-12 bg-gradient-to-br ${action.color} rounded-xl flex items-center justify-center mb-4`}>
                  <action.icon className="w-6 h-6 text-white" />
                </div>
                <h3 className="text-lg font-semibold text-text-primary mb-1">
                  {action.title}
                </h3>
                <p className="text-sm text-text-tertiary">
                  {action.description}
                </p>
              </div>
            </Link>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
