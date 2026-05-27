import { create } from 'zustand';
import type { Preset } from '../types';
import { PRESETS } from '../data/presets';

interface PresetStore {
  presets: Preset[];
  favorites: string[];
  currentPreset: Preset | null;
  setCurrentPreset: (preset: Preset | null) => void;
  toggleFavorite: (presetId: string) => void;
  getPresetById: (id: string) => Preset | undefined;
}

export const usePresetStore = create<PresetStore>((set, get) => ({
  presets: PRESETS,
  favorites: PRESETS.filter(p => p.isFavorite).map(p => p.id),
  currentPreset: null,
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
}));
