import { motion } from 'framer-motion';
import { 
  Sliders, 
  Eye, 
  Save, 
  Download, 
  Upload,
  RotateCcw,
  Trash2
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
  vignette: false
};

const filterOptions = [
  '标准', '明艳', '复古', '胶片', '清新', 
  '通透', '黑白', '童话', '梦幻', '冷调', '暖调'
];

export default function PresetEditor() {
  const [params, setParams] = useState<PresetParams>(defaultParams);
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const [savedPresets, setSavedPresets] = useState<PresetParams[]>([]);
  const [showSaveDialog, setShowSaveDialog] = useState(false);

  // 加载已保存的预设
  useEffect(() => {
    const saved = localStorage.getItem('customPresets');
    if (saved) {
      setSavedPresets(JSON.parse(saved));
    }
  }, []);

  // 更新参数
  const updateParam = (key: keyof PresetParams, value: any) => {
    setParams(prev => ({ ...prev, [key]: value }));
  };

  // 重置参数
  const resetParams = () => {
    setParams({ ...defaultParams, id: Date.now().toString() });
  };

  // 保存预设
  const savePreset = () => {
    const newPreset = { ...params, id: Date.now().toString() };
    const updated = [...savedPresets, newPreset];
    setSavedPresets(updated);
    localStorage.setItem('customPresets', JSON.stringify(updated));
    setShowSaveDialog(false);
    alert('预设保存成功！');
  };

  // 加载预设
  const loadPreset = (preset: PresetParams) => {
    setParams(preset);
  };

  // 删除预设
  const deletePreset = (id: string) => {
    const updated = savedPresets.filter(p => p.id !== id);
    setSavedPresets(updated);
    localStorage.setItem('customPresets', JSON.stringify(updated));
  };

  // 导出预设
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

  // 导入预设（CAM-011: JSON格式参数导入）
  const importPreset = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // AI-011: 非图像文件格式校验
      if (!file.name.endsWith('.json')) {
        alert('不支持的文件格式，请上传JSON格式的文件');
        return;
      }

      // AI-012: 超大图像文件检测
      const maxSize = 5 * 1024 * 1024; // 5MB
      if (file.size > maxSize) {
        alert('文件过大（超过5MB），请检查文件');
        return;
      }

      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const imported = JSON.parse(event.target?.result as string);
          setParams({ ...imported, id: Date.now().toString() });
          alert('预设导入成功！');
        } catch (err) {
          alert('预设导入失败，请检查文件格式！');
        }
      };
      reader.readAsText(file);
    }
  };

  // 获取CSS滤镜字符串
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
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-yellow-500 to-orange-500 rounded-2xl mb-6">
            <Sliders className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text">
            预设编辑器
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            调整影像参数，实时预览效果，创建您的专属预设
          </p>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left: Preview */}
          <div className="lg:col-span-1">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className="card p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center space-x-2">
                <Eye className="w-5 h-5 text-yellow-500" />
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
                <label className="text-sm text-white/60">预览图片</label>
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
                  className="input text-sm"
                />
              </div>

              {/* Quick Actions */}
              <div className="mt-4 flex gap-2">
                <button onClick={resetParams} className="btn-secondary flex-1 text-sm py-2 flex items-center justify-center space-x-1">
                  <RotateCcw className="w-4 h-4" />
                  <span>重置</span>
                </button>
                <button onClick={exportPreset} className="btn-secondary flex-1 text-sm py-2 flex items-center justify-center space-x-1">
                  <Download className="w-4 h-4" />
                  <span>导出</span>
                </button>
              </div>

              {/* Import Button (CAM-011) */}
              <div className="mt-4">
                <label className="btn-secondary w-full text-sm py-2 flex items-center justify-center space-x-1 cursor-pointer hover:bg-slate-700/50 transition-colors">
                  <Upload className="w-4 h-4" />
                  <span>导入预设</span>
                  <input
                    type="file"
                    accept=".json"
                    onChange={importPreset}
                    className="hidden"
                  />
                </label>
              </div>
            </motion.div>
          </div>

          {/* Center: Controls */}
          <div className="lg:col-span-2 space-y-6">
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
              className="card p-6"
            >
              <h2 className="text-lg font-bold mb-4 flex items-center space-x-2">
                <Sliders className="w-5 h-5 text-orange-500" />
                <span>参数调节</span>
              </h2>

              {/* Filter Selection */}
              <div className="mb-6">
                <label className="block text-sm font-medium text-white/70 mb-3">滤镜风格</label>
                <div className="flex flex-wrap gap-2">
                  {filterOptions.map((filter) => (
                    <button
                      key={filter}
                      onClick={() => updateParam('filter', filter)}
                      className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                        params.filter === filter
                          ? 'bg-yellow-500 text-black'
                          : 'bg-white/10 hover:bg-white/20'
                      }`}
                    >
                      {filter}
                    </button>
                  ))}
                </div>
              </div>

              {/* Sliders */}
              <div className="space-y-6">
                {/* Filter Intensity */}
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
                    className="slider"
                    style={{
                      background: `linear-gradient(to right, #F59E0B ${params.filterIntensity}%, rgba(255,255,255,0.1) ${params.filterIntensity}%)`
                    }}
                  />
                </div>

                {/* Saturation */}
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
                    className="slider"
                    style={{
                      background: `linear-gradient(to right, #F59E0B ${(params.saturation + 100) / 2}%, rgba(255,255,255,0.1) ${(params.saturation + 100) / 2}%)`
                    }}
                  />
                </div>

                {/* Contrast */}
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
                    className="slider"
                    style={{
                      background: `linear-gradient(to right, #F59E0B ${(params.contrast + 100) / 2}%, rgba(255,255,255,0.1) ${(params.contrast + 100) / 2}%)`
                    }}
                  />
                </div>

                {/* Brightness */}
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
                    className="slider"
                    style={{
                      background: `linear-gradient(to right, #F59E0B ${(params.brightness + 100) / 2}%, rgba(255,255,255,0.1) ${(params.brightness + 100) / 2}%)`
                    }}
                  />
                </div>

                {/* Warm/Cool */}
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
                    className="slider"
                    style={{
                      background: `linear-gradient(to right, #3B82F6 ${(params.warmCool + 100) / 2}%, #F59E0B ${(params.warmCool + 100) / 2}%)`
                    }}
                  />
                </div>

                {/* Vignette */}
                <div className="flex items-center justify-between">
                  <label className="text-sm font-medium">暗角效果</label>
                  <button
                    onClick={() => updateParam('vignette', !params.vignette)}
                    className={`relative w-12 h-6 rounded-full transition-colors ${
                      params.vignette ? 'bg-yellow-500' : 'bg-white/20'
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

              {/* Save Button */}
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setShowSaveDialog(true)}
                className="btn-primary w-full mt-6 flex items-center justify-center space-x-2"
              >
                <Save className="w-5 h-5" />
                <span>保存预设</span>
              </motion.button>
            </motion.div>

            {/* Saved Presets */}
            {savedPresets.length > 0 && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="card p-6"
              >
                <h2 className="text-lg font-bold mb-4 flex items-center space-x-2">
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

            {/* Tips */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="card p-6 bg-gradient-to-br from-yellow-500/10 to-transparent"
            >
              <h2 className="text-lg font-bold mb-3">💡 使用技巧</h2>
              <ul className="space-y-2 text-sm text-white/70">
                <li>• 调整参数时观察左侧预览效果</li>
                <li>• 可上传自己的图片作为预览底图</li>
                <li>• 预设会自动保存到浏览器本地</li>
                <li>• 支持JSON格式导入导出，方便分享</li>
                <li>• 点击"导出"下载预设配置文件</li>
              </ul>
            </motion.div>
          </div>
        </div>

        {/* Save Dialog */}
        {showSaveDialog && (
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="bg-deep-space-light rounded-2xl p-6 max-w-md w-full"
            >
              <h3 className="text-xl font-bold mb-4">保存预设</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-white/70 mb-2">预设名称</label>
                  <input
                    type="text"
                    value={params.name}
                    onChange={(e) => updateParam('name', e.target.value)}
                    className="input"
                    placeholder="输入预设名称"
                  />
                </div>
                <div className="flex gap-3">
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
      </div>

      <style>{`
        .slider::-webkit-slider-thumb {
          -webkit-appearance: none;
          width: 16px;
          height: 16px;
          background: #F59E0B;
          border-radius: 50%;
          cursor: pointer;
          box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
        }
        
        .slider::-moz-range-thumb {
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
