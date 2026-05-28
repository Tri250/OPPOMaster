import { motion } from 'framer-motion';
import { 
  Cpu, 
  Database, 
  Cloud, 
  Zap, 
  Shield, 
  Layers 
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
    icon: Layers,
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
    icon: Database,
    title: 'Room数据库',
    description: '本地优先的数据架构，支持离线使用，数据同步更安心',
    color: 'from-green-500 to-emerald-500',
    features: [
      '本地预设缓存',
      '编辑历史记录',
      '隐私数据保护'
    ]
  },
  {
    icon: Cloud,
    title: '云端同步',
    description: '跨设备数据同步，支持收藏、参数、自定义预设云端备份',
    color: 'from-yellow-500 to-orange-500',
    features: [
      '实时数据同步',
      '多设备协同',
      '智能冲突解决'
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
  },
  {
    icon: Zap,
    title: '性能优化',
    description: '精细的性能调优，确保流畅的用户体验和低功耗运行',
    color: 'from-red-500 to-pink-500',
    features: [
      '冷启动<1.5秒',
      '内存占用<150MB',
      '帧率稳定60fps'
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
            技术架构
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
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
              className="card p-6 group"
            >
              <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${tech.color} p-4 mb-4 group-hover:scale-110 transition-transform duration-300`}>
                <tech.icon className="w-full h-full text-white" />
              </div>
              
              <h3 className="text-xl font-bold mb-2 group-hover:text-hasselblad transition-colors">
                {tech.title}
              </h3>
              
              <p className="text-white/60 text-sm mb-4">
                {tech.description}
              </p>
              
              <ul className="space-y-2">
                {tech.features.map((feature, fIdx) => (
                  <li key={fIdx} className="flex items-center text-sm text-white/50">
                    <div className="w-1.5 h-1.5 bg-hasselblad rounded-full mr-2" />
                    {feature}
                  </li>
                ))}
              </ul>
            </motion.div>
          ))}
        </div>

        {/* Architecture Diagram */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="card p-8"
        >
          <h2 className="text-2xl font-bold mb-6 text-center gradient-text">
            系统架构图
          </h2>
          
          <div className="relative">
            {/* Architecture Layers */}
            <div className="space-y-4">
              {/* UI Layer */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                className="relative"
              >
                <div className="border-2 border-purple-500 rounded-xl p-4 bg-purple-500/10">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-purple-400 font-bold">表现层 UI Layer</span>
                    <span className="text-xs text-white/40">Jetpack Compose</span>
                  </div>
                  <div className="grid grid-cols-3 gap-3 text-sm">
                    <div className="bg-white/5 rounded-lg p-2 text-center">首页</div>
                    <div className="bg-white/5 rounded-lg p-2 text-center">详情</div>
                    <div className="bg-white/5 rounded-lg p-2 text-center">设置</div>
                  </div>
                </div>
                <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 w-0 h-0 border-l-8 border-r-8 border-t-8 border-l-transparent border-r-transparent border-t-purple-500" />
              </motion.div>

              {/* State Layer */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.1 }}
                className="relative pl-8"
              >
                <div className="border-2 border-blue-500 rounded-xl p-4 bg-blue-500/10">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-blue-400 font-bold">状态管理层 State</span>
                    <span className="text-xs text-white/40">MVI Architecture</span>
                  </div>
                  <div className="grid grid-cols-2 gap-3 text-sm">
                    <div className="bg-white/5 rounded-lg p-2 text-center">ViewModel</div>
                    <div className="bg-white/5 rounded-lg p-2 text-center">StateFlow</div>
                  </div>
                </div>
                <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 w-0 h-0 border-l-8 border-r-8 border-t-8 border-l-transparent border-r-transparent border-t-blue-500" />
              </motion.div>

              {/* Data Layer */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.2 }}
                className="relative pl-8"
              >
                <div className="border-2 border-green-500 rounded-xl p-4 bg-green-500/10">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-green-400 font-bold">数据层 Data</span>
                    <span className="text-xs text-white/40">Repository Pattern</span>
                  </div>
                  <div className="grid grid-cols-3 gap-3 text-sm">
                    <div className="bg-white/5 rounded-lg p-2 text-center">Room</div>
                    <div className="bg-white/5 rounded-lg p-2 text-center">Retrofit</div>
                    <div className="bg-white/5 rounded-lg p-2 text-center">DataStore</div>
                  </div>
                </div>
                <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 w-0 h-0 border-l-8 border-r-8 border-t-8 border-l-transparent border-r-transparent border-t-green-500" />
              </motion.div>

              {/* AI Layer */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.3 }}
                className="relative pl-8"
              >
                <div className="border-2 border-pink-500 rounded-xl p-4 bg-pink-500/10">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-pink-400 font-bold">AI层 AI Engine</span>
                    <span className="text-xs text-white/40">ML Kit + TensorFlow</span>
                  </div>
                  <div className="grid grid-cols-3 gap-3 text-sm">
                    <div className="bg-white/5 rounded-lg p-2 text-center">识别</div>
                    <div className="bg-white/5 rounded-lg p-2 text-center">推荐</div>
                    <div className="bg-white/5 rounded-lg p-2 text-center">处理</div>
                  </div>
                </div>
              </motion.div>
            </div>
          </div>
        </motion.div>

        {/* Performance Stats */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="mt-12 grid grid-cols-2 md:grid-cols-4 gap-6"
        >
          {[
            { label: '启动时间', value: '<1.5s', unit: '秒' },
            { label: '内存占用', value: '<150', unit: 'MB' },
            { label: '响应延迟', value: '<100', unit: 'ms' },
            { label: '崩溃率', value: '<0.1', unit: '%' }
          ].map((stat, idx) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, scale: 0.8 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ delay: idx * 0.1 }}
              className="card p-6 text-center"
            >
              <div className="text-3xl font-bold text-hasselblad mb-1">
                {stat.value}
                <span className="text-lg text-white/60">{stat.unit}</span>
              </div>
              <div className="text-sm text-white/60">{stat.label}</div>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </div>
  );
}
