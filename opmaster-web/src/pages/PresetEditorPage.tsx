import { motion, AnimatePresence } from 'framer-motion';
import { 
  Sliders, 
  Eye, 
  Save, 
  Download, 
  Upload,
  RotateCcw,
  Trash2,
  Share2,
  Trophy,
  GitBranch,
  Tags,
  Image as ImageIcon,
  CheckCircle2,
  Copy,
  Edit3,
  SortAsc,
  SortDesc,
  X
} from 'lucide-react';
import { useState, useEffect } from 'react';

interface PresetParams {
  id: string;
  name: string;
  filter: string;
  filterIntensity: number;
  saturation: number;
  contrast: number;
  brightness: number;
  warmCool: number;
  vignette: boolean;
  tags: string[];
  description: string;
  createdAt?: number;
}

const defaultParams: PresetParams = {
  id: '',
  name: '我的预设',
  filter: '标准',
  filterIntensity: 0,
  saturation: 0,
  contrast: 0,
  brightness: 0,
  warmCool: 0,
  vignette: false,
  tags: [],
  description: '',
  createdAt: Date.now()
};

const filterOptions = [
  '标准', '明艳', '复古', '胶片', '清新', 
  '通透', '黑白', '童话', '梦幻', '冷调', '暖调'
];

const tagOptions = [
  '人像', '风光', '夜景', '美食', '街头',
  '胶片', '复古', '日系', '韩系', '电影感'
];

interface PresetRanking {
  id: string;
  name: string;
  author: string;
  downloads: number;
  favorites: number;
  rating: number;
  tags: string[];
}

const rankingData: PresetRanking[] = [
  { id: '1', name: '哈苏自然', author: '影像大师', downloads: 12345, favorites: 8765, rating: 4.9, tags: ['风光', '人像'] },
  { id: '2', name: '富士经典', author: '胶片爱好者', downloads: 9876, favorites: 6543, rating: 4.8, tags: ['胶片', '复古'] },
  { id: '3', name: '人像暖调', author: '摄影师阿东', downloads: 8765, favorites: 5432, rating: 4.7, tags: ['人像', '韩系'] },
  { id: '4', name: '夜景大师', author: '夜拍达人', downloads: 7654, favorites: 4321, rating: 4.6, tags: ['夜景', '街头'] },
  { id: '5', name: '日系小清新', author: '东京Style', downloads: 6543, favorites: 3210, rating: 4.5, tags: ['日系', '清新'] }
];

type SortField = 'name' | 'createdAt';
type SortOrder = 'asc' | 'desc';

export default function PresetEditorPage() {
  const [params, setParams] = useState<PresetParams>(defaultParams);
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const [savedPresets, setSavedPresets] = useState<PresetParams[]>([]);
  const [showSaveDialog, setShowSaveDialog] = useState(false);
  const [showContributeDialog, setShowContributeDialog] = useState(false);
  const [showRenameDialog, setShowRenameDialog] = useState(false);
  const [showSortDialog, setShowSortDialog] = useState(false);
  const [showImportDialog, setShowImportDialog] = useState(false);
  const [editingPreset, setEditingPreset] = useState<PresetParams | null>(null);
  const [newPresetName, setNewPresetName] = useState('');
  const [sortField, setSortField] = useState<SortField>('createdAt');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [contributeStep, setContributeStep] = useState(1);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [notification, setNotification] = useState<{message: string; type: 'success' | 'error'} | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem('customPresets');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        setSavedPresets(Array.isArray(parsed) ? parsed : []);
      } catch {
        setSavedPresets([]);
      }
    }
  }, []);

  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
  };

  const updateParam = (key: keyof PresetParams, value: any) => {
    setParams(prev => ({ ...prev, [key]: value }));
  };

  const resetParams = () => {
    setParams({ ...defaultParams, id: Date.now().toString(), createdAt: Date.now() });
    setSelectedTags([]);
    showNotification('参数已重置', 'success');
  };

  const savePreset = () => {
    if (!params.name.trim()) {
      showNotification('请输入预设名称', 'error');
      return;
    }
    const newPreset = { 
      ...params, 
      id: Date.now().toString(), 
      tags: selectedTags,
      createdAt: Date.now()
    };
    const updated = [...savedPresets, newPreset];
    setSavedPresets(updated);
    localStorage.setItem('customPresets', JSON.stringify(updated));
    setShowSaveDialog(false);
    showNotification('预设保存成功！', 'success');
  };

  const loadPreset = (preset: PresetParams) => {
    setParams(preset);
    setSelectedTags(preset.tags || []);
    showNotification(`已加载: ${preset.name}`, 'success');
  };

  const deletePreset = (id: string) => {
    const updated = savedPresets.filter(p => p.id !== id);
    setSavedPresets(updated);
    localStorage.setItem('customPresets', JSON.stringify(updated));
    showNotification('预设已删除', 'success');
  };

  const copyPreset = (preset: PresetParams) => {
    const copiedPreset = {
      ...preset,
      id: Date.now().toString(),
      name: `${preset.name} (副本)`,
      createdAt: Date.now()
    };
    const updated = [...savedPresets, copiedPreset];
    setSavedPresets(updated);
    localStorage.setItem('customPresets', JSON.stringify(updated));
    showNotification('预设已复制', 'success');
  };

  const openRenameDialog = (preset: PresetParams) => {
    setEditingPreset(preset);
    setNewPresetName(preset.name);
    setShowRenameDialog(true);
  };

  const renamePreset = () => {
    if (!editingPreset || !newPresetName.trim()) {
      showNotification('请输入有效的名称', 'error');
      return;
    }
    const updated = savedPresets.map(p => 
      p.id === editingPreset.id ? { ...p, name: newPresetName.trim() } : p
    );
    setSavedPresets(updated);
    localStorage.setItem('customPresets', JSON.stringify(updated));
    setShowRenameDialog(false);
    setEditingPreset(null);
    setNewPresetName('');
    showNotification('预设已重命名', 'success');
  };

  const sortPresets = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
    setShowSortDialog(false);
  };

  const getSortedPresets = () => {
    return [...savedPresets].sort((a, b) => {
      let comparison = 0;
      if (sortField === 'name') {
        comparison = a.name.localeCompare(b.name);
      } else if (sortField === 'createdAt') {
        comparison = (a.createdAt || 0) - (b.createdAt || 0);
      }
      return sortOrder === 'asc' ? comparison : -comparison;
    });
  };

  const exportPreset = () => {
    const dataStr = JSON.stringify(params, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.download = `${params.name}_preset.json`;
    link.href = url;
    link.click();
    URL.revokeObjectURL(url);
    showNotification('预设已导出', 'success');
  };

  const exportAllPresets = () => {
    if (savedPresets.length === 0) {
      showNotification('没有可导出的预设', 'error');
      return;
    }
    const dataStr = JSON.stringify(savedPresets, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.download = `all_presets_${Date.now()}.json`;
    link.href = url;
    link.click();
    URL.revokeObjectURL(url);
    showNotification(`已导出 ${savedPresets.length} 个预设`, 'success');
  };

  const importPresets = (file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const content = e.target?.result as string;
        const imported = JSON.parse(content);
        if (Array.isArray(imported)) {
          const newPresets = imported.map(p => ({
            ...p,
            id: Date.now().toString() + Math.random().toString(36).substr(2, 9),
            createdAt: Date.now()
          }));
          const updated = [...savedPresets, ...newPresets];
          setSavedPresets(updated);
          localStorage.setItem('customPresets', JSON.stringify(updated));
          showNotification(`成功导入 ${newPresets.length} 个预设`, 'success');
        } else {
          showNotification('文件格式不正确', 'error');
        }
      } catch {
        showNotification('导入失败，请检查文件格式', 'error');
      }
    };
    reader.readAsText(file);
    setShowImportDialog(false);
  };

  const handleContribute = () => {
    setContributeStep(1);
    setShowContributeDialog(true);
  };

  const toggleTag = (tag: string) => {
    setSelectedTags(prev => 
      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
    );
  };

  const getFilterStyle = () => {
    return `
      saturate(${100 + params.saturation}%) 
      contrast(${100 + params.contrast}%) 
      brightness(${100 + params.brightness}%) 
      sepia(${params.filter === '复古' || params.filter === '胶片' ? params.filterIntensity : 0}%)
      hue-rotate(${params.warmCool * 2}deg)
    `;
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setPreviewImage(event.target?.result as string);
        showNotification('预览图片已更新', 'success');
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <AnimatePresence>
        {notification && (
          <motion.div
            initial={{ opacity: 0, y: -20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            className={`fixed top-24 left-1/2 -translate-x-1/2 z-50 px-6 py-3 rounded-2xl shadow-lg backdrop-blur-xl ${
              notification.type === 'success' 
                ? 'bg-oppo-green/20 border border-oppo-green/30 text-oppo-green' 
                : 'bg-red-500/20 border border-red-500/30 text-red-400'
            }`}
          >
            <div className="flex items-center gap-2">
              {notification.type === 'success' ? (
                <CheckCircle2 className="w-5 h-5" />
              ) : (
                <X className="w-5 h-5" />
              )}
              <span className="font-medium">{notification.message}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="max-w-7xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', stiffness: 200, damping: 15 }}
            className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-orange to-hasselblad-orange rounded-2xl mb-6 shadow-lg"
          >
            <Sliders className="w-12 h-12 text-oppo-black" />
          </motion.div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text-oppo">
            预设编辑器
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            1:1复刻原生相机大师模式参数，创建专属预设，一键贡献社区
          </p>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-1">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
              className="card p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Eye className="w-5 h-5 text-oppo-orange" />
                <span>实时预览</span>
              </h2>
              
              <motion.div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={`relative aspect-[3/4] rounded-xl overflow-hidden bg-black/20 mb-4 transition-all duration-300 ${
                  isDragging ? 'ring-2 ring-oppo-orange ring-offset-2 ring-offset-transparent scale-105' : ''
                }`}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <img
                  src={previewImage || 'https://picsum.photos/seed/preset-editor/400/600'}
                  alt="Preview"
                  className="w-full h-full object-cover transition-all duration-300"
                  style={{ filter: getFilterStyle() }}
                />
                {params.vignette && (
                  <div className="absolute inset-0 shadow-[inset_0_0_80px_rgba(0,0,0,0.6)] pointer-events-none" />
                )}
                {isDragging && (
                  <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="absolute inset-0 bg-oppo-orange/20 flex items-center justify-center"
                  >
                    <Upload className="w-12 h-12 text-oppo-orange" />
                  </motion.div>
                )}
              </motion.div>

              <div className="space-y-2">
                <label className="text-sm text-white/60 flex items-center gap-2">
                  <ImageIcon className="w-4 h-4" />
                  预览图片
                </label>
                <motion.input
                  type="file"
                  accept="image/*"
                  whileFocus={{ scale: 1.02 }}
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) {
                      const reader = new FileReader();
                      reader.onload = (event) => {
                        setPreviewImage(event.target?.result as string);
                        showNotification('预览图片已更新', 'success');
                      };
                      reader.readAsDataURL(file);
                    }
                  }}
                  className="w-full px-4 py-2 bg-white/10 border border-white/20 rounded-lg text-white placeholder-white/40 focus:outline-none focus:border-oppo-orange/50 text-sm file:mr-4 file:py-1 file:px-3 file:rounded-full file:border-0 file:text-sm file:bg-oppo-orange file:text-oppo-black file:cursor-pointer hover:file:bg-oppo-orange/80 transition-colors"
                />
              </div>

              <motion.div 
                className="mt-4 flex gap-2"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.2 }}
              >
                <motion.button
                  whileHover={{ scale: 1.05, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={resetParams}
                  className="btn-secondary flex-1 text-sm py-2 flex items-center justify-center gap-1"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>重置</span>
                </motion.button>
                <motion.button
                  whileHover={{ scale: 1.05, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={exportPreset}
                  className="btn-secondary flex-1 text-sm py-2 flex items-center justify-center gap-1"
                >
                  <Download className="w-4 h-4" />
                  <span>导出</span>
                </motion.button>
              </motion.div>
            </motion.div>
          </div>

          <div className="lg:col-span-2 space-y-6">
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
              className="card p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Sliders className="w-5 h-5 text-oppo-orange" />
                <span>参数调节</span>
              </h2>

              <div className="mb-6">
                <label className="block text-sm font-medium text-white/70 mb-3">滤镜风格</label>
                <div className="flex flex-wrap gap-2">
                  {filterOptions.map((filter, index) => (
                    <motion.button
                      key={filter}
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: index * 0.03 }}
                      whileHover={{ scale: 1.1, y: -2 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => {
                        updateParam('filter', filter);
                        showNotification(`已切换到${filter}`, 'success');
                      }}
                      className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                        params.filter === filter
                          ? 'bg-oppo-orange text-oppo-black shadow-lg shadow-oppo-orange/30'
                          : 'bg-white/10 hover:bg-white/20 text-white'
                      }`}
                    >
                      {filter}
                    </motion.button>
                  ))}
                </div>
              </div>

              <div className="space-y-6">
                {[
                  { key: 'filterIntensity' as const, label: '滤镜强度', min: 0, max: 100, suffix: '%' },
                  { key: 'saturation' as const, label: '饱和度', min: -100, max: 100, suffix: '%' },
                  { key: 'contrast' as const, label: '对比度', min: -100, max: 100, suffix: '%' },
                  { key: 'brightness' as const, label: '亮度', min: -100, max: 100, suffix: '%' },
                ].map(({ key, label, min, max, suffix }) => (
                  <motion.div
                    key={key}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.1 }}
                  >
                    <div className="flex justify-between mb-2">
                      <label className="text-sm font-medium">{label}</label>
                      <motion.span
                        key={params[key]}
                        initial={{ scale: 1.2, color: '#F59E0B' }}
                        animate={{ scale: 1, color: 'rgba(255,255,255,0.6)' }}
                        className="text-sm text-white/60"
                      >
                        {params[key] > 0 ? '+' : ''}{params[key]}{suffix}
                      </motion.span>
                    </div>
                    <input
                      type="range"
                      min={min}
                      max={max}
                      value={params[key]}
                      onChange={(e) => updateParam(key, parseInt(e.target.value))}
                      className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer"
                      style={{
                        background: `linear-gradient(to right, #F59E0B ${((params[key] - min) / (max - min)) * 100}%, rgba(255,255,255,0.1) ${((params[key] - min) / (max - min)) * 100}%)`
                      }}
                    />
                  </motion.div>
                ))}

                <motion.div
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.1 }}
                >
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium">冷暖调</label>
                    <motion.span
                      key={params.warmCool}
                      initial={{ scale: 1.2 }}
                      animate={{ scale: 1 }}
                      className="text-sm"
                      style={{
                        color: params.warmCool > 0 ? '#F59E0B' : params.warmCool < 0 ? '#3B82F6' : 'rgba(255,255,255,0.6)'
                      }}
                    >
                      {params.warmCool > 0 ? '暖+' : params.warmCool < 0 ? '冷+' : ''}{Math.abs(params.warmCool)}
                    </motion.span>
                  </div>
                  <input
                    type="range"
                    min="-100"
                    max="100"
                    value={params.warmCool}
                    onChange={(e) => updateParam('warmCool', parseInt(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer"
                    style={{
                      background: `linear-gradient(to right, #3B82F6 ${(params.warmCool + 100) / 2}%, #F59E0B ${(params.warmCool + 100) / 2}%)`
                    }}
                  />
                </motion.div>

                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.2 }}
                  className="flex items-center justify-between"
                >
                  <label className="text-sm font-medium">暗角效果</label>
                  <motion.button
                    whileTap={{ scale: 0.95 }}
                    onClick={() => {
                      updateParam('vignette', !params.vignette);
                      showNotification(params.vignette ? '暗角已关闭' : '暗角已开启', 'success');
                    }}
                    className={`relative w-12 h-6 rounded-full transition-colors ${
                      params.vignette ? 'bg-oppo-orange' : 'bg-white/20'
                    }`}
                  >
                    <motion.div
                      animate={{ x: params.vignette ? 24 : 0 }}
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                      className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow-md`}
                    />
                  </motion.button>
                </motion.div>
              </div>

              <motion.div 
                className="flex gap-3 mt-6"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
              >
                <motion.button
                  whileHover={{ scale: 1.02, y: -2 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => setShowSaveDialog(true)}
                  className="btn-primary flex-1 flex items-center justify-center gap-2"
                >
                  <Save className="w-5 h-5" />
                  <span>保存预设</span>
                </motion.button>
                <motion.button
                  whileHover={{ scale: 1.02, y: -2 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleContribute}
                  className="btn-secondary flex-1 flex items-center justify-center gap-2"
                >
                  <Share2 className="w-5 h-5" />
                  <span>贡献社区</span>
                </motion.button>
              </motion.div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="card p-6"
            >
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold flex items-center gap-2">
                  <Trophy className="w-5 h-5 text-yellow-500" />
                  <span>预设排行榜</span>
                </h2>
              </div>
              
              <div className="space-y-3">
                {rankingData.map((preset, index) => (
                  <motion.div
                    key={preset.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.4 + index * 0.05 }}
                    whileHover={{ x: 4, backgroundColor: 'rgba(255,255,255,0.05)' }}
                    className="flex items-center gap-4 p-4 bg-white/5 rounded-xl transition-colors"
                  >
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ delay: 0.5 + index * 0.05, type: 'spring' }}
                      className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                        index === 0 ? 'bg-yellow-500 text-black' :
                        index === 1 ? 'bg-gray-400 text-black' :
                        index === 2 ? 'bg-amber-700 text-white' :
                        'bg-white/10 text-white/60'
                      }`}
                    >
                      {index + 1}
                    </motion.div>
                    <div className="flex-1 min-w-0">
                      <h3 className="font-medium truncate">{preset.name}</h3>
                      <p className="text-xs text-white/50">@{preset.author}</p>
                      <div className="flex gap-2 mt-1">
                        {preset.tags.slice(0, 2).map(tag => (
                          <span key={tag} className="text-xs px-2 py-0.5 bg-white/10 rounded-full">
                            {tag}
                          </span>
                        ))}
                      </div>
                    </div>
                    <div className="text-right text-xs text-white/50">
                      <div>{preset.downloads.toLocaleString()} 下载</div>
                      <div className="flex items-center justify-end gap-1">
                        <Trophy className="w-3 h-3 text-yellow-500" />
                        <span>{preset.rating}</span>
                      </div>
                    </div>
                  </motion.div>
                ))}
              </div>
            </motion.div>

            {savedPresets.length > 0 && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 }}
                className="card p-6"
              >
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-bold flex items-center gap-2">
                    <Save className="w-5 h-5 text-green-500" />
                    <span>已保存的预设 ({savedPresets.length})</span>
                  </h2>
                  <div className="flex gap-2">
                    <motion.button
                      whileHover={{ scale: 1.1 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => setShowSortDialog(true)}
                      className="p-2 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
                      title="排序"
                    >
                      {sortOrder === 'asc' ? <SortAsc className="w-4 h-4" /> : <SortDesc className="w-4 h-4" />}
                    </motion.button>
                    <motion.button
                      whileHover={{ scale: 1.1 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={exportAllPresets}
                      className="p-2 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
                      title="导出全部"
                    >
                      <Download className="w-4 h-4" />
                    </motion.button>
                    <motion.button
                      whileHover={{ scale: 1.1 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => setShowImportDialog(true)}
                      className="p-2 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
                      title="导入"
                    >
                      <Upload className="w-4 h-4" />
                    </motion.button>
                  </div>
                </div>
                
                <div className="space-y-3">
                  {getSortedPresets().slice(-5).reverse().map((preset, index) => (
                    <motion.div
                      key={preset.id}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: index * 0.05 }}
                      whileHover={{ x: 4, backgroundColor: 'rgba(255,255,255,0.05)' }}
                      className="flex items-center justify-between p-3 bg-white/5 rounded-lg transition-colors"
                    >
                      <div className="flex-1">
                        <h3 className="font-medium text-sm">{preset.name}</h3>
                        <p className="text-xs text-white/50">
                          {preset.filter} | 饱和{preset.saturation} | 对比{preset.contrast}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <motion.button
                          whileHover={{ scale: 1.1, backgroundColor: 'rgba(255,255,255,0.15)' }}
                          whileTap={{ scale: 0.95 }}
                          onClick={() => copyPreset(preset)}
                          className="p-2 bg-white/10 rounded-lg transition-colors"
                          title="复制"
                        >
                          <Copy className="w-4 h-4" />
                        </motion.button>
                        <motion.button
                          whileHover={{ scale: 1.1, backgroundColor: 'rgba(255,255,255,0.15)' }}
                          whileTap={{ scale: 0.95 }}
                          onClick={() => openRenameDialog(preset)}
                          className="p-2 bg-white/10 rounded-lg transition-colors"
                          title="重命名"
                        >
                          <Edit3 className="w-4 h-4" />
                        </motion.button>
                        <motion.button
                          whileHover={{ scale: 1.1 }}
                          whileTap={{ scale: 0.95 }}
                          onClick={() => loadPreset(preset)}
                          className="p-2 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
                          title="加载"
                        >
                          <Upload className="w-4 h-4" />
                        </motion.button>
                        <motion.button
                          whileHover={{ scale: 1.1, backgroundColor: 'rgba(239,68,68,0.2)' }}
                          whileTap={{ scale: 0.95 }}
                          onClick={() => deletePreset(preset.id)}
                          className="p-2 bg-red-500/20 rounded-lg transition-colors"
                          title="删除"
                        >
                          <Trash2 className="w-4 h-4 text-red-400" />
                        </motion.button>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            )}

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
              className="card p-6 bg-gradient-to-br from-oppo-orange/10 to-transparent"
            >
              <h2 className="text-lg font-bold mb-3 flex items-center gap-2">
                <span className="text-2xl">💡</span>
                <span>使用技巧</span>
              </h2>
              <ul className="space-y-2 text-sm text-white/70">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-orange mt-0.5 flex-shrink-0" />
                  <span>调整参数时观察左侧预览效果，实时同步</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-orange mt-0.5 flex-shrink-0" />
                  <span>上传自己的作品作为预览底图，效果更直观</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-orange mt-0.5 flex-shrink-0" />
                  <span>为预设添加标签，方便后续分类管理</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-orange mt-0.5 flex-shrink-0" />
                  <span>好的预设可以一键贡献社区，帮助更多摄影师</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-orange mt-0.5 flex-shrink-0" />
                  <span>支持JSON格式导入导出，换机备份无忧</span>
                </li>
              </ul>
            </motion.div>
          </div>
        </div>
      </div>

      {/* Save Dialog */}
      <AnimatePresence>
        {showSaveDialog && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowSaveDialog(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
              onClick={(e) => e.stopPropagation()}
              className="card p-6 max-w-md w-full"
            >
              <h3 className="text-xl font-bold mb-4">保存预设</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-2">预设名称</label>
                  <input
                    type="text"
                    value={params.name}
                    onChange={(e) => updateParam('name', e.target.value)}
                    className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-white/40 focus:outline-none focus:border-oppo-orange/50"
                    placeholder="输入预设名称"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-2 flex items-center gap-2">
                    <Tags className="w-4 h-4" />
                    添加标签
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {tagOptions.map(tag => (
                      <motion.button
                        key={tag}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => toggleTag(tag)}
                        className={`px-3 py-1 rounded-full text-xs transition-all ${
                          selectedTags.includes(tag)
                            ? 'bg-oppo-orange text-oppo-black'
                            : 'bg-white/10 hover:bg-white/20'
                        }`}
                      >
                        {tag}
                      </motion.button>
                    ))}
                  </div>
                </div>
                <div className="flex gap-3 pt-2">
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setShowSaveDialog(false)}
                    className="btn-secondary flex-1"
                  >
                    取消
                  </motion.button>
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={savePreset}
                    className="btn-primary flex-1"
                  >
                    保存
                  </motion.button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Rename Dialog */}
      <AnimatePresence>
        {showRenameDialog && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowRenameDialog(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
              onClick={(e) => e.stopPropagation()}
              className="card p-6 max-w-md w-full"
            >
              <h3 className="text-xl font-bold mb-4">重命名预设</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-2">新名称</label>
                  <input
                    type="text"
                    value={newPresetName}
                    onChange={(e) => setNewPresetName(e.target.value)}
                    className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-white/40 focus:outline-none focus:border-oppo-orange/50"
                    placeholder="输入新名称"
                  />
                </div>
                <div className="flex gap-3 pt-2">
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setShowRenameDialog(false)}
                    className="btn-secondary flex-1"
                  >
                    取消
                  </motion.button>
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={renamePreset}
                    className="btn-primary flex-1"
                  >
                    确定
                  </motion.button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Sort Dialog */}
      <AnimatePresence>
        {showSortDialog && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowSortDialog(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
              onClick={(e) => e.stopPropagation()}
              className="card p-6 max-w-sm w-full"
            >
              <h3 className="text-xl font-bold mb-4">排序方式</h3>
              <div className="space-y-2">
                <motion.button
                  whileHover={{ scale: 1.02, backgroundColor: 'rgba(255,255,255,0.1)' }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => sortPresets('name')}
                  className={`w-full p-4 rounded-xl text-left flex items-center justify-between transition-colors ${
                    sortField === 'name' ? 'bg-oppo-orange/20 border border-oppo-orange/30' : 'bg-white/5'
                  }`}
                >
                  <span>按名称排序</span>
                  {sortField === 'name' && (
                    <span className="text-oppo-orange">
                      {sortOrder === 'asc' ? <SortAsc className="w-5 h-5" /> : <SortDesc className="w-5 h-5" />}
                    </span>
                  )}
                </motion.button>
                <motion.button
                  whileHover={{ scale: 1.02, backgroundColor: 'rgba(255,255,255,0.1)' }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => sortPresets('createdAt')}
                  className={`w-full p-4 rounded-xl text-left flex items-center justify-between transition-colors ${
                    sortField === 'createdAt' ? 'bg-oppo-orange/20 border border-oppo-orange/30' : 'bg-white/5'
                  }`}
                >
                  <span>按时间排序</span>
                  {sortField === 'createdAt' && (
                    <span className="text-oppo-orange">
                      {sortOrder === 'asc' ? <SortAsc className="w-5 h-5" /> : <SortDesc className="w-5 h-5" />}
                    </span>
                  )}
                </motion.button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Import Dialog */}
      <AnimatePresence>
        {showImportDialog && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowImportDialog(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
              onClick={(e) => e.stopPropagation()}
              className="card p-6 max-w-md w-full"
            >
              <h3 className="text-xl font-bold mb-4">导入预设</h3>
              <div className="space-y-4">
                <div className="border-2 border-dashed border-white/20 rounded-xl p-8 text-center hover:border-oppo-orange/50 transition-colors">
                  <input
                    type="file"
                    accept=".json"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) importPresets(file);
                    }}
                    className="hidden"
                    id="import-file"
                  />
                  <label htmlFor="import-file" className="cursor-pointer">
                    <Upload className="w-12 h-12 mx-auto mb-3 text-white/50" />
                    <p className="text-white/70">点击选择JSON文件</p>
                    <p className="text-xs text-white/50 mt-1">支持批量导入预设</p>
                  </label>
                </div>
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => setShowImportDialog(false)}
                  className="btn-secondary w-full"
                >
                  取消
                </motion.button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Contribute Dialog */}
      <AnimatePresence>
        {showContributeDialog && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowContributeDialog(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
              onClick={(e) => e.stopPropagation()}
              className="card p-6 max-w-md w-full"
            >
              {contributeStep === 1 ? (
                <>
                  <h3 className="text-xl font-bold mb-4 flex items-center gap-2">
                    <GitBranch className="w-5 h-5" />
                    贡献预设到社区
                  </h3>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-white/70 mb-2">预设名称</label>
                      <input
                        type="text"
                        value={params.name}
                        onChange={(e) => updateParam('name', e.target.value)}
                        className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-white/40 focus:outline-none focus:border-oppo-orange/50"
                        placeholder="输入预设名称"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-white/70 mb-2">预设描述</label>
                      <textarea
                        value={params.description}
                        onChange={(e) => updateParam('description', e.target.value)}
                        className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-white/40 focus:outline-none focus:border-oppo-orange/50 resize-none"
                        rows={3}
                        placeholder="描述这个预设的特点和适用场景"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-white/70 mb-2 flex items-center gap-2">
                        <Tags className="w-4 h-4" />
                        选择标签
                      </label>
                      <div className="flex flex-wrap gap-2">
                        {tagOptions.map(tag => (
                          <motion.button
                            key={tag}
                            whileTap={{ scale: 0.95 }}
                            onClick={() => toggleTag(tag)}
                            className={`px-3 py-1 rounded-full text-xs transition-all ${
                              selectedTags.includes(tag)
                                ? 'bg-oppo-orange text-oppo-black'
                                : 'bg-white/10 hover:bg-white/20'
                            }`}
                          >
                            {tag}
                          </motion.button>
                        ))}
                      </div>
                    </div>
                    <div className="flex gap-3 pt-2">
                      <motion.button
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => setShowContributeDialog(false)}
                        className="btn-secondary flex-1"
                      >
                        取消
                      </motion.button>
                      <motion.button
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => setContributeStep(2)}
                        className="btn-primary flex-1"
                      >
                        下一步
                      </motion.button>
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <div className="text-center">
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', stiffness: 200, damping: 15 }}
                      className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-4"
                    >
                      <CheckCircle2 className="w-8 h-8 text-green-500" />
                    </motion.div>
                    <h3 className="text-xl font-bold mb-2">提交成功！</h3>
                    <p className="text-white/60 mb-6">
                      您的预设已提交到社区审核，审核通过后将会展示给所有用户
                    </p>
                    <div className="p-4 bg-white/5 rounded-xl text-left mb-6">
                      <h4 className="font-medium mb-2">提交信息</h4>
                      <p className="text-sm text-white/60">预设名称: {params.name}</p>
                      <p className="text-sm text-white/60">标签: {selectedTags.join(', ') || '无'}</p>
                      <p className="text-sm text-white/60">状态: 审核中</p>
                    </div>
                    <motion.button
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      onClick={() => setShowContributeDialog(false)}
                      className="btn-primary w-full"
                    >
                      完成
                    </motion.button>
                  </div>
                </>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <style>{`
        input[type="range"]::-webkit-slider-thumb {
          -webkit-appearance: none;
          width: 16px;
          height: 16px;
          background: #F59E0B;
          border-radius: 50%;
          cursor: pointer;
          box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
          transition: transform 0.2s ease;
        }
        
        input[type="range"]::-webkit-slider-thumb:hover {
          transform: scale(1.2);
        }
        
        input[type="range"]::-moz-range-thumb {
          width: 16px;
          height: 16px;
          background: #F59E0B;
          border-radius: 50%;
          cursor: pointer;
          border: none;
          box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
        }
      `}</style>
    </div>
  );
}
