import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'
import path from 'path'
import fs from 'fs'

const router = Router()
router.use(authenticate)

async function findFile(id: string, type: string): Promise<{ path: string; originalName: string; mimeType: string | null } | null> {
  if (type === 'shipment') return prisma.shipmentFile.findUnique({ where: { id } })
  if (type === 'tcgrs') return prisma.tcGrsFile.findUnique({ where: { id } })
  if (type === 'template') return prisma.templateFile.findUnique({ where: { id } })
  return null
}

// View file inline (for PDF viewer)
router.get('/view/:type/:id', async (req: Request, res: Response): Promise<void> => {
  const file = await findFile(req.params.id, req.params.type)
  if (!file || !fs.existsSync(file.path)) { res.status(404).json({ error: 'File not found' }); return }
  res.setHeader('Content-Type', file.mimeType || 'application/octet-stream')
  res.setHeader('Content-Disposition', 'inline')
  fs.createReadStream(file.path).pipe(res)
})

// Download file
router.get('/download/:type/:id', async (req: Request, res: Response): Promise<void> => {
  const file = await findFile(req.params.id, req.params.type)
  if (!file || !fs.existsSync(file.path)) { res.status(404).json({ error: 'File not found' }); return }
  res.setHeader('Content-Type', file.mimeType || 'application/octet-stream')
  res.setHeader('Content-Disposition', `attachment; filename="${encodeURIComponent(file.originalName)}"`)
  fs.createReadStream(file.path).pipe(res)
})

export default router
