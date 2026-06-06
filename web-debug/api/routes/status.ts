import { Router, type Request, type Response } from 'express'
import os from 'os'

const router = Router()

// 系统启动时间
const START_TIME = Date.now()

// 获取真实系统状态
const getSystemStatus = () => {
  const uptime = Math.floor((Date.now() - START_TIME) / 1000)
  const totalMem = os.totalmem()
  const freeMem = os.freemem()
  const usedMem = totalMem - freeMem
  
  return {
    status: 'running' as const,
    uptime,
    startTime: START_TIME,
    memory: {
      used: Math.floor(usedMem / 1024 / 1024), // MB
      total: Math.floor(totalMem / 1024 / 1024) // MB
    },
    network: {
      connected: true,
      latency: 0 // 需要实际测量
    }
  }
}

// 获取真实性能指标
const getPerformanceMetrics = () => {
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
    cpu: Math.min(cpuUsage, 100),
    memory: memoryUsage,
    network: 0, // 需要实际测量
    fps: 0 // 需要实际测量
  }
}

// 获取历史性能数据（从文件或数据库）
const getPerformanceHistory = (points: number = 20) => {
  // 这里应该从数据库或文件读取历史数据
  // 目前返回空数组，表示没有历史数据
  return []
}

// 获取系统状态
router.get('/', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: getSystemStatus()
  })
})

// 获取性能指标
router.get('/metrics', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: getPerformanceMetrics()
  })
})

// 获取历史性能数据
router.get('/metrics/history', (req: Request, res: Response) => {
  const { points = '20' } = req.query
  const pointsNum = parseInt(points as string)
  
  const history = getPerformanceHistory(pointsNum)
  
  res.json({
    success: true,
    data: history,
    message: history.length === 0 ? '暂无历史数据' : undefined
  })
})

export default router
