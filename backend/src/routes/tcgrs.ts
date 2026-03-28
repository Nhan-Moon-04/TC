import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'
import { uploadTcGrs } from '../middleware/upload'
import fs from 'fs'

const router = Router()
router.use(authenticate)

router.get('/', async (req: Request, res: Response): Promise<void> => {
  const { companyId, type, completed } = req.query
  const list = await prisma.tcGrs.findMany({
    where: {
      ...(companyId ? { companyId: companyId as string } : {}),
      ...(type ? { type: type as any } : {}),
      ...(completed !== undefined ? { completed: completed === 'true' } : {})
    },
    include: {
      company: { select: { id: true, name: true, code: true } },
      files: true
    },
    orderBy: { createdAt: 'desc' }
  })
  // Add alert: not completed AND created > 30 days ago
  const now = new Date()
  const result = list.map(t => ({
    ...t,
    alert: !t.completed && (now.getTime() - new Date(t.createdAt).getTime()) > 30 * 24 * 60 * 60 * 1000
  }))
  res.json(result)
})

router.get('/:id', async (req: Request, res: Response): Promise<void> => {
  const item = await prisma.tcGrs.findUnique({
    where: { id: req.params.id },
    include: { company: true, files: true }
  })
  if (!item) { res.status(404).json({ error: 'Not found' }); return }
  const now = new Date()
  const alert = !item.completed && (now.getTime() - new Date(item.createdAt).getTime()) > 30 * 24 * 60 * 60 * 1000
  res.json({ ...item, alert })
})

router.post('/', async (req: Request, res: Response): Promise<void> => {
  const { companyId, type, certNumber, issueDate, expiryDate, notes } = req.body
  if (!companyId || !type) { res.status(400).json({ error: 'companyId and type required' }); return }
  const item = await prisma.tcGrs.create({
    data: {
      companyId, type, certNumber,
      issueDate: issueDate ? new Date(issueDate) : undefined,
      expiryDate: expiryDate ? new Date(expiryDate) : undefined,
      notes
    },
    include: { company: true }
  })
  res.status(201).json(item)
})

router.put('/:id', async (req: Request, res: Response): Promise<void> => {
  const { companyId, type, certNumber, issueDate, expiryDate, completed, notes } = req.body
  const item = await prisma.tcGrs.update({
    where: { id: req.params.id },
    data: {
      companyId, type, certNumber, completed, notes,
      issueDate: issueDate ? new Date(issueDate) : undefined,
      expiryDate: expiryDate ? new Date(expiryDate) : undefined,
    },
    include: { company: true }
  })
  res.json(item)
})

router.delete('/:id', async (req: Request, res: Response): Promise<void> => {
  const files = await prisma.tcGrsFile.findMany({ where: { tcGrsId: req.params.id } })
  files.forEach(f => { try { fs.unlinkSync(f.path) } catch {} })
  await prisma.tcGrs.delete({ where: { id: req.params.id } })
  res.json({ success: true })
})

// Upload files
router.post('/:id/files', uploadTcGrs.array('files', 20), async (req: Request, res: Response): Promise<void> => {
  const files = req.files as Express.Multer.File[]
  if (!files?.length) { res.status(400).json({ error: 'No files' }); return }
  const created = await prisma.$transaction(
    files.map(f => prisma.tcGrsFile.create({
      data: {
        tcGrsId: req.params.id,
        name: f.filename,
        originalName: Buffer.from(f.originalname, 'latin1').toString('utf8'),
        path: f.path,
        size: f.size,
        mimeType: f.mimetype
      }
    }))
  )
  res.status(201).json(created)
})

router.delete('/:id/files/:fileId', async (req: Request, res: Response): Promise<void> => {
  const file = await prisma.tcGrsFile.findUnique({ where: { id: req.params.fileId } })
  if (file) {
    try { fs.unlinkSync(file.path) } catch {}
    await prisma.tcGrsFile.delete({ where: { id: req.params.fileId } })
  }
  res.json({ success: true })
})

export default router
