import { motion } from 'framer-motion';
import { 
  Maximize2, 
  Minimize2, 
  Smartphone, 
  ChevronLeft,
  ChevronRight,
  Layers,
  Palette,
  Sun,
  Moon,
  Monitor
} from 'lucide-react';
import { useState } from 'react';
import { ColorOSSwitch } from '../components/common/ColorOSComponents';

interface FloatingWindowSettings {
  enabled: boolean;
  autoStart: boolean;
  transparency: number;
  position: 'left' | 'right';
  size: 'small' | 'medium' | 'large';
  showParams: boolean;
  showDevice: boolean;
  theme: 'light' | 'dark' | 'auto';
  presets: string[];
  currentPreset: string | null;
}

export default function FloatingWindowPage() {
  const [settings, setSettings] = useState<FloatingWindowSettings>({
    enabled: false,
    autoStart: true,
    transparency: 80,
    position: 'right',
    size: 'medium',
    showParams: true,
    showDevice: true,
    theme: 'dark',
    presets: [],
    currentPreset: null
  });
  
  const [isRunning, setIsRunning] = useState(false);
  const [previewMode, setPreviewMode] = useState(false);

  const presetExamples = [
    { id: '1', name: '哈苏人像大师', device: 'OPPO Find X8 Ultra', params: 'HNCS HDR 饱和:10 对比:8' },
    { id: '2', name: '徕卡经典', device: 'Xiaomi 16 Ultra', params: '徕卡 饱:12 对比:10' },
    { id: '3', name: '蔡司自然', device: 'vivo X200 Ultra', params: '蔡司 饱:8 对比:12' },
    { id: '4', name: 'XMAGE影像', device: 'Huawei Mate 80 Pro+', params: 'XMAGE 饱和:10' },
    { id: '5', name: '电影色调', device: '通用', params: '对比:18 饱:6' },
  ];

  const toggleFloatingWindow = () => {
    setIsRunning(!isRunning);
  };

  const previewSizes = {
    small: 'w-64',
    medium: 'w-80',
    large: 'w-96'
  };

  return (
    <div className="min-h-screen bg-deep-space text-white pb-20">
      {/* 顶部导航 */}
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Layers className="w-5 h-5 text-oppo-sunrise-gold" />
            <h1 className="text-lg font-semibold">悬浮窗</h1>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPreviewMode(!previewMode)}
              className={`px-3 py-1.5 rounded-full text-sm transition-colors ${
                previewMode 
                  ? 'bg-oppo-sunrise-gold text-black' 
                  : 'bg-white/10 text-white'
              }`}
            >
              {previewMode ? '退出预览' : '预览效果'}
            </button>
            <button
              onClick={toggleFloatingWindow}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                isRunning
                  ? 'bg-error-vital text-white'
                  : 'bg-oppo-green text-white hover:bg-oppo-green/90'
              }`}
            >
              {isRunning ? '停止悬浮窗' : '启动悬浮窗'}
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        {/* 状态卡片 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className={`card-oppo p-6 ${
            isRunning ? 'ring-2 ring-oppo-green/50' : ''
          }`}
        >
          <div className="flex items-center gap-4">
            <div className={`w-16 h-16 rounded-2xl flex items-center justify-center ${
              isRunning ? 'bg-oppo-green/20' : 'bg-white/5'
            }`}>
              <Layers className={`w-8 h-8 ${isRunning ? 'text-oppo-green' : 'text-text-tertiary'}`} />
            </div>
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-semibold">悬浮窗服务</h2>
                <span className={`px-2 py-0.5 text-xs rounded-full ${
                  isRunning 
                    ? 'bg-oppo-green/20 text-oppo-green' 
                    : 'bg-white/10 text-text-tertiary'
                }`}>
                  {isRunning ? '运行中' : '未启动'}
                </span>
              </div>
              <p className="text-sm text-text-secondary mt-1">
                {isRunning 
                  ? '悬浮窗正在运行中，可以在其他应用上方显示预设参数'
                  : '点击"启动悬浮窗"在拍照时显示预设参数参考'
                }
              </p>
            </div>
            <div className={`w-3 h-3 rounded-full ${
              isRunning ? 'bg-oppo-green animate-pulse' : 'bg-text-tertiary'
            }`} />
          </div>
        </motion.div>

        {/* 预览区域 */}
        {previewMode && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="card-oppo p-6"
          >
            <h3 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">
              悬浮窗预览
            </h3>
            <div className="relative bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl p-8 min-h-64 flex items-center justify-center">
              {/* 模拟相机界面 */}
              <div className="absolute inset-0 opacity-30">
                <div className="w-full h-full bg-gradient-to-br from-oppo-sunrise-gold/20 to-transparent" />
              </div>
              
              {/* 悬浮窗预览 */}
              <div className={`absolute ${settings.position === 'right' ? 'right-8' : 'left-8'} ${
                previewSizes[settings.size]
              }`}>
                <motion.div
                  initial={{ scale: 0.9, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  className="bg-black/90 backdrop-blur-xl rounded-2xl p-4 border border-white/10"
                  style={{ opacity: settings.transparency / 100 }}
                >
                  <div className="flex items-start gap-3">
                    <div className="flex-1">
                      <p className="text-white text-sm font-medium">
                        {presetExamples[0].name}
                      </p>
                      {settings.showDevice && (
                        <p className="text-oppo-green text-xs mt-1">
                          {presetExamples[0].device}
                        </p>
                      )}
                      {settings.showParams && (
                        <p className="text-text-tertiary text-xs mt-2">
                          {presetExamples[0].params}
                        </p>
                      )}
                    </div>
                    <span className="text-text-tertiary text-xs">1/5</span>
                  </div>
                  
                  <div className="flex items-center justify-center gap-3 mt-3">
                    <button className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
                      <ChevronRight className="w-4 h-4" />
                    </button>
                    <button className="w-8 h-8 rounded-full bg-error-vital/50 flex items-center justify-center">
                      <Minimize2 className="w-4 h-4" />
                    </button>
                  </div>
                </motion.div>
              </div>
              
              <p className="text-text-tertiary text-sm">相机取景区域</p>
            </div>
          </motion.div>
        )}

        {/* 快速设置 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">
            快速设置
          </h2>
          <div className="card-oppo divide-y divide-white/5">
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={settings.enabled}
                onChange={(v) => setSettings({ ...settings, enabled: v })}
                label="启用悬浮窗"
                description="在应用上方显示悬浮窗口"
              />
            </div>
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={settings.autoStart}
                onChange={(v) => setSettings({ ...settings, autoStart: v })}
                label="开机自启"
                description="设备启动时自动开启悬浮窗"
              />
            </div>
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={settings.showParams}
                onChange={(v) => setSettings({ ...settings, showParams: v })}
                label="显示参数"
                description="显示预设的详细参数信息"
              />
            </div>
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={settings.showDevice}
                onChange={(v) => setSettings({ ...settings, showDevice: v })}
                label="显示设备"
                description="显示预设对应的设备型号"
              />
            </div>
          </div>
        </motion.section>

        {/* 外观设置 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">
            外观设置
          </h2>
          <div className="card-oppo p-4 space-y-4">
            {/* 透明度 */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-sm">透明度</label>
                <span className="text-sm text-text-secondary">{settings.transparency}%</span>
              </div>
              <input
                type="range"
                min="30"
                max="100"
                value={settings.transparency}
                onChange={(e) => setSettings({ ...settings, transparency: Number(e.target.value) })}
                className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer slider"
              />
            </div>

            {/* 位置 */}
            <div>
              <label className="text-sm block mb-2">显示位置</label>
              <div className="flex gap-2">
                <button
                  onClick={() => setSettings({ ...settings, position: 'left' })}
                  className={`flex-1 py-2 px-4 rounded-lg text-sm transition-colors ${
                    settings.position === 'left'
                      ? 'bg-oppo-sunrise-gold text-black'
                      : 'bg-white/10 text-white'
                  }`}
                >
                  左侧
                </button>
                <button
                  onClick={() => setSettings({ ...settings, position: 'right' })}
                  className={`flex-1 py-2 px-4 rounded-lg text-sm transition-colors ${
                    settings.position === 'right'
                      ? 'bg-oppo-sunrise-gold text-black'
                      : 'bg-white/10 text-white'
                  }`}
                >
                  右侧
                </button>
              </div>
            </div>

            {/* 尺寸 */}
            <div>
              <label className="text-sm block mb-2">窗口尺寸</label>
              <div className="flex gap-2">
                <button
                  onClick={() => setSettings({ ...settings, size: 'small' })}
                  className={`flex-1 py-2 px-4 rounded-lg text-sm flex items-center justify-center gap-2 transition-colors ${
                    settings.size === 'small'
                      ? 'bg-oppo-sunrise-gold text-black'
                      : 'bg-white/10 text-white'
                  }`}
                >
                  <Minimize2 className="w-4 h-4" />
                  小
                </button>
                <button
                  onClick={() => setSettings({ ...settings, size: 'medium' })}
                  className={`flex-1 py-2 px-4 rounded-lg text-sm flex items-center justify-center gap-2 transition-colors ${
                    settings.size === 'medium'
                      ? 'bg-oppo-sunrise-gold text-black'
                      : 'bg-white/10 text-white'
                  }`}
                >
                  <Maximize2 className="w-4 h-4" />
                  中
                </button>
                <button
                  onClick={() => setSettings({ ...settings, size: 'large' })}
                  className={`flex-1 py-2 px-4 rounded-lg text-sm flex items-center justify-center gap-2 transition-colors ${
                    settings.size === 'large'
                      ? 'bg-oppo-sunrise-gold text-black'
                      : 'bg-white/10 text-white'
                  }`}
                >
                  <Maximize2 className="w-5 h-5" />
                  大
                </button>
              </div>
            </div>

            {/* 主题 */}
            <div>
              <label className="text-sm block mb-2">主题</label>
              <div className="flex gap-2">
                <button
                  onClick={() => setSettings({ ...settings, theme: 'light' })}
                  className={`flex-1 py-2 px-4 rounded-lg text-sm flex items-center justify-center gap-2 transition-colors ${
                    settings.theme === 'light'
                      ? 'bg-oppo-sunrise-gold text-black'
                      : 'bg-white/10 text-white'
                  }`}
                >
                  <Sun className="w-4 h-4" />
                  浅色
                </button>
                <button
                  onClick={() => setSettings({ ...settings, theme: 'dark' })}
                  className={`flex-1 py-2 px-4 rounded-lg text-sm flex items-center justify-center gap-2 transition-colors ${
                    settings.theme === 'dark'
                      ? 'bg-oppo-sunrise-gold text-black'
                      : 'bg-white/10 text-white'
                  }`}
                >
                  <Moon className="w-4 h-4" />
                  深色
                </button>
                <button
                  onClick={() => setSettings({ ...settings, theme: 'auto' })}
                  className={`flex-1 py-2 px-4 rounded-lg text-sm flex items-center justify-center gap-2 transition-colors ${
                    settings.theme === 'auto'
                      ? 'bg-oppo-sunrise-gold text-black'
                      : 'bg-white/10 text-white'
                  }`}
                >
                  <Monitor className="w-4 h-4" />
                  自动
                </button>
              </div>
            </div>
          </div>
        </motion.section>

        {/* 预设选择 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">
            预设列表
          </h2>
          <div className="space-y-2">
            {presetExamples.map((preset, index) => (
              <motion.div
                key={preset.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.3 + index * 0.05 }}
                className={`card-oppo p-4 flex items-center gap-4 cursor-pointer transition-all ${
                  settings.currentPreset === preset.id 
                    ? 'ring-2 ring-oppo-sunrise-gold/50' 
                    : 'hover:bg-white/5'
                }`}
                onClick={() => setSettings({ 
                  ...settings, 
                  currentPreset: settings.currentPreset === preset.id ? null : preset.id 
                })}
              >
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                  settings.currentPreset === preset.id
                    ? 'bg-oppo-sunrise-gold/20'
                    : 'bg-white/5'
                }`}>
                  <Palette className={`w-5 h-5 ${
                    settings.currentPreset === preset.id
                      ? 'text-oppo-sunrise-gold'
                      : 'text-text-tertiary'
                  }`} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-sm">{preset.name}</p>
                  <p className="text-xs text-oppo-green mt-0.5">{preset.device}</p>
                  <p className="text-xs text-text-tertiary mt-0.5 truncate">{preset.params}</p>
                </div>
                {settings.currentPreset === preset.id && (
                  <div className="w-6 h-6 rounded-full bg-oppo-sunrise-gold flex items-center justify-center">
                    <span className="text-black text-xs">✓</span>
                  </div>
                )}
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* 使用指南 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">
            使用指南
          </h2>
          <div className="card-oppo p-4 space-y-4">
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-ocean-blue/20 flex items-center justify-center flex-shrink-0">
                <span className="text-ocean-blue text-sm font-bold">1</span>
              </div>
              <div>
                <p className="text-sm font-medium">启动悬浮窗</p>
                <p className="text-xs text-text-secondary mt-0.5">
                  点击"启动悬浮窗"按钮，悬浮窗口将出现在屏幕边缘
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-ocean-blue/20 flex items-center justify-center flex-shrink-0">
                <span className="text-ocean-blue text-sm font-bold">2</span>
              </div>
              <div>
                <p className="text-sm font-medium">打开相机应用</p>
                <p className="text-xs text-text-secondary mt-0.5">
                  悬浮窗会在其他应用上方显示，可在拍照时参考参数
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-ocean-blue/20 flex items-center justify-center flex-shrink-0">
                <span className="text-ocean-blue text-sm font-bold">3</span>
              </div>
              <div>
                <p className="text-sm font-medium">切换预设</p>
                <p className="text-xs text-text-secondary mt-0.5">
                  点击左右箭头或展开列表选择不同的预设参数
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-ocean-blue/20 flex items-center justify-center flex-shrink-0">
                <span className="text-ocean-blue text-sm font-bold">4</span>
              </div>
              <div>
                <p className="text-sm font-medium">拖动位置</p>
                <p className="text-xs text-text-secondary mt-0.5">
                  长按悬浮窗可拖动到屏幕任意位置
                </p>
              </div>
            </div>
          </div>
        </motion.section>

        {/* 支持的设备 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">
            支持的设备
          </h2>
          <div className="grid grid-cols-2 gap-4">
            <div className="card-oppo p-4 flex items-center gap-3">
              <Smartphone className="w-8 h-8 text-oppo-sunrise-gold" />
              <div>
                <p className="text-sm font-medium">ColorOS 14+</p>
                <p className="text-xs text-text-secondary">OPPO/一加</p>
              </div>
            </div>
            <div className="card-oppo p-4 flex items-center gap-3">
              <Smartphone className="w-8 h-8 text-ocean-blue" />
              <div>
                <p className="text-sm font-medium">OriginOS 4+</p>
                <p className="text-xs text-text-secondary">vivo/iQOO</p>
              </div>
            </div>
            <div className="card-oppo p-4 flex items-center gap-3">
              <Smartphone className="w-8 h-8 text-rose-gold" />
              <div>
                <p className="text-sm font-medium">MIUI 15+</p>
                <p className="text-xs text-text-secondary">小米/红米</p>
              </div>
            </div>
            <div className="card-oppo p-4 flex items-center gap-3">
              <Smartphone className="w-8 h-8 text-pure-green" />
              <div>
                <p className="text-sm font-medium">HarmonyOS 4+</p>
                <p className="text-xs text-text-secondary">华为</p>
              </div>
            </div>
          </div>
        </motion.section>
      </main>
    </div>
  );
}
