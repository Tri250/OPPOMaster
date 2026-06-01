import { useState } from 'react';
import {
  Camera,
  Sparkles,
  Palette,
  Share2,
  Settings,
  Image,
  Layers,
  Zap,
  Shield,
  Smartphone,
  ChevronRight,
  Star,
  Check
} from 'lucide-react';

const mockScreens = [
  {
    name: '主界面',
    icon: <Camera className="w-5 h-5" />,
    color: 'text-hasselblad'
  },
  {
    name: 'AI识别',
    icon: <Sparkles className="w-5 h-5" />,
    color: 'text-purple-400'
  },
  {
    name: '水印编辑',
    icon: <Image className="w-5 h-5" />,
    color: 'text-blue-400'
  },
  {
    name: '社交分享',
    icon: <Share2 className="w-5 h-5" />,
    color: 'text-green-400'
  },
  {
    name: '设置',
    icon: <Settings className="w-5 h-5" />,
    color: 'text-gray-400'
  },
  {
    name: '主题',
    icon: <Palette className="w-5 h-5" />,
    color: 'text-pink-400'
  }
];

export default function MobileDemoPage() {
  const [activeScreen, setActiveScreen] = useState(0);

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center p-4">
      {/* Device Frame */}
      <div className="relative">
        {/* Device Outer Frame */}
        <div className="bg-black rounded-[3rem] p-3 shadow-2xl shadow-hasselblad/30">
          <div className="bg-gray-800 rounded-[2.5rem] p-2 w-[375px] h-[812px] relative overflow-hidden">
            {/* Notch */}
            <div className="absolute top-2 left-1/2 -translate-x-1/2 w-32 h-6 bg-black rounded-b-xl z-20" />

            {/* Status Bar */}
            <div className="flex justify-between items-center px-6 pt-8 pb-2 text-xs text-white/70 z-10 relative">
              <span>9:41</span>
              <div className="flex gap-1">
                <div className="w-3 h-2 border border-white/70 rounded-sm" />
                <div className="w-2 h-2 border border-white/70 rounded-full" />
                <div className="w-4 h-2 border border-white/70 rounded-sm relative">
                  <div className="absolute right-0 top-0 bottom-0 w-3 bg-white/70 rounded-r-sm" />
                </div>
              </div>
            </div>

            {/* Screen Content */}
            <div className="w-full h-full rounded-[2rem] overflow-hidden bg-deep-space pt-2">
              {activeScreen === 0 && <HomeScreen />}
              {activeScreen === 1 && <AIScreen />}
              {activeScreen === 2 && <WatermarkScreen />}
              {activeScreen === 3 && <ShareScreen />}
              {activeScreen === 4 && <SettingsScreen />}
              {activeScreen === 5 && <ThemeScreen />}
            </div>

            {/* Bottom Navigation */}
            <div className="absolute bottom-0 left-0 right-0 bg-deep-space-light/95 backdrop-blur-lg border-t border-white/10">
              <div className="flex justify-around py-3">
                {mockScreens.map((screen, index) => (
                  <button
                    key={index}
                    onClick={() => setActiveScreen(index)}
                    className={`flex flex-col items-center gap-1 p-2 rounded-xl transition-all ${
                      activeScreen === index ? 'bg-hasselblad/20' : ''
                    }`}
                  >
                    <span className={activeScreen === index ? screen.color : 'text-white/40'}>
                      {screen.icon}
                    </span>
                    <span className={`text-[10px] ${
                      activeScreen === index ? screen.color : 'text-white/40'
                    }`}>
                      {screen.name}
                    </span>
                  </button>
                ))}
              </div>
              {/* Home Indicator */}
              <div className="w-32 h-1 bg-white/30 rounded-full mx-auto mb-2" />
            </div>
          </div>
        </div>

        {/* Side Info Panel */}
        <div className="absolute left-full ml-8 top-1/2 -translate-y-1/2 hidden lg:block">
          <div className="bg-white/5 backdrop-blur-lg border border-white/10 rounded-2xl p-6 w-64">
            <h3 className="text-lg font-semibold text-white mb-4">{mockScreens[activeScreen].name}</h3>
            <p className="text-white/60 text-sm mb-4">点击底部导航切换界面</p>
            <div className="space-y-2">
              {mockScreens.map((screen, index) => (
                <button
                  key={index}
                  onClick={() => setActiveScreen(index)}
                  className={`w-full flex items-center gap-3 p-3 rounded-xl text-left transition-all ${
                    activeScreen === index
                      ? 'bg-hasselblad/20 border border-hasselblad/30'
                      : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <span className={activeScreen === index ? screen.color : 'text-white/40'}>
                    {screen.icon}
                  </span>
                  <span className={`text-sm ${
                    activeScreen === index ? 'text-white' : 'text-white/60'
                  }`}>
                    {screen.name}
                  </span>
                  {activeScreen === index && <Check className="w-4 h-4 ml-auto text-oppo-green" />}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function HomeScreen() {
  return (
    <div className="h-full overflow-y-auto pb-20">
      {/* Header */}
      <div className="p-4">
        <h1 className="text-2xl font-bold gradient-text mb-2">哈苏影像</h1>
        <p className="text-white/60 text-sm">HNCS认证预设 · 专业参数助手</p>
      </div>

      {/* Featured Presets */}
      <div className="px-4 mb-6">
        <div className="flex justify-between items-center mb-3">
          <h2 className="font-semibold text-white">精选预设</h2>
          <ChevronRight className="w-4 h-4 text-white/40" />
        </div>
        <div className="grid grid-cols-2 gap-3">
          {[
            { name: '自然色彩', icon: '🎨' },
            { name: '人像模式', icon: '👤' },
            { name: '风景拍摄', icon: '🏔️' },
            { name: '街拍纪实', icon: '🌆' }
          ].map((preset, i) => (
            <div key={i} className="bg-white/5 rounded-xl p-4 border border-white/10">
              <div className="text-3xl mb-2">{preset.icon}</div>
              <div className="font-medium text-sm text-white">{preset.name}</div>
              <div className="flex items-center gap-1 mt-1">
                <Star className="w-3 h-3 text-hasselblad" />
                <span className="text-[10px] text-white/60">HNCS认证</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="px-4 mb-6">
        <h2 className="font-semibold text-white mb-3">快捷功能</h2>
        <div className="grid grid-cols-4 gap-2">
          {[
            { name: 'AI识别', icon: '✨' },
            { name: '水印', icon: '💧' },
            { name: '相机', icon: '📷' },
            { name: '分享', icon: '📤' }
          ].map((action, i) => (
            <div key={i} className="bg-white/5 rounded-xl p-3 text-center border border-white/10">
              <div className="text-2xl mb-1">{action.icon}</div>
              <div className="text-[10px] text-white/70">{action.name}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Hot Presets */}
      <div className="px-4">
        <h2 className="font-semibold text-white mb-3">热门推荐</h2>
        <div className="space-y-2">
          {[
            { name: '徕卡经典', desc: '德味色彩表现', tag: '热门' },
            { name: '哈苏自然', desc: '真实色彩还原', tag: '推荐' },
            { name: '富士胶片', desc: '胶片模拟效果', tag: '精选' }
          ].map((item, i) => (
            <div key={i} className="bg-white/5 rounded-xl p-3 flex items-center gap-3 border border-white/10">
              <div className="w-10 h-10 bg-hasselblad/20 rounded-full flex items-center justify-center text-xl">
                📸
              </div>
              <div className="flex-1">
                <div className="font-medium text-sm text-white">{item.name}</div>
                <div className="text-xs text-white/50">{item.desc}</div>
              </div>
              <span className="bg-hasselblad/20 text-hasselblad text-[10px] px-2 py-1 rounded-full">
                {item.tag}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function AIScreen() {
  return (
    <div className="h-full overflow-y-auto pb-20">
      <div className="p-4">
        <h1 className="text-2xl font-bold text-purple-400 mb-2">AI场景识别</h1>
        <p className="text-white/60 text-sm">智能识别24种拍摄场景</p>
      </div>

      <div className="px-4 mb-6">
        <div className="bg-gradient-to-br from-purple-500/20 to-blue-500/20 rounded-2xl p-6 text-center border border-purple-500/20">
          <div className="w-20 h-20 bg-purple-500/30 rounded-full flex items-center justify-center mx-auto mb-4 animate-pulse">
            <Sparkles className="w-10 h-10 text-purple-400" />
          </div>
          <h3 className="text-lg font-semibold text-white mb-2">正在识别...</h3>
          <p className="text-white/60 text-sm">请上传或拍摄照片</p>
        </div>
      </div>

      <div className="px-4">
        <h2 className="font-semibold text-white mb-3">支持场景</h2>
        <div className="grid grid-cols-3 gap-2">
          {['人像', '风景', '夜景', '美食', '宠物', '建筑', '微距', '运动', '日落'].map((scene, i) => (
            <div key={i} className="bg-white/5 rounded-xl p-3 text-center border border-white/10">
              <div className="text-xl mb-1">{['👤', '🏔️', '🌃', '🍜', '🐕', '🏢', '🔍', '⚡', '🌅'][i]}</div>
              <div className="text-[10px] text-white/70">{scene}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function WatermarkScreen() {
  return (
    <div className="h-full overflow-y-auto pb-20">
      <div className="p-4">
        <h1 className="text-2xl font-bold text-blue-400 mb-2">水印编辑</h1>
        <p className="text-white/60 text-sm">专业水印模板 · 自定义样式</p>
      </div>

      <div className="px-4 mb-6">
        <div className="bg-white/5 rounded-xl aspect-video flex items-center justify-center border border-white/10 border-dashed">
          <div className="text-center">
            <div className="text-4xl mb-2">📷</div>
            <p className="text-white/50 text-sm">点击添加照片</p>
          </div>
        </div>
      </div>

      <div className="px-4 mb-6">
        <div className="flex gap-2">
          {['文字', '图片', '模板'].map((tab, i) => (
            <button key={i} className={`flex-1 py-2 rounded-lg text-sm font-medium ${
              i === 0 ? 'bg-hasselblad text-deep-space' : 'bg-white/5 text-white/70'
            }`}>
              {tab}
            </button>
          ))}
        </div>
      </div>

      <div className="px-4">
        <h2 className="font-semibold text-white mb-3">推荐模板</h2>
        <div className="grid grid-cols-2 gap-2">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-white/5 rounded-xl aspect-[4/3] border border-white/10" />
          ))}
        </div>
      </div>
    </div>
  );
}

function ShareScreen() {
  return (
    <div className="h-full overflow-y-auto pb-20">
      <div className="p-4">
        <h1 className="text-2xl font-bold text-green-400 mb-2">社交分享</h1>
        <p className="text-white/60 text-sm">一键分享到各大平台</p>
      </div>

      <div className="px-4 mb-6">
        <div className="grid grid-cols-4 gap-3">
          {[
            { name: '微信', icon: '💬' },
            { name: '朋友圈', icon: '📱' },
            { name: 'QQ', icon: '🐧' },
            { name: '微博', icon: '📄' },
            { name: '抖音', icon: '🎵' },
            { name: '小红书', icon: '📖' },
            { name: '原图', icon: '💯' },
            { name: '更多', icon: '⋮' }
          ].map((app, i) => (
            <div key={i} className="text-center">
              <div className="w-14 h-14 bg-white/10 rounded-2xl flex items-center justify-center mx-auto mb-2 border border-white/10">
                <span className="text-2xl">{app.icon}</span>
              </div>
              <span className="text-[10px] text-white/70">{app.name}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="px-4">
        <div className="bg-white/5 rounded-xl p-4 border border-white/10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-hasselblad/20 rounded-full flex items-center justify-center text-xl">
              📸
            </div>
            <div className="flex-1">
              <div className="font-medium text-sm text-white">照片.jpg</div>
              <div className="text-xs text-white/50">3.2 MB · 4032×3024</div>
            </div>
            <Check className="w-5 h-5 text-oppo-green" />
          </div>
        </div>
      </div>
    </div>
  );
}

function SettingsScreen() {
  return (
    <div className="h-full overflow-y-auto pb-20">
      <div className="p-4">
        <h1 className="text-2xl font-bold text-white mb-2">设置</h1>
      </div>

      <div className="px-4 space-y-2">
        {[
          { name: '悬浮窗开关', desc: '相机参数悬浮窗', hasToggle: true },
          { name: '启动时相机', desc: '打开相机时自动启动', hasToggle: true },
          { name: '自动保存', desc: '照片自动添加水印', hasToggle: true },
          { name: '清除缓存', desc: '清理临时文件', hasToggle: false },
          { name: '隐私设置', desc: '权限管理', hasToggle: false }
        ].map((item, i) => (
          <div key={i} className="bg-white/5 rounded-xl p-4 flex items-center justify-between border border-white/10">
            <div>
              <div className="font-medium text-sm text-white">{item.name}</div>
              <div className="text-xs text-white/50">{item.desc}</div>
            </div>
            {item.hasToggle && (
              <div className="w-12 h-7 bg-hasselblad rounded-full relative">
                <div className="absolute right-1 top-1 w-5 h-5 bg-white rounded-full" />
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="px-4 mt-8">
        <div className="text-center text-white/30 text-xs">
          小O帮帮 v1.2.1
        </div>
      </div>
    </div>
  );
}

function ThemeScreen() {
  return (
    <div className="h-full overflow-y-auto pb-20">
      <div className="p-4">
        <h1 className="text-2xl font-bold text-pink-400 mb-2">主题设置</h1>
        <p className="text-white/60 text-sm">自定义您的应用外观</p>
      </div>

      <div className="px-4 space-y-2">
        {[
          { name: '浅色主题', icon: '☀️', active: false },
          { name: '深色主题', icon: '🌙', active: true },
          { name: '跟随系统', icon: '📱', active: false },
          { name: '护眼模式', icon: '👁️', active: false }
        ].map((theme, i) => (
          <div key={i} className={`bg-white/5 rounded-xl p-4 flex items-center gap-3 border border-white/10 ${
            theme.active ? 'border-hasselblad/30 bg-hasselblad/10' : ''
          }`}>
            <span className="text-2xl">{theme.icon}</span>
            <span className="flex-1 font-medium text-sm text-white">{theme.name}</span>
            {theme.active && <Check className="w-5 h-5 text-oppo-green" />}
          </div>
        ))}
      </div>
    </div>
  );
}
