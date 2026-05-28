import { create } from 'zustand';
import type { Preset } from '../data/mockPresets';
import { FilterType, mockPresets } from '../data/mockPresets';

interface AppState {
  presets: Preset[];
  selectedPreset: Preset | null;
  filterType: typeof FilterType[keyof typeof FilterType];
  searchQuery: string;
  favorites: Set<string>;
  isLoading: boolean;
  
  setSelectedPreset: (preset: Preset | null) => void;
  setFilterType: (type: typeof FilterType[keyof typeof FilterType]) => void;
  setSearchQuery: (query: string) => void;
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
  favorites: new Set(['2', '6']),
  isLoading: false,
  
  setSelectedPreset: (preset) => set({ selectedPreset: preset }),
  
  setFilterType: (type) => set({ filterType: type }),
  
  setSearchQuery: (query) => set({ searchQuery: query }),
  
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
    const { presets, filterType, searchQuery } = get();
    
    let filtered = [...presets];
    
    // Apply filter
    switch (filterType) {
      case FilterType.FAVORITES:
        filtered = filtered.filter(p => p.isFavorite);
        break;
      case FilterType.HNCS:
        filtered = filtered.filter(p => p.cameraParams?.hasselblad_hncs);
        break;
      case FilterType.NEW:
        filtered = filtered.filter(p => p.isNew);
        break;
      case FilterType.TRENDING:
        filtered = filtered.filter((_, index) => index < 8); // 简单模拟热门
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
        (p.category && p.category.toLowerCase().includes(query))
      );
    }
    
    return filtered;
  },
}));
