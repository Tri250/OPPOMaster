import { motion } from 'framer-motion';
import { Camera, Heart } from 'lucide-react';

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
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-orange to-hasselblad rounded-2xl mb-6">
            <Camera className="w-12 h-12 text-deep-space" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 bg-gradient-to-r from-white via-oppo-orange to-hasselblad bg-clip-text text-transparent">
            关于我
          </h1>
          <p className="text-lg text-text-secondary max-w-2xl mx-auto">
            致力于为摄影爱好者提供专业、便捷的移动摄影体验
          </p>
        </motion.div>

        {/* Brand Story */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="bg-surface border border-border-subtle rounded-3xl p-8 mb-12"
        >
          <h2 className="text-2xl font-bold mb-6 bg-gradient-to-r from-white via-oppo-orange to-hasselblad bg-clip-text text-transparent">品牌故事</h2>
          <div className="space-y-4 text-text-secondary leading-relaxed">
            <p>
              小O帮帮 诞生于对完美摄影体验的追求。我们相信，每一次按下快门都值得被认真对待。
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

        {/* About Me */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="mb-12"
        >
          <h2 className="text-3xl font-bold mb-8 text-center bg-gradient-to-r from-white via-oppo-orange to-hasselblad bg-clip-text text-transparent">
            热爱摄影的：小陈工
          </h2>
          <div className="bg-surface border border-border-subtle rounded-3xl p-8 text-center">
            <div className="w-32 h-32 mx-auto rounded-full bg-gradient-to-br from-oppo-orange to-hasselblad p-4 mb-6">
              <Camera className="w-full h-full text-deep-space" />
            </div>
            <p className="text-text-secondary text-lg">
              一个热爱摄影的程序员，专注于为大家打造好用的摄影工具
            </p>
          </div>
        </motion.div>

        {/* Contact */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="bg-surface border border-border-subtle rounded-3xl p-8 text-center mb-8"
        >
          <h2 className="text-2xl font-bold mb-4 bg-gradient-to-r from-white via-oppo-orange to-hasselblad bg-clip-text text-transparent">联系我</h2>
          <p className="text-text-secondary mb-6">
            有任何问题或建议，抖音、小红书搜索 带娃的小陈工
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <div className="flex items-center gap-2 px-6 py-3 bg-surface-hover rounded-xl">
              <Heart className="w-5 h-5 text-oppo-orange" />
              <span className="text-text-primary">感谢您的支持！</span>
            </div>
          </div>
        </motion.div>

        {/* Footer */}
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="mt-12 text-center text-sm text-text-tertiary"
        >
          <p>© 2026 小O帮帮. 基于 OPPO 与哈苏合作技术打造</p>
          <p className="mt-2">Made with ❤️ for photography enthusiasts</p>
        </motion.div>
      </div>
    </div>
  );
}
