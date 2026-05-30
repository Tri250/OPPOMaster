import React, { useState } from 'react';
import { Search, Filter, Star, Sparkles, ChevronDown, ChevronUp } from 'lucide-react';
import { Preset, PresetStyles, PresetScenes, ALL_STYLES, ALL_SCENES } from '../data/mockPresets';
import { usePresetSearch } from '../hooks/usePresetSearch';

interface PresetSearchFilterProps {
  presets: Preset[];
  onFilteredPresets: (presets: Preset[]) => void;
  className?: string;
}

export function PresetSearchFilter({
  presets,
  onFilteredPresets,
  className = '',
}: PresetSearchFilterProps) {
  const {
    filteredPresets,
    filterConfig,
    lastSearchTime,
    setStyle,
    setScene,
    setSearchQuery,
    setFavoriteOnly,
    setNewOnly,
    resetFilters,
    getSearchSuggestions,
  } = usePresetSearch(presets);

  const [showFilters, setShowFilters] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // 当筛选结果变化时通知父组件
  React.useEffect(() => {
    onFilteredPresets(filteredPresets);
  }, [filteredPresets, onFilteredPresets]);

  const searchSuggestions = React.useMemo(() => {
    if (filterConfig.searchQuery.length < 2) return [];
    return getSearchSuggestions(filterConfig.searchQuery);
  }, [filterConfig.searchQuery, getSearchSuggestions]);

  const handleSuggestionClick = (suggestion: string) => {
    setSearchQuery(suggestion);
    setShowSuggestions(false);
  };

  const hasActiveFilters = filterConfig.selectedStyle !== null ||
    filterConfig.selectedScene !== null ||
    filterConfig.searchQuery !== '' ||
    filterConfig.isFavoriteOnly ||
    filterConfig.isNewOnly;

  return (
    <div className={`w-full ${className}`}>
      {/* 搜索栏 */}
      <div className="relative mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="搜索预设、风格、标签..."
            value={filterConfig.searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onFocus={() => setShowSuggestions(true)}
            onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
            className="w-full pl-10 pr-4 py-3 bg-gray-900 border border-gray-700 rounded-xl text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
          />
          {filterConfig.searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white"
            >
              ✕
            </button>
          )}
        </div>

        {/* 搜索建议 */}
        {showSuggestions && searchSuggestions.length > 0 && (
          <div className="absolute z-10 w-full mt-1 bg-gray-900 border border-gray-700 rounded-lg shadow-xl overflow-hidden">
            {searchSuggestions.map((suggestion, index) => (
              <button
                key={index}
                onClick={() => handleSuggestionClick(suggestion)}
                className="w-full px-4 py-2 text-left text-gray-300 hover:bg-gray-800 hover:text-white transition-colors"
              >
                {suggestion}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* 快速筛选按钮 */}
      <div className="flex flex-wrap gap-2 mb-4">
        <button
          onClick={() => setShowFilters(!showFilters)}
          className="flex items-center gap-2 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition-colors"
        >
          <Filter className="w-4 h-4" />
          筛选
          {showFilters ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </button>

        <button
          onClick={() => setFavoriteOnly(!filterConfig.isFavoriteOnly)}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
            filterConfig.isFavoriteOnly
              ? 'bg-orange-500 text-white'
              : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
          }`}
        >
          <Star className="w-4 h-4" />
          仅收藏
        </button>

        <button
          onClick={() => setNewOnly(!filterConfig.isNewOnly)}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
            filterConfig.isNewOnly
              ? 'bg-green-500 text-white'
              : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
          }`}
        >
          <Sparkles className="w-4 h-4" />
          新品
        </button>

        {hasActiveFilters && (
          <button
            onClick={resetFilters}
            className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
          >
            重置筛选
          </button>
        )}
      </div>

      {/* 筛选面板 */}
      {showFilters && (
        <div className="mb-6 p-4 bg-gray-900 border border-gray-700 rounded-xl space-y-6">
          {/* 风格筛选 */}
          <div>
            <h3 className="text-sm font-semibold text-gray-400 mb-3 uppercase tracking-wide">
              风格
            </h3>
            <div className="flex flex-wrap gap-2">
              {ALL_STYLES.map((style) => (
                <button
                  key={style}
                  onClick={() => setStyle(style === PresetStyles.ALL ? null : style)}
                  className={`px-3 py-1.5 text-sm rounded-full transition-colors ${
                    (style === PresetStyles.ALL && !filterConfig.selectedStyle) ||
                    filterConfig.selectedStyle === style
                      ? 'bg-orange-500 text-white'
                      : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                  }`}
                >
                  {style}
                </button>
              ))}
            </div>
          </div>

          {/* 场景筛选 */}
          <div>
            <h3 className="text-sm font-semibold text-gray-400 mb-3 uppercase tracking-wide">
              场景
            </h3>
            <div className="flex flex-wrap gap-2">
              {ALL_SCENES.map((scene) => (
                <button
                  key={scene}
                  onClick={() => setScene(scene === PresetScenes.ALL ? null : scene)}
                  className={`px-3 py-1.5 text-sm rounded-full transition-colors ${
                    (scene === PresetScenes.ALL && !filterConfig.selectedScene) ||
                    filterConfig.selectedScene === scene
                      ? 'bg-orange-500 text-white'
                      : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                  }`}
                >
                  {scene}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 搜索结果统计 */}
      <div className="flex items-center justify-between text-sm text-gray-400 mb-4">
        <span>找到 {filteredPresets.length} 个预设</span>
        {process.env.NODE_ENV === 'development' && (
          <span>搜索耗时: {lastSearchTime.toFixed(2)}ms</span>
        )}
      </div>
    </div>
  );
}
