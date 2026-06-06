import { Router, type Request, type Response } from 'express'
import { randomUUID } from 'crypto'
import fs from 'fs'
import path from 'path'

const router = Router()

// 日志存储目录
const LOGS_DIR = process.env.LOGS_DIR || path.join(process.cwd(), 'logs')

// 确保日志目录存在
if (!fs.existsSync(LOGS_DIR)) {
  fs.mkdirSync(LOGS_DIR, { recursive: true })
}

// 日志级别
const LOG_LEVELS = ['DEBUG', 'INFO', 'WARN', 'ERROR'] as const
type LogLevel = typeof LOG_LEVELS[number]

// 日志条目接口
interface LogEntry {
  id: string
  timestamp: number
  level: LogLevel
  tag: string
  message: string
  stackTrace?: string
  deviceId?: string
  userId?: string
}

// 从文件加载日志
const loadLogsFromFile = (): LogEntry[] => {
  try {
    const logFile = path.join(LOGS_DIR, 'app.log')
    if (!fs.existsSync(logFile)) {
      return []
    }
    const content = fs.readFileSync(logFile, 'utf-8')
    return content.split('\n')
      .filter(line => line.trim())
      .map(line => {
        try {
          return JSON.parse(line) as LogEntry
        } catch {
          return null
        }
      })
      .filter((log): log is LogEntry => log !== null)
  } catch (error) {
    console.error('Failed to load logs:', error)
    return []
  }
}

// 保存日志到文件
const saveLogToFile = (log: LogEntry) => {
  try {
    const logFile = path.join(LOGS_DIR, 'app.log')
    fs.appendFileSync(logFile, JSON.stringify(log) + '\n')
  } catch (error) {
    console.error('Failed to save log:', error)
  }
}

// 获取日志列表
router.get('/', (req: Request, res: Response) => {
  const { level, search, limit = '50', offset = '0' } = req.query
  
  // 从文件加载真实日志
  let logs = loadLogsFromFile()
  
  // 按级别筛选
  if (level && level !== 'ALL') {
    logs = logs.filter(log => log.level === level)
  }
  
  // 按关键字搜索
  if (search) {
    const searchStr = search.toString().toLowerCase()
    logs = logs.filter(log => 
      log.message.toLowerCase().includes(searchStr) ||
      log.tag.toLowerCase().includes(searchStr)
    )
  }
  
  // 按时间倒序排序
  logs.sort((a, b) => b.timestamp - a.timestamp)
  
  // 分页
  const offsetNum = parseInt(offset as string)
  const limitNum = parseInt(limit as string)
  const total = logs.length
  logs = logs.slice(offsetNum, offsetNum + limitNum)
  
  res.json({
    success: true,
    data: logs,
    pagination: {
      total,
      offset: offsetNum,
      limit: limitNum,
      hasMore: offsetNum + limitNum < total
    }
  })
})

// 清空日志
router.delete('/', (req: Request, res: Response) => {
  try {
    const logFile = path.join(LOGS_DIR, 'app.log')
    if (fs.existsSync(logFile)) {
      fs.writeFileSync(logFile, '')
    }
    res.json({
      success: true,
      message: '日志已清空'
    })
  } catch (error) {
    res.status(500).json({
      success: false,
      message: '清空日志失败',
      error: error instanceof Error ? error.message : 'Unknown error'
    })
  }
})

// 添加新日志（来自设备的真实日志）
router.post('/', (req: Request, res: Response) => {
  const { level = 'INFO', tag = 'System', message, stackTrace, deviceId, userId } = req.body
  
  if (!message) {
    res.status(400).json({
      success: false,
      message: '日志内容不能为空'
    })
    return
  }
  
  const newLog: LogEntry = {
    id: randomUUID(),
    timestamp: Date.now(),
    level: LOG_LEVELS.includes(level) ? level as LogLevel : 'INFO',
    tag,
    message,
    stackTrace,
    deviceId,
    userId
  }
  
  // 保存到文件
  saveLogToFile(newLog)
  
  res.json({
    success: true,
    data: newLog
  })
})

// 导出日志
router.get('/export', (req: Request, res: Response) => {
  try {
    const logs = loadLogsFromFile()
    const exportData = {
      exportTime: new Date().toISOString(),
      totalLogs: logs.length,
      logs
    }
    
    res.setHeader('Content-Type', 'application/json')
    res.setHeader('Content-Disposition', `attachment; filename="logs-${Date.now()}.json"`)
    res.json(exportData)
  } catch (error) {
    res.status(500).json({
      success: false,
      message: '导出日志失败',
      error: error instanceof Error ? error.message : 'Unknown error'
    })
  }
})

export default router
