import { motion } from 'framer-motion';
import { 
  Rss, 
  Plus, 
  Trash2, 
  RefreshCw, 
  Check, 
  Clock,
  ExternalLink,
  Bell,
  BellOff
} from 'lucide-react';
import { useState } from 'react';
import { useSubscriptionStore } from '../store/useSyncStore';
import { ColorOSSwitch } from '../components/common/ColorOSComponents';

export default function SubscriptionPage() {
  const [showAddModal, setShowAddModal] = useState(false);
  const [newSource, setNewSource] = useState({
    name: '',
    url: '',
    version: '1.0.0',
    updateInterval: 86400000,
    enabled: true,
    autoUpdate: false
  });

  const {
    subscriptions,
    activeSubscription,
    isChecking,
    isSyncing,
    updateAvailable,
    lastUpdate,
    addSubscription,
    removeSubscription,
    setActiveSubscription,
    checkForUpdates,
    syncSubscription,
    toggleEnabled,
    toggleAutoUpdate
  } = useSubscriptionStore();

  const handleAddSubscription = () => {
    if (newSource.name && newSource.url) {
      addSubscription(newSource);
      setShowAddModal(false);
      setNewSource({
        name: '',
        url: '',
        version: '1.0.0',
        updateInterval: 86400000,
        enabled: true,
        autoUpdate: false
      });
    }
  };

  const formatInterval = (ms: number) => {
    const hours = ms / 3600000;
    if (hours >= 24) return `${Math.floor(hours / 24)}天`;
    return `${hours}小时`;
  };

  const formatLastCheck = (date: string | null) => {
    if (!date) return '从未检查';
    const d = new Date(date);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 60) return `${minutes}分钟前`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}小时前`;
    return d.toLocaleDateString();
  };

  return (
    <div className="min-h-screen bg-bg-primary text-text-primary pb-20">
      {/* 顶部导航 */}
      <header className="sticky top-0 z-40 bg-bg-primary/90 backdrop-blur-xl border-b border-border-default">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Rss className="w-5 h-5 text-oppo-orange" />
            <h1 className="text-lg font-semibold">订阅管理</h1>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => checkForUpdates()}
              disabled={isChecking}
              className="p-2 rounded-full bg-bg-secondary hover:bg-white/10 transition-colors"
              aria-label="检查更新"
            >
              <RefreshCw className={`w-4 h-4 ${isChecking ? 'animate-spin' : ''}`} />
            </button>
            <button
              onClick={() => setShowAddModal(true)}
              className="p-2 rounded-full bg-bg-secondary hover:bg-white/10 transition-colors"
              aria-label="添加订阅"
            >
              <Plus className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-6 space-y-6">
        {/* 更新状态卡片 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card-oppo p-6"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className={`w-3 h-3 rounded-full ${updateAvailable ? 'bg-oppo-orange animate-pulse' : 'bg-oppo-green'}`} />
              <span className="text-sm">
                {isChecking ? '正在检查更新...' : updateAvailable ? '发现新预设' : '已是最新版本'}
              </span>
            </div>
            <span className="text-xs text-text-tertiary">
              上次检查: {formatLastCheck(lastUpdate)}
            </span>
          </div>
          {updateAvailable && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="flex items-center gap-3 p-3 rounded-lg bg-oppo-orange/10 border border-oppo-orange/30"
            >
              <Bell className="w-5 h-5 text-oppo-orange" />
              <div className="flex-1">
                <p className="text-sm font-medium">发现新预设</p>
                <p className="text-xs text-text-secondary">订阅源已更新</p>
              </div>
              <button className="btn-primary text-sm py-2 px-4">
                立即更新
              </button>
            </motion.div>
          )}
        </motion.div>

        {/* 订阅列表 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">
            订阅源列表
          </h2>
          <div className="space-y-3">
            {subscriptions.map((sub, index) => (
              <motion.div
                key={sub.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.1 + index * 0.05 }}
                className={`card-oppo p-4 ${activeSubscription === sub.id ? 'ring-2 ring-oppo-orange/50' : ''}`}
              >
                <div className="flex items-start gap-4">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                    sub.enabled ? 'bg-oppo-orange/20' : 'bg-bg-tertiary'
                  }`}>
                    <Rss className={`w-6 h-6 ${sub.enabled ? 'text-oppo-orange' : 'text-text-tertiary'}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <h3 className="font-medium text-base">{sub.name}</h3>
                      {sub.autoUpdate && (
                        <span className="px-2 py-0.5 text-xs rounded-full bg-oppo-green/20 text-oppo-green">
                          自动
                        </span>
                      )}
                      {sub.presets.length > 0 && (
                        <span className="px-2 py-0.5 text-xs rounded-full bg-oppo-blue/20 text-oppo-blue">
                          {sub.presets.length}个预设
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-text-tertiary mt-1 truncate">
                      {sub.url}
                    </p>
                    <div className="flex items-center gap-4 mt-2 text-xs text-text-secondary">
                      <span>v{sub.version}</span>
                      <span className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        {formatInterval(sub.updateInterval)}
                      </span>
                      <span>检查: {formatLastCheck(sub.lastCheck)}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <ColorOSSwitch
                    checked={sub.enabled}
                    onChange={() => toggleEnabled(sub.id)}
                  />
                    <button
                      onClick={() => syncSubscription(sub.id)}
                      disabled={isSyncing}
                      className="p-2 rounded-full bg-bg-tertiary hover:bg-white/10 transition-colors disabled:opacity-50"
                      aria-label="同步"
                    >
                      <RefreshCw className={`w-4 h-4 ${isSyncing ? 'animate-spin' : ''}`} />
                    </button>
                    <button
                      onClick={() => removeSubscription(sub.id)}
                      className="p-2 rounded-full bg-red-500/10 hover:bg-red-500/20 transition-colors"
                      aria-label="删除"
                    >
                      <Trash2 className="w-4 h-4 text-red-500" />
                    </button>
                  </div>
                </div>
                
                {/* 高级设置 */}
                <div className="mt-4 pt-4 border-t border-border-default">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <button
                        onClick={() => toggleAutoUpdate(sub.id)}
                        className={`flex items-center gap-2 text-xs px-3 py-1.5 rounded-full transition-colors ${
                          sub.autoUpdate 
                            ? 'bg-oppo-green/20 text-oppo-green' 
                            : 'bg-bg-tertiary text-text-secondary'
                        }`}
                      >
                        {sub.autoUpdate ? <Bell className="w-3 h-3" /> : <BellOff className="w-3 h-3" />}
                        自动更新
                      </button>
                      <button
                        onClick={() => setActiveSubscription(sub.id)}
                        className={`flex items-center gap-2 text-xs px-3 py-1.5 rounded-full transition-colors ${
                          activeSubscription === sub.id
                            ? 'bg-oppo-blue/20 text-oppo-blue'
                            : 'bg-bg-tertiary text-text-secondary'
                        }`}
                      >
                        {activeSubscription === sub.id ? <Check className="w-3 h-3" /> : null}
                        设为默认
                      </button>
                    </div>
                    <button className="flex items-center gap-1 text-xs text-oppo-blue hover:underline">
                      详细设置
                      <ExternalLink className="w-3 h-3" />
                    </button>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* 订阅统计 */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <h2 className="text-sm font-medium text-text-tertiary uppercase tracking-wider mb-4">
            订阅统计
          </h2>
          <div className="grid grid-cols-3 gap-4">
            <div className="card-oppo p-4 text-center">
              <p className="text-2xl font-bold text-oppo-orange">{subscriptions.length}</p>
              <p className="text-xs text-text-secondary mt-1">订阅源</p>
            </div>
            <div className="card-oppo p-4 text-center">
              <p className="text-2xl font-bold text-oppo-green">
                {subscriptions.filter(s => s.enabled).length}
              </p>
              <p className="text-xs text-text-secondary mt-1">已启用</p>
            </div>
            <div className="card-oppo p-4 text-center">
              <p className="text-2xl font-bold text-oppo-blue">
                {subscriptions.reduce((acc, s) => acc + s.presets.length, 0)}
              </p>
              <p className="text-xs text-text-secondary mt-1">可用预设</p>
            </div>
          </div>
        </motion.section>
      </main>

      {/* 添加订阅弹窗 */}
      {showAddModal && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60"
          onClick={() => setShowAddModal(false)}
        >
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            transition={{ type: 'spring', damping: 25 }}
            className="w-full sm:max-w-md bg-bg-primary rounded-t-3xl sm:rounded-2xl p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold">添加订阅源</h2>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-2 rounded-full bg-bg-tertiary hover:bg-white/10"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm text-text-secondary mb-2">订阅名称</label>
                <input
                  type="text"
                  value={newSource.name}
                  onChange={(e) => setNewSource({ ...newSource, name: e.target.value })}
                  placeholder="例如：官方预设库"
                  className="w-full px-4 py-3 rounded-xl bg-bg-secondary border border-border-default focus:border-oppo-orange focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-sm text-text-secondary mb-2">订阅地址</label>
                <input
                  type="url"
                  value={newSource.url}
                  onChange={(e) => setNewSource({ ...newSource, url: e.target.value })}
                  placeholder="https://example.com/presets.json"
                  className="w-full px-4 py-3 rounded-xl bg-bg-secondary border border-border-default focus:border-oppo-orange focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-sm text-text-secondary mb-2">版本号</label>
                <input
                  type="text"
                  value={newSource.version}
                  onChange={(e) => setNewSource({ ...newSource, version: e.target.value })}
                  placeholder="1.0.0"
                  className="w-full px-4 py-3 rounded-xl bg-bg-secondary border border-border-default focus:border-oppo-orange focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-sm text-text-secondary mb-2">更新间隔</label>
                <select
                  value={newSource.updateInterval}
                  onChange={(e) => setNewSource({ ...newSource, updateInterval: Number(e.target.value) })}
                  className="w-full px-4 py-3 rounded-xl bg-bg-secondary border border-border-default focus:border-oppo-orange focus:outline-none"
                >
                  <option value={3600000}>1小时</option>
                  <option value={86400000}>24小时</option>
                  <option value={172800000}>48小时</option>
                  <option value={604800000}>7天</option>
                </select>
              </div>

              <div className="flex items-center justify-between py-4">
                <span className="text-sm">启用订阅</span>
                <ColorOSSwitch
                  checked={newSource.enabled}
                  onChange={(v) => setNewSource({ ...newSource, enabled: v })}
                />
              </div>

              <div className="flex items-center justify-between">
                <span className="text-sm">自动更新</span>
                <ColorOSSwitch
                  checked={newSource.autoUpdate}
                  onChange={(v) => setNewSource({ ...newSource, autoUpdate: v })}
                />
              </div>

              <button
                onClick={handleAddSubscription}
                disabled={!newSource.name || !newSource.url}
                className="w-full btn-primary py-3 mt-4 disabled:opacity-50"
              >
                添加订阅
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  );
}
