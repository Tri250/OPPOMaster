import { motion, AnimatePresence } from 'framer-motion'
import { Filter, Heart, Search, Star, Grid, List, SortAsc, ChevronRight } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ColorOSCard, ColorOSButton, ColorOSChip, ColorOSAnimations } from '../components/common/ColorOSComponents'

const categories = [
  { id: 'all', name: '全部', count: 48 },
  { id: 'portrait', name: '人像', count: 12 },
  { id: 'landscape', name: '风光', count: 15 },
  { id: 'food', name: '美食', count: 8 },
  { id: 'night', name: '夜景', count: 7 },
  { id: 'film', name: '胶片', count: 6 },
]

const filters = [
  { id: 1, name: '富士胶片', author: '色彩实验室', category: 'film', isHNCS: true, isNew: true, isFavorite: true, rating: 4.9, usage: 12345 },
  { id: 2, name: '徕卡经典', author: '光影猎人', category: 'portrait', isHNCS: true, isNew: false, isFavorite: false, rating: 4.8, usage: 9876 },
  { id: 3, name: '哈苏自然', author: '山水之间', category: 'landscape', isHNCS: true, isNew: true, isFavorite: true, rating: 4.7, usage: 8765 },
  { id: 4, name: '赛博朋克', author: '未来视觉', category: 'night', isHNCS: false, isNew: false, isFavorite: false, rating: 4.6, usage: 7654 },
  { id: 5, name: '人像暖色', author: '人像大师', category: 'portrait', isHNCS: false, isNew: true, isFavorite: true, rating: 4.8, usage: 6543 },
  { id: 6, name: '风光HDR', author: '风光摄影', category: 'landscape', isHNCS: true, isNew: false, isFavorite: true, rating: 4.7, usage: 5432 },
  { id: 7, name: '夜景大师', author: '夜行者', category: 'night', isHNCS: true, isNew: true, isFavorite: false, rating: 4.9, usage: 4321 },
  { id: 8, name: '美食鲜艳', author: '美食家', category: 'food', isHNCS: false, isNew: false, isFavorite: false, rating: 4.5, usage: 3210 },
]

export default function FilterLibraryPage() {
  const [activeCategory, setActiveCategory] = useState('all')
  const [searchQuery, setSearchQuery] = useState('')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [sortBy, setSortBy] = useState<'popular' | 'newest' | 'rating'>('popular')

  const filteredFilters = filters.filter(filter => {
    const matchesCategory = activeCategory === 'all' || filter.category === activeCategory
    const matchesSearch = filter.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          filter.author.toLowerCase().includes(searchQuery.toLowerCase())
    return matchesCategory && matchesSearch
  })

  const sortedFilters = [...filteredFilters].sort((a, b) => {
    switch (sortBy) {
      case 'popular': return b.usage - a.usage
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
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-aurora-purple to-ocean-blue flex items-center justify-center">
              <Filter className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">滤镜库</h1>
              <p className="text-text-tertiary text-sm">专业调色，触手可及</p>
            </div>
          </div>

          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="mb-6"
          >
            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
              <input
                type="text"
                placeholder="搜索滤镜..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-12 pr-4 py-3 bg-white/5 border border-white/10 rounded-2xl text-white placeholder-text-tertiary focus:outline-none focus:border-oppo-sunrise-gold/50 transition-all"
              />
            </div>
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
                <option value="popular">最热门</option>
                <option value="newest">最新</option>
                <option value="rating">评分最高</option>
              </select>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2 rounded-lg transition-colors ${
                  viewMode === 'grid' ? 'bg-oppo-sunrise-gold text-deep-space' : 'bg-white/5 text-text-secondary hover:bg-white/10'
                }`}
              >
                <Grid className="w-5 h-5" />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2 rounded-lg transition-colors ${
                  viewMode === 'list' ? 'bg-oppo-sunrise-gold text-deep-space' : 'bg-white/5 text-text-secondary hover:bg-white/10'
                }`}
              >
                <List className="w-5 h-5" />
              </button>
            </div>
          </motion.div>

          <motion.div
            variants={ColorOSAnimations.stagger}
            initial="initial"
            animate="animate"
          >
            {viewMode === 'grid' ? (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {sortedFilters.map((filter, i) => (
                  <motion.div
                    key={filter.id}
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: i * 0.05 }}
                    whileHover={{ y: -4, scale: 1.02 }}
                  >
                    <FilterGridCard filter={filter} />
                  </motion.div>
                ))}
              </div>
            ) : (
              <div className="space-y-3">
                {sortedFilters.map((filter, i) => (
                  <motion.div
                    key={filter.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.05 }}
                  >
                    <FilterListCard filter={filter} />
                  </motion.div>
                ))}
              </div>
            )}
          </motion.div>

          {sortedFilters.length === 0 && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-center py-16"
            >
              <div className="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-4">
                <Filter className="w-8 h-8 text-text-tertiary" />
              </div>
              <p className="text-text-secondary">未找到匹配的滤镜</p>
              <p className="text-text-tertiary text-sm mt-1">尝试更换关键词或分类</p>
            </motion.div>
          )}
        </motion.div>
      </div>
    </div>
  )
}

function FilterGridCard({ filter }: { filter: any }) {
  return (
    <ColorOSCard variant="default" className="overflow-hidden group cursor-pointer">
      <div className="aspect-square bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 relative">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
        
        <div className="absolute top-2 left-2 flex gap-1.5 z-10">
          {filter.isNew && (
            <span className="px-2 py-0.5 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>
          )}
          {filter.isHNCS && (
            <span className="px-2 py-0.5 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">HNCS</span>
          )}
        </div>

        <button className="absolute top-2 right-2 z-10 w-7 h-7 rounded-full bg-black/30 backdrop-blur-sm flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
          <Heart className={`w-4 h-4 ${filter.isFavorite ? 'fill-sakura-pink text-sakura-pink' : 'text-white'}`} />
        </button>

        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-12 h-12 rounded-full bg-white/10 flex items-center justify-center">
            <div className="w-6 h-6 rounded-full border-2 border-oppo-sunrise-gold/50" />
          </div>
        </div>

        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
      </div>

      <div className="p-4">
        <h3 className="text-white font-medium mb-1 truncate">{filter.name}</h3>
        <p className="text-text-tertiary text-sm truncate mb-2">@{filter.author}</p>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1">
            <Star className="w-4 h-4 fill-oppo-sunrise-gold text-oppo-sunrise-gold" />
            <span className="text-white text-sm font-medium">{filter.rating}</span>
          </div>
          <span className="text-text-tertiary text-xs">{filter.usage.toLocaleString()}</span>
        </div>
      </div>
    </ColorOSCard>
  )
}

function FilterListCard({ filter }: { filter: any }) {
  return (
    <ColorOSCard variant="default" className="p-4 flex items-center gap-4">
      <div className="w-20 h-20 rounded-xl bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex-shrink-0 relative overflow-hidden">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
            <div className="w-4 h-4 rounded-full border border-oppo-sunrise-gold/50" />
          </div>
        </div>
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-1">
          <h3 className="text-white font-medium truncate">{filter.name}</h3>
          {filter.isNew && <span className="px-1.5 py-0.5 bg-oppo-green text-deep-space text-[10px] font-bold rounded-full">NEW</span>}
          {filter.isHNCS && <span className="px-1.5 py-0.5 bg-hasselblad-pro text-deep-space text-[10px] font-bold rounded-full">HNCS</span>}
        </div>
        <p className="text-text-tertiary text-sm truncate mb-2">@{filter.author}</p>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1">
            <Star className="w-4 h-4 fill-oppo-sunrise-gold text-oppo-sunrise-gold" />
            <span className="text-white text-sm">{filter.rating}</span>
          </div>
          <span className="text-text-tertiary text-sm">{filter.usage.toLocaleString()} 使用</span>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <button className="p-2 rounded-lg text-text-secondary hover:text-white hover:bg-white/5 transition-colors">
          <Heart className={`w-5 h-5 ${filter.isFavorite ? 'fill-sakura-pink text-sakura-pink' : ''}`} />
        </button>
        <ChevronRight className="w-5 h-5 text-text-tertiary" />
      </div>
    </ColorOSCard>
  )
}
