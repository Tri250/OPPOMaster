import React, { useState } from 'react';
import { 
  Search, 
  Settings, 
  Heart, 
  ChevronLeft, 
  Filter,
  Smartphone,
  Moon,
  Sun,
  Monitor,
  Star,
  Camera,
  Aperture,
  Gauge,
  Thermometer
} from 'lucide-react';

interface Preset {
  id: string;
  name: string;
  deviceModel: string;
  isFavorite: boolean;
  isHNCS: boolean;
  params: {
    mode: string;
    filter: string;
    iso: number;
    shutter: string;
    ev: string;
    wb: string;
  };
}

const mockPresets: Preset[] = [
  {
    id: '1',
    name: '哈苏 X2D | 慵懒午后的佛罗伦萨',
    deviceModel: 'Find X8 Pro',
    isFavorite: true,
    isHNCS: true,
    params: { mode: 'master', filter: '复古', iso: 200, shutter: '1/250', ev: '+0.3', wb: '5600K' },
  },
  {
    id: '2',
    name: '京都夜色 | 霓虹光斑',
    deviceModel: 'Find X8 Ultra',
    isFavorite: false,
    isHNCS: false,
    params: { mode: 'master', filter: '夜景', iso: 800, shutter: '1/30', ev: '-0.7', wb: '4200K' },
  },
  {
    id: '3',
    name: '北欧森林 | 自然清新',
    deviceModel: 'Reno 12 Pro',
    isFavorite: false,
    isHNCS: true,
    params: { mode: 'master', filter: '自然', iso: 100, shutter: '1/500', ev: '0', wb: '5200K' },
  },
  {
    id: '4',
    name: '海边日落 | 温暖橙调',
    deviceModel: 'Find X7 Ultra',
    isFavorite: true,
    isHNCS: true,
    params: { mode: 'master', filter: '暖调', iso: 100, shutter: '1/200', ev: '+0.7', wb: '6000K' },
  },
  {
    id: '5',
    name: '城市街头 | 黑白纪实',
    deviceModel: 'Find X8',
    isFavorite: false,
    isHNCS: false,
    params: { mode: 'master', filter: '黑白', iso: 400, shutter: '1/1000', ev: '0', wb: '自动' },
  },
  {
    id: '6',
    name: '春日樱花 | 粉调柔焦',
    deviceModel: 'Reno 12',
    isFavorite: false,
    isHNCS: true,
    params: { mode: 'master', filter: '人像', iso: 200, shutter: '1/320', ev: '+0.3', wb: '5800K' },
  },
];

type Screen = 'home' | 'detail' | 'settings';
type ThemeMode = 'system' | 'light' | 'dark';
type FilterType = 'all' | 'favorites' | 'hncs' | 'findx' | 'reno';

export function AppPreview() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [selectedPreset, setSelectedPreset] = useState<Preset | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<FilterType>('all');
  const [themeMode, setThemeMode] = useState<ThemeMode>('system');
  const [fluidCloudEnabled, setFluidCloudEnabled] = useState(true);
  const [overlayEnabled, setOverlayEnabled] = useState(false);
  const [favorites, setFavorites] = useState<string[]>(['1', '4']);
  const [snackbar, setSnackbar] = useState<string | null>(null);

  const isDark = themeMode === 'dark' || (themeMode === 'system' && true);

  const showSnackbar = (message: string) => {
    setSnackbar(message);
    setTimeout(() => setSnackbar(null), 2000);
  };

  const toggleFavorite = (presetId: string) => {
    if (favorites.includes(presetId)) {
      setFavorites(favorites.filter(id => id !== presetId));
      showSnackbar('已取消收藏');
    } else {
      setFavorites([...favorites, presetId]);
      showSnackbar('已添加收藏');
    }
  };

  const filteredPresets = mockPresets.filter(preset => {
    const matchesSearch = preset.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter = 
      filterType === 'all' ? true :
      filterType === 'favorites' ? favorites.includes(preset.id) :
      filterType === 'hncs' ? preset.isHNCS :
      filterType === 'findx' ? preset.deviceModel.includes('Find X') :
      filterType === 'reno' ? preset.deviceModel.includes('Reno') :
      true;
    return matchesSearch && matchesFilter;
  });

  const filters: { type: FilterType; label: string }[] = [
    { type: 'all', label: '全部' },
    { type: 'favorites', label: '收藏' },
    { type: 'hncs', label: 'HNCS' },
    { type: 'findx', label: 'Find X' },
    { type: 'reno', label: 'Reno' },
  ];

  // Home Screen
  if (currentScreen === 'home') {
    return (
      <div className={`w-full h-full flex flex-col ${isDark ? 'bg-gray-900' : 'bg-gray-50'}`}>
        {/* Header */}
        <div className={`px-4 py-3 border-b ${isDark ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200'}`}>
          <div className="flex items-center justify-between">
            <h1 className={`text-xl font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>
              <span className="text-blue-500">O</span>Master
            </h1>
            <button 
              onClick={() => setCurrentScreen('settings')}
              className={`p-2 rounded-full ${isDark ? 'hover:bg-gray-700' : 'hover:bg-gray-100'}`}
            >
              <Settings className={`w-5 h-5 ${isDark ? 'text-gray-300' : 'text-gray-600'}`} />
            </button>
          </div>
        </div>

        {/* Search Bar */}
        <div className={`px-4 py-3 ${isDark ? 'bg-gray-800' : 'bg-white'}`}>
          <div className={`flex items-center gap-2 px-3 py-2 rounded-lg ${isDark ? 'bg-gray-700' : 'bg-gray-100'}`}>
            <Search className={`w-5 h-5 ${isDark ? 'text-gray-400' : 'text-gray-500'}`} />
            <input
              type="text"
              placeholder="搜索预设..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className={`flex-1 bg-transparent outline-none text-sm ${isDark ? 'text-white placeholder-gray-400' : 'text-gray-900 placeholder-gray-500'}`}
            />
            {searchQuery && (
              <button onClick={() => setSearchQuery('')} className="text-gray-400 hover:text-gray-300">
                ×
              </button>
            )}
          </div>
        </div>

        {/* Filter Chips */}
        <div className={`px-4 py-2 flex gap-2 overflow-x-auto ${isDark ? 'bg-gray-800' : 'bg-white'}`}>
          {filters.map(filter => (
            <button
              key={filter.type}
              onClick={() => setFilterType(filter.type)}
              className={`px-3 py-1.5 rounded-full text-sm whitespace-nowrap transition-all ${
                filterType === filter.type
                  ? 'bg-blue-500 text-white'
                  : isDark ? 'bg-gray-700 text-gray-300 hover:bg-gray-600' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>

        {/* Preset List */}
        <div className={`flex-1 overflow-auto p-4 ${isDark ? 'bg-gray-900' : 'bg-gray-50'}`}>
          <div className="space-y-3">
            {filteredPresets.map(preset => (
              <div
                key={preset.id}
                onClick={() => {
                  setSelectedPreset(preset);
                  setCurrentScreen('detail');
                }}
                className={`p-4 rounded-xl cursor-pointer transition-all ${
                  isDark ? 'bg-gray-800 hover:bg-gray-750' : 'bg-white hover:bg-gray-50 shadow-sm'
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <h3 className={`font-medium mb-1 ${isDark ? 'text-white' : 'text-gray-900'}`}>
                      {preset.name}
                    </h3>
                    <div className="flex items-center gap-2">
                      <span className={`text-xs px-2 py-0.5 rounded ${isDark ? 'bg-gray-700 text-gray-300' : 'bg-gray-200 text-gray-600'}`}>
                        {preset.deviceModel}
                      </span>
                      {preset.isHNCS && (
                        <span className="text-xs px-2 py-0.5 rounded bg-orange-500 text-white font-medium">
                          HNCS
                        </span>
                      )}
                    </div>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleFavorite(preset.id);
                    }}
                    className="p-2"
                  >
                    <Heart 
                      className={`w-5 h-5 ${favorites.includes(preset.id) ? 'fill-red-500 text-red-500' : isDark ? 'text-gray-500' : 'text-gray-400'}`} 
                    />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Snackbar */}
        {snackbar && (
          <div className="absolute bottom-20 left-1/2 -translate-x-1/2 px-4 py-2 bg-gray-800 text-white rounded-lg shadow-lg text-sm">
            {snackbar}
          </div>
        )}
      </div>
    );
  }

  // Detail Screen
  if (currentScreen === 'detail' && selectedPreset) {
    return (
      <div className={`w-full h-full flex flex-col ${isDark ? 'bg-gray-900' : 'bg-gray-50'}`}>
        {/* Header */}
        <div className={`px-4 py-3 border-b flex items-center gap-3 ${isDark ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200'}`}>
          <button 
            onClick={() => setCurrentScreen('home')}
            className={`p-2 rounded-full ${isDark ? 'hover:bg-gray-700' : 'hover:bg-gray-100'}`}
          >
            <ChevronLeft className={`w-5 h-5 ${isDark ? 'text-gray-300' : 'text-gray-600'}`} />
          </button>
          <h1 className={`text-lg font-semibold flex-1 truncate ${isDark ? 'text-white' : 'text-gray-900'}`}>
            预设详情
          </h1>
          <button
            onClick={() => toggleFavorite(selectedPreset.id)}
            className="p-2"
          >
            <Heart 
              className={`w-5 h-5 ${favorites.includes(selectedPreset.id) ? 'fill-red-500 text-red-500' : isDark ? 'text-gray-500' : 'text-gray-400'}`} 
            />
          </button>
        </div>

        {/* Content */}
        <div className={`flex-1 overflow-auto p-4 ${isDark ? 'bg-gray-900' : 'bg-gray-50'}`}>
          {/* Cover Image Placeholder */}
          <div className="w-full h-48 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 mb-4 flex items-center justify-center">
            <Camera className="w-16 h-16 text-white/50" />
          </div>

          {/* Title */}
          <h2 className={`text-xl font-bold mb-2 ${isDark ? 'text-white' : 'text-gray-900'}`}>
            {selectedPreset.name}
          </h2>
          
          <div className="flex items-center gap-2 mb-6">
            <span className={`text-sm px-3 py-1 rounded-full ${isDark ? 'bg-gray-700 text-gray-300' : 'bg-gray-200 text-gray-700'}`}>
              {selectedPreset.deviceModel}
            </span>
            {selectedPreset.isHNCS && (
              <span className="text-sm px-3 py-1 rounded-full bg-orange-500 text-white font-medium">
                HNCS 认证
              </span>
            )}
          </div>

          {/* Camera Params */}
          <div className={`p-4 rounded-xl mb-4 ${isDark ? 'bg-gray-800' : 'bg-white shadow-sm'}`}>
            <h3 className={`text-sm font-medium mb-3 flex items-center gap-2 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>
              <Aperture className="w-4 h-4" />
              相机参数
            </h3>
            <div className="grid grid-cols-2 gap-3">
              <div className={`p-3 rounded-lg ${isDark ? 'bg-gray-700' : 'bg-gray-100'}`}>
                <div className={`text-xs mb-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>模式</div>
                <div className={`font-medium ${isDark ? 'text-white' : 'text-gray-900'}`}>{selectedPreset.params.mode}</div>
              </div>
              <div className={`p-3 rounded-lg ${isDark ? 'bg-gray-700' : 'bg-gray-100'}`}>
                <div className={`text-xs mb-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>滤镜</div>
                <div className={`font-medium ${isDark ? 'text-white' : 'text-gray-900'}`}>{selectedPreset.params.filter}</div>
              </div>
              <div className={`p-3 rounded-lg ${isDark ? 'bg-gray-700' : 'bg-gray-100'}`}>
                <div className={`text-xs mb-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>ISO</div>
                <div className={`font-medium ${isDark ? 'text-white' : 'text-gray-900'}`}>{selectedPreset.params.iso}</div>
              </div>
              <div className={`p-3 rounded-lg ${isDark ? 'bg-gray-700' : 'bg-gray-100'}`}>
                <div className={`text-xs mb-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>快门</div>
                <div className={`font-medium ${isDark ? 'text-white' : 'text-gray-900'}`}>{selectedPreset.params.shutter}</div>
              </div>
              <div className={`p-3 rounded-lg ${isDark ? 'bg-gray-700' : 'bg-gray-100'}`}>
                <div className={`text-xs mb-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>EV</div>
                <div className={`font-medium ${isDark ? 'text-white' : 'text-gray-900'}`}>{selectedPreset.params.ev}</div>
              </div>
              <div className={`p-3 rounded-lg ${isDark ? 'bg-gray-700' : 'bg-gray-100'}`}>
                <div className={`text-xs mb-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>白平衡</div>
                <div className={`font-medium ${isDark ? 'text-white' : 'text-gray-900'}`}>{selectedPreset.params.wb}</div>
              </div>
            </div>
          </div>

          {/* Apply Button */}
          <button className="w-full py-3 bg-blue-500 hover:bg-blue-600 text-white font-medium rounded-xl transition-colors">
            应用到相机
          </button>
        </div>

        {/* Snackbar */}
        {snackbar && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 px-4 py-2 bg-gray-800 text-white rounded-lg shadow-lg text-sm">
            {snackbar}
          </div>
        )}
      </div>
    );
  }

  // Settings Screen
  if (currentScreen === 'settings') {
    return (
      <div className={`w-full h-full flex flex-col ${isDark ? 'bg-gray-900' : 'bg-gray-50'}`}>
        {/* Header */}
        <div className={`px-4 py-3 border-b flex items-center gap-3 ${isDark ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200'}`}>
          <button 
            onClick={() => setCurrentScreen('home')}
            className={`p-2 rounded-full ${isDark ? 'hover:bg-gray-700' : 'hover:bg-gray-100'}`}
          >
            <ChevronLeft className={`w-5 h-5 ${isDark ? 'text-gray-300' : 'text-gray-600'}`} />
          </button>
          <h1 className={`text-lg font-semibold ${isDark ? 'text-white' : 'text-gray-900'}`}>
            设置
          </h1>
        </div>

        {/* Content */}
        <div className={`flex-1 overflow-auto p-4 ${isDark ? 'bg-gray-900' : 'bg-gray-50'}`}>
          {/* Appearance Section */}
          <div className="mb-6">
            <h2 className={`text-sm font-medium mb-3 px-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>
              外观
            </h2>
            <div className={`rounded-xl overflow-hidden ${isDark ? 'bg-gray-800' : 'bg-white shadow-sm'}`}>
              <button
                onClick={() => setThemeMode('system')}
                className={`w-full flex items-center justify-between px-4 py-3 border-b ${isDark ? 'border-gray-700' : 'border-gray-100'}`}
              >
                <div className="flex items-center gap-3">
                  <Monitor className={`w-5 h-5 ${isDark ? 'text-gray-400' : 'text-gray-500'}`} />
                  <span className={isDark ? 'text-white' : 'text-gray-900'}>跟随系统</span>
                </div>
                <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                  themeMode === 'system' ? 'border-blue-500' : isDark ? 'border-gray-600' : 'border-gray-300'
                }`}>
                  {themeMode === 'system' && <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />}
                </div>
              </button>
              <button
                onClick={() => setThemeMode('light')}
                className={`w-full flex items-center justify-between px-4 py-3 border-b ${isDark ? 'border-gray-700' : 'border-gray-100'}`}
              >
                <div className="flex items-center gap-3">
                  <Sun className={`w-5 h-5 ${isDark ? 'text-gray-400' : 'text-gray-500'}`} />
                  <span className={isDark ? 'text-white' : 'text-gray-900'}>浅色模式</span>
                </div>
                <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                  themeMode === 'light' ? 'border-blue-500' : isDark ? 'border-gray-600' : 'border-gray-300'
                }`}>
                  {themeMode === 'light' && <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />}
                </div>
              </button>
              <button
                onClick={() => setThemeMode('dark')}
                className="w-full flex items-center justify-between px-4 py-3"
              >
                <div className="flex items-center gap-3">
                  <Moon className={`w-5 h-5 ${isDark ? 'text-gray-400' : 'text-gray-500'}`} />
                  <span className={isDark ? 'text-white' : 'text-gray-900'}>深色模式</span>
                </div>
                <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                  themeMode === 'dark' ? 'border-blue-500' : isDark ? 'border-gray-600' : 'border-gray-300'
                }`}>
                  {themeMode === 'dark' && <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />}
                </div>
              </button>
            </div>
          </div>

          {/* System Capabilities Section */}
          <div className="mb-6">
            <h2 className={`text-sm font-medium mb-3 px-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>
              系统能力
            </h2>
            <div className={`rounded-xl overflow-hidden ${isDark ? 'bg-gray-800' : 'bg-white shadow-sm'}`}>
              <div className={`flex items-center justify-between px-4 py-3 border-b ${isDark ? 'border-gray-700' : 'border-gray-100'}`}>
                <div>
                  <div className={isDark ? 'text-white' : 'text-gray-900'}>流体云胶囊</div>
                  <div className={`text-xs mt-0.5 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>
                    在系统侧边栏显示当前选中的预设
                  </div>
                </div>
                <button
                  onClick={() => setFluidCloudEnabled(!fluidCloudEnabled)}
                  className={`w-12 h-6 rounded-full transition-colors ${
                    fluidCloudEnabled ? 'bg-blue-500' : isDark ? 'bg-gray-600' : 'bg-gray-300'
                  }`}
                >
                  <div className={`w-5 h-5 rounded-full bg-white shadow-sm transition-transform ${
                    fluidCloudEnabled ? 'translate-x-6' : 'translate-x-0.5'
                  }`} />
                </button>
              </div>
              <div className="flex items-center justify-between px-4 py-3">
                <div>
                  <div className={isDark ? 'text-white' : 'text-gray-900'}>悬浮窗（降级方案）</div>
                  <div className={`text-xs mt-0.5 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>
                    仅适用于不支持流体云的旧系统
                  </div>
                </div>
                <button
                  onClick={() => setOverlayEnabled(!overlayEnabled)}
                  className={`w-12 h-6 rounded-full transition-colors ${
                    overlayEnabled ? 'bg-blue-500' : isDark ? 'bg-gray-600' : 'bg-gray-300'
                  }`}
                >
                  <div className={`w-5 h-5 rounded-full bg-white shadow-sm transition-transform ${
                    overlayEnabled ? 'translate-x-6' : 'translate-x-0.5'
                  }`} />
                </button>
              </div>
            </div>
          </div>

          {/* About Section */}
          <div>
            <h2 className={`text-sm font-medium mb-3 px-1 ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>
              关于
            </h2>
            <div className={`p-4 rounded-xl ${isDark ? 'bg-gray-800' : 'bg-white shadow-sm'}`}>
              <div className="flex items-center gap-3 mb-2">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
                  <span className="text-white font-bold text-lg">O</span>
                </div>
                <div>
                  <div className={`font-semibold ${isDark ? 'text-white' : 'text-gray-900'}`}>OMaster</div>
                  <div className={`text-xs ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>版本 1.0.0</div>
                </div>
              </div>
              <p className={`text-sm mt-3 ${isDark ? 'text-gray-400' : 'text-gray-600'}`}>
                OPPO 哈苏影像系统级参数中枢
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return null;
}
