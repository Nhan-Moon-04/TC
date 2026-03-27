import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { format } from 'date-fns'
import client from '../api/client'

interface Shipment {
  id: number
  code: string
  type: string
  status: string
  tcRequired: boolean
  tcCompleted: boolean
  tcAlert: boolean
  createdAt: string
  company?: { id: number; name: string }
}

interface TcGrsRecord {
  id: number
  type: string
  certNumber: string
  completed: boolean
  alert: boolean
  company?: { id: number; name: string }
}

interface Stats {
  shipments: number
  pendingTC: number
  companies: number
  templates: number
}

const statusColors: Record<string, string> = {
  PENDING: 'badge-yellow',
  WAITING_TC: 'badge-blue',
  COMPLETED: 'badge-green',
  CANCELLED: 'badge-gray'
}

export default function Dashboard() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [stats, setStats] = useState<Stats>({ shipments: 0, pendingTC: 0, companies: 0, templates: 0 })
  const [tcAlerts, setTcAlerts] = useState<TcGrsRecord[]>([])
  const [shipmentAlerts, setShipmentAlerts] = useState<Shipment[]>([])
  const [recentShipments, setRecentShipments] = useState<Shipment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const [shipmentsRes, companiesRes, templatesRes, tcgrsRes] = await Promise.all([
          client.get('/shipments?limit=100'),
          client.get('/companies'),
          client.get('/templates'),
          client.get('/tcgrs?limit=100')
        ])

        const allShipments: Shipment[] = shipmentsRes.data.data || shipmentsRes.data || []
        const allTcgrs: TcGrsRecord[] = tcgrsRes.data.data || tcgrsRes.data || []

        const pendingTC = allShipments.filter(s => s.tcRequired && !s.tcCompleted).length
        const companiesCount = (companiesRes.data.data || companiesRes.data || []).length
        const templatesCount = (templatesRes.data.data || templatesRes.data || []).length

        setStats({
          shipments: allShipments.length,
          pendingTC,
          companies: companiesCount,
          templates: templatesCount
        })

        setTcAlerts(allTcgrs.filter(t => t.alert))
        setShipmentAlerts(allShipments.filter(s => s.tcAlert))
        setRecentShipments(allShipments.slice(0, 10))
      } catch (e) {
        console.error(e)
      } finally {
        setLoading(false)
      }
    }
    fetchAll()
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t('dashboard.title')}</h1>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label={t('dashboard.totalShipments')}
          value={stats.shipments}
          icon="📦"
          color="blue"
          onClick={() => navigate('/shipments')}
        />
        <StatCard
          label={t('dashboard.pendingTC')}
          value={stats.pendingTC}
          icon="⏳"
          color={stats.pendingTC > 0 ? 'orange' : 'green'}
          onClick={() => navigate('/shipments')}
        />
        <StatCard
          label={t('dashboard.activeCompanies')}
          value={stats.companies}
          icon="🏢"
          color="purple"
          onClick={() => navigate('/companies')}
        />
        <StatCard
          label={t('dashboard.totalTemplates')}
          value={stats.templates}
          icon="📄"
          color="teal"
          onClick={() => navigate('/templates')}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* TC/GRS Alerts */}
        <div className="card">
          <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
            <h2 className="font-semibold text-gray-800 flex items-center gap-2">
              <span className="text-red-500">🔔</span>
              {t('dashboard.tcAlerts')}
              {(tcAlerts.length + shipmentAlerts.length) > 0 && (
                <span className="badge badge-red">{tcAlerts.length + shipmentAlerts.length}</span>
              )}
            </h2>
          </div>
          <div className="divide-y divide-gray-50">
            {tcAlerts.length === 0 && shipmentAlerts.length === 0 ? (
              <div className="px-5 py-8 text-center text-gray-400 text-sm">{t('dashboard.noAlerts')}</div>
            ) : (
              <>
                {shipmentAlerts.slice(0, 5).map(s => (
                  <div
                    key={`s-${s.id}`}
                    className="px-5 py-3 hover:bg-red-50 cursor-pointer transition-colors"
                    onClick={() => navigate(`/shipments/${s.id}`)}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <span className="font-medium text-sm text-gray-800">{s.code}</span>
                        <span className="text-gray-400 text-xs ml-2">{s.company?.name}</span>
                      </div>
                      <span className="badge badge-red">{t('alert.tcOverdue')}</span>
                    </div>
                    <div className="text-xs text-gray-500 mt-0.5">{t('dashboard.alertDescription')}</div>
                  </div>
                ))}
                {tcAlerts.slice(0, 5).map(tc => (
                  <div
                    key={`tc-${tc.id}`}
                    className="px-5 py-3 hover:bg-red-50 cursor-pointer transition-colors"
                    onClick={() => navigate('/tcgrs')}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <span className="font-medium text-sm text-gray-800">[{tc.type}] {tc.certNumber || '-'}</span>
                        <span className="text-gray-400 text-xs ml-2">{tc.company?.name}</span>
                      </div>
                      <span className="badge badge-red">⚠ {t('tcgrs.alertBadge')}</span>
                    </div>
                  </div>
                ))}
              </>
            )}
          </div>
        </div>

        {/* Recent Shipments */}
        <div className="card">
          <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
            <h2 className="font-semibold text-gray-800">{t('dashboard.recentShipments')}</h2>
            <button onClick={() => navigate('/shipments')} className="text-blue-600 text-sm hover:underline">
              {t('common.all')} →
            </button>
          </div>
          <div className="divide-y divide-gray-50">
            {recentShipments.length === 0 ? (
              <div className="px-5 py-8 text-center text-gray-400 text-sm">{t('common.noData')}</div>
            ) : (
              recentShipments.map(s => (
                <div
                  key={s.id}
                  className="px-5 py-3 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between"
                  onClick={() => navigate(`/shipments/${s.id}`)}
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-sm text-gray-800">{s.code}</span>
                      {s.tcAlert && <span className="badge badge-red text-xs">TC!</span>}
                    </div>
                    <div className="text-xs text-gray-400 mt-0.5">
                      {s.company?.name} · {format(new Date(s.createdAt), 'dd/MM/yyyy')}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0 ml-3">
                    <span className="text-xs text-gray-400">
                      {s.type === 'EXPORT' ? t('shipment.export') : t('shipment.import')}
                    </span>
                    <span className={`badge ${statusColors[s.status] || 'badge-gray'}`}>
                      {t(`shipment.statuses.${s.status}`)}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function StatCard({ label, value, icon, color, onClick }: {
  label: string; value: number; icon: string; color: string; onClick?: () => void
}) {
  const colorMap: Record<string, string> = {
    blue: 'bg-blue-50 border-blue-100',
    orange: 'bg-orange-50 border-orange-100',
    green: 'bg-green-50 border-green-100',
    purple: 'bg-purple-50 border-purple-100',
    teal: 'bg-teal-50 border-teal-100'
  }
  const textMap: Record<string, string> = {
    blue: 'text-blue-700',
    orange: 'text-orange-700',
    green: 'text-green-700',
    purple: 'text-purple-700',
    teal: 'text-teal-700'
  }

  return (
    <div
      onClick={onClick}
      className={`card p-5 cursor-pointer hover:shadow-md transition-shadow border ${colorMap[color] || 'bg-gray-50 border-gray-100'}`}
    >
      <div className="flex items-center justify-between mb-3">
        <span className="text-2xl">{icon}</span>
      </div>
      <div className={`text-3xl font-bold ${textMap[color] || 'text-gray-700'}`}>{value}</div>
      <div className="text-sm text-gray-600 mt-1">{label}</div>
    </div>
  )
}
