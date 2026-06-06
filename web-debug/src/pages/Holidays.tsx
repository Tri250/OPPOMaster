import { useEffect } from 'react'
import { Calendar, Eye } from 'lucide-react'
import { useHolidaysStore } from '@/store'

export default function Holidays() {
  const { holidays, currentHoliday, fetchHolidays, fetchCurrentHoliday, updateHoliday } = useHolidaysStore()

  useEffect(() => {
    fetchHolidays()
    fetchCurrentHoliday()
  }, [])

  const toggleHoliday = (id: string, isActive: boolean) => {
    updateHoliday(id, { isActive: !isActive })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">节日配置</h1>
        <p className="text-gray-400 mt-1">管理节日问候系统</p>
      </div>

      {/* Current Holiday */}
      {currentHoliday && (
        <div 
          className="rounded-xl p-6 text-white"
          style={{
            background: `linear-gradient(135deg, ${currentHoliday.theme.backgroundGradient[0]}, ${currentHoliday.theme.backgroundGradient[1]})`
          }}
        >
          <div className="flex items-center gap-4">
            <span className="text-6xl">{currentHoliday.theme.icon}</span>
            <div>
              <p className="text-white/80 text-sm">当前节日</p>
              <h2 className="text-3xl font-bold">{currentHoliday.name}</h2>
              <p className="text-white/90 mt-1">{currentHoliday.greeting}</p>
            </div>
          </div>
        </div>
      )}

      {/* Holidays List */}
      <div className="bg-[#161B22] rounded-xl border border-[#30363D] overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-[#0D1117] border-b border-[#30363D]">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">节日</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">日期</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">问候语</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">预设数</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">状态</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-400">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#30363D]">
              {holidays.map((holiday) => (
                <tr key={holiday.id} className="hover:bg-[#0D1117]/50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <span className="text-2xl">{holiday.theme.icon}</span>
                      <span className="text-white font-medium">{holiday.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-400">
                    {holiday.startDate === holiday.endDate 
                      ? holiday.startDate 
                      : `${holiday.startDate} ~ ${holiday.endDate}`}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-300 max-w-xs truncate">
                    {holiday.greeting}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-400">
                    {holiday.presetIds.length} 个
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => toggleHoliday(holiday.id, holiday.isActive)}
                      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                        holiday.isActive ? 'bg-[#FF6B35]' : 'bg-gray-600'
                      }`}
                    >
                      <span
                        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                          holiday.isActive ? 'translate-x-6' : 'translate-x-1'
                        }`}
                      />
                    </button>
                  </td>
                  <td className="px-4 py-3">
                    <button className="p-2 hover:bg-[#30363D] rounded-lg text-gray-400 hover:text-white transition-colors">
                      <Eye size={18} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
