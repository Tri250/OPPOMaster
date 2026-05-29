import { motion } from 'framer-motion'
import { Cloud, CloudOff, RefreshCw, Check, AlertCircle, Smartphone, Download, Upload } from 'lucide-react'
import { useState } from 'react'
import { ColorOSSwitch } from '../components/common/ColorOSComponents'

const syncHistory = [
  { id: 1, name: '富士胶片.cube', date: '2024-01-15 14:30', status: 'completed', size: '2.3 MB' },
  { id: 2, name: '城市夜景参数.json', date: '2024-01-15 12:15', status: 'completed', size: '856 KB' },
  { id: 3, name: '徕卡预设.cube', date: '2024-01-14 18:45', status: 'failed', size: '1.8 MB' },
  { id: 4, name: '人像柔光.json', date: '2024-01-14 10:20', status: 'completed', size: '1.2 MB' },
]

export default function CloudSyncPage() {
  const [autoSync, setAutoSync] = useState(false)
  const [syncOnWifi, setSyncOnWifi] = useState(true)
  const [isSyncing, setIsSyncing] = useState(false)

  const handleSync = () => {
    setIsSyncing(true)
    setTimeout(() => setIsSyncing(false), 3000)
  }

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
          <h1 className="text-lg font-semibold">云同步</h1>
          <button 
            onClick={handleSync}
            disabled={isSyncing}
            className="btn-primary text-sm py-2 px-4 flex items-center gap-2 touch-feedback disabled:opacity-50"
            aria-label="立即同步"
          >
            <RefreshCw className={`w-4 h-4 ${isSyncing ? 'animate-spin' : ''}`} />
            {isSyncing ? '同步中...' : '立即同步'}
          </button>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card-oppo p-6"
        >
          <div className="flex items-center gap-4 mb-6">
            <div className="w-16 h-16 rounded-oppo bg-ocean-blue/20 flex items-center justify-center">
              <Cloud className="w-8 h-8 text-ocean-blue" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-semibold mb-1">OPPO Cloud</h2>
              <p className="text-text-secondary text-sm">已同步 48 个预设</p>
              <p className="text-text-tertiary text-xs mt-1">存储空间: 2.5 GB / 5 GB</p>
            </div>
          </div>
          <div className="w-full h-2 bg-white/10 rounded-full overflow-hidden">
            <motion.div 
              initial={{ width: 0 }}
              animate={{ width: '50%' }}
              transition={{ duration: 1 }}
              className="h-full bg-gradient-to-r from-ocean-blue to-ocean-blue-light rounded-full"
            />
          </div>
        </motion.div>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">同步设置</h2>
          <div className="card-oppo divide-y divide-white/5">
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={autoSync}
                onChange={setAutoSync}
                label="自动同步"
                description="有新预设时自动同步到云端"
              />
            </div>
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={syncOnWifi}
                onChange={setSyncOnWifi}
                label="仅在 Wi-Fi 下同步"
                description="节省移动数据流量"
              />
            </div>
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">同步历史</h2>
          <div className="space-y-3">
            {syncHistory.map((item, i) => (
              <motion.div
                key={item.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.2 + i * 0.05 }}
                className="card-oppo p-4 flex items-center gap-4"
              >
                <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                  item.status === 'completed' ? 'bg-oppo-green/20' : 'bg-error-vital/20'
                }`}>
                  {item.status === 'completed' ? (
                    <Check className="w-5 h-5 text-oppo-green" />
                  ) : (
                    <AlertCircle className="w-5 h-5 text-error-vital" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-sm truncate">{item.name}</p>
                  <p className="text-text-tertiary text-xs">{item.date}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-text-tertiary text-xs">{item.size}</span>
                  <button className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors duration-200 touch-feedback" aria-label="重新同步">
                    <RefreshCw className="w-4 h-4" />
                  </button>
                </div>
              </motion.div>
            ))}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">支持的设备</h2>
          <div className="card-oppo p-4">
            <div className="grid grid-cols-3 gap-4">
              <div className="text-center">
                <Smartphone className="w-8 h-8 text-oppo-sunrise-gold mx-auto mb-2" />
                <p className="text-sm font-medium">OPPO Find X7</p>
                <p className="text-text-tertiary text-xs">已连接</p>
              </div>
              <div className="text-center">
                <Smartphone className="w-8 h-8 text-text-tertiary mx-auto mb-2" />
                <p className="text-sm font-medium">OnePlus 12</p>
                <p className="text-text-tertiary text-xs">上次同步: 2小时前</p>
              </div>
              <div className="text-center">
                <Smartphone className="w-8 h-8 text-text-tertiary mx-auto mb-2" />
                <p className="text-sm font-medium">Find N3</p>
                <p className="text-text-tertiary text-xs">上次同步: 昨天</p>
              </div>
            </div>
          </div>
        </motion.section>
      </main>
    </div>
  )
}
