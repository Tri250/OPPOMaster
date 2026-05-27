import { create } from 'zustand';
import type { Preset } from '../types';
import { loadCloudPresets, samplePresets } from '../data/presets';

interface PresetStore {
  presets: Preset[];
  favorites: string[];
  currentPreset: Preset | null;
  isLoading: boolean;
  error: string | null;
  setCurrentPreset: (preset: Preset | null) => void;
  toggleFavorite: (presetId: string) => void;
  getPresetById: (id: string) => Preset | undefined;
  loadPresets: () => Promise<void>;
}

export const usePresetStore = create<PresetStore>((set, get) => ({
  presets: [],
  favorites: [],
  currentPreset: null,
  isLoading: true,
  error: null,
  
  setCurrentPreset: (preset: Preset | null) => set({ currentPreset: preset }),
  
  toggleFavorite: (presetId: string) =>
    set((state) => {
      const isFav = state.favorites.includes(presetId);
      const newFavorites = isFav
        ? state.favorites.filter(id => id !== presetId)
        : [...state.favorites, presetId];
      
      const newPresets = state.presets.map(p => 
        p.id === presetId ? { ...p, isFavorite: !p.isFavorite } : p
      );
      
      return { favorites: newFavorites, presets: newPresets };
    }),
    
  getPresetById: (id: string) => get().presets.find(p => p.id === id),
  
  loadPresets: async () => {
    set({ isLoading: true, error: null });
    try {
      const cloudPresets = await loadCloudPresets();
      const allPresets = cloudPresets.length > 0 ? cloudPresets : samplePresets;
      set({ 
        presets: allPresets, 
        favorites: allPresets.filter(p => p.isFavorite).map(p => p.id),
        isLoading: false 
      });
    } catch (error) {
      console.error('Failed to load presets:', error);
      set({ 
        error: '加载预设失败', 
        presets: samplePresets, 
        isLoading: false 
      });
    }
  }
}));
