import { Router, type Request, type Response } from 'express'

const router = Router()

// 模拟设备信息
const deviceInfo = {
  model: 'OPPO Find X7 Ultra',
  manufacturer: 'OPPO',
  androidVersion: '14',
  sdkVersion: 34,
  screenWidth: 1440,
  screenHeight: 3168,
  density: 3.5,
  totalMemory: 16384,
  availableMemory: 8240,
  totalStorage: 512000,
  availableStorage: 234000,
  cpu: 'Snapdragon 8 Gen 3',
  gpu: 'Adreno 750',
  cameraInfo: {
    main: '50MP f/1.8',
    ultraWide: '50MP f/2.0',
    telephoto: '50MP f/2.6',
    periscope: '50MP f/4.3'
  }
}

// 获取设备信息
router.get('/', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: deviceInfo
  })
})

// 获取实时性能数据
router.get('/performance', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: {
      cpuUsage: Math.floor(Math.random() * 40) + 10,
      memoryUsage: Math.floor((deviceInfo.totalMemory - deviceInfo.availableMemory) / deviceInfo.totalMemory * 100),
      gpuUsage: Math.floor(Math.random() * 60) + 20,
      temperature: 35 + Math.floor(Math.random() * 10),
      battery: 78,
      fps: 58 + Math.floor(Math.random() * 4)
    }
  })
})

// 获取应用信息
router.get('/app', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: {
      appName: '小O帮帮',
      version: '1.5.0',
      buildNumber: 150,
      packageName: 'com.omaster.app',
      installDate: '2024-12-01',
      lastUpdate: '2024-12-20',
      databaseSize: 45.2,
      cacheSize: 128.5
    }
  })
})

export default router
