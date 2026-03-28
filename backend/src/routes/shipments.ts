import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'
import { uploadShipment } from '../middleware/upload'
import path from 'path'
import fs from 'fs'

const router = Router()
router.use(authenticate)

router.get('/', async (req: Request, res: Response): Promise<void> => {
  const { status, type, companyId, q } = req.query
  const shipments = await prisma.shipment.findMany({
    where: {
      ...(status ? { status: status as any } : {}),
      ...(type ? { type: type as any } : {}),
      ...(companyId ? { companyId: companyId as string } : {}),
      ...(q ? {
        OR: [
          { code: { contains: q as string, mode: 'insensitive' } },
          { notes: { contains: q as string, mode: 'insensitive' } },
          { company: { name: { contains: q as string, mode: 'insensitive' } } },
        ]
      } : {})
    },
    include: {
      company: { select: { id: true, name: true, code: true } },
      _count: { select: { files: true, checklist: true } }
    },
    orderBy: { createdAt: 'desc' }
  })
  // Add alert flag: tcRequired && !tcCompleted && createdAt > 30 days ago
  const now = new Date()
  const result = shipments.map(s => ({
    ...s,
    tcAlert: s.tcRequired && !s.tcCompleted &&
      (now.getTime() - new Date(s.createdAt).getTime()) > 30 * 24 * 60 * 60 * 1000
  }))
  res.json(result)
})

router.get('/:id', async (req: Request, res: Response): Promise<void> => {
  const shipment = await prisma.shipment.findUnique({
    where: { id: req.params.id },
    include: {
      company: true,
      products: { include: { product: { include: { hsCodeNote: true } } } },
      files: { orderBy: { uploadedAt: 'desc' } },
      checklist: { orderBy: { order: 'asc' } }
    }
  })
  if (!shipment) { res.status(404).json({ error: 'Not found' }); return }
  const now = new Date()
  const tcAlert = shipment.tcRequired && !shipment.tcCompleted &&
    (now.getTime() - new Date(shipment.createdAt).getTime()) > 30 * 24 * 60 * 60 * 1000
  res.json({ ...shipment, tcAlert })
})

router.post('/', async (req: Request, res: Response): Promise<void> => {
  const { code, companyId, type, status, tcRequired, tcDueDate, notes, checklist } = req.body
  if (!code || !companyId || !type) { res.status(400).json({ error: 'code, companyId, type required' }); return }
  const shipment = await prisma.shipment.create({
    data: {
      code, companyId, type, status, tcRequired, tcDueDate: tcDueDate ? new Date(tcDueDate) : undefined, notes,
      checklist: checklist?.length ? {
        create: checklist.map((label: string, i: number) => ({ label, order: i }))
      } : undefined
    },
    include: { company: true, checklist: true }
  })
  res.status(201).json(shipment)
})

router.put('/:id', async (req: Request, res: Response): Promise<void> => {
  const { code, companyId, type, status, tcRequired, tcCompleted, tcDueDate, notes, productIds } = req.body
  
  const updateData: any = { code, companyId, type, status, tcRequired, tcCompleted, tcDueDate: tcDueDate ? new Date(tcDueDate) : undefined, notes }
  
  if (Array.isArray(productIds)) {
    await prisma.shipmentProduct.deleteMany({ where: { shipmentId: String(req.params.id) } })
    updateData.products = {
      create: productIds.map((pid: string) => ({ productId: pid }))
    }
  }

  const shipment = await prisma.shipment.update({
    where: { id: req.params.id },
    data: updateData,
  })
  res.json(shipment)
})

router.delete('/:id', async (req: Request, res: Response): Promise<void> => {
  // Delete files from disk
  const files = await prisma.shipmentFile.findMany({ where: { shipmentId: req.params.id } })
  files.forEach(f => { try { fs.unlinkSync(f.path) } catch {} })
  await prisma.shipment.delete({ where: { id: req.params.id } })
  res.json({ success: true })
})

// Upload files
router.post('/:id/files', uploadShipment.array('files', 20), async (req: Request, res: Response): Promise<void> => {
  const files = req.files as Express.Multer.File[]
  if (!files?.length) { res.status(400).json({ error: 'No files uploaded' }); return }
  const created = await prisma.$transaction(
    files.map(f => prisma.shipmentFile.create({
      data: {
        shipmentId: req.params.id,
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

// Delete file
router.delete('/:id/files/:fileId', async (req: Request, res: Response): Promise<void> => {
  const file = await prisma.shipmentFile.findUnique({ where: { id: req.params.fileId } })
  if (file) {
    try { fs.unlinkSync(file.path) } catch {}
    await prisma.shipmentFile.delete({ where: { id: req.params.fileId } })
  }
  res.json({ success: true })
})

// Checklist
router.put('/:id/checklist/:itemId', async (req: Request, res: Response): Promise<void> => {
  const item = await prisma.checklistItem.update({
    where: { id: req.params.itemId },
    data: { completed: req.body.completed }
  })
  res.json(item)
})

router.post('/:id/checklist', async (req: Request, res: Response): Promise<void> => {
  const { label } = req.body
  const count = await prisma.checklistItem.count({ where: { shipmentId: req.params.id } })
  const item = await prisma.checklistItem.create({
    data: { shipmentId: req.params.id, label, order: count }
  })
  res.status(201).json(item)
})

router.delete('/:id/checklist/:itemId', async (req: Request, res: Response): Promise<void> => {
  await prisma.checklistItem.delete({ where: { id: req.params.itemId } })
  res.json({ success: true })
})

export default router
