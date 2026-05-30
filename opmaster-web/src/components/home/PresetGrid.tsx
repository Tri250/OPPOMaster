import { motion } from 'framer-motion';
import { Search, Filter, X, ArrowUpDown, Clock, TrendingUp, Star } from 'lucide-react';
import { FilterType } from '../../data/mockPresets';
import { useAppStore, type SortType } from '../../store/useAppStore';
import PresetCard from './PresetCard';
import { useState, useEffect } from 'react';

const filterOptions = [
  { type: FilterType.ALL, label: '全部' },
  { type: FilterType.HNCS, label: '哈苏认证' },
  { type: FilterType.NEW, label: '最新' },
  { type: FilterType.TRENDING, label: '热门' },
  { type: FilterType.FAVORITES, label: '收藏' }
];

const sortOptions = [
  { type: 'latest' as SortType, label: '最新', icon: Clock },
  { type: 'popular' as SortType, label: '最热', icon: TrendingUp },
  { type: 'rating' as SortType, label: '评分', icon: Star }
];

export default function PresetGrid() {
  const {
    filterType,
    setFilterType,
    searchQuery,
    setSearchQuery,
    sortType,
    setSortType,
    addToSearchHistory,
    searchHistory,
    clearSearchHistory,
    getFilteredPresets,
    isLoading
  } = useAppStore();
  const filteredPresets = getFilteredPresets();
  const [showSortMenu, setShowSortMenu] = useState(false);
  const [showSearchHistory, setShowSearchHistory] = useState(false);

  // 加载搜索历史
  useEffect(() => {
    const saved = localStorage.getItem('searchHistory');
    if (saved) {
      try {
        const history = JSON.parse(saved);
        if (Array.isArray(history)) {
          // 更新store中的历史
          history.forEach(query => addToSearchHistory(query));
        }
      } catch (e) {
        console.error('Failed to load search history:', e);
      }
    }
  }, []);

  // 处理搜索提交
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      addToSearchHistory(searchQuery);
      setShowSearchHistory(false);
    }
  };

  // 处理历史项点击
  const handleHistoryClick = (query: string) => {
    setSearchQuery(query);
    setShowSearchHistory(false);
  };

  return (
    <section className="py-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        className="text-center mb-10"
      >
        <h2 className="text-3xl md:text-4xl font-bold mb-3 gradient-text">
          精选影像推荐
        </h2>
        <p className="text-base text-white/60 max-w-xl mx-auto">
          专业摄影师精心调校的影像参数推荐 - 专为哈苏大师模式设计
        </p>
      </motion.div>

      {/* Search & Filter */}
      <div className="mb-8 space-y-4">
        {/* Search Bar */}
        <div className="relative max-w-xl mx-auto">
          <form onSubmit={handleSearchSubmit}>
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/40" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onFocus={() => setShowSearchHistory(true)}
              onBlur={() => setTimeout(() => setShowSearchHistory(false), 200)}
              placeholder="搜索预设名称、标签或相机型号..."
              className="input pl-12 pr-12"
            />
          </form>
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-white/40 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
          )}

          {/* Search History Dropdown (PRESET-007) */}
          {showSearchHistory && searchHistory.length > 0 && (
            <div className="absolute top-full left-0 right-0 mt-2 bg-slate-800 rounded-xl shadow-xl border border-slate-700 z-50">
              <div className="flex items-center justify-between p-3 border-b border-slate-700">
                <span className="text-sm text-slate-400">搜索历史</span>
                <button
                  onClick={clearSearchHistory}
                  className="text-xs text-red-400 hover:text-red-300"
                >
                  清除
                </button>
              </div>
              <div className="p-2 max-h-48 overflow-y-auto">
                {searchHistory.slice(0, 5).map((query, index) => (
                  <button
                    key={index}
                    onClick={() => handleHistoryClick(query)}
                    className="w-full text-left px-3 py-2 text-sm text-white/80 hover:bg-slate-700 rounded-lg transition-colors"
                  >
                    {query}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Filter Tabs & Sort Button */}
        <div className="flex flex-wrap justify-between items-center gap-2">
          {/* Filter Tabs */}
          <div className="flex flex-wrap gap-2">
            {filterOptions.map((option) => (
              <motion.button
                key={option.type}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setFilterType(option.type)}
                className={`px-5 py-2 rounded-full text-sm font-medium transition-all ${
                  filterType === option.type
                    ? 'bg-hasselblad text-deep-space'
                    : 'glass-effect text-white/70 hover:text-white'
                }`}
              >
                {option.label}
              </motion.button>
            ))}
          </div>

          {/* Sort Button (PRESET-008) */}
          <div className="relative">
            <button
              onClick={() => setShowSortMenu(!showSortMenu)}
              className="flex items-center gap-2 px-4 py-2 glass-effect rounded-full text-sm text-white/70 hover:text-white transition-colors"
            >
              <ArrowUpDown className="w-4 h-4" />
              <span>排序</span>
            </button>

            {showSortMenu && (
              <div className="absolute top-full right-0 mt-2 bg-slate-800 rounded-xl shadow-xl border border-slate-700 z-50 min-w-36">
                {sortOptions.map((option) => {
                  const Icon = option.icon;
                  return (
                    <button
                      key={option.type}
                      onClick={() => {
                        setSortType(option.type);
                        setShowSortMenu(false);
                      }}
                      className={`w-full flex items-center gap-2 px-4 py-3 text-sm transition-colors first:rounded-t-xl last:rounded-b-xl ${
                        sortType === option.type
                          ? 'bg-hasselblad/20 text-hasselblad'
                          : 'text-white/80 hover:bg-slate-700'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                      <span>{option.label}</span>
                      {sortType === option.type && (
                        <span className="ml-auto text-xs">✓</span>
                      )}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Loading State */}
      {isLoading ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="text-center py-20"
        >
          <div className="w-12 h-12 border-4 border-hasselblad/30 border-t-hasselblad rounded-full animate-spin mx-auto mb-4" />
          <p className="text-white/60">加载影像数据中...</p>
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
          <Filter className="w-16 h-16 text-white/20 mx-auto mb-4" />
          <p className="text-xl text-white/60">没有找到匹配的影像</p>
          <p className="text-sm text-white/40 mt-2">尝试调整筛选条件</p>
        </motion.div>
      )}
    </section>
  );
}
