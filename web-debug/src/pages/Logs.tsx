import { useEffect } from 'react'
import { Trash2, Search, Filter } from 'lucide-react'
import { useLogsStore } from '@/store'

const levelColors = {
  DEBUG: 'text-gray-400 bg-gray-400/10',
  INFO: 'text-blue-400 bg-blue-400/10',
  WARN: 'text-yellow-400 bg-yellow-400/10',
  ERROR: 'text-red-400 bg-red-400/10'
}

export default function Logs() {
  const { logs, level, search, setLevel, setSearch, fetchLogs, clearLogs } = useLogsStore()

  useEffect(() => {
    fetchLogs()
    const interval = setInterval(fetchLogs, 3000)
    return () => clearInterval(interval)
  }, [level, search])

  const formatTime = (timestamp: number) => {
    return new Date(timestamp).toLocaleTimeString('zh-CN')
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">日志查看器</h1>
          <p className="text-gray-400 mt-1">实时查看应用日志</p>
        </div>
        <button
          onClick={clearLogs}
          className="flex items-center gap-2 px-4 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-lg transition-colors"
        >
          <Trash2 size={18} />
          清空日志
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4 bg-[#161B22] rounded-xl border border-[#30363D] p-4">
        <div className="flex items-center gap-2">
          <Filter size={18} className="text-gray-400" />
          <select
            value={level}
            onChange={(e) => setLevel(e.target.value)}
            className="bg-[#0D1117] border border-[#30363D] rounded-lg px-3 py-2 text-white focus:outline-none focus:border-[#FF6B35]"
          >
            <option value="ALL">所有级别</option>
            <option value="DEBUG">DEBUG</option>
            <option value="INFO">INFO</option>
            <option value="WARN">WARN</option>
            <option value="ERROR">ERROR</option>
          </select>
        </div>
        <div className="flex items-center gap-2 flex-1 min-w-[200px]">
          <Search size={18} className="text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="搜索日志..."
            className="flex-1 bg-[#0D1117] border border-[#30363D] rounded-lg px-3 py-2 text-white placeholder-gray-500 focus:outline-none focus:border-[#FF6B35]"
          />
        </div>
      </div>

      {/* Logs Table */}
      <div className="bg-[#161B22] rounded-xl border border-[#30363D] overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-[#0D1117] border-b border-[#30363D]">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">时间</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">级别</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">标签</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">消息</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#30363D]">
              {logs.map((log) => (
                <tr key={log.id} className="hover:bg-[#0D1117]/50">
                  <td className="px-4 py-3 text-sm text-gray-400 whitespace-nowrap">
                    {formatTime(log.timestamp)}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${levelColors[log.level]}`}>
                      {log.level}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-300">{log.tag}</td>
                  <td className="px-4 py-3 text-sm text-gray-300">
                    <div>{log.message}</div>
                    {log.stackTrace && (
                      <pre className="mt-2 text-xs text-red-400 bg-red-400/5 p-2 rounded overflow-x-auto">
                        {log.stackTrace}
                      </pre>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {logs.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            暂无日志数据
          </div>
        )}
      </div>
    </div>
  )
}
