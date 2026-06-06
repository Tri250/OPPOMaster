import { Router, type Request, type Response } from 'express'
import { execSync } from 'child_process'
import os from 'os'

const router = Router()

// 获取真实设备信息
const getDeviceInfo = () => {
  try {
    const totalMem = Math.floor(os.totalmem() / 1024 / 1024) // MB
    const freeMem = Math.floor(os.freemem() / 1024 / 1024) // MB
    const usedMem = totalMem - freeMem
    
    return {
      model: process.env.DEVICE_MODEL || 'Unknown Device',
      manufacturer: process.env.DEVICE_MANUFACTURER || 'Unknown',
      androidVersion: process.env.ANDROID_VERSION || 'Unknown',
      sdkVersion: parseInt(process.env.ANDROID_SDK_VERSION || '0'),
      screenWidth: parseInt(process.env.SCREEN_WIDTH || '0'),
      screenHeight: parseInt(process.env.SCREEN_HEIGHT || '0'),
      density: parseFloat(process.env.SCREEN_DENSITY || '0'),
      totalMemory: totalMem,
      availableMemory: freeMem,
      totalStorage: 0, // 需要从设备获取
      availableStorage: 0, // 需要从设备获取
      cpu: os.cpus()[0]?.model || 'Unknown',
      gpu: process.env.GPU_INFO || 'Unknown',
      cameraInfo: {
        main: process.env.CAMERA_MAIN || 'Unknown',
        ultraWide: process.env.CAMERA_ULTRAWIDE || 'Unknown',
        telephoto: process.env.CAMERA_TELEPHOTO || 'Unknown',
        periscope: process.env.CAMERA_PERISCOPE || 'Unknown'
      }
    }
  } catch (error) {
    return {
      model: 'Unknown',
      manufacturer: 'Unknown',
      androidVersion: 'Unknown',
      sdkVersion: 0,
      screenWidth: 0,
      screenHeight: 0,
      density: 0,
      totalMemory: 0,
      availableMemory: 0,
      totalStorage: 0,
      availableStorage: 0,
      cpu: 'Unknown',
      gpu: 'Unknown',
      cameraInfo: {
        main: 'Unknown',
        ultraWide: 'Unknown',
        telephoto: 'Unknown',
        periscope: 'Unknown'
      }
    }
  }
}

// 获取实时性能数据
const getPerformanceData = () => {
  try {
    const totalMem = os.totalmem()
    const freeMem = os.freemem()
    const usedMem = totalMem - freeMem
    const memoryUsage = Math.floor((usedMem / totalMem) * 100)
    
    // 获取CPU使用率
    const cpus = os.cpus()
    let totalIdle = 0
    let totalTick = 0
    cpus.forEach(cpu => {
      for (const type in cpu.times) {
        totalTick += cpu.times[type as keyof typeof cpu.times]
      }
      totalIdle += cpu.times.idle
    })
    const cpuUsage = Math.floor(100 - (100 * totalIdle / totalTick))
    
    return {
      cpuUsage: Math.min(cpuUsage, 100),
      memoryUsage: Math.min(memoryUsage, 100),
      gpuUsage: 0, // 需要从设备获取
      temperature: 0, // 需要从设备获取
      battery: parseInt(process.env.BATTERY_LEVEL || '0'),
      fps: 0 // 需要从设备获取
    }
  } catch (error) {
    return {
      cpuUsage: 0,
      memoryUsage: 0,
      gpuUsage: 0,
      temperature: 0,
      battery: 0,
      fps: 0
    }
  }
}

// 获取设备信息
router.get('/', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: getDeviceInfo()
  })
})

// 获取实时性能数据
router.get('/performance', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: getPerformanceData()
  })
})

// 获取应用信息
router.get('/app', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: {
      appName: '小O帮帮',
      version: process.env.APP_VERSION || '1.0.0',
      buildNumber: parseInt(process.env.BUILD_NUMBER || '0'),
      packageName: 'com.omaster.app',
      installDate: process.env.INSTALL_DATE || 'Unknown',
      lastUpdate: process.env.LAST_UPDATE || 'Unknown',
      databaseSize: 0, // 需要从设备获取
      cacheSize: 0 // 需要从设备获取
    }
  })
})

export default router
