import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import toast from 'react-hot-toast'
import client from '../api/client'

interface PDFViewerProps {
  fileId: string
  fileType: 'shipment' | 'tcgrs' | 'template'
  fileName: string
  mimeType?: string
  onClose: () => void
}

function getFileCategory(fileName: string, mimeType?: string): 'pdf' | 'image' | 'office' | 'other' {
  const ext = fileName.split('.').pop()?.toLowerCase() || ''
  const mime = mimeType || ''

  if (ext === 'pdf' || mime === 'application/pdf') return 'pdf'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'].includes(ext) || mime.startsWith('image/')) return 'image'
  if (['xls', 'xlsx', 'doc', 'docx', 'ppt', 'pptx', 'odt', 'ods', 'odp', 'csv'].includes(ext)) return 'office'
  return 'other'
}

const OFFICE_LABELS: Record<string, string> = {
  xls: 'Excel', xlsx: 'Excel', doc: 'Word', docx: 'Word',
  ppt: 'PowerPoint', pptx: 'PowerPoint', csv: 'CSV', odt: 'Writer', ods: 'Calc', odp: 'Impress'
}

export default function PDFViewer({ fileId, fileType, fileName, mimeType, onClose }: PDFViewerProps) {
  const { t } = useTranslation()
  const category = getFileCategory(fileName, mimeType)
  const ext = fileName.split('.').pop()?.toLowerCase() || ''

  const [objectUrl, setObjectUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(category === 'pdf' || category === 'image')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (category !== 'pdf' && category !== 'image') return

    let url: string | null = null
    setLoading(true)
    setError(null)

    client.get(`/files/view/${fileType}/${fileId}`, { responseType: 'blob' })
      .then(res => {
        url = URL.createObjectURL(res.data)
        setObjectUrl(url)
      })
      .catch(() => setError('Không thể tải file'))
      .finally(() => setLoading(false))

    return () => { if (url) URL.revokeObjectURL(url) }
  }, [fileId, fileType, category])

  const handleDownload = useCallback(async () => {
    try {
      const res = await client.get(`/files/download/${fileType}/${fileId}`, { responseType: 'blob' })
      const url = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch {
      toast.error(t('common.error'))
    }
  }, [fileId, fileType, fileName, t])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  return (
    <>
      <div className="fixed inset-0 bg-black/40 z-40" onClick={onClose} />
      <div className="fixed right-0 top-0 h-full w-2/3 max-w-4xl bg-white shadow-2xl z-50 flex flex-col">
        {/* Header */}
        <div className="flex items-center gap-3 px-5 py-3 border-b border-gray-200 bg-gray-50 flex-shrink-0">
          <div className="flex-1 min-w-0">
            <div className="text-sm font-semibold text-gray-800 truncate">{fileName}</div>
            <div className="text-xs text-gray-500">{fileType}</div>
          </div>
          <button onClick={handleDownload} className="btn btn-primary btn-sm">
            <IconDownload /> {t('common.download')}
          </button>
          <button onClick={onClose} className="btn btn-ghost btn-sm text-gray-600 hover:text-gray-900">
            <IconClose />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 relative bg-gray-100 overflow-hidden">

          {/* Loading */}
          {loading && (
            <div className="absolute inset-0 flex items-center justify-center bg-white z-10">
              <div className="flex flex-col items-center gap-3">
                <div className="w-10 h-10 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
                <p className="text-gray-500 text-sm">{t('common.loading')}</p>
              </div>
            </div>
          )}

          {/* Error */}
          {error && !loading && (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="text-center space-y-4">
                <div className="text-red-400 text-5xl">⚠</div>
                <p className="text-gray-500">Không thể tải file</p>
                <button onClick={handleDownload} className="btn btn-primary">
                  <IconDownload /> Tải về máy
                </button>
              </div>
            </div>
          )}

          {/* PDF */}
          {category === 'pdf' && objectUrl && !loading && !error && (
            <iframe src={objectUrl} className="w-full h-full border-0" title={fileName} />
          )}

          {/* Image */}
          {category === 'image' && objectUrl && !loading && !error && (
            <div className="absolute inset-0 flex items-center justify-center p-4 overflow-auto">
              <img src={objectUrl} alt={fileName} className="max-w-full max-h-full object-contain rounded shadow" />
            </div>
          )}

          {/* Office / không xem được */}
          {(category === 'office' || category === 'other') && (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="text-center space-y-5 p-8">
                <div className="text-7xl">
                  {category === 'office' ? (
                    ['xls', 'xlsx', 'csv', 'ods'].includes(ext) ? '📊'
                    : ['doc', 'docx', 'odt'].includes(ext) ? '📝'
                    : '📋'
                  ) : '📄'}
                </div>
                <div>
                  <p className="text-gray-700 font-semibold text-lg">
                    {OFFICE_LABELS[ext] || 'File'} không thể xem trước trên trình duyệt
                  </p>
                  <p className="text-gray-400 text-sm mt-1">
                    Tải về máy để mở bằng phần mềm tương ứng
                  </p>
                </div>
                <button onClick={handleDownload} className="btn btn-primary px-8 py-2.5 text-base">
                  <IconDownload /> Tải xuống
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  )
}

function IconDownload() {
  return <svg className="w-3.5 h-3.5 inline mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
}
function IconClose() {
  return <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
}
