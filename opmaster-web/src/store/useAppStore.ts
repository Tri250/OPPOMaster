import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { Preset } from '../data/mockPresets';
import { FilterType, mockPresets } from '../data/mockPresets';

interface AppState {
  presets: Preset[];
  selectedPreset: Preset | null;
  filterType: typeof FilterType[keyof typeof FilterType];
  searchQuery: string;
  favorites: string[]; // 使用数组代替Set以便序列化
  isLoading: boolean;
  toastMessage: { text: string; type: 'success' | 'info' | 'error' } | null;
  
  setSelectedPreset: (preset: Preset | null) => void;
  setFilterType: (type: typeof FilterType[keyof typeof FilterType]) => void;
  setSearchQuery: (query: string) => void;
  setPresets: (presets: Preset[]) => void;
  toggleFavorite: (presetId: string) => void;
  getFilteredPresets: () => Preset[];
  setIsLoading: (loading: boolean) => void;
  showToast: (text: string, type: 'success' | 'info' | 'error') => void;
  clearToast: () => void;
  isFavorite: (presetId: string) => boolean;
}

// 初始化收藏 - 从localStorage读取或使用默认值
const getInitialFavorites = () => {
  if (typeof window !== 'undefined') {
    const saved = localStorage.getItem('opmaster-favorites');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        // 解析失败时使用默认值
      }
    }
  }
  return ['fujifilm_film', 'fairy_tale', 'ricoh_blue', 'ricoh_negative'];
};

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      presets: mockPresets,
      selectedPreset: null,
      filterType: FilterType.ALL,
      searchQuery: '',
      favorites: getInitialFavorites(),
      isLoading: false,
      toastMessage: null,
      
      setSelectedPreset: (preset) => set({ selectedPreset: preset }),
      
      setFilterType: (type) => set({ filterType: type }),
      
      setSearchQuery: (query) => set({ searchQuery: query }),
      
      setPresets: (presets) => set({ presets }),
      
      setIsLoading: (loading) => set({ isLoading: loading }),
      
      toggleFavorite: (presetId) => {
        const { favorites, presets } = get();
        let newFavorites;
        
        if (favorites.includes(presetId)) {
          newFavorites = favorites.filter(id => id !== presetId);
        } else {
          newFavorites = [...favorites, presetId];
        }
        
        const updatedPresets = presets.map(preset => ({
          ...preset,
          isFavorite: newFavorites.includes(preset.id)
        }));
        
        set({ favorites: newFavorites, presets: updatedPresets });
        get().showToast(
          newFavorites.includes(presetId) ? '已添加到收藏' : '已取消收藏',
          'success'
        );
      },
      
      isFavorite: (presetId) => {
        return get().favorites.includes(presetId);
      },
      
      showToast: (text, type) => {
        set({ toastMessage: { text, type } });
        // 自动关闭toast
        setTimeout(() => {
          get().clearToast();
        }, 3000);
      },
      
      clearToast: () => {
        set({ toastMessage: null });
      },
      
      getFilteredPresets: () => {
        const { presets, filterType, searchQuery, favorites } = get();
        
        // 先根据收藏状态更新presets
        const presetsWithFavorites = presets.map(p => ({
          ...p,
          isFavorite: favorites.includes(p.id)
        }));
        
        let filtered = [...presetsWithFavorites];
        
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
        
        return filtered;
      },
    }),
    {
      name: 'opmaster-favorites', // localStorage key
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ 
        favorites: state.favorites // 只持久化收藏信息
      }),
    }
  )
);
