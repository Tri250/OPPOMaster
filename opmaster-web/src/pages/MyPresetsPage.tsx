import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, ChevronRight, Download, Edit, Trash2, Plus, Folder, Upload, Share2, Settings, Sparkles, FileText, X } from 'lucide-react';
import { usePresetStore } from '../store/usePresetStore';
import { mockPresets } from '../data/mockPresets';

export default function MyPresetsPage() {
  const navigate = useNavigate();
  const { myPresets, customCategories, updateMyPreset, deletePreset, addCustomCategory, removeCustomCategory, toggleFavorite } = usePresetStore();

  const [activeTab, setActiveTab] = useState<'all' | 'favorites' | 'custom'>('all');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [isEditMode, setIsEditMode] = useState(false);
  const [selectedPresets, setSelectedPresets] = useState<string[]>([]);
  const [showCreateCategory, setShowCreateCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [showImportExport, setShowImportExport] = useState(false);
  const [presetToEdit, setPresetToEdit] = useState<any>(null);
  const [editName, setEditName] = useState('');

  // 如果没有我的预设，使用一些mock数据
  const displayPresets = myPresets.length > 0 ? myPresets : mockPresets.slice(0, 4).map(p => ({ ...p, isDownloaded: true }));

  // 过滤预设
  const filteredPresets = displayPresets.filter(preset => {
    if (activeTab === 'favorites') return preset.isFavorite;
    if (activeTab === 'custom' && selectedCategory !== 'all') {
      // 这里可以根据自定义分类过滤
      return true;
    }
    return true;
  });

  const toggleSelectPreset = (presetId: string) => {
    setSelectedPresets(prev => 
      prev.includes(presetId) 
        ? prev.filter(id => id !== presetId)
        : [...prev, presetId]
    );
  };

  const handleDeletePreset = (presetId: string) => {
    if (confirm('确定要删除这个预设吗？已下载的官方预设可以重新下载。')) {
      deletePreset(presetId);
    }
  };

  const handleBatchDelete = () => {
    if (confirm(`确定要删除选中的 ${selectedPresets.length} 个预设吗？`)) {
      selectedPresets.forEach(id => deletePreset(id));
      setSelectedPresets([]);
      setIsEditMode(false);
    }
  };

  const handleCreateCategory = () => {
    if (newCategoryName.trim()) {
      addCustomCategory(newCategoryName);
      setNewCategoryName('');
      setShowCreateCategory(false);
    }
  };

  const openEditPreset = (preset: any) => {
    setPresetToEdit(preset);
    setEditName(preset.name);
  };

  const saveEditPreset = () => {
    if (presetToEdit && editName.trim()) {
      updateMyPreset(presetToEdit.id, { name: editName });
      setPresetToEdit(null);
    }
  };

  return (
    <div className="min-h-screen bg-[#000000]">
      {/* 顶部导航栏 */}
      <div className="sticky top-0 z-30 bg-[#000000]/95 backdrop-blur-sm border-b border-[#262626]">
        <div className="max-w-6xl mx-auto px-4 sm:px-6">
          <div className="flex items-center h-16">
            <button
              onClick={() => navigate('/preset-ecosystem')}
              className="flex items-center gap-2 text-[#8A8A8A] hover:text-white transition-colors"
            >
              <ChevronRight className="w-5 h-5 rotate-180" />
              <span className="text-sm">返回</span>
            </button>
            <div className="flex-1 text-center">
              <h1 className="text-lg font-semibold">我的预设</h1>
            </div>
            <div className="flex items-center gap-2">
              {isEditMode ? (
                <button
                  onClick={() => {
                    setIsEditMode(false);
                    setSelectedPresets([]);
                  }}
                  className="text-sm text-[#8A8A8A] hover:text-white"
                >
                  取消
                </button>
              ) : (
                <>
                  <button
                    onClick={() => setShowImportExport(true)}
                    className="p-2 text-[#8A8A8A] hover:text-white"
                  >
                    <Upload className="w-5 h-5" />
                  </button>
                  <button
                    onClick={() => setIsEditMode(true)}
                    className="p-2 text-[#8A8A8A] hover:text-white"
                  >
                    <Settings className="w-5 h-5" />
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6">
        {/* 标签页 */}
        <div className="flex gap-1 mb-6 bg-[#141414] rounded-lg p-1">
          <button
            onClick={() => setActiveTab('all')}
            className={`flex-1 py-2.5 rounded-md text-sm font-medium transition-colors ${
              activeTab === 'all' ? 'bg-[#FF6B35]' : 'text-[#8A8A8A] hover:text-white'
            }`}
          >
            全部
          </button>
          <button
            onClick={() => setActiveTab('favorites')}
            className={`flex-1 py-2.5 rounded-md text-sm font-medium transition-colors ${
              activeTab === 'favorites' ? 'bg-[#FF6B35]' : 'text-[#8A8A8A] hover:text-white'
            }`}
          >
            <div className="flex items-center justify-center gap-2">
              <Heart className="w-4 h-4" />
              收藏
            </div>
          </button>
          <button
            onClick={() => setActiveTab('custom')}
            className={`flex-1 py-2.5 rounded-md text-sm font-medium transition-colors ${
              activeTab === 'custom' ? 'bg-[#FF6B35]' : 'text-[#8A8A8A] hover:text-white'
            }`}
          >
            <div className="flex items-center justify-center gap-2">
              <Folder className="w-4 h-4" />
              分类
            </div>
          </button>
        </div>

        {/* 自定义分类（仅在分类标签页显示） */}
        {activeTab === 'custom' && (
          <div className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-medium text-[#8A8A8A]">分类管理</h3>
              <button
                onClick={() => setShowCreateCategory(true)}
                className="flex items-center gap-1 text-sm text-[#FF6B35]"
              >
                <Plus className="w-4 h-4" />
                新建分类
              </button>
            </div>
            <div className="flex gap-2 overflow-x-auto pb-2">
              <button
                onClick={() => setSelectedCategory('all')}
                className={`px-4 py-2 rounded-full text-sm whitespace-nowrap transition-colors ${
                  selectedCategory === 'all' ? 'bg-[#FF6B35]' : 'bg-[#141414] text-[#8A8A8A]'
                }`}
              >
                全部
              </button>
              {customCategories.map(category => (
                <div key={category.id} className="relative">
                  <button
                    onClick={() => setSelectedCategory(category.id)}
                    className={`px-4 py-2 rounded-full text-sm whitespace-nowrap transition-colors ${
                      selectedCategory === category.id ? 'bg-[#FF6B35]' : 'bg-[#141414] text-[#8A8A8A]'
                    }`}
                  >
                    {category.name}
                  </button>
                  {category.id !== 'default' && category.id !== 'favorites' && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        if (confirm('确定要删除这个分类吗？分类内的预设将移到默认分类。')) {
                          removeCustomCategory(category.id);
                        }
                      }}
                      className="absolute -top-1 -right-1 w-4 h-4 bg-red-500 rounded-full flex items-center justify-center text-[10px]"
                    >
                      ×
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 编辑模式操作栏 */}
        {isEditMode && selectedPresets.length > 0 && (
          <div className="fixed bottom-0 left-0 right-0 bg-[#141414] border-t border-[#262626] p-4 z-40">
            <div className="max-w-6xl mx-auto flex items-center justify-between">
              <span className="text-sm text-[#8A8A8A]">已选择 {selectedPresets.length} 个预设</span>
              <div className="flex gap-3">
                <button
                  onClick={() => {
                    // 移动到分类
                  }}
                  className="px-4 py-2 border border-[#262626] rounded-lg text-sm"
                >
                  移动到...
                </button>
                <button
                  onClick={handleBatchDelete}
                  className="px-4 py-2 bg-red-500 rounded-lg text-sm"
                >
                  删除
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 预设网格 */}
        {filteredPresets.length > 0 ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 pb-24">
            {filteredPresets.map(preset => (
              <div
                key={preset.id}
                onClick={() => !isEditMode && navigate(`/preset/${preset.id}`, { state: { preset } })}
                className="group cursor-pointer"
              >
                <div className="relative aspect-[3/4] rounded-2xl overflow-hidden mb-2">
                  <img
                    src={preset.coverPath}
                    alt={preset.name}
                    className="w-full h-full object-cover"
                  />
                  
                  {/* 编辑模式选择框 */}
                  {isEditMode && (
                    <div
                      className="absolute inset-0 bg-black/50 flex items-center justify-center"
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleSelectPreset(preset.id);
                      }}
                    >
                      <div className={`w-8 h-8 rounded-full border-2 flex items-center justify-center ${
                        selectedPresets.includes(preset.id) ? 'bg-[#FF6B35] border-[#FF6B35]' : 'border-white'
                      }`}>
                        {selectedPresets.includes(preset.id) && (
                          <span className="text-white text-sm">✓</span>
                        )}
                      </div>
                    </div>
                  )}

                  {/* 悬浮操作 */}
                  {!isEditMode && (
                    <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          openEditPreset(preset);
                        }}
                        className="p-2.5 bg-white/20 backdrop-blur-sm rounded-full hover:bg-white/30"
                      >
                        <Edit className="w-5 h-5" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleFavorite(preset.id);
                        }}
                        className="p-2.5 bg-white/20 backdrop-blur-sm rounded-full hover:bg-white/30"
                      >
                        <Heart className={`w-5 h-5 ${preset.isFavorite ? 'fill-[#FF6B35] text-[#FF6B35]' : ''}`} />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeletePreset(preset.id);
                        }}
                        className="p-2.5 bg-red-500/80 rounded-full hover:bg-red-500"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    </div>
                  )}

                  {/* 标签 */}
                  {preset.isNew && (
                    <div className="absolute top-2 left-2 px-2 py-1 rounded-full bg-[#FF6B35] text-xs font-medium flex items-center gap-1">
                      <Sparkles className="w-3 h-3" />
                      新品
                    </div>
                  )}
                </div>
                
                <div className="space-y-0.5">
                  <h3 className="text-sm font-medium truncate">{preset.name}</h3>
                  <p className="text-xs text-[#8A8A8A]">{preset.author || '系统预设'}</p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <div className="w-20 h-20 rounded-full bg-[#141414] flex items-center justify-center mb-4">
              <Download className="w-10 h-10 text-[#8A8A8A]" />
            </div>
            <h3 className="text-lg font-medium mb-2">暂无预设</h3>
            <p className="text-sm text-[#8A8A8A] mb-6">
              去预设生态下载或导入自己的预设吧
            </p>
            <button
              onClick={() => navigate('/preset-ecosystem')}
              className="px-6 py-3 bg-[#FF6B35] rounded-xl font-medium"
            >
              浏览预设
            </button>
          </div>
        )}
      </div>

      {/* 新建分类弹窗 */}
      {showCreateCategory && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="bg-[#141414] rounded-2xl p-6 w-full max-w-sm mx-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">新建分类</h3>
              <button
                onClick={() => setShowCreateCategory(false)}
                className="p-1 text-[#8A8A8A] hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <input
              type="text"
              placeholder="输入分类名称"
              value={newCategoryName}
              onChange={(e) => setNewCategoryName(e.target.value)}
              className="w-full px-4 py-3 bg-[#0a0a0a] border border-[#262626] rounded-xl text-sm focus:outline-none focus:border-[#FF6B35]"
              autoFocus
            />
            <div className="flex gap-3 mt-6">
              <button
                onClick={() => setShowCreateCategory(false)}
                className="flex-1 py-3 border border-[#262626] rounded-xl text-sm"
              >
                取消
              </button>
              <button
                onClick={handleCreateCategory}
                disabled={!newCategoryName.trim()}
                className="flex-1 py-3 bg-[#FF6B35] rounded-xl text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
              >
                创建
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 导入导出弹窗 */}
      {showImportExport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="bg-[#141414] rounded-2xl p-6 w-full max-w-sm mx-4">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold">导入/导出</h3>
              <button
                onClick={() => setShowImportExport(false)}
                className="p-1 text-[#8A8A8A] hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-3">
              <button className="w-full flex items-center gap-3 p-4 bg-[#0a0a0a] rounded-xl hover:bg-[#1a1a1a] transition-colors">
                <div className="w-10 h-10 bg-[#FF6B35]/20 rounded-lg flex items-center justify-center">
                  <Upload className="w-5 h-5 text-[#FF6B35]" />
                </div>
                <div className="text-left">
                  <p className="font-medium">导入预设</p>
                  <p className="text-xs text-[#8A8A8A]">支持 .cube, .xmp, .dng 格式</p>
                </div>
              </button>
              <button className="w-full flex items-center gap-3 p-4 bg-[#0a0a0a] rounded-xl hover:bg-[#1a1a1a] transition-colors">
                <div className="w-10 h-10 bg-blue-500/20 rounded-lg flex items-center justify-center">
                  <FileText className="w-5 h-5 text-blue-500" />
                </div>
                <div className="text-left">
                  <p className="font-medium">导出预设</p>
                  <p className="text-xs text-[#8A8A8A]">导出为 .cube 或 .xmp 格式</p>
                </div>
              </button>
              <button className="w-full flex items-center gap-3 p-4 bg-[#0a0a0a] rounded-xl hover:bg-[#1a1a1a] transition-colors">
                <div className="w-10 h-10 bg-green-500/20 rounded-lg flex items-center justify-center">
                  <Share2 className="w-5 h-5 text-green-500" />
                </div>
                <div className="text-left">
                  <p className="font-medium">分享预设</p>
                  <p className="text-xs text-[#8A8A8A]">生成分享链接给朋友</p>
                </div>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 编辑预设弹窗 */}
      {presetToEdit && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="bg-[#141414] rounded-2xl p-6 w-full max-w-sm mx-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">编辑预设</h3>
              <button
                onClick={() => setPresetToEdit(null)}
                className="p-1 text-[#8A8A8A] hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="mb-4">
              <label className="block text-sm text-[#8A8A8A] mb-2">预设名称</label>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                className="w-full px-4 py-3 bg-[#0a0a0a] border border-[#262626] rounded-xl text-sm focus:outline-none focus:border-[#FF6B35]"
                autoFocus
              />
            </div>
            {/* 这里可以添加更多编辑项 */}
            <div className="flex gap-3 mt-6">
              <button
                onClick={() => setPresetToEdit(null)}
                className="flex-1 py-3 border border-[#262626] rounded-xl text-sm"
              >
                取消
              </button>
              <button
                onClick={saveEditPreset}
                disabled={!editName.trim()}
                className="flex-1 py-3 bg-[#FF6B35] rounded-xl text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
              >
                保存
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
