import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'
import { uploadTemplate } from '../middleware/upload'
import fs from 'fs'

const router = Router()
router.use(authenticate)

router.get('/', async (req: Request, res: Response): Promise<void> => {
  const { category, q } = req.query
  const templates = await prisma.template.findMany({
    where: {
      ...(category ? { category: category as string } : {}),
      ...(q ? {
        OR: [
          { name: { contains: q as string, mode: 'insensitive' } },
          { description: { contains: q as string, mode: 'insensitive' } },
          { tags: { has: q as string } }
        ]
      } : {})
    },
    include: { files: true },
    orderBy: { name: 'asc' }
  })
  res.json(templates)
})

router.get('/categories', async (_req: Request, res: Response): Promise<void> => {
  const cats = await prisma.template.findMany({
    select: { category: true },
    distinct: ['category'],
    orderBy: { category: 'asc' }
  })
  res.json(cats.map(c => c.category))
})

router.get('/:id', async (req: Request, res: Response): Promise<void> => {
  const template = await prisma.template.findUnique({
    where: { id: req.params.id },
    include: { files: true }
  })
  if (!template) { res.status(404).json({ error: 'Not found' }); return }
  res.json(template)
})

router.post('/', async (req: Request, res: Response): Promise<void> => {
  const { name, category, description, tags } = req.body
  if (!name || !category) { res.status(400).json({ error: 'name and category required' }); return }
  const template = await prisma.template.create({
    data: { name, category, description, tags: tags || [] }
  })
  res.status(201).json(template)
})

router.put('/:id', async (req: Request, res: Response): Promise<void> => {
  const { name, category, description, tags } = req.body
  const template = await prisma.template.update({
    where: { id: req.params.id },
    data: { name, category, description, tags },
    include: { files: true }
  })
  res.json(template)
})

router.delete('/:id', async (req: Request, res: Response): Promise<void> => {
  const files = await prisma.templateFile.findMany({ where: { templateId: req.params.id } })
  files.forEach(f => { try { fs.unlinkSync(f.path) } catch {} })
  await prisma.template.delete({ where: { id: req.params.id } })
  res.json({ success: true })
})

router.post('/:id/files', uploadTemplate.array('files', 20), async (req: Request, res: Response): Promise<void> => {
  const files = req.files as Express.Multer.File[]
  if (!files?.length) { res.status(400).json({ error: 'No files' }); return }
  const created = await prisma.$transaction(
    files.map(f => prisma.templateFile.create({
      data: {
        templateId: req.params.id,
        name: f.filename,
        originalName: f.originalname,
        path: f.path,
        size: f.size,
        mimeType: f.mimetype
      }
    }))
  )
  res.status(201).json(created)
})

router.delete('/:id/files/:fileId', async (req: Request, res: Response): Promise<void> => {
  const file = await prisma.templateFile.findUnique({ where: { id: req.params.fileId } })
  if (file) {
    try { fs.unlinkSync(file.path) } catch {}
    await prisma.templateFile.delete({ where: { id: req.params.fileId } })
  }
  res.json({ success: true })
})

export default router
