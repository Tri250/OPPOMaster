import { motion, AnimatePresence } from 'framer-motion'
import { ScrollText, Heart, Star, Download, Upload, Filter, ChevronRight, Check, Zap, TrendingUp } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ColorOSCard, ColorOSButton, ColorOSChip, ColorOSAnimations } from '../components/common/ColorOSComponents'

const categories = [
  { id: 'all', name: '全部', count: 48 },
  { id: 'portrait', name: '人像', count: 15 },
  { id: 'landscape', name: '风光', count: 12 },
  { id: 'night', name: '夜景', count: 10 },
  { id: 'film', name: '胶片', count: 8 },
  { id: 'custom', name: '自定义', count: 3 },
]

const masterPresets = [
  { id: 1, name: '城市夜景大师', author: '摄影阿东', category: 'night', isHNCS: true, isNew: true, isFavorite: true, rating: 4.9, downloads: 56789, params: { iso: 400, shutter: '1/250', aperture: 'f/1.8', wb: 5200 } },
  { id: 2, name: '人像柔光', author: '光影猎人', category: 'portrait', isHNCS: true, isNew: false, isFavorite: false, rating: 4.8, downloads: 45678, params: { iso: 200, shutter: '1/500', aperture: 'f/1.4', wb: 5600 } },
  { id: 3, name: '风光HDR', author: '山水之间', category: 'landscape', isHNCS: false, isNew: true, isFavorite: true, rating: 4.7, downloads: 34567, params: { iso: 100, shutter: '1/60', aperture: 'f/8', wb: 5000 } },
  { id: 4, name: '富士胶片', author: '色彩实验室', category: 'film', isHNCS: true, isNew: false, isFavorite: true, rating: 4.9, downloads: 23456, params: { iso: 200, shutter: '1/250', aperture: 'f/2.8', wb: 5400 } },
  { id: 5, name: '美食鲜艳', author: '美食家', category: 'portrait', isHNCS: false, isNew: true, isFavorite: false, rating: 4.6, downloads: 12345, params: { iso: 400, shutter: '1/125', aperture: 'f/2.0', wb: 4800 } },
  { id: 6, name: '街拍黑白', author: '街头摄影师', category: 'film', isHNCS: true, isNew: false, isFavorite: true, rating: 4.8, downloads: 9876, params: { iso: 800, shutter: '1/500', aperture: 'f/2.8', wb: 5600 } },
]

export default function MasterParamsPage() {
  const [activeCategory, setActiveCategory] = useState('all')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [sortBy, setSortBy] = useState<'popular' | 'newest' | 'rating'>('popular')

  const filteredPresets = masterPresets.filter(p => 
    activeCategory === 'all' || p.category === activeCategory
  )

  const sortedPresets = [...filteredPresets].sort((a, b) => {
    switch (sortBy) {
      case 'popular': return b.downloads - a.downloads
      case 'newest': return b.isNew ? 1 : -1
      case 'rating': return b.rating - a.rating
      default: return 0
    }
  })

  return (
    <div className="min-h-screen bg-deep-space pb-20">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-72 h-72 top-1/4 -left-36 animate-float" />
        <div className="orb-oppo orb-2 w-56 h-56 bottom-1/4 -right-28 animate-float" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative max-w-7xl mx-auto px-4 py-8">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
        >
          <div className="flex items-center gap-3 mb-6">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-hasselblad-pro to-oppo-sunrise-gold flex items-center justify-center">
              <ScrollText className="w-6 h-6 text-deep-space" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">大师参数库</h1>
              <p className="text-text-tertiary text-sm">专业摄影师的智慧结晶</p>
            </div>
          </div>

          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8"
          >
            <StatCard icon={Zap} title="HNCS认证" value="24" subtitle="哈苏自然色彩" />
            <StatCard icon={TrendingUp} title="总下载" value="234.5K" subtitle="热门预设" />
            <StatCard icon={Star} title="平均分" value="4.8" subtitle="用户评分" />
            <StatCard icon={Heart} title="收藏" value="12.3K" subtitle="用户喜爱" />
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="flex flex-wrap gap-2 mb-6"
          >
            {categories.map((cat) => (
              <ColorOSChip
                key={cat.id}
                label={`${cat.name} (${cat.count})`}
                selected={activeCategory === cat.id}
                onClick={() => setActiveCategory(cat.id)}
              />
            ))}
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="flex items-center justify-between mb-6"
          >
            <div className="flex items-center gap-2">
              <span className="text-text-secondary text-sm">排序:</span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="bg-white/5 border border-white/10 rounded-lg text-white text-sm px-3 py-2 focus:outline-none focus:border-oppo-sunrise-gold/50"
              >
                <option value="popular">下载最多</option>
                <option value="newest">最新</option>
                <option value="rating">评分最高</option>
              </select>
            </div>

            <ColorOSButton variant="secondary" size="sm" icon={<Upload className="w-4 h-4" />}>
              导入
            </ColorOSButton>
          </motion.div>

          <motion.div
            variants={ColorOSAnimations.stagger}
            initial="initial"
            animate="animate"
          >
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
              {sortedPresets.map((preset, i) => (
                <motion.div
                  key={preset.id}
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: i * 0.05 }}
                  whileHover={{ y: -4, scale: 1.02 }}
                >
                  <MasterParamCard preset={preset} />
                </motion.div>
              ))}
            </div>
          </motion.div>
        </motion.div>
      </div>
    </div>
  )
}

function StatCard({ icon: Icon, title, value, subtitle }: { icon: any; title: string; value: string; subtitle: string }) {
  return (
    <ColorOSCard variant="default" className="p-5">
      <div className="flex items-center gap-3 mb-3">
        <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center">
          <Icon className="w-5 h-5 text-oppo-sunrise-gold" />
        </div>
        <p className="text-text-secondary text-sm">{title}</p>
      </div>
      <p className="text-2xl font-bold text-white mb-1">{value}</p>
      <p className="text-text-tertiary text-sm">{subtitle}</p>
    </ColorOSCard>
  )
}

function MasterParamCard({ preset }: { preset: any }) {
  return (
    <ColorOSCard variant="default" className="overflow-hidden group cursor-pointer">
      <div className="aspect-[4/3] bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 relative">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
        
        <div className="absolute top-3 left-3 flex gap-1.5 z-10">
          {preset.isNew && <span className="px-2 py-0.5 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>}
          {preset.isHNCS && <span className="px-2 py-0.5 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">HNCS</span>}
        </div>

        <div className="absolute top-3 right-3 flex gap-2 z-10 opacity-0 group-hover:opacity-100 transition-opacity">
          <button className="w-8 h-8 rounded-full bg-black/30 backdrop-blur-sm flex items-center justify-center">
            <Heart className={`w-4 h-4 ${preset.isFavorite ? 'fill-sakura-pink text-sakura-pink' : 'text-white'}`} />
          </button>
          <button className="w-8 h-8 rounded-full bg-black/30 backdrop-blur-sm flex items-center justify-center">
            <Download className="w-4 h-4 text-white" />
          </button>
        </div>

        <div className="absolute bottom-3 left-3 right-3 z-10">
          <div className="flex flex-wrap gap-2">
            {Object.entries(preset.params).map(([key, value]) => (
              <span key={key} className="px-2 py-1 bg-black/30 backdrop-blur-sm rounded-lg text-white text-xs font-mono">
                {key.toUpperCase()}: {value}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className="p-5">
        <h3 className="text-white font-semibold mb-1">{preset.name}</h3>
        <p className="text-text-tertiary text-sm mb-3">@{preset.author}</p>
        
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Star className="w-4 h-4 fill-oppo-sunrise-gold text-oppo-sunrise-gold" />
            <span className="text-white font-medium">{preset.rating}</span>
            <span className="text-text-tertiary text-sm">· {preset.downloads.toLocaleString()}</span>
          </div>
          <ColorOSButton variant="primary" size="sm" icon={<Check className="w-4 h-4" />}>
            应用
          </ColorOSButton>
        </div>
      </div>
    </ColorOSCard>
  )
}
