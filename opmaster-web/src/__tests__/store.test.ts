import { describe, it, expect } from 'vitest';
import { useAppStore } from '../store/useAppStore';
import { mockPresets } from '../data/mockPresets';

describe('状态管理功能测试', () => {
  describe('初始状态验证', () => {
    it('应该有预设列表', () => {
      const state = useAppStore.getState();
      expect(state.presets).toBeDefined();
      expect(Array.isArray(state.presets)).toBe(true);
      expect(state.presets.length).toBeGreaterThan(0);
    });

    it('初始筛选类型应该是ALL', () => {
      const state = useAppStore.getState();
      expect(state.filterType).toBe('all');
    });

    it('初始搜索查询应该为空', () => {
      const state = useAppStore.getState();
      expect(state.searchQuery).toBe('');
    });

    it('初始排序类型应该是latest', () => {
      const state = useAppStore.getState();
      expect(state.sortType).toBe('latest');
    });

    it('初始收藏列表应该存在', () => {
      const state = useAppStore.getState();
      expect(state.favorites).toBeDefined();
      expect(state.favorites instanceof Set).toBe(true);
    });

    it('初始加载状态应该是false', () => {
      const state = useAppStore.getState();
      expect(state.isLoading).toBe(false);
    });

    it('初始选中的预设应该是null', () => {
      const state = useAppStore.getState();
      expect(state.selectedPreset).toBeNull();
    });
  });

  describe('设置状态功能测试', () => {
    it('setSelectedPreset应该更新选中的预设', () => {
      const preset = mockPresets[0];
      useAppStore.getState().setSelectedPreset(preset);
      expect(useAppStore.getState().selectedPreset).toEqual(preset);
    });

    it('setSelectedPreset应该接受null', () => {
      useAppStore.getState().setSelectedPreset(null);
      expect(useAppStore.getState().selectedPreset).toBeNull();
    });

    it('setFilterType应该更新筛选类型', () => {
      useAppStore.getState().setFilterType('favorites');
      expect(useAppStore.getState().filterType).toBe('favorites');
    });

    it('setSearchQuery应该更新搜索查询', () => {
      useAppStore.getState().setSearchQuery('test query');
      expect(useAppStore.getState().searchQuery).toBe('test query');
    });

    it('setSortType应该更新排序类型', () => {
      useAppStore.getState().setSortType('popular');
      expect(useAppStore.getState().sortType).toBe('popular');
    });

    it('setIsLoading应该更新加载状态', () => {
      useAppStore.getState().setIsLoading(true);
      expect(useAppStore.getState().isLoading).toBe(true);
    });

    it('setPresets应该更新预设列表', () => {
      const newPresets = mockPresets.slice(0, 5);
      useAppStore.getState().setPresets(newPresets);
      expect(useAppStore.getState().presets).toEqual(newPresets);
      // 恢复原始数据
      useAppStore.getState().setPresets(mockPresets);
    });
  });

  describe('收藏功能测试', () => {
    it('toggleFavorite应该添加收藏', () => {
      const initialFavorites = useAppStore.getState().favorites;
      const presetToAdd = mockPresets.find(p => !p.isFavorite);
      
      if (presetToAdd) {
        useAppStore.getState().toggleFavorite(presetToAdd.id);
        const newFavorites = useAppStore.getState().favorites;
        expect(newFavorites.has(presetToAdd.id)).toBe(true);
      }
    });

    it('toggleFavorite应该移除收藏', () => {
      const favoritePreset = mockPresets.find(p => p.isFavorite);
      
      if (favoritePreset) {
        useAppStore.getState().toggleFavorite(favoritePreset.id);
        const newFavorites = useAppStore.getState().favorites;
        expect(newFavorites.has(favoritePreset.id)).toBe(false);
        // 恢复原始状态
        useAppStore.getState().toggleFavorite(favoritePreset.id);
      }
    });

    it('toggleFavorite应该更新预设的isFavorite状态', () => {
      // 找到一个非收藏的预设
      const preset = mockPresets.find(p => !p.isFavorite);
      
      if (preset) {
        // 检查初始状态
        const initialState = useAppStore.getState().presets.find(p => p.id === preset.id)?.isFavorite;
        
        // 切换收藏状态
        useAppStore.getState().toggleFavorite(preset.id);
        
        // 检查更新后的状态
        const updatedPreset = useAppStore.getState().presets.find(p => p.id === preset.id);
        expect(updatedPreset?.isFavorite).toBe(!initialState);
        
        // 恢复原始状态
        useAppStore.getState().toggleFavorite(preset.id);
      }
    });
  });

  describe('搜索历史功能测试', () => {
    it('addToSearchHistory应该添加历史记录', () => {
      useAppStore.getState().clearSearchHistory();
      useAppStore.getState().addToSearchHistory('test search');
      const history = useAppStore.getState().searchHistory;
      expect(history.includes('test search')).toBe(true);
    });

    it('addToSearchHistory应该去重', () => {
      useAppStore.getState().clearSearchHistory();
      useAppStore.getState().addToSearchHistory('duplicate');
      useAppStore.getState().addToSearchHistory('duplicate');
      const history = useAppStore.getState().searchHistory;
      const count = history.filter(q => q === 'duplicate').length;
      expect(count).toBe(1);
    });

    it('addToSearchHistory应该限制在10条以内', () => {
      useAppStore.getState().clearSearchHistory();
      for (let i = 0; i < 15; i++) {
        useAppStore.getState().addToSearchHistory(`search${i}`);
      }
      const history = useAppStore.getState().searchHistory;
      expect(history.length).toBeLessThanOrEqual(10);
    });

    it('addToSearchHistory应该忽略空字符串', () => {
      const initialHistory = useAppStore.getState().searchHistory.length;
      useAppStore.getState().addToSearchHistory('');
      expect(useAppStore.getState().searchHistory.length).toBe(initialHistory);
    });

    it('clearSearchHistory应该清空历史', () => {
      useAppStore.getState().addToSearchHistory('test');
      useAppStore.getState().clearSearchHistory();
      expect(useAppStore.getState().searchHistory.length).toBe(0);
    });
  });

  describe('获取筛选预设功能测试', () => {
    it('getFilteredPresets应该返回所有预设当没有筛选时', () => {
      useAppStore.getState().setFilterType('all');
      useAppStore.getState().setSearchQuery('');
      const filtered = useAppStore.getState().getFilteredPresets();
      expect(filtered.length).toBe(mockPresets.length);
    });

    it('getFilteredPresets应该应用搜索筛选', () => {
      useAppStore.getState().setSearchQuery('富士');
      const filtered = useAppStore.getState().getFilteredPresets();
      expect(filtered.length).toBeGreaterThan(0);
      filtered.forEach(preset => {
        expect(
          preset.name.includes('富士') ||
          preset.category?.includes('富士')
        ).toBe(true);
      });
    });

    it('getFilteredPresets应该应用收藏筛选', () => {
      useAppStore.getState().setFilterType('favorites');
      useAppStore.getState().setSearchQuery('');
      const filtered = useAppStore.getState().getFilteredPresets();
      filtered.forEach(preset => {
        expect(preset.isFavorite).toBe(true);
      });
    });

    it('getFilteredPresets应该应用HNCS筛选', () => {
      useAppStore.getState().setFilterType('hncs');
      useAppStore.getState().setSearchQuery('');
      const filtered = useAppStore.getState().getFilteredPresets();
      filtered.forEach(preset => {
        expect(preset.cameraParams?.hncs).toBe(true);
      });
    });

    it('getFilteredPresets应该应用NEW筛选', () => {
      useAppStore.getState().setFilterType('new');
      useAppStore.getState().setSearchQuery('');
      const filtered = useAppStore.getState().getFilteredPresets();
      filtered.forEach(preset => {
        expect(preset.isNew).toBe(true);
      });
    });

    it('getFilteredPresets应该应用TRENDING筛选', () => {
      useAppStore.getState().setFilterType('trending');
      useAppStore.getState().setSearchQuery('');
      const filtered = useAppStore.getState().getFilteredPresets();
      expect(filtered.length).toBeLessThanOrEqual(8);
    });

    it('getFilteredPresets应该组合筛选和搜索', () => {
      useAppStore.getState().setFilterType('favorites');
      useAppStore.getState().setSearchQuery('富士');
      const filtered = useAppStore.getState().getFilteredPresets();
      filtered.forEach(preset => {
        expect(preset.isFavorite).toBe(true);
        expect(
          preset.name.includes('富士') ||
          preset.category?.includes('富士')
        ).toBe(true);
      });
    });
  });

  describe('排序功能测试', () => {
    it('getFilteredPresets应该支持latest排序', () => {
      useAppStore.getState().setSortType('latest');
      useAppStore.getState().setSearchQuery('');
      const filtered = useAppStore.getState().getFilteredPresets();
      expect(Array.isArray(filtered)).toBe(true);
    });

    it('getFilteredPresets应该支持popular排序', () => {
      useAppStore.getState().setSortType('popular');
      useAppStore.getState().setSearchQuery('');
      const filtered = useAppStore.getState().getFilteredPresets();
      expect(Array.isArray(filtered)).toBe(true);
    });

    it('getFilteredPresets应该支持rating排序', () => {
      useAppStore.getState().setSortType('rating');
      useAppStore.getState().setSearchQuery('');
      const filtered = useAppStore.getState().getFilteredPresets();
      expect(Array.isArray(filtered)).toBe(true);
    });
  });
});
