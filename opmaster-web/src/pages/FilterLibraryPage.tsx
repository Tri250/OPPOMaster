import { motion, AnimatePresence } from 'framer-motion'
import { Search, Grid, List, Heart, Star, Clock, TrendingUp, Camera, Filter, ChevronRight, ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ColorOSFilterCard, ColorOSChip } from '../components/common/ColorOSComponents'
import PageLayout from '../components/common/PageLayout'

interface Preset {
  id: number
  name: string
  author: string
  isNew: boolean
  isHasselblad: boolean
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
  { id: 1, name: '富士胶片', author: '影像大师', isNew: true, isHasselblad: true, category: '胶片', scene: '风光', compatibleModels: ['OPPO Find X7', '一加 12'], popularity: 98, favorites: 1234, createdAt: '2026-01-15' },
  { id: 2, name: '徕卡经典', author: '光影猎人', isNew: false, isHasselblad: true, category: '复古', scene: '人像', compatibleModels: ['OPPO Find X7', 'OPPO Find X6'], popularity: 95, favorites: 987, createdAt: '2025-12-20' },
  { id: 3, name: '哈苏自然', author: '色彩玩家', isNew: true, isHasselblad: true, category: '清新', scene: '风光', compatibleModels: ['OPPO Find X7', 'realme GT5 Pro'], popularity: 92, favorites: 856, createdAt: '2026-01-10' },
  { id: 4, name: '赛博朋克', author: '未来派', isNew: false, isHasselblad: false, category: '复古', scene: '夜景', compatibleModels: ['一加 12', 'realme GT5 Pro'], popularity: 88, favorites: 743, createdAt: '2025-11-25' },
  { id: 5, name: '人像暖色', author: '摄影师阿东', isNew: true, isHasselblad: false, category: '清新', scene: '人像', compatibleModels: ['OPPO Find X7', 'OPPO Find X6'], popularity: 90, favorites: 632, createdAt: '2026-01-08' },
  { id: 6, name: '风光HDR', author: '山水之间', isNew: false, isHasselblad: true, category: '清新', scene: '风光', compatibleModels: ['OPPO Find X7', '一加 12'], popularity: 85, favorites: 521, createdAt: '2025-12-15' },
  { id: 7, name: '夜景大师', author: '夜拍达人', isNew: true, isHasselblad: true, category: '复古', scene: '夜景', compatibleModels: ['OPPO Find X7', 'realme GT5 Pro'], popularity: 93, favorites: 456, createdAt: '2026-01-12' },
  { id: 8, name: '美食鲜艳', author: '美食博主', isNew: false, isHasselblad: false, category: '清新', scene: '美食', compatibleModels: ['OPPO Find X6', '一加 12'], popularity: 82, favorites: 398, createdAt: '2025-11-30' },
  { id: 9, name: '复古胶片', author: '怀旧玩家', isNew: false, isHasselblad: false, category: '胶片', scene: '人像', compatibleModels: ['OPPO Find X7', 'OPPO Find X6'], popularity: 87, favorites: 345, createdAt: '2025-12-05' },
  { id: 10, name: '小清新', author: '少女心', isNew: true, isHasselblad: false, category: '清新', scene: '人像', compatibleModels: ['一加 12', 'realme GT5 Pro'], popularity: 89, favorites: 298, createdAt: '2026-01-05' },
  { id: 11, name: '黑白电影', author: '电影感', isNew: false, isHasselblad: true, category: '复古', scene: '人像', compatibleModels: ['OPPO Find X7', '一加 12'], popularity: 91, favorites: 256, createdAt: '2025-12-25' },
  { id: 12, name: '日系', author: '东京Style', isNew: true, isHasselblad: false, category: '清新', scene: '风光', compatibleModels: ['OPPO Find X6', 'realme GT5 Pro'], popularity: 84, favorites: 213, createdAt: '2026-01-02' }
]

const easeOppoEnter = [0.05, 0.7, 0.1, 1.0] as [number, number, number, number]

export default function FilterLibraryPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [activeCategory, setActiveCategory] = useState('全部')
  const [activeModel, setActiveModel] = useState<string | null>(null)
  const [sortBy, setSortBy] = useState('popularity')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [favorites, setFavorites] = useState<number[]>([2, 4, 6, 8])
  const [selectedFilters, setSelectedFilters] = useState<number[]>([])
  const [showFilters, setShowFilters] = useState(false)

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
    <PageLayout>
      <div className="max-w-7xl mx-auto px-4 py-8 lg:px-8 lg:py-12">
        {/* 页面头部 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <div className="flex items-center gap-4 mb-6">
            <Link to="/" className="p-2 rounded-xl hover:bg-white/10 transition-colors touch-feedback">
              <ArrowLeft className="w-5 h-5 text-text-primary" />
            </Link>
            <div>
              <h1 className="text-h1 font-bold">预设库</h1>
              <p className="text-body2 text-text-secondary">探索专业摄影师精心调校的预设参数</p>
            </div>
          </div>

          {/* Hero 区域 */}
          <div className="card-glass p-6 sm:p-8">
            <h2 className="text-h2 font-bold mb-3 gradient-text-oppo">
              专业预设库
            </h2>
            <p className="text-body1 text-text-secondary max-w-2xl">
              按风格、场景、适配机型分类，支持全文搜索和多种排序方式，让找预设变得简单
            </p>
          </div>
        </motion.div>

        {/* 搜索栏 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="mb-6"
        >
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="搜索预设名称、作者、风格、场景..."
              className="w-full pl-12 pr-4 py-4 bg-bg-secondary border border-border-default rounded-2xl text-text-primary placeholder-text-tertiary focus:outline-none focus:border-oppo-orange focus:ring-2 focus:ring-oppo-orange/20 transition-all duration-200 text-body1"
              aria-label="搜索预设"
            />
          </div>
        </motion.div>

        {/* 筛选面板 */}
        <AnimatePresence>
          {showFilters && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.3, ease: easeOppoEnter }}
              className="overflow-hidden mb-6"
            >
              <div className="space-y-4">
                {/* 风格/场景分类 */}
                <div className="card-oppo p-6">
                  <h3 className="text-body2 font-bold text-text-secondary mb-4">按风格/场景分类</h3>
                  <div className="flex flex-wrap gap-2">
                    {categories.map((cat) => (
                      <ColorOSChip
                        key={cat.name}
                        label={`${cat.name} (${cat.count})`}
                        selected={activeCategory === cat.name}
                        onClick={() => {
                          setActiveCategory(cat.name)
                          setActiveModel(null)
                        }}
                      />
                    ))}
                  </div>
                </div>

                {/* 适配机型 */}
                <div className="card-oppo p-6">
                  <h3 className="text-body2 font-bold text-text-secondary mb-4">按适配机型</h3>
                  <div className="flex flex-wrap gap-2">
                    <ColorOSChip
                      label="全部机型"
                      selected={!activeModel}
                      onClick={() => {
                        setActiveModel(null)
                        setActiveCategory('全部')
                      }}
                    />
                    {compatibleModels.map((model) => (
                      <ColorOSChip
                        key={model.name}
                        label={`${model.name} (${model.count})`}
                        selected={activeModel === model.name}
                        onClick={() => {
                          setActiveModel(model.name)
                          setActiveCategory('全部')
                        }}
                        icon={<Camera className="w-3.5 h-3.5" />}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* 工具栏 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-6 flex flex-wrap items-center justify-between gap-4"
        >
          <div className="flex items-center gap-2">
            <span className="text-body2 text-text-tertiary">排序：</span>
            {sortOptions.map((option) => (
              <ColorOSChip
                key={option.id}
                label={option.name}
                selected={sortBy === option.id}
                onClick={() => setSortBy(option.id)}
                icon={option.icon}
              />
            ))}
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-body2 font-medium transition-all duration-200 ${
                showFilters ? 'bg-oppo-orange/20 text-oppo-orange' : 'bg-bg-secondary text-text-secondary hover:text-text-primary hover:bg-bg-tertiary'
              }`}
            >
              <Filter className="w-4.5 h-4.5" />
              筛选
            </button>

            <div className="flex items-center gap-2 p-1 bg-bg-secondary rounded-2xl">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2.5 rounded-xl transition-all duration-200 ${
                  viewMode === 'grid' ? 'bg-oppo-orange text-oppo-black shadow-oppo-elevation-1' : 'text-text-tertiary hover:text-text-primary'
                }`}
                aria-label="网格视图"
              >
                <Grid className="w-5 h-5" />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2.5 rounded-xl transition-all duration-200 ${
                  viewMode === 'list' ? 'bg-oppo-orange text-oppo-black shadow-oppo-elevation-1' : 'text-text-tertiary hover:text-text-primary'
                }`}
                aria-label="列表视图"
              >
                <List className="w-5 h-5" />
              </button>
            </div>
          </div>
        </motion.div>

        {/* 已选预设 */}
        <AnimatePresence>
          {selectedFilters.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="mb-6"
            >
              <div className="card-glass p-4 flex items-center justify-between">
                <span className="text-body2 text-text-secondary">
                  已选择 <span className="text-oppo-orange font-bold">{selectedFilters.length}</span> 个预设
                </span>
                <div className="flex gap-2">
                  <button className="btn-primary text-sm py-2.5 px-4">批量应用</button>
                  <button 
                    onClick={() => setSelectedFilters([])}
                    className="btn-secondary text-sm py-2.5 px-4"
                  >
                    取消
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* 结果数量 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="mb-4"
        >
          <p className="text-body2 text-text-tertiary">
            找到 <span className="text-text-primary font-bold">{filteredPresets.length}</span> 个预设
          </p>
        </motion.div>

        {/* 预设网格/列表 */}
        {viewMode === 'grid' ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {filteredPresets.map((preset, i) => (
              <motion.div
                key={preset.id}
                initial={{ opacity: 0, scale: 0.9, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                transition={{ delay: i * 0.05, duration: 0.4, ease: easeOppoEnter }}
              >
                <ColorOSFilterCard
                  name={preset.name}
                  author={preset.author}
                  category={preset.category}
                  isNew={preset.isNew}
                  isHasselblad={preset.isHasselblad}
                  isFavorited={favorites.includes(preset.id)}
                  isSelected={selectedFilters.includes(preset.id)}
                  onClick={() => toggleSelect(preset.id)}
                  onFavorite={() => toggleFavorite(preset.id)}
                />
                <div className="mt-3 flex items-center justify-between text-caption text-text-tertiary">
                  <div className="flex items-center gap-3">
                    <div className="flex items-center gap-1">
                      <Star className="w-3.5 h-3.5 text-yellow-500 fill-yellow-500" />
                      <span>{preset.popularity}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Heart className="w-3.5 h-3.5 text-oppo-pink fill-oppo-pink" />
                      <span>{preset.favorites}</span>
                    </div>
                  </div>
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
                transition={{ delay: i * 0.05, duration: 0.4, ease: easeOppoEnter }}
                whileTap={{ scale: 0.98 }}
                onClick={() => toggleSelect(preset.id)}
                className={`card-oppo p-5 flex items-center gap-5 cursor-pointer transition-all duration-200 ${
                  selectedFilters.includes(preset.id) ? 'border-oppo-orange bg-oppo-orange/5' : ''
                }`}
              >
                <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-oppo-orange/30 to-hasselblad-orange/30 flex-shrink-0 flex items-center justify-center">
                  <div className="w-8 h-8 rounded-full border-2 border-oppo-orange/50" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1.5 flex-wrap">
                    <p className="font-bold text-text-primary truncate">{preset.name}</p>
                    {preset.isNew && (
                      <span className="tag-new text-[10px]">NEW</span>
                    )}
                    {preset.isHasselblad && (
                      <span className="tag-hncs text-[10px]">HNCS</span>
                    )}
                  </div>
                  <p className="text-text-tertiary text-body2 mb-1.5">@{preset.author}</p>
                  <div className="flex items-center gap-3">
                    <span className="tag-oppo">{preset.category}</span>
                    <span className="tag-oppo">{preset.scene}</span>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-3">
                  <div className="flex items-center gap-4 text-body2 text-text-tertiary">
                    <div className="flex items-center gap-1.5">
                      <Star className="w-4 h-4 text-yellow-500 fill-yellow-500" />
                      <span>{preset.popularity}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Heart className="w-4 h-4 text-oppo-pink fill-oppo-pink" />
                      <span>{preset.favorites}</span>
                    </div>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      toggleFavorite(preset.id)
                    }}
                    className="p-2.5 rounded-xl hover:bg-white/10 transition-colors duration-200"
                  >
                    <Heart className={`w-5 h-5 transition-colors duration-200 ${favorites.includes(preset.id) ? 'fill-oppo-pink text-oppo-pink' : 'text-text-tertiary'}`} />
                  </button>
                </div>
                <ChevronRight className="w-5 h-5 text-text-tertiary flex-shrink-0" />
              </motion.div>
            ))}
          </div>
        )}

        {/* 空状态 */}
        {filteredPresets.length === 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="card-oppo p-12 text-center"
          >
            <div className="w-20 h-20 rounded-3xl bg-white/5 flex items-center justify-center mx-auto mb-5">
              <Search className="w-10 h-10 text-text-tertiary" />
            </div>
            <h3 className="text-h2 font-bold mb-2">没有找到预设</h3>
            <p className="text-body1 text-text-secondary max-w-xs mx-auto">尝试调整搜索条件或分类</p>
          </motion.div>
        )}

        {/* 性能统计 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="mt-10"
        >
          <div className="card-elevated p-6 sm:p-8">
            <div className="grid grid-cols-3 gap-6 text-center">
              <div>
                <div className="text-number-lg font-bold gradient-text-oppo">&lt;2s</div>
                <div className="text-body2 text-text-tertiary mt-1">搜索响应时间</div>
              </div>
              <div>
                <div className="text-number-lg font-bold gradient-text-oppo">1000+</div>
                <div className="text-body2 text-text-tertiary mt-1">预设库容量</div>
              </div>
              <div>
                <div className="text-number-lg font-bold gradient-text-oppo">3</div>
                <div className="text-body2 text-text-tertiary mt-1">分类维度</div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </PageLayout>
  )
}
