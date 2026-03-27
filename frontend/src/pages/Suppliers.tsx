import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import toast from 'react-hot-toast'
import axios from 'axios'
import client from '../api/client'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'

interface Supplier {
  id: string
  name: string
  country?: string
  contactName?: string
  email?: string
  phone?: string
  paymentTerms?: string
  notes?: string
}

export default function Suppliers() {
  const { t } = useTranslation()
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQ, setSearchQ] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editSupplier, setEditSupplier] = useState<Supplier | null>(null)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [expandedNoteId, setExpandedNoteId] = useState<string | null>(null)
  const [form, setForm] = useState({ name: '', country: '', contact: '', email: '', phone: '', paymentTerms: '', notes: '' })
  const [saving, setSaving] = useState(false)

  const getApiError = (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { error?: string } | undefined)?.error
      if (message) return message
    }
    return t('common.error')
  }

  const fetchSuppliers = async () => {
    try {
      const res = await client.get('/suppliers')
      setSuppliers(res.data.data || res.data || [])
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchSuppliers() }, [])

  const openAdd = () => {
    setEditSupplier(null)
    setForm({ name: '', country: '', contact: '', email: '', phone: '', paymentTerms: '', notes: '' })
    setShowModal(true)
  }

  const openEdit = (s: Supplier) => {
    setEditSupplier(s)
    setForm({ name: s.name, country: s.country || '', contact: s.contactName || '', email: s.email || '', phone: s.phone || '', paymentTerms: s.paymentTerms || '', notes: s.notes || '' })
    setShowModal(true)
  }

  const handleSave = async () => {
    if (!form.name) { toast.error(t('common.required')); return }
    setSaving(true)
    try {
      const payload = { ...form, contactName: form.contact }
      if (editSupplier) {
        await client.put(`/suppliers/${editSupplier.id}`, payload)
      } else {
        await client.post('/suppliers', payload)
      }
      toast.success(t('common.saveSuccess'))
      setShowModal(false)
      fetchSuppliers()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await client.delete(`/suppliers/${id}`)
      toast.success(t('common.deleteSuccess'))
      fetchSuppliers()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const filtered = suppliers.filter(s => {
    if (!searchQ) return true
    const q = searchQ.toLowerCase()
    return s.name.toLowerCase().includes(q) || s.country?.toLowerCase()?.includes(q) || s.contactName?.toLowerCase()?.includes(q) || s.email?.toLowerCase()?.includes(q)
  })

  if (loading) {
    return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" /></div>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t('supplier.title')}</h1>
        <button onClick={openAdd} className="btn btn-primary">+ {t('supplier.newSupplier')}</button>
      </div>

      <div className="card p-4">
        <input type="text" placeholder={t('common.search')} value={searchQ} onChange={e => setSearchQ(e.target.value)} className="input max-w-sm" />
      </div>

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="table-header">{t('common.name')}</th>
                <th className="table-header">{t('supplier.country')}</th>
                <th className="table-header">{t('supplier.contact')}</th>
                <th className="table-header">{t('supplier.email')}</th>
                <th className="table-header">{t('supplier.phone')}</th>
                <th className="table-header">{t('supplier.paymentTerms')}</th>
                <th className="table-header">{t('supplier.notes')}</th>
                <th className="table-header">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={8} className="text-center py-12 text-gray-400">{t('common.noData')}</td></tr>
              ) : filtered.map(s => (
                <>
                  <tr key={s.id} className="table-row">
                    <td className="table-cell font-semibold text-gray-800">{s.name}</td>
                    <td className="table-cell text-gray-500">{s.country || '—'}</td>
                    <td className="table-cell text-gray-600">{s.contactName || '—'}</td>
                    <td className="table-cell">
                      {s.email ? (
                        <a href={`mailto:${s.email}`} className="text-blue-600 hover:underline text-sm">{s.email}</a>
                      ) : '—'}
                    </td>
                    <td className="table-cell text-gray-500">{s.phone || '—'}</td>
                    <td className="table-cell">
                      {s.paymentTerms ? (
                        <span className="badge badge-blue text-xs">{s.paymentTerms}</span>
                      ) : '—'}
                    </td>
                    <td className="table-cell">
                      {s.notes ? (
                        <div className="max-w-xs">
                          <button
                            onClick={() => setExpandedNoteId(expandedNoteId === s.id ? null : s.id)}
                            className="text-left text-xs text-gray-500 hover:text-gray-700"
                          >
                            <span className="truncate block max-w-[160px]">{s.notes}</span>
                          </button>
                        </div>
                      ) : '—'}
                    </td>
                    <td className="table-cell">
                      <div className="flex gap-1.5">
                        <button onClick={() => openEdit(s)} className="btn btn-secondary btn-sm">{t('common.edit')}</button>
                        <button onClick={() => setDeleteId(s.id)} className="btn btn-danger btn-sm">{t('common.delete')}</button>
                      </div>
                    </td>
                  </tr>
                  {expandedNoteId === s.id && s.notes && (
                    <tr key={`note-${s.id}`}>
                      <td colSpan={8} className="px-6 py-3 bg-yellow-50 border-b border-yellow-100">
                        <div className="flex items-start gap-2">
                          <span className="text-yellow-500 mt-0.5">📝</span>
                          <p className="text-sm text-yellow-800 whitespace-pre-wrap">{s.notes}</p>
                        </div>
                      </td>
                    </tr>
                  )}
                </>
              ))}
            </tbody>
          </table>
        </div>
        {filtered.length > 0 && (
          <div className="px-4 py-3 border-t border-gray-100 text-sm text-gray-500">{t('common.total')}: {filtered.length}</div>
        )}
      </div>

      {/* Add/Edit Modal */}
      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title={editSupplier ? t('supplier.editSupplier') : t('supplier.newSupplier')} size="lg">
        <div className="space-y-4">
          <div>
            <label className="label">{t('common.name')} *</label>
            <input className="input" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">{t('supplier.country')}</label>
              <input className="input" value={form.country} onChange={e => setForm(f => ({ ...f, country: e.target.value }))} placeholder="Trung Quốc, Việt Nam..." />
            </div>
            <div>
              <label className="label">{t('supplier.contact')}</label>
              <input className="input" value={form.contact} onChange={e => setForm(f => ({ ...f, contact: e.target.value }))} placeholder="Tên người liên hệ" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">{t('supplier.email')}</label>
              <input type="email" className="input" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t('supplier.phone')}</label>
              <input type="tel" className="input" value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
            </div>
          </div>
          <div>
            <label className="label">{t('supplier.paymentTerms')}</label>
            <input className="input" value={form.paymentTerms} onChange={e => setForm(f => ({ ...f, paymentTerms: e.target.value }))} placeholder="VD: 30% trước, 70% sau · T/T · L/C" />
          </div>
          <div>
            <label className="label">{t('supplier.notes')}</label>
            <textarea
              className="input"
              rows={4}
              value={form.notes}
              onChange={e => setForm(f => ({ ...f, notes: e.target.value }))}
              placeholder="VD: Long Cheng Wu - tự làm sales contract, 30% trước khi giao hàng..."
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={() => setShowModal(false)} className="btn btn-secondary flex-1">{t('common.cancel')}</button>
            <button onClick={handleSave} disabled={saving} className="btn btn-primary flex-1">{saving ? t('common.loading') : t('common.save')}</button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog isOpen={deleteId !== null} onClose={() => setDeleteId(null)} onConfirm={() => deleteId && handleDelete(deleteId)} danger />
    </div>
  )
}
