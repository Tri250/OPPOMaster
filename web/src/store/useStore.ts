
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { Preset, FilterType, ThemeMode } from '@/types';
import { mockPresets } from '@/data/mockData';

interface AppState {
  presets: Preset[];
  searchQuery: string;
  filterType: FilterType;
  themeMode: ThemeMode;
  setSearchQuery: (query: string) => void;
  setFilterType: (type: FilterType) => void;
  toggleFavorite: (presetId: string) => void;
  setThemeMode: (mode: ThemeMode) => void;
  getFilteredPresets: () => Preset[];
  getPresetById: (id: string) => Preset | undefined;
}

export const useStore = create<AppState>()(
  persist(
    (set, get) => ({
      presets: mockPresets,
      searchQuery: '',
      filterType: 'ALL',
      themeMode: 'system',
      
      setSearchQuery: (query) => set({ searchQuery: query }),
      
      setFilterType: (type) => set({ filterType: type }),
      
      toggleFavorite: (presetId) => 
        set((state) => ({
          presets: state.presets.map((preset) =>
            preset.id === presetId
              ? { ...preset, isFavorite: !preset.isFavorite }
              : preset
          ),
        })),
      
      setThemeMode: (mode) => set({ themeMode: mode }),
      
      getFilteredPresets: () => {
        const { presets, searchQuery, filterType } = get();
        return presets.filter((preset) => {
          const matchesQuery = 
            searchQuery === '' || 
            preset.name.toLowerCase().includes(searchQuery.toLowerCase());
          
          let matchesFilter = true;
          switch (filterType) {
            case 'FAVORITES':
              matchesFilter = preset.isFavorite;
              break;
            case 'HNCS':
              matchesFilter = preset.cameraParams?.hasselblad_hncs === true;
              break;
            case 'FIND_X':
              matchesFilter = preset.deviceModel.toLowerCase().includes('find x');
              break;
            case 'RENO':
              matchesFilter = preset.deviceModel.toLowerCase().includes('reno');
              break;
            default:
              matchesFilter = true;
          }
          
          return matchesQuery && matchesFilter;
        });
      },
      
      getPresetById: (id) => {
        return get().presets.find((preset) => preset.id === id);
      },
    }),
    {
      name: 'omaster-storage',
      partialize: (state) => ({
        presets: state.presets,
        themeMode: state.themeMode,
      }),
    }
  )
);
