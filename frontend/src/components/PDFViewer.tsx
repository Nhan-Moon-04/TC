import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import toast from 'react-hot-toast'
import client from '../api/client'

interface PDFViewerProps {
  fileId: string
  fileType: 'shipment' | 'tcgrs' | 'template'
  fileName: string
  onClose: () => void
}

export default function PDFViewer({ fileId, fileType, fileName, onClose }: PDFViewerProps) {
  const { t } = useTranslation()
  const [objectUrl, setObjectUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let url: string | null = null
    setLoading(true)
    setError(null)

    client.get(`/files/view/${fileType}/${fileId}`, { responseType: 'blob' })
      .then(res => {
        url = URL.createObjectURL(res.data)
        setObjectUrl(url)
      })
      .catch(() => {
        setError('Could not load file')
      })
      .finally(() => {
        setLoading(false)
      })

    return () => {
      if (url) URL.revokeObjectURL(url)
    }
  }, [fileId, fileType])

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

  const handleCopyName = useCallback(() => {
    navigator.clipboard.writeText(fileName).then(() => {
      toast.success(t('common.copied'))
    })
  }, [fileName, t])

  // Close on ESC
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/40 z-40 transition-opacity"
        onClick={onClose}
      />

      {/* Panel */}
      <div className="fixed right-0 top-0 h-full w-2/3 max-w-4xl bg-white shadow-2xl z-50 flex flex-col animate-slide-in">
        {/* Header */}
        <div className="flex items-center gap-3 px-5 py-3 border-b border-gray-200 bg-gray-50 flex-shrink-0">
          <div className="flex-1 min-w-0">
            <div className="text-sm font-semibold text-gray-800 truncate">{fileName}</div>
            <div className="text-xs text-gray-500 capitalize">{fileType}</div>
          </div>
          <button onClick={handleCopyName} className="btn btn-secondary btn-sm" title={t('common.copyName')}>
            <IconCopy /> {t('common.copyName')}
          </button>
          <button onClick={handleDownload} className="btn btn-primary btn-sm">
            <IconDownload /> {t('common.download')}
          </button>
          <button onClick={onClose} className="btn btn-ghost btn-sm text-gray-600 hover:text-gray-900">
            <IconClose />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 relative bg-gray-100 overflow-hidden">
          {loading && (
            <div className="absolute inset-0 flex items-center justify-center bg-white z-10">
              <div className="flex flex-col items-center gap-3">
                <div className="w-10 h-10 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
                <p className="text-gray-500 text-sm">{t('common.loading')}</p>
              </div>
            </div>
          )}
          {error && !loading && (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="text-center">
                <div className="text-red-400 text-5xl mb-3">⚠</div>
                <p className="text-gray-500">{error}</p>
              </div>
            </div>
          )}
          {objectUrl && !loading && (
            <iframe
              src={objectUrl}
              className="w-full h-full border-0"
              title={fileName}
            />
          )}
        </div>
      </div>
    </>
  )
}

function IconCopy() {
  return <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
}
function IconDownload() {
  return <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
}
function IconClose() {
  return <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
}
