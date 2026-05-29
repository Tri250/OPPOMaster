import { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Camera,
  Sparkles,
  Layers,
  Search,
  Database,
  Upload,
  Palette,
  Code,
  Image as ImageIcon,
  ChevronLeft,
  Menu,
  Zap,
  Globe,
  Bot,
  Smartphone
} from 'lucide-react';
import { ColorOSAnimations } from '../components/common/ColorOSComponents';

export default function XiaoOHelpPage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const coreFeatures = [
    {
      id: 1,
      icon: Sparkles,
      title: 'AI场景识别',
      description: '智能识别拍摄场景，自动推荐最佳影像参数',
      features: [
        '50+场景类型识别',
        '毫秒级响应速度',
        '自适应参数推荐'
      ],
      color: 'from-purple-500 to-pink-500',
      path: '/scene-detection'
    },
    {
      id: 2,
      icon: Smartphone,
      title: '原生相机参数自动填入',
      description: '基于安卓无障碍服务，无Root自动填参数',
      features: [
        '支持六大品牌相机',
        '无Root合法合规',
        '10+步简化为2步'
      ],
      color: 'from-emerald-500 to-teal-500',
      path: '/native-camera'
    },
    {
      id: 3,
      icon: Layers,
      title: '悬浮窗',
      description: '多悬浮窗类型兼容方案，适配率95%+',
      features: [
        '标准悬浮窗类型',
        '相机上层显示',
        'SurfaceView绘制'
      ],
      color: 'from-blue-500 to-indigo-500',
      path: '/floating-window'
    },
    {
      id: 4,
      icon: Search,
      title: '预设分类搜索',
      description: '多维度分类体系，快速找到心仪预设',
      features: [
        '按风格分类',
        '按场景分类',
        '全文搜索'
      ],
      color: 'from-green-500 to-emerald-500',
      path: '/filter-library'
    },
    {
      id: 5,
      icon: Code,
      title: '预设生态',
      description: '内置预设编辑器+一键社区贡献系统',
      features: [
        '预设编辑器',
        '一键贡献',
        '预设排行榜'
      ],
      color: 'from-amber-500 to-orange-500',
      path: '/preset-ecosystem'
    },
    {
      id: 6,
      icon: Upload,
      title: '多格式预设导入导出',
      description: '支持主流修图工具预设格式',
      features: [
        'LUT文件解析',
        '泼辣修图预设',
        'Lightroom预设'
      ],
      color: 'from-red-500 to-pink-500',
      path: '/lut-manager'
    }
  ];

  const imageTools = [
    {
      id: 7,
      icon: Palette,
      title: '水印生成器',
      description: '10+品牌水印模板',
      color: 'from-gray-600 to-gray-700',
      path: '/watermark',
      featured: true
    },
    {
      id: 8,
      icon: ImageIcon,
      title: '预设编辑器',
      description: '创建专属预设',
      color: 'from-gray-700 to-gray-800',
      path: '/editor'
    }
  ];

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-72 h-72 top-1/3 -left-36 animate-float opacity-20" />
        <div className="orb-oppo orb-3 w-56 h-56 bottom-1/4 right-0 animate-float opacity-20" style={{ animationDelay: '3s' }} />
      </div>

      {/* 导航栏 */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-deep-space/80 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 sm:px-6">
          <div className="flex items-center justify-between h-16">
            <Link to="/" className="flex items-center space-x-3">
              <button className="p-2 rounded-xl hover:bg-white/10 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center">
                <ChevronLeft className="w-6 h-6 text-white" />
              </button>
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                <Camera className="w-5 h-5 text-deep-space" />
              </div>
              <span className="text-lg font-bold text-white">小O帮帮</span>
            </Link>

            <button 
              className="p-2 rounded-xl hover:bg-white/10 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              <Menu className="w-6 h-6 text-white" />
            </button>
          </div>
        </div>
      </nav>

      {/* 主内容 */}
      <div className="relative pt-24 pb-12 px-4 sm:px-6">
        <div className="max-w-4xl mx-auto">
          <motion.div
            initial="initial"
            animate="animate"
            variants={ColorOSAnimations.fadeIn}
            className="space-y-6"
          >
            {/* 顶部标题区 */}
            <motion.div variants={ColorOSAnimations.fadeIn} className="text-center space-y-2 mb-6">
              <h1 className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-oppo-sunrise-gold via-hasselblad-pro to-oppo-sunrise-gold bg-clip-text text-transparent">
                影像参数
              </h1>
              <p className="text-text-secondary text-sm md:text-base">
                核心功能展示 - 点击卡片查看详情
              </p>
            </motion.div>

            {/* 核心功能网格 */}
            <div className="space-y-4">
              {coreFeatures.map((feature, index) => (
                <motion.div
                  key={feature.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                  whileHover={{ scale: 1.02, y: -4 }}
                >
                  <Link to={feature.path} className="block">
                    <div className="card-oppo p-5 md:p-6 group cursor-pointer">
                      <div className="flex flex-col md:flex-row md:items-start gap-5">
                        <div className={`w-16 h-16 md:w-20 md:h-20 rounded-2xl bg-gradient-to-br ${feature.color} flex items-center justify-center flex-shrink-0`}>
                          <feature.icon className="w-8 h-8 md:w-10 md:h-10 text-white" />
                        </div>
                        <div className="flex-1">
                          <h3 className="text-xl md:text-2xl font-bold text-white mb-2 group-hover:text-oppo-sunrise-gold transition-colors">
                            {feature.title}
                          </h3>
                          <p className="text-text-secondary text-sm md:text-base mb-4">
                            {feature.description}
                          </p>
                          <div className="flex flex-wrap gap-2">
                            {feature.features.map((feat, i) => (
                              <span
                                key={i}
                                className="px-3 py-1 bg-white/5 rounded-full text-text-secondary text-xs md:text-sm"
                              >
                                {feat}
                              </span>
                            ))}
                          </div>
                        </div>
                      </div>
                    </div>
                  </Link>
                </motion.div>
              ))}
            </div>

            {/* 影像工具区 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6 }}
              className="mt-10"
            >
              <div className="text-center mb-6">
                <h2 className="text-2xl md:text-3xl font-bold bg-gradient-to-r from-oppo-sunrise-gold to-hasselblad-pro bg-clip-text text-transparent">
                  影像工具
                </h2>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {imageTools.map((tool, index) => (
                  <motion.div
                    key={tool.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.7 + index * 0.1 }}
                    whileHover={{ scale: 1.02, y: -4 }}
                  >
                    <Link to={tool.path} className="block">
                      <div className={`card-oppo p-5 md:p-6 group cursor-pointer ${tool.featured ? 'ring-2 ring-oppo-sunrise-gold/30' : ''}`}>
                        <div className="flex items-center gap-4">
                          <div className={`w-14 h-14 md:w-16 md:h-16 rounded-2xl bg-gradient-to-br ${tool.color} flex items-center justify-center flex-shrink-0`}>
                            <tool.icon className="w-7 h-7 md:w-8 md:h-8 text-white" />
                          </div>
                          <div className="flex-1">
                            <h3 className="text-lg md:text-xl font-bold text-white mb-1 group-hover:text-oppo-sunrise-gold transition-colors">
                              {tool.title}
                              {tool.featured && (
                                <span className="ml-2 text-xs bg-oppo-sunrise-gold text-deep-space px-2 py-0.5 rounded-full font-medium">
                                  推荐
                                </span>
                              )}
                            </h3>
                            <p className="text-text-secondary text-sm">
                              {tool.description}
                            </p>
                          </div>
                        </div>
                      </div>
                    </Link>
                  </motion.div>
                ))}
              </div>
            </motion.div>

            {/* 底部说明 */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.9 }}
              className="mt-12 text-center"
            >
              <p className="text-text-tertiary text-xs md:text-sm">
                点击任意卡片即可体验对应功能
              </p>
            </motion.div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
