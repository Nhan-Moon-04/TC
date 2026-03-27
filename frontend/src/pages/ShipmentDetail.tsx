import { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { format } from 'date-fns'
import toast from 'react-hot-toast'
import axios from 'axios'
import client from '../api/client'
import Modal from '../components/Modal'
import PDFViewer from '../components/PDFViewer'
import ConfirmDialog from '../components/ConfirmDialog'

interface ShipmentFile {
  id: string
  originalName: string
  size: number
  uploadedAt: string
  mimeType?: string
}

interface ChecklistItem {
  id: string
  label: string
  completed: boolean
}

interface HsCodeNote {
  notes: string
}

interface Product {
  id: string
  name: string
  nameEn?: string
  hsCode?: string
  itemCode?: string
  hsCodeNote?: HsCodeNote
}

interface ShipmentProduct {
  id: string
  productId: string
  product: Product
}

interface Shipment {
  id: string
  code: string
  type: 'EXPORT' | 'IMPORT'
  status: 'PENDING' | 'WAITING_TC' | 'COMPLETED' | 'CANCELLED'
  tcRequired: boolean
  tcCompleted: boolean
  tcAlert: boolean
  notes?: string
  createdAt: string
  updatedAt: string
  company?: { id: string; name: string, notes?: string }
  files?: ShipmentFile[]
  checklist?: ChecklistItem[]
  products?: ShipmentProduct[]
}

const statusBadge: Record<string, string> = {
  PENDING: 'badge-yellow',
  WAITING_TC: 'badge-blue',
  COMPLETED: 'badge-green',
  CANCELLED: 'badge-gray'
}

type TabKey = 'files' | 'checklist' | 'products' | 'notes'

export default function ShipmentDetail() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [shipment, setShipment] = useState<Shipment | null>(null)
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState<TabKey>('files')
  const [viewerFile, setViewerFile] = useState<ShipmentFile | null>(null)
  const [uploading, setUploading] = useState(false)
  const [newCheckItem, setNewCheckItem] = useState('')
  const [showEditModal, setShowEditModal] = useState(false)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showStatusModal, setShowStatusModal] = useState(false)
  const [allProducts, setAllProducts] = useState<Product[]>([])
  const [showAddProduct, setShowAddProduct] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [editForm, setEditForm] = useState<Partial<Shipment>>({})
  const [saving, setSaving] = useState(false)

  const getApiError = (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const message = (error.response?.data as { error?: string } | undefined)?.error
      if (message) return message
    }
    return t('common.error')
  }

  const fetchShipment = async () => {
    try {
      const res = await client.get(`/shipments/${id}`)
      const data = res.data.data || res.data
      setShipment(data)
      setEditForm({
        code: data.code,
        type: data.type,
        status: data.status,
        tcRequired: data.tcRequired,
        tcCompleted: data.tcCompleted,
        notes: data.notes
      })
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchShipment()
    client.get('/products').then(r => setAllProducts(r.data.data || r.data || []))
  }, [id])

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const fd = new FormData()
      fd.append('files', file)
      await client.post(`/shipments/${id}/files`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      toast.success(t('common.saveSuccess'))
      fetchShipment()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const handleDeleteFile = async (fileId: string) => {
    try {
      await client.delete(`/shipments/${id}/files/${fileId}`)
      toast.success(t('common.deleteSuccess'))
      fetchShipment()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleAddChecklist = async () => {
    if (!newCheckItem.trim()) return
    try {
      await client.post(`/shipments/${id}/checklist`, { label: newCheckItem })
      setNewCheckItem('')
      fetchShipment()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleToggleCheck = async (itemId: string, done: boolean) => {
    try {
      await client.put(`/shipments/${id}/checklist/${itemId}`, { completed: !done })
      fetchShipment()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleDeleteCheck = async (itemId: string) => {
    try {
      await client.delete(`/shipments/${id}/checklist/${itemId}`)
      fetchShipment()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleMarkTcComplete = async () => {
    try {
      await client.put(`/shipments/${id}`, { tcCompleted: true })
      toast.success(t('common.saveSuccess'))
      fetchShipment()
    } catch {
      toast.error(t('common.error'))
    }
  }

  const handleSaveEdit = async () => {
    setSaving(true)
    try {
      await client.put(`/shipments/${id}`, editForm)
      toast.success(t('common.saveSuccess'))
      setShowEditModal(false)
      fetchShipment()
    } catch (error) {
      toast.error(getApiError(error))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    try {
      await client.delete(`/shipments/${id}`)
      toast.success(t('common.deleteSuccess'))
      navigate('/shipments')
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleChangeStatus = async (status: string) => {
    try {
      await client.put(`/shipments/${id}`, { status })
      toast.success(t('common.saveSuccess'))
      setShowStatusModal(false)
      fetchShipment()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleAddProduct = async (productId: string) => {
    try {
      const current = shipment?.products?.map(p => p.productId) || []
      if (current.includes(productId)) return
      await client.put(`/shipments/${id}`, { productIds: [...current, productId] })
      setShowAddProduct(false)
      fetchShipment()
    } catch (error) {
      toast.error(getApiError(error))
    }
  }

  const handleRemoveProduct = async (productId: string) => {
    try {
      const current = shipment?.products?.map(p => p.productId).filter(pid => pid !== productId) || []
      await client.put(`/shipments/${id}`, { productIds: current })
      fetchShipment()
    } catch {
      toast.error(t('common.error'))
    }
  }

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  }

  if (loading) {
    return <div className="flex items-center justify-center h-64"><div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" /></div>
  }

  if (!shipment) {
    return <div className="text-center py-12 text-gray-400">{t('common.noData')}</div>
  }

  return (
    <div className="space-y-5">
      {/* TC Alert Banner */}
      {shipment.tcAlert && (
        <div className="bg-red-600 text-white px-5 py-3 rounded-xl flex items-center gap-3 shadow-lg">
          <span className="text-xl">🔔</span>
          <div>
            <div className="font-bold">{t('shipment.tcAlertBanner')}</div>
            <div className="text-red-100 text-sm">{t('dashboard.alertDescription')}</div>
          </div>
        </div>
      )}

      {/* Header card */}
      <div className="card p-5">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <button onClick={() => navigate('/shipments')} className="text-gray-400 hover:text-gray-600">
                ← {t('shipment.title')}
              </button>
            </div>
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-2xl font-bold text-gray-900">{shipment.code}</h1>
              <span className={`badge ${statusBadge[shipment.status] || 'badge-gray'}`}>
                {t(`shipment.statuses.${shipment.status}`)}
              </span>
              <span className={`badge ${shipment.type === 'EXPORT' ? 'badge-blue' : 'badge-yellow'}`}>
                {t(`shipment.types.${shipment.type}`)}
              </span>
              {shipment.tcAlert && <span className="badge badge-red">🔔 TC!</span>}
            </div>
            <div className="text-gray-500 mt-1 text-sm">
              {shipment.company?.name} · {format(new Date(shipment.createdAt), 'dd/MM/yyyy HH:mm')}
            </div>            {shipment.company?.notes && (
              <div className="mt-2 text-xs p-2 bg-yellow-50 text-yellow-800 border border-yellow-200 rounded inline-flex gap-2">
                <span>â°</span>
                <span>{shipment.company.notes}</span>
              </div>
            )}          </div>
          <div className="flex gap-2 flex-wrap">
            {shipment.tcRequired && !shipment.tcCompleted && (
              <button onClick={handleMarkTcComplete} className="btn btn-success">
                ✓ {t('shipment.markTcComplete')}
              </button>
            )}
            <button onClick={() => setShowStatusModal(true)} className="btn btn-secondary">
              {t('shipment.changeStatus')}
            </button>
            <button onClick={() => setShowEditModal(true)} className="btn btn-secondary">
              {t('common.edit')}
            </button>
            <button onClick={() => setShowDeleteConfirm(true)} className="btn btn-danger">
              {t('common.delete')}
            </button>
          </div>
        </div>

        {/* TC status row */}
        {shipment.tcRequired && (
          <div className="mt-4 pt-4 border-t border-gray-100 flex items-center gap-6 text-sm">
            <div className="flex items-center gap-2">
              <span className="text-gray-500">{t('shipment.tcRequired')}:</span>
              <span className="font-medium text-gray-800">{t('common.yes')}</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-gray-500">{t('shipment.tcCompleted')}:</span>
              <span className={`font-medium ${shipment.tcCompleted ? 'text-green-600' : 'text-orange-500'}`}>
                {shipment.tcCompleted ? t('common.yes') : t('common.no')}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="card">
        <div className="border-b border-gray-200 px-5">
          <nav className="flex gap-1 -mb-px">
            {(['files', 'checklist', 'products', 'notes'] as TabKey[]).map(tabKey => (
              <button
                key={tabKey}
                onClick={() => setTab(tabKey)}
                className={`px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                  tab === tabKey
                    ? 'border-blue-600 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {t(`shipment.${tabKey}`)}
                {tabKey === 'files' && shipment.files && shipment.files.length > 0 && (
                  <span className="ml-1.5 badge badge-gray">{shipment.files.length}</span>
                )}
                {tabKey === 'checklist' && shipment.checklist && shipment.checklist.length > 0 && (
                  <span className="ml-1.5 badge badge-gray">{shipment.checklist.length}</span>
                )}
              </button>
            ))}
          </nav>
        </div>

        <div className="p-5">
          {/* Files Tab */}
          {tab === 'files' && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="font-medium text-gray-700">{t('shipment.files')}</h3>
                <div>
                  <input type="file" ref={fileInputRef} onChange={handleFileUpload} className="hidden" />
                  <button
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                    className="btn btn-primary btn-sm"
                  >
                    {uploading ? t('common.loading') : `+ ${t('shipment.addFile')}`}
                  </button>
                </div>
              </div>
              {!shipment.files || shipment.files.length === 0 ? (
                <div className="text-center py-10 text-gray-400">
                  <div className="text-4xl mb-2">📎</div>
                  {t('common.noFiles')}
                </div>
              ) : (
                <div className="space-y-2">
                  {shipment.files.map(file => (
                    <div key={file.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                      <span className="text-2xl">📄</span>
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium text-gray-800 truncate">{file.originalName}</div>
                        <div className="text-xs text-gray-400">
                          {formatSize(file.size)} · {format(new Date(file.uploadedAt), 'dd/MM/yyyy')}
                        </div>
                      </div>
                      <div className="flex gap-1.5">
                        <button
                          onClick={() => setViewerFile(file)}
                          className="btn btn-secondary btn-sm"
                        >
                          {t('common.view')}
                        </button>
                        <button
                          onClick={async () => {
                            const res = await client.get(`/files/download/shipment/${file.id}`, { responseType: 'blob' })
                            const url = URL.createObjectURL(res.data)
                            const a = document.createElement('a')
                            a.href = url; a.download = file.originalName
                            a.click(); URL.revokeObjectURL(url)
                          }}
                          className="btn btn-ghost btn-sm"
                        >
                          {t('common.download')}
                        </button>
                        <button onClick={() => handleDeleteFile(file.id)} className="btn btn-danger btn-sm">
                          {t('common.delete')}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Checklist Tab */}
          {tab === 'checklist' && (
            <div className="space-y-3">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newCheckItem}
                  onChange={e => setNewCheckItem(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleAddChecklist()}
                  placeholder={t('shipment.checklistItem')}
                  className="input flex-1"
                />
                <button onClick={handleAddChecklist} className="btn btn-primary">{t('shipment.addItem')}</button>
              </div>
              {!shipment.checklist || shipment.checklist.length === 0 ? (
                <div className="text-center py-8 text-gray-400">{t('shipment.checklistEmpty')}</div>
              ) : (
                <div className="space-y-2">
                  {shipment.checklist.map(item => (
                    <div key={item.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                      <input
                        type="checkbox"
                        checked={item.completed}
                        onChange={() => handleToggleCheck(item.id, item.completed)}
                        className="w-4 h-4 text-blue-600 rounded cursor-pointer"
                      />
                      <span className={`flex-1 text-sm ${item.completed ? 'line-through text-gray-400' : 'text-gray-700'}`}>
                        {item.label}
                      </span>
                      <button onClick={() => handleDeleteCheck(item.id)} className="text-red-400 hover:text-red-600 text-xs px-2 py-1 hover:bg-red-50 rounded">
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Products Tab */}
          {tab === 'products' && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="font-medium text-gray-700">{t('shipment.products')}</h3>
                <button onClick={() => setShowAddProduct(true)} className="btn btn-primary btn-sm">
                  + {t('shipment.addProduct')}
                </button>
              </div>
              {!shipment.products || shipment.products.length === 0 ? (
                <div className="text-center py-8 text-gray-400">{t('shipment.noProducts')}</div>
              ) : (
                <div className="space-y-2">
                  {shipment.products.map(sp => (
                    <div key={sp.id} className="flex flex-col gap-2 p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <div className="text-sm font-medium text-gray-800">{sp.product.name}</div>
                          <div className="text-xs text-gray-400">
                            {sp.product.hsCode && <span>HS: {sp.product.hsCode}</span>}
                            {sp.product.itemCode && <span className="ml-2">SKU: {sp.product.itemCode}</span>}
                          </div>
                        </div>
                        <button onClick={() => handleRemoveProduct(sp.productId)} className="btn btn-danger btn-sm">
                          {t('shipment.removeProduct')}
                        </button>
                      </div>
                      {/* Warning for HS Code Note */}
                      {sp.product.hsCodeNote?.notes && (
                        <div className="text-xs p-2 bg-red-50 text-red-700 border border-red-100 rounded flex gap-2">
                          <span>âš ï¸</span>
                          <span>{sp.product.hsCodeNote.notes}</span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Notes Tab */}
          {tab === 'notes' && (
            <div>
              {shipment.notes ? (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-sm text-gray-700 whitespace-pre-wrap">
                  {shipment.notes}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-400">{t('common.noData')}</div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* PDF Viewer */}
      {viewerFile && (
        <PDFViewer
          fileId={String(viewerFile.id)}
          fileType="shipment"
          fileName={viewerFile.originalName}
          onClose={() => setViewerFile(null)}
        />
      )}

      {/* Edit Modal */}
      <Modal isOpen={showEditModal} onClose={() => setShowEditModal(false)} title={t('shipment.editShipment')} size="md">
        <div className="space-y-4">
          <div>
            <label className="label">{t('shipment.code')}</label>
            <input className="input" value={editForm.code || ''} onChange={e => setEditForm(f => ({ ...f, code: e.target.value }))} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">{t('shipment.type')}</label>
              <select className="input" value={editForm.type || 'EXPORT'} onChange={e => setEditForm(f => ({ ...f, type: e.target.value as any }))}>
                <option value="EXPORT">{t('shipment.export')}</option>
                <option value="IMPORT">{t('shipment.import')}</option>
              </select>
            </div>
            <div>
              <label className="label">{t('shipment.status')}</label>
              <select className="input" value={editForm.status || 'PENDING'} onChange={e => setEditForm(f => ({ ...f, status: e.target.value as any }))}>
                {['PENDING', 'WAITING_TC', 'COMPLETED', 'CANCELLED'].map(s => (
                  <option key={s} value={s}>{t(`shipment.statuses.${s}`)}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" id="editTcReq" checked={!!editForm.tcRequired} onChange={e => setEditForm(f => ({ ...f, tcRequired: e.target.checked }))} className="w-4 h-4" />
            <label htmlFor="editTcReq" className="text-sm text-gray-700">{t('shipment.tcRequired')}</label>
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" id="editTcComp" checked={!!editForm.tcCompleted} onChange={e => setEditForm(f => ({ ...f, tcCompleted: e.target.checked }))} className="w-4 h-4" />
            <label htmlFor="editTcComp" className="text-sm text-gray-700">{t('shipment.tcCompleted')}</label>
          </div>
          <div>
            <label className="label">{t('common.notes')}</label>
            <textarea className="input" rows={3} value={editForm.notes || ''} onChange={e => setEditForm(f => ({ ...f, notes: e.target.value }))} />
          </div>
          <div className="flex gap-3 pt-2">
            <button onClick={() => setShowEditModal(false)} className="btn btn-secondary flex-1">{t('common.cancel')}</button>
            <button onClick={handleSaveEdit} disabled={saving} className="btn btn-primary flex-1">{saving ? t('common.loading') : t('common.save')}</button>
          </div>
        </div>
      </Modal>

      {/* Status change modal */}
      <Modal isOpen={showStatusModal} onClose={() => setShowStatusModal(false)} title={t('shipment.changeStatus')} size="sm">
        <div className="space-y-2 py-2">
          {['PENDING', 'WAITING_TC', 'COMPLETED', 'CANCELLED'].map(s => (
            <button
              key={s}
              onClick={() => handleChangeStatus(s)}
              className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                shipment.status === s ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:bg-gray-50'
              }`}
            >
              <span className={`badge ${statusBadge[s] || 'badge-gray'}`}>{t(`shipment.statuses.${s}`)}</span>
            </button>
          ))}
        </div>
      </Modal>

      {/* Add product modal */}
      <Modal isOpen={showAddProduct} onClose={() => setShowAddProduct(false)} title={t('shipment.addProduct')} size="lg">
        <div className="space-y-2 max-h-80 overflow-y-auto">
          {allProducts.filter(p => !shipment.products?.find(sp => sp.productId === p.id)).map(p => (
            <div key={p.id} className="flex items-center justify-between p-3 hover:bg-gray-50 rounded-lg">
              <div>
                <div className="text-sm font-medium text-gray-800">{p.name}</div>
                <div className="text-xs text-gray-400">{p.hsCode} {p.itemCode}</div>
              </div>
              <button onClick={() => handleAddProduct(p.id)} className="btn btn-primary btn-sm">{t('shipment.addProduct')}</button>
            </div>
          ))}
        </div>
      </Modal>

      {/* Delete confirm */}
      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        danger
        message={t('common.confirmDelete')}
      />
    </div>
  )
}
