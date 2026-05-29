import { motion } from 'framer-motion'
import { Upload, Download, Trash2, File, FolderOpen, Grid, List, Search, ChevronRight, Check } from 'lucide-react'
import { useState } from 'react'

interface LutFile {
  id: string
  name: string
  size: string
  date: string
  type: 'cube' | 'json'
}

const mockLutFiles: LutFile[] = [
  { id: '1', name: 'Fuji_Pro_400H.cube', size: '2.3 MB', date: '2024-01-15', type: 'cube' },
  { id: '2', name: 'Leica_M_Mono.cube', size: '1.8 MB', date: '2024-01-14', type: 'cube' },
  { id: '3', name: 'Cinematic_Tone.json', size: '856 KB', date: '2024-01-13', type: 'json' },
  { id: '4', name: 'Vintage_Film.cube', size: '3.1 MB', date: '2024-01-12', type: 'cube' },
  { id: '5', name: 'Portraits_Soft.cube', size: '2.5 MB', date: '2024-01-11', type: 'cube' },
  { id: '6', name: 'Night_City.cube', size: '2.8 MB', date: '2024-01-10', type: 'cube' },
]

export default function LutManagerPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [selectedFiles, setSelectedFiles] = useState<string[]>([])
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const filteredFiles = mockLutFiles.filter(f => 
    f.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const toggleSelect = (id: string) => {
    setSelectedFiles(prev =>
      prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
    )
  }

  const selectAll = () => {
    if (selectedFiles.length === filteredFiles.length) {
      setSelectedFiles([])
    } else {
      setSelectedFiles(filteredFiles.map(f => f.id))
    }
  }

  const deleteSelected = () => {
    setSelectedFiles([])
    setShowDeleteConfirm(false)
  }

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between gap-4">
          <h1 className="text-lg font-semibold">LUT 滤镜</h1>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded-oppo transition-colors duration-200 touch-feedback ${
                viewMode === 'grid' ? 'bg-oppo-sunrise-gold/20 text-oppo-sunrise-gold' : 'text-text-tertiary hover:text-white'
              }`}
              aria-label="网格视图"
            >
              <Grid className="w-5 h-5" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-oppo transition-colors duration-200 touch-feedback ${
                viewMode === 'list' ? 'bg-oppo-sunrise-gold/20 text-oppo-sunrise-gold' : 'text-text-tertiary hover:text-white'
              }`}
              aria-label="列表视图"
            >
              <List className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-4 space-y-4">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="搜索 LUT 文件..."
              className="w-full pl-12 pr-4 py-3 bg-white/5 border border-white/10 rounded-oppo text-white placeholder-text-tertiary focus:outline-none focus:border-oppo-sunrise-gold/50 transition-colors duration-200"
              aria-label="搜索 LUT 文件"
            />
          </div>
          <button className="btn-primary px-4 flex items-center gap-2 touch-feedback" aria-label="导入 LUT 文件">
            <Upload className="w-5 h-5" />
            <span className="hidden sm:inline">导入</span>
          </button>
        </div>

        {selectedFiles.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="card-oppo p-4 flex items-center justify-between"
          >
            <span className="text-sm text-text-secondary">
              已选择 {selectedFiles.length} 个文件
            </span>
            <div className="flex gap-2">
              <button className="btn-primary text-sm py-2 touch-feedback">
                导出
              </button>
              <button 
                onClick={() => setShowDeleteConfirm(true)}
                className="btn-secondary text-sm py-2 text-error-vital touch-feedback"
              >
                删除
              </button>
              <button 
                onClick={() => setSelectedFiles([])}
                className="btn-secondary text-sm py-2 touch-feedback"
              >
                取消
              </button>
            </div>
          </motion.div>
        )}

        {filteredFiles.length === 0 ? (
          <div className="card-oppo p-12 text-center">
            <FolderOpen className="w-16 h-16 text-text-tertiary mx-auto mb-4" />
            <p className="text-text-secondary mb-2">暂无 LUT 文件</p>
            <p className="text-text-tertiary text-sm mb-4">点击导入按钮添加 LUT 文件</p>
            <button className="btn-primary touch-feedback" aria-label="导入 LUT 文件">
              <Upload className="w-5 h-5 mr-2" />
              导入 LUT 文件
            </button>
          </div>
        ) : viewMode === 'grid' ? (
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {filteredFiles.map((file, i) => (
              <motion.div
                key={file.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: i * 0.05 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => toggleSelect(file.id)}
                className={`card-oppo p-4 cursor-pointer transition-all duration-200 touch-feedback ${
                  selectedFiles.includes(file.id) ? 'border-oppo-green ring-2 ring-oppo-green/30' : ''
                }`}
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="w-12 h-12 rounded-oppo bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex items-center justify-center">
                    <File className="w-6 h-6 text-oppo-sunrise-gold" />
                  </div>
                  {selectedFiles.includes(file.id) && (
                    <div className="w-6 h-6 rounded-full bg-oppo-green flex items-center justify-center">
                      <Check className="w-4 h-4 text-deep-space" />
                    </div>
                  )}
                </div>
                <p className="font-medium text-sm truncate mb-1">{file.name}</p>
                <p className="text-text-tertiary text-xs">{file.size}</p>
                <p className="text-text-tertiary text-xs mt-1">{file.date}</p>
              </motion.div>
            ))}
          </div>
        ) : (
          <div className="space-y-2">
            {filteredFiles.map((file, i) => (
              <motion.div
                key={file.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => toggleSelect(file.id)}
                className={`card-oppo p-4 flex items-center gap-4 cursor-pointer transition-all duration-200 touch-feedback ${
                  selectedFiles.includes(file.id) ? 'border-oppo-green' : ''
                }`}
              >
                <div className="w-10 h-10 rounded-oppo bg-gradient-to-br from-oppo-sunrise-gold/30 to-ocean-blue/30 flex items-center justify-center flex-shrink-0">
                  <File className="w-5 h-5 text-oppo-sunrise-gold" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-sm truncate">{file.name}</p>
                  <p className="text-text-tertiary text-xs">{file.size} · {file.date}</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 bg-white/10 rounded text-xs uppercase">{file.type}</span>
                  {selectedFiles.includes(file.id) && (
                    <div className="w-6 h-6 rounded-full bg-oppo-green flex items-center justify-center">
                      <Check className="w-4 h-4 text-deep-space" />
                    </div>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        )}

        <div className="card-oppo p-4">
          <h2 className="text-sm font-medium text-text-secondary mb-3">支持的格式</h2>
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">.cube</p>
              <p className="text-text-tertiary text-xs">3D LUT 标准格式</p>
            </div>
            <div className="p-3 bg-white/5 rounded-oppo">
              <p className="font-medium text-sm mb-1">.json</p>
              <p className="text-text-tertiary text-xs">自定义参数格式</p>
            </div>
          </div>
        </div>
      </main>

      {showDeleteConfirm && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md"
          onClick={() => setShowDeleteConfirm(false)}
        >
          <motion.div
            initial={{ scale: 0.95 }}
            animate={{ scale: 1 }}
            className="card-oppo p-6 w-full max-w-sm"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-semibold mb-2">确认删除</h3>
            <p className="text-text-secondary mb-6">
              确定要删除选中的 {selectedFiles.length} 个文件吗？此操作无法撤销。
            </p>
            <div className="flex gap-3">
              <button 
                onClick={deleteSelected}
                className="flex-1 btn-primary bg-error-vital touch-feedback"
              >
                删除
              </button>
              <button 
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 btn-secondary touch-feedback"
              >
                取消
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  )
}
