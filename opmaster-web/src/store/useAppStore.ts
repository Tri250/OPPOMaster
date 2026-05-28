import { create } from 'zustand';
import type { Preset } from '../data/mockPresets';
import { FilterType, mockPresets } from '../data/mockPresets';

interface AppState {
  presets: Preset[];
  selectedPreset: Preset | null;
  filterType: typeof FilterType[keyof typeof FilterType];
  searchQuery: string;
  favorites: Set<string>;
  
  setSelectedPreset: (preset: Preset | null) => void;
  setFilterType: (type: typeof FilterType[keyof typeof FilterType]) => void;
  setSearchQuery: (query: string) => void;
  toggleFavorite: (presetId: string) => void;
  getFilteredPresets: () => Preset[];
}

export const useAppStore = create<AppState>((set, get) => ({
  presets: mockPresets,
  selectedPreset: null,
  filterType: FilterType.ALL,
  searchQuery: '',
  favorites: new Set(['2', '6']),
  
  setSelectedPreset: (preset) => set({ selectedPreset: preset }),
  
  setFilterType: (type) => set({ filterType: type }),
  
  setSearchQuery: (query) => set({ searchQuery: query }),
  
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
      case FilterType.FIND_X:
        filtered = filtered.filter(p => p.deviceModel.includes('Find X'));
        break;
      case FilterType.RENO:
        filtered = filtered.filter(p => p.deviceModel.includes('Reno'));
        break;
      default:
        break;
    }
    
    // Apply search
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(p => 
        p.name.toLowerCase().includes(query) ||
        p.deviceModel.toLowerCase().includes(query)
      );
    }
    
    return filtered;
  }
}));
