import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, Star, Sparkles, ChevronRight, Camera, Search } from 'lucide-react';
import { PresetSearchFilter } from '../components/PresetSearchFilter';
import { mockPresets, Preset } from '../data/mockPresets';

// ColorOS 16 设计规范常量
const COLORS = {
  primary: '#FF6B35', // OPPO 橙色
  background: '#000000',
  cardBg: '#141414',
  textPrimary: '#FFFFFF',
  textSecondary: '#8A8A8A',
  border: '#262626',
  surface: '#1A1A1A'
};

export default function PresetLibraryPage() {
  const navigate = useNavigate();
  const [filteredPresets, setFilteredPresets] = useState<Preset[]>(mockPresets);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());

  // 切换收藏状态
  const toggleFavorite = (e: React.MouseEvent, presetId: string) => {
    e.stopPropagation();
    const newFavorites = new Set(favorites);
    if (newFavorites.has(presetId)) {
      newFavorites.delete(presetId);
    } else {
      newFavorites.add(presetId);
    }
    setFavorites(newFavorites);
  };

  // 打开预设详情
  const openPresetDetail = (preset: Preset) => {
    navigate(`/preset/${preset.id}`, { state: { preset } });
  };

  return (
    <div className="min-h-screen bg-[#000000] text-white">
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
              <h1 className="text-lg font-semibold">预设库</h1>
            </div>
            <div className="w-16"></div>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6">
        {/* 搜索筛选区域 */}
        <PresetSearchFilter
          presets={mockPresets}
          onFilteredPresets={setFilteredPresets}
        />

        {/* 筛选结果统计 */}
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm text-[#8A8A8A]">
            共 <span className="text-white font-medium">{filteredPresets.length}</span> 个预设
          </p>
          <div className="flex items-center gap-2 text-xs text-[#8A8A8A]">
            <Camera className="w-4 h-4" />
            <span>OPPO哈苏大师模式</span>
          </div>
        </div>

        {/* 无结果提示 */}
        {filteredPresets.length === 0 && (
          <div className="flex flex-col items-center justify-center py-20">
            <div className="w-16 h-16 rounded-full bg-[#141414] flex items-center justify-center mb-4">
              <Search className="w-8 h-8 text-[#8A8A8A]" />
            </div>
            <h3 className="text-lg font-medium mb-2">未找到相关预设</h3>
            <p className="text-sm text-[#8A8A8A]">请尝试修改筛选条件或搜索关键词</p>
          </div>
        )}

        {/* 预设网格 */}
        {filteredPresets.length > 0 && (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {filteredPresets.map((preset) => (
              <div
                key={preset.id}
                onClick={() => openPresetDetail(preset)}
                className="group cursor-pointer"
              >
                <div className="relative aspect-[3/4] rounded-2xl overflow-hidden mb-3">
                  <img
                    src={preset.coverPath}
                    alt={preset.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  
                  {/* 渐变遮罩 */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
                  
                  {/* 新品标签 */}
                  {preset.isNew && (
                    <div className="absolute top-3 left-3">
                      <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-[#FF6B35] text-xs font-medium">
                        <Sparkles className="w-3 h-3" />
                        新品
                      </div>
                    </div>
                  )}
                  
                  {/* 收藏按钮 */}
                  <button
                    onClick={(e) => toggleFavorite(e, preset.id)}
                    className="absolute top-3 right-3 p-2 rounded-full bg-black/40 backdrop-blur-sm hover:bg-black/60 transition-colors"
                  >
                    <Heart
                      className={`w-4 h-4 transition-colors ${
                        favorites.has(preset.id)
                          ? 'fill-[#FF6B35] text-[#FF6B35]'
                          : 'text-white'
                      }`}
                    />
                  </button>

                  {/* 风格标签 */}
                  {preset.style && (
                    <div className="absolute bottom-3 left-3">
                      <span className="px-2 py-1 rounded-full bg-white/15 backdrop-blur-sm text-xs">
                        {preset.style}
                      </span>
                    </div>
                  )}
                </div>
                
                <div className="space-y-1">
                  <h3 className="text-sm font-medium truncate">{preset.name}</h3>
                  <div className="flex items-center gap-2 text-xs text-[#8A8A8A]">
                    {preset.author && (
                      <span className="truncate">{preset.author}</span>
                    )}
                    {preset.style && preset.author && (
                      <span>•</span>
                    )}
                    {preset.style && <span>{preset.style}</span>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
