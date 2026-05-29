import { motion } from 'framer-motion'
import { Search, Grid, List, Heart, Star, Clock, TrendingUp, Camera } from 'lucide-react'
import { useState } from 'react'
import { ColorOSFilterCard } from '../components/common/ColorOSComponents'

interface Preset {
  id: number
  name: string
  author: string
  isNew: boolean
  isHNCS: boolean
  category: string
  scene: string
  compatibleModels: string[]
  popularity: number
  favorites: number
  createdAt: string
}

const categories = [
  { name: '全部', count: 48, type: 'all' },
  { name: '胶片', count: 12, type: 'style' },
  { name: '复古', count: 15, type: 'style' },
  { name: '清新', count: 8, type: 'style' },
  { name: '人像', count: 12, type: 'scene' },
  { name: '风光', count: 15, type: 'scene' },
  { name: '美食', count: 8, type: 'scene' },
  { name: '夜景', count: 7, type: 'scene' }
]

const compatibleModels = [
  { name: 'OPPO Find X7', count: 25 },
  { name: 'OPPO Find X6', count: 20 },
  { name: '一加 12', count: 18 },
  { name: 'realme GT5 Pro', count: 15 }
]

const sortOptions = [
  { id: 'popularity', name: '热度', icon: <TrendingUp className="w-4 h-4" /> },
  { id: 'favorites', name: '收藏', icon: <Heart className="w-4 h-4" /> },
  { id: 'newest', name: '最新', icon: <Clock className="w-4 h-4" /> }
]

const presets: Preset[] = [
  { id: 1, name: '富士胶片', author: '影像大师', isNew: true, isHNCS: true, category: '胶片', scene: '风光', compatibleModels: ['OPPO Find X7', '一加 12'], popularity: 98, favorites: 1234, createdAt: '2026-01-15' },
  { id: 2, name: '徕卡经典', author: '光影猎人', isNew: false, isHNCS: true, category: '复古', scene: '人像', compatibleModels: ['OPPO Find X7', 'OPPO Find X6'], popularity: 95, favorites: 987, createdAt: '2025-12-20' },
  { id: 3, name: '哈苏自然', author: '色彩玩家', isNew: true, isHNCS: true, category: '清新', scene: '风光', compatibleModels: ['OPPO Find X7', 'realme GT5 Pro'], popularity: 92, favorites: 856, createdAt: '2026-01-10' },
  { id: 4, name: '赛博朋克', author: '未来派', isNew: false, isHNCS: false, category: '复古', scene: '夜景', compatibleModels: ['一加 12', 'realme GT5 Pro'], popularity: 88, favorites: 743, createdAt: '2025-11-25' },
  { id: 5, name: '人像暖色', author: '摄影师阿东', isNew: true, isHNCS: false, category: '清新', scene: '人像', compatibleModels: ['OPPO Find X7', 'OPPO Find X6'], popularity: 90, favorites: 632, createdAt: '2026-01-08' },
  { id: 6, name: '风光HDR', author: '山水之间', isNew: false, isHNCS: true, category: '清新', scene: '风光', compatibleModels: ['OPPO Find X7', '一加 12'], popularity: 85, favorites: 521, createdAt: '2025-12-15' },
  { id: 7, name: '夜景大师', author: '夜拍达人', isNew: true, isHNCS: true, category: '复古', scene: '夜景', compatibleModels: ['OPPO Find X7', 'realme GT5 Pro'], popularity: 93, favorites: 456, createdAt: '2026-01-12' },
  { id: 8, name: '美食鲜艳', author: '美食博主', isNew: false, isHNCS: false, category: '清新', scene: '美食', compatibleModels: ['OPPO Find X6', '一加 12'], popularity: 82, favorites: 398, createdAt: '2025-11-30' },
  { id: 9, name: '复古胶片', author: '怀旧玩家', isNew: false, isHNCS: false, category: '胶片', scene: '人像', compatibleModels: ['OPPO Find X7', 'OPPO Find X6'], popularity: 87, favorites: 345, createdAt: '2025-12-05' },
  { id: 10, name: '小清新', author: '少女心', isNew: true, isHNCS: false, category: '清新', scene: '人像', compatibleModels: ['一加 12', 'realme GT5 Pro'], popularity: 89, favorites: 298, createdAt: '2026-01-05' },
  { id: 11, name: '黑白电影', author: '电影感', isNew: false, isHNCS: true, category: '复古', scene: '人像', compatibleModels: ['OPPO Find X7', '一加 12'], popularity: 91, favorites: 256, createdAt: '2025-12-25' },
  { id: 12, name: '日系', author: '东京Style', isNew: true, isHNCS: false, category: '清新', scene: '风光', compatibleModels: ['OPPO Find X6', 'realme GT5 Pro'], popularity: 84, favorites: 213, createdAt: '2026-01-02' }
]

export default function FilterLibraryPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [activeCategory, setActiveCategory] = useState('全部')
  const [activeModel, setActiveModel] = useState<string | null>(null)
  const [sortBy, setSortBy] = useState('popularity')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [favorites, setFavorites] = useState<number[]>([2, 4, 6, 8])
  const [selectedFilters, setSelectedFilters] = useState<number[]>([])

  const filteredPresets = presets.filter(p => {
    const matchesSearch = searchQuery === '' || 
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.author.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.category.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.scene.toLowerCase().includes(searchQuery.toLowerCase())
    
    const matchesCategory = activeCategory === '全部' || 
      p.category === activeCategory || 
      p.scene === activeCategory
    
    const matchesModel = !activeModel || p.compatibleModels.includes(activeModel)
    
    return matchesSearch && matchesCategory && matchesModel
  }).sort((a, b) => {
    if (sortBy === 'popularity') return b.popularity - a.popularity
    if (sortBy === 'favorites') return b.favorites - a.favorites
    if (sortBy === 'newest') return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    return 0
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
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-8"
        >
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text-oppo">
            预设库
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            按风格、场景、适配机型分类，支持全文搜索和多种排序方式，让找预设变得简单
          </p>
        </motion.div>

        {/* Search */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="mb-6"
        >
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/40" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="搜索预设名称、作者、风格、场景..."
              className="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:border-oppo-orange/50 transition-colors duration-200 text-lg"
              aria-label="搜索预设"
            />
          </div>
        </motion.div>

        {/* Categories */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-6"
        >
          <div className="card p-6">
            <h3 className="text-sm font-bold text-white/60 mb-4">按风格/场景分类</h3>
            <div className="flex flex-wrap gap-2">
              {categories.map((cat) => (
                <button
                  key={cat.name}
                  onClick={() => {
                    setActiveCategory(cat.name)
                    setActiveModel(null)
                  }}
                  className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 ${
                    activeCategory === cat.name
                      ? 'bg-oppo-orange text-oppo-black'
                      : 'bg-white/10 text-white/60 hover:bg-white/20 hover:text-white'
                  }`}
                >
                  {cat.name} ({cat.count})
                </button>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Compatible Models */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="mb-6"
        >
          <div className="card p-6">
            <h3 className="text-sm font-bold text-white/60 mb-4">按适配机型</h3>
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => {
                  setActiveModel(null)
                  setActiveCategory('全部')
                }}
                className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 ${
                  !activeModel
                    ? 'bg-oppo-orange text-oppo-black'
                    : 'bg-white/10 text-white/60 hover:bg-white/20 hover:text-white'
                }`}
              >
                全部机型
              </button>
              {compatibleModels.map((model) => (
                <button
                  key={model.name}
                  onClick={() => {
                    setActiveModel(model.name)
                    setActiveCategory('全部')
                  }}
                  className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 flex items-center gap-2 ${
                    activeModel === model.name
                      ? 'bg-oppo-orange text-oppo-black'
                      : 'bg-white/10 text-white/60 hover:bg-white/20 hover:text-white'
                  }`}
                >
                  <Camera className="w-3.5 h-3.5" />
                  {model.name} ({model.count})
                </button>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Controls */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="mb-6 flex flex-wrap items-center justify-between gap-4"
        >
          <div className="flex items-center gap-2">
            <span className="text-sm text-white/60">排序：</span>
            {sortOptions.map((option) => (
              <button
                key={option.id}
                onClick={() => setSortBy(option.id)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all duration-200 flex items-center gap-1.5 ${
                  sortBy === option.id
                    ? 'bg-oppo-orange/20 text-oppo-orange'
                    : 'bg-white/10 text-white/60 hover:bg-white/20 hover:text-white'
                }`}
              >
                {option.icon}
                {option.name}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded-lg transition-colors duration-200 ${
                viewMode === 'grid' ? 'bg-oppo-orange/20 text-oppo-orange' : 'text-white/40 hover:text-white'
              }`}
              aria-label="网格视图"
            >
              <Grid className="w-5 h-5" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-lg transition-colors duration-200 ${
                viewMode === 'list' ? 'bg-oppo-orange/20 text-oppo-orange' : 'text-white/40 hover:text-white'
              }`}
              aria-label="列表视图"
            >
              <List className="w-5 h-5" />
            </button>
          </div>
        </motion.div>

        {/* Selected Filters */}
        {selectedFilters.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-6"
          >
            <div className="card p-4 flex items-center justify-between">
              <span className="text-sm text-white/60">
                已选择 {selectedFilters.length} 个预设
              </span>
              <div className="flex gap-2">
                <button className="btn-primary text-sm py-2 px-4">批量应用</button>
                <button 
                  onClick={() => setSelectedFilters([])}
                  className="btn-secondary text-sm py-2 px-4"
                >
                  取消
                </button>
              </div>
            </div>
          </motion.div>
        )}

        {/* Results Count */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="mb-4"
        >
          <p className="text-sm text-white/60">
            找到 {filteredPresets.length} 个预设
          </p>
        </motion.div>

        {/* Presets Grid/List */}
        {viewMode === 'grid' ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {filteredPresets.map((preset, i) => (
              <motion.div
                key={preset.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: i * 0.05 }}
              >
                <ColorOSFilterCard
                  name={preset.name}
                  author={preset.author}
                  isNew={preset.isNew}
                  isHasselblad={preset.isHNCS}
                  isFavorited={favorites.includes(preset.id)}
                  isSelected={selectedFilters.includes(preset.id)}
                  onClick={() => toggleSelect(preset.id)}
                />
                {/* Additional info for grid mode */}
                <div className="mt-2 flex items-center justify-between text-xs text-white/40">
                  <div className="flex items-center gap-2">
                    <div className="flex items-center gap-1">
                      <Star className="w-3 h-3 text-yellow-500" />
                      <span>{preset.popularity}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Heart className="w-3 h-3 text-pink-500" />
                      <span>{preset.favorites}</span>
                    </div>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      toggleFavorite(preset.id)
                    }}
                    className="p-1"
                  >
                    <Heart className={`w-4 h-4 ${favorites.includes(preset.id) ? 'fill-pink-500 text-pink-500' : ''}`} />
                  </button>
                </div>
              </motion.div>
            ))}
          </div>
        ) : (
          <div className="space-y-3">
            {filteredPresets.map((preset, i) => (
              <motion.div
                key={preset.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => toggleSelect(preset.id)}
                className={`card p-4 flex items-center gap-4 cursor-pointer ${
                  selectedFilters.includes(preset.id) ? 'border-oppo-orange' : ''
                }`}
              >
                <div className="w-20 h-20 rounded-xl bg-gradient-to-br from-oppo-orange/30 to-hasselblad-orange/30 flex-shrink-0 flex items-center justify-center">
                  <div className="w-8 h-8 rounded-full border-2 border-oppo-orange/50" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1 flex-wrap">
                    <p className="font-bold truncate">{preset.name}</p>
                    {preset.isNew && (
                      <span className="px-2 py-0.5 bg-oppo-green text-oppo-black text-xs font-bold rounded-full">NEW</span>
                    )}
                    {preset.isHNCS && (
                      <span className="px-2 py-0.5 bg-hasselblad-orange text-oppo-black text-xs font-bold rounded-full">HNCS</span>
                    )}
                  </div>
                  <p className="text-white/40 text-sm mb-1">@{preset.author}</p>
                  <div className="flex items-center gap-3 text-xs text-white/40">
                    <span className="bg-white/10 px-2 py-0.5 rounded">{preset.category}</span>
                    <span className="bg-white/10 px-2 py-0.5 rounded">{preset.scene}</span>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <div className="flex items-center gap-3 text-sm text-white/40">
                    <div className="flex items-center gap-1">
                      <Star className="w-4 h-4 text-yellow-500" />
                      <span>{preset.popularity}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Heart className="w-4 h-4 text-pink-500" />
                      <span>{preset.favorites}</span>
                    </div>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      toggleFavorite(preset.id)
                    }}
                    className="p-2 rounded-full hover:bg-white/10 transition-colors duration-200"
                  >
                    <Heart className={`w-5 h-5 ${favorites.includes(preset.id) ? 'fill-pink-500 text-pink-500' : ''}`} />
                  </button>
                </div>
              </motion.div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {filteredPresets.length === 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="card p-12 text-center"
          >
            <Search className="w-16 h-16 text-white/20 mx-auto mb-4" />
            <h3 className="text-lg font-bold mb-2">没有找到预设</h3>
            <p className="text-white/40">尝试调整搜索条件或分类</p>
          </motion.div>
        )}

        {/* Performance Stats */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
          className="mt-8"
        >
          <div className="card p-6">
            <div className="grid md:grid-cols-3 gap-6 text-center">
              <div>
                <div className="text-3xl font-bold text-oppo-orange">&lt;2s</div>
                <div className="text-white/40 text-sm mt-1">搜索响应时间</div>
              </div>
              <div>
                <div className="text-3xl font-bold text-oppo-orange">1000+</div>
                <div className="text-white/40 text-sm mt-1">预设库容量</div>
              </div>
              <div>
                <div className="text-3xl font-bold text-oppo-orange">3</div>
                <div className="text-white/40 text-sm mt-1">分类维度</div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
