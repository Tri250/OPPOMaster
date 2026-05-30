import { describe, it, expect } from 'vitest';
import { mockPresets, FilterType, type Preset } from '../data/mockPresets';

describe('预设管理功能测试', () => {
  describe('预设数据模型验证', () => {
    it('预设列表应该包含数据', () => {
      expect(mockPresets).toBeDefined();
      expect(Array.isArray(mockPresets)).toBe(true);
      expect(mockPresets.length).toBeGreaterThan(0);
    });

    it('每个预设应该有必要的字段', () => {
      mockPresets.forEach((preset) => {
        expect(preset.id).toBeDefined();
        expect(preset.name).toBeDefined();
        expect(preset.coverPath).toBeDefined();
        expect(preset.sections).toBeDefined();
        expect(preset.deviceModel).toBeDefined();
        expect(preset.source).toBeDefined();
        expect(preset.cameraParams).toBeDefined();
      });
    });

    it('预设ID应该是唯一的', () => {
      const ids = mockPresets.map(p => p.id);
      const uniqueIds = new Set(ids);
      expect(uniqueIds.size).toBe(ids.length);
    });

    it('预设名称不应该为空', () => {
      mockPresets.forEach((preset) => {
        expect(preset.name.trim().length).toBeGreaterThan(0);
      });
    });

    it('预设来源应该是有效的值', () => {
      const validSources = ['omaster_cloud', 'community'];
      mockPresets.forEach((preset) => {
        expect(validSources.includes(preset.source)).toBe(true);
      });
    });
  });

  describe('相机参数验证', () => {
    it('带有相机参数的预设应该有有效的模式', () => {
      mockPresets.forEach((preset) => {
        if (preset.cameraParams) {
          expect(['master', 'pro', 'auto']).toContain(preset.cameraParams.mode);
        }
      });
    });

    it('滤镜强度应该在有效范围内', () => {
      mockPresets.forEach((preset) => {
        if (preset.cameraParams) {
          expect(preset.cameraParams.filter_intensity).toBeGreaterThanOrEqual(0);
          expect(preset.cameraParams.filter_intensity).toBeLessThanOrEqual(100);
        }
      });
    });

    it('色调曲线应该在有效范围内', () => {
      mockPresets.forEach((preset) => {
        if (preset.cameraParams) {
          expect(preset.cameraParams.tone_curve).toBeGreaterThanOrEqual(-100);
          expect(preset.cameraParams.tone_curve).toBeLessThanOrEqual(100);
        }
      });
    });

    it('饱和度应该在有效范围内', () => {
      mockPresets.forEach((preset) => {
        if (preset.cameraParams) {
          expect(preset.cameraParams.saturation).toBeGreaterThanOrEqual(-100);
          expect(preset.cameraParams.saturation).toBeLessThanOrEqual(100);
        }
      });
    });

    it('冷暖调应该在有效范围内', () => {
      mockPresets.forEach((preset) => {
        if (preset.cameraParams) {
          expect(preset.cameraParams.warm_cool).toBeGreaterThanOrEqual(-100);
          expect(preset.cameraParams.warm_cool).toBeLessThanOrEqual(100);
        }
      });
    });

    it('青红调应该在有效范围内', () => {
      mockPresets.forEach((preset) => {
        if (preset.cameraParams) {
          expect(preset.cameraParams.cyan_magenta).toBeGreaterThanOrEqual(-100);
          expect(preset.cameraParams.cyan_magenta).toBeLessThanOrEqual(100);
        }
      });
    });

    it('暗角应该是布尔值', () => {
      mockPresets.forEach((preset) => {
        if (preset.cameraParams) {
          expect(typeof preset.cameraParams.vignette).toBe('boolean');
        }
      });
    });
  });

  describe('预设分类和标签验证', () => {
    it('预设类别应该是预定义的类别之一', () => {
      const validCategories = ['街拍', '胶片', '人文', '人像', '纪实', '风景', '夜景', '美食', '生活', '电影', '建筑'];
      mockPresets.forEach((preset) => {
        if (preset.category) {
          expect(validCategories).toContain(preset.category);
        }
      });
    });

    it('预设难度应该是预定义的难度之一', () => {
      const validDifficulties = ['简单', '中等', '进阶', '专家'];
      mockPresets.forEach((preset) => {
        if (preset.difficulty) {
          expect(validDifficulties).toContain(preset.difficulty);
        }
      });
    });

    it('预设标签应该是数组', () => {
      mockPresets.forEach((preset) => {
        if (preset.tags) {
          expect(Array.isArray(preset.tags)).toBe(true);
        }
      });
    });
  });

  describe('预设收藏状态验证', () => {
    it('收藏状态应该是布尔值', () => {
      mockPresets.forEach((preset) => {
        expect(typeof preset.isFavorite).toBe('boolean');
      });
    });

    it('新预设标记应该是布尔值', () => {
      mockPresets.forEach((preset) => {
        if (preset.isNew !== undefined) {
          expect(typeof preset.isNew).toBe('boolean');
        }
      });
    });
  });

  describe('预设图片URL验证', () => {
    it('封面路径应该是有效的URL或路径', () => {
      mockPresets.forEach((preset) => {
        expect(preset.coverPath).toMatch(/^(https?:\/\/|\/|https:\/\/)/);
      });
    });

    it('如果有画廊图片，应该是数组', () => {
      mockPresets.forEach((preset) => {
        if (preset.galleryImages) {
          expect(Array.isArray(preset.galleryImages)).toBe(true);
          expect(preset.galleryImages.length).toBeGreaterThan(0);
        }
      });
    });
  });

  describe('预设数量统计', () => {
    it('应该至少有20个预设', () => {
      expect(mockPresets.length).toBeGreaterThanOrEqual(20);
    });

    it('应该有哈苏官方预设', () => {
      const officialPresets = mockPresets.filter(p => p.source === 'omaster_cloud');
      expect(officialPresets.length).toBeGreaterThan(0);
    });

    it('应该有社区预设', () => {
      const communityPresets = mockPresets.filter(p => p.source === 'community');
      expect(communityPresets.length).toBeGreaterThan(0);
    });

    it('应该有HNCS认证预设', () => {
      const hncsPresets = mockPresets.filter(p => p.cameraParams?.hncs === true);
      expect(hncsPresets.length).toBeGreaterThan(0);
    });

    it('应该有新的预设', () => {
      const newPresets = mockPresets.filter(p => p.isNew === true);
      expect(newPresets.length).toBeGreaterThan(0);
    });

    it('应该有收藏的预设', () => {
      const favoritePresets = mockPresets.filter(p => p.isFavorite === true);
      expect(favoritePresets.length).toBeGreaterThan(0);
    });
  });
});
