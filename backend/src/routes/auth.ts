import { Router, Request, Response } from 'express'
import bcrypt from 'bcryptjs'
import jwt from 'jsonwebtoken'
import { prisma } from '../lib/prisma'
import { authenticate, requireAdmin, AuthRequest } from '../middleware/auth'

const router = Router()

// Login
router.post('/login', async (req: Request, res: Response): Promise<void> => {
  const { username, password } = req.body
  if (!username || !password) {
    res.status(400).json({ error: 'Username and password required' })
    return
  }
  const user = await prisma.user.findUnique({ where: { username } })
  if (!user || !user.active) {
    res.status(401).json({ error: 'Invalid credentials' })
    return
  }
  const valid = await bcrypt.compare(password, user.password)
  if (!valid) {
    res.status(401).json({ error: 'Invalid credentials' })
    return
  }
  const token = jwt.sign(
    { id: user.id, username: user.username, role: user.role },
    process.env.JWT_SECRET!,
    { expiresIn: (process.env.JWT_EXPIRES_IN || '7d') as any }
  )
  res.json({ token, user: { id: user.id, username: user.username, name: user.name, role: user.role } })
})

// Get current user
router.get('/me', authenticate, async (req: AuthRequest, res: Response): Promise<void> => {
  const user = await prisma.user.findUnique({
    where: { id: req.user!.id },
    select: { id: true, username: true, name: true, role: true, active: true, createdAt: true }
  })
  res.json(user)
})

// List users (admin)
router.get('/users', authenticate, requireAdmin, async (_req: Request, res: Response): Promise<void> => {
  const users = await prisma.user.findMany({
    select: { id: true, username: true, name: true, role: true, active: true, createdAt: true },
    orderBy: { createdAt: 'desc' }
  })
  res.json(users)
})

// Create user (admin)
router.post('/users', authenticate, requireAdmin, async (req: Request, res: Response): Promise<void> => {
  const { username, password, name, role } = req.body
  if (!username || !password || !name) {
    res.status(400).json({ error: 'username, password, name required' })
    return
  }
  const exists = await prisma.user.findUnique({ where: { username } })
  if (exists) {
    res.status(409).json({ error: 'Username already exists' })
    return
  }
  const hash = await bcrypt.hash(password, 10)
  const user = await prisma.user.create({
    data: { username, password: hash, name, role: role || 'STAFF' },
    select: { id: true, username: true, name: true, role: true, active: true, createdAt: true }
  })
  res.status(201).json(user)
})

// Update user (admin)
router.put('/users/:id', authenticate, requireAdmin, async (req: Request, res: Response): Promise<void> => {
  const { name, role, active, password } = req.body
  const data: any = {}
  if (name !== undefined) data.name = name
  if (role !== undefined) data.role = role
  if (active !== undefined) data.active = active
  if (password) data.password = await bcrypt.hash(password, 10)
  const user = await prisma.user.update({
    where: { id: req.params.id },
    data,
    select: { id: true, username: true, name: true, role: true, active: true }
  })
  res.json(user)
})

// Delete user (admin)
router.delete('/users/:id', authenticate, requireAdmin, async (req: AuthRequest, res: Response): Promise<void> => {
  if (req.params.id === req.user!.id) {
    res.status(400).json({ error: 'Cannot delete yourself' })
    return
  }
  await prisma.user.delete({ where: { id: req.params.id } })
  res.json({ success: true })
})

export default router
