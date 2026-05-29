import { motion } from 'framer-motion';
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
  CheckCircle2
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
  description: ''
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

export default function PresetEditorPage() {
  const [params, setParams] = useState<PresetParams>(defaultParams);
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const [savedPresets, setSavedPresets] = useState<PresetParams[]>([]);
  const [showSaveDialog, setShowSaveDialog] = useState(false);
  const [showContributeDialog, setShowContributeDialog] = useState(false);
  const [contributeStep, setContributeStep] = useState(1);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);

  useEffect(() => {
    const saved = localStorage.getItem('customPresets');
    if (saved) {
      setSavedPresets(JSON.parse(saved));
    }
  }, []);

  const updateParam = (key: keyof PresetParams, value: any) => {
    setParams(prev => ({ ...prev, [key]: value }));
  };

  const resetParams = () => {
    setParams({ ...defaultParams, id: Date.now().toString() });
    setSelectedTags([]);
  };

  const savePreset = () => {
    const newPreset = { ...params, id: Date.now().toString(), tags: selectedTags };
    const updated = [...savedPresets, newPreset];
    setSavedPresets(updated);
    localStorage.setItem('customPresets', JSON.stringify(updated));
    setShowSaveDialog(false);
    alert('预设保存成功！');
  };

  const loadPreset = (preset: PresetParams) => {
    setParams(preset);
    setSelectedTags(preset.tags || []);
  };

  const deletePreset = (id: string) => {
    const updated = savedPresets.filter(p => p.id !== id);
    setSavedPresets(updated);
    localStorage.setItem('customPresets', JSON.stringify(updated));
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

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-orange to-hasselblad-orange rounded-2xl mb-6">
            <Sliders className="w-12 h-12 text-white" />
          </div>
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
              className="card p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Eye className="w-5 h-5 text-oppo-orange" />
                <span>实时预览</span>
              </h2>
              
              <div className="relative aspect-[3/4] rounded-xl overflow-hidden bg-black/20 mb-4">
                <img
                  src={previewImage || 'https://picsum.photos/seed/preset-editor/400/600'}
                  alt="Preview"
                  className="w-full h-full object-cover"
                  style={{ filter: getFilterStyle() }}
                />
                {params.vignette && (
                  <div className="absolute inset-0 shadow-[inset_0_0_80px_rgba(0,0,0,0.6)] pointer-events-none" />
                )}
              </div>

              <div className="space-y-2">
                <label className="text-sm text-white/60 flex items-center gap-2">
                  <ImageIcon className="w-4 h-4" />
                  预览图片
                </label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) {
                      const reader = new FileReader();
                      reader.onload = (event) => {
                        setPreviewImage(event.target?.result as string);
                      };
                      reader.readAsDataURL(file);
                    }
                  }}
                  className="w-full px-4 py-2 bg-white/10 border border-white/20 rounded-lg text-white placeholder-white/40 focus:outline-none focus:border-oppo-orange/50 text-sm"
                />
              </div>

              <div className="mt-4 flex gap-2">
                <button onClick={resetParams} className="btn-secondary flex-1 text-sm py-2 flex items-center justify-center gap-1">
                  <RotateCcw className="w-4 h-4" />
                  <span>重置</span>
                </button>
                <button onClick={exportPreset} className="btn-secondary flex-1 text-sm py-2 flex items-center justify-center gap-1">
                  <Download className="w-4 h-4" />
                  <span>导出</span>
                </button>
              </div>
            </motion.div>
          </div>

          <div className="lg:col-span-2 space-y-6">
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
              className="card p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Sliders className="w-5 h-5 text-oppo-orange" />
                <span>参数调节</span>
              </h2>

              <div className="mb-6">
                <label className="block text-sm font-medium text-white/70 mb-3">滤镜风格</label>
                <div className="flex flex-wrap gap-2">
                  {filterOptions.map((filter) => (
                    <button
                      key={filter}
                      onClick={() => updateParam('filter', filter)}
                      className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                        params.filter === filter
                          ? 'bg-oppo-orange text-oppo-black'
                          : 'bg-white/10 hover:bg-white/20 text-white'
                      }`}
                    >
                      {filter}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-6">
                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium">滤镜强度</label>
                    <span className="text-sm text-white/60">{params.filterIntensity}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={params.filterIntensity}
                    onChange={(e) => updateParam('filterIntensity', parseInt(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer"
                    style={{
                      background: `linear-gradient(to right, #F59E0B ${params.filterIntensity}%, rgba(255,255,255,0.1) ${params.filterIntensity}%)`
                    }}
                  />
                </div>

                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium">饱和度</label>
                    <span className="text-sm text-white/60">{params.saturation > 0 ? '+' : ''}{params.saturation}%</span>
                  </div>
                  <input
                    type="range"
                    min="-100"
                    max="100"
                    value={params.saturation}
                    onChange={(e) => updateParam('saturation', parseInt(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer"
                    style={{
                      background: `linear-gradient(to right, #F59E0B ${(params.saturation + 100) / 2}%, rgba(255,255,255,0.1) ${(params.saturation + 100) / 2}%)`
                    }}
                  />
                </div>

                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium">对比度</label>
                    <span className="text-sm text-white/60">{params.contrast > 0 ? '+' : ''}{params.contrast}%</span>
                  </div>
                  <input
                    type="range"
                    min="-100"
                    max="100"
                    value={params.contrast}
                    onChange={(e) => updateParam('contrast', parseInt(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer"
                    style={{
                      background: `linear-gradient(to right, #F59E0B ${(params.contrast + 100) / 2}%, rgba(255,255,255,0.1) ${(params.contrast + 100) / 2}%)`
                    }}
                  />
                </div>

                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium">亮度</label>
                    <span className="text-sm text-white/60">{params.brightness > 0 ? '+' : ''}{params.brightness}%</span>
                  </div>
                  <input
                    type="range"
                    min="-100"
                    max="100"
                    value={params.brightness}
                    onChange={(e) => updateParam('brightness', parseInt(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer"
                    style={{
                      background: `linear-gradient(to right, #F59E0B ${(params.brightness + 100) / 2}%, rgba(255,255,255,0.1) ${(params.brightness + 100) / 2}%)`
                    }}
                  />
                </div>

                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium">冷暖调</label>
                    <span className="text-sm text-white/60">
                      {params.warmCool > 0 ? '暖+' : params.warmCool < 0 ? '冷+' : ''}{Math.abs(params.warmCool)}
                    </span>
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
                </div>

                <div className="flex items-center justify-between">
                  <label className="text-sm font-medium">暗角效果</label>
                  <button
                    onClick={() => updateParam('vignette', !params.vignette)}
                    className={`relative w-12 h-6 rounded-full transition-colors ${
                      params.vignette ? 'bg-oppo-orange' : 'bg-white/20'
                    }`}
                  >
                    <div
                      className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-transform ${
                        params.vignette ? 'translate-x-7' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              </div>

              <div className="flex gap-3 mt-6">
                <button
                  onClick={() => setShowSaveDialog(true)}
                  className="btn-primary flex-1 flex items-center justify-center gap-2"
                >
                  <Save className="w-5 h-5" />
                  <span>保存预设</span>
                </button>
                <button
                  onClick={handleContribute}
                  className="btn-secondary flex-1 flex items-center justify-center gap-2"
                >
                  <Share2 className="w-5 h-5" />
                  <span>贡献社区</span>
                </button>
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="card p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                <Trophy className="w-5 h-5 text-yellow-500" />
                <span>预设排行榜</span>
              </h2>
              
              <div className="space-y-3">
                {rankingData.map((preset, index) => (
                  <motion.div
                    key={preset.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.3 + index * 0.05 }}
                    className="flex items-center gap-4 p-4 bg-white/5 rounded-xl hover:bg-white/10 transition-colors"
                  >
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                      index === 0 ? 'bg-yellow-500 text-black' :
                      index === 1 ? 'bg-gray-400 text-black' :
                      index === 2 ? 'bg-amber-700 text-white' :
                      'bg-white/10 text-white/60'
                    }`}>
                      {index + 1}
                    </div>
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
                transition={{ delay: 0.3 }}
                className="card p-6"
              >
                <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                  <Save className="w-5 h-5 text-green-500" />
                  <span>已保存的预设 ({savedPresets.length})</span>
                </h2>
                
                <div className="space-y-3">
                  {savedPresets.slice(-5).reverse().map((preset) => (
                    <div
                      key={preset.id}
                      className="flex items-center justify-between p-3 bg-white/5 rounded-lg hover:bg-white/10 transition-colors"
                    >
                      <div className="flex-1">
                        <h3 className="font-medium text-sm">{preset.name}</h3>
                        <p className="text-xs text-white/50">
                          {preset.filter} | 饱和{preset.saturation} | 对比{preset.contrast}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={() => loadPreset(preset)}
                          className="p-2 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
                        >
                          <Upload className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => deletePreset(preset.id)}
                          className="p-2 bg-red-500/20 rounded-lg hover:bg-red-500/30 transition-colors"
                        >
                          <Trash2 className="w-4 h-4 text-red-400" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="card p-6 bg-gradient-to-br from-oppo-orange/10 to-transparent"
            >
              <h2 className="text-lg font-bold mb-3">💡 使用技巧</h2>
              <ul className="space-y-2 text-sm text-white/70">
                <li>• 调整参数时观察左侧预览效果，实时同步</li>
                <li>• 上传自己的作品作为预览底图，效果更直观</li>
                <li>• 为预设添加标签，方便后续分类管理</li>
                <li>• 好的预设可以一键贡献社区，帮助更多摄影师</li>
                <li>• 支持JSON格式导入导出，换机备份无忧</li>
              </ul>
            </motion.div>
          </div>
        </div>

        {showSaveDialog && (
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
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
                      <button
                        key={tag}
                        onClick={() => toggleTag(tag)}
                        className={`px-3 py-1 rounded-full text-xs transition-all ${
                          selectedTags.includes(tag)
                            ? 'bg-oppo-orange text-oppo-black'
                            : 'bg-white/10 hover:bg-white/20'
                        }`}
                      >
                        {tag}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="flex gap-3 pt-2">
                  <button
                    onClick={() => setShowSaveDialog(false)}
                    className="btn-secondary flex-1"
                  >
                    取消
                  </button>
                  <button
                    onClick={savePreset}
                    className="btn-primary flex-1"
                  >
                    保存
                  </button>
                </div>
              </div>
            </motion.div>
          </div>
        )}

        {showContributeDialog && (
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
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
                          <button
                            key={tag}
                            onClick={() => toggleTag(tag)}
                            className={`px-3 py-1 rounded-full text-xs transition-all ${
                              selectedTags.includes(tag)
                                ? 'bg-oppo-orange text-oppo-black'
                                : 'bg-white/10 hover:bg-white/20'
                            }`}
                          >
                            {tag}
                          </button>
                        ))}
                      </div>
                    </div>
                    <div className="flex gap-3 pt-2">
                      <button
                        onClick={() => setShowContributeDialog(false)}
                        className="btn-secondary flex-1"
                      >
                        取消
                      </button>
                      <button
                        onClick={() => setContributeStep(2)}
                        className="btn-primary flex-1"
                      >
                        下一步
                      </button>
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <div className="text-center">
                    <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
                      <CheckCircle2 className="w-8 h-8 text-green-500" />
                    </div>
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
                    <button
                      onClick={() => setShowContributeDialog(false)}
                      className="btn-primary w-full"
                    >
                      完成
                    </button>
                  </div>
                </>
              )}
            </motion.div>
          </div>
        )}
      </div>

      <style>{`
        input[type="range"]::-webkit-slider-thumb {
          -webkit-appearance: none;
          width: 16px;
          height: 16px;
          background: #F59E0B;
          border-radius: 50%;
          cursor: pointer;
          box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
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
