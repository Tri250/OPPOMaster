import { useState } from 'react';
import { Camera, Image, Settings, Sparkles, Share2, Palette } from 'lucide-react';

const mockScreens = [
  {
    name: '主界面',
    icon: <Camera className="w-5 h-5" />,
    color: 'text-hasselblad',
    description: 'HNCS认证预设展示，专业色彩推荐'
  },
  {
    name: 'AI识别',
    icon: <Sparkles className="w-5 h-5" />,
    color: 'text-purple-400',
    description: '24种场景智能识别，参数自动匹配'
  },
  {
    name: '水印编辑',
    icon: <Image className="w-5 h-5" />,
    color: 'text-blue-400',
    description: '专业模板，自定义文字和图片水印'
  },
  {
    name: '社交分享',
    icon: <Share2 className="w-5 h-5" />,
    color: 'text-green-400',
    description: '9+分享渠道，一键发送到各平台'
  },
  {
    name: '设置',
    icon: <Settings className="w-5 h-5" />,
    color: 'text-gray-400',
    description: '主题切换，悬浮窗设置，隐私管理'
  },
  {
    name: '主题',
    icon: <Palette className="w-5 h-5" />,
    color: 'text-pink-400',
    description: '深色/浅色/跟随系统，护眼模式'
  }
];

export default function AndroidDevicePreview() {
  const [activeScreen, setActiveScreen] = useState(0);

  return (
    <div className="flex flex-col lg:flex-row items-center justify-center gap-12 py-16">
      {/* Device Mockup */}
      <div className="relative">
        <div className="bg-black rounded-[3rem] p-3 shadow-2xl shadow-hasselblad/20">
          <div className="bg-deep-space-light rounded-[2.5rem] p-2 w-72 h-[500px] relative overflow-hidden">
            {/* Notch */}
            <div className="absolute top-2 left-1/2 -translate-x-1/2 w-32 h-6 bg-black rounded-b-xl z-20" />
            
            {/* Screen Content */}
            <div className="w-full h-full rounded-[2rem] overflow-hidden bg-deep-space">
              {/* Status Bar */}
              <div className="flex justify-between items-center px-6 pt-8 pb-2 text-xs text-white/70">
                <span>9:41</span>
                <div className="flex gap-1">
                  <div className="w-3 h-2 border border-white/70 rounded-sm" />
                  <div className="w-2 h-2 border border-white/70 rounded-full" />
                  <div className="w-4 h-2 border border-white/70 rounded-sm relative">
                    <div className="absolute right-0 top-0 bottom-0 w-2 bg-white/70 rounded-r-sm" />
                  </div>
                </div>
              </div>

              {/* Screen Content */}
              <div className="p-4 h-full overflow-y-auto">
                {activeScreen === 0 && (
                  <div className="space-y-4">
                    <h3 className="text-lg font-bold gradient-text">哈苏预设</h3>
                    <div className="grid grid-cols-2 gap-3">
                      {[1,2,3,4].map(i => (
                        <div key={i} className="bg-white/10 rounded-xl p-3 aspect-square flex items-center justify-center">
                          <span className="text-3xl">🎨</span>
                        </div>
                      ))}
                    </div>
                    <h4 className="text-sm font-semibold mt-4 text-white/80">热门预设</h4>
                    <div className="space-y-2">
                      {['自然色彩', '肖像模式', '风景模式', '街拍模式'].map((name, i) => (
                        <div key={i} className="flex items-center gap-3 bg-white/5 p-3 rounded-xl">
                          <div className="w-10 h-10 bg-hasselblad/20 rounded-full flex items-center justify-center">
                            <span>✨</span>
                          </div>
                          <span className="text-sm">{name}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {activeScreen === 1 && (
                  <div className="text-center space-y-4 pt-8">
                    <div className="w-24 h-24 bg-purple-500/20 rounded-full flex items-center justify-center mx-auto animate-pulse">
                      <Sparkles className="w-12 h-12 text-purple-400" />
                    </div>
                    <h3 className="text-lg font-bold text-white">AI场景识别中...</h3>
                    <p className="text-sm text-white/60">智能分析您的拍摄场景</p>
                    <div className="bg-white/10 rounded-xl p-4 mt-4">
                      <div className="flex items-center gap-2 text-sm">
                        <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                        <span>检测到：风景模式</span>
                      </div>
                    </div>
                  </div>
                )}

                {activeScreen === 2 && (
                  <div className="space-y-4">
                    <h3 className="text-lg font-bold gradient-text">水印编辑</h3>
                    <div className="bg-white/10 rounded-xl aspect-video flex items-center justify-center">
                      <span className="text-4xl">📷</span>
                    </div>
                    <div className="flex gap-2">
                      {['文字', '图片', '模板'].map((btn, i) => (
                        <button key={i} className="flex-1 py-2 bg-hasselblad/20 text-hasselblad rounded-lg text-sm">
                          {btn}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {activeScreen === 3 && (
                  <div className="space-y-4">
                    <h3 className="text-lg font-bold gradient-text">分享到</h3>
                    <div className="grid grid-cols-4 gap-4">
                      {['微信', '朋友圈', 'QQ', '微博', '抖音', '小红书', '原图', '更多'].map((app, i) => (
                        <div key={i} className="text-center">
                          <div className="w-12 h-12 bg-white/10 rounded-2xl flex items-center justify-center mx-auto mb-2">
                            <span className="text-2xl">{['💬', '📱', '🐧', '📄', '🎵', '📖', '💯', '⋮'][i]}</span>
                          </div>
                          <span className="text-xs text-white/70">{app}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {activeScreen === 4 && (
                  <div className="space-y-3">
                    <h3 className="text-lg font-bold gradient-text">设置</h3>
                    {['悬浮窗开关', '启动时相机', '自动保存', '清除缓存', '隐私设置'].map((item, i) => (
                      <div key={i} className="flex items-center justify-between bg-white/5 p-3 rounded-xl">
                        <span className="text-sm">{item}</span>
                        <div className="w-10 h-6 bg-hasselblad rounded-full relative">
                          <div className="absolute right-0.5 top-0.5 w-5 h-5 bg-white rounded-full" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {activeScreen === 5 && (
                  <div className="space-y-4">
                    <h3 className="text-lg font-bold gradient-text">主题</h3>
                    {[
                      { name: '浅色', icon: '☀️', active: false },
                      { name: '深色', icon: '🌙', active: true },
                      { name: '跟随系统', icon: '📱', active: false },
                      { name: '护眼模式', icon: '👁️', active: false }
                    ].map((theme, i) => (
                      <div key={i} className={`flex items-center gap-3 p-3 rounded-xl ${theme.active ? 'bg-hasselblad/20 border border-hasselblad/30' : 'bg-white/5'}`}>
                        <span className="text-2xl">{theme.icon}</span>
                        <span className="text-sm">{theme.name}</span>
                        {theme.active && <span className="ml-auto text-hasselblad text-sm">✓</span>}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Bottom Navigation */}
            <div className="absolute bottom-4 left-0 right-0 flex justify-center gap-6 px-6">
              {mockScreens.map((screen, index) => (
                <button
                  key={index}
                  onClick={() => setActiveScreen(index)}
                  className={`flex flex-col items-center gap-1 transition-all ${
                    activeScreen === index ? screen.color : 'text-white/40'
                  }`}
                >
                  {screen.icon}
                  <span className="text-xs">{screen.name.split('')[0]}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Feature Description */}
      <div className="lg:max-w-md">
        <h3 className="text-2xl font-bold mb-2">{mockScreens[activeScreen].name}</h3>
        <p className="text-white/60 mb-6">{mockScreens[activeScreen].description}</p>
        
        <div className="space-y-3">
          {mockScreens.map((screen, index) => (
            <button
              key={index}
              onClick={() => setActiveScreen(index)}
              className={`w-full flex items-center gap-4 p-4 rounded-xl transition-all ${
                activeScreen === index 
                  ? 'bg-hasselblad/20 border border-hasselblad/30' 
                  : 'bg-white/5 hover:bg-white/10'
              }`}
            >
              <div className={`${screen.color}`}>
                {screen.icon}
              </div>
              <div className="text-left">
                <div className="font-medium">{screen.name}</div>
                <div className="text-sm text-white/50">{screen.description}</div>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
