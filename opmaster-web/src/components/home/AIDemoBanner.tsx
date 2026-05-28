import { motion, AnimatePresence } from 'framer-motion';
import { Sparkles, ArrowRight, Play, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useState } from 'react';

export default function AIDemoBanner() {
  const [showVideo, setShowVideo] = useState(false);

  return (
    <section className="py-20 px-4 sm:px-6 lg:px-8">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        whileInView={{ opacity: 1, scale: 1 }}
        viewport={{ once: true }}
        className="max-w-7xl mx-auto relative overflow-hidden rounded-3xl"
      >
        {/* Background */}
        <div className="absolute inset-0 bg-gradient-to-br from-purple-900/50 via-deep-space to-pink-900/50" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_50%,rgba(139,92,246,0.3)_0%,transparent_50%)]" />
        
        {/* Content */}
        <div className="relative z-10 px-8 py-16 md:px-16 md:py-20 text-center">
          <motion.div
            initial={{ scale: 0 }}
            whileInView={{ scale: 1 }}
            viewport={{ once: true }}
            className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-purple-500 to-pink-500 rounded-2xl mb-8 shadow-2xl shadow-purple-500/30"
          >
            <Sparkles className="w-12 h-12 text-white" />
          </motion.div>

          <h2 className="text-4xl md:text-6xl font-bold mb-6">
            <span className="bg-gradient-to-r from-purple-400 via-pink-400 to-purple-400 bg-clip-text text-transparent">
              AI智能场景识别
            </span>
          </h2>

          <p className="text-xl text-white/70 mb-8 max-w-2xl mx-auto">
            上传您的照片，AI将自动识别场景类型，智能推荐最匹配的预设参数，
            让每一张照片都能呈现最佳效果
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link
              to="/ai-demo"
              className="btn-primary text-lg px-8 py-4 flex items-center space-x-2 group"
            >
              <span>立即体验</span>
              <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
            </Link>
            <button 
              onClick={() => setShowVideo(true)}
              className="btn-secondary text-lg px-8 py-4 flex items-center space-x-2"
            >
              <Play className="w-5 h-5" />
              <span>观看演示视频</span>
            </button>
          </div>

          {/* Feature Points */}
          <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-8 max-w-4xl mx-auto">
            {[
              { title: '智能识别', desc: '支持50+场景类型自动识别' },
              { title: '精准推荐', desc: '基于百万级样本训练的AI模型' },
              { title: '实时预览', desc: '毫秒级响应，即时查看效果' }
            ].map((point, idx) => (
              <motion.div
                key={point.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.3 + idx * 0.1 }}
                className="text-center"
              >
                <h3 className="text-lg font-bold mb-2 text-white">{point.title}</h3>
                <p className="text-sm text-white/60">{point.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </motion.div>

      {/* Video Modal */}
      <AnimatePresence>
        {showVideo && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
            onClick={() => setShowVideo(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              onClick={(e) => e.stopPropagation()}
              className="bg-deep-space-light rounded-2xl max-w-4xl w-full overflow-hidden"
            >
              {/* Modal Header */}
              <div className="bg-gradient-to-r from-purple-500 to-pink-500 p-4 flex items-center justify-between">
                <h3 className="text-xl font-bold text-white">AI场景识别演示</h3>
                <button
                  onClick={() => setShowVideo(false)}
                  className="w-10 h-10 bg-white/20 hover:bg-white/30 rounded-full flex items-center justify-center transition-colors"
                >
                  <X className="w-6 h-6 text-white" />
                </button>
              </div>
              {/* Video Content */}
              <div className="p-6">
                <div className="aspect-video bg-black/50 rounded-xl flex items-center justify-center">
                  <div className="text-center">
                    <Play className="w-16 h-16 text-white/50 mx-auto mb-4" />
                    <p className="text-white/70 mb-4">演示视频正在制作中...</p>
                    <div className="grid grid-cols-1 gap-4 text-left text-sm text-white/60">
                      <div className="bg-white/5 p-4 rounded-lg">
                        <h4 className="font-bold text-white/80 mb-2">🎉 您可以体验：</h4>
                        <ul className="space-y-1">
                          <li>• 点击"立即体验"上传自己的照片</li>
                          <li>• 或选择示例图片体验AI识别</li>
                          <li>• 查看识别结果和推荐预设</li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>
                <div className="mt-6 flex justify-center">
                  <Link
                    to="/ai-demo"
                    onClick={() => setShowVideo(false)}
                    className="btn-primary flex items-center space-x-2"
                  >
                    <Sparkles className="w-5 h-5" />
                    <span>立即体验AI识别</span>
                  </Link>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </section>
  );
}
