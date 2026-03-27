import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { format } from 'date-fns'
import toast from 'react-hot-toast'
import axios from 'axios'
import client from '../api/client'
import Modal from '../components/Modal'
import ConfirmDialog from '../components/ConfirmDialog'
import PDFViewer from '../components/PDFViewer'

interface TemplateFile {
  id: string
  originalName: string
  size: number
  uploadedAt: string
}

interface Template {
  id: number
  name: string
  category?: string
  description?: string
  tags?: string[]
  files?: TemplateFile[]
  _count?: { files: number }
}

const CATEGORIES = ['Quy cách', 'Thuế', 'Sales Contract', 'Hồ sơ mẫu', 'CO', 'Khác']

export default function Templates() {
  const { t } = useTranslation()
  const [templates, setTemplates] = useState<Template[]>([])
  const [loading, setLoading] = useState(true)
  const [filterCategory, setFilterCategory] = useState('')
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [showModal, setShowModal] = useState(false)
  const [editTemplate, setEditTemplate] = useState<Template | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [viewerFile, setViewerFile] = useState<{ id: string; name: string; mimeType?: string } | null>(null)
  const [uploading, setUploading] = useState(false)
  const [form, setForm] = useState({ name: '', category: '', description: '', tags: '' })
  const [saving, setSaving] = useState(false)
  const getApiError = (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { error?: string } | undefined)?.error
      if (message) return message
    }
    return t('common.error')
  }

  const fetchTemplates = async () => {
    try {
      const res = await client.get('/templates')
      setTemplates(res.data.data || res.data || [])
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchTemplates() }, [])

  const openAdd = () => {
    setEditTemplate(null)
    setForm({ name: '', category: '', description: '', tags: '' })
    setShowModal(true)
  }

  const openEdit = (tmpl: Template) => {
    setEditTemplate(tmpl)
    setForm({
      name: tmpl.name,
      category: tmpl.category || '',
      description: tmpl.description || '',
      tags: (tmpl.tags || []).join(', ')
    })
    setShowModal(true)
  }

  const handleSave = async () => {
    if (!form.name) { toast.error(t('common.required')); return }
    setSaving(true)
    try {
      const payload = {
        ...form,
        tags: form.tags
          .split(',')
          .map(tag => tag.trim())
          .filter(Boolean)
      }
      if (editTemplate) {
        await client.put(`/templates/${editTemplate.id}`, payload)
      } else {
        await client.post('/templates', payload)
      }
      toast.success(t('common.saveSuccess'))
      setShowModal(false)
      fetchTemplates()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await client.delete(`/templates/${id}`)
      toast.success(t('common.deleteSuccess'))
      fetchTemplates()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>, templateId: number) => {
    const files = Array.from(e.currentTarget.files || [])
    if (files.length === 0) return
    setUploading(true)
    try {
      const fd = new FormData()
      files.forEach(file => fd.append('files', file))
      await client.post(`/templates/${templateId}/files`, fd)
      toast.success(t('common.saveSuccess'))
      fetchTemplates()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setUploading(false)
      e.currentTarget.value = ''
    }
  }

  const handleDeleteFile = async (templateId: number, fileId: string) => {
    try {
      // Delete file - backend should support DELETE /templates/:id/files/:fileId
      await client.delete(`/templates/${templateId}/files/${fileId}`)
      fetchTemplates()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / 1048576).toFixed(1)} MB`
  }

  const allCategories = Array.from(new Set([...CATEGORIES, ...templates.map(t => t.category).filter(Boolean)])) as string[]

  const filtered = filterCategory
    ? templates.filter(t => t.category === filterCategory)
    : templates

  if (loading) {
    return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" /></div>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t('template.title')}</h1>
        <button onClick={openAdd} className="btn btn-primary">+ {t('template.newTemplate')}</button>
      </div>

      <div className="flex gap-6">
        {/* Sidebar categories */}
        <aside className="w-44 flex-shrink-0">
          <div className="card p-2 space-y-1">
            <button
              onClick={() => setFilterCategory('')}
              className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${!filterCategory ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}
            >
              {t('template.categories.all')} ({templates.length})
            </button>
            {allCategories.map(cat => {
              const count = templates.filter(t => t.category === cat).length
              return (
                <button
                  key={cat}
                  onClick={() => setFilterCategory(cat)}
                  className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${filterCategory === cat ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}
                >
                  {cat} ({count})
                </button>
              )
            })}
          </div>
        </aside>

        {/* Grid */}
        <div className="flex-1">
          {filtered.length === 0 ? (
            <div className="card p-12 text-center text-gray-400">
              <div className="text-4xl mb-2">📁</div>
              {t('common.noData')}
            </div>
          ) : (
            <div className="space-y-3">
              {filtered.map(tmpl => (
                <div key={tmpl.id} className="card">
                  {/* Template header */}
                  <div
                    className="p-4 cursor-pointer flex items-start justify-between"
                    onClick={() => setExpandedId(expandedId === tmpl.id ? null : tmpl.id)}
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h3 className="font-semibold text-gray-800">{tmpl.name}</h3>
                        {tmpl.category && (
                          <span className="badge badge-blue text-xs">{tmpl.category}</span>
                        )}
                        {tmpl.tags && tmpl.tags.map((tag, i) => (
                          <span key={i} className="badge badge-gray text-xs">{tag.trim()}</span>
                        ))}
                      </div>
                      {tmpl.description && (
                        <p className="text-sm text-gray-500 mt-1">{tmpl.description}</p>
                      )}
                      <div className="text-xs text-gray-400 mt-1">
                        📎 {tmpl._count?.files ?? tmpl.files?.length ?? 0} {t('common.files')}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 ml-4 flex-shrink-0">
                      <button onClick={e => { e.stopPropagation(); openEdit(tmpl) }} className="btn btn-secondary btn-sm">{t('common.edit')}</button>
                      <button onClick={e => { e.stopPropagation(); setDeleteId(tmpl.id) }} className="btn btn-danger btn-sm">{t('common.delete')}</button>
                      <span className="text-gray-400">{expandedId === tmpl.id ? '▲' : '▼'}</span>
                    </div>
                  </div>

                  {/* Expanded files */}
                  {expandedId === tmpl.id && (
                    <div className="border-t border-gray-100 px-4 pb-4 pt-3">
                      <div className="flex items-center justify-between mb-3">
                        <span className="text-sm font-medium text-gray-700">{t('common.files')}</span>
                        <div>
                          <input
                            type="file"
                            onChange={e => handleFileUpload(e, tmpl.id)}
                            className="hidden"
                            id={`tmpl-file-${tmpl.id}`}
                            multiple
                            accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.webp"
                          />
                          <label htmlFor={`tmpl-file-${tmpl.id}`} className="btn btn-primary btn-sm cursor-pointer">
                            {uploading ? t('common.loading') : `+ ${t('template.uploadFile')}`}
                          </label>
                        </div>
                      </div>
                      {!tmpl.files || tmpl.files.length === 0 ? (
                        <p className="text-sm text-gray-400 py-4 text-center">{t('common.noFiles')}</p>
                      ) : (
                        <div className="space-y-2">
                          {tmpl.files.map(f => (
                            <div key={f.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                              <span className="text-xl">📄</span>
                              <div className="flex-1 min-w-0">
                                <div className="text-sm font-medium text-gray-800 truncate">{f.originalName}</div>
                                <div className="text-xs text-gray-400">{formatSize(f.size)} · {format(new Date(f.uploadedAt), 'dd/MM/yyyy')}</div>
                              </div>
                              <div className="flex gap-1.5">
                                <button onClick={() => setViewerFile({ id: f.id, name: f.originalName, mimeType: (f as any).mimeType })} className="btn btn-secondary btn-sm">{t('common.view')}</button>
                                <button
                                  onClick={async () => {
                                    const res = await client.get(`/files/download/template/${f.id}`, { responseType: 'blob' })
                                    const url = URL.createObjectURL(res.data)
                                    const a = document.createElement('a'); a.href = url; a.download = f.originalName; a.click(); URL.revokeObjectURL(url)
                                  }}
                                  className="btn btn-ghost btn-sm"
                                >
                                  {t('common.download')}
                                </button>
                                <button onClick={() => handleDeleteFile(tmpl.id, f.id)} className="btn btn-danger btn-sm">{t('common.delete')}</button>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* PDF Viewer */}
      {viewerFile && (
        <PDFViewer
          fileId={String(viewerFile.id)}
          fileType="template"
          fileName={viewerFile.name}
          mimeType={viewerFile.mimeType}
          onClose={() => setViewerFile(null)}
        />
      )}

      {/* Add/Edit Modal */}
      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title={editTemplate ? t('template.editTemplate') : t('template.newTemplate')} size="md">
        <div className="space-y-4">
          <div>
            <label className="label">{t('common.name')} *</label>
            <input className="input" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          </div>
          <div>
            <label className="label">{t('template.category')}</label>
            <input
              className="input"
              list="categories-list"
              value={form.category}
              onChange={e => setForm(f => ({ ...f, category: e.target.value }))}
              placeholder="Quy cách, Thuế, CO..."
            />
            <datalist id="categories-list">
              {CATEGORIES.map(c => <option key={c} value={c} />)}
            </datalist>
          </div>
          <div>
            <label className="label">{t('template.description')}</label>
            <textarea className="input" rows={2} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          </div>
          <div>
            <label className="label">{t('template.tags')} <span className="text-gray-400 font-normal text-xs">(phân cách bằng dấu phẩy)</span></label>
            <input className="input" value={form.tags} onChange={e => setForm(f => ({ ...f, tags: e.target.value }))} placeholder="tag1, tag2, tag3" />
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
