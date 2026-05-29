import { useState, useMemo } from 'react';
import { motion } from 'framer-motion';
import { Search, Filter, ArrowUpDown, Star, Download, Clock, Zap, Camera, Sparkles } from 'lucide-react';
import PageLayout from '../components/common/PageLayout';
import {
  mockPresets,
  styleCategories,
  sceneCategories,
  modelCategories,
  sortOptions,
  Preset,
} from '../data/presetsData';

export default function NewFilterLibraryPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeStyle, setActiveStyle] = useState('all');
  const [activeScene, setActiveScene] = useState('all');
  const [activeModel, setActiveModel] = useState('all');
  const [sortBy, setSortBy] = useState('match');
  const [showFilterSheet, setShowFilterSheet] = useState(false);
  const [showSemanticSearch, setShowSemanticSearch] = useState(false);
  const [semanticQuery, setSemanticQuery] = useState('');

  const filteredPresets = useMemo(() => {
    let result = [...mockPresets];

    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      result = result.filter(
        (preset) =>
          preset.name.toLowerCase().includes(query) ||
          preset.description.toLowerCase().includes(query) ||
          preset.author.toLowerCase().includes(query) ||
          preset.tags.some((tag) => tag.toLowerCase().includes(query))
      );
    }

    if (activeStyle !== 'all') {
      result = result.filter((preset) => preset.style === activeStyle);
    }

    if (activeScene !== 'all') {
      result = result.filter((preset) => preset.scene === activeScene);
    }

    if (activeModel !== 'all') {
      result = result.filter((preset) => preset.compatibleModels.includes(activeModel));
    }

    switch (sortBy) {
      case 'hot':
        result.sort((a, b) => b.downloads - a.downloads);
        break;
      case 'favorites':
        result.sort((a, b) => b.favorites - a.favorites);
        break;
      case 'new':
        result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        break;
      case 'usage':
        result.sort((a, b) => b.usageCount - a.usageCount);
        break;
      default:
        break;
    }

    return result;
  }, [searchQuery, activeStyle, activeScene, activeModel, sortBy]);

  const handleSemanticSearch = () => {
    setSearchQuery(semanticQuery);
    setShowSemanticSearch(false);
  };

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-4 py-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <h1 className="text-h2 font-bold text-white mb-2">预设库</h1>
          <p className="text-body-lg text-neutral-400">
            探索 1000+ 专业预设，找到适合你的完美滤镜
          </p>
        </motion.div>

        <div className="mb-6">
          <div className="relative mb-4">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-neutral-500 w-5 h-5" />
            <input
              type="text"
              placeholder="搜索预设、作者、标签..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-12 pr-24 py-3.5 bg-neutral-900/50 border border-neutral-800 rounded-2xl text-white placeholder-neutral-500 focus:outline-none focus:border-oppo-orange focus:ring-2 focus:ring-oppo-orange/20 transition-all"
            />
            <button
              onClick={() => setShowSemanticSearch(true)}
              className="absolute right-2 top-1/2 -translate-y-1/2 bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-white px-4 py-1.5 rounded-xl text-sm font-medium flex items-center gap-1.5 hover:opacity-90 transition-opacity"
            >
              <Sparkles className="w-4 h-4" />
              AI 搜索
            </button>
          </div>

          <div className="flex flex-wrap gap-2 mb-4">
            {styleCategories.slice(0, 6).map((cat) => (
              <button
                key={cat.id}
                onClick={() => setActiveStyle(activeStyle === cat.id ? 'all' : cat.id)}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                  activeStyle === cat.id
                    ? 'bg-oppo-orange text-white'
                    : 'bg-neutral-900/50 text-neutral-400 hover:bg-neutral-800'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>

          <div className="flex items-center justify-between">
            <div className="flex gap-2 overflow-x-auto pb-2">
              {sceneCategories.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => setActiveScene(activeScene === cat.id ? 'all' : cat.id)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all whitespace-nowrap ${
                    activeScene === cat.id
                      ? 'bg-oppo-blue text-white'
                      : 'bg-neutral-900/50 text-neutral-500 hover:bg-neutral-800'
                  }`}
                >
                  {cat.label}
                </button>
              ))}
            </div>

            <div className="flex items-center gap-3">
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="bg-neutral-900/50 border border-neutral-800 text-white text-sm rounded-xl px-3 py-2 focus:outline-none focus:border-oppo-orange"
              >
                {sortOptions.map((opt) => (
                  <option key={opt.id} value={opt.id}>
                    {opt.label}
                  </option>
                ))}
              </select>

              <button
                onClick={() => setShowFilterSheet(true)}
                className="bg-neutral-900/50 border border-neutral-800 text-white p-2.5 rounded-xl hover:bg-neutral-800 transition-colors"
              >
                <Filter className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {filteredPresets.map((preset, index) => (
            <PresetCard key={preset.id} preset={preset} index={index} />
          ))}
        </div>

        {filteredPresets.length === 0 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center py-20"
          >
            <Camera className="w-16 h-16 text-neutral-700 mx-auto mb-4" />
            <h3 className="text-h4 text-white mb-2">没有找到预设</h3>
            <p className="text-body text-neutral-500">尝试调整搜索条件或分类</p>
          </motion.div>
        )}
      </div>

      {showFilterSheet && (
        <FilterBottomSheet
          isOpen={showFilterSheet}
          onClose={() => setShowFilterSheet(false)}
          activeModel={activeModel}
          onModelChange={setActiveModel}
        />
      )}

      {showSemanticSearch && (
        <SemanticSearchModal
          isOpen={showSemanticSearch}
          onClose={() => setShowSemanticSearch(false)}
          query={semanticQuery}
          onQueryChange={setSemanticQuery}
          onSearch={handleSemanticSearch}
        />
      )}
    </PageLayout>
  );
}

function PresetCard({ preset, index }: { preset: Preset; index: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: index * 0.05 }}
      className="group bg-neutral-900/50 border border-neutral-800 rounded-2xl overflow-hidden hover:border-oppo-orange/50 transition-all hover:shadow-xl hover:shadow-oppo-orange/5"
    >
      <div className="relative aspect-[4/3] overflow-hidden">
        <img
          src={preset.coverImage}
          alt={preset.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
        {preset.badge && (
          <div className="absolute top-3 left-3 bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-white text-xs font-bold px-2.5 py-1 rounded-lg">
            {preset.badge}
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
        <div className="absolute bottom-3 left-3 right-3 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <button className="flex-1 bg-white/20 backdrop-blur-sm text-white text-sm font-medium py-2 rounded-xl hover:bg-white/30 transition-colors">
            预览
          </button>
          <button className="bg-oppo-orange text-white px-4 py-2 rounded-xl hover:bg-oppo-orange/90 transition-colors">
            使用
          </button>
        </div>
      </div>

      <div className="p-4">
        <h3 className="font-bold text-white mb-1 truncate">{preset.name}</h3>
        <p className="text-sm text-neutral-500 mb-3">by {preset.author}</p>

        <div className="flex flex-wrap gap-1.5 mb-3">
          {preset.tags.slice(0, 3).map((tag, i) => (
            <span
              key={i}
              className="text-xs text-neutral-400 bg-neutral-800 px-2 py-0.5 rounded-lg"
            >
              {tag}
            </span>
          ))}
        </div>

        <div className="flex items-center justify-between text-xs text-neutral-500">
          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1">
              <Download className="w-3.5 h-3.5" />
              {preset.downloads.toLocaleString()}
            </span>
            <span className="flex items-center gap-1">
              <Star className="w-3.5 h-3.5" />
              {preset.favorites.toLocaleString()}
            </span>
          </div>
          <span className="flex items-center gap-1">
            <Clock className="w-3.5 h-3.5" />
            {preset.createdAt}
          </span>
        </div>
      </div>
    </motion.div>
  );
}

function FilterBottomSheet({
  isOpen,
  onClose,
  activeModel,
  onModelChange,
}: {
  isOpen: boolean;
  onClose: () => void;
  activeModel: string;
  onModelChange: (id: string) => void;
}) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50">
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />
      <motion.div
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        className="absolute bottom-0 left-0 right-0 bg-neutral-900 rounded-t-3xl border-t border-neutral-800"
      >
        <div className="p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-h4 font-bold text-white">筛选</h2>
            <button onClick={onClose} className="text-neutral-400 hover:text-white">
              ✕
            </button>
          </div>

          <div className="mb-6">
            <h3 className="text-body font-semibold text-white mb-3">适配机型</h3>
            <div className="grid grid-cols-2 gap-2">
              {modelCategories.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => onModelChange(cat.id)}
                  className={`py-3 rounded-xl text-sm font-medium transition-all ${
                    activeModel === cat.id
                      ? 'bg-oppo-orange text-white'
                      : 'bg-neutral-800 text-neutral-400 hover:bg-neutral-700'
                  }`}
                >
                  {cat.label}
                </button>
              ))}
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-full bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-white font-semibold py-3.5 rounded-2xl"
          >
            应用筛选
          </button>
        </div>
      </motion.div>
    </div>
  );
}

function SemanticSearchModal({
  isOpen,
  onClose,
  query,
  onQueryChange,
  onSearch,
}: {
  isOpen: boolean;
  onClose: () => void;
  query: string;
  onQueryChange: (q: string) => void;
  onSearch: () => void;
}) {
  if (!isOpen) return null;

  const examples = [
    '适合夜景拍人像的胶片预设',
    '美食拍摄用的清新风格',
    '城市街拍赛博朋克滤镜',
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="relative bg-neutral-900 border border-neutral-800 rounded-3xl w-full max-w-lg overflow-hidden"
      >
        <div className="p-6">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-gradient-to-br from-oppo-orange to-hasselblad-orange rounded-xl flex items-center justify-center">
                <Sparkles className="w-5 h-5 text-white" />
              </div>
              <h2 className="text-h4 font-bold text-white">AI 智能搜索</h2>
            </div>
            <button onClick={onClose} className="text-neutral-400 hover:text-white">
              ✕
            </button>
          </div>

          <p className="text-body text-neutral-400 mb-4">
            用自然语言描述你想要的效果，AI 会帮你找到最合适的预设
          </p>

          <input
            type="text"
            placeholder="例如：适合拍人像的清新风格预设"
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onSearch()}
            className="w-full px-4 py-3 bg-neutral-800 border border-neutral-700 rounded-xl text-white placeholder-neutral-500 focus:outline-none focus:border-oppo-orange mb-4"
          />

          <div className="space-y-2 mb-6">
            <p className="text-sm text-neutral-500">试试这些：</p>
            {examples.map((ex, i) => (
              <button
                key={i}
                onClick={() => {
                  onQueryChange(ex);
                  onSearch();
                }}
                className="w-full text-left px-4 py-2.5 bg-neutral-800/50 text-neutral-400 rounded-xl text-sm hover:bg-neutral-800 hover:text-white transition-colors"
              >
                {ex}
              </button>
            ))}
          </div>

          <button
            onClick={onSearch}
            className="w-full bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-white font-semibold py-3.5 rounded-xl flex items-center justify-center gap-2"
          >
            <Zap className="w-5 h-5" />
            开始搜索
          </button>
        </div>
      </motion.div>
    </div>
  );
}
