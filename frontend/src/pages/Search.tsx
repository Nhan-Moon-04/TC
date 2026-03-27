import { useEffect, useState, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { format } from 'date-fns'
import client from '../api/client'

interface SearchResults {
  products?: Array<{ id: number; name: string; hsCode?: string; itemCode?: string }>
  shipments?: Array<{ id: number; code: string; type: string; status: string; company?: { name: string } }>
  companies?: Array<{ id: number; name: string; country?: string; code?: string }>
  tcgrs?: Array<{ id: number; type: string; certNumber?: string; company?: { name: string }; completed: boolean; alert: boolean }>
  templates?: Array<{ id: number; name: string; category?: string; tags?: string }>
  suppliers?: Array<{ id: number; name: string; country?: string; contact?: string }>
}

function debounce<T extends (...args: any[]) => any>(fn: T, delay: number): T {
  let timer: ReturnType<typeof setTimeout>
  return ((...args: any[]) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }) as T
}

const statusBadge: Record<string, string> = {
  PENDING: 'badge-yellow',
  WAITING_TC: 'badge-blue',
  COMPLETED: 'badge-green',
  CANCELLED: 'badge-gray'
}

export default function Search() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [query, setQuery] = useState(searchParams.get('q') || '')
  const [results, setResults] = useState<SearchResults | null>(null)
  const [loading, setLoading] = useState(false)

  const doSearch = useCallback(
    debounce(async (q: string) => {
      if (!q.trim()) { setResults(null); return }
      setLoading(true)
      try {
        const res = await client.get(`/search?q=${encodeURIComponent(q)}`)
        setResults(res.data)
      } catch {
        setResults(null)
      } finally {
        setLoading(false)
      }
    }, 300),
    []
  )

  useEffect(() => {
    const q = searchParams.get('q') || ''
    setQuery(q)
    if (q) doSearch(q)
  }, [searchParams])

  const handleChange = (val: string) => {
    setQuery(val)
    setSearchParams(val ? { q: val } : {})
    doSearch(val)
  }

  const totalResults = results
    ? Object.values(results).reduce((sum, arr) => sum + (arr?.length || 0), 0)
    : 0

  return (
    <div className="space-y-5 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900">{t('search.title')}</h1>

      {/* Search input */}
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </div>
        <input
          type="text"
          autoFocus
          value={query}
          onChange={e => handleChange(e.target.value)}
          placeholder={t('search.placeholder')}
          className="w-full pl-12 pr-4 py-3.5 text-base border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white shadow-sm"
        />
        {loading && (
          <div className="absolute inset-y-0 right-4 flex items-center">
            <div className="w-5 h-5 border-2 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
          </div>
        )}
      </div>

      {/* Results */}
      {query && !loading && results && totalResults === 0 && (
        <div className="text-center py-12 text-gray-400">
          <div className="text-4xl mb-2">🔍</div>
          <p>{t('search.noResults')}: <strong>"{query}"</strong></p>
        </div>
      )}

      {results && totalResults > 0 && (
        <div className="space-y-4">
          {/* Shipments */}
          {results.shipments && results.shipments.length > 0 && (
            <div className="card">
              <div className="px-5 py-3 border-b border-gray-100 flex items-center gap-2">
                <span className="text-blue-500">📦</span>
                <h2 className="font-semibold text-gray-800">{t('search.categories.shipments')}</h2>
                <span className="badge badge-blue">{results.shipments.length}</span>
              </div>
              <div className="divide-y divide-gray-50">
                {results.shipments.map(s => (
                  <div
                    key={s.id}
                    onClick={() => navigate(`/shipments/${s.id}`)}
                    className="px-5 py-3 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between"
                  >
                    <div>
                      <span className="font-semibold text-blue-600">{s.code}</span>
                      <span className="text-gray-400 text-sm ml-2">{s.company?.name}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-gray-400">{s.type}</span>
                      <span className={`badge ${statusBadge[s.status] || 'badge-gray'}`}>
                        {t(`shipment.statuses.${s.status}`)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Products */}
          {results.products && results.products.length > 0 && (
            <div className="card">
              <div className="px-5 py-3 border-b border-gray-100 flex items-center gap-2">
                <span className="text-green-500">🏷</span>
                <h2 className="font-semibold text-gray-800">{t('search.categories.products')}</h2>
                <span className="badge badge-green">{results.products.length}</span>
              </div>
              <div className="divide-y divide-gray-50">
                {results.products.map(p => (
                  <div
                    key={p.id}
                    onClick={() => navigate('/products')}
                    className="px-5 py-3 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between"
                  >
                    <span className="font-medium text-gray-800">{p.name}</span>
                    <div className="flex items-center gap-3">
                      {p.hsCode && <span className="font-mono text-sm text-gray-500 bg-gray-100 px-2 py-0.5 rounded">HS: {p.hsCode}</span>}
                      {p.itemCode && <span className="text-sm text-gray-400">{p.itemCode}</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Companies */}
          {results.companies && results.companies.length > 0 && (
            <div className="card">
              <div className="px-5 py-3 border-b border-gray-100 flex items-center gap-2">
                <span className="text-purple-500">🏢</span>
                <h2 className="font-semibold text-gray-800">{t('search.categories.companies')}</h2>
                <span className="badge" style={{ background: '#f3e8ff', color: '#7e22ce' }}>{results.companies.length}</span>
              </div>
              <div className="divide-y divide-gray-50">
                {results.companies.map(c => (
                  <div
                    key={c.id}
                    onClick={() => navigate('/companies')}
                    className="px-5 py-3 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between"
                  >
                    <span className="font-medium text-gray-800">{c.name}</span>
                    <div className="flex items-center gap-2">
                      {c.code && <span className="font-mono text-sm text-gray-400">{c.code}</span>}
                      {c.country && <span className="text-sm text-gray-400">{c.country}</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TC/GRS */}
          {results.tcgrs && results.tcgrs.length > 0 && (
            <div className="card">
              <div className="px-5 py-3 border-b border-gray-100 flex items-center gap-2">
                <span className="text-orange-500">📋</span>
                <h2 className="font-semibold text-gray-800">{t('search.categories.tcgrs')}</h2>
                <span className="badge badge-yellow">{results.tcgrs.length}</span>
              </div>
              <div className="divide-y divide-gray-50">
                {results.tcgrs.map(tc => (
                  <div
                    key={tc.id}
                    onClick={() => navigate('/tcgrs')}
                    className="px-5 py-3 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between"
                  >
                    <div>
                      <span className={`badge mr-2 ${tc.type === 'TC' ? 'badge-blue' : tc.type === 'GRS' ? 'badge-green' : 'badge-gray'}`}>{tc.type}</span>
                      <span className="font-medium text-gray-800">{tc.certNumber || '—'}</span>
                      <span className="text-gray-400 text-sm ml-2">{tc.company?.name}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      {tc.alert && <span className="badge badge-red">⚠</span>}
                      {tc.completed && <span className="badge badge-green">✓</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Templates */}
          {results.templates && results.templates.length > 0 && (
            <div className="card">
              <div className="px-5 py-3 border-b border-gray-100 flex items-center gap-2">
                <span className="text-teal-500">📁</span>
                <h2 className="font-semibold text-gray-800">{t('search.categories.templates')}</h2>
                <span className="badge" style={{ background: '#ccfbf1', color: '#0f766e' }}>{results.templates.length}</span>
              </div>
              <div className="divide-y divide-gray-50">
                {results.templates.map(tmpl => (
                  <div
                    key={tmpl.id}
                    onClick={() => navigate('/templates')}
                    className="px-5 py-3 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between"
                  >
                    <span className="font-medium text-gray-800">{tmpl.name}</span>
                    <div className="flex items-center gap-2">
                      {tmpl.category && <span className="badge badge-blue text-xs">{tmpl.category}</span>}
                      {tmpl.tags && <span className="text-xs text-gray-400">{tmpl.tags}</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Suppliers */}
          {results.suppliers && results.suppliers.length > 0 && (
            <div className="card">
              <div className="px-5 py-3 border-b border-gray-100 flex items-center gap-2">
                <span className="text-red-400">👤</span>
                <h2 className="font-semibold text-gray-800">{t('search.categories.suppliers')}</h2>
                <span className="badge badge-red">{results.suppliers.length}</span>
              </div>
              <div className="divide-y divide-gray-50">
                {results.suppliers.map(s => (
                  <div
                    key={s.id}
                    onClick={() => navigate('/suppliers')}
                    className="px-5 py-3 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between"
                  >
                    <span className="font-medium text-gray-800">{s.name}</span>
                    <div className="flex items-center gap-2">
                      {s.country && <span className="text-sm text-gray-400">{s.country}</span>}
                      {s.contact && <span className="text-sm text-gray-500">{s.contact}</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {!query && (
        <div className="text-center py-16 text-gray-400">
          <div className="text-5xl mb-3">🔍</div>
          <p className="text-lg">{t('search.placeholder')}</p>
          <p className="text-sm mt-1">{t('search.title')}: {t('search.categories.shipments')}, {t('search.categories.products')}, {t('search.categories.companies')}...</p>
        </div>
      )}
    </div>
  )
}
