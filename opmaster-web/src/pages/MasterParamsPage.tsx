import { motion } from 'framer-motion'
import { ScrollText, Star, Heart, Download, Share2, Check } from 'lucide-react'
import { useState } from 'react'

const masterPresets = [
  { 
    id: 1, 
    name: '城市夜景大师', 
    author: '摄影阿东',
    rating: 4.9,
    downloads: 12853,
    isHNCS: true,
    isNew: true,
    category: '夜景',
    description: '专为城市夜景设计的专业参数，保留暗部细节，增强灯光层次感',
    params: { brightness: 0.8, contrast: 1.2, saturation: 1.1, shadows: -20, highlights: 15 }
  },
  { 
    id: 2, 
    name: '人像柔光', 
    author: '光影猎人',
    rating: 4.8,
    downloads: 8921,
    isHNCS: true,
    isNew: false,
    category: '人像',
    description: '优化人像肤色，增加柔和光效，让皮肤更加通透自然',
    params: { brightness: 0.5, contrast: 0.9, saturation: 1.05, shadows: 10, highlights: -5 }
  },
  { 
    id: 3, 
    name: '风光HDR', 
    author: '山水之间',
    rating: 4.7,
    downloads: 6543,
    isHNCS: false,
    isNew: true,
    category: '风光',
    description: '高动态范围优化，增强天空和地面的细节表现',
    params: { brightness: 0.3, contrast: 1.3, saturation: 1.15, shadows: 15, highlights: -10 }
  },
  { 
    id: 4, 
    name: '富士胶片', 
    author: '色彩实验室',
    rating: 4.9,
    downloads: 15632,
    isHNCS: true,
    isNew: false,
    category: '胶片',
    description: '模拟经典富士胶片色彩，浓郁而不失真',
    params: { brightness: 0.2, contrast: 1.1, saturation: 1.2, shadows: -10, highlights: 5 }
  },
  { 
    id: 5, 
    name: '美食鲜艳', 
    author: '美食博主',
    rating: 4.6,
    downloads: 4521,
    isHNCS: false,
    isNew: true,
    category: '美食',
    description: '增强食物的色彩饱和度，让美食更加诱人',
    params: { brightness: 0.4, contrast: 1.15, saturation: 1.3, shadows: 5, highlights: 0 }
  },
  { 
    id: 6, 
    name: '黑白电影', 
    author: '电影感',
    rating: 4.8,
    downloads: 7892,
    isHNCS: true,
    isNew: false,
    category: '胶片',
    description: '经典黑白电影质感，高对比度，强氛围感',
    params: { brightness: 0.1, contrast: 1.4, saturation: 0, shadows: -15, highlights: 20 }
  }
]

const categories = ['全部', '人像', '风光', '夜景', '美食', '胶片']

export default function MasterParamsPage() {
  const [activeCategory, setActiveCategory] = useState('全部')
  const [favorites, setFavorites] = useState<number[]>([1, 4])
  const [appliedParams, setAppliedParams] = useState<number[]>([])

  const filteredPresets = activeCategory === '全部' 
    ? masterPresets 
    : masterPresets.filter(p => p.category === activeCategory)

  const toggleFavorite = (id: number) => {
    setFavorites(prev => 
      prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
    )
  }

  const applyParam = (id: number) => {
    setAppliedParams(prev => 
      prev.includes(id) ? prev : [...prev, id]
    )
    setTimeout(() => {
      setAppliedParams(prev => prev.filter(p => p !== id))
    }, 2000)
  }

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">大师参数库</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-4 space-y-6">
        <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4 scrollbar-hide">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 touch-feedback ${
                activeCategory === cat
                  ? 'bg-oppo-sunrise-gold text-deep-space'
                  : 'bg-white/10 text-text-secondary hover:bg-white/20 hover:text-white'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        <div className="space-y-4">
          {filteredPresets.map((preset, i) => (
            <motion.div
              key={preset.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
              className="card-oppo overflow-hidden"
            >
              <div className="p-4">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="text-lg font-semibold">{preset.name}</h3>
                      {preset.isHNCS && (
                        <span className="px-2 py-0.5 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">HNCS</span>
                      )}
                      {preset.isNew && (
                        <span className="px-2 py-0.5 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>
                      )}
                    </div>
                    <p className="text-text-tertiary text-sm">@{preset.author}</p>
                  </div>
                  <button
                    onClick={() => toggleFavorite(preset.id)}
                    className="p-2 rounded-full hover:bg-white/10 transition-colors duration-200 touch-feedback"
                    aria-label={favorites.includes(preset.id) ? '取消收藏' : '收藏'}
                  >
                    <Heart className={`w-5 h-5 ${favorites.includes(preset.id) ? 'fill-sakura-pink text-sakura-pink' : 'text-text-tertiary'}`} />
                  </button>
                </div>

                <p className="text-text-secondary text-sm mb-4">{preset.description}</p>

                <div className="flex items-center gap-4 mb-4 text-sm text-text-tertiary">
                  <div className="flex items-center gap-1">
                    <Star className="w-4 h-4 fill-oppo-sunrise-gold text-oppo-sunrise-gold" />
                    <span>{preset.rating}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Download className="w-4 h-4" />
                    <span>{preset.downloads.toLocaleString()}</span>
                  </div>
                  <span className="px-2 py-0.5 bg-white/10 rounded-full text-xs">{preset.category}</span>
                </div>

                <div className="grid grid-cols-5 gap-2 mb-4">
                  {Object.entries(preset.params).map(([key, value]) => (
                    <div key={key} className="text-center p-2 bg-white/5 rounded-oppo">
                      <p className="text-text-tertiary text-xs capitalize">{key}</p>
                      <p className="text-white font-medium text-sm">{value}</p>
                    </div>
                  ))}
                </div>

                <div className="flex gap-3">
                  <button 
                    onClick={() => applyParam(preset.id)}
                    className={`flex-1 btn-primary text-sm py-2.5 flex items-center justify-center gap-2 touch-feedback ${
                      appliedParams.includes(preset.id) ? 'bg-oppo-green' : ''
                    }`}
                  >
                    {appliedParams.includes(preset.id) ? (
                      <>
                        <Check className="w-4 h-4" />
                        已应用
                      </>
                    ) : (
                      <>
                        <ScrollText className="w-4 h-4" />
                        一键应用
                      </>
                    )}
                  </button>
                  <button className="btn-secondary text-sm py-2.5 px-4 touch-feedback">
                    <Share2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </main>
    </div>
  )
}
