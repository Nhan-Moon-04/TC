import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import axios from 'axios'
import client from '../api/client'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'

interface Company {
  id: number
  name: string
  code?: string
  country?: string
  notes?: string
  _count?: { shipments: number; tcgrs: number }
}

export default function Companies() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [companies, setCompanies] = useState<Company[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQ, setSearchQ] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editCompany, setEditCompany] = useState<Company | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [form, setForm] = useState({ name: '', code: '', country: '', notes: '' })
  const [saving, setSaving] = useState(false)

  const getApiError = (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { error?: string } | undefined)?.error
      if (message) return message
    }
    return t('common.error')
  }

  const fetchCompanies = async () => {
    try {
      const res = await client.get('/companies')
      setCompanies(res.data.data || res.data || [])
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchCompanies() }, [])

  const openAdd = () => {
    setEditCompany(null)
    setForm({ name: '', code: '', country: '', notes: '' })
    setShowModal(true)
  }

  const openEdit = (c: Company) => {
    setEditCompany(c)
    setForm({ name: c.name, code: c.code || '', country: c.country || '', notes: c.notes || '' })
    setShowModal(true)
  }

  const handleSave = async () => {
    if (!form.name) { toast.error(t('common.required')); return }
    if (!form.code) { toast.error('Company code is required'); return }
    setSaving(true)
    try {
      if (editCompany) {
        await client.put(`/companies/${editCompany.id}`, form)
      } else {
        await client.post('/companies', form)
      }
      toast.success(t('common.saveSuccess'))
      setShowModal(false)
      fetchCompanies()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await client.delete(`/companies/${id}`)
      toast.success(t('common.deleteSuccess'))
      fetchCompanies()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const filtered = companies.filter(c => {
    if (!searchQ) return true
    const q = searchQ.toLowerCase()
    return c.name.toLowerCase().includes(q) || c.code?.toLowerCase()?.includes(q) || c.country?.toLowerCase()?.includes(q)
  })

  if (loading) {
    return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" /></div>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t('company.title')}</h1>
        <button onClick={openAdd} className="btn btn-primary">+ {t('company.newCompany')}</button>
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
                <th className="table-header">{t('company.code')}</th>
                <th className="table-header">{t('company.country')}</th>
                <th className="table-header">{t('company.linkedShipments')}</th>
                <th className="table-header">{t('company.linkedTcGrs')}</th>
                <th className="table-header">{t('common.notes')}</th>
                <th className="table-header">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={7} className="text-center py-12 text-gray-400">{t('common.noData')}</td></tr>
              ) : filtered.map(c => (
                <tr key={c.id} className="table-row">
                  <td className="table-cell font-semibold text-gray-800">{c.name}</td>
                  <td className="table-cell">
                    {c.code ? <span className="font-mono text-sm bg-gray-100 px-2 py-0.5 rounded">{c.code}</span> : '—'}
                  </td>
                  <td className="table-cell text-gray-500">{c.country || '—'}</td>
                  <td className="table-cell">
                    {c._count?.shipments !== undefined ? (
                      <button
                        onClick={() => navigate(`/shipments?company=${c.id}`)}
                        className="text-blue-600 hover:underline text-sm"
                      >
                        {c._count.shipments}
                      </button>
                    ) : '—'}
                  </td>
                  <td className="table-cell">
                    {c._count?.tcgrs !== undefined ? (
                      <button
                        onClick={() => navigate(`/tcgrs?company=${c.id}`)}
                        className="text-blue-600 hover:underline text-sm"
                      >
                        {c._count.tcgrs}
                      </button>
                    ) : '—'}
                  </td>
                  <td className="table-cell">
                    {c.notes ? (
                      <div className="max-w-xs">
                        <div className="text-xs text-gray-500 truncate" title={c.notes}>{c.notes}</div>
                      </div>
                    ) : '—'}
                  </td>
                  <td className="table-cell">
                    <div className="flex gap-1.5">
                      <button onClick={() => openEdit(c)} className="btn btn-secondary btn-sm">{t('common.edit')}</button>
                      <button onClick={() => setDeleteId(c.id)} className="btn btn-danger btn-sm">{t('common.delete')}</button>
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

      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={editCompany ? t('company.editCompany') : t('company.newCompany')}
        size="md"
      >
        <div className="space-y-4">
          <div>
            <label className="label">{t('common.name')} *</label>
            <input className="input" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">{t('company.code')}</label>
              <input className="input" value={form.code} onChange={e => setForm(f => ({ ...f, code: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t('company.country')}</label>
              <input className="input" value={form.country} onChange={e => setForm(f => ({ ...f, country: e.target.value }))} placeholder="VD: Việt Nam, Trung Quốc..." />
            </div>
          </div>
          <div>
            <label className="label">{t('company.notes')}</label>
            <textarea className="input" rows={3} value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={() => setShowModal(false)} className="btn btn-secondary flex-1">{t('common.cancel')}</button>
            <button onClick={handleSave} disabled={saving} className="btn btn-primary flex-1">{saving ? t('common.loading') : t('common.save')}</button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        isOpen={deleteId !== null}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId && handleDelete(deleteId)}
        danger
      />
    </div>
  )
}
