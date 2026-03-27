import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import toast from 'react-hot-toast'
import axios from 'axios'
import client from '../api/client'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'

interface Product {
  id: number
  name: string
  nameEn?: string
  nameZh?: string
  hsCode?: string
  itemCode?: string
  notes?: string
  hsCodeNote?: { notes?: string } | null
}

interface HsNote {
  code: string
  note: string
}

export default function Products() {
  const { t } = useTranslation()
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const didFetchRef = useRef(false)
  const [searchQ, setSearchQ] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editProduct, setEditProduct] = useState<Product | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [hsNoteModal, setHsNoteModal] = useState<{ code: string; note: string } | null>(null)
  const [form, setForm] = useState({ name: '', nameEn: '', nameZh: '', hsCode: '', itemCode: '', notes: '' })
  const [saving, setSaving] = useState(false)

  const getApiError = (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { error?: string } | undefined)?.error
      if (message) return message
    }
    return t('common.error')
  }

  const fetchProducts = async () => {
    try {
      const res = await client.get('/products')
      setProducts(res.data.data || res.data || [])
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (didFetchRef.current) return
    didFetchRef.current = true
    fetchProducts()
  }, [])

  const openAdd = () => {
    setEditProduct(null)
    setForm({ name: '', nameEn: '', nameZh: '', hsCode: '', itemCode: '', notes: '' })
    setShowModal(true)
  }

  const openEdit = (p: Product) => {
    setEditProduct(p)
    setForm({ name: p.name, nameEn: p.nameEn || '', nameZh: p.nameZh || '', hsCode: p.hsCode || '', itemCode: p.itemCode || '', notes: p.notes || '' })
    setShowModal(true)
  }

  const handleSave = async () => {
    if (!form.name) { toast.error(t('common.required')); return }
    if (!form.hsCode) { toast.error('HS Code is required'); return }
    setSaving(true)
    try {
      if (editProduct) {
        await client.put(`/products/${editProduct.id}`, form)
      } else {
        await client.post('/products', form)
      }
      toast.success(t('common.saveSuccess'))
      setShowModal(false)
      fetchProducts()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await client.delete(`/products/${id}`)
      toast.success(t('common.deleteSuccess'))
      fetchProducts()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const openHsNote = (p: Product) => {
    if (!p.hsCode) { toast.error('No HS code'); return }
    setHsNoteModal({ code: p.hsCode, note: p.hsCodeNote?.notes || '' })
  }

  const saveHsNote = async () => {
    if (!hsNoteModal) return
    try {
      await client.post(`/products/hscode/${hsNoteModal.code}/note`, { notes: hsNoteModal.note })
      toast.success(t('common.saveSuccess'))
      setHsNoteModal(null)
      fetchProducts()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const filtered = products.filter(p => {
    if (!searchQ) return true
    const q = searchQ.toLowerCase()
    return (
      p.name?.toLowerCase().includes(q) ||
      p.nameEn?.toLowerCase()?.includes(q) ||
      p.hsCode?.toLowerCase()?.includes(q) ||
      p.itemCode?.toLowerCase()?.includes(q)
    )
  })

  if (loading) {
    return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" /></div>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t('product.title')}</h1>
        <button onClick={openAdd} className="btn btn-primary">+ {t('product.newProduct')}</button>
      </div>

      <div className="card p-4">
        <input
          type="text"
          placeholder={t('common.search')}
          value={searchQ}
          onChange={e => setSearchQ(e.target.value)}
          className="input max-w-sm"
        />
      </div>

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="table-header">{t('common.name')}</th>
                <th className="table-header">{t('product.nameEn')}</th>
                <th className="table-header">{t('product.hsCode')}</th>
                <th className="table-header">{t('product.itemCode')}</th>
                <th className="table-header">{t('product.hsCodeNote')}</th>
                <th className="table-header">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={6} className="text-center py-12 text-gray-400">{t('common.noData')}</td></tr>
              ) : filtered.map(p => (
                <tr key={p.id} className="table-row">
                  <td className="table-cell font-medium text-gray-800">{p.name}</td>
                  <td className="table-cell text-gray-500">{p.nameEn || '—'}</td>
                  <td className="table-cell">
                    {p.hsCode ? (
                      <span className="font-mono text-sm bg-gray-100 px-2 py-0.5 rounded">{p.hsCode}</span>
                    ) : '—'}
                  </td>
                  <td className="table-cell text-gray-500">{p.itemCode || '—'}</td>
                  <td className="table-cell">
                    {p.hsCodeNote?.notes ? (
                      <div className="max-w-xs">
                        <div className="bg-yellow-50 border border-yellow-200 text-yellow-800 text-xs px-2 py-1 rounded truncate">
                          {p.hsCodeNote.notes}
                        </div>
                      </div>
                    ) : (
                      p.hsCode && (
                        <button onClick={() => openHsNote(p)} className="text-blue-500 hover:underline text-xs">
                          + {t('product.hsNoteHint')}
                        </button>
                      )
                    )}
                  </td>
                  <td className="table-cell">
                    <div className="flex gap-1.5">
                      {p.hsCode && (
                        <button onClick={() => openHsNote(p)} className="btn btn-ghost btn-sm text-yellow-600">
                          HS
                        </button>
                      )}
                      <button onClick={() => openEdit(p)} className="btn btn-secondary btn-sm">{t('common.edit')}</button>
                      <button onClick={() => setDeleteId(p.id)} className="btn btn-danger btn-sm">{t('common.delete')}</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {filtered.length > 0 && (
          <div className="px-4 py-3 border-t border-gray-100 text-sm text-gray-500">{t('common.total')}: {filtered.length}</div>
        )}
      </div>

      {/* Add/Edit Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={editProduct ? t('product.editProduct') : t('product.newProduct')}
        size="lg"
      >
        <div className="space-y-4">
          <div>
            <label className="label">{t('product.nameVi')} *</label>
            <input className="input" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">{t('product.nameEn')}</label>
              <input className="input" value={form.nameEn} onChange={e => setForm(f => ({ ...f, nameEn: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t('product.nameZh')}</label>
              <input className="input" value={form.nameZh} onChange={e => setForm(f => ({ ...f, nameZh: e.target.value }))} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">{t('product.hsCode')}</label>
              <input className="input font-mono" value={form.hsCode} onChange={e => setForm(f => ({ ...f, hsCode: e.target.value }))} placeholder="e.g. 6201.40.00" />
            </div>
            <div>
              <label className="label">{t('product.itemCode')}</label>
              <input className="input" value={form.itemCode} onChange={e => setForm(f => ({ ...f, itemCode: e.target.value }))} />
            </div>
          </div>
          <div>
            <label className="label">{t('common.notes')}</label>
            <textarea className="input" rows={2} value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={() => setShowModal(false)} className="btn btn-secondary flex-1">{t('common.cancel')}</button>
            <button onClick={handleSave} disabled={saving} className="btn btn-primary flex-1">{saving ? t('common.loading') : t('common.save')}</button>
          </div>
        </div>
      </Modal>

      {/* HS Code Note Modal */}
      {hsNoteModal && (
        <Modal isOpen={true} onClose={() => setHsNoteModal(null)} title={`${t('product.hsCodeNote')} — ${hsNoteModal.code}`} size="md">
          <div className="space-y-4">
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-700">
              HS Code: <strong className="font-mono">{hsNoteModal.code}</strong>
            </div>
            <div>
              <label className="label">{t('product.hsNoteHint')}</label>
              <textarea
                className="input"
                rows={5}
                value={hsNoteModal.note}
                onChange={e => setHsNoteModal(n => n ? { ...n, note: e.target.value } : null)}
                placeholder="Ghi chú thuế suất, quy định, điều kiện nhập khẩu..."
              />
            </div>
            <div className="flex gap-3 pt-2">
              <button onClick={() => setHsNoteModal(null)} className="btn btn-secondary flex-1">{t('common.cancel')}</button>
              <button onClick={saveHsNote} className="btn btn-primary flex-1">{t('common.save')}</button>
            </div>
          </div>
        </Modal>
      )}

      {/* Delete confirm */}
      <ConfirmDialog
        isOpen={deleteId !== null}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId && handleDelete(deleteId)}
        danger
      />
    </div>
  )
}
