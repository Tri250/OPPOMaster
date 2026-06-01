import { useState } from 'react';
import { Camera, Sparkles, Palette, Share2, Settings, Image, Layers, Zap, Shield, Smartphone } from 'lucide-react';

const features = [
  {
    category: '预设管理',
    icon: <Layers className="w-8 h-8" />,
    features: [
      {
        name: 'HNCS认证预设',
        description: '精选哈苏自然色彩解决方案认证预设，专业调校的色彩表现',
        icon: '🎨'
      },
      {
        name: '预设分类展示',
        description: '按场景、风格、用途智能分类，快速找到适合的预设',
        icon: '📁'
      },
      {
        name: '收藏与分享',
        description: '收藏常用预设，一键分享给好友或社交平台',
        icon: '❤️'
      },
      {
        name: '智能搜索筛选',
        description: '关键词搜索，多维度筛选，快速定位目标预设',
        icon: '🔍'
      }
    ]
  },
  {
    category: 'AI智能功能',
    icon: <Sparkles className="w-8 h-8" />,
    features: [
      {
        name: 'AI场景识别',
        description: '智能识别24种常见拍摄场景，自动匹配最佳参数',
        icon: '🤖'
      },
      {
        name: 'AI参数建议',
        description: '基于场景智能分析，提供专业的相机参数推荐',
        icon: '💡'
      },
      {
        name: 'AI样张微调',
        description: '实时预览参数调整效果，所见即所得',
        icon: '🎯'
      },
      {
        name: 'AI批量处理',
        description: '批量应用预设和调整，提高工作效率',
        icon: '⚡'
      },
      {
        name: 'AI参数识别',
        description: 'OCR识别照片中的相机参数，快速还原拍摄设置',
        icon: '📷'
      }
    ]
  },
  {
    category: '相机参数系统',
    icon: <Camera className="w-8 h-8" />,
    features: [
      {
        name: '实时参数显示',
        description: 'ISO、快门、光圈、焦距、曝光补偿等专业参数',
        icon: '📊'
      },
      {
        name: '悬浮窗显示',
        description: '相机取景框实时叠加参数显示，专业拍摄辅助',
        icon: '🪟'
      },
      {
        name: '参数截图分享',
        description: '一键生成参数截图，专业水准的分享展示',
        icon: '📸'
      }
    ]
  },
  {
    category: '水印编辑',
    icon: <Image className="w-8 h-8" />,
    features: [
      {
        name: '文字水印',
        description: '自定义文字内容、字体、颜色、位置',
        icon: '✍️'
      },
      {
        name: '图片水印',
        description: '添加品牌Logo或个人水印图片',
        icon: '🖼️'
      },
      {
        name: '模板系统',
        description: '预置多款专业水印模板，一键应用',
        icon: '📋'
      },
      {
        name: '批量处理',
        description: '批量添加水印，高效工作流',
        icon: '🔄'
      },
      {
        name: '撤销重做',
        description: '完整的操作历史记录，放心编辑',
        icon: '↩️'
      }
    ]
  },
  {
    category: '社交分享',
    icon: <Share2 className="w-8 h-8" />,
    features: [
      {
        name: '微信好友',
        description: '直接分享到微信好友或群聊',
        icon: '💬'
      },
      {
        name: '微信朋友圈',
        description: '精美的朋友圈分享样式',
        icon: '📱'
      },
      {
        name: 'QQ与空间',
        description: 'QQ好友和QQ空间分享支持',
        icon: '🐧'
      },
      {
        name: '微博与抖音',
        description: '微博和抖音平台分享',
        icon: '🎵'
      },
      {
        name: '小红书',
        description: '小红书社区分享',
        icon: '📖'
      },
      {
        name: '原图分享',
        description: '无损原图质量分享',
        icon: '💯'
      }
    ]
  },
  {
    category: '主题与设计',
    icon: <Palette className="w-8 h-8" />,
    features: [
      {
        name: '浅色主题',
        description: '明亮清爽的日间模式',
        icon: '☀️'
      },
      {
        name: '深色主题',
        description: '护眼舒适的夜间模式',
        icon: '🌙'
      },
      {
        name: '跟随系统',
        description: '自动跟随系统主题设置',
        icon: '⚙️'
      },
      {
        name: 'ColorOS 16风格',
        description: '完美适配ColorOS 16设计语言',
        icon: '🎨'
      },
      {
        name: '护眼模式',
        description: '暖色调护眼配色方案',
        icon: '👁️'
      }
    ]
  },
  {
    category: '性能优化',
    icon: <Zap className="w-8 h-8" />,
    features: [
      {
        name: '60fps流畅动画',
        description: '丝滑流畅的交互动画体验',
        icon: '🎬'
      },
      {
        name: '内存监控',
        description: '实时内存使用监控和优化',
        icon: '🧠'
      },
      {
        name: '启动优化',
        description: '快速冷启动和热启动',
        icon: '🚀'
      },
      {
        name: '弱网适配',
        description: '弱网环境下的优化处理',
        icon: '📶'
      },
      {
        name: '图片加载优化',
        description: '高效的图片加载和缓存策略',
        icon: '🖼️'
      }
    ]
  },
  {
    category: '安全与隐私',
    icon: <Shield className="w-8 h-8" />,
    features: [
      {
        name: '数据加密',
        description: '敏感数据加密存储',
        icon: '🔒'
      },
      {
        name: '权限管理',
        description: '精细的应用权限控制',
        icon: '🔐'
      },
      {
        name: '本地处理',
        description: 'AI处理在本地完成，不上传数据',
        icon: '🏠'
      },
      {
        name: '安全存储',
        description: 'EncryptedSharedPreferences加密存储',
        icon: '🛡️'
      },
      {
        name: '权限请求',
        description: '优雅的动态权限请求引导',
        icon: '📋'
      }
    ]
  },
  {
    category: '用户中心',
    icon: <Smartphone className="w-8 h-8" />,
    features: [
      {
        name: '我的收藏',
        description: '管理所有收藏的预设',
        icon: '⭐'
      },
      {
        name: '下载历史',
        description: '查看预设下载记录',
        icon: '📥'
      },
      {
        name: '使用统计',
        description: '详细的使用数据统计',
        icon: '📈'
      },
      {
        name: '隐私设置',
        description: '隐私保护相关设置',
        icon: '🔒'
      },
      {
        name: '通知设置',
        description: '个性化通知偏好',
        icon: '🔔'
      },
      {
        name: '意见反馈',
        description: '便捷的用户反馈通道',
        icon: '💬'
      },
      {
        name: '关于我们',
        description: '应用信息和版本更新',
        icon: 'ℹ️'
      }
    ]
  }
];

export default function FeaturesPage() {
  const [activeCategory, setActiveCategory] = useState(0);

  return (
    <div className="min-h-screen pt-20 pb-16">
      {/* 页面头部 */}
      <div className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-hasselblad/10 to-transparent" />
        <div className="max-w-6xl mx-auto px-6 py-16 text-center">
          <h1 className="text-4xl md:text-5xl font-bold mb-6">
            <span className="gradient-text">App功能一览</span>
          </h1>
          <p className="text-white/60 text-lg max-w-2xl mx-auto">
            探索OPPO哈苏影像系统的强大功能，从专业预设到AI智能，从水印编辑到社交分享
          </p>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-6">
        {/* 分类导航 - 桌面端侧边栏，移动端横向滚动 */}
        <div className="flex flex-col lg:flex-row gap-8">
          {/* 分类列表 */}
          <div className="lg:w-64 flex-shrink-0">
            <div className="sticky top-24">
              <div className="lg:block overflow-x-auto lg:overflow-visible pb-4 lg:pb-0">
                <div className="flex lg:flex-col gap-2 min-w-max lg:min-w-0">
                  {features.map((featureGroup, index) => (
                    <button
                      key={index}
                      onClick={() => setActiveCategory(index)}
                      className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300 whitespace-nowrap ${
                        activeCategory === index
                          ? 'bg-hasselblad text-deep-space shadow-lg shadow-hasselblad/20'
                          : 'bg-white/5 text-white/70 hover:bg-white/10 hover:text-white'
                      }`}
                    >
                      <span className={activeCategory === index ? 'text-deep-space' : 'text-hasselblad'}>
                        {featureGroup.icon}
                      </span>
                      <span className="font-medium">{featureGroup.category}</span>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* 功能展示区域 */}
          <div className="flex-1">
            <div className="mb-8">
              <h2 className="text-2xl font-bold mb-2 flex items-center gap-3">
                <span className="text-hasselblad">
                  {features[activeCategory].icon}
                </span>
                {features[activeCategory].category}
              </h2>
              <p className="text-white/50">共 {features[activeCategory].features.length} 个功能</p>
            </div>

            <div className="grid md:grid-cols-2 gap-4">
              {features[activeCategory].features.map((feature, index) => (
                <div
                  key={index}
                  className="card p-6 group hover:scale-[1.02] cursor-pointer"
                >
                  <div className="text-4xl mb-4 group-hover:scale-110 transition-transform duration-300">
                    {feature.icon}
                  </div>
                  <h3 className="text-xl font-semibold mb-2 text-white">
                    {feature.name}
                  </h3>
                  <p className="text-white/60 text-sm leading-relaxed">
                    {feature.description}
                  </p>
                  <div className="mt-4 pt-4 border-t border-white/10">
                    <span className="text-hasselblad/80 text-sm flex items-center gap-1">
                      已实现
                      <span className="w-2 h-2 bg-oppo-green rounded-full animate-pulse" />
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* 功能统计 */}
            <div className="mt-12 grid grid-cols-2 md:grid-cols-4 gap-4">
              {[
                { label: '核心功能', value: '32+', color: 'text-hasselblad' },
                { label: 'AI场景', value: '24种', color: 'text-purple-400' },
                { label: '水印模板', value: '8+', color: 'text-blue-400' },
                { label: '分享渠道', value: '9+', color: 'text-green-400' }
              ].map((stat, index) => (
                <div key={index} className="card p-6 text-center">
                  <div className={`text-3xl font-bold mb-2 ${stat.color}`}>
                    {stat.value}
                  </div>
                  <div className="text-white/60 text-sm">{stat.label}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* 完整功能矩阵 */}
        <div className="mt-16">
          <h2 className="text-2xl font-bold mb-8 text-center">完整功能矩阵</h2>
          <div className="grid md:grid-cols-3 gap-4">
            {features.map((group, groupIndex) => (
              <div key={groupIndex} className="card p-6">
                <div className="flex items-center gap-3 mb-4">
                  <span className="text-hasselblad">{group.icon}</span>
                  <h3 className="font-semibold">{group.category}</h3>
                </div>
                <div className="space-y-2">
                  {group.features.map((feature, featureIndex) => (
                    <div key={featureIndex} className="flex items-center gap-2 text-sm">
                      <span className="text-oppo-green">✓</span>
                      <span className="text-white/80">{feature.name}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
