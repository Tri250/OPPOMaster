import { useState, useMemo, useCallback } from 'react';
import { Preset, FilterConfig, PresetStyles, PresetScenes } from '../data/mockPresets';

/**
 * 预设搜索和筛选管理器
 * 实现Search-001至Search-006功能
 */
export function usePresetSearch(presets: Preset[]) {
  // 筛选状态
  const [filterConfig, setFilterConfig] = useState<FilterConfig>({
    selectedStyle: null,
    selectedScene: null,
    searchQuery: '',
    isFavoriteOnly: false,
    isNewOnly: false,
  });

  // 上一次搜索耗时
  const [lastSearchTime, setLastSearchTime] = useState<number>(0);

  // 模糊匹配 - Levenshtein距离算法
  const levenshteinDistance = useCallback((s: string, t: string): number => {
    if (!s.length) return t.length;
    if (!t.length) return s.length;
    
    const dp = Array.from({ length: s.length + 1 }, (_, i) => 
      Array(t.length + 1).fill(0).map((_, j) => j)
    );
    
    for (let i = 1; i <= s.length; i++) {
      dp[i][0] = i;
      for (let j = 1; j <= t.length; j++) {
        const cost = s[i - 1] === t[j - 1] ? 0 : 1;
        dp[i][j] = Math.min(
          dp[i - 1][j] + 1,      // 删除
          dp[i][j - 1] + 1,      // 插入
          dp[i - 1][j - 1] + cost // 替换
        );
      }
    }
    
    return dp[s.length][t.length];
  }, []);

  // 标准化搜索文本 - 支持特殊字符和生僻词
  const normalizeSearch = useCallback((text: string): string => {
    if (!text) return '';
    return text
      .toLowerCase()
      .trim()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, ' ');
  }, []);

  // 检查预设是否匹配风格
  const matchesStyle = useCallback((preset: Preset, style: string): boolean => {
    if (style === PresetStyles.ALL || !style) return true;
    
    // 检查style字段
    if (preset.style === style) return true;
    
    // 检查category字段
    if (preset.category === style) return true;
    
    // 检查tags
    if (preset.tags?.some(tag => 
      tag.toLowerCase().includes(style.toLowerCase()) || 
      style.toLowerCase().includes(tag.toLowerCase())
    )) return true;
    
    // 检查描述
    if (preset.description?.content?.toLowerCase().includes(style.toLowerCase())) return true;
    
    // 检查名称
    if (preset.name.toLowerCase().includes(style.toLowerCase())) return true;
    
    return false;
  }, []);

  // 检查预设是否匹配场景
  const matchesScene = useCallback((preset: Preset, scene: string): boolean => {
    if (scene === PresetScenes.ALL || !scene) return true;
    
    // 检查scene字段
    if (preset.scene === scene) return true;
    
    // 检查category字段
    if (preset.category === scene) return true;
    
    // 检查tags
    if (preset.tags?.some(tag => 
      tag.toLowerCase().includes(scene.toLowerCase()) || 
      scene.toLowerCase().includes(tag.toLowerCase())
    )) return true;
    
    // 检查描述
    if (preset.description?.content?.toLowerCase().includes(scene.toLowerCase())) return true;
    
    // 检查名称
    if (preset.name.toLowerCase().includes(scene.toLowerCase())) return true;
    
    return false;
  }, []);

  // 检查预设是否匹配搜索查询 - 支持模糊匹配
  const matchesSearch = useCallback((preset: Preset, query: string): boolean => {
    if (!query) return true;
    
    const normalizedQuery = normalizeSearch(query);
    
    // 搜索名称
    if (normalizeSearch(preset.name).includes(normalizedQuery)) return true;
    
    // 搜索标签
    if (preset.tags?.some(tag => normalizeSearch(tag).includes(normalizedQuery))) return true;
    
    // 搜索分类
    if (preset.category && normalizeSearch(preset.category).includes(normalizedQuery)) return true;
    
    // 搜索风格
    if (preset.style && normalizeSearch(preset.style).includes(normalizedQuery)) return true;
    
    // 搜索场景
    if (preset.scene && normalizeSearch(preset.scene).includes(normalizedQuery)) return true;
    
    // 搜索作者
    if (preset.author && normalizeSearch(preset.author).includes(normalizedQuery)) return true;
    
    // 搜索设备型号
    if (normalizeSearch(preset.deviceModel).includes(normalizedQuery)) return true;
    
    // 搜索描述
    if (preset.description?.content && normalizeSearch(preset.description.content).includes(normalizedQuery)) return true;
    
    // 模糊匹配（只对长度>=2的查询进行）
    if (query.length >= 2) {
      if (levenshteinDistance(normalizeSearch(preset.name), normalizedQuery) <= 2) return true;
      
      if (preset.tags?.some(tag => levenshteinDistance(normalizeSearch(tag), normalizedQuery) <= 2)) return true;
    }
    
    return false;
  }, [normalizeSearch, levenshteinDistance]);

  // 应用所有筛选条件
  const filteredPresets = useMemo(() => {
    const startTime = performance.now();
    
    const results = presets.filter(preset => {
      // 风格筛选
      if (filterConfig.selectedStyle && !matchesStyle(preset, filterConfig.selectedStyle)) {
        return false;
      }
      
      // 场景筛选
      if (filterConfig.selectedScene && !matchesScene(preset, filterConfig.selectedScene)) {
        return false;
      }
      
      // 搜索查询
      if (filterConfig.searchQuery && !matchesSearch(preset, filterConfig.searchQuery)) {
        return false;
      }
      
      // 仅收藏
      if (filterConfig.isFavoriteOnly && !preset.isFavorite) {
        return false;
      }
      
      // 仅新品
      if (filterConfig.isNewOnly && !preset.isNew) {
        return false;
      }
      
      return true;
    });
    
    const endTime = performance.now();
    setLastSearchTime(endTime - startTime);
    
    return results;
  }, [presets, filterConfig, matchesStyle, matchesScene, matchesSearch]);

  // 设置风格筛选
  const setStyle = useCallback((style: string | null) => {
    setFilterConfig(prev => ({
      ...prev,
      selectedStyle: style === PresetStyles.ALL ? null : style,
    }));
  }, []);

  // 设置场景筛选
  const setScene = useCallback((scene: string | null) => {
    setFilterConfig(prev => ({
      ...prev,
      selectedScene: scene === PresetScenes.ALL ? null : scene,
    }));
  }, []);

  // 设置搜索查询
  const setSearchQuery = useCallback((query: string) => {
    setFilterConfig(prev => ({
      ...prev,
      searchQuery: query,
    }));
  }, []);

  // 设置仅收藏
  const setFavoriteOnly = useCallback((only: boolean) => {
    setFilterConfig(prev => ({
      ...prev,
      isFavoriteOnly: only,
    }));
  }, []);

  // 设置仅新品
  const setNewOnly = useCallback((only: boolean) => {
    setFilterConfig(prev => ({
      ...prev,
      isNewOnly: only,
    }));
  }, []);

  // 重置所有筛选
  const resetFilters = useCallback(() => {
    setFilterConfig({
      selectedStyle: null,
      selectedScene: null,
      searchQuery: '',
      isFavoriteOnly: false,
      isNewOnly: false,
    });
  }, []);

  // 获取搜索建议
  const getSearchSuggestions = useCallback((query: string): string[] => {
    if (query.length < 2) return [];
    
    const suggestions = new Set<string>();
    const normalizedQuery = normalizeSearch(query);
    
    presets.forEach(preset => {
      // 从名称中获取建议
      if (normalizeSearch(preset.name).includes(normalizedQuery)) {
        suggestions.add(preset.name);
      }
      
      // 从标签中获取建议
      preset.tags?.forEach(tag => {
        if (normalizeSearch(tag).includes(normalizedQuery)) {
          suggestions.add(tag);
        }
      });
    });
    
    return Array.from(suggestions).slice(0, 10);
  }, [presets, normalizeSearch]);

  return {
    filteredPresets,
    filterConfig,
    lastSearchTime,
    
    // 筛选操作
    setStyle,
    setScene,
    setSearchQuery,
    setFavoriteOnly,
    setNewOnly,
    resetFilters,
    
    // 工具函数
    getSearchSuggestions,
  };
}
