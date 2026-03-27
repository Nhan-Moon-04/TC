import multer from 'multer'
import path from 'path'
import fs from 'fs'
import { v4 as uuidv4 } from 'uuid'

function ensureDir(dir: string) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function createStorage(subfolder: string) {
  return multer.diskStorage({
    destination: (_req, _file, cb) => {
      const dir = path.join(process.env.UPLOAD_DIR || './uploads', subfolder)
      ensureDir(dir)
      cb(null, dir)
    },
    filename: (_req, file, cb) => {
      const ext = path.extname(file.originalname)
      cb(null, `${uuidv4()}${ext}`)
    },
  })
}

const fileFilter = (_req: any, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
  const allowed = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp',
    'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']
  if (allowed.includes(file.mimetype)) cb(null, true)
  else cb(new Error('File type not allowed'))
}

export const uploadShipment = multer({ storage: createStorage('shipments'), fileFilter, limits: { fileSize: 50 * 1024 * 1024 } })
export const uploadTcGrs    = multer({ storage: createStorage('tcgrs'),     fileFilter, limits: { fileSize: 50 * 1024 * 1024 } })
export const uploadTemplate = multer({ storage: createStorage('templates'), fileFilter, limits: { fileSize: 50 * 1024 * 1024 } })
