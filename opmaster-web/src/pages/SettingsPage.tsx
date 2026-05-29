import { motion } from 'framer-motion'
import { 
  Moon, Sun, Bell, Wifi, Bluetooth, Palette, 
  Globe, Shield, HelpCircle, Info, ChevronRight,
  Download, Trash2, RefreshCw, Smartphone, Monitor
} from 'lucide-react'
import { useState } from 'react'
import { ColorOSSwitch, ColorOSListItem } from '../components/common/ColorOSComponents'

export default function SettingsPage() {
  const [theme, setTheme] = useState<'dark' | 'light' | 'auto'>('dark')
  const [notifications, setNotifications] = useState(true)
  const [soundEffects, setSoundEffects] = useState(true)
  const [hapticFeedback, setHapticFeedback] = useState(true)
  const [autoSync, setAutoSync] = useState(false)
  const [language, setLanguage] = useState('简体中文')

  const themes = [
    { id: 'dark', label: '深色模式', icon: Moon },
    { id: 'light', label: '浅色模式', icon: Sun },
    { id: 'auto', label: '跟随系统', icon: Monitor }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-white">
      <header className="sticky top-0 z-40 bg-deep-space/90 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <h1 className="text-lg font-semibold">设置</h1>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-4"
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider">外观</h2>
          
          <div className="card-oppo p-4 space-y-3">
            <p className="text-sm font-medium text-text-secondary mb-3">主题模式</p>
            <div className="grid grid-cols-3 gap-2">
              {themes.map((t) => (
                <motion.button
                  key={t.id}
                  onClick={() => setTheme(t.id as typeof theme)}
                  whileTap={{ scale: 0.95 }}
                  className={`flex flex-col items-center gap-2 p-4 rounded-oppo transition-all duration-200 touch-feedback ${
                    theme === t.id
                      ? 'bg-oppo-sunrise-gold/10 border border-oppo-sunrise-gold/30'
                      : 'bg-white/5 border border-transparent hover:bg-white/10'
                  }`}
                >
                  <t.icon className={`w-6 h-6 ${theme === t.id ? 'text-oppo-sunrise-gold' : 'text-text-secondary'}`} />
                  <span className={`text-xs ${theme === t.id ? 'text-white' : 'text-text-tertiary'}`}>
                    {t.label}
                  </span>
                </motion.button>
              ))}
            </div>
          </div>

          <div className="card-oppo divide-y divide-white/5">
            <ColorOSListItem
              icon={<Palette className="w-5 h-5 text-oppo-sunrise-gold" />}
              title="主题色"
              subtitle="哈苏橙"
              trailing={
                <div className="flex items-center gap-2">
                  <div className="w-5 h-5 rounded-full bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro" />
                  <ChevronRight className="w-4 h-4 text-text-tertiary" />
                </div>
              }
            />
            <ColorOSListItem
              icon={<Globe className="w-5 h-5 text-ocean-blue" />}
              title="语言"
              subtitle={language}
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<Monitor className="w-5 h-5 text-aurora-purple" />}
              title="字体大小"
              subtitle="标准"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="space-y-4"
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider">通知与反馈</h2>
          
          <div className="card-oppo divide-y divide-white/5">
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={notifications}
                onChange={setNotifications}
                label="推送通知"
                description="接收新功能和更新提醒"
              />
            </div>
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={soundEffects}
                onChange={setSoundEffects}
                label="音效"
                description="操作反馈音"
              />
            </div>
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={hapticFeedback}
                onChange={setHapticFeedback}
                label="触感反馈"
                description="按钮和交互的震动效果"
              />
            </div>
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="space-y-4"
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider">同步与存储</h2>
          
          <div className="card-oppo divide-y divide-white/5">
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={autoSync}
                onChange={setAutoSync}
                label="自动同步"
                description="Wi-Fi 下自动同步滤镜和参数"
              />
            </div>
            <ColorOSListItem
              icon={<Download className="w-5 h-5 text-oppo-green" />}
              title="下载管理"
              subtitle="最近更新: 2024-01-15"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<Trash2 className="w-5 h-5 text-error-vital" />}
              title="清除缓存"
              subtitle="当前缓存: 128.5 MB"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="space-y-4"
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider">关于</h2>
          
          <div className="card-oppo divide-y divide-white/5">
            <ColorOSListItem
              icon={<Smartphone className="w-5 h-5 text-text-secondary" />}
              title="版本信息"
              subtitle="v1.0.0 (Build 20240115)"
              trailing={<span className="text-oppo-green text-sm font-medium">已是最新</span>}
            />
            <ColorOSListItem
              icon={<RefreshCw className="w-5 h-5 text-ocean-blue" />}
              title="检查更新"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<Shield className="w-5 h-5 text-oppo-green" />}
              title="用户协议"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<HelpCircle className="w-5 h-5 text-aurora-purple" />}
              title="帮助与反馈"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<Info className="w-5 h-5 text-text-secondary" />}
              title="关于 OPPO Master"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
          </div>
        </motion.section>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="pt-6 pb-12 text-center"
        >
          <p className="text-text-tertiary text-sm mb-2">OPPO Master v1.0.0</p>
          <p className="text-text-tertiary text-xs">Made with ❤️ for ColorOS 16</p>
        </motion.div>
      </main>
    </div>
  )
}
