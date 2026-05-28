import { motion } from 'framer-motion';
import { Search, Filter, X } from 'lucide-react';
import { FilterType } from '../../data/mockPresets';
import { useAppStore } from '../../store/useAppStore';
import PresetCard from './PresetCard';

const filterOptions = [
  { type: FilterType.ALL, label: '全部' },
  { type: FilterType.HNCS, label: 'HNCS' },
  { type: FilterType.FAVORITES, label: '收藏' },
  { type: FilterType.FIND_X, label: 'Find X' },
  { type: FilterType.RENO, label: 'Reno' }
];

export default function PresetGrid() {
  const { filterType, setFilterType, searchQuery, setSearchQuery, getFilteredPresets } = useAppStore();
  const filteredPresets = getFilteredPresets();

  return (
    <section className="py-20 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="text-center mb-12"
      >
        <h2 className="text-4xl md:text-5xl font-bold mb-4 gradient-text">
          精选预设库
        </h2>
        <p className="text-lg text-white/60 max-w-2xl mx-auto">
          探索专业摄影师精心调校的预设参数，一键应用，轻松创作
        </p>
      </motion.div>

      {/* Search & Filter */}
      <div className="mb-8 space-y-4">
        {/* Search Bar */}
        <div className="relative max-w-2xl mx-auto">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/40" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索预设名称或设备..."
            className="input pl-12 pr-12"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-white/40 hover:text-white"
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
              className={`px-6 py-2 rounded-full text-sm font-medium transition-all ${
                filterType === option.type
                  ? 'bg-hasselblad text-deep-space'
                  : 'glass-effect text-white/70 hover:text-white'
              }`}
            >
              {option.label}
            </motion.button>
          ))}
        </div>
      </div>

      {/* Presets Grid */}
      {filteredPresets.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
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
          <Filter className="w-16 h-16 text-white/20 mx-auto mb-4" />
          <p className="text-xl text-white/60">没有找到匹配的预设</p>
          <p className="text-sm text-white/40 mt-2">尝试调整筛选条件或关键词</p>
        </motion.div>
      )}
    </section>
  );
}
