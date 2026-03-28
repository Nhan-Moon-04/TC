import { Router, Request, Response } from 'express'
import { prisma } from '../lib/prisma'
import { authenticate } from '../middleware/auth'
import path from 'path'
import fs from 'fs'
import archiver from 'archiver'

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

// Download all files of a shipment as zip
router.get('/download-zip/shipment/:shipmentId', async (req: Request, res: Response): Promise<void> => {
  const files = await prisma.shipmentFile.findMany({ where: { shipmentId: req.params.shipmentId } })
  const existing = files.filter(f => fs.existsSync(f.path))
  if (existing.length === 0) { res.status(404).json({ error: 'No files found' }); return }

  const shipment = await prisma.shipment.findUnique({ where: { id: req.params.shipmentId }, select: { code: true } })
  const zipName = `${shipment?.code || req.params.shipmentId}.zip`

  res.setHeader('Content-Type', 'application/zip')
  res.setHeader('Content-Disposition', `attachment; filename="${encodeURIComponent(zipName)}"`)

  const archive = archiver('zip', { zlib: { level: 6 } })
  archive.pipe(res)

  // Deduplicate filenames in zip
  const usedNames = new Map<string, number>()
  for (const f of existing) {
    let name = f.originalName
    const count = usedNames.get(name) || 0
    if (count > 0) {
      const ext = path.extname(name)
      name = `${path.basename(name, ext)} (${count})${ext}`
    }
    usedNames.set(f.originalName, count + 1)
    archive.file(f.path, { name })
  }

  await archive.finalize()
})

export default router
