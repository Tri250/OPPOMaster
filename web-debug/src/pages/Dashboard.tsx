import { useEffect } from 'react'
import { 
  Activity, 
  Wifi, 
  Clock,
  Cpu,
  Database,
  HardDrive
} from 'lucide-react'
import { useStatusStore } from '@/store'
import { 
  AreaChart, 
  Area, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer 
} from 'recharts'

function StatCard({ 
  icon: Icon, 
  label, 
  value, 
  subtext, 
  color 
}: { 
  icon: any
  label: string
  value: string
  subtext?: string
  color: string 
}) {
  return (
    <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-6">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-gray-400 text-sm">{label}</p>
          <p className="text-2xl font-bold text-white mt-1">{value}</p>
          {subtext && <p className="text-sm text-gray-500 mt-1">{subtext}</p>}
        </div>
        <div 
          className="p-3 rounded-lg"
          style={{ backgroundColor: `${color}20` }}
        >
          <Icon size={24} style={{ color }} />
        </div>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { 
    systemStatus, 
    metrics, 
    history,
    fetchStatus, 
    fetchMetrics, 
    fetchHistory 
  } = useStatusStore()

  useEffect(() => {
    fetchStatus()
    fetchMetrics()
    fetchHistory()
    
    const interval = setInterval(() => {
      fetchStatus()
      fetchMetrics()
    }, 5000)
    
    return () => clearInterval(interval)
  }, [])

  const formatUptime = (seconds: number) => {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    return `${hours}小时 ${minutes}分钟`
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">仪表盘</h1>
        <p className="text-gray-400 mt-1">实时监控应用运行状态</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard
          icon={Activity}
          label="应用状态"
          value={systemStatus?.status === 'running' ? '运行中' : '已停止'}
          subtext={`运行时长: ${systemStatus ? formatUptime(systemStatus.uptime) : '-'}`}
          color="#22C55E"
        />
        <StatCard
          icon={HardDrive}
          label="内存使用"
          value={systemStatus ? `${Math.round((systemStatus.memory.used / systemStatus.memory.total) * 100)}%` : '-'}
          subtext={`${systemStatus?.memory.used}MB / ${systemStatus?.memory.total}MB`}
          color="#3B82F6"
        />
        <StatCard
          icon={Wifi}
          label="网络延迟"
          value={systemStatus?.network?.latency ? `${systemStatus.network.latency}ms` : '-'}
          subtext={systemStatus?.network?.connected ? '已连接' : '未连接'}
          color="#8B5CF6"
        />
        <StatCard
          icon={Cpu}
          label="CPU使用率"
          value={metrics ? `${metrics.cpu}%` : '-'}
          subtext={`FPS: ${metrics?.fps || '-'}`}
          color="#F59E0B"
        />
        <StatCard
          icon={Database}
          label="网络流量"
          value={metrics ? `${metrics.network}KB/s` : '-'}
          subtext="实时传输速率"
          color="#EC4899"
        />
        <StatCard
          icon={Clock}
          label="系统时间"
          value={new Date().toLocaleTimeString('zh-CN')}
          subtext={new Date().toLocaleDateString('zh-CN')}
          color="#10B981"
        />
      </div>

      {/* Performance Chart */}
      <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-6">
        <h2 className="text-lg font-semibold text-white mb-4">性能趋势</h2>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={history}>
              <defs>
                <linearGradient id="colorCpu" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#FF6B35" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="#FF6B35" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="colorMemory" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#30363D" />
              <XAxis 
                dataKey="time" 
                stroke="#6B7280"
                fontSize={12}
              />
              <YAxis stroke="#6B7280" fontSize={12} />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#161B22', 
                  border: '1px solid #30363D',
                  borderRadius: '8px'
                }}
                labelStyle={{ color: '#9CA3AF' }}
              />
              <Area 
                type="monotone" 
                dataKey="cpu" 
                stroke="#FF6B35" 
                fillOpacity={1} 
                fill="url(#colorCpu)" 
                name="CPU %"
              />
              <Area 
                type="monotone" 
                dataKey="memory" 
                stroke="#3B82F6" 
                fillOpacity={1} 
                fill="url(#colorMemory)" 
                name="内存 %"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-6">
        <h2 className="text-lg font-semibold text-white mb-4">快捷操作</h2>
        <div className="flex flex-wrap gap-3">
          <button className="px-4 py-2 bg-[#FF6B35] hover:bg-[#FF8C42] text-white rounded-lg font-medium transition-colors">
            刷新数据
          </button>
          <button className="px-4 py-2 bg-[#30363D] hover:bg-[#484F58] text-white rounded-lg font-medium transition-colors">
            导出日志
          </button>
          <button className="px-4 py-2 bg-[#30363D] hover:bg-[#484F58] text-white rounded-lg font-medium transition-colors">
            清理缓存
          </button>
          <button className="px-4 py-2 bg-[#30363D] hover:bg-[#484F58] text-white rounded-lg font-medium transition-colors">
            重启应用
          </button>
        </div>
      </div>
    </div>
  )
}
