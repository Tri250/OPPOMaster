import { Router, type Request, type Response } from 'express'
import { randomUUID } from 'crypto'

const router = Router()

// 模拟日志数据
const logLevels = ['DEBUG', 'INFO', 'WARN', 'ERROR'] as const
const logTags = ['Camera', 'Preset', 'Network', 'UI', 'Sync', 'Database', 'System']
const logMessages = [
  '初始化相机参数',
  '加载预设数据完成',
  '网络请求成功',
  '用户点击了收藏按钮',
  '同步数据到云端',
  '数据库查询完成',
  '应用进入后台',
  '检测到内存不足',
  'API请求超时',
  '成功保存预设',
  '加载图片失败',
  '用户切换主题',
  'WebSocket连接建立',
  '缓存已清理'
]

// 生成模拟日志
const generateLogs = (count: number = 50) => {
  return Array.from({ length: count }, (_, i) => ({
    id: randomUUID(),
    timestamp: Date.now() - i * 1000 * Math.floor(Math.random() * 60),
    level: logLevels[Math.floor(Math.random() * logLevels.length)],
    tag: logTags[Math.floor(Math.random() * logTags.length)],
    message: logMessages[Math.floor(Math.random() * logMessages.length)],
    stackTrace: Math.random() > 0.9 ? 'Error at line 42\n    at functionName (file.js:42:15)' : undefined
  }))
}

let logs = generateLogs(100)

// 获取日志列表
router.get('/', (req: Request, res: Response) => {
  const { level, search, limit = '50' } = req.query
  let filteredLogs = [...logs]
  
  // 按级别筛选
  if (level && level !== 'ALL') {
    filteredLogs = filteredLogs.filter(log => log.level === level)
  }
  
  // 按关键字搜索
  if (search) {
    const searchStr = search.toString().toLowerCase()
    filteredLogs = filteredLogs.filter(log => 
      log.message.toLowerCase().includes(searchStr) ||
      log.tag.toLowerCase().includes(searchStr)
    )
  }
  
  // 限制数量
  const limitNum = parseInt(limit as string)
  filteredLogs = filteredLogs.slice(0, limitNum)
  
  res.json({
    success: true,
    data: filteredLogs
  })
})

// 清空日志
router.delete('/', (req: Request, res: Response) => {
  logs = []
  res.json({
    success: true,
    message: '日志已清空'
  })
})

// 添加新日志（模拟）
router.post('/', (req: Request, res: Response) => {
  const newLog = {
    id: randomUUID(),
    timestamp: Date.now(),
    level: req.body.level || 'INFO',
    tag: req.body.tag || 'System',
    message: req.body.message || 'New log entry',
    stackTrace: req.body.stackTrace
  }
  logs.unshift(newLog)
  res.json({
    success: true,
    data: newLog
  })
})

export default router
