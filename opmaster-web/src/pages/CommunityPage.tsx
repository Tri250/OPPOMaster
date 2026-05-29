import { motion } from 'framer-motion';
import { 
  Users, 
  Upload, 
  Download, 
  Star, 
  Trophy, 
  TrendingUp,
  Heart,
  MessageSquare,
  Share2,
  Award,
  Target,
  Zap,
  Clock,
  CheckCircle,
  ArrowRight,
  Camera,
  Palette,
  Filter,
  Grid3x3,
  List,
  Search,
  Bookmark,
  TrendingDown
} from 'lucide-react';
import { useState, useEffect } from 'react';
import { useSubscriptionStore } from '../store/useSyncStore';
import { ColorOSSwitch } from '../components/common/ColorOSComponents';

interface Contribution {
  id: string;
  author: string;
  authorAvatar: string;
  title: string;
  description: string;
  presetName: string;
  deviceModel: string;
  category: string;
  tags: string[];
  downloads: number;
  likes: number;
  rating: number;
  createdAt: string;
  status: 'pending' | 'approved' | 'rejected';
  verified: boolean;
  featured: boolean;
  premium: boolean;
}

interface CommunityStats {
  totalContributors: number;
  totalPresets: number;
  totalDownloads: number;
  totalLikes: number;
}

export default function CommunityPage() {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [activeTab, setActiveTab] = useState<'trending' | 'latest' | 'top'>('trending');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  
  const { subscriptions } = useSubscriptionStore();
  
  const [stats] = useState<CommunityStats>({
    totalContributors: 12847,
    totalPresets: 34592,
    totalDownloads: 1284593,
    totalLikes: 892347
  });

  const [contributions] = useState<Contribution[]>([
    {
      id: '1',
      author: '摄影达人小王',
      authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=wang',
      title: '哈苏人像经典参数',
      description: '复刻哈苏X2D经典人像色调，柔和自然，肤色表现优异',
      presetName: '哈苏人像大师',
      deviceModel: 'OPPO Find X8 Ultra',
      category: 'portrait',
      tags: ['哈苏', '人像', '经典'],
      downloads: 12845,
      likes: 892,
      rating: 4.9,
      createdAt: '2024-01-15',
      status: 'approved',
      verified: true,
      featured: true,
      premium: false
    },
    {
      id: '2',
      author: '城市摄影师',
      authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=li',
      title: '城市夜景霓虹',
      description: '专为城市夜景设计，增强霓虹灯效果，高对比度',
      presetName: '霓虹都市',
      deviceModel: 'OPPO Find N3',
      category: 'night',
      tags: ['夜景', '霓虹', '城市'],
      downloads: 8934,
      likes: 567,
      rating: 4.7,
      createdAt: '2024-01-14',
      status: 'approved',
      verified: true,
      featured: false,
      premium: true
    },
    {
      id: '3',
      author: '美食博主阿明',
      authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=ming',
      title: '美食增强色调',
      description: '提升食物色彩饱和度和清晰度，让美食更诱人',
      presetName: '美食增强',
      deviceModel: 'OnePlus 12',
      category: 'food',
      tags: ['美食', '饱和度', '清晰'],
      downloads: 6789,
      likes: 423,
      rating: 4.6,
      createdAt: '2024-01-13',
      status: 'approved',
      verified: false,
      featured: false,
      premium: false
    },
    {
      id: '4',
      author: '风光摄影师李华',
      authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=hua',
      title: '自然风光增强',
      description: '增强天空和自然的色彩，适合风景摄影',
      presetName: '自然风光',
      deviceModel: 'OPPO Find X7 Pro',
      category: 'landscape',
      tags: ['风光', '自然', '天空'],
      downloads: 11234,
      likes: 789,
      rating: 4.8,
      createdAt: '2024-01-12',
      status: 'approved',
      verified: true,
      featured: true,
      premium: false
    },
    {
      id: '5',
      author: '胶片爱好者',
      authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=film',
      title: '经典胶片模拟',
      description: '模拟经典彩色负片效果，温暖色调，颗粒感',
      presetName: '胶片模拟Kodak',
      deviceModel: '通用',
      category: 'film',
      tags: ['胶片', '复古', 'Kodak'],
      downloads: 15678,
      likes: 1023,
      rating: 4.9,
      createdAt: '2024-01-11',
      status: 'approved',
      verified: true,
      featured: false,
      premium: true
    }
  ]);

  const categories = [
    { id: 'all', name: '全部', icon: Grid3x3, count: contributions.length },
    { id: 'portrait', name: '人像', icon: Users, count: contributions.filter(c => c.category === 'portrait').length },
    { id: 'landscape', name: '风光', icon: Filter, count: contributions.filter(c => c.category === 'landscape').length },
    { id: 'night', name: '夜景', icon: Target, count: contributions.filter(c => c.category === 'night').length },
    { id: 'food', name: '美食', icon: Palette, count: contributions.filter(c => c.category === 'food').length },
    { id: 'film', name: '胶片', icon: Camera, count: contributions.filter(c => c.category === 'film').length }
  ];

  const filteredContributions = contributions.filter(c => {
    const matchesCategory = selectedCategory === 'all' || c.category === selectedCategory;
    const matchesSearch = c.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         c.author.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         c.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesCategory && matchesSearch;
  });

  const formatNumber = (num: number) => {
    if (num >= 10000) return `${(num / 10000).toFixed(1)}w`;
    if (num >= 1000) return `${(num / 1000).toFixed(1)}k`;
    return num.toString();
  };

  const getTimeAgo = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return '今天';
    if (diffDays === 1) return '昨天';
    if (diffDays < 7) return `${diffDays}天前`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)}周前`;
    return `${Math.floor(diffDays / 30)}月前`;
  };

  return (
    <div className="min-h-screen bg-deep-space text-white pb-20">
      {/* 顶部导航 */}
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Users className="w-5 h-5 text-oppo-sunrise-gold" />
            <h1 className="text-lg font-semibold">社区</h1>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowSubmitModal(true)}
              className="px-4 py-2 rounded-full bg-oppo-sunrise-gold text-black text-sm font-medium hover:bg-oppo-sunrise-gold/90 transition-colors"
            >
              贡献预设
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6 space-y-6">
        {/* 社区统计数据 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="grid grid-cols-4 gap-4"
        >
          <div className="card-oppo p-4 text-center">
            <Users className="w-6 h-6 text-ocean-blue mx-auto mb-2" />
            <p className="text-2xl font-bold text-white">{formatNumber(stats.totalContributors)}</p>
            <p className="text-xs text-text-secondary mt-1">贡献者</p>
          </div>
          <div className="card-oppo p-4 text-center">
            <Palette className="w-6 h-6 text-oppo-sunrise-gold mx-auto mb-2" />
            <p className="text-2xl font-bold text-white">{formatNumber(stats.totalPresets)}</p>
            <p className="text-xs text-text-secondary mt-1">预设总数</p>
          </div>
          <div className="card-oppo p-4 text-center">
            <Download className="w-6 h-6 text-oppo-green mx-auto mb-2" />
            <p className="text-2xl font-bold text-white">{formatNumber(stats.totalDownloads)}</p>
            <p className="text-xs text-text-secondary mt-1">下载次数</p>
          </div>
          <div className="card-oppo p-4 text-center">
            <Heart className="w-6 h-6 text-rose-gold mx-auto mb-2" />
            <p className="text-2xl font-bold text-white">{formatNumber(stats.totalLikes)}</p>
            <p className="text-xs text-text-secondary mt-1">获赞总数</p>
          </div>
        </motion.div>

        {/* 搜索栏 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="relative"
        >
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
          <input
            type="text"
            placeholder="搜索预设、作者或标签..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-12 pr-4 py-3 rounded-2xl bg-white/5 border border-white/10 focus:border-oppo-sunrise-gold focus:outline-none"
          />
        </motion.div>

        {/* 分类筛选 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="flex items-center gap-3 overflow-x-auto pb-2"
        >
          {categories.map((category) => {
            const Icon = category.icon;
            return (
              <button
                key={category.id}
                onClick={() => setSelectedCategory(category.id)}
                className={`flex items-center gap-2 px-4 py-2 rounded-full whitespace-nowrap transition-all ${
                  selectedCategory === category.id
                    ? 'bg-oppo-sunrise-gold text-black'
                    : 'bg-white/5 text-white hover:bg-white/10'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="text-sm font-medium">{category.name}</span>
                <span className={`text-xs ${
                  selectedCategory === category.id ? 'text-black/60' : 'text-text-tertiary'
                }`}>
                  {category.count}
                </span>
              </button>
            );
          })}
        </motion.div>

        {/* 标签页切换 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="flex items-center justify-between"
        >
          <div className="flex items-center gap-2 bg-white/5 rounded-full p-1">
            {[
              { id: 'trending', label: '热门', icon: TrendingUp },
              { id: 'latest', label: '最新', icon: Clock },
              { id: 'top', label: '精选', icon: Trophy }
            ].map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`flex items-center gap-2 px-4 py-2 rounded-full transition-all ${
                    activeTab === tab.id
                      ? 'bg-white text-black'
                      : 'text-white hover:bg-white/10'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span className="text-sm font-medium">{tab.label}</span>
                </button>
              );
            })}
          </div>
          
          <div className="flex items-center gap-2">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded-lg transition-colors ${
                viewMode === 'grid' ? 'bg-white/10' : 'hover:bg-white/5'
              }`}
            >
              <Grid3x3 className="w-5 h-5" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-lg transition-colors ${
                viewMode === 'list' ? 'bg-white/10' : 'hover:bg-white/5'
              }`}
            >
              <List className="w-5 h-5" />
            </button>
          </div>
        </motion.div>

        {/* 贡献列表 */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className={viewMode === 'grid' ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4' : 'space-y-3'}
        >
          {filteredContributions.map((contribution, index) => (
            <motion.div
              key={contribution.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 + index * 0.05 }}
              className={`card-oppo p-4 cursor-pointer hover:bg-white/5 transition-all group ${
                contribution.featured ? 'ring-2 ring-oppo-sunrise-gold/50' : ''
              } ${viewMode === 'list' ? 'flex items-center gap-4' : ''}`}
            >
              {viewMode === 'grid' ? (
                <>
                  {/* 网格视图 */}
                  <div className="relative mb-3">
                    <div className="aspect-video bg-gradient-to-br from-oppo-sunrise-gold/20 to-oppo-green/20 rounded-xl flex items-center justify-center">
                      <Camera className="w-12 h-12 text-white/50" />
                    </div>
                    {contribution.featured && (
                      <div className="absolute top-2 left-2 px-2 py-1 rounded-full bg-oppo-sunrise-gold text-black text-xs font-medium flex items-center gap-1">
                        <Star className="w-3 h-3" />
                        精选
                      </div>
                    )}
                    {contribution.premium && (
                      <div className="absolute top-2 right-2 px-2 py-1 rounded-full bg-rose-gold text-white text-xs font-medium flex items-center gap-1">
                        <Award className="w-3 h-3" />
                        付费
                      </div>
                    )}
                  </div>
                  
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <img 
                        src={contribution.authorAvatar} 
                        alt={contribution.author}
                        className="w-6 h-6 rounded-full"
                      />
                      <span className="text-sm text-text-secondary">{contribution.author}</span>
                      {contribution.verified && (
                        <CheckCircle className="w-4 h-4 text-oppo-green" />
                      )}
                    </div>
                    
                    <h3 className="font-medium text-base group-hover:text-oppo-sunrise-gold transition-colors">
                      {contribution.title}
                    </h3>
                    
                    <p className="text-xs text-text-tertiary line-clamp-2">
                      {contribution.description}
                    </p>
                    
                    <div className="flex items-center gap-2 text-xs text-text-secondary">
                      <span className="flex items-center gap-1">
                        <Download className="w-3 h-3" />
                        {formatNumber(contribution.downloads)}
                      </span>
                      <span className="flex items-center gap-1">
                        <Heart className="w-3 h-3" />
                        {formatNumber(contribution.likes)}
                      </span>
                      <span className="flex items-center gap-1">
                        <Star className="w-3 h-3" />
                        {contribution.rating}
                      </span>
                      <span className="ml-auto text-text-tertiary">
                        {getTimeAgo(contribution.createdAt)}
                      </span>
                    </div>
                    
                    <div className="flex items-center gap-2 pt-2 border-t border-white/5">
                      {contribution.tags.slice(0, 2).map((tag) => (
                        <span key={tag} className="px-2 py-0.5 rounded-full bg-white/5 text-xs text-text-secondary">
                          #{tag}
                        </span>
                      ))}
                      <span className="ml-auto text-xs text-oppo-green">{contribution.deviceModel}</span>
                    </div>
                  </div>
                </>
              ) : (
                <>
                  {/* 列表视图 */}
                  <div className="w-24 h-24 rounded-xl bg-gradient-to-br from-oppo-sunrise-gold/20 to-oppo-green/20 flex items-center justify-center flex-shrink-0">
                    <Camera className="w-8 h-8 text-white/50" />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <img 
                        src={contribution.authorAvatar} 
                        alt={contribution.author}
                        className="w-5 h-5 rounded-full"
                      />
                      <span className="text-sm text-text-secondary">{contribution.author}</span>
                      {contribution.verified && (
                        <CheckCircle className="w-4 h-4 text-oppo-green" />
                      )}
                      {contribution.featured && (
                        <span className="px-2 py-0.5 rounded-full bg-oppo-sunrise-gold/20 text-oppo-sunrise-gold text-xs">
                          精选
                        </span>
                      )}
                    </div>
                    
                    <h3 className="font-medium group-hover:text-oppo-sunrise-gold transition-colors">
                      {contribution.title}
                    </h3>
                    
                    <div className="flex items-center gap-4 text-xs text-text-secondary mt-1">
                      <span className="flex items-center gap-1">
                        <Download className="w-3 h-3" />
                        {formatNumber(contribution.downloads)}
                      </span>
                      <span className="flex items-center gap-1">
                        <Heart className="w-3 h-3" />
                        {formatNumber(contribution.likes)}
                      </span>
                      <span className="flex items-center gap-1">
                        <Star className="w-3 h-3" />
                        {contribution.rating}
                      </span>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-2">
                    {contribution.tags.slice(0, 2).map((tag) => (
                      <span key={tag} className="px-2 py-0.5 rounded-full bg-white/5 text-xs text-text-secondary">
                        #{tag}
                      </span>
                    ))}
                  </div>
                </>
              )}
            </motion.div>
          ))}
        </motion.div>

        {/* 贡献指南 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="card-oppo p-6"
        >
          <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <Award className="w-5 h-5 text-oppo-sunrise-gold" />
            如何贡献优质预设
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-ocean-blue/20 flex items-center justify-center">
                  <Zap className="w-4 h-4 text-ocean-blue" />
                </div>
                <h3 className="font-medium">高质量参数</h3>
              </div>
              <p className="text-sm text-text-secondary">
                提供经过实际测试验证的参数设置，确保在不同场景下都能有出色表现
              </p>
            </div>
            
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-oppo-green/20 flex items-center justify-center">
                  <Target className="w-4 h-4 text-oppo-green" />
                </div>
                <h3 className="font-medium">明确使用场景</h3>
              </div>
              <p className="text-sm text-text-secondary">
                详细说明预设的适用场景，如人像、风光、夜景等，帮助其他用户选择
              </p>
            </div>
            
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-oppo-sunrise-gold/20 flex items-center justify-center">
                  <MessageSquare className="w-4 h-4 text-oppo-sunrise-gold" />
                </div>
                <h3 className="font-medium">详细描述</h3>
              </div>
              <p className="text-sm text-text-secondary">
                提供清晰的预设说明和使用建议，包括推荐设备和拍摄场景
              </p>
            </div>
          </div>
          
          <button
            onClick={() => setShowSubmitModal(true)}
            className="w-full mt-6 btn-primary py-3 flex items-center justify-center gap-2"
          >
            <Upload className="w-5 h-5" />
            开始贡献
            <ArrowRight className="w-5 h-5" />
          </button>
        </motion.div>

        {/* 排行榜入口 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
          className="card-oppo p-6"
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold flex items-center gap-2">
              <Trophy className="w-5 h-5 text-oppo-sunrise-gold" />
              贡献排行榜
            </h2>
            <button className="text-sm text-ocean-blue hover:underline flex items-center gap-1">
              查看全部
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
          
          <div className="space-y-3">
            {[
              { rank: 1, name: '摄影达人小王', presets: 128, likes: 8923, avatar: 'wang' },
              { rank: 2, name: '城市摄影师', presets: 95, likes: 6789, avatar: 'li' },
              { rank: 3, name: '风光摄影师李华', presets: 87, likes: 5432, avatar: 'hua' }
            ].map((user, index) => (
              <div
                key={user.rank}
                className="flex items-center gap-3 p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors"
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                  user.rank === 1 ? 'bg-oppo-sunrise-gold text-black' :
                  user.rank === 2 ? 'bg-gray-400 text-black' :
                  'bg-amber-700 text-white'
                }`}>
                  {user.rank}
                </div>
                <img 
                  src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${user.avatar}`}
                  alt={user.name}
                  className="w-10 h-10 rounded-full"
                />
                <div className="flex-1">
                  <p className="font-medium">{user.name}</p>
                  <p className="text-xs text-text-secondary">{user.presets} 个预设</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium text-oppo-sunrise-gold">
                    {formatNumber(user.likes)} 赞
                  </p>
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      </main>

      {/* 提交预设弹窗 */}
      {showSubmitModal && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60"
          onClick={() => setShowSubmitModal(false)}
        >
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            transition={{ type: 'spring', damping: 25 }}
            className="w-full sm:max-w-2xl max-h-[90vh] overflow-y-auto bg-deep-space rounded-t-3xl sm:rounded-2xl p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-semibold">贡献你的预设</h2>
              <button
                onClick={() => setShowSubmitModal(false)}
                className="p-2 rounded-full bg-white/5 hover:bg-white/10"
              >
                ✕
              </button>
            </div>

            <div className="space-y-6">
              {/* 基本信息 */}
              <div>
                <label className="block text-sm font-medium mb-2">预设名称</label>
                <input
                  type="text"
                  placeholder="例如：哈苏人像大师"
                  className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 focus:border-oppo-sunrise-gold focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">详细描述</label>
                <textarea
                  rows={4}
                  placeholder="描述你的预设特点、适用场景和使用建议..."
                  className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 focus:border-oppo-sunrise-gold focus:outline-none resize-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">设备型号</label>
                  <select className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 focus:border-oppo-sunrise-gold focus:outline-none">
                    <option value="">选择设备</option>
                    <option value="OPPO Find X8 Ultra">OPPO Find X8 Ultra</option>
                    <option value="OPPO Find X7 Ultra">OPPO Find X7 Ultra</option>
                    <option value="OnePlus 12">OnePlus 12</option>
                    <option value="通用">通用</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">分类</label>
                  <select className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 focus:border-oppo-sunrise-gold focus:outline-none">
                    <option value="">选择分类</option>
                    <option value="portrait">人像</option>
                    <option value="landscape">风光</option>
                    <option value="night">夜景</option>
                    <option value="food">美食</option>
                    <option value="film">胶片</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">标签（用逗号分隔）</label>
                <input
                  type="text"
                  placeholder="例如：哈苏, 人像, 经典"
                  className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 focus:border-oppo-sunrise-gold focus:outline-none"
                />
              </div>

              {/* 贡献协议 */}
              <div className="p-4 rounded-xl bg-white/5 space-y-3">
                <h3 className="font-medium">贡献协议</h3>
                <ul className="text-sm text-text-secondary space-y-2">
                  <li className="flex items-start gap-2">
                    <CheckCircle className="w-4 h-4 text-oppo-green mt-0.5" />
                    <span>提交的预设必须是你原创或已获得授权的</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle className="w-4 h-4 text-oppo-green mt-0.5" />
                    <span>允许其他用户免费使用你的预设</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <CheckCircle className="w-4 h-4 text-oppo-green mt-0.5" />
                    <span>管理员有权对预设进行审核和分类</span>
                  </li>
                </ul>
              </div>

              <button className="w-full btn-primary py-3 flex items-center justify-center gap-2">
                <Upload className="w-5 h-5" />
                提交预设
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  );
}
