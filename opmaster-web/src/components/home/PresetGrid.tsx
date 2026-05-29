import { motion } from 'framer-motion';
import { Search, Filter, X } from 'lucide-react';
import { FilterType } from '../../data/mockPresets';
import { useAppStore } from '../../store/useAppStore';
import PresetCard from './PresetCard';

const filterOptions = [
  { type: FilterType.ALL, label: '全部' },
  { type: FilterType.HNCS, label: '哈苏认证' },
  { type: FilterType.NEW, label: '最新' },
  { type: FilterType.TRENDING, label: '热门' },
  { type: FilterType.FAVORITES, label: '收藏' }
];

export default function PresetGrid() {
  const { 
    filterType, 
    setFilterType, 
    searchQuery, 
    setSearchQuery, 
    getFilteredPresets,
    isLoading
  } = useAppStore();
  const filteredPresets = getFilteredPresets();

  return (
    <section className="py-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto relative z-10">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="text-center mb-10"
      >
        <h2 className="text-3xl md:text-4xl font-bold mb-3 gradient-text-oppo">
          精选影像推荐
        </h2>
        <p className="text-base text-text-secondary max-w-xl mx-auto">
          专业摄影师精心调校的影像参数推荐 - 专为哈苏大师模式设计
        </p>
      </motion.div>

      {/* Search & Filter */}
      <div className="mb-8 space-y-4">
        {/* Search Bar */}
        <div className="relative max-w-xl mx-auto">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索预设名称、标签或相机型号..."
            className="input-oppo pl-12 pr-12"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-text-tertiary hover:text-text-primary"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Filter Tabs */}
        <div className="flex flex-wrap justify-center gap-2">
          {filterOptions.map((option) => (
            <motion.button
              key={option.type}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setFilterType(option.type)}
              className={`px-5 py-2 rounded-full text-sm font-medium transition-all ${
                filterType === option.type
                  ? 'bg-hasselblad-orange text-oppo-black'
                  : 'glass-effect text-text-secondary hover:text-text-primary'
              }`}
            >
              {option.label}
            </motion.button>
          ))}
        </div>
      </div>

      {/* Loading State */}
      {isLoading ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="text-center py-20"
        >
          <div className="w-12 h-12 border-4 border-hasselblad-orange/30 border-t-hasselblad-orange rounded-full animate-spin mx-auto mb-4" />
          <p className="text-text-secondary">加载影像数据中...</p>
        </motion.div>
      ) : filteredPresets.length > 0 ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {filteredPresets.map((preset, index) => (
            <PresetCard key={preset.id} preset={preset} index={index} />
          ))}
        </div>
      ) : (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="text-center py-20"
        >
          <Filter className="w-16 h-16 text-text-tertiary mx-auto mb-4" />
          <p className="text-xl text-text-secondary">没有找到匹配的影像</p>
          <p className="text-sm text-text-tertiary mt-2">尝试调整筛选条件</p>
        </motion.div>
      )}
    </section>
  );
}
