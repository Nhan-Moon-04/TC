import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'

const router = Router()
router.use(authenticate)

router.get('/', async (req: Request, res: Response): Promise<void> => {
  const { q } = req.query
  if (!q || (q as string).trim().length < 2) {
    res.json({ products: [], shipments: [], companies: [], tcgrs: [], templates: [], suppliers: [] })
    return
  }
  const query = q as string

  const [products, shipments, companies, tcgrs, templates, suppliers] = await Promise.all([
    prisma.product.findMany({
      where: { OR: [
        { name: { contains: query, mode: 'insensitive' } },
        { hsCode: { contains: query, mode: 'insensitive' } },
        { itemCode: { contains: query, mode: 'insensitive' } },
        { notes: { contains: query, mode: 'insensitive' } },
      ]},
      take: 10
    }),
    prisma.shipment.findMany({
      where: { OR: [
        { code: { contains: query, mode: 'insensitive' } },
        { notes: { contains: query, mode: 'insensitive' } },
        { company: { name: { contains: query, mode: 'insensitive' } } },
      ]},
      include: { company: { select: { name: true } } },
      take: 10
    }),
    prisma.company.findMany({
      where: { OR: [
        { name: { contains: query, mode: 'insensitive' } },
        { code: { contains: query, mode: 'insensitive' } },
        { notes: { contains: query, mode: 'insensitive' } },
      ]},
      take: 10
    }),
    prisma.tcGrs.findMany({
      where: { OR: [
        { certNumber: { contains: query, mode: 'insensitive' } },
        { notes: { contains: query, mode: 'insensitive' } },
        { company: { name: { contains: query, mode: 'insensitive' } } },
      ]},
      include: { company: { select: { name: true } } },
      take: 10
    }),
    prisma.template.findMany({
      where: { OR: [
        { name: { contains: query, mode: 'insensitive' } },
        { description: { contains: query, mode: 'insensitive' } },
        { category: { contains: query, mode: 'insensitive' } },
      ]},
      take: 10
    }),
    prisma.supplier.findMany({
      where: { OR: [
        { name: { contains: query, mode: 'insensitive' } },
        { notes: { contains: query, mode: 'insensitive' } },
        { country: { contains: query, mode: 'insensitive' } },
      ]},
      take: 10
    }),
  ])

  res.json({ products, shipments, companies, tcgrs, templates, suppliers })
})

export default router
