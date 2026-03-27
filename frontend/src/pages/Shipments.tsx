import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { format } from 'date-fns'
import toast from 'react-hot-toast'
import axios from 'axios'
import client from '../api/client'
import Modal from '../components/Modal'

interface Shipment {
  id: number
  code: string
  type: 'EXPORT' | 'IMPORT'
  status: 'PENDING' | 'WAITING_TC' | 'COMPLETED' | 'CANCELLED'
  tcRequired: boolean
  tcCompleted: boolean
  tcAlert: boolean
  notes?: string
  createdAt: string
  company?: { id: number; name: string }
  _count?: { files: number }
}

interface Company {
  id: number
  name: string
}

const statusBadge: Record<string, string> = {
  PENDING: 'badge-yellow',
  WAITING_TC: 'badge-blue',
  COMPLETED: 'badge-green',
  CANCELLED: 'badge-gray'
}

export default function Shipments() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [shipments, setShipments] = useState<Shipment[]>([])
  const [companies, setCompanies] = useState<Company[]>([])
  const [loading, setLoading] = useState(true)
  const [filterStatus, setFilterStatus] = useState('')
  const [filterType, setFilterType] = useState('')
  const [filterTc, setFilterTc] = useState(false)
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [searchQ, setSearchQ] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState({
    code: '', companyId: '', type: 'EXPORT', status: 'PENDING',
    tcRequired: false, notes: ''
  })
  const [saving, setSaving] = useState(false)

  const getApiError = (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { error?: string } | undefined)?.error
      if (message) return message
    }
    return t('common.error')
  }

  const fetchData = async () => {
    try {
      const [sRes, cRes] = await Promise.all([
        client.get('/shipments'),
        client.get('/companies')
      ])
      setShipments(sRes.data.data || sRes.data || [])
      setCompanies(cRes.data.data || cRes.data || [])
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [])

  const filtered = shipments.filter(s => {
    if (filterStatus && s.status !== filterStatus) return false
    if (filterType && s.type !== filterType) return false
    if (filterTc && !s.tcRequired) return false
    if (dateFrom && new Date(s.createdAt) < new Date(dateFrom)) return false
    if (dateTo && new Date(s.createdAt) > new Date(dateTo + 'T23:59:59')) return false
    if (searchQ) {
      const q = searchQ.toLowerCase()
      return s.code.toLowerCase().includes(q) || s.company?.name?.toLowerCase()?.includes(q)
    }
    return true
  })

  const handleSave = async () => {
    if (!form.code || !form.companyId) {
      toast.error(t('common.required'))
      return
    }
    setSaving(true)
    try {
      await client.post('/shipments', {
        ...form,
        companyId: form.companyId,
        tcRequired: form.tcRequired
      })
      toast.success(t('common.saveSuccess'))
      setShowModal(false)
      setForm({ code: '', companyId: '', type: 'EXPORT', status: 'PENDING', tcRequired: false, notes: '' })
      fetchData()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" /></div>
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t('shipment.title')}</h1>
        <button onClick={() => setShowModal(true)} className="btn btn-primary">
          + {t('shipment.newShipment')}
        </button>
      </div>

      {/* Filter bar */}
      <div className="card p-4 flex flex-wrap gap-3 items-center">
        <input
          type="text"
          placeholder={t('common.search')}
          value={searchQ}
          onChange={e => setSearchQ(e.target.value)}
          className="input w-48"
        />
        <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)} className="input w-44">
          <option value="">{t('common.all')} {t('common.status')}</option>
          {['PENDING', 'WAITING_TC', 'COMPLETED', 'CANCELLED'].map(s => (
            <option key={s} value={s}>{t(`shipment.statuses.${s}`)}</option>
          ))}
        </select>
        <select value={filterType} onChange={e => setFilterType(e.target.value)} className="input w-40">
          <option value="">{t('common.all')} {t('common.type')}</option>
          <option value="EXPORT">{t('shipment.export')}</option>
          <option value="IMPORT">{t('shipment.import')}</option>
        </select>
        <label className="flex items-center gap-2 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={filterTc}
            onChange={e => setFilterTc(e.target.checked)}
            className="w-4 h-4 text-blue-600 rounded"
          />
          <span className="text-sm text-gray-700 whitespace-nowrap">Có TC</span>
        </label>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500 whitespace-nowrap">Từ ngày</span>
          <input
            type="date"
            value={dateFrom}
            onChange={e => setDateFrom(e.target.value)}
            className="input w-36"
          />
          <span className="text-sm text-gray-500">đến</span>
          <input
            type="date"
            value={dateTo}
            onChange={e => setDateTo(e.target.value)}
            className="input w-36"
          />
        </div>
        {(filterStatus || filterType || searchQ || filterTc || dateFrom || dateTo) && (
          <button onClick={() => { setFilterStatus(''); setFilterType(''); setSearchQ(''); setFilterTc(false); setDateFrom(''); setDateTo('') }} className="btn btn-secondary btn-sm">
            {t('common.clear')}
          </button>
        )}
      </div>

      {/* Table */}
      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="table-header">{t('shipment.code')}</th>
                <th className="table-header">{t('shipment.company')}</th>
                <th className="table-header">{t('shipment.type')}</th>
                <th className="table-header">{t('shipment.status')}</th>
                <th className="table-header">TC</th>
                <th className="table-header">{t('common.files')}</th>
                <th className="table-header">{t('common.createdAt')}</th>
                <th className="table-header">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={8} className="text-center py-12 text-gray-400">{t('common.noData')}</td></tr>
              ) : (
                filtered.map(s => (
                  <tr key={s.id} className="table-row cursor-pointer" onClick={() => navigate(`/shipments/${s.id}`)}>
                    <td className="table-cell">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-blue-600">{s.code}</span>
                        {s.tcAlert && (
                          <span className="badge badge-red text-xs">🔔 TC!</span>
                        )}
                      </div>
                    </td>
                    <td className="table-cell text-gray-600">{s.company?.name || '-'}</td>
                    <td className="table-cell">
                      <span className={`badge ${s.type === 'EXPORT' ? 'badge-blue' : 'badge-yellow'}`}>
                        {t(`shipment.types.${s.type}`)}
                      </span>
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${statusBadge[s.status] || 'badge-gray'}`}>
                        {t(`shipment.statuses.${s.status}`)}
                      </span>
                    </td>
                    <td className="table-cell">
                      {s.tcRequired ? (
                        <div className="flex items-center gap-1">
                          <span className={`w-2 h-2 rounded-full ${s.tcCompleted ? 'bg-green-500' : 'bg-orange-400'}`} />
                          <span className="text-xs text-gray-600">{s.tcCompleted ? t('shipment.tcCompleted') : t('shipment.tcRequired')}</span>
                        </div>
                      ) : (
                        <span className="text-gray-300 text-xs">—</span>
                      )}
                    </td>
                    <td className="table-cell text-gray-500 text-xs">{s._count?.files ?? 0}</td>
                    <td className="table-cell text-gray-500 text-xs">
                      {format(new Date(s.createdAt), 'dd/MM/yyyy')}
                    </td>
                    <td className="table-cell" onClick={e => e.stopPropagation()}>
                      <button
                        onClick={() => navigate(`/shipments/${s.id}`)}
                        className="btn btn-ghost btn-sm text-blue-600"
                      >
                        {t('common.view')}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {filtered.length > 0 && (
          <div className="px-4 py-3 border-t border-gray-100 text-sm text-gray-500">
            {t('common.total')}: {filtered.length}
          </div>
        )}
      </div>

      {/* New Shipment Modal */}
      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title={t('shipment.newShipment')} size="md">
        <div className="space-y-4">
          <div>
            <label className="label">{t('shipment.code')} *</label>
            <input className="input" value={form.code} onChange={e => setForm(f => ({ ...f, code: e.target.value }))} placeholder="VD: EXP-2024-001" />
          </div>
          <div>
            <label className="label">{t('shipment.company')} *</label>
            <select className="input" value={form.companyId} onChange={e => setForm(f => ({ ...f, companyId: e.target.value }))}>
              <option value="">-- {t('shipment.company')} --</option>
              {companies.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">{t('shipment.type')}</label>
              <select className="input" value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))}>
                <option value="EXPORT">{t('shipment.export')}</option>
                <option value="IMPORT">{t('shipment.import')}</option>
              </select>
            </div>
            <div>
              <label className="label">{t('shipment.status')}</label>
              <select className="input" value={form.status} onChange={e => setForm(f => ({ ...f, status: e.target.value }))}>
                {['PENDING', 'WAITING_TC', 'COMPLETED', 'CANCELLED'].map(s => (
                  <option key={s} value={s}>{t(`shipment.statuses.${s}`)}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" id="tcRequired" checked={form.tcRequired} onChange={e => setForm(f => ({ ...f, tcRequired: e.target.checked }))} className="w-4 h-4 text-blue-600" />
            <label htmlFor="tcRequired" className="text-sm text-gray-700">{t('shipment.tcRequired')}</label>
          </div>
          <div>
            <label className="label">{t('common.notes')}</label>
            <textarea className="input" rows={3} value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={() => setShowModal(false)} className="btn btn-secondary flex-1">{t('common.cancel')}</button>
            <button onClick={handleSave} disabled={saving} className="btn btn-primary flex-1">{saving ? t('common.loading') : t('common.save')}</button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
