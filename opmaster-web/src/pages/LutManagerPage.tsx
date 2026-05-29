import { motion, AnimatePresence } from 'framer-motion'
import { 
  Layers, Upload, Download, Trash2, Eye, 
  Check, Plus, Folder, FileImage, Filter,
  Search, Grid, List, MoreVertical, Share2
} from 'lucide-react'
import { useState } from 'react'
import { 
  ColorOSCard, ColorOSButton, ColorOSSectionHeader,
  ColorOSAnimations, ColorOSChip, ColorOSProgressBar
} from '../components/common/ColorOSComponents'

interface LutFile {
  id: string
  name: string
  type: '1D' | '3D'
  size: string
  preview: string
  isBuiltIn: boolean
}

const mockLuts: LutFile[] = [
  { id: '1', name: '哈苏经典', type: '3D', size: '128KB', preview: 'hasselblad_classic', isBuiltIn: true },
  { id: '2', name: '电影胶片', type: '3D', size: '256KB', preview: 'film_look', isBuiltIn: true },
  { id: '3', name: '复古暖调', type: '3D', size: '128KB', preview: 'vintage_warm', isBuiltIn: true },
  { id: '4', name: '冷色调', type: '3D', size: '128KB', preview: 'cool_tone', isBuiltIn: false },
  { id: '5', name: '人像优化', type: '3D', size: '64KB', preview: 'portrait_enhance', isBuiltIn: false },
]

export default function LutManagerPage() {
  const [luts, setLuts] = useState<LutFile[]>(mockLuts)
  const [selectedLut, setSelectedLut] = useState<string | null>(null)
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [searchQuery, setSearchQuery] = useState('')
  const [isImporting, setIsImporting] = useState(false)

  const filteredLuts = luts.filter(lut => 
    lut.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleImport = async () => {
    setIsImporting(true)
    await new Promise(resolve => setTimeout(resolve, 1500))
    const newLut: LutFile = {
      id: Date.now().toString(),
      name: `自定义滤镜 ${luts.length + 1}`,
      type: '3D',
      size: '128KB',
      preview: `custom_${Date.now()}`,
      isBuiltIn: false
    }
    setLuts([...luts, newLut])
    setIsImporting(false)
  }

  const handleDelete = (id: string) => {
    setLuts(luts.filter(l => l.id !== id))
    if (selectedLut === id) setSelectedLut(null)
  }

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-80 h-80 top-1/4 -left-40 animate-float" />
        <div className="orb-oppo orb-3 w-64 h-64 bottom-1/3 -right-32 animate-float" style={{ animationDelay: '3s' }} />
      </div>

      <div className="relative max-w-4xl mx-auto px-4 py-8">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
        >
          <div className="flex items-center gap-3 mb-8">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-hasselblad-pro to-oppo-sunrise-gold flex items-center justify-center">
              <Filter className="w-6 h-6 text-deep-space" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">LUT 滤镜管理</h1>
              <p className="text-text-tertiary text-sm">导入和管理 3D LUT 滤镜文件</p>
            </div>
          </div>

          <div className="grid lg:grid-cols-3 gap-6">
            <motion.div 
              className="lg:col-span-2"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <div className="flex items-center justify-between mb-4">
                <ColorOSSectionHeader 
                  title="滤镜库" 
                  subtitle={`${luts.length} 个滤镜`}
                />
                <div className="flex items-center gap-2">
                  <ColorOSChip
                    icon={<Grid className="w-4 h-4" />}
                    label=""
                    selected={viewMode === 'grid'}
                    onClick={() => setViewMode('grid')}
                  />
                  <ColorOSChip
                    icon={<List className="w-4 h-4" />}
                    label=""
                    selected={viewMode === 'list'}
                    onClick={() => setViewMode('list')}
                  />
                </div>
              </div>

              <div className="relative mb-4">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-tertiary" />
                <input
                  type="text"
                  placeholder="搜索滤镜..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-12 pr-4 py-3 bg-white/5 border border-white/10 rounded-oppo-sm text-white placeholder-text-tertiary focus:outline-none focus:border-oppo-sunrise-gold/50"
                />
              </div>

              <AnimatePresence mode="wait">
                {viewMode === 'grid' ? (
                  <motion.div
                    key="grid"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="grid grid-cols-2 sm:grid-cols-3 gap-4"
                  >
                    {filteredLuts.map((lut, index) => (
                      <motion.div
                        key={lut.id}
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: index * 0.05 }}
                      >
                        <ColorOSCard
                          variant="default"
                          interactive
                          className={`overflow-hidden ${selectedLut === lut.id ? 'ring-2 ring-oppo-sunrise-gold' : ''}`}
                        >
                          <div 
                            className="aspect-square bg-gradient-to-br from-oppo-sunrise-gold/20 to-ocean-blue/20 relative cursor-pointer"
                            onClick={() => setSelectedLut(lut.id)}
                          >
                            <div className="absolute inset-0 flex items-center justify-center">
                              <FileImage className="w-12 h-12 text-oppo-sunrise-gold/30" />
                            </div>
                            {selectedLut === lut.id && (
                              <motion.div
                                initial={{ scale: 0 }}
                                animate={{ scale: 1 }}
                                className="absolute top-2 right-2 w-6 h-6 rounded-full bg-oppo-sunrise-gold flex items-center justify-center"
                              >
                                <Check className="w-4 h-4 text-deep-space" />
                              </motion.div>
                            )}
                            {lut.isBuiltIn && (
                              <span className="absolute top-2 left-2 px-2 py-1 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">
                                内置
                              </span>
                            )}
                          </div>
                          <div className="p-3">
                            <p className="text-white font-medium text-sm truncate">{lut.name}</p>
                            <div className="flex items-center justify-between mt-1">
                              <span className="text-text-tertiary text-xs">{lut.type} LUT</span>
                              <span className="text-text-tertiary text-xs">{lut.size}</span>
                            </div>
                          </div>
                        </ColorOSCard>
                      </motion.div>
                    ))}
                  </motion.div>
                ) : (
                  <motion.div
                    key="list"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="space-y-2"
                  >
                    {filteredLuts.map((lut, index) => (
                      <motion.div
                        key={lut.id}
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: index * 0.05 }}
                      >
                        <ColorOSCard
                          variant="default"
                          interactive
                          className={`p-4 ${selectedLut === lut.id ? 'ring-2 ring-oppo-sunrise-gold' : ''}`}
                        >
                          <div className="flex items-center gap-4">
                            <div 
                              className="w-14 h-14 rounded-xl bg-gradient-to-br from-oppo-sunrise-gold/20 to-ocean-blue/20 flex items-center justify-center cursor-pointer"
                              onClick={() => setSelectedLut(lut.id)}
                            >
                              <FileImage className="w-6 h-6 text-oppo-sunrise-gold" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2">
                                <p className="text-white font-medium truncate">{lut.name}</p>
                                {lut.isBuiltIn && (
                                  <span className="px-2 py-0.5 bg-hasselblad-pro text-deep-space text-xs font-bold rounded-full">
                                    内置
                                  </span>
                                )}
                              </div>
                              <p className="text-text-tertiary text-sm">{lut.type} LUT · {lut.size}</p>
                            </div>
                            <div className="flex items-center gap-2">
                              <motion.button
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.9 }}
                                className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center text-text-secondary hover:text-white"
                              >
                                <Eye className="w-4 h-4" />
                              </motion.button>
                              {!lut.isBuiltIn && (
                                <motion.button
                                  whileHover={{ scale: 1.1 }}
                                  whileTap={{ scale: 0.9 }}
                                  onClick={() => handleDelete(lut.id)}
                                  className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center text-text-secondary hover:text-error-vital"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </motion.button>
                              )}
                            </div>
                          </div>
                        </ColorOSCard>
                      </motion.div>
                    ))}
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
            >
              <ColorOSSectionHeader 
                title="导入滤镜" 
                subtitle="支持 .cube 格式"
              />

              <ColorOSCard variant="default" className="p-5">
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleImport}
                  disabled={isImporting}
                  className="w-full aspect-square rounded-xl border-2 border-dashed border-oppo-sunrise-gold/30 flex flex-col items-center justify-center gap-3 text-oppo-sunrise-gold hover:border-oppo-sunrise-gold/50 transition-colors"
                >
                  {isImporting ? (
                    <>
                      <motion.div
                        animate={{ rotate: 360 }}
                        transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                      >
                        <Upload className="w-8 h-8" />
                      </motion.div>
                      <span className="text-sm">导入中...</span>
                    </>
                  ) : (
                    <>
                      <Upload className="w-8 h-8" />
                      <span className="text-sm font-medium">点击导入 LUT 文件</span>
                      <span className="text-xs text-text-tertiary">支持 .cube 格式</span>
                    </>
                  )}
                </motion.button>
              </ColorOSCard>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
                className="mt-6"
              >
                <ColorOSSectionHeader 
                  title="存储空间" 
                  subtitle="滤镜文件占用"
                />

                <ColorOSCard variant="default" className="p-5">
                  <ColorOSProgressBar
                    label="已使用空间"
                    value={luts.length * 128}
                    max={10240}
                    showPercentage
                  />
                  <div className="mt-4 flex items-center justify-between text-sm">
                    <span className="text-text-tertiary">{luts.length * 128} KB</span>
                    <span className="text-text-tertiary">10 MB</span>
                  </div>
                </ColorOSCard>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 }}
                className="mt-6"
              >
                <ColorOSSectionHeader 
                  title="快速操作" 
                  subtitle="批量管理滤镜"
                />

                <div className="space-y-3">
                  <ColorOSButton variant="secondary" className="w-full" icon={<Download className="w-4 h-4" />}>
                    导出全部
                  </ColorOSButton>
                  <ColorOSButton variant="ghost" className="w-full" icon={<Folder className="w-4 h-4" />}>
                    打开存储目录
                  </ColorOSButton>
                </div>
              </motion.div>
            </motion.div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
