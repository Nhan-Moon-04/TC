import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'

const router = Router()
router.use(authenticate)

router.get('/', async (req: Request, res: Response): Promise<void> => {
  const { q } = req.query
  const products = await prisma.product.findMany({
    where: q ? {
      OR: [
        { name: { contains: q as string, mode: 'insensitive' } },
        { hsCode: { contains: q as string, mode: 'insensitive' } },
        { itemCode: { contains: q as string, mode: 'insensitive' } },
      ]
    } : undefined,
    include: { hsCodeNote: true },
    orderBy: { name: 'asc' }
  })
  res.json(products)
})

router.get('/:id', async (req: Request, res: Response): Promise<void> => {
  const product = await prisma.product.findUnique({
    where: { id: req.params.id },
    include: { hsCodeNote: true }
  })
  if (!product) { res.status(404).json({ error: 'Not found' }); return }
  res.json(product)
})

router.post('/', async (req: Request, res: Response): Promise<void> => {
  const { name, nameEn, nameZh, hsCode, itemCode, notes } = req.body
  if (!name || !hsCode) { res.status(400).json({ error: 'name and hsCode required' }); return }
  const normalizedHsCode = String(hsCode).trim()
  await prisma.hsCodeNote.createMany({
    data: [{ hsCode: normalizedHsCode, notes: '' }],
    skipDuplicates: true
  })
  const product = await prisma.product.create({
    data: { name, nameEn, nameZh, hsCode: normalizedHsCode, itemCode, notes }
  })
  res.status(201).json(product)
})

router.put('/:id', async (req: Request, res: Response): Promise<void> => {
  const { name, nameEn, nameZh, hsCode, itemCode, notes } = req.body
  if (hsCode) {
    const normalizedHsCode = String(hsCode).trim()
    await prisma.hsCodeNote.createMany({
      data: [{ hsCode: normalizedHsCode, notes: '' }],
      skipDuplicates: true
    })
  }
  const normalizedHsCode = hsCode ? String(hsCode).trim() : undefined
  const product = await prisma.product.update({
    where: { id: req.params.id },
    data: { name, nameEn, nameZh, hsCode: normalizedHsCode, itemCode, notes }
  })
  res.json(product)
})

router.delete('/:id', async (req: Request, res: Response): Promise<void> => {
  await prisma.product.delete({ where: { id: req.params.id } })
  res.json({ success: true })
})

// HS Code notes
router.get('/hscode/:code/note', async (req: Request, res: Response): Promise<void> => {
  const note = await prisma.hsCodeNote.findUnique({ where: { hsCode: req.params.code } })
  res.json(note)
})

router.post('/hscode/:code/note', async (req: Request, res: Response): Promise<void> => {
  const notes = req.body?.notes ?? req.body?.note
  if (!notes) {
    res.status(400).json({ error: 'notes required' })
    return
  }
  const note = await prisma.hsCodeNote.upsert({
    where: { hsCode: req.params.code },
    update: { notes },
    create: { hsCode: req.params.code, notes }
  })
  res.json(note)
})

export default router
