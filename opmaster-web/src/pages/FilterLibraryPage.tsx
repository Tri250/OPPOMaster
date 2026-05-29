import { motion } from 'framer-motion'
import { Search, Filter, Grid, List, Heart, Clock, Star, ChevronRight } from 'lucide-react'
import { useState } from 'react'
import { ColorOSFilterCard } from '../components/common/ColorOSComponents'

const categories = [
  { name: '全部', count: 48 },
  { name: '人像', count: 12 },
  { name: '风光', count: 15 },
  { name: '美食', count: 8 },
  { name: '夜景', count: 7 },
  { name: '胶片', count: 6 }
]

const filters = [
  { id: 1, name: '富士胶片', author: '影像大师', isNew: true, isHNCS: true },
  { id: 2, name: '徕卡经典', author: '光影猎人', isNew: false, isHNCS: true },
  { id: 3, name: '哈苏自然', author: '色彩玩家', isNew: true, isHNCS: true },
  { id: 4, name: '赛博朋克', author: '未来派', isNew: false, isHNCS: false },
  { id: 5, name: '人像暖色', author: '摄影师阿东', isNew: true, isHNCS: false },
  { id: 6, name: '风光HDR', author: '山水之间', isNew: false, isHNCS: true },
  { id: 7, name: '夜景大师', author: '夜拍达人', isNew: true, isHNCS: true },
  { id: 8, name: '美食鲜艳', author: '美食博主', isNew: false, isHNCS: false },
  { id: 9, name: '复古胶片', author: '怀旧玩家', isNew: false, isHNCS: false },
  { id: 10, name: '小清新', author: '少女心', isNew: true, isHNCS: false },
  { id: 11, name: '黑白电影', author: '电影感', isNew: false, isHNCS: true },
  { id: 12, name: '日系', author: '东京Style', isNew: true, isHNCS: false }
]

export default function FilterLibraryPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [activeCategory, setActiveCategory] = useState('全部')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [favorites, setFavorites] = useState<number[]>([2, 4, 6, 8])
  const [selectedFilters, setSelectedFilters] = useState<number[]>([])

  const filteredFilters = filters.filter(f => {
    const matchesSearch = f.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         f.author.toLowerCase().includes(searchQuery.toLowerCase())
    return matchesSearch
  })

  const toggleFavorite = (id: number) => {
    setFavorites(prev => 
      prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
    )
  }

  const toggleSelect = (id: number) => {
    setSelectedFilters(prev =>
      prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
    )
  }

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between gap-4">
          <h1 className="text-lg font-semibold">滤镜库</h1>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded-oppo transition-colors duration-200 touch-feedback ${
                viewMode === 'grid' ? 'bg-oppo-sunrise-gold/20 text-oppo-sunrise-gold' : 'text-text-tertiary hover:text-white'
              }`}
              aria-label="网格视图"
            >
              <Grid className="w-5 h-5" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-oppo transition-colors duration-200 touch-feedback ${
                viewMode === 'list' ? 'bg-oppo-sunrise-gold/20 text-oppo-sunrise-gold' : 'text-text-tertiary hover:text-white'
              }`}
              aria-label="列表视图"
            >
              <List className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-4 space-y-4">
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索滤镜或作者..."
            className="w-full pl-12 pr-4 py-3 bg-white/5 border border-white/10 rounded-oppo text-white placeholder-text-tertiary focus:outline-none focus:border-oppo-sunrise-gold/50 transition-colors duration-200"
            aria-label="搜索滤镜"
          />
        </div>

        <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4 scrollbar-hide">
          {categories.map((cat) => (
            <button
              key={cat.name}
              onClick={() => setActiveCategory(cat.name)}
              className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 touch-feedback ${
                activeCategory === cat.name
                  ? 'bg-oppo-sunrise-gold text-deep-space'
                  : 'bg-white/10 text-text-secondary hover:bg-white/20 hover:text-white'
              }`}
            >
              {cat.name} ({cat.count})
            </button>
          ))}
        </div>

        {selectedFilters.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="card-oppo p-4 flex items-center justify-between"
          >
            <span className="text-sm text-text-secondary">
              已选择 {selectedFilters.length} 个滤镜
            </span>
            <div className="flex gap-2">
              <button className="btn-primary text-sm py-2 touch-feedback">批量应用</button>
              <button 
                onClick={() => setSelectedFilters([])}
                className="btn-secondary text-sm py-2 touch-feedback"
              >
                取消
              </button>
            </div>
          </motion.div>
        )}

        {viewMode === 'grid' ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {filteredFilters.map((filter, i) => (
              <motion.div
                key={filter.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: i * 0.05 }}
              >
                <ColorOSFilterCard
                  name={filter.name}
                  author={filter.author}
                  isNew={filter.isNew}
                  isHasselblad={filter.isHNCS}
                  isFavorited={favorites.includes(filter.id)}
                  isSelected={selectedFilters.includes(filter.id)}
                  onClick={() => toggleSelect(filter.id)}
                />
              </motion.div>
            ))}
          </div>
        ) : (
          <div className="space-y-3">
            {filteredFilters.map((filter, i) => (
              <motion.div
                key={filter.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => toggleSelect(filter.id)}
                className={`card-oppo p-4 flex items-center gap-4 cursor-pointer touch-feedback ${
                  selectedFilters.includes(filter.id) ? 'border-oppo-green' : ''
                }`}
              >
                <div className="w-16 h-16 rounded-oppo bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex-shrink-0 flex items-center justify-center">
                  <div className="w-6 h-6 rounded-full border-2 border-oppo-sunrise-gold/50" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <p className="font-medium truncate">{filter.name}</p>
                    {filter.isNew && (
                      <span className="px-2 py-0.5 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>
                    )}
                    {filter.isHNCS && (
                      <span className="px-2 py-0.5 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">HNCS</span>
                    )}
                  </div>
                  <p className="text-text-tertiary text-sm">@{filter.author}</p>
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    toggleFavorite(filter.id)
                  }}
                  className="p-2 rounded-full hover:bg-white/10 transition-colors duration-200 touch-feedback"
                  aria-label={favorites.includes(filter.id) ? '取消收藏' : '收藏'}
                >
                  <Heart className={`w-5 h-5 ${favorites.includes(filter.id) ? 'fill-sakura-pink text-sakura-pink' : 'text-text-tertiary'}`} />
                </button>
              </motion.div>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
