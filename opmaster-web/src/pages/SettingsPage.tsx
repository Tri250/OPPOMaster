import { motion } from 'framer-motion'
import { 
  Moon, Sun, Palette, Globe, Shield, HelpCircle, Info, ChevronRight,
  Download, Trash2, RefreshCw, Smartphone, Monitor, ArrowLeft, 
  Bell, Vibrate, Zap, User, LogOut, UserCircle2
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { 
  ColorOSSwitch, 
  ColorOSListItem, 
  ColorOSSectionHeader,
  ColorOSCard
} from '../components/common/ColorOSComponents'

const easeOppoEnter = [0.05, 0.7, 0.1, 1.0] as [number, number, number, number]

export default function SettingsPage() {
  const [theme, setTheme] = useState<'dark' | 'light' | 'auto'>('dark')
  const [notifications, setNotifications] = useState(true)
  const [soundEffects, setSoundEffects] = useState(true)
  const [hapticFeedback, setHapticFeedback] = useState(true)
  const [autoSync, setAutoSync] = useState(false)

  const themes = [
    { id: 'dark', label: '深色模式', icon: Moon },
    { id: 'light', label: '浅色模式', icon: Sun },
    { id: 'auto', label: '跟随系统', icon: Monitor }
  ]

  return (
    <div className="min-h-screen bg-bg-primary text-text-primary">
      {/* ColorOS 16 背景光效 */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <motion.div
          animate={{ 
            x: [0, 80, 0], 
            y: [0, 40, 0],
          }}
          transition={{ 
            duration: 25, 
            repeat: Infinity, 
            ease: 'easeInOut' 
          }}
          className="absolute -top-52 -left-52 w-[500px] h-[500px] orb-oppo orb-purple"
        />
      </div>

      {/* 顶部导航栏 */}
      <header className="sticky top-0 z-40 glass-navigation">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center">
          <Link to="/" className="flex items-center gap-2 touch-feedback mr-4">
            <div className="p-1.5 rounded-xl hover:bg-white/10 transition-colors">
              <ArrowLeft className="w-5 h-5 text-text-primary" />
            </div>
          </Link>
          <h1 className="text-h2 font-bold">设置</h1>
        </div>
      </header>

      <main className="relative z-10 max-w-4xl mx-auto px-4 py-6 space-y-8">
        {/* 外观设置 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: easeOppoEnter }}
        >
          <ColorOSSectionHeader title="外观" />
          
          {/* 主题选择 */}
          <ColorOSCard className="p-6">
            <p className="text-body2 font-medium text-text-secondary mb-4">主题模式</p>
            <div className="grid grid-cols-3 gap-3">
              {themes.map((t) => (
                <motion.button
                  key={t.id}
                  onClick={() => setTheme(t.id as typeof theme)}
                  whileTap={{ scale: 0.96 }}
                  className={`flex flex-col items-center gap-3 p-4 rounded-2xl transition-all duration-200 touch-feedback ${
                    theme === t.id
                      ? 'bg-gradient-to-br from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 shadow-oppo-elevation-1'
                      : 'bg-bg-secondary border border-transparent hover:bg-white/5'
                  }`}
                >
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                    theme === t.id
                      ? 'bg-gradient-to-br from-oppo-orange to-hasselblad-orange'
                      : 'bg-white/5'
                  }`}>
                    <t.icon className={`w-5 h-5 ${theme === t.id ? 'text-oppo-black' : 'text-text-secondary'}`} />
                  </div>
                  <span className={`text-caption font-semibold ${
                    theme === t.id ? 'text-text-primary' : 'text-text-tertiary'
                  }`}>
                    {t.label}
                  </span>
                </motion.button>
              ))}
            </div>
          </ColorOSCard>

          {/* 外观列表 */}
          <ColorOSCard className="divide-y divide-border-default overflow-hidden">
            <ColorOSListItem
              icon={<Palette className="w-5 h-5 text-oppo-orange" />}
              title="主题色"
              subtitle="哈苏橙"
              trailing={
                <div className="flex items-center gap-2">
                  <div className="w-5 h-5 rounded-full bg-gradient-to-br from-oppo-orange to-hasselblad-orange" />
                  <ChevronRight className="w-4 h-4 text-text-tertiary" />
                </div>
              }
            />
            <ColorOSListItem
              icon={<Globe className="w-5 h-5 text-oppo-blue" />}
              title="语言"
              subtitle="简体中文"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<Monitor className="w-5 h-5 text-oppo-purple" />}
              title="字体大小"
              subtitle="标准"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
          </ColorOSCard>
        </motion.section>

        {/* 通知与反馈 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1, duration: 0.5, ease: easeOppoEnter }}
        >
          <ColorOSSectionHeader title="通知与反馈" />
          
          <ColorOSCard className="divide-y divide-border-default overflow-hidden">
            <div className="px-4 py-1">
              <ColorOSSwitch
                checked={notifications}
                onChange={setNotifications}
                label="推送通知"
                description="接收新功能和更新提醒"
              />
            </div>
            <ColorOSListItem
              icon={<Bell className="w-5 h-5 text-oppo-pink" />}
              title="音效"
              subtitle="操作反馈音"
              trailing={
                <div className="transform scale-75 origin-right">
                  <div className="relative">
                    <div 
                      className={`w-10 h-5 rounded-full transition-colors duration-200 ${
                        soundEffects ? 'bg-oppo-orange' : 'bg-white/15'
                      }`}
                    />
                    <motion.div
                      animate={{ x: soundEffects ? 22 : 2 }}
                      className="absolute top-0.5 w-4 h-4 rounded-full bg-white shadow-lg"
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                    />
                  </div>
                  <button
                    onClick={() => setSoundEffects(!soundEffects)}
                    className="absolute inset-0 z-10"
                  />
                </div>
              }
            />
            <ColorOSListItem
              icon={<Vibrate className="w-5 h-5 text-oppo-green" />}
              title="触感反馈"
              subtitle="按钮和交互的震动效果"
              trailing={
                <div className="transform scale-75 origin-right">
                  <div className="relative">
                    <div 
                      className={`w-10 h-5 rounded-full transition-colors duration-200 ${
                        hapticFeedback ? 'bg-oppo-orange' : 'bg-white/15'
                      }`}
                    />
                    <motion.div
                      animate={{ x: hapticFeedback ? 22 : 2 }}
                      className="absolute top-0.5 w-4 h-4 rounded-full bg-white shadow-lg"
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                    />
                  </div>
                  <button
                    onClick={() => setHapticFeedback(!hapticFeedback)}
                    className="absolute inset-0 z-10"
                  />
                </div>
              }
            />
          </ColorOSCard>
        </motion.section>

        {/* 同步与存储 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2, duration: 0.5, ease: easeOppoEnter }}
        >
          <ColorOSSectionHeader title="同步与存储" />
          
          <ColorOSCard className="divide-y divide-border-default overflow-hidden">
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
              subtitle="最近更新: 2026-01-15"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<Trash2 className="w-5 h-5 text-oppo-pink" />}
              title="清除缓存"
              subtitle="当前缓存: 128.5 MB"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
          </ColorOSCard>
        </motion.section>

        {/* 关于 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.5, ease: easeOppoEnter }}
        >
          <ColorOSSectionHeader title="关于" />
          
          <ColorOSCard className="divide-y divide-border-default overflow-hidden">
            <ColorOSListItem
              icon={<Smartphone className="w-5 h-5 text-text-secondary" />}
              title="版本信息"
              subtitle="v1.0.0 (Build 20260115)"
              trailing={<span className="text-oppo-green text-body2 font-semibold">已是最新</span>}
            />
            <ColorOSListItem
              icon={<RefreshCw className="w-5 h-5 text-oppo-blue" />}
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
              icon={<HelpCircle className="w-5 h-5 text-oppo-purple" />}
              title="帮助与反馈"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<Info className="w-5 h-5 text-text-secondary" />}
              title="关于 小O帮帮"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
          </ColorOSCard>
        </motion.section>

        {/* 账户 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4, duration: 0.5, ease: easeOppoEnter }}
        >
          <ColorOSSectionHeader title="账户" />
          
          <ColorOSCard className="divide-y divide-border-default overflow-hidden">
            <ColorOSListItem
              icon={<UserCircle2 className="w-5 h-5 text-oppo-orange" />}
              title="个人资料"
              subtitle="编辑个人信息"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
            <ColorOSListItem
              icon={<LogOut className="w-5 h-5 text-oppo-pink" />}
              title="退出登录"
              trailing={<ChevronRight className="w-4 h-4 text-text-tertiary" />}
              onClick={() => {}}
            />
          </ColorOSCard>
        </motion.section>

        {/* 版本信息 */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5 }}
          className="pt-4 pb-24 text-center"
        >
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center mx-auto mb-4 shadow-oppo-elevation-2">
            <Zap className="w-8 h-8 text-oppo-black" />
          </div>
          <p className="text-text-tertiary text-body2 mb-1">小O帮帮 v1.0.0</p>
          <p className="text-text-tertiary text-caption">Made with ❤️ for ColorOS 16</p>
        </motion.div>
      </main>

      {/* 底部导航栏 - 移动端 */}
      <nav className="bottom-nav-bar md:hidden">
        <div className="flex items-center justify-around h-full max-w-md mx-auto">
          {[
            { icon: Smartphone, label: '首页', path: '/', active: false },
            { icon: Palette, label: '预设', path: '/filter-library', active: false },
            { icon: Zap, label: 'AI', path: '/ai-demo', active: false },
            { icon: User, label: '我的', path: '/settings', active: true },
          ].map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className="flex flex-col items-center gap-1.5 px-4 py-2.5 touch-feedback-strong"
            >
              <div className={`p-2 rounded-xl transition-all duration-300 ease-out-elastic ${
                item.active 
                  ? 'bg-gradient-to-br from-oppo-orange/20 to-hasselblad-orange/20' 
                  : 'hover:bg-white/8'
              }`}>
                <item.icon className={`w-5.5 h-5.5 ${item.active ? 'text-oppo-orange' : 'text-text-tertiary'}`} />
              </div>
              <span className={`text-caption font-semibold ${
                item.active ? 'text-oppo-orange' : 'text-text-tertiary'
              }`}>
                {item.label}
              </span>
            </Link>
          ))}
        </div>
      </nav>
    </div>
  )
}
