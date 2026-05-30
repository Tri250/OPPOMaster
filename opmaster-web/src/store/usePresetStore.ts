import { create } from 'zustand';
import { Preset, DownloadTask, PresetCategory, Comment, FilterConfig } from '../data/mockPresets';

interface PresetStore {
  // 预设数据
  allPresets: Preset[];
  filteredPresets: Preset[];
  myPresets: Preset[];
  customCategories: PresetCategory[];
  
  // 下载管理
  downloadQueue: DownloadTask[];
  
  // 筛选和搜索
  filterConfig: FilterConfig;
  searchQuery: string;
  activeCategory: string;
  
  // UI状态
  isFilterPanelOpen: boolean;
  isDownloadPanelOpen: boolean;
  
  // Actions
  setAllPresets: (presets: Preset[]) => void;
  setFilteredPresets: (presets: Preset[]) => void;
  addMyPreset: (preset: Preset) => void;
  removeMyPreset: (presetId: string) => void;
  updateMyPreset: (presetId: string, updates: Partial<Preset>) => void;
  
  addDownloadTask: (task: DownloadTask) => void;
  removeDownloadTask: (taskId: string) => void;
  updateDownloadTask: (taskId: string, updates: Partial<DownloadTask>) => void;
  updateDownloadProgress: (taskId: string, progress: number) => void;
  
  setFilterConfig: (config: Partial<FilterConfig>) => void;
  setSearchQuery: (query: string) => void;
  setActiveCategory: (category: string) => void;
  
  toggleFilterPanel: () => void;
  toggleDownloadPanel: () => void;
  
  addCustomCategory: (name: string) => void;
  removeCustomCategory: (categoryId: string) => void;
  updateCustomCategory: (categoryId: string, updates: Partial<PresetCategory>) => void;
  
  toggleFavorite: (presetId: string) => void;
  downloadPreset: (preset: Preset) => void;
  deletePreset: (presetId: string) => void;
}

export const usePresetStore = create<PresetStore>((set, get) => ({
  allPresets: [],
  filteredPresets: [],
  myPresets: [],
  customCategories: [
    { id: 'default', name: '默认', presets: [] },
    { id: 'favorites', name: '收藏', presets: [] }
  ],
  
  downloadQueue: [],
  
  filterConfig: {
    selectedStyle: null,
    selectedScene: null,
    searchQuery: '',
    isFavoriteOnly: false,
    isNewOnly: false
  },
  searchQuery: '',
  activeCategory: '全部',
  
  isFilterPanelOpen: false,
  isDownloadPanelOpen: false,
  
  setAllPresets: (presets) => set({ allPresets: presets }),
  setFilteredPresets: (presets) => set({ filteredPresets: presets }),
  
  addMyPreset: (preset) => set((state) => ({ 
    myPresets: [...state.myPresets, { ...preset, isDownloaded: true }] 
  })),
  
  removeMyPreset: (presetId) => set((state) => ({ 
    myPresets: state.myPresets.filter(p => p.id !== presetId) 
  })),
  
  updateMyPreset: (presetId, updates) => set((state) => ({ 
    myPresets: state.myPresets.map(p => p.id === presetId ? { ...p, ...updates } : p) 
  })),
  
  addDownloadTask: (task) => set((state) => ({ 
    downloadQueue: [...state.downloadQueue, task] 
  })),
  
  removeDownloadTask: (taskId) => set((state) => ({ 
    downloadQueue: state.downloadQueue.filter(t => t.id !== taskId) 
  })),
  
  updateDownloadTask: (taskId, updates) => set((state) => ({ 
    downloadQueue: state.downloadQueue.map(t => t.id === taskId ? { ...t, ...updates } : t) 
  })),
  
  updateDownloadProgress: (taskId, progress) => set((state) => ({ 
    downloadQueue: state.downloadQueue.map(t => t.id === taskId ? { ...t, progress } : t) 
  })),
  
  setFilterConfig: (config) => set((state) => ({ 
    filterConfig: { ...state.filterConfig, ...config } 
  })),
  
  setSearchQuery: (query) => set({ searchQuery: query }),
  setActiveCategory: (category) => set({ activeCategory: category }),
  
  toggleFilterPanel: () => set((state) => ({ isFilterPanelOpen: !state.isFilterPanelOpen })),
  toggleDownloadPanel: () => set((state) => ({ isDownloadPanelOpen: !state.isDownloadPanelOpen })),
  
  addCustomCategory: (name) => set((state) => ({ 
    customCategories: [...state.customCategories, { id: Date.now().toString(), name, presets: [] }] 
  })),
  
  removeCustomCategory: (categoryId) => set((state) => {
    const category = state.customCategories.find(c => c.id === categoryId);
    if (!category) return state;
    
    // 将预设移到默认分类
    const defaultCategory = state.customCategories.find(c => c.id === 'default');
    const updatedDefault = defaultCategory 
      ? { ...defaultCategory, presets: [...defaultCategory.presets, ...category.presets] }
      : null;
      
    return {
      customCategories: state.customCategories
        .filter(c => c.id !== categoryId)
        .map(c => updatedDefault && c.id === 'default' ? updatedDefault : c)
    };
  }),
  
  updateCustomCategory: (categoryId, updates) => set((state) => ({ 
    customCategories: state.customCategories.map(c => c.id === categoryId ? { ...c, ...updates } : c) 
  })),
  
  toggleFavorite: (presetId) => set((state) => ({
    allPresets: state.allPresets.map(p => 
      p.id === presetId ? { ...p, isFavorite: !p.isFavorite } : p
    ),
    myPresets: state.myPresets.map(p => 
      p.id === presetId ? { ...p, isFavorite: !p.isFavorite } : p
    )
  })),
  
  downloadPreset: (preset) => {
    const state = get();
    const existingTask = state.downloadQueue.find(t => t.preset.id === preset.id);
    
    if (existingTask) {
      if (existingTask.status === 'paused') {
        // 恢复下载
        get().updateDownloadTask(existingTask.id, { status: 'downloading' });
      }
    } else {
      const task: DownloadTask = {
        id: Date.now().toString(),
        preset,
        progress: 0,
        status: 'pending'
      };
      get().addDownloadTask(task);
    }
  },
  
  deletePreset: (presetId) => {
    get().removeMyPreset(presetId);
  }
}));
