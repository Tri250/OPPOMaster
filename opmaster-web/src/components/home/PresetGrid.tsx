import { motion, AnimatePresence } from 'framer-motion';
import { Search, Filter, X, ArrowUpDown, Clock, TrendingUp, Star, ChevronRight } from 'lucide-react';
import { FilterType } from '../../data/mockPresets';
import { useAppStore, type SortType } from '../../store/useAppStore';
import PresetCard from './PresetCard';
import { EmptySearchState, EmptyFavoritesState } from '../ui/EmptyState';
import { useState, useEffect, useRef } from 'react';

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
  const sortMenuRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

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
  }, [addToSearchHistory]);

  // 点击外部关闭排序菜单
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (sortMenuRef.current && !sortMenuRef.current.contains(event.target as Node)) {
        setShowSortMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
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
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  };

  // 处理清除搜索
  const handleClearSearch = () => {
    setSearchQuery('');
    setShowSearchHistory(false);
  };

  return (
    <div className="py-8">
      <div className="container-os">
        {/* 标题区域 */}
        <div className="mb-8">
          <motion.h1 
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-3xl font-bold text-white mb-2"
          >
            哈苏大师预设
          </motion.h1>
          <motion.p 
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="text-white/60"
          >
            探索专业摄影师的调色配方，为您的照片增添哈苏质感
          </motion.p>
        </div>

        {/* 搜索与筛选区域 */}
        <div className="mb-8 space-y-4">
          {/* 搜索框 */}
          <div className="relative max-w-xl mx-auto">
            <form onSubmit={handleSearchSubmit}>
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/40" />
                <input
                  ref={searchInputRef}
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onFocus={() => searchHistory.length > 0 && setShowSearchHistory(true)}
                  placeholder="搜索预设名称、标签或相机型号..."
                  className="input pl-12 pr-12"
                />
              </div>
            </form>
            
            {searchQuery && (
              <button
                onClick={handleClearSearch}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-white/40 hover:text-white transition-colors"
                aria-label="清除搜索"
              >
                <X className="w-5 h-5" />
              </button>
            )}

            {/* 搜索历史下拉菜单 */}
            <AnimatePresence>
              {showSearchHistory && searchHistory.length > 0 && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  className="absolute top-full left-0 right-0 mt-2 bg-deep-space-light rounded-xl shadow-xl border border-white/10 z-50"
                >
                  <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
                    <span className="text-sm text-white/60">搜索历史</span>
                    <button
                      onClick={clearSearchHistory}
                      className="text-xs text-red-400 hover:text-red-300 transition-colors"
                    >
                      清除
                    </button>
                  </div>
                  <div className="p-2 max-h-48 overflow-y-auto">
                    {searchHistory.slice(0, 5).map((query, index) => (
                      <button
                        key={index}
                        onClick={() => handleHistoryClick(query)}
                        className="w-full text-left px-3 py-2 text-sm text-white/80 hover:bg-white/10 rounded-lg transition-colors flex items-center gap-2"
                      >
                        <Clock className="w-4 h-4 text-white/40" />
                        {query}
                      </button>
                    ))}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* 筛选标签与排序按钮 */}
          <div className="flex flex-wrap justify-between items-center gap-2">
            {/* 筛选标签 */}
            <div className="flex flex-wrap gap-2">
              {filterOptions.map((option) => (
                <motion.button
                  key={option.type}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setFilterType(option.type)}
                  className={`px-5 py-2 rounded-full text-sm font-medium transition-all ${
                    filterType === option.type
                      ? 'bg-hasselblad text-deep-space shadow-lg shadow-hasselblad/20'
                      : 'glass-effect text-white/70 hover:text-white'
                  }`}
                >
                  {option.label}
                </motion.button>
              ))}
            </div>

            {/* 排序按钮 */}
            <div className="relative" ref={sortMenuRef}>
              <button
                onClick={() => setShowSortMenu(!showSortMenu)}
                className="flex items-center gap-2 px-4 py-2 glass-effect rounded-full text-sm text-white/70 hover:text-white transition-colors"
              >
                <ArrowUpDown className="w-4 h-4" />
                <span>排序</span>
              </button>

              <AnimatePresence>
                {showSortMenu && (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.95, y: -10 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95, y: -10 }}
                    className="absolute top-full right-0 mt-2 bg-deep-space-light rounded-xl shadow-xl border border-white/10 z-50 min-w-40"
                  >
                    {sortOptions.map((option) => {
                      const Icon = option.icon;
                      const isActive = sortType === option.type;
                      return (
                        <button
                          key={option.type}
                          onClick={() => {
                            setSortType(option.type);
                            setShowSortMenu(false);
                          }}
                          className={`w-full flex items-center gap-2 px-4 py-3 text-sm transition-colors first:rounded-t-xl last:rounded-b-xl ${
                            isActive
                              ? 'bg-hasselblad/20 text-hasselblad'
                              : 'text-white/80 hover:bg-white/10'
                          }`}
                        >
                          <Icon className="w-4 h-4" />
                          <span>{option.label}</span>
                          {isActive && (
                            <span className="ml-auto text-xs">✓</span>
                          )}
                        </button>
                      );
                    })}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>
        </div>

        {/* 预设列表 */}
        <AnimatePresence mode="wait">
          {isLoading ? (
            <motion.div
              key="loading"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {[...Array(8)].map((_, index) => (
                  <div key={index} className="skeleton aspect-[4/5] rounded-2xl" />
                ))}
              </div>
            </motion.div>
          ) : filteredPresets.length === 0 ? (
            <motion.div
              key="empty"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              {searchQuery ? (
                <EmptySearchState query={searchQuery} onReset={handleClearSearch} />
              ) : filterType === FilterType.FAVORITES ? (
                <EmptyFavoritesState onExplore={() => setFilterType(FilterType.ALL)} />
              ) : (
                <div className="text-center py-16">
                  <p className="text-white/60">暂无预设</p>
                </div>
              )}
            </motion.div>
          ) : (
            <motion.div
              key="presets"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
            >
              {filteredPresets.map((preset, index) => (
                <motion.div
                  key={preset.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                >
                  <PresetCard preset={preset} />
                </motion.div>
              ))}
            </motion.div>
          )}
        </AnimatePresence>

        {/* 底部信息 */}
        {filteredPresets.length > 0 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="mt-8 text-center"
          >
            <p className="text-white/40 text-sm">
              显示 {filteredPresets.length} 个预设
            </p>
          </motion.div>
        )}
      </div>
    </div>
  );
}