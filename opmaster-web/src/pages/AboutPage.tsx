import { motion } from 'framer-motion';
import { Heart, Camera, Users, Award } from 'lucide-react';

const teamMembers = [
  {
    name: '产品团队',
    role: '产品设计',
    description: '负责产品规划与用户体验设计',
    icon: Users,
    color: 'from-blue-500 to-cyan-500'
  },
  {
    name: '摄影专家',
    role: '内容创作',
    description: '专业摄影师与调色师团队',
    icon: Camera,
    color: 'from-hasselblad to-orange-500'
  },
  {
    name: '工程师',
    role: '技术开发',
    description: '移动端与AI技术专家',
    icon: Award,
    color: 'from-purple-500 to-pink-500'
  },
  {
    name: '社区运营',
    role: '用户支持',
    description: '用户反馈与社区建设',
    icon: Heart,
    color: 'from-pink-500 to-red-500'
  }
];

const milestones = [
  { year: '2023', event: '项目启动，开始产品研发' },
  { year: '2024 Q1', event: 'ColorOS深度集成功能上线' },
  { year: '2024 Q2', event: 'AI智能推荐系统发布' },
  { year: '2024 Q3', event: '社区功能与UGC体系上线' },
  { year: '2024 Q4', event: '云同步与多设备协同功能' }
];

export default function AboutPage() {
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
            关于我们
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            致力于为摄影爱好者提供专业、便捷的移动摄影体验
          </p>
        </motion.div>

        {/* Brand Story */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="card p-8 mb-12"
        >
          <h2 className="text-2xl font-bold mb-6 gradient-text">品牌故事</h2>
          <div className="space-y-4 text-white/70 leading-relaxed">
            <p>
              OPPOMaster 诞生于对完美摄影体验的追求。我们相信，每一次按下快门都值得被认真对待。
            </p>
            <p>
              作为 OPPO 官方合作伙伴，我们深度整合 ColorOS 16 系统能力，结合哈苏在影像领域的专业积累，
              为 Find X 系列和 Reno 系列用户打造前所未有的摄影参数管理体验。
            </p>
            <p>
              从一键闪记到流体云胶囊，从 HNCS 认证预设到 AI 智能推荐，每一个功能都凝聚了我们对
              "专业却简单" 这一理念的坚持。
            </p>
          </div>
        </motion.div>

        {/* Team */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="mb-12"
        >
          <h2 className="text-3xl font-bold mb-8 text-center gradient-text">
            核心团队
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {teamMembers.map((member, idx) => (
              <motion.div
                key={member.name}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: idx * 0.1 }}
                whileHover={{ y: -8 }}
                className="card p-6 text-center"
              >
                <div className={`w-20 h-20 mx-auto rounded-2xl bg-gradient-to-br ${member.color} p-4 mb-4`}>
                  <member.icon className="w-full h-full text-white" />
                </div>
                <h3 className="text-lg font-bold mb-1">{member.name}</h3>
                <p className="text-sm text-hasselblad mb-2">{member.role}</p>
                <p className="text-sm text-white/60">{member.description}</p>
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* Timeline */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="mb-12"
        >
          <h2 className="text-3xl font-bold mb-8 text-center gradient-text">
            发展历程
          </h2>
          <div className="relative">
            {/* Timeline Line */}
            <div className="absolute left-4 md:left-1/2 top-0 bottom-0 w-0.5 bg-gradient-to-b from-hasselblad to-transparent" />
            
            {/* Timeline Items */}
            <div className="space-y-8">
              {milestones.map((milestone, idx) => (
                <motion.div
                  key={milestone.year}
                  initial={{ opacity: 0, x: idx % 2 === 0 ? -20 : 20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: idx * 0.1 }}
                  className={`relative flex items-center ${
                    idx % 2 === 0 ? 'md:flex-row' : 'md:flex-row-reverse'
                  }`}
                >
                  <div className={`flex-1 ${idx % 2 === 0 ? 'md:pr-12 md:text-right' : 'md:pl-12 md:text-left'} pl-12`}>
                    <div className="card p-4 inline-block">
                      <span className="text-hasselblad font-bold">{milestone.year}</span>
                      <p className="text-white/80 mt-1">{milestone.event}</p>
                    </div>
                  </div>
                  
                  {/* Timeline Dot */}
                  <div className="absolute left-4 md:left-1/2 md:-translate-x-1/2 w-3 h-3 bg-hasselblad rounded-full shadow-lg shadow-hasselblad/50" />
                  
                  <div className="flex-1 hidden md:block" />
                </motion.div>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Contact */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="card p-8 text-center"
        >
          <h2 className="text-2xl font-bold mb-4 gradient-text">联系我们</h2>
          <p className="text-white/60 mb-6">
            有任何问题或建议？我们随时欢迎您的反馈
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <button className="btn-primary">
              发送反馈
            </button>
            <button className="btn-secondary">
              加入社区
            </button>
          </div>
        </motion.div>

        {/* Footer */}
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="mt-12 text-center text-sm text-white/40"
        >
          <p>© 2024 OPPOMaster. 基于 OPPO 与哈苏合作技术打造</p>
          <p className="mt-2">Made with ❤️ for photography enthusiasts</p>
        </motion.div>
      </div>
    </div>
  );
}
