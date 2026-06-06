import { useEffect } from 'react'
import { Smartphone, Cpu, HardDrive, Battery, Thermometer } from 'lucide-react'
import { useDeviceStore } from '@/store'

function InfoCard({ 
  icon: Icon, 
  title, 
  items 
}: { 
  icon: any
  title: string
  items: { label: string; value: string }[]
}) {
  return (
    <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-6">
      <div className="flex items-center gap-3 mb-4">
        <div className="p-2 bg-[#FF6B35]/10 rounded-lg">
          <Icon size={20} className="text-[#FF6B35]" />
        </div>
        <h3 className="text-white font-semibold">{title}</h3>
      </div>
      <div className="space-y-3">
        {items.map((item, i) => (
          <div key={i} className="flex justify-between text-sm">
            <span className="text-gray-400">{item.label}</span>
            <span className="text-white">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function Device() {
  const { deviceInfo, performance, appInfo, fetchDeviceInfo, fetchPerformance, fetchAppInfo } = useDeviceStore()

  useEffect(() => {
    fetchDeviceInfo()
    fetchPerformance()
    fetchAppInfo()
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">设备信息</h1>
        <p className="text-gray-400 mt-1">查看设备和应用详细信息</p>
      </div>

      {/* Performance Stats */}
      {performance && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-4 text-center">
            <Cpu size={24} className="mx-auto text-[#FF6B35] mb-2" />
            <p className="text-2xl font-bold text-white">{performance.cpuUsage}%</p>
            <p className="text-sm text-gray-400">CPU使用率</p>
          </div>
          <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-4 text-center">
            <HardDrive size={24} className="mx-auto text-blue-400 mb-2" />
            <p className="text-2xl font-bold text-white">{performance.memoryUsage}%</p>
            <p className="text-sm text-gray-400">内存使用</p>
          </div>
          <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-4 text-center">
            <Thermometer size={24} className="mx-auto text-yellow-400 mb-2" />
            <p className="text-2xl font-bold text-white">{performance.temperature}°C</p>
            <p className="text-sm text-gray-400">设备温度</p>
          </div>
          <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-4 text-center">
            <Battery size={24} className="mx-auto text-green-400 mb-2" />
            <p className="text-2xl font-bold text-white">{performance.battery}%</p>
            <p className="text-sm text-gray-400">电池电量</p>
          </div>
        </div>
      )}

      {/* Device Info */}
      {deviceInfo && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <InfoCard
            icon={Smartphone}
            title="设备信息"
            items={[
              { label: '型号', value: deviceInfo.model },
              { label: '制造商', value: deviceInfo.manufacturer },
              { label: 'Android版本', value: deviceInfo.androidVersion },
              { label: '屏幕分辨率', value: `${deviceInfo.screenWidth} x ${deviceInfo.screenHeight}` },
              { label: '屏幕密度', value: `${deviceInfo.density}x` },
            ]}
          />
          <InfoCard
            icon={Cpu}
            title="硬件信息"
            items={[
              { label: '处理器', value: deviceInfo.cpu },
              { label: 'GPU', value: deviceInfo.gpu },
              { label: '总内存', value: `${Math.round(deviceInfo.totalMemory / 1024)}GB` },
              { label: '可用内存', value: `${Math.round(deviceInfo.availableMemory / 1024)}GB` },
              { label: '总存储', value: `${Math.round(deviceInfo.totalStorage / 1024)}GB` },
            ]}
          />
        </div>
      )}

      {/* App Info */}
      {appInfo && (
        <div className="bg-[#161B22] rounded-xl border border-[#30363D] p-6">
          <h3 className="text-white font-semibold mb-4">应用信息</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p className="text-sm text-gray-400">应用名称</p>
              <p className="text-white">{appInfo.appName}</p>
            </div>
            <div>
              <p className="text-sm text-gray-400">版本号</p>
              <p className="text-white">{appInfo.version} ({appInfo.buildNumber})</p>
            </div>
            <div>
              <p className="text-sm text-gray-400">包名</p>
              <p className="text-white font-mono text-sm">{appInfo.packageName}</p>
            </div>
            <div>
              <p className="text-sm text-gray-400">数据库大小</p>
              <p className="text-white">{appInfo.databaseSize}MB</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
