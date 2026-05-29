import { motion } from 'framer-motion'
import { 
  Settings, Sun, Moon, Monitor, ChevronRight, 
  Info, Heart, ExternalLink, Shield, Bell, 
  Cloud, Smartphone, Palette, Globe, FileText,
  Mail, MessageCircle, Camera
} from 'lucide-react'
import { useState } from 'react'
import { 
  ColorOSCard, ColorOSListItem, ColorOSSectionHeader,
  ColorOSRadioGroup, ColorOSSwitch, ColorOSAnimations
} from '../components/common/ColorOSComponents'

type ThemeMode = 'system' | 'light' | 'dark'

export default function SettingsPage() {
  const [themeMode, setThemeMode] = useState<ThemeMode>('system')
  const [notifications, setNotifications] = useState(true)
  const [autoSync, setAutoSync] = useState(true)
  const [showThemeDialog, setShowThemeDialog] = useState(false)

  const themeOptions = [
    { value: 'system', label: '跟随系统', description: '自动跟随系统深浅色模式' },
    { value: 'light', label: '浅色模式', description: '始终使用浅色主题' },
    { value: 'dark', label: '深色模式', description: '始终使用深色主题' }
  ]

  const getThemeIcon = (mode: ThemeMode) => {
    switch (mode) {
      case 'light': return <Sun className="w-5 h-5 text-oppo-sunrise-gold" />
      case 'dark': return <Moon className="w-5 h-5 text-aurora-purple" />
      default: return <Monitor className="w-5 h-5 text-ocean-blue" />
    }
  }

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-72 h-72 top-20 -right-36 animate-float" />
        <div className="orb-oppo orb-2 w-56 h-56 bottom-40 -left-28 animate-float" style={{ animationDelay: '3s' }} />
      </div>

      <div className="relative max-w-2xl mx-auto px-4 py-8">
        <motion.div
          initial="initial"
          animate="animate"
          variants={ColorOSAnimations.fadeIn}
        >
          <div className="flex items-center gap-3 mb-8">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
              <Settings className="w-6 h-6 text-deep-space" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">设置</h1>
              <p className="text-text-tertiary text-sm">个性化您的体验</p>
            </div>
          </div>

          <div className="space-y-6">
            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
            >
              <ColorOSSectionHeader 
                title="外观" 
                subtitle="自定义应用外观和显示"
              />
              <ColorOSCard variant="default">
                <ColorOSListItem
                  icon={getThemeIcon(themeMode)}
                  title="主题模式"
                  subtitle={themeOptions.find(o => o.value === themeMode)?.label}
                  showArrow
                  onClick={() => setShowThemeDialog(true)}
                />
                <div className="h-px bg-oppo-border/50 mx-4" />
                <ColorOSListItem
                  icon={<Palette className="w-5 h-5 text-sakura-pink" />}
                  title="强调色"
                  subtitle="OPPO 日出金"
                  trailing={
                    <div className="w-6 h-6 rounded-full bg-oppo-sunrise-gold shadow-lg" />
                  }
                  showArrow
                />
              </ColorOSCard>
            </motion.section>

            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
            >
              <ColorOSSectionHeader 
                title="功能" 
                subtitle="管理应用功能设置"
              />
              <ColorOSCard variant="default">
                <div className="p-4">
                  <ColorOSSwitch
                    checked={notifications}
                    onChange={setNotifications}
                    label="推送通知"
                    description="接收新预设和更新通知"
                  />
                </div>
                <div className="h-px bg-oppo-border/50 mx-4" />
                <div className="p-4">
                  <ColorOSSwitch
                    checked={autoSync}
                    onChange={setAutoSync}
                    label="自动同步"
                    description="自动同步预设到云端"
                  />
                </div>
                <div className="h-px bg-oppo-border/50 mx-4" />
                <ColorOSListItem
                  icon={<Shield className="w-5 h-5 text-oppo-green" />}
                  title="隐私设置"
                  subtitle="管理数据隐私权限"
                  showArrow
                />
              </ColorOSCard>
            </motion.section>

            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
            >
              <ColorOSSectionHeader 
                title="存储" 
                subtitle="管理本地存储空间"
              />
              <ColorOSCard variant="default">
                <ColorOSListItem
                  icon={<Cloud className="w-5 h-5 text-ocean-blue" />}
                  title="云端存储"
                  subtitle="已使用 128 MB / 1 GB"
                  trailing={
                    <div className="w-24 h-2 bg-white/10 rounded-full overflow-hidden">
                      <div className="w-1/4 h-full bg-ocean-blue rounded-full" />
                    </div>
                  }
                />
                <div className="h-px bg-oppo-border/50 mx-4" />
                <ColorOSListItem
                  icon={<Smartphone className="w-5 h-5 text-aurora-purple" />}
                  title="本地缓存"
                  subtitle="256 MB"
                  showArrow
                />
              </ColorOSCard>
            </motion.section>

            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
            >
              <ColorOSSectionHeader 
                title="关于" 
                subtitle="应用信息和联系方式"
              />
              <ColorOSCard variant="gradient" className="p-6">
                <div className="flex items-center gap-4 mb-6">
                  <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center shadow-lg">
                    <Camera className="w-8 h-8 text-deep-space" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold gradient-text-oppo">小O帮帮</h3>
                    <p className="text-text-secondary text-sm">OMaster for Hasselblad</p>
                  </div>
                </div>
                
                <div className="grid grid-cols-2 gap-4 mb-6">
                  <div className="bg-white/5 rounded-xl p-4 text-center">
                    <p className="text-2xl font-bold text-oppo-sunrise-gold">1.0.0</p>
                    <p className="text-text-tertiary text-xs mt-1">当前版本</p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-4 text-center">
                    <p className="text-2xl font-bold text-oppo-green">50K+</p>
                    <p className="text-text-tertiary text-xs mt-1">活跃用户</p>
                  </div>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center gap-3 text-text-secondary text-sm">
                    <Heart className="w-4 h-4 text-sakura-pink" />
                    <span>热爱摄影的：小陈工</span>
                  </div>
                  <div className="flex items-center gap-3 text-text-secondary text-sm">
                    <Globe className="w-4 h-4 text-ocean-blue" />
                    <span>专为 OPPO 哈苏影像系统打造</span>
                  </div>
                </div>
              </ColorOSCard>
            </motion.section>

            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
            >
              <ColorOSSectionHeader 
                title="联系我" 
                subtitle="有问题或建议？欢迎联系"
              />
              <ColorOSCard variant="default">
                <ColorOSListItem
                  icon={<MessageCircle className="w-5 h-5 text-oppo-sunrise-gold" />}
                  title="抖音 / 小红书"
                  subtitle="搜索「带娃的小陈工」"
                  trailing={
                    <ExternalLink className="w-4 h-4 text-text-tertiary" />
                  }
                  onClick={() => window.open('https://www.douyin.com/', '_blank')}
                />
                <div className="h-px bg-oppo-border/50 mx-4" />
                <ColorOSListItem
                  icon={<Mail className="w-5 h-5 text-ocean-blue" />}
                  title="邮件反馈"
                  subtitle="support@omaster.app"
                  trailing={
                    <ExternalLink className="w-4 h-4 text-text-tertiary" />
                  }
                />
              </ColorOSCard>
            </motion.section>

            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6 }}
            >
              <ColorOSCard variant="default">
                <ColorOSListItem
                  icon={<FileText className="w-5 h-5 text-text-secondary" />}
                  title="用户协议"
                  showArrow
                />
                <div className="h-px bg-oppo-border/50 mx-4" />
                <ColorOSListItem
                  icon={<Shield className="w-5 h-5 text-text-secondary" />}
                  title="隐私政策"
                  showArrow
                />
              </ColorOSCard>
            </motion.section>

            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.7 }}
              className="text-center py-8"
            >
              <p className="text-text-tertiary text-sm">
                © 2026 小O帮帮. All rights reserved.
              </p>
              <p className="text-text-tertiary/50 text-xs mt-2">
                Made with ❤️ for ColorOS 16
              </p>
            </motion.div>
          </div>
        </motion.div>
      </div>

      {showThemeDialog && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
          onClick={() => setShowThemeDialog(false)}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            className="w-full max-w-md bg-card-surface rounded-oppo-md border border-oppo-border/50 overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6">
              <h2 className="text-xl font-semibold text-white mb-6">选择主题</h2>
              <ColorOSRadioGroup
                options={themeOptions}
                value={themeMode}
                onChange={(v) => {
                  setThemeMode(v as ThemeMode)
                  setShowThemeDialog(false)
                }}
              />
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  )
}
