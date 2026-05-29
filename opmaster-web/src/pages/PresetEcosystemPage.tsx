import { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Code,
  Trophy,
  Users,
  Zap,
  ChevronLeft,
  Menu,
  Edit3,
  Upload,
  Star,
  TrendingUp,
  Globe
} from 'lucide-react';
import { ColorOSAnimations } from '../components/common/ColorOSComponents';

export default function PresetEcosystemPage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const features = [
    {
      id: 1,
      title: '预设编辑器',
      description: '可视化参数调整，实时预览效果',
      icon: Edit3,
      color: 'from-oppo-sunrise-gold to-hasselblad-pro',
      path: '/editor'
    },
    {
      id: 2,
      title: '一键贡献',
      description: '发布你的预设到社区',
      icon: Upload,
      color: 'from-blue-500 to-indigo-500',
      path: '/editor'
    },
    {
      id: 3,
      title: '预设排行榜',
      description: '发现最受欢迎的预设',
      icon: Trophy,
      color: 'from-purple-500 to-pink-500',
      path: '/filter-library'
    },
    {
      id: 4,
      title: '创作者认证',
      description: '专属标识与社区影响力',
      icon: Users,
      color: 'from-emerald-500 to-teal-500',
      path: '/about'
    }
  ];

  const topPresets = [
    {
      id: 1,
      rank: 1,
      name: '哈苏自然色彩',
      author: 'OPPO官方',
      downloads: '156.2k',
      rating: 4.9,
      isCertified: true,
      isNew: false
    },
    {
      id: 2,
      rank: 2,
      name: '富士胶片模拟',
      author: '色彩实验室',
      downloads: '98.5k',
      rating: 4.8,
      isCertified: true,
      isNew: false
    },
    {
      id: 3,
      rank: 3,
      name: '徕卡经典单色',
      author: '光影猎人',
      downloads: '76.3k',
      rating: 4.7,
      isCertified: false,
      isNew: true
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
            <Link to="/xiao-o-help" className="flex items-center space-x-3">
              <button className="p-2 rounded-xl hover:bg-white/10 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center">
                <ChevronLeft className="w-6 h-6 text-white" />
              </button>
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center">
                <Code className="w-5 h-5 text-deep-space" />
              </div>
              <span className="text-lg font-bold text-white">预设生态</span>
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
            {/* Hero区 */}
            <motion.div variants={ColorOSAnimations.fadeIn} className="text-center mb-10">
              <div className="w-24 h-24 md:w-28 md:h-28 mx-auto mb-6 rounded-3xl bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center">
                <Code className="w-12 h-12 text-deep-space" />
              </div>
              <h1 className="text-3xl md:text-4xl font-bold text-white mb-3">预设生态</h1>
              <p className="text-text-secondary text-sm md:text-base">
                内置预设编辑器+一键社区贡献系统
              </p>
            </motion.div>

            {/* 功能特点网格 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
              {features.map((feature, index) => (
                <motion.div
                  key={feature.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                >
                  <Link to={feature.path} className="block">
                    <div className="card-oppo p-6 group cursor-pointer">
                      <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${feature.color} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                        <feature.icon className="w-7 h-7 text-white" />
                      </div>
                      <h3 className="text-lg font-bold text-white mb-2 group-hover:text-oppo-sunrise-gold transition-colors">{feature.title}</h3>
                      <p className="text-text-secondary text-sm">{feature.description}</p>
                    </div>
                  </Link>
                </motion.div>
              ))}
            </div>

            {/* 热门预设排行 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="mt-12"
            >
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white flex items-center gap-2">
                  <TrendingUp className="w-6 h-6 text-oppo-sunrise-gold" />
                  热门预设排行榜
                </h2>
                <Link to="/filter-library">
                  <button className="text-oppo-sunrise-gold text-sm hover:text-amber-400 transition-colors">
                    查看全部
                  </button>
                </Link>
              </div>

              <div className="space-y-3">
                {topPresets.map((preset, index) => (
                  <motion.div
                    key={preset.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.5 + index * 0.1 }}
                  >
                    <Link to="/filter-library" className="block">
                      <div className="card-oppo p-5 flex items-center gap-4 group cursor-pointer">
                        <div className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-lg flex-shrink-0 ${
                          preset.rank === 1 ? 'bg-gradient-to-br from-yellow-500 to-amber-600 text-black' :
                          preset.rank === 2 ? 'bg-gradient-to-br from-gray-400 to-gray-500 text-black' :
                          preset.rank === 3 ? 'bg-gradient-to-br from-orange-700 to-orange-900 text-white' :
                          'bg-white/10 text-text-secondary'
                        }`}>
                          {preset.rank}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <h3 className="text-white font-medium truncate">{preset.name}</h3>
                            {preset.isCertified && (
                              <span className="w-5 h-5 rounded-full bg-oppo-sunrise-gold/20 text-oppo-sunrise-gold flex items-center justify-center">
                                <CheckCircle size={14} />
                              </span>
                            )}
                            {preset.isNew && (
                              <span className="px-2 py-0.5 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>
                            )}
                          </div>
                          <p className="text-text-tertiary text-sm">@{preset.author}</p>
                        </div>
                        <div className="text-right flex-shrink-0">
                          <div className="flex items-center gap-1 text-oppo-sunrise-gold text-sm mb-1">
                            <Star className="w-4 h-4 fill-current" />
                            <span className="font-medium">{preset.rating}</span>
                          </div>
                          <p className="text-text-tertiary text-xs">{preset.downloads}</p>
                        </div>
                      </div>
                    </Link>
                  </motion.div>
                ))}
              </div>
            </motion.div>

            {/* 贡献指南 */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.8 }}
              className="mt-12"
            >
              <div className="card-oppo p-6 border-l-4 border-oppo-sunrise-gold bg-gradient-to-r from-oppo-sunrise-gold/10 to-transparent">
                <div className="flex items-start gap-4">
                  <Globe className="w-10 h-10 text-oppo-sunrise-gold flex-shrink-0" />
                  <div>
                    <h3 className="text-lg font-bold text-white mb-1">加入社区创作者</h3>
                    <p className="text-text-secondary text-sm md:text-base mb-4">
                      分享你的创作，与全球摄影爱好者一起探索影像的无限可能
                    </p>
                    <div className="flex flex-wrap gap-2">
                      <span className="px-3 py-1 bg-white/10 rounded-full text-text-secondary text-sm">免费发布</span>
                      <span className="px-3 py-1 bg-white/10 rounded-full text-text-secondary text-sm">版权保护</span>
                      <span className="px-3 py-1 bg-white/10 rounded-full text-text-secondary text-sm">创作分成</span>
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
