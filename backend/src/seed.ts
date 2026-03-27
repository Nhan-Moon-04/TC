import 'dotenv/config'
import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'

const prisma = new PrismaClient()

async function main() {
  const hash = await bcrypt.hash('admin123', 10)
  const admin = await prisma.user.upsert({
    where: { username: 'admin' },
    update: {},
    create: { username: 'admin', password: hash, name: 'Administrator', role: 'ADMIN' }
  })
  console.log('Seeded admin user:', admin.username)
  console.log('Password: admin123')
  console.log('Please change password after first login!')
}

main().catch(console.error).finally(() => prisma.$disconnect())
