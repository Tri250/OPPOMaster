import { motion } from 'framer-motion'
import { Upload, File, FolderOpen, Grid, List, Search, Check, Download, Share2, QrCode, Trash2 } from 'lucide-react'
import { useState, useCallback } from 'react'

interface LutFile {
  id: string
  name: string
  size: string
  date: string
  type: 'cube' | 'json' | 'xmp' | 'lrtemplate' | 'dng'
  category: string
  author: string
  compatible: string[]
}

const mockLutFiles: LutFile[] = [
  { id: '1', name: 'Fuji_Pro_400H.cube', size: '2.3 MB', date: '2026-01-15', type: 'cube', category: '胶片', author: '影像大师', compatible: ['小O帮帮', 'Lightroom', 'Photoshop'] },
  { id: '2', name: 'Leica_M_Mono.cube', size: '1.8 MB', date: '2026-01-14', type: 'cube', category: '黑白', author: '光影猎人', compatible: ['小O帮帮', 'Lightroom'] },
  { id: '3', name: 'Cinematic_Tone.json', size: '856 KB', date: '2026-01-13', type: 'json', category: '电影感', author: '色彩玩家', compatible: ['小O帮帮'] },
  { id: '4', name: 'Vintage_Film.xmp', size: '1.2 MB', date: '2026-01-12', type: 'xmp', category: '复古', author: '怀旧玩家', compatible: ['小O帮帮', 'Lightroom', 'Camera Raw'] },
  { id: '5', name: 'Portraits_Soft.lrtemplate', size: '925 KB', date: '2026-01-11', type: 'lrtemplate', category: '人像', author: '摄影师阿东', compatible: ['Lightroom'] },
  { id: '6', name: 'Night_City.cube', size: '2.8 MB', date: '2026-01-10', type: 'cube', category: '夜景', author: '夜拍达人', compatible: ['小O帮帮', 'Lightroom', 'Photoshop', 'DaVinci Resolve'] },
]

const formatOptions = [
  { name: '.cube', desc: '3D LUT 标准格式', compatible: ['小O帮帮', 'Lightroom', 'Photoshop', 'DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'] },
  { name: '.json', desc: '小O帮帮自定义格式', compatible: ['小O帮帮'] },
  { name: '.xmp', desc: 'Adobe XMP 预设', compatible: ['小O帮帮', 'Lightroom', 'Camera Raw', 'Photoshop'] },
  { name: '.lrtemplate', desc: 'Lightroom 旧版预设', compatible: ['Lightroom Classic'] },
  { name: '.dng', desc: 'DNG 配置文件', compatible: ['Lightroom', 'Camera Raw'] },
]

const exportFormats = [
  { name: 'JSON', desc: '小O帮帮格式', icon: <File className="w-4 h-4" /> },
  { name: 'CUBE', desc: '3D LUT 格式', icon: <File className="w-4 h-4" /> },
  { name: 'XMP', desc: 'Adobe 格式', icon: <File className="w-4 h-4" /> },
  { name: '二维码', desc: '分享给好友', icon: <QrCode className="w-4 h-4" /> },
  { name: '链接', desc: '生成分享链接', icon: <Share2 className="w-4 h-4" /> },
]

export default function LutManagerPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [selectedFiles, setSelectedFiles] = useState<string[]>([])
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showImportDialog, setShowImportDialog] = useState(false)
  const [showExportDialog, setShowExportDialog] = useState(false)
  const [dragOver, setDragOver] = useState(false)

  const filteredFiles = mockLutFiles.filter(f => 
    f.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    f.category.toLowerCase().includes(searchQuery.toLowerCase()) ||
    f.author.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const toggleSelect = (id: string) => {
    setSelectedFiles(prev =>
      prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
    )
  }

  const deleteSelected = () => {
    setSelectedFiles([])
    setShowDeleteConfirm(false)
  }

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragOver(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragOver(false)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragOver(false)
    const files = Array.from(e.dataTransfer.files)
    if (files.length > 0) {
      setShowImportDialog(true)
    }
  }, [])

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-8"
        >
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text-oppo">
            预设管理
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            支持多种格式导入导出，打通主流修图工具的预设生态
          </p>
        </motion.div>

        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="card p-4 text-center"
          >
            <div className="text-3xl font-bold text-oppo-orange mb-1">6</div>
            <div className="text-sm text-white/60">预设总数</div>
          </motion.div>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 }}
            className="card p-4 text-center"
          >
            <div className="text-3xl font-bold text-green-500 mb-1">4</div>
            <div className="text-sm text-white/60">CUBE 格式</div>
          </motion.div>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="card p-4 text-center"
          >
            <div className="text-3xl font-bold text-blue-500 mb-1">1</div>
            <div className="text-sm text-white/60">XMP 格式</div>
          </motion.div>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.25 }}
            className="card p-4 text-center"
          >
            <div className="text-3xl font-bold text-purple-500 mb-1">5</div>
            <div className="text-sm text-white/60">支持小O帮帮</div>
          </motion.div>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="card p-4 text-center"
          >
            <div className="text-3xl font-bold text-yellow-500 mb-1">12.5 MB</div>
            <div className="text-sm text-white/60">总大小</div>
          </motion.div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35 }}
          className="mb-6"
        >
          <div className="flex flex-wrap gap-2 items-center justify-between">
            <div className="flex flex-1 gap-2 min-w-[250px]">
              <div className="relative flex-1">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/40" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="搜索预设名称、分类、作者..."
                  className="w-full pl-12 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:border-oppo-orange/50 transition-colors duration-200"
                  aria-label="搜索预设"
                />
              </div>
              <button
                onClick={() => setShowImportDialog(true)}
                className="btn-primary px-4 flex items-center gap-2"
                aria-label="导入预设文件"
              >
                <Upload className="w-5 h-5" />
                <span className="hidden sm:inline">导入</span>
              </button>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2 rounded-lg transition-colors ${
                  viewMode === 'grid' ? 'bg-oppo-orange/20 text-oppo-orange' : 'text-white/40 hover:text-white'
                }`}
                aria-label="网格视图"
              >
                <Grid className="w-5 h-5" />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2 rounded-lg transition-colors ${
                  viewMode === 'list' ? 'bg-oppo-orange/20 text-oppo-orange' : 'text-white/40 hover:text-white'
                }`}
                aria-label="列表视图"
              >
                <List className="w-5 h-5" />
              </button>
            </div>
          </div>
        </motion.div>

        {selectedFiles.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="card p-4 flex items-center justify-between mb-6"
          >
            <span className="text-white/60">
              已选择 {selectedFiles.length} 个文件
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setShowExportDialog(true)}
                className="btn-primary text-sm py-2 px-4 flex items-center gap-2"
              >
                <Download className="w-4 h-4" />
                导出
              </button>
              <button 
                onClick={() => setShowDeleteConfirm(true)}
                className="btn-secondary text-sm py-2 px-4 text-red-400 flex items-center gap-2"
              >
                <Trash2 className="w-4 h-4" />
                删除
              </button>
              <button 
                onClick={() => setSelectedFiles([])}
                className="btn-secondary text-sm py-2 px-4"
              >
                取消
              </button>
            </div>
          </motion.div>
        )}

        {filteredFiles.length === 0 ? (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="card p-12 text-center"
          >
            <FolderOpen className="w-16 h-16 text-white/20 mx-auto mb-4" />
            <p className="text-white/60 mb-2">暂无预设文件</p>
            <p className="text-white/40 text-sm mb-4">点击导入按钮或拖拽文件添加预设</p>
            <button
              onClick={() => setShowImportDialog(true)}
              className="btn-primary flex items-center justify-center gap-2 mx-auto"
            >
              <Upload className="w-5 h-5" />
              导入预设
            </button>
          </motion.div>
        ) : viewMode === 'grid' ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {filteredFiles.map((file, i) => (
              <motion.div
                key={file.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: i * 0.05 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => toggleSelect(file.id)}
                className={`card p-4 cursor-pointer transition-all ${
                  selectedFiles.includes(file.id) ? 'border-oppo-orange ring-2 ring-oppo-orange/30' : ''
                }`}
              >
                <div className="flex items-center justify-between mb-3">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                    file.type === 'cube' ? 'bg-gradient-to-br from-blue-500/30 to-purple-500/30' :
                    file.type === 'json' ? 'bg-gradient-to-br from-green-500/30 to-emerald-500/30' :
                    file.type === 'xmp' ? 'bg-gradient-to-br from-pink-500/30 to-rose-500/30' :
                    'bg-gradient-to-br from-yellow-500/30 to-orange-500/30'
                  }`}>
                    <File className="w-6 h-6" />
                  </div>
                  {selectedFiles.includes(file.id) && (
                    <div className="w-6 h-6 rounded-full bg-oppo-green flex items-center justify-center">
                      <Check className="w-4 h-4 text-oppo-black" />
                    </div>
                  )}
                </div>
                <p className="font-medium text-sm truncate mb-1">{file.name}</p>
                <div className="flex flex-wrap gap-1 mb-2">
                  <span className="px-2 py-0.5 bg-white/10 rounded-full text-xs">{file.category}</span>
                  <span className="px-2 py-0.5 bg-white/10 rounded-full text-xs">{file.type.toUpperCase()}</span>
                </div>
                <p className="text-white/40 text-xs">by {file.author}</p>
                <p className="text-white/40 text-xs">{file.size} · {file.date}</p>
              </motion.div>
            ))}
          </div>
        ) : (
          <div className="space-y-3">
            {filteredFiles.map((file, i) => (
              <motion.div
                key={file.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => toggleSelect(file.id)}
                className={`card p-4 flex items-center gap-4 cursor-pointer transition-all ${
                  selectedFiles.includes(file.id) ? 'border-oppo-orange' : ''
                }`}
              >
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 ${
                  file.type === 'cube' ? 'bg-gradient-to-br from-blue-500/30 to-purple-500/30' :
                  file.type === 'json' ? 'bg-gradient-to-br from-green-500/30 to-emerald-500/30' :
                  file.type === 'xmp' ? 'bg-gradient-to-br from-pink-500/30 to-rose-500/30' :
                  'bg-gradient-to-br from-yellow-500/30 to-orange-500/30'
                }`}>
                  <File className="w-6 h-6" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium truncate">{file.name}</p>
                  <div className="flex flex-wrap gap-2 mt-1">
                    <span className="text-white/40 text-xs">{file.author}</span>
                    <span className="text-white/40 text-xs">·</span>
                    <span className="text-white/40 text-xs">{file.size}</span>
                    <span className="text-white/40 text-xs">·</span>
                    <span className="text-white/40 text-xs">{file.date}</span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 bg-white/10 rounded-full text-xs uppercase">{file.type}</span>
                  <span className="px-2 py-0.5 bg-white/10 rounded-full text-xs">{file.category}</span>
                  {selectedFiles.includes(file.id) && (
                    <div className="w-6 h-6 rounded-full bg-oppo-green flex items-center justify-center">
                      <Check className="w-4 h-4 text-oppo-black" />
                    </div>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        )}

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="mt-8"
        >
          <div className="card p-6">
            <h2 className="text-xl font-bold mb-4">支持的格式</h2>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
              {formatOptions.map((format, i) => (
                <motion.div
                  key={format.name}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.45 + i * 0.05 }}
                  className="p-4 bg-white/5 rounded-xl"
                >
                  <p className="font-bold mb-1">{format.name}</p>
                  <p className="text-white/60 text-sm mb-2">{format.desc}</p>
                  <div className="flex flex-wrap gap-1">
                    {format.compatible.map((comp, j) => (
                      <span key={j} className="text-xs px-2 py-0.5 bg-oppo-orange/20 text-oppo-orange rounded-full">
                        {comp}
                      </span>
                    ))}
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="mt-8"
        >
          <div className="card p-6 bg-gradient-to-br from-oppo-orange/10 to-transparent">
            <h2 className="text-xl font-bold mb-3">💡 使用提示</h2>
            <ul className="space-y-2 text-sm text-white/60">
              <li>• 支持批量导入多个预设文件，拖拽即可上传</li>
              <li>• 小O帮帮会自动识别并转换不同格式的预设</li>
              <li>• 可以通过二维码或链接快速分享预设给好友</li>
              <li>• 支持批量导出为多种格式，方便在其他软件使用</li>
              <li>• 建议定期备份预设，换机时可以快速恢复</li>
            </ul>
          </div>
        </motion.div>
      </div>

      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="card p-6 w-full max-w-sm"
          >
            <h3 className="text-xl font-bold mb-2">确认删除</h3>
            <p className="text-white/60 mb-6">
              确定要删除选中的 {selectedFiles.length} 个文件吗？此操作无法撤销。
            </p>
            <div className="flex gap-3">
              <button 
                onClick={deleteSelected}
                className="flex-1 btn-primary bg-red-500 hover:bg-red-600"
              >
                删除
              </button>
              <button 
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 btn-secondary"
              >
                取消
              </button>
            </div>
          </motion.div>
        </div>
      )}

      {showImportDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="card p-6 w-full max-w-lg"
          >
            <h3 className="text-xl font-bold mb-4">导入预设</h3>
            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              className={`border-2 border-dashed rounded-xl p-8 text-center mb-4 transition-all ${
                dragOver ? 'border-oppo-orange bg-oppo-orange/10' : 'border-white/20 hover:border-white/40'
              }`}
            >
              <Upload className="w-12 h-12 text-white/40 mx-auto mb-3" />
              <p className="text-white/60 mb-2">拖拽文件到此处，或点击选择文件</p>
              <p className="text-sm text-white/40">支持 .cube, .json, .xmp, .lrtemplate, .dng 格式</p>
              <input
                type="file"
                multiple
                accept=".cube,.json,.xmp,.lrtemplate,.dng"
                className="hidden"
                id="file-upload"
              />
              <label htmlFor="file-upload" className="btn-primary inline-flex mt-4 cursor-pointer">
                选择文件
              </label>
            </div>
            <div className="flex gap-3">
              <button 
                onClick={() => setShowImportDialog(false)}
                className="flex-1 btn-secondary"
              >
                取消
              </button>
            </div>
          </motion.div>
        </div>
      )}

      {showExportDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <motion.div
            initial={{ scale: 0.95, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="card p-6 w-full max-w-lg"
          >
            <h3 className="text-xl font-bold mb-4">导出预设</h3>
            <p className="text-white/60 mb-4">已选择 {selectedFiles.length} 个预设，请选择导出格式</p>
            <div className="grid grid-cols-1 gap-3 mb-6">
              {exportFormats.map((format, i) => (
                <button
                  key={i}
                  className="p-4 bg-white/5 rounded-xl flex items-center gap-3 hover:bg-white/10 transition-colors text-left"
                >
                  <div className="w-10 h-10 bg-oppo-orange/20 rounded-lg flex items-center justify-center text-oppo-orange">
                    {format.icon}
                  </div>
                  <div>
                    <p className="font-medium">{format.name}</p>
                    <p className="text-sm text-white/40">{format.desc}</p>
                  </div>
                </button>
              ))}
            </div>
            <div className="flex gap-3">
              <button 
                onClick={() => setShowExportDialog(false)}
                className="flex-1 btn-secondary"
              >
                取消
              </button>
              <button 
                onClick={() => setShowExportDialog(false)}
                className="flex-1 btn-primary"
              >
                开始导出
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  )
}
