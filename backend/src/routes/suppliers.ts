import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'

const router = Router()
router.use(authenticate)

router.get('/', async (req: Request, res: Response): Promise<void> => {
  const { q } = req.query
  const suppliers = await prisma.supplier.findMany({
    where: q ? {
      OR: [
        { name: { contains: q as string, mode: 'insensitive' } },
        { country: { contains: q as string, mode: 'insensitive' } },
        { notes: { contains: q as string, mode: 'insensitive' } },
      ]
    } : undefined,
    orderBy: { name: 'asc' }
  })
  res.json(suppliers)
})

router.get('/:id', async (req: Request, res: Response): Promise<void> => {
  const supplier = await prisma.supplier.findUnique({ where: { id: req.params.id } })
  if (!supplier) { res.status(404).json({ error: 'Not found' }); return }
  res.json(supplier)
})

router.post('/', async (req: Request, res: Response): Promise<void> => {
  const { name, country, contactName, email, phone, notes, paymentTerms } = req.body
  if (!name) { res.status(400).json({ error: 'name required' }); return }
  const supplier = await prisma.supplier.create({ data: { name, country, contactName, email, phone, notes, paymentTerms } })
  res.status(201).json(supplier)
})

router.put('/:id', async (req: Request, res: Response): Promise<void> => {
  const { name, country, contactName, email, phone, notes, paymentTerms } = req.body
  const supplier = await prisma.supplier.update({
    where: { id: req.params.id },
    data: { name, country, contactName, email, phone, notes, paymentTerms }
  })
  res.json(supplier)
})

router.delete('/:id', async (req: Request, res: Response): Promise<void> => {
  await prisma.supplier.delete({ where: { id: req.params.id } })
  res.json({ success: true })
})

export default router
