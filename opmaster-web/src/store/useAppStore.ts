import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Preset } from '../data/mockPresets';
import { FilterType, mockPresets, inferCategoryFromName, PresetCategory } from '../data/mockPresets';
import { detectScene, getRecommendedPresets, type SceneDetectionResult } from '../services/sceneDetection';

// 排序类型定义
export type SortType = 'latest' | 'popular' | 'rating';

// 加密存储管理器
const SecureStorage = {
  getItem: (key: string): string | null => {
    try {
      const value = localStorage.getItem(key);
      if (value) {
        // 简单的Base64编码（Web端无法使用Android Keystore）
        const decoded = atob(value);
        return decoded;
      }
      return null;
    } catch {
      return localStorage.getItem(key);
    }
  },
  
  setItem: (key: string, value: string): void => {
    try {
      // 简单的Base64编码
      const encoded = btoa(value);
      localStorage.setItem(key, encoded);
    } catch {
      localStorage.setItem(key, value);
    }
  },
  
  removeItem: (key: string): void => {
    localStorage.removeItem(key);
  }
};

interface AppState {
  presets: Preset[];
  selectedPreset: Preset | null;
  filterType: typeof FilterType[keyof typeof FilterType];
  searchQuery: string;
  sortType: SortType;
  searchHistory: string[];
  favorites: Set<string>;
  isLoading: boolean;
  sceneDetectionResult: SceneDetectionResult | null;
  
  // Actions
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
  detectSceneBySearch: (query: string) => void;
  getRecommendedPresetsForScene: () => Preset[];
  getFavorites: () => string[];
}

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      presets: mockPresets.map(preset => ({
        ...preset,
        category: preset.category || inferCategoryFromName(preset.name) || undefined
      })),
      selectedPreset: null,
      filterType: FilterType.ALL,
      searchQuery: '',
      sortType: 'latest',
      searchHistory: [],
      favorites: new Set(['fujifilm_film', 'fairy_tale', 'ricoh_blue', 'ricoh_negative']),
      isLoading: false,
      sceneDetectionResult: null,
      
      setSelectedPreset: (preset) => set({ selectedPreset: preset }),
      
      setFilterType: (type) => set({ filterType: type }),
      
      setSearchQuery: (query) => set({ searchQuery: query }),
      
      setSortType: (type) => set({ sortType: type }),
      
      addToSearchHistory: (query) => {
        if (!query.trim()) return;
        const { searchHistory } = get();
        const newHistory = [query, ...searchHistory.filter(q => q !== query)].slice(0, 10);
        set({ searchHistory: newHistory });
        SecureStorage.setItem('searchHistory', JSON.stringify(newHistory));
      },
      
      clearSearchHistory: () => {
        set({ searchHistory: [] });
        SecureStorage.removeItem('searchHistory');
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
        
        // 加密存储收藏
        SecureStorage.setItem('favorites', JSON.stringify([...newFavorites]));
        
        set({ favorites: newFavorites, presets: updatedPresets });
      },
      
      detectSceneBySearch: (query: string) => {
        const result = detectScene(query);
        set({ sceneDetectionResult: result });
      },
      
      getRecommendedPresetsForScene: () => {
        const { sceneDetectionResult, presets } = get();
        if (!sceneDetectionResult || sceneDetectionResult.isEdgeCase) {
          return [];
        }
        return getRecommendedPresets(sceneDetectionResult, presets);
      },
      
      getFavorites: () => {
        const { favorites } = get();
        return [...favorites];
      },
      
      getFilteredPresets: () => {
        const { presets, filterType, searchQuery, sortType, sceneDetectionResult } = get();
        
        let filtered = [...presets];
        
        // 应用筛选
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
        
        // 应用搜索
        if (searchQuery) {
          const query = searchQuery.toLowerCase();
          filtered = filtered.filter(p =>
            p.name.toLowerCase().includes(query) ||
            p.deviceModel.toLowerCase().includes(query) ||
            (p.category && p.category.toLowerCase().includes(query)) ||
            (p.tags && p.tags.some(tag => tag.toLowerCase().includes(query)))
          );
        }
        
        // 应用排序
        switch (sortType) {
          case 'popular':
            filtered.sort((a, b) => {
              const aIndex = presets.indexOf(a);
              const bIndex = presets.indexOf(b);
              return aIndex - bIndex;
            });
            break;
          case 'rating':
            filtered.sort((a, b) => {
              const aScore = a.cameraParams?.hncs ? 10 : 5;
              const bScore = b.cameraParams?.hncs ? 10 : 5;
              return bScore - aScore;
            });
            break;
          case 'latest':
          default:
            break;
        }
        
        return filtered;
      },
    }),
    {
      name: 'omaster-storage',
      storage: {
        getItem: (name) => {
          const str = SecureStorage.getItem(name);
          return str ? JSON.parse(str) : null;
        },
        setItem: (name, value) => {
          SecureStorage.setItem(name, JSON.stringify(value));
        },
        removeItem: (name) => {
          SecureStorage.removeItem(name);
        },
      },
      partialize: (state) => ({
        favorites: [...state.favorites],
        searchHistory: state.searchHistory,
        themeMode: state.themeMode,
      }),
    }
  )
);

// 初始化收藏数据
const initializeFavorites = () => {
  try {
    const stored = SecureStorage.getItem('favorites');
    if (stored) {
      const favorites = JSON.parse(stored);
      useAppStore.setState({
        favorites: new Set(favorites)
      });
    }
  } catch (error) {
    console.error('Failed to initialize favorites:', error);
  }
};

// 页面加载时初始化
if (typeof window !== 'undefined') {
  initializeFavorites();
}
