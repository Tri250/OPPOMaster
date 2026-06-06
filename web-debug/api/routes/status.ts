import { Router, type Request, type Response } from 'express'

const router = Router()

// 模拟系统状态
let systemStatus = {
  status: 'running' as const,
  uptime: 3600,
  startTime: Date.now() - 3600000,
  memory: {
    used: 128,
    total: 512
  },
  network: {
    connected: true,
    latency: 24
  }
}

// 获取系统状态
router.get('/', (req: Request, res: Response) => {
  const uptime = Math.floor((Date.now() - systemStatus.startTime) / 1000)
  res.json({
    success: true,
    data: {
      ...systemStatus,
      uptime
    }
  })
})

// 获取性能指标
router.get('/metrics', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: {
      cpu: Math.floor(Math.random() * 30) + 10,
      memory: Math.floor(Math.random() * 40) + 20,
      network: Math.floor(Math.random() * 100),
      fps: 58 + Math.floor(Math.random() * 4)
    }
  })
})

// 获取历史性能数据
router.get('/metrics/history', (req: Request, res: Response) => {
  const points = 20
  const history = Array.from({ length: points }, (_, i) => ({
    time: new Date(Date.now() - (points - i) * 60000).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
    cpu: Math.floor(Math.random() * 30) + 10,
    memory: Math.floor(Math.random() * 40) + 20,
    network: Math.floor(Math.random() * 100)
  }))
  
  res.json({
    success: true,
    data: history
  })
})

export default router
