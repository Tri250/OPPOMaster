import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Search, 
  Filter, 
  Heart, 
  Star, 
  Sparkles, 
  ChevronRight, 
  ChevronDown,
  Download,
  Play,
  Pause,
  Trash2,
  X,
  Settings,
  Plus,
  Folder,
  Image as ImageIcon,
  Upload,
  FileText
} from 'lucide-react';
import { usePresetStore } from '../store/usePresetStore';
import { Preset, ALL_STYLES, ALL_SCENES, mockPresets, PresetStyles, PresetScenes } from '../data/mockPresets';

// 分类标签
const CATEGORIES = ['全部', '人像', '风景', '街拍', '胶片', '夜景', '美食', '建筑', '生活', '电影'];

export default function PresetEcosystemPage() {
  const navigate = useNavigate();
  const { 
    allPresets, 
    filteredPresets, 
    setAllPresets, 
    setFilteredPresets,
    filterConfig,
    setFilterConfig,
    searchQuery,
    setSearchQuery,
    activeCategory,
    setActiveCategory,
    isFilterPanelOpen,
    toggleFilterPanel,
    toggleFavorite,
    downloadPreset,
    downloadQueue
  } = usePresetStore();

  const [showSearchSuggestions, setShowSearchSuggestions] = useState(false);
  const [showDownloadPanel, setShowDownloadPanel] = useState(false);

  // 初始化数据
  useEffect(() => {
    setAllPresets(mockPresets);
    setFilteredPresets(mockPresets);
  }, [setAllPresets, setFilteredPresets]);

  // 应用筛选
  useEffect(() => {
    let results = [...allPresets];
    
    // 分类筛选
    if (activeCategory !== '全部') {
      results = results.filter(p => 
        p.category === activeCategory || 
        p.style === activeCategory || 
        p.scene === activeCategory ||
        p.tags?.includes(activeCategory)
      );
    }
    
    // 搜索筛选
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      results = results.filter(p => 
        p.name.toLowerCase().includes(query) ||
        p.tags?.some(tag => tag.toLowerCase().includes(query)) ||
        p.description?.content.toLowerCase().includes(query) ||
        p.author?.toLowerCase().includes(query)
      );
    }
    
    // 其他筛选
    if (filterConfig.selectedStyle) {
      results = results.filter(p => p.style === filterConfig.selectedStyle);
    }
    
    if (filterConfig.selectedScene) {
      results = results.filter(p => p.scene === filterConfig.selectedScene);
    }
    
    if (filterConfig.isFavoriteOnly) {
      results = results.filter(p => p.isFavorite);
    }
    
    if (filterConfig.isNewOnly) {
      results = results.filter(p => p.isNew);
    }
    
    setFilteredPresets(results);
  }, [allPresets, activeCategory, searchQuery, filterConfig, setFilteredPresets]);

  // 搜索建议
  const searchSuggestions = useMemo(() => {
    if (searchQuery.length < 2) return [];
    const suggestions = new Set<string>();
    
    allPresets.forEach(preset => {
      if (preset.name.toLowerCase().includes(searchQuery.toLowerCase())) {
        suggestions.add(preset.name);
      }
      preset.tags?.forEach(tag => {
        if (tag.toLowerCase().includes(searchQuery.toLowerCase())) {
          suggestions.add(tag);
        }
      });
    });
    
    return Array.from(suggestions).slice(0, 8);
  }, [searchQuery, allPresets]);

  // 下载进度模拟
  useEffect(() => {
    const activeDownloads = downloadQueue.filter(t => t.status === 'pending' || t.status === 'downloading');
    
    if (activeDownloads.length > 0) {
      const interval = setInterval(() => {
        activeDownloads.forEach(task => {
          usePresetStore.getState().updateDownloadTask(task.id, { status: 'downloading' });
          
          if (task.progress < 100) {
            const newProgress = Math.min(task.progress + Math.random() * 10, 100);
            usePresetStore.getState().updateDownloadProgress(task.id, newProgress);
            
            if (newProgress >= 100) {
              usePresetStore.getState().updateDownloadTask(task.id, { status: 'completed' });
              usePresetStore.getState().addMyPreset(task.preset);
            }
          }
        });
      }, 500);
      
      return () => clearInterval(interval);
    }
  }, [downloadQueue]);

  return (
    <div className="min-h-screen bg-[#000000] text-white pb-20">
      {/* 顶部导航栏 */}
      <div className="sticky top-0 z-30 bg-[#000000]/95 backdrop-blur-sm border-b border-[#262626]">
        <div className="max-w-6xl mx-auto px-4 sm:px-6">
          <div className="flex items-center h-16">
            <button
              onClick={() => navigate('/')}
              className="flex items-center gap-2 text-[#8A8A8A] hover:text-white transition-colors"
            >
              <ChevronRight className="w-5 h-5 rotate-180" />
              <span className="text-sm">返回</span>
            </button>
            <div className="flex-1 text-center">
              <h1 className="text-lg font-semibold">预设生态</h1>
            </div>
            <button
              onClick={() => setShowDownloadPanel(!showDownloadPanel)}
              className="relative p-2 text-[#8A8A8A] hover:text-white transition-colors"
            >
              <Download className="w-5 h-5" />
              {downloadQueue.length > 0 && (
                <span className="absolute -top-1 -right-1 w-5 h-5 bg-[#FF6B35] rounded-full text-xs flex items-center justify-center">
                  {downloadQueue.length}
                </span>
              )}
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6">
        {/* 搜索栏 */}
        <div className="relative mb-6">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[#8A8A8A]" />
            <input
              type="text"
              placeholder="搜索预设、风格、作者..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onFocus={() => setShowSearchSuggestions(true)}
              onBlur={() => setTimeout(() => setShowSearchSuggestions(false), 200)}
              className="w-full pl-12 pr-4 py-4 bg-[#141414] border border-[#262626] rounded-2xl text-white placeholder-[#8A8A8A] focus:outline-none focus:ring-2 focus:ring-[#FF6B35] focus:border-transparent"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#8A8A8A] hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            )}
          </div>

          {/* 搜索建议 */}
          {showSearchSuggestions && searchSuggestions.length > 0 && (
            <div className="absolute z-20 w-full mt-2 bg-[#141414] border border-[#262626] rounded-2xl shadow-2xl overflow-hidden">
              {searchSuggestions.map((suggestion, index) => (
                <button
                  key={index}
                  onClick={() => {
                    setSearchQuery(suggestion);
                    setShowSearchSuggestions(false);
                  }}
                  className="w-full px-4 py-3 text-left text-[#E5E5E5] hover:bg-[#1A1A1A] transition-colors"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 分类标签 - 横向滚动 */}
        <div className="mb-6">
          <div className="flex gap-3 overflow-x-auto pb-2 -mx-4 px-4 scrollbar-hide">
            {CATEGORIES.map((category) => (
              <button
                key={category}
                onClick={() => setActiveCategory(category)}
                className={`px-5 py-2.5 rounded-full text-sm font-medium whitespace-nowrap transition-all ${
                  activeCategory === category
                    ? 'bg-[#FF6B35] text-white shadow-lg shadow-[#FF6B35]/20'
                    : 'bg-[#141414] text-[#8A8A8A] hover:text-white hover:bg-[#1A1A1A]'
                }`}
              >
                {category}
              </button>
            ))}
          </div>
        </div>

        {/* 快速筛选栏 */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <button
              onClick={toggleFilterPanel}
              className="flex items-center gap-2 px-4 py-2.5 bg-[#141414] hover:bg-[#1A1A1A] text-white rounded-xl transition-colors"
            >
              <Filter className="w-4 h-4" />
              <span className="text-sm">筛选</span>
              <ChevronDown className={`w-4 h-4 transition-transform ${isFilterPanelOpen ? 'rotate-180' : ''}`} />
            </button>
            <button
              onClick={() => setFilterConfig({ isNewOnly: !filterConfig.isNewOnly })}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl transition-colors ${
                filterConfig.isNewOnly
                  ? 'bg-[#FF6B35] text-white'
                  : 'bg-[#141414] text-[#8A8A8A] hover:text-white hover:bg-[#1A1A1A]'
              }`}
            >
              <Sparkles className="w-4 h-4" />
              <span className="text-sm">新品</span>
            </button>
            <button
              onClick={() => setFilterConfig({ isFavoriteOnly: !filterConfig.isFavoriteOnly })}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl transition-colors ${
                filterConfig.isFavoriteOnly
                  ? 'bg-[#FF6B35] text-white'
                  : 'bg-[#141414] text-[#8A8A8A] hover:text-white hover:bg-[#1A1A1A]'
              }`}
            >
              <Heart className="w-4 h-4" />
              <span className="text-sm">收藏</span>
            </button>
          </div>
          <div className="text-sm text-[#8A8A8A]">
            {filteredPresets.length} 个预设
          </div>
        </div>

        {/* 高级筛选面板 */}
        {isFilterPanelOpen && (
          <div className="mb-6 p-6 bg-[#141414] border border-[#262626] rounded-2xl space-y-6">
            {/* 风格筛选 */}
            <div>
              <h3 className="text-sm font-semibold text-[#8A8A8A] mb-3 uppercase tracking-wide">
                风格
              </h3>
              <div className="flex flex-wrap gap-2">
                {ALL_STYLES.map((style) => (
                  <button
                    key={style}
                    onClick={() => setFilterConfig({ 
                      selectedStyle: filterConfig.selectedStyle === style ? null : style 
                    })}
                    className={`px-3 py-1.5 text-sm rounded-full transition-colors ${
                      filterConfig.selectedStyle === style
                        ? 'bg-[#FF6B35] text-white'
                        : 'bg-[#1A1A1A] text-[#8A8A8A] hover:text-white hover:bg-[#262626]'
                    }`}
                  >
                    {style}
                  </button>
                ))}
              </div>
            </div>

            {/* 场景筛选 */}
            <div>
              <h3 className="text-sm font-semibold text-[#8A8A8A] mb-3 uppercase tracking-wide">
                场景
              </h3>
              <div className="flex flex-wrap gap-2">
                {ALL_SCENES.map((scene) => (
                  <button
                    key={scene}
                    onClick={() => setFilterConfig({ 
                      selectedScene: filterConfig.selectedScene === scene ? null : scene 
                    })}
                    className={`px-3 py-1.5 text-sm rounded-full transition-colors ${
                      filterConfig.selectedScene === scene
                        ? 'bg-[#FF6B35] text-white'
                        : 'bg-[#1A1A1A] text-[#8A8A8A] hover:text-white hover:bg-[#262626]'
                    }`}
                  >
                    {scene}
                  </button>
                ))}
              </div>
            </div>

            {/* 排序选项 */}
            <div className="flex items-center justify-between pt-4 border-t border-[#262626]">
              <button
                onClick={() => setFilterConfig({ 
                  selectedStyle: null, 
                  selectedScene: null, 
                  isFavoriteOnly: false, 
                  isNewOnly: false 
                })}
                className="text-[#8A8A8A] hover:text-white text-sm transition-colors"
              >
                重置
              </button>
              <div className="flex items-center gap-2">
                <span className="text-[#8A8A8A] text-sm">排序：</span>
                <button className="text-[#FF6B35] text-sm font-medium">热门</button>
                <button className="text-[#8A8A8A] hover:text-white text-sm transition-colors">最新</button>
                <button className="text-[#8A8A8A] hover:text-white text-sm transition-colors">评分</button>
              </div>
            </div>
          </div>
        )}

        {/* 无结果提示 */}
        {filteredPresets.length === 0 && (
          <div className="flex flex-col items-center justify-center py-20">
            <div className="w-20 h-20 rounded-full bg-[#141414] flex items-center justify-center mb-4">
              <Search className="w-10 h-10 text-[#8A8A8A]" />
            </div>
            <h3 className="text-lg font-medium mb-2">未找到相关预设</h3>
            <p className="text-sm text-[#8A8A8A]">请尝试修改筛选条件或搜索关键词</p>
          </div>
        )}

        {/* 预设网格 */}
        {filteredPresets.length > 0 && (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-5">
            {filteredPresets.map((preset) => (
              <PresetCard
                key={preset.id}
                preset={preset}
                onTap={() => navigate(`/preset/${preset.id}`, { state: { preset } })}
                onToggleFavorite={() => toggleFavorite(preset.id)}
                onDownload={() => downloadPreset(preset)}
              />
            ))}
          </div>
        )}
      </div>

      {/* 下载管理面板 */}
      {showDownloadPanel && (
        <DownloadPanel onClose={() => setShowDownloadPanel(false)} />
      )}
    </div>
  );
}

// 预设卡片组件
function PresetCard({ 
  preset, 
  onTap, 
  onToggleFavorite, 
  onDownload 
}: { 
  preset: Preset;
  onTap: () => void;
  onToggleFavorite: () => void;
  onDownload: () => void;
}) {
  return (
    <div 
      onClick={onTap}
      className="group cursor-pointer"
    >
      <div className="relative aspect-[3/4] rounded-2xl overflow-hidden mb-3 bg-[#141414]">
        <img
          src={preset.coverPath}
          alt={preset.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
        
        {/* 渐变遮罩 */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
        
        {/* 状态标签 */}
        <div className="absolute top-3 left-3 flex gap-2">
          {preset.isNew && (
            <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-[#FF6B35] text-xs font-medium">
              <Sparkles className="w-3 h-3" />
              新品
            </div>
          )}
          {preset.price === 'free' && (
            <div className="px-2 py-1 rounded-full bg-white/20 backdrop-blur-sm text-xs font-medium">
              免费
            </div>
          )}
        </div>
        
        {/* 操作按钮 */}
        <div className="absolute top-3 right-3 flex flex-col gap-2">
          <button
            onClick={(e) => { e.stopPropagation(); onToggleFavorite(); }}
            className="p-2 rounded-full bg-black/40 backdrop-blur-sm hover:bg-black/60 transition-colors"
          >
            <Heart
              className={`w-4 h-4 transition-colors ${
                preset.isFavorite
                  ? 'fill-[#FF6B35] text-[#FF6B35]'
                  : 'text-white'
              }`}
            />
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); onDownload(); }}
            className="p-2 rounded-full bg-black/40 backdrop-blur-sm hover:bg-black/60 transition-colors"
          >
            <Download className="w-4 h-4 text-white" />
          </button>
        </div>

        {/* 评分和下载量 */}
        <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-white/15 backdrop-blur-sm text-xs">
              <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
              <span>{preset.rating?.toFixed(1) || '4.8'}</span>
            </div>
            {preset.style && (
              <span className="px-2 py-1 rounded-full bg-white/15 backdrop-blur-sm text-xs">
                {preset.style}
              </span>
            )}
          </div>
          <span className="text-xs text-white/80">
            {(preset.downloadCount || 1234).toLocaleString()} 下载
          </span>
        </div>
      </div>
      
      <div className="space-y-1">
        <h3 className="text-sm font-medium truncate">{preset.name}</h3>
        <div className="flex items-center justify-between text-xs text-[#8A8A8A]">
          <span className="truncate">{preset.author || '@OPPO影像'}</span>
          {preset.price !== 'free' && preset.price && (
            <span className="text-[#FF6B35] font-medium">¥{preset.price}</span>
          )}
        </div>
      </div>
    </div>
  );
}

// 下载管理面板
function DownloadPanel({ onClose }: { onClose: () => void }) {
  const { downloadQueue, removeDownloadTask, updateDownloadTask, myPresets } = usePresetStore();
  
  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center">
      {/* 背景遮罩 */}
      <div 
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />
      
      {/* 面板 */}
      <div className="relative w-full max-w-lg bg-[#141414] rounded-t-3xl sm:rounded-3xl border-t sm:border border-[#262626] max-h-[80vh] overflow-hidden">
        {/* 顶部手柄 */}
        <div className="flex justify-center pt-4 pb-2">
          <div className="w-12 h-1.5 bg-[#262626] rounded-full" />
        </div>
        
        {/* 标题栏 */}
        <div className="flex items-center justify-between px-6 pb-4">
          <h2 className="text-lg font-semibold">下载管理</h2>
          <button
            onClick={onClose}
            className="p-2 text-[#8A8A8A] hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        
        <div className="overflow-y-auto max-h-[60vh] px-6 pb-6">
          {/* 下载中的任务 */}
          {downloadQueue.length > 0 && (
            <div className="space-y-4 mb-6">
              <h3 className="text-sm font-medium text-[#8A8A8A]">下载中</h3>
              {downloadQueue.map((task) => (
                <div key={task.id} className="flex items-center gap-4 p-4 bg-[#1A1A1A] rounded-xl">
                  <img
                    src={task.preset.coverPath}
                    alt={task.preset.name}
                    className="w-14 h-14 object-cover rounded-lg"
                  />
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-medium truncate">{task.preset.name}</h4>
                    <div className="mt-1 h-1.5 bg-[#262626] rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-[#FF6B35] rounded-full transition-all duration-300"
                        style={{ width: `${task.progress}%` }}
                      />
                    </div>
                    <p className="text-xs text-[#8A8A8A] mt-1">
                      {task.status === 'completed' 
                        ? '下载完成' 
                        : task.status === 'paused' 
                          ? '已暂停' 
                          : task.status === 'error'
                            ? '下载失败'
                            : `${Math.round(task.progress)}%`
                      }
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {task.status === 'downloading' && (
                      <button
                        onClick={() => updateDownloadTask(task.id, { status: 'paused' })}
                        className="p-2 text-[#8A8A8A] hover:text-white"
                      >
                        <Pause className="w-4 h-4" />
                      </button>
                    )}
                    {task.status === 'paused' && (
                      <button
                        onClick={() => updateDownloadTask(task.id, { status: 'downloading' })}
                        className="p-2 text-[#FF6B35]"
                      >
                        <Play className="w-4 h-4" />
                      </button>
                    )}
                    <button
                      onClick={() => removeDownloadTask(task.id)}
                      className="p-2 text-[#8A8A8A] hover:text-red-500"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
          
          {/* 已下载的预设 */}
          {myPresets.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-sm font-medium text-[#8A8A8A]">已下载</h3>
              {myPresets.map((preset) => (
                <div key={preset.id} className="flex items-center gap-4 p-4 bg-[#1A1A1A] rounded-xl">
                  <img
                    src={preset.coverPath}
                    alt={preset.name}
                    className="w-14 h-14 object-cover rounded-lg"
                  />
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-medium truncate">{preset.name}</h4>
                    <p className="text-xs text-[#8A8A8A]">{preset.author || '@OPPO影像'}</p>
                  </div>
                  <button className="p-2 text-[#8A8A8A] hover:text-white">
                    <Settings className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
          
          {downloadQueue.length === 0 && myPresets.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Download className="w-12 h-12 text-[#8A8A8A] mb-3" />
              <p className="text-[#8A8A8A] text-sm">暂无下载任务</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
