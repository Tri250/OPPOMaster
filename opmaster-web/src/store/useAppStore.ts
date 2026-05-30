import { create } from 'zustand';
import type { Preset } from '../data/mockPresets';
import { FilterType, mockPresets } from '../data/mockPresets';

// 排序类型定义
export type SortType = 'latest' | 'popular' | 'rating';

interface AppState {
  presets: Preset[];
  selectedPreset: Preset | null;
  filterType: typeof FilterType[keyof typeof FilterType];
  searchQuery: string;
  sortType: SortType;
  searchHistory: string[];
  favorites: Set<string>;
  isLoading: boolean;

  setSelectedPreset: (preset: Preset | null) => void;
  setFilterType: (type: typeof FilterType[keyof typeof FilterType]) => void;
  setSearchQuery: (query: string) => void;
  setSortType: (type: SortType) => void;
  addToSearchHistory: (query: string) => void;
  clearSearchHistory: () => void;
  setPresets: (presets: Preset[]) => void;
  toggleFavorite: (presetId: string) => void;
  getFilteredPresets: () => Preset[];
  setIsLoading: (loading: boolean) => void;
}

export const useAppStore = create<AppState>((set, get) => ({
  presets: mockPresets,
  selectedPreset: null,
  filterType: FilterType.ALL,
  searchQuery: '',
  sortType: 'latest',
  searchHistory: [],
  favorites: new Set(['fujifilm_film', 'fairy_tale', 'ricoh_blue', 'ricoh_negative']),
  isLoading: false,

  setSelectedPreset: (preset) => set({ selectedPreset: preset }),

  setFilterType: (type) => set({ filterType: type }),

  setSearchQuery: (query) => set({ searchQuery: query }),

  setSortType: (type) => set({ sortType: type }),

  // PRESET-007: 添加搜索历史
  addToSearchHistory: (query) => {
    if (!query.trim()) return;
    const { searchHistory } = get();
    const newHistory = [query, ...searchHistory.filter(q => q !== query)].slice(0, 10);
    set({ searchHistory: newHistory });
    localStorage.setItem('searchHistory', JSON.stringify(newHistory));
  },

  // PRESET-007: 清除搜索历史
  clearSearchHistory: () => {
    set({ searchHistory: [] });
    localStorage.removeItem('searchHistory');
  },

  setPresets: (presets) => set({ presets }),

  setIsLoading: (loading) => set({ isLoading: loading }),

  toggleFavorite: (presetId) => {
    const { favorites, presets } = get();
    const newFavorites = new Set(favorites);

    if (newFavorites.has(presetId)) {
      newFavorites.delete(presetId);
    } else {
      newFavorites.add(presetId);
    }

    const updatedPresets = presets.map(preset => ({
      ...preset,
      isFavorite: newFavorites.has(preset.id)
    }));

    set({ favorites: newFavorites, presets: updatedPresets });
  },

  getFilteredPresets: () => {
    const { presets, filterType, searchQuery, sortType } = get();

    let filtered = [...presets];

    // Apply filter
    switch (filterType) {
      case FilterType.FAVORITES:
        filtered = filtered.filter(p => p.isFavorite);
        break;
      case FilterType.HNCS:
        filtered = filtered.filter(p => p.cameraParams?.hncs);
        break;
      case FilterType.NEW:
        filtered = filtered.filter(p => p.isNew);
        break;
      case FilterType.TRENDING:
        filtered = filtered.filter((_, index) => index < 8);
        break;
      default:
        break;
    }

    // Apply search
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(p =>
        p.name.toLowerCase().includes(query) ||
        p.deviceModel.toLowerCase().includes(query) ||
        (p.category && p.category.toLowerCase().includes(query)) ||
        (p.tags && p.tags.some(tag => tag.toLowerCase().includes(query)))
      );
    }

    // PRESET-008: 搜索结果排序
    switch (sortType) {
      case 'popular':
        // 按热门程度排序（这里使用索引作为代理）
        filtered.sort((a, b) => {
          const aIndex = presets.indexOf(a);
          const bIndex = presets.indexOf(b);
          return aIndex - bIndex;
        });
        break;
      case 'rating':
        // 按评分排序（假设有rating字段或使用其他代理）
        filtered.sort((a, b) => {
          const aScore = a.cameraParams?.hncs ? 10 : 5;
          const bScore = b.cameraParams?.hncs ? 10 : 5;
          return bScore - aScore;
        });
        break;
      case 'latest':
      default:
        // 默认按最新排序（保持原顺序）
        break;
    }

    return filtered;
  },
}));
