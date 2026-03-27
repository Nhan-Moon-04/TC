import express from 'express'
import cors from 'cors'
import 'dotenv/config'
import 'express-async-errors'
import type { ErrorRequestHandler } from 'express'

import authRouter from './routes/auth'
import companiesRouter from './routes/companies'
import productsRouter from './routes/products'
import shipmentsRouter from './routes/shipments'
import tcgrsRouter from './routes/tcgrs'
import templatesRouter from './routes/templates'
import suppliersRouter from './routes/suppliers'
import searchRouter from './routes/search'
import filesRouter from './routes/files'

const app = express()

app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }))
app.use(express.json())
app.use(express.urlencoded({ extended: true }))

app.use('/api/auth', authRouter)
app.use('/api/companies', companiesRouter)
app.use('/api/products', productsRouter)
app.use('/api/shipments', shipmentsRouter)
app.use('/api/tcgrs', tcgrsRouter)
app.use('/api/templates', templatesRouter)
app.use('/api/suppliers', suppliersRouter)
app.use('/api/search', searchRouter)
app.use('/api/files', filesRouter)

app.get('/api/health', (_req, res) => res.json({ status: 'ok' }))

const errorHandler: ErrorRequestHandler = (err, _req, res, _next) => {
	console.error(err)
	if (process.env.NODE_ENV !== 'production') {
		const message = err instanceof Error ? err.message : 'Internal server error'
		res.status(500).json({ error: message })
		return
	}
	res.status(500).json({ error: 'Internal server error' })
}

app.use(errorHandler)

export default app
