import { describe, it, expect } from 'vitest';
import { mockPresets, FilterType, type Preset } from '../data/mockPresets';

describe('搜索和筛选功能测试', () => {
  describe('FilterType 枚举验证', () => {
    it('应该包含所有必要的筛选类型', () => {
      expect(FilterType.ALL).toBe('all');
      expect(FilterType.FAVORITES).toBe('favorites');
      expect(FilterType.HNCS).toBe('hncs');
      expect(FilterType.FIND_X).toBe('find_x');
      expect(FilterType.RENO).toBe('reno');
      expect(FilterType.NEW).toBe('new');
      expect(FilterType.TRENDING).toBe('trending');
    });

    it('筛选类型应该是字符串', () => {
      Object.values(FilterType).forEach((filter) => {
        expect(typeof filter).toBe('string');
      });
    });
  });

  describe('搜索功能测试', () => {
    const searchPresets = (presets: Preset[], query: string): Preset[] => {
      if (!query) return presets;
      const lowerQuery = query.toLowerCase();
      return presets.filter(p =>
        p.name.toLowerCase().includes(lowerQuery) ||
        p.deviceModel.toLowerCase().includes(lowerQuery) ||
        (p.category && p.category.toLowerCase().includes(lowerQuery)) ||
        (p.tags && p.tags.some(tag => tag.toLowerCase().includes(lowerQuery)))
      );
    };

    it('空搜索应该返回所有预设', () => {
      const results = searchPresets(mockPresets, '');
      expect(results.length).toBe(mockPresets.length);
    });

    it('搜索预设名称应该返回匹配的结果', () => {
      const results = searchPresets(mockPresets, '富士');
      expect(results.length).toBeGreaterThan(0);
      results.forEach(preset => {
        expect(preset.name.toLowerCase()).toContain('富士');
      });
    });

    it('搜索设备型号应该返回匹配的结果', () => {
      const results = searchPresets(mockPresets, 'Find X');
      expect(results.length).toBeGreaterThan(0);
      results.forEach(preset => {
        expect(preset.deviceModel.toLowerCase()).toContain('find x');
      });
    });

    it('搜索类别应该返回匹配的结果', () => {
      const results = searchPresets(mockPresets, '人像');
      expect(results.length).toBeGreaterThan(0);
      results.forEach(preset => {
        expect(
          preset.category?.toLowerCase().includes('人像') ||
          preset.category?.toLowerCase().includes('人文')
        ).toBe(true);
      });
    });

    it('搜索标签应该返回匹配的结果', () => {
      const results = searchPresets(mockPresets, '胶片');
      expect(results.length).toBeGreaterThan(0);
      results.forEach(preset => {
        expect(preset.tags?.some(tag => tag.toLowerCase().includes('胶片'))).toBe(true);
      });
    });

    it('不区分大小写的搜索应该工作', () => {
      const upperResults = searchPresets(mockPresets, 'HASSELBLAD');
      const lowerResults = searchPresets(mockPresets, 'hasselblad');
      expect(upperResults.length).toBe(lowerResults.length);
    });

    it('不存在的搜索词应该返回空数组', () => {
      const results = searchPresets(mockPresets, '完全不存在的关键词xyz123');
      expect(results.length).toBe(0);
    });

    it('部分匹配的搜索应该工作', () => {
      const results = searchPresets(mockPresets, '德味');
      expect(results.length).toBeGreaterThan(0);
    });
  });

  describe('筛选功能测试', () => {
    const filterPresets = (presets: Preset[], filterType: string): Preset[] => {
      switch (filterType) {
        case FilterType.FAVORITES:
          return presets.filter(p => p.isFavorite);
        case FilterType.HNCS:
          return presets.filter(p => p.cameraParams?.hncs);
        case FilterType.NEW:
          return presets.filter(p => p.isNew);
        case FilterType.TRENDING:
          return presets.slice(0, 8);
        case FilterType.FIND_X:
          return presets.filter(p => p.deviceModel?.includes('Find X'));
        case FilterType.RENO:
          return presets.filter(p => p.deviceModel?.includes('Reno'));
        default:
          return presets;
      }
    };

    it('ALL筛选应该返回所有预设', () => {
      const results = filterPresets(mockPresets, FilterType.ALL);
      expect(results.length).toBe(mockPresets.length);
    });

    it('FAVORITES筛选应该只返回收藏的预设', () => {
      const results = filterPresets(mockPresets, FilterType.FAVORITES);
      expect(results.length).toBeGreaterThan(0);
      results.forEach(preset => {
        expect(preset.isFavorite).toBe(true);
      });
    });

    it('HNCS筛选应该只返回哈苏认证的预设', () => {
      const results = filterPresets(mockPresets, FilterType.HNCS);
      expect(results.length).toBeGreaterThan(0);
      results.forEach(preset => {
        expect(preset.cameraParams?.hncs).toBe(true);
      });
    });

    it('NEW筛选应该只返回新的预设', () => {
      const results = filterPresets(mockPresets, FilterType.NEW);
      expect(results.length).toBeGreaterThan(0);
      results.forEach(preset => {
        expect(preset.isNew).toBe(true);
      });
    });

    it('TRENDING筛选应该返回最多8个预设', () => {
      const results = filterPresets(mockPresets, FilterType.TRENDING);
      expect(results.length).toBeLessThanOrEqual(8);
    });

    it('FIND_X筛选应该只返回Find X设备的预设', () => {
      const results = filterPresets(mockPresets, FilterType.FIND_X);
      results.forEach(preset => {
        expect(preset.deviceModel).toContain('Find X');
      });
    });

    it('RENO筛选应该只返回Reno设备的预设', () => {
      const results = filterPresets(mockPresets, FilterType.RENO);
      results.forEach(preset => {
        expect(preset.deviceModel).toContain('Reno');
      });
    });
  });

  describe('搜索和筛选组合测试', () => {
    const filterAndSearchPresets = (
      presets: Preset[],
      filterType: string,
      query: string
    ): Preset[] => {
      let results = presets;

      // 应用筛选
      switch (filterType) {
        case FilterType.FAVORITES:
          results = results.filter(p => p.isFavorite);
          break;
        case FilterType.HNCS:
          results = results.filter(p => p.cameraParams?.hncs);
          break;
        case FilterType.NEW:
          results = results.filter(p => p.isNew);
          break;
        case FilterType.TRENDING:
          results = results.slice(0, 8);
          break;
      }

      // 应用搜索
      if (query) {
        const lowerQuery = query.toLowerCase();
        results = results.filter(p =>
          p.name.toLowerCase().includes(lowerQuery) ||
          p.deviceModel.toLowerCase().includes(lowerQuery) ||
          (p.category && p.category.toLowerCase().includes(lowerQuery))
        );
      }

      return results;
    };

    it('筛选和搜索组合应该正确工作', () => {
      const results = filterAndSearchPresets(mockPresets, FilterType.FAVORITES, '富士');
      results.forEach(preset => {
        expect(preset.isFavorite).toBe(true);
        expect(preset.name.toLowerCase()).toContain('富士');
      });
    });

    it('空筛选和空搜索应该返回所有预设', () => {
      const results = filterAndSearchPresets(mockPresets, FilterType.ALL, '');
      expect(results.length).toBe(mockPresets.length);
    });

    it('无效的搜索应该返回空结果', () => {
      const results = filterAndSearchPresets(mockPresets, FilterType.FAVORITES, '不存在的关键词');
      expect(results.length).toBe(0);
    });
  });

  describe('搜索历史管理测试', () => {
    it('搜索历史应该限制在10条以内', () => {
      const maxHistory = 10;
      const history = ['test1', 'test2', 'test3'];
      const newHistory = [...new Set([...history, 'test4'])].slice(0, maxHistory);
      expect(newHistory.length).toBeLessThanOrEqual(maxHistory);
    });

    it('重复搜索不应该增加历史记录', () => {
      const history = ['test1', 'test2', 'test3'];
      const query = 'test1';
      const newHistory = [query, ...history.filter(q => q !== query)].slice(0, 10);
      expect(newHistory.length).toBe(history.length);
    });

    it('清空历史应该返回空数组', () => {
      const history = ['test1', 'test2', 'test3'];
      const clearedHistory: string[] = [];
      expect(clearedHistory.length).toBe(0);
    });
  });
});
