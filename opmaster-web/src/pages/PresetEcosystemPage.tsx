import { motion } from 'framer-motion'
import { Palette, Download, Star, ChevronRight } from 'lucide-react'
import { useState } from 'react'

const communityPresets = [
  { id: 1, name: '街头摄影', author: '摄影师阿东', rating: 4.8, downloads: 2341, category: '人文' },
  { id: 2, name: '复古胶片感', author: '光影玩家', rating: 4.9, downloads: 4567, category: '胶片' },
  { id: 3, name: '小清新', author: '少女心', rating: 4.7, downloads: 3456, category: '人像' },
  { id: 4, name: '电影感调色', author: '电影人', rating: 4.6, downloads: 1234, category: '电影' },
]

export default function PresetEcosystemPage() {
  const [activeTab, setActiveTab] = useState('editor')

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">预设生态系统</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <div className="flex gap-1 p-1 bg-white/5 rounded-oppo">
          {['editor', 'community'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex-1 py-2.5 rounded-oppo-sm text-sm font-medium transition-all duration-200 touch-feedback ${
                activeTab === tab
                  ? 'bg-oppo-sunrise-gold text-deep-space'
                  : 'text-text-secondary hover:text-white'
              }`}
            >
              {tab === 'editor' ? '预设编辑器' : '社区'}
            </button>
          ))}
        </div>

        {activeTab === 'editor' ? (
          <>
            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="card-oppo p-4"
            >
              <h2 className="text-sm font-medium text-text-secondary mb-4">创建新预设</h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm text-text-secondary mb-2">预设名称</label>
                  <input
                    type="text"
                    placeholder="输入预设名称..."
                    className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-oppo text-white placeholder-text-tertiary focus:outline-none focus:border-oppo-sunrise-gold/50 transition-colors duration-200"
                    aria-label="预设名称"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm text-text-secondary mb-2">亮度</label>
                    <input
                      type="range"
                      min="-100"
                      max="100"
                      defaultValue="0"
                      className="w-full"
                      aria-label="亮度调整"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-text-secondary mb-2">对比度</label>
                    <input
                      type="range"
                      min="-100"
                      max="100"
                      defaultValue="0"
                      className="w-full"
                      aria-label="对比度调整"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-text-secondary mb-2">饱和度</label>
                    <input
                      type="range"
                      min="-100"
                      max="100"
                      defaultValue="0"
                      className="w-full"
                      aria-label="饱和度调整"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-text-secondary mb-2">色温</label>
                    <input
                      type="range"
                      min="-100"
                      max="100"
                      defaultValue="0"
                      className="w-full"
                      aria-label="色温调整"
                    />
                  </div>
                </div>
                <div className="flex gap-3">
                  <button className="flex-1 btn-primary touch-feedback">保存预设</button>
                  <button className="flex-1 btn-secondary touch-feedback">导出</button>
                </div>
              </div>
            </motion.section>

            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="card-oppo p-4"
            >
              <h2 className="text-sm font-medium text-text-secondary mb-4">预设参数</h2>
              <div className="space-y-3">
                {['我的预设 1', '我的预设 2', '我的预设 3'].map((preset) => (
                  <div key={preset} className="flex items-center justify-between py-2 border-b border-white/5 last:border-0">
                    <span className="text-sm">{preset}</span>
                    <div className="flex gap-2">
                      <button className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors duration-200 touch-feedback" aria-label="编辑">
                        <Palette className="w-4 h-4" />
                      </button>
                      <button className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors duration-200 touch-feedback" aria-label="删除">
                        <ChevronRight className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </motion.section>
          </>
        ) : (
          <>
            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="card-oppo p-4"
            >
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-medium text-text-secondary">热门社区预设</h2>
                <button className="btn-secondary text-sm py-2 touch-feedback">上传我的预设</button>
              </div>
              <div className="space-y-3">
                {communityPresets.map((preset, i) => (
                  <motion.div
                    key={preset.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.05 }}
                    className="p-4 bg-white/5 rounded-oppo flex items-center gap-4"
                  >
                    <div className="w-12 h-12 rounded-oppo bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex-shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm truncate">{preset.name}</p>
                      <p className="text-text-tertiary text-xs">@{preset.author}</p>
                    </div>
                    <div className="flex items-center gap-4 text-xs text-text-tertiary">
                      <div className="flex items-center gap-1">
                        <Star className="w-3 h-3 fill-oppo-sunrise-gold text-oppo-sunrise-gold" />
                        <span>{preset.rating}</span>
                      </div>
                      <span>{preset.downloads.toLocaleString()}</span>
                    </div>
                    <button className="btn-primary text-sm py-2 px-3 touch-feedback">
                      <Download className="w-4 h-4" />
                    </button>
                  </motion.div>
                ))}
              </div>
            </motion.section>
          </>
        )}
      </main>
    </div>
  )
}
