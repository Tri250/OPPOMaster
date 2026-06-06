import { useEffect } from 'react'
import { Search, Trash2, Star, Download, Plus } from 'lucide-react'
import { usePresetsStore } from '@/store'

export default function Presets() {
  const { presets, search, sort, setSearch, setSort, fetchPresets, deletePreset } = usePresetsStore()

  useEffect(() => {
    fetchPresets()
  }, [search, sort])

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">预设管理</h1>
          <p className="text-gray-400 mt-1">管理应用预设数据</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2 bg-[#FF6B35] hover:bg-[#FF8C42] text-white rounded-lg transition-colors">
          <Plus size={18} />
          新增预设
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4 bg-[#161B22] rounded-xl border border-[#30363D] p-4">
        <div className="flex items-center gap-2 flex-1 min-w-[200px]">
          <Search size={18} className="text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="搜索预设..."
            className="flex-1 bg-[#0D1117] border border-[#30363D] rounded-lg px-3 py-2 text-white placeholder-gray-500 focus:outline-none focus:border-[#FF6B35]"
          />
        </div>
        <select
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          className="bg-[#0D1117] border border-[#30363D] rounded-lg px-3 py-2 text-white focus:outline-none focus:border-[#FF6B35]"
        >
          <option value="newest">最新发布</option>
          <option value="popular">最受欢迎</option>
          <option value="rating">评分最高</option>
        </select>
      </div>

      {/* Presets Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {presets.map((preset) => (
          <div key={preset.id} className="bg-[#161B22] rounded-xl border border-[#30363D] overflow-hidden group hover:border-[#FF6B35]/50 transition-colors">
            <div className="relative aspect-video">
              <img 
                src={preset.coverUrl} 
                alt={preset.name}
                className="w-full h-full object-cover"
              />
              {preset.isNew && (
                <span className="absolute top-2 left-2 px-2 py-1 bg-[#FF6B35] text-white text-xs font-bold rounded">
                  NEW
                </span>
              )}
            </div>
            <div className="p-4">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="text-white font-semibold">{preset.name}</h3>
                  <p className="text-sm text-gray-400">{preset.author}</p>
                </div>
                <div className="flex items-center gap-1 text-yellow-400">
                  <Star size={14} fill="currentColor" />
                  <span className="text-sm">{preset.rating}</span>
                </div>
              </div>
              <p className="text-sm text-gray-500 mt-2 line-clamp-2">{preset.description}</p>
              <div className="flex flex-wrap gap-1 mt-3">
                {preset.tags.map(tag => (
                  <span key={tag} className="px-2 py-0.5 bg-[#30363D] text-gray-300 text-xs rounded">
                    {tag}
                  </span>
                ))}
              </div>
              <div className="flex items-center justify-between mt-4 pt-4 border-t border-[#30363D]">
                <div className="flex items-center gap-1 text-gray-400 text-sm">
                  <Download size={14} />
                  {preset.downloadCount}
                </div>
                <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button className="p-1.5 hover:bg-[#30363D] rounded text-gray-400 hover:text-white transition-colors">
                    编辑
                  </button>
                  <button 
                    onClick={() => deletePreset(preset.id)}
                    className="p-1.5 hover:bg-red-500/10 rounded text-gray-400 hover:text-red-400 transition-colors"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
