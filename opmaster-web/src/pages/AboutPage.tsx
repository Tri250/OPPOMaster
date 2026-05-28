import { motion } from 'framer-motion';
import { Heart, Camera, Sparkles } from 'lucide-react';

export default function AboutPage() {
  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-16"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-green to-hasselblad rounded-2xl mb-6">
            <Camera className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text">
            关于我
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            热爱摄影的开发者，为摄影爱好者打造专业工具
          </p>
        </motion.div>

        {/* About Content */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="card p-8 mb-12"
        >
          <h2 className="text-2xl font-bold mb-6 gradient-text flex items-center gap-2">
            <Sparkles className="w-6 h-6" />
            小O帮帮
          </h2>
          <div className="space-y-4 text-white/70 leading-relaxed">
            <p>
              你好！我是「带娃的小陈工」，一名热爱摄影的开发者。小O帮帮诞生于对完美摄影体验的追求。
            </p>
            <p>
              我相信，每一次按下快门都值得被认真对待。从一键闪记到流体云胶囊，从 HNCS 认证预设到 AI 智能推荐，
              每一个功能都凝聚了我对"专业却简单"这一理念的坚持。
            </p>
            <p>
              希望小O帮帮能帮助你拍出更美的照片！
            </p>
          </div>
        </motion.div>

        {/* Contact */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="card p-8 text-center"
        >
          <h2 className="text-2xl font-bold mb-4 gradient-text flex items-center justify-center gap-2">
            <Heart className="w-6 h-6" />
            联系我
          </h2>
          <p className="text-white/60 mb-6">
            有任何问题或建议？抖音、小红书搜索「带娃的小陈工」
          </p>
        </motion.div>

        {/* Footer */}
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="mt-12 text-center text-sm text-white/40"
        >
          <p>© 2026 小O帮帮. 为摄影爱好者打造</p>
        </motion.div>
      </div>
    </div>
  );
}
