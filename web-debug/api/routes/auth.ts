/**
 * 用户认证API路由 - 企业级实现
 * 处理用户注册、登录、登出、Token管理等功能
 */
import { Router, type Request, type Response } from 'express'
import { randomUUID } from 'crypto'
import bcrypt from 'bcrypt'
import jwt from 'jsonwebtoken'

const router = Router()

// JWT密钥（生产环境应从环境变量获取）
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production'
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d'

// 用户数据存储（生产环境应使用数据库）
interface User {
  id: string
  email: string
  password: string // 加密后的密码
  username: string
  avatar?: string
  createdAt: number
  updatedAt: number
  lastLoginAt?: number
  isActive: boolean
}

// 内存中的用户存储（生产环境应使用数据库）
const users: Map<string, User> = new Map()

// Token黑名单（用于登出）
const tokenBlacklist: Set<string> = new Set()

// 生成JWT Token
const generateToken = (userId: string): string => {
  return jwt.sign({ userId }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN })
}

// 验证JWT Token
const verifyToken = (token: string): { userId: string } | null => {
  try {
    if (tokenBlacklist.has(token)) {
      return null
    }
    return jwt.verify(token, JWT_SECRET) as { userId: string }
  } catch {
    return null
  }
}

/**
 * 用户注册
 * POST /api/auth/register
 */
router.post('/register', async (req: Request, res: Response): Promise<void> => {
  try {
    const { email, password, username } = req.body

    // 验证必填字段
    if (!email || !password || !username) {
      res.status(400).json({
        success: false,
        message: '请填写所有必填字段'
      })
      return
    }

    // 验证邮箱格式
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      res.status(400).json({
        success: false,
        message: '邮箱格式不正确'
      })
      return
    }

    // 验证密码强度
    if (password.length < 8) {
      res.status(400).json({
        success: false,
        message: '密码长度至少8位'
      })
      return
    }

    // 检查邮箱是否已注册
    const existingUser = Array.from(users.values()).find(u => u.email === email)
    if (existingUser) {
      res.status(409).json({
        success: false,
        message: '该邮箱已被注册'
      })
      return
    }

    // 加密密码
    const hashedPassword = await bcrypt.hash(password, 10)

    // 创建新用户
    const newUser: User = {
      id: randomUUID(),
      email,
      password: hashedPassword,
      username,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      isActive: true
    }

    users.set(newUser.id, newUser)

    // 生成Token
    const token = generateToken(newUser.id)

    res.status(201).json({
      success: true,
      message: '注册成功',
      data: {
        user: {
          id: newUser.id,
          email: newUser.email,
          username: newUser.username,
          avatar: newUser.avatar,
          createdAt: newUser.createdAt
        },
        token
      }
    })
  } catch (error) {
    res.status(500).json({
      success: false,
      message: '注册失败',
      error: error instanceof Error ? error.message : 'Unknown error'
    })
  }
})

/**
 * 用户登录
 * POST /api/auth/login
 */
router.post('/login', async (req: Request, res: Response): Promise<void> => {
  try {
    const { email, password } = req.body

    // 验证必填字段
    if (!email || !password) {
      res.status(400).json({
        success: false,
        message: '请填写邮箱和密码'
      })
      return
    }

    // 查找用户
    const user = Array.from(users.values()).find(u => u.email === email)
    if (!user) {
      res.status(401).json({
        success: false,
        message: '邮箱或密码错误'
      })
      return
    }

    // 检查用户是否被禁用
    if (!user.isActive) {
      res.status(403).json({
        success: false,
        message: '账号已被禁用'
      })
      return
    }

    // 验证密码
    const isPasswordValid = await bcrypt.compare(password, user.password)
    if (!isPasswordValid) {
      res.status(401).json({
        success: false,
        message: '邮箱或密码错误'
      })
      return
    }

    // 更新最后登录时间
    user.lastLoginAt = Date.now()
    users.set(user.id, user)

    // 生成Token
    const token = generateToken(user.id)

    res.json({
      success: true,
      message: '登录成功',
      data: {
        user: {
          id: user.id,
          email: user.email,
          username: user.username,
          avatar: user.avatar,
          lastLoginAt: user.lastLoginAt
        },
        token
      }
    })
  } catch (error) {
    res.status(500).json({
      success: false,
      message: '登录失败',
      error: error instanceof Error ? error.message : 'Unknown error'
    })
  }
})

/**
 * 用户登出
 * POST /api/auth/logout
 */
router.post('/logout', (req: Request, res: Response): void => {
  try {
    const authHeader = req.headers.authorization
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7)
      tokenBlacklist.add(token)
    }

    res.json({
      success: true,
      message: '登出成功'
    })
  } catch (error) {
    res.status(500).json({
      success: false,
      message: '登出失败',
      error: error instanceof Error ? error.message : 'Unknown error'
    })
  }
})

/**
 * 获取当前用户信息
 * GET /api/auth/me
 */
router.get('/me', (req: Request, res: Response): void => {
  try {
    const authHeader = req.headers.authorization
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({
        success: false,
        message: '未提供认证Token'
      })
      return
    }

    const token = authHeader.substring(7)
    const decoded = verifyToken(token)

    if (!decoded) {
      res.status(401).json({
        success: false,
        message: 'Token无效或已过期'
      })
      return
    }

    const user = users.get(decoded.userId)
    if (!user) {
      res.status(404).json({
        success: false,
        message: '用户不存在'
      })
      return
    }

    res.json({
      success: true,
      data: {
        id: user.id,
        email: user.email,
        username: user.username,
        avatar: user.avatar,
        createdAt: user.createdAt,
        lastLoginAt: user.lastLoginAt
      }
    })
  } catch (error) {
    res.status(500).json({
      success: false,
      message: '获取用户信息失败',
      error: error instanceof Error ? error.message : 'Unknown error'
    })
  }
})

/**
 * 刷新Token
 * POST /api/auth/refresh
 */
router.post('/refresh', (req: Request, res: Response): void => {
  try {
    const authHeader = req.headers.authorization
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({
        success: false,
        message: '未提供认证Token'
      })
      return
    }

    const token = authHeader.substring(7)
    const decoded = verifyToken(token)

    if (!decoded) {
      res.status(401).json({
        success: false,
        message: 'Token无效或已过期'
      })
      return
    }

    // 将旧Token加入黑名单
    tokenBlacklist.add(token)

    // 生成新Token
    const newToken = generateToken(decoded.userId)

    res.json({
      success: true,
      message: 'Token刷新成功',
      data: {
        token: newToken
      }
    })
  } catch (error) {
    res.status(500).json({
      success: false,
      message: '刷新Token失败',
      error: error instanceof Error ? error.message : 'Unknown error'
    })
  }
})

export default router
