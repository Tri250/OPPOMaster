import { motion, AnimatePresence } from 'framer-motion'
import { 
  Cloud, CloudOff, RefreshCw, Check, Upload, 
  Download, Clock, AlertCircle, ChevronRight,
  Smartphone, Archive, Trash2, Settings, Sync
} from 'lucide-react'
import { useState } from 'react'
import { 
  ColorOSCard, ColorOSButton, ColorOSSwitch,
  ColorOSSectionHeader, ColorOSAnimations, ColorOSProgressBar
} from '../components/common/ColorOSComponents'

interface SyncItem {
  id: string
  name: string
  type: 'preset' | 'lut' | 'settings'
  syncedAt: string
  size: string
}

const syncHistory: SyncItem[] = [
  { id: '1', name: '城市夜景大师', type: 'preset', syncedAt: '2分钟前', size: '24KB' },
  { id: '2', name: '人像柔光模式', type: 'preset', syncedAt: '15分钟前', size: '18KB' },
  { id: '3', name: '电影胶片 LUT', type: 'lut', syncedAt: '1小时前', size: '256KB' },
  { id: '4', name: '应用设置', type: 'settings', syncedAt: '3小时前', size: '4KB' },
]

export default function CloudSyncPage() {
  const [isSyncing, setIsSyncing] = useState(false)
  const [autoSync, setAutoSync] = useState(true)
  const [syncOverWifi, setSyncOverWifi] = useState(true)
  const [lastSyncTime, setLastSyncTime] = useState('2分钟前')
  const [storageUsed, setStorageUsed] = useState(128)

  const handleSync = async () => {
    setIsSyncing(true)
    await new Promise(resolve => setTimeout(resolve, 3000))
    setLastSyncTime('刚刚')
    setIsSyncing(false)
  }

  const getTypeIcon = (type: SyncItem['type']) => {
    switch (type) {
      case 'preset': return <Archive className="w-4 h-4" />
      case 'lut': return <Cloud className="w-4 h-4" />
      case 'settings': return <Settings className="w-4 h-4" />
    }
  }

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-2 w-96 h-96 top-1/4 -right-48 animate-float" />
        <div className="orb-oppo orb-1 w-64 h-64 bottom-1/3 -left-32 animate-float" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative max-w-4xl mx-auto px-4 py-8">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
        >
          <div className="flex items-center gap-3 mb-8">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-ocean-blue to-aurora-purple flex items-center justify-center">
              <Cloud className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">云同步</h1>
              <p className="text-text-tertiary text-sm">备份和同步您的预设数据</p>
            </div>
          </div>

          <div className="grid lg:grid-cols-3 gap-6">
            <motion.div 
              className="lg:col-span-2"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <ColorOSCard variant="gradient" className="p-6 mb-6">
                <div className="flex items-center justify-between mb-6">
                  <div className="flex items-center gap-4">
                    <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${
                      isSyncing ? 'bg-ocean-blue/20' : 'bg-oppo-green/20'
                    }`}>
                      {isSyncing ? (
                        <motion.div
                          animate={{ rotate: 360 }}
                          transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                        >
                          <RefreshCw className="w-7 h-7 text-ocean-blue" />
                        </motion.div>
                      ) : (
                        <Cloud className="w-7 h-7 text-oppo-green" />
                      )}
                    </div>
                    <div>
                      <p className="text-white font-semibold text-lg">
                        {isSyncing ? '正在同步...' : '云同步已启用'}
                      </p>
                      <p className="text-text-secondary text-sm">
                        上次同步：{lastSyncTime}
                      </p>
                    </div>
                  </div>
                  <ColorOSButton
                    variant="primary"
                    loading={isSyncing}
                    onClick={handleSync}
                    icon={isSyncing ? undefined : <Sync className="w-4 h-4" />}
                  >
                    {isSyncing ? '同步中' : '立即同步'}
                  </ColorOSButton>
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div className="bg-white/5 rounded-xl p-4 text-center">
                    <p className="text-2xl font-bold text-oppo-sunrise-gold">24</p>
                    <p className="text-text-tertiary text-sm mt-1">预设已同步</p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-4 text-center">
                    <p className="text-2xl font-bold text-ocean-blue">5</p>
                    <p className="text-text-tertiary text-sm mt-1">LUT 已同步</p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-4 text-center">
                    <p className="text-2xl font-bold text-oppo-green">128</p>
                    <p className="text-text-tertiary text-sm mt-1">MB 已使用</p>
                  </div>
                </div>
              </ColorOSCard>

              <ColorOSSectionHeader 
                title="同步历史" 
                subtitle="最近同步的项目"
              />

              <div className="space-y-3">
                {syncHistory.map((item, index) => (
                  <motion.div
                    key={item.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.2 + index * 0.05 }}
                  >
                    <ColorOSCard variant="default" className="p-4">
                      <div className="flex items-center gap-4">
                        <div className="w-10 h-10 rounded-xl bg-oppo-sunrise-gold/10 flex items-center justify-center text-oppo-sunrise-gold">
                          {getTypeIcon(item.type)}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-white font-medium truncate">{item.name}</p>
                          <div className="flex items-center gap-3 text-text-tertiary text-sm">
                            <span className="flex items-center gap-1">
                              <Clock className="w-3 h-3" />
                              {item.syncedAt}
                            </span>
                            <span>{item.size}</span>
                          </div>
                        </div>
                        <Check className="w-5 h-5 text-oppo-green" />
                      </div>
                    </ColorOSCard>
                  </motion.div>
                ))}
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
            >
              <ColorOSSectionHeader 
                title="同步设置" 
                subtitle="自定义同步行为"
              />

              <ColorOSCard variant="default" className="p-5 space-y-4">
                <ColorOSSwitch
                  checked={autoSync}
                  onChange={setAutoSync}
                  label="自动同步"
                  description="在后台自动同步数据"
                />
                <div className="h-px bg-oppo-border/50" />
                <ColorOSSwitch
                  checked={syncOverWifi}
                  onChange={setSyncOverWifi}
                  label="仅 Wi-Fi 同步"
                  description="避免消耗移动数据"
                />
              </ColorOSCard>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 }}
                className="mt-6"
              >
                <ColorOSSectionHeader 
                  title="云端存储" 
                  subtitle="管理存储空间"
                />

                <ColorOSCard variant="default" className="p-5">
                  <ColorOSProgressBar
                    label="已使用"
                    value={storageUsed}
                    max={1024}
                    showPercentage
                  />
                  <div className="mt-4 flex items-center justify-between text-sm">
                    <span className="text-text-tertiary">{storageUsed} MB</span>
                    <span className="text-text-tertiary">1 GB</span>
                  </div>
                  <div className="mt-4 pt-4 border-t border-oppo-border/50">
                    <ColorOSButton variant="ghost" className="w-full" icon={<Download className="w-4 h-4" />}>
                      升级存储空间
                    </ColorOSButton>
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
                  title="数据管理" 
                  subtitle="备份和恢复"
                />

                <div className="space-y-3">
                  <ColorOSCard variant="default" interactive className="p-4">
                    <div className="flex items-center gap-3">
                      <Upload className="w-5 h-5 text-oppo-sunrise-gold" />
                      <span className="text-white font-medium">备份数据</span>
                      <ChevronRight className="w-4 h-4 text-text-tertiary ml-auto" />
                    </div>
                  </ColorOSCard>
                  <ColorOSCard variant="default" interactive className="p-4">
                    <div className="flex items-center gap-3">
                      <Download className="w-5 h-5 text-ocean-blue" />
                      <span className="text-white font-medium">恢复数据</span>
                      <ChevronRight className="w-4 h-4 text-text-tertiary ml-auto" />
                    </div>
                  </ColorOSCard>
                  <ColorOSCard variant="default" interactive className="p-4">
                    <div className="flex items-center gap-3">
                      <Trash2 className="w-5 h-5 text-error-vital" />
                      <span className="text-white font-medium">清除云端数据</span>
                      <ChevronRight className="w-4 h-4 text-text-tertiary ml-auto" />
                    </div>
                  </ColorOSCard>
                </div>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.5 }}
                className="mt-6"
              >
                <ColorOSCard variant="glass" className="p-4">
                  <div className="flex items-center gap-3">
                    <Smartphone className="w-5 h-5 text-oppo-sunrise-gold" />
                    <div className="flex-1">
                      <p className="text-white text-sm font-medium">已连接设备</p>
                      <p className="text-text-tertiary text-xs">OPPO Find X7 Ultra</p>
                    </div>
                    <Check className="w-4 h-4 text-oppo-green" />
                  </div>
                </ColorOSCard>
              </motion.div>
            </motion.div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
