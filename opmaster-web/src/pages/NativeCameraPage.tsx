import { motion, AnimatePresence } from 'framer-motion'
import { 
  Copy, Check, Smartphone, Zap, Shield, Wifi, WifiOff, Camera, 
  Settings, RefreshCw, AlertTriangle, UploadCloud, Image as ImageIcon
} from 'lucide-react'
import { useState, useCallback } from 'react'
import { ColorOSCard, ColorOSButton } from '../components/common/ColorOSComponents'

// 2026金标版本相机品牌模板
const cameraBrands = [
  {
    id: 'oppo',
    name: 'OPPO Find X9 Ultra',
    version: '2026金标基线',
    icon: '📱',
    supportedSensors: ['IMX989', 'IMX858', 'IMX890', 'OV08K10'],
    lenses: [
      {
        id: 'main',
        name: '主摄',
        sensor: 'IMX989',
        params: {
          '色彩模式': 'HNCS 自然',
          '专业模式': '开启',
          'HDR': '智能',
          'AI 场景增强': '开启',
          '夜景模式': '智能',
          'RAW格式': 'DNG',
          '快门速度': '自动',
          'ISO': '自动',
          '白平衡': '自动',
          '曝光补偿': '0EV'
        }
      },
      {
        id: 'ultrawide',
        name: '超广角',
        sensor: 'IMX858',
        params: {
          '色彩模式': 'HNCS 自然',
          '专业模式': '开启',
          '畸变校正': '开启',
          '边缘增强': '开启'
        }
      },
      {
        id: 'telephoto',
        name: '长焦',
        sensor: 'IMX890',
        params: {
          '色彩模式': 'HNCS 自然',
          '专业模式': '开启',
          '防抖': 'OIS+EIS',
          '3倍变焦': '光学'
        }
      },
      {
        id: 'front',
        name: '前置',
        sensor: 'OV08K10',
        params: {
          '美颜模式': '自然',
          '人像模式': '开启',
          'HDR': '自动'
        }
      }
    ]
  },
  {
    id: 'oneplus',
    name: 'OnePlus 13 Pro',
    version: '2026金标基线',
    icon: '📱',
    supportedSensors: ['LYT-808', 'LYT-600', 'IMX890'],
    lenses: [
      {
        id: 'main',
        name: '主摄',
        sensor: 'LYT-808',
        params: {
          '色彩模式': '自然',
          '专业模式': '开启',
          'HDR': '自动',
          'AI 增强': '开启',
          'Nightscape': '开启',
          '快门速度': '自动',
          'ISO': '自动'
        }
      },
      {
        id: 'ultrawide',
        name: '超广角',
        sensor: 'LYT-600',
        params: {
          '色彩模式': '自然',
          '专业模式': '开启',
          '畸变校正': '开启'
        }
      },
      {
        id: 'telephoto',
        name: '长焦',
        sensor: 'IMX890',
        params: {
          '色彩模式': '自然',
          '专业模式': '开启',
          '3倍变焦': '光学'
        }
      }
    ]
  },
  {
    id: 'realme',
    name: 'realme GT7 Pro',
    version: '2026金标基线',
    icon: '📱',
    supportedSensors: ['IMX890', 'IMX766'],
    lenses: [
      {
        id: 'main',
        name: '主摄',
        sensor: 'IMX890',
        params: {
          '色彩模式': '鲜明',
          '专业模式': '开启',
          'HDR': '自动',
          'AI 场景': '开启',
          '星空模式': '开启',
          '快门速度': '自动',
          'ISO': '自动'
        }
      },
      {
        id: 'telephoto',
        name: '长焦',
        sensor: 'IMX766',
        params: {
          '色彩模式': '鲜明',
          '专业模式': '开启',
          '3倍变焦': '光学'
        }
      }
    ]
  },
  {
    id: 'xiaomi',
    name: '小米 15 Ultra',
    version: '2026金标基线',
    icon: '📱',
    supportedSensors: ['IMX989', 'IMX858'],
    lenses: [
      {
        id: 'main',
        name: '主摄',
        sensor: 'IMX989',
        params: {
          '色彩模式': '徕卡自然',
          '专业模式': '开启',
          'HDR': '自动',
          'AI 魔法分身': '关闭',
          '徕卡水印': '开启',
          '快门速度': '自动',
          'ISO': '自动'
        }
      },
      {
        id: 'ultrawide',
        name: '超广角',
        sensor: 'IMX858',
        params: {
          '色彩模式': '徕卡自然',
          '专业模式': '开启',
          '徕卡水印': '开启'
        }
      }
    ]
  },
  {
    id: 'vivo',
    name: 'vivo X200 Pro',
    version: '2026金标基线',
    icon: '📱',
    supportedSensors: ['LYT-900', 'LYT-808'],
    lenses: [
      {
        id: 'main',
        name: '主摄',
        sensor: 'LYT-900',
        params: {
          '色彩模式': '蔡司自然',
          '专业模式': '开启',
          'HDR': '自动',
          '蔡司光学镜头': '启用',
          'V3 芯片': '开启',
          '快门速度': '自动',
          'ISO': '自动'
        }
      },
      {
        id: 'telephoto',
        name: '长焦',
        sensor: 'LYT-808',
        params: {
          '色彩模式': '蔡司自然',
          '专业模式': '开启',
          'V3 芯片': '开启'
        }
      }
    ]
  },
  {
    id: 'honor',
    name: '荣耀 Magic7 Pro',
    version: '2026金标基线',
    icon: '📱',
    supportedSensors: ['IMX989', 'IMX858'],
    lenses: [
      {
        id: 'main',
        name: '主摄',
        sensor: 'IMX989',
        params: {
          '色彩模式': '标准',
          '专业模式': '开启',
          'HDR': '智能',
          'AI 摄影': '开启',
          '快门速度': '自动',
          'ISO': '自动'
        }
      },
      {
        id: 'ultrawide',
        name: '超广角',
        sensor: 'IMX858',
        params: {
          '色彩模式': '标准',
          '专业模式': '开启'
        }
      }
    ]
  }
]

// 模拟硬件设备数据
const mockDevices = [
  {
    id: 'device-001',
    name: 'OPPO Find X9 Ultra',
    brand: 'oppo',
    status: 'online',
    sensors: ['IMX989', 'IMX858', 'IMX890', 'OV08K10'],
    ip: '192.168.1.100'
  },
  {
    id: 'device-002',
    name: 'OnePlus 13 Pro',
    brand: 'oneplus',
    status: 'online',
    sensors: ['LYT-808', 'LYT-600', 'IMX890'],
    ip: '192.168.1.101'
  },
  {
    id: 'device-003',
    name: '未知机型',
    brand: 'unknown',
    status: 'online',
    sensors: ['NEW-SENSOR-001'],
    ip: '192.168.1.102'
  },
  {
    id: 'device-004',
    name: '离线设备',
    brand: 'xiaomi',
    status: 'offline',
    sensors: ['IMX989'],
    ip: '192.168.1.103'
  }
]

const accessibilitySteps = [
  {
    title: '开启无障碍服务',
    desc: '在系统设置中找到"无障碍"，找到小O帮帮服务并开启',
    icon: <Shield className="w-6 h-6" />
  },
  {
    title: '选择相机品牌',
    desc: '选择您的手机品牌，系统会自动适配相应的相机控件',
    icon: <Smartphone className="w-6 h-6" />
  },
  {
    title: '一键自动填入',
    desc: '打开原生相机大师模式，点击悬浮窗即可自动填入所有参数',
    icon: <Zap className="w-6 h-6" />
  }
]

export default function NativeCameraPage() {
  const [selectedBrand, setSelectedBrand] = useState('oppo')
  const [selectedLens, setSelectedLens] = useState('main')
  const [copied, setCopied] = useState(false)
  const [isScanning, setIsScanning] = useState(false)
  const [isFilling, setIsFilling] = useState(false)
  const [showUploadModal, setShowUploadModal] = useState(false)
  const [devices, setDevices] = useState(mockDevices)
  const [fillProgress, setFillProgress] = useState(0)
  const [fillSuccess, setFillSuccess] = useState(false)
  const [isOfflineMode, setIsOfflineMode] = useState(false)
  const [showAccessibilityPrompt, setShowAccessibilityPrompt] = useState(false)
  
  const currentBrand = cameraBrands.find(b => b.id === selectedBrand)
  const currentLens = currentBrand?.lenses.find(l => l.id === selectedLens)

  // 模拟检查无障碍服务状态
  const checkAccessibility = useCallback(() => {
    // 模拟随机状态，实际项目中应该调用原生API
    const enabled = Math.random() > 0.5
    if (!enabled) {
      setShowAccessibilityPrompt(true)
    }
    return enabled
  }, [])

  // 模拟一键自动填入
  const handleAutoFill = useCallback(() => {
    // 先检查无障碍服务
    if (!checkAccessibility()) {
      return
    }
    
    setIsFilling(true)
    setFillProgress(0)
    
    // 模拟填入过程
    const steps = 10
    let currentStep = 0
    const interval = setInterval(() => {
      currentStep++
      setFillProgress(Math.round((currentStep / steps) * 100))
      
      if (currentStep >= steps) {
        clearInterval(interval)
        setIsFilling(false)
        setFillSuccess(true)
        
        // 3秒后重置成功状态
        setTimeout(() => setFillSuccess(false), 3000)
      }
    }, 300)
  }, [checkAccessibility])

  // 复制参数
  const handleCopy = useCallback(() => {
    if (!currentLens) return
    
    const text = Object.entries(currentLens.params)
      .map(([key, value]) => `${key}: ${value}`)
      .join('\n')
    
    navigator.clipboard.writeText(text)
      .then(() => {
        setCopied(true)
        setTimeout(() => setCopied(false), 2000)
      })
  }, [currentLens])

  // 硬件扫描
  const handleScanDevices = useCallback(() => {
    setIsScanning(true)
    
    // 模拟扫描过程
    setTimeout(() => {
      setIsScanning(false)
      setDevices(mockDevices.map(d => ({
        ...d,
        status: Math.random() > 0.3 ? 'online' : 'offline'
      })))
    }, 2000)
  }, [])

  // 批量填充
  const handleBatchFill = useCallback(() => {
    // 检查是否有未收录的Sensor
    const hasUnknownSensors = devices.some(d => 
      d.status === 'online' && !cameraBrands.some(b => 
        b.id === d.brand && d.sensors.some(s => b.supportedSensors.includes(s))
      )
    )
    
    if (hasUnknownSensors) {
      setShowUploadModal(true)
      return
    }
    
    // 过滤离线设备
    const onlineDevices = devices.filter(d => d.status === 'online')
    console.log('开始批量填充任务，过滤后的在线设备:', onlineDevices)
  }, [devices])

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-orange to-hasselblad-orange rounded-2xl mb-6">
            <Smartphone className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4 bg-gradient-to-r from-white via-oppo-orange to-hasselblad-orange bg-clip-text text-transparent">
            原生相机参数自动填入
          </h1>
          <div className="flex items-center justify-center gap-4 mb-4">
            <span className="text-sm bg-oppo-orange/20 text-oppo-orange px-3 py-1 rounded-full font-medium">
              2026金标基线
            </span>
            {isOfflineMode ? (
              <span className="text-sm bg-gray-500/20 text-gray-300 px-3 py-1 rounded-full font-medium flex items-center gap-1">
                <WifiOff className="w-3.5 h-3.5" />
                离线模式
              </span>
            ) : (
              <span className="text-sm bg-green-500/20 text-green-400 px-3 py-1 rounded-full font-medium flex items-center gap-1">
                <Wifi className="w-3.5 h-3.5" />
                在线模式
              </span>
            )}
          </div>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            基于安卓无障碍服务，无需Root权限，一键自动填入相机参数，让摄影更简单
          </p>
        </motion.div>

        {/* 模式切换 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className="mb-8"
        >
          <ColorOSCard className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                {isOfflineMode ? (
                  <WifiOff className="w-5 h-5 text-gray-400" />
                ) : (
                  <Wifi className="w-5 h-5 text-green-400" />
                )}
                <span className="font-medium">
                  {isOfflineMode ? '本地模板库' : '云端同步模式'}
                </span>
              </div>
              <button
                onClick={() => setIsOfflineMode(!isOfflineMode)}
                className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors text-sm"
              >
                切换模式
              </button>
            </div>
          </ColorOSCard>
        </motion.div>

        {/* Main Features */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="mb-12"
        >
          <div className="grid md:grid-cols-3 gap-6">
            {accessibilitySteps.map((step, idx) => (
              <motion.div
                key={idx}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.15 + idx * 0.05 }}
                className="p-6 bg-white/5 backdrop-blur-xl rounded-2xl border border-white/10"
              >
                <div className="w-12 h-12 bg-oppo-orange/20 rounded-xl flex items-center justify-center text-oppo-orange mb-4">
                  {step.icon}
                </div>
                <h3 className="text-lg font-bold mb-2">{step.title}</h3>
                <p className="text-white/60 text-sm">{step.desc}</p>
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* 硬件设备扫描区域 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-8"
        >
          <ColorOSCard className="p-8">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-bold">硬件识别与模板匹配</h2>
              <ColorOSButton
                onClick={handleScanDevices}
                loading={isScanning}
                icon={<RefreshCw className="w-5 h-5" />}
              >
                {isScanning ? '扫描中...' : '扫描设备'}
              </ColorOSButton>
            </div>

            <div className="space-y-4">
              {devices.map((device) => {
                const hasTemplate = cameraBrands.some(b => 
                  b.id === device.brand && 
                  device.sensors.some(s => b.supportedSensors.includes(s))
                )
                
                return (
                  <motion.div
                    key={device.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    className={`p-4 rounded-xl border ${
                      device.status === 'online' 
                        ? 'bg-white/5 border-white/10' 
                        : 'bg-gray-800/30 border-gray-700/30'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className={`w-3 h-3 rounded-full ${
                          device.status === 'online' ? 'bg-green-500' : 'bg-gray-500'
                        }`} />
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-medium">{device.name}</span>
                            {device.status === 'online' && hasTemplate && (
                              <span className="text-xs bg-oppo-orange/20 text-oppo-orange px-2 py-0.5 rounded-full">
                                2026金标
                              </span>
                            )}
                          </div>
                          <div className="text-xs text-white/50">
                            {device.sensors.join(', ')} • {device.ip}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {device.status === 'offline' && (
                          <span className="text-xs text-red-400 bg-red-500/10 px-2 py-1 rounded-full">
                            离线
                          </span>
                        )}
                        {device.status === 'online' && !hasTemplate && (
                          <ColorOSButton
                            variant="secondary"
                            size="sm"
                            icon={<UploadCloud className="w-4 h-4" />}
                            onClick={() => setShowUploadModal(true)}
                          >
                            上传模板
                          </ColorOSButton>
                        )}
                      </div>
                    </div>
                  </motion.div>
                )
              })}
            </div>

            <div className="mt-6 pt-4 border-t border-white/10">
              <ColorOSButton
                onClick={handleBatchFill}
                icon={<Zap className="w-5 h-5" />}
                className="w-full"
              >
                批量填充参数
              </ColorOSButton>
            </div>
          </ColorOSCard>
        </motion.div>

        {/* Brand Selection */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
          className="mb-8"
        >
          <ColorOSCard className="p-8">
            <h2 className="text-xl font-bold mb-6">选择您的手机品牌</h2>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              {cameraBrands.map((brand) => (
                <motion.button
                  key={brand.id}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setSelectedBrand(brand.id)}
                  className={`p-6 rounded-xl transition-all duration-200 border-2 ${
                    selectedBrand === brand.id
                      ? 'bg-oppo-orange/20 border-oppo-orange/50'
                      : 'bg-white/5 border-white/10 hover:bg-white/10'
                  }`}
                >
                  <div className="text-4xl mb-3">{brand.icon}</div>
                  <p className={`font-medium ${
                    selectedBrand === brand.id ? 'text-oppo-orange' : 'text-white'
                  }`}>{brand.name}</p>
                  {selectedBrand === brand.id && (
                    <p className="text-xs text-oppo-orange/70 mt-1">{brand.version}</p>
                  )}
                </motion.button>
              ))}
            </div>
          </ColorOSCard>
        </motion.div>

        {/* 镜头切换 */}
        {currentBrand && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="mb-8"
          >
            <ColorOSCard className="p-6">
              <h3 className="text-lg font-bold mb-4">选择镜头</h3>
              <div className="flex flex-wrap gap-2">
                {currentBrand.lenses.map((lens) => (
                  <motion.button
                    key={lens.id}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => setSelectedLens(lens.id)}
                    className={`px-4 py-2 rounded-xl border transition-all duration-200 ${
                      selectedLens === lens.id
                        ? 'bg-oppo-orange/20 border-oppo-orange/50 text-oppo-orange'
                        : 'bg-white/5 border-white/10 text-white hover:bg-white/10'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <Camera className="w-4 h-4" />
                      <span>{lens.name}</span>
                    </div>
                    <div className="text-xs opacity-70 mt-1">{lens.sensor}</div>
                  </motion.button>
                ))}
              </div>
            </ColorOSCard>
          </motion.div>
        )}

        {/* Parameters Display */}
        {currentBrand && currentLens && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.35 }}
            className="mb-8"
          >
            <ColorOSCard className="p-8">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="text-xl font-bold">
                    {currentBrand.name} - {currentLens.name}
                  </h2>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-xs text-oppo-orange bg-oppo-orange/20 px-2 py-0.5 rounded-full">
                      {currentLens.sensor}
                    </span>
                    <span className="text-xs text-white/50">
                      {currentBrand.version}
                    </span>
                  </div>
                </div>
                <button 
                  onClick={handleCopy}
                  className="px-4 py-2 rounded-xl bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-white font-medium flex items-center gap-2 transition-all hover:opacity-90"
                  aria-label="复制参数"
                >
                  {copied ? (
                    <>
                      <Check className="w-4 h-4" />
                      已复制
                    </>
                  ) : (
                    <>
                      <Copy className="w-4 h-4" />
                      复制参数
                    </>
                  )}
                </button>
              </div>
              
              <div className="grid md:grid-cols-2 gap-4">
                {Object.entries(currentLens.params).map(([key, value]) => (
                  <motion.div
                    key={key}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="flex items-center justify-between p-4 bg-white/5 rounded-xl"
                  >
                    <span className="text-white/60">{key}</span>
                    <span className="text-white font-medium">{value}</span>
                  </motion.div>
                ))}
              </div>

              {/* 操作按钮 */}
              <div className="mt-8 flex flex-col sm:flex-row gap-3">
                <ColorOSButton
                  onClick={handleAutoFill}
                  loading={isFilling}
                  icon={fillSuccess ? <Check className="w-5 h-5" /> : <Zap className="w-5 h-5" />}
                  className="flex-1"
                  variant="primary"
                  size="lg"
                >
                  {isFilling 
                    ? `填入中 ${fillProgress}%` 
                    : fillSuccess 
                      ? '填入成功！' 
                      : '一键自动填入'
                  }
                </ColorOSButton>
              </div>
            </ColorOSCard>
          </motion.div>
        )}

        {/* Instructions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <ColorOSCard className="p-8">
            <h2 className="text-xl font-bold mb-6">使用说明</h2>
            
            <div className="space-y-6">
              <div className="flex gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-oppo-orange rounded-full flex items-center justify-center text-oppo-black font-bold">1</div>
                <div>
                  <h3 className="font-medium mb-1">安装小O帮帮APP</h3>
                  <p className="text-white/60 text-sm">从官网下载并安装小O帮帮APP到您的手机</p>
                </div>
              </div>

              <div className="flex gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-oppo-orange rounded-full flex items-center justify-center text-oppo-black font-bold">2</div>
                <div>
                  <h3 className="font-medium mb-1">开启无障碍服务</h3>
                  <p className="text-white/60 text-sm">进入设置 → 无障碍 → 找到"小O帮帮"并开启</p>
                </div>
              </div>

              <div className="flex gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-oppo-orange rounded-full flex items-center justify-center text-oppo-black font-bold">3</div>
                <div>
                  <h3 className="font-medium mb-1">选择您的机型和镜头</h3>
                  <p className="text-white/60 text-sm">在APP中选择您的手机品牌、型号和镜头</p>
                </div>
              </div>

              <div className="flex gap-4">
                <div className="flex-shrink-0 w-8 h-8 bg-oppo-orange rounded-full flex items-center justify-center text-oppo-black font-bold">4</div>
                <div>
                  <h3 className="font-medium mb-1">一键自动填入</h3>
                  <p className="text-white/60 text-sm">打开原生相机大师模式，点击悬浮窗即可自动填入所有参数</p>
                </div>
              </div>
            </div>

            {/* Backup Option */}
            <div className="mt-8 p-6 bg-yellow-500/10 border border-yellow-500/20 rounded-xl">
              <h3 className="text-lg font-bold mb-3 text-yellow-400">兜底方案：悬浮窗复制粘贴</h3>
              <p className="text-white/60 text-sm">
                对于无法自动适配的机型，您可以使用悬浮窗一键复制参数，然后在相机中手动粘贴。
                我们会持续更新支持更多机型！
              </p>
            </div>
          </ColorOSCard>
        </motion.div>
      </div>

      {/* 上传适配模板Modal */}
      <AnimatePresence>
        {showUploadModal && (
          <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="w-full max-w-md"
            >
              <ColorOSCard className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-bold">需要适配新机型</h3>
                  <button
                    onClick={() => setShowUploadModal(false)}
                    className="p-2 rounded-lg hover:bg-white/10"
                  >
                    ✕
                  </button>
                </div>
                
                <div className="mb-4 p-4 bg-yellow-500/10 border border-yellow-500/20 rounded-xl">
                  <div className="flex items-start gap-3">
                    <AlertTriangle className="w-5 h-5 text-yellow-400 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="font-medium text-yellow-400">检测到未收录的Sensor</p>
                      <p className="text-sm text-white/60 mt-1">请上传适配模板后再进行填充操作</p>
                    </div>
                  </div>
                </div>
                
                <div className="border-2 border-dashed border-white/20 rounded-xl p-8 text-center mb-4">
                  <ImageIcon className="w-10 h-10 mx-auto mb-3 text-white/40" />
                  <p className="text-white/60 mb-3">点击或拖拽上传适配模板</p>
                  <p className="text-xs text-white/40">支持JSON、XML格式，最大5MB</p>
                </div>
                
                <div className="flex gap-3">
                  <ColorOSButton
                    variant="secondary"
                    onClick={() => setShowUploadModal(false)}
                    className="flex-1"
                  >
                    取消
                  </ColorOSButton>
                  <ColorOSButton
                    icon={<UploadCloud className="w-5 h-5" />}
                    className="flex-1"
                  >
                    上传模板
                  </ColorOSButton>
                </div>
              </ColorOSCard>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 无障碍服务提示Modal */}
      <AnimatePresence>
        {showAccessibilityPrompt && (
          <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="w-full max-w-md"
            >
              <ColorOSCard className="p-6">
                <div className="text-center mb-6">
                  <div className="w-16 h-16 bg-red-500/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
                    <Shield className="w-8 h-8 text-red-400" />
                  </div>
                  <h3 className="text-xl font-bold mb-2">请先开启无障碍服务</h3>
                  <p className="text-white/60">需要开启无障碍服务才能执行自动填入操作</p>
                </div>
                
                <div className="bg-white/5 rounded-xl p-4 mb-6">
                  <p className="text-sm text-white/80">1. 打开系统设置</p>
                  <p className="text-sm text-white/80">2. 进入"无障碍"选项</p>
                  <p className="text-sm text-white/80">3. 找到并开启"小O帮帮"服务</p>
                </div>
                
                <div className="flex gap-3">
                  <ColorOSButton
                    variant="secondary"
                    onClick={() => setShowAccessibilityPrompt(false)}
                    className="flex-1"
                  >
                    稍后再说
                  </ColorOSButton>
                  <ColorOSButton
                    icon={<Settings className="w-5 h-5" />}
                    onClick={() => {
                      setShowAccessibilityPrompt(false)
                    }}
                    className="flex-1"
                  >
                    前往设置
                  </ColorOSButton>
                </div>
              </ColorOSCard>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  )
}
