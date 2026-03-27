import { useEffect, useState, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { format } from 'date-fns'
import toast from 'react-hot-toast'
import axios from 'axios'
import client from '../api/client'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import PDFViewer from '../components/PDFViewer'

interface TcGrsFile {
  id: string
  originalName: string
  size: number
  mimeType?: string
  uploadedAt: string
}

interface TcGrsRecord {
  id: string
  type: 'TC' | 'GRS' | 'OTHER'
  certNumber?: string
  issueDate?: string
  expiryDate?: string
  completed: boolean
  alert: boolean
  notes?: string
  company?: { id: string; name: string }
  files?: TcGrsFile[]
}

interface Company {
  id: string
  name: string
}

export default function TcGrs() {
  const { t } = useTranslation()
  const [records, setRecords] = useState<TcGrsRecord[]>([])
  const [companies, setCompanies] = useState<Company[]>([])
  const [loading, setLoading] = useState(true)
  const [filterCompany, setFilterCompany] = useState('')
  const [filterType, setFilterType] = useState('')
  const [filterCompleted, setFilterCompleted] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editRecord, setEditRecord] = useState<TcGrsRecord | null>(null)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [viewerFile, setViewerFile] = useState<{ id: string; name: string; mimeType?: string } | null>(null)
  const [uploading, setUploading] = useState(false)
  const [form, setForm] = useState({ type: 'TC', companyId: '', certNumber: '', issueDate: '', expiryDate: '', completed: false, notes: '' })
  const [saving, setSaving] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const getApiError = (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { error?: string } | undefined)?.error
      if (message) return message
    }
    return t('common.error')
  }

  const fetchData = async () => {
    try {
      const [rRes, cRes] = await Promise.all([
        client.get('/tcgrs'),
        client.get('/companies')
      ])
      setRecords(rRes.data.data || rRes.data || [])
      setCompanies(cRes.data.data || cRes.data || [])
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [])

  const openAdd = () => {
    setEditRecord(null)
    setForm({ type: 'TC', companyId: '', certNumber: '', issueDate: '', expiryDate: '', completed: false, notes: '' })
    setShowModal(true)
  }

  const openEdit = (r: TcGrsRecord) => {
    setEditRecord(r)
    setForm({
      type: r.type,
      companyId: String(r.company?.id || ''),
      certNumber: r.certNumber || '',
      issueDate: r.issueDate ? r.issueDate.split('T')[0] : '',
      expiryDate: r.expiryDate ? r.expiryDate.split('T')[0] : '',
      completed: r.completed,
      notes: r.notes || ''
    })
    setShowModal(true)
  }

  const handleSave = async () => {
    if (!form.companyId) { toast.error(t('common.required')); return }
    setSaving(true)
    try {
      const payload = { ...form, companyId: form.companyId }
      if (editRecord) {
        await client.put(`/tcgrs/${editRecord.id}`, payload)
      } else {
        await client.post('/tcgrs', payload)
      }
      toast.success(t('common.saveSuccess'))
      setShowModal(false)
      fetchData()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await client.delete(`/tcgrs/${id}`)
      toast.success(t('common.deleteSuccess'))
      fetchData()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleToggleComplete = async (r: TcGrsRecord) => {
    try {
      await client.put(`/tcgrs/${r.id}`, { completed: !r.completed })
      fetchData()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>, tcgrsId: string) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const fd = new FormData()
      fd.append('files', file)
      await client.post(`/tcgrs/${tcgrsId}/files`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      toast.success(t('common.saveSuccess'))
      fetchData()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const handleDeleteFile = async (tcgrsId: string, fileId: string) => {
    try {
      await client.delete(`/tcgrs/${tcgrsId}/files/${fileId}`)
      toast.success(t('common.deleteSuccess'))
      fetchData()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / 1048576).toFixed(1)} MB`
  }

  const filtered = records.filter(r => {
    if (filterCompany && String(r.company?.id) !== filterCompany) return false
    if (filterType && r.type !== filterType) return false
    if (filterCompleted === 'yes' && !r.completed) return false
    if (filterCompleted === 'no' && r.completed) return false
    return true
  })

  if (loading) {
    return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" /></div>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t('tcgrs.title')}</h1>
        <button onClick={openAdd} className="btn btn-primary">+ {t('tcgrs.newRecord')}</button>
      </div>

      {/* Filter bar */}
      <div className="card p-4 flex flex-wrap gap-3">
        <select value={filterCompany} onChange={e => setFilterCompany(e.target.value)} className="input w-48">
          <option value="">{t('common.all')} {t('tcgrs.company')}</option>
          {companies.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={filterType} onChange={e => setFilterType(e.target.value)} className="input w-36">
          <option value="">{t('common.all')} {t('common.type')}</option>
          {['TC', 'GRS', 'OTHER'].map(t2 => <option key={t2} value={t2}>{t2}</option>)}
        </select>
        <select value={filterCompleted} onChange={e => setFilterCompleted(e.target.value)} className="input w-40">
          <option value="">{t('common.all')}</option>
          <option value="yes">{t('tcgrs.completed')}</option>
          <option value="no">{t('common.no')}</option>
        </select>
        {(filterCompany || filterType || filterCompleted) && (
          <button onClick={() => { setFilterCompany(''); setFilterType(''); setFilterCompleted('') }} className="btn btn-secondary btn-sm">
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
                <th className="table-header">{t('tcgrs.company')}</th>
                <th className="table-header">{t('common.type')}</th>
                <th className="table-header">{t('tcgrs.certNumber')}</th>
                <th className="table-header">{t('tcgrs.issueDate')}</th>
                <th className="table-header">{t('tcgrs.expiryDate')}</th>
                <th className="table-header">{t('tcgrs.completed')}</th>
                <th className="table-header">{t('tcgrs.alert')}</th>
                <th className="table-header">{t('common.files')}</th>
                <th className="table-header">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={9} className="text-center py-12 text-gray-400">{t('common.noData')}</td></tr>
              ) : filtered.map(r => (
                <>
                  <tr key={r.id} className="table-row">
                    <td className="table-cell font-medium text-gray-800">{r.company?.name || '—'}</td>
                    <td className="table-cell">
                      <span className={`badge ${r.type === 'TC' ? 'badge-blue' : r.type === 'GRS' ? 'badge-green' : 'badge-gray'}`}>
                        {t(`tcgrs.types.${r.type}`)}
                      </span>
                    </td>
                    <td className="table-cell font-mono text-sm">{r.certNumber || '—'}</td>
                    <td className="table-cell text-sm text-gray-500">
                      {r.issueDate ? format(new Date(r.issueDate), 'dd/MM/yyyy') : '—'}
                    </td>
                    <td className="table-cell text-sm text-gray-500">
                      {r.expiryDate ? format(new Date(r.expiryDate), 'dd/MM/yyyy') : '—'}
                    </td>
                    <td className="table-cell">
                      <input
                        type="checkbox"
                        checked={r.completed}
                        onChange={() => handleToggleComplete(r)}
                        className="w-4 h-4 text-green-600 rounded cursor-pointer"
                      />
                    </td>
                    <td className="table-cell">
                      {r.alert && (
                        <span className="badge badge-red">⚠ {t('tcgrs.alertBadge')}</span>
                      )}
                    </td>
                    <td className="table-cell text-sm text-gray-500">{r.files?.length ?? 0}</td>
                    <td className="table-cell">
                      <div className="flex gap-1.5">
                        <button
                          onClick={() => setExpandedId(expandedId === r.id ? null : r.id)}
                          className="btn btn-ghost btn-sm"
                        >
                          {expandedId === r.id ? '▲' : '▼'}
                        </button>
                        <button onClick={() => openEdit(r)} className="btn btn-secondary btn-sm">{t('common.edit')}</button>
                        <button onClick={() => setDeleteId(r.id)} className="btn btn-danger btn-sm">{t('common.delete')}</button>
                      </div>
                    </td>
                  </tr>
                  {expandedId === r.id && (
                    <tr key={`expand-${r.id}`}>
                      <td colSpan={9} className="px-6 py-4 bg-gray-50 border-b border-gray-200">
                        <div className="space-y-3">
                          {/* Notes */}
                          {r.notes && (
                            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 text-sm text-yellow-800">
                              {r.notes}
                            </div>
                          )}
                          {/* Files */}
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-gray-700">{t('tcgrs.files')}</span>
                            <div>
                              <input
                                type="file"
                                ref={fileInputRef}
                                onChange={e => handleFileUpload(e, r.id)}
                                className="hidden"
                                id={`file-input-${r.id}`}
                              />
                              <label
                                htmlFor={`file-input-${r.id}`}
                                className="btn btn-primary btn-sm cursor-pointer"
                              >
                                {uploading ? t('common.loading') : `+ ${t('common.upload')}`}
                              </label>
                            </div>
                          </div>
                          {!r.files || r.files.length === 0 ? (
                            <p className="text-sm text-gray-400">{t('common.noFiles')}</p>
                          ) : (
                            <div className="space-y-2">
                              {r.files.map(f => (
                                <div key={f.id} className="flex items-center gap-3 p-3 bg-white rounded-lg border border-gray-200">
                                  <span className="text-xl">📄</span>
                                  <div className="flex-1 min-w-0">
                                    <div className="text-sm font-medium text-gray-800 truncate">{f.originalName}</div>
                                    <div className="text-xs text-gray-400">{formatSize(f.size)} · {format(new Date(f.uploadedAt), 'dd/MM/yyyy')}</div>
                                  </div>
                                  <div className="flex gap-1.5">
                                    <button onClick={() => setViewerFile({ id: f.id, name: f.originalName, mimeType: f.mimeType })} className="btn btn-secondary btn-sm">{t('common.view')}</button>
                                    <button
                                      onClick={async () => {
                                        const res = await client.get(`/files/download/tcgrs/${f.id}`, { responseType: 'blob' })
                                        const url = URL.createObjectURL(res.data)
                                        const a = document.createElement('a'); a.href = url; a.download = f.originalName; a.click(); URL.revokeObjectURL(url)
                                      }}
                                      className="btn btn-ghost btn-sm"
                                    >
                                      {t('common.download')}
                                    </button>
                                    <button onClick={() => handleDeleteFile(r.id, f.id)} className="btn btn-danger btn-sm">{t('common.delete')}</button>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  )}
                </>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* PDF Viewer */}
      {viewerFile && (
        <PDFViewer
          fileId={String(viewerFile.id)}
          fileType="tcgrs"
          fileName={viewerFile.name}
          mimeType={viewerFile.mimeType}
          onClose={() => setViewerFile(null)}
        />
      )}

      {/* Add/Edit Modal */}
      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title={editRecord ? t('tcgrs.editRecord') : t('tcgrs.newRecord')} size="md">
        <div className="space-y-4">
          <div>
            <label className="label">{t('tcgrs.company')} *</label>
            <select className="input" value={form.companyId} onChange={e => setForm(f => ({ ...f, companyId: e.target.value }))}>
              <option value="">-- {t('tcgrs.company')} --</option>
              {companies.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="label">{t('common.type')}</label>
            <select className="input" value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))}>
              <option value="TC">TC</option>
              <option value="GRS">GRS</option>
              <option value="OTHER">{t('tcgrs.types.OTHER')}</option>
            </select>
          </div>
          <div>
            <label className="label">{t('tcgrs.certNumber')}</label>
            <input className="input" value={form.certNumber} onChange={e => setForm(f => ({ ...f, certNumber: e.target.value }))} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">{t('tcgrs.issueDate')}</label>
              <input type="date" className="input" value={form.issueDate} onChange={e => setForm(f => ({ ...f, issueDate: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t('tcgrs.expiryDate')}</label>
              <input type="date" className="input" value={form.expiryDate} onChange={e => setForm(f => ({ ...f, expiryDate: e.target.value }))} />
            </div>
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" id="tcCompleted" checked={form.completed} onChange={e => setForm(f => ({ ...f, completed: e.target.checked }))} className="w-4 h-4" />
            <label htmlFor="tcCompleted" className="text-sm text-gray-700">{t('tcgrs.completed')}</label>
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

      <ConfirmDialog isOpen={deleteId !== null} onClose={() => setDeleteId(null)} onConfirm={() => deleteId && handleDelete(deleteId)} danger />
    </div>
  )
}
