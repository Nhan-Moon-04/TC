import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'

const router = Router()
router.use(authenticate)

router.get('/', async (_req: Request, res: Response): Promise<void> => {
  const companies = await prisma.company.findMany({ orderBy: { name: 'asc' } })
  res.json(companies)
})

router.get('/:id', async (req: Request, res: Response): Promise<void> => {
  const company = await prisma.company.findUnique({ where: { id: req.params.id } })
  if (!company) { res.status(404).json({ error: 'Not found' }); return }
  res.json(company)
})

router.post('/', async (req: Request, res: Response): Promise<void> => {
  const { name, code, country, notes } = req.body
  if (!name || !code) { res.status(400).json({ error: 'name and code required' }); return }
  try {
    const company = await prisma.company.create({ data: { name, code, country, notes } })
    res.status(201).json(company)
  } catch {
    res.status(409).json({ error: 'Company code already exists' })
  }
})

router.put('/:id', async (req: Request, res: Response): Promise<void> => {
  const { name, code, country, notes } = req.body
  const company = await prisma.company.update({
    where: { id: req.params.id },
    data: { name, code, country, notes }
  })
  res.json(company)
})

router.delete('/:id', async (req: Request, res: Response): Promise<void> => {
  await prisma.company.delete({ where: { id: req.params.id } })
  res.json({ success: true })
})

export default router
