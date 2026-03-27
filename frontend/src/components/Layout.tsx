import React, { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import i18n from '../i18n'

const NavItem = ({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) => (
  <NavLink
    to={to}
    className={({ isActive }) =>
      `flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 mx-2 ${
        isActive
          ? 'bg-blue-600 text-white shadow-sm'
          : 'text-slate-300 hover:bg-slate-700 hover:text-white'
      }`
    }
  >
    <span className="w-5 h-5 flex-shrink-0">{icon}</span>
    <span>{label}</span>
  </NavLink>
)

const languages = [
  { code: 'vi', label: 'VI', flag: '🇻🇳' },
  { code: 'en', label: 'EN', flag: '🇬🇧' },
  { code: 'zh', label: 'ZH', flag: '🇨🇳' }
]

export default function Layout({ children }: { children: React.ReactNode }) {
  const { t } = useTranslation()
  const { user, logout, isAdmin } = useAuth()
  const navigate = useNavigate()
  const [searchQ, setSearchQ] = useState('')
  const [currentLang, setCurrentLang] = useState(localStorage.getItem('tcgrs_lang') || 'vi')

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchQ.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQ.trim())}`)
      setSearchQ('')
    }
  }

  const switchLang = (code: string) => {
    i18n.changeLanguage(code)
    localStorage.setItem('tcgrs_lang', code)
    setCurrentLang(code)
  }

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      {/* Sidebar */}
      <aside className="w-56 flex-shrink-0 bg-sidebar flex flex-col shadow-xl">
        {/* Logo */}
        <div className="px-6 py-5 border-b border-slate-700">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
              <span className="text-white font-bold text-sm">TC</span>
            </div>
            <div>
              <div className="text-white font-bold text-base leading-tight">TC/GRS</div>
              <div className="text-slate-400 text-xs">Manager</div>
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 py-3 space-y-0.5 overflow-y-auto">
          <NavItem to="/" icon={<IconDashboard />} label={t('nav.dashboard')} />
          <NavItem to="/shipments" icon={<IconShipment />} label={t('nav.shipments')} />
          <NavItem to="/products" icon={<IconProduct />} label={t('nav.products')} />
          <NavItem to="/tcgrs" icon={<IconTcGrs />} label={t('nav.tcgrs')} />
          <NavItem to="/companies" icon={<IconCompany />} label={t('nav.companies')} />
          <NavItem to="/templates" icon={<IconTemplate />} label={t('nav.templates')} />
          <NavItem to="/suppliers" icon={<IconSupplier />} label={t('nav.suppliers')} />
          <NavItem to="/search" icon={<IconSearch />} label={t('nav.search')} />
          {isAdmin && (
            <>
              <div className="mx-4 my-2 border-t border-slate-700" />
              <NavItem to="/admin" icon={<IconAdmin />} label={t('nav.admin')} />
            </>
          )}
        </nav>

        {/* User */}
        <div className="border-t border-slate-700 px-4 py-3">
          <div className="flex items-center gap-2 mb-2">
            <div className="w-7 h-7 bg-blue-600 rounded-full flex items-center justify-center flex-shrink-0">
              <span className="text-white text-xs font-bold">{user?.name?.charAt(0).toUpperCase()}</span>
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-white text-sm font-medium truncate">{user?.name}</div>
              <div className="text-slate-400 text-xs">{user?.role === 'ADMIN' ? t('admin.roles.ADMIN') : t('admin.roles.STAFF')}</div>
            </div>
          </div>
          <button onClick={logout} className="w-full flex items-center gap-2 px-3 py-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700 text-xs transition-colors">
            <IconLogout />
            {t('common.logout')}
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top bar */}
        <header className="h-14 bg-white border-b border-gray-200 flex items-center gap-4 px-6 flex-shrink-0 shadow-sm">
          <form onSubmit={handleSearch} className="flex-1 max-w-md">
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                <IconSearch />
              </div>
              <input
                type="text"
                value={searchQ}
                onChange={e => setSearchQ(e.target.value)}
                placeholder={t('common.searchPlaceholder')}
                className="w-full pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-gray-50"
              />
            </div>
          </form>

          <div className="flex items-center gap-1 ml-auto">
            {languages.map(lang => (
              <button
                key={lang.code}
                onClick={() => switchLang(lang.code)}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all ${
                  currentLang === lang.code
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-500 hover:bg-gray-100'
                }`}
              >
                {lang.flag} {lang.label}
              </button>
            ))}
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}

// SVG Icons
function IconDashboard() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-5 h-5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
}
function IconShipment() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-5 h-5"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
}
function IconProduct() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-5 h-5"><path d="M20 7H4a2 2 0 00-2 2v6a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2z"/><path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/></svg>
}
function IconTcGrs() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-5 h-5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
}
function IconCompany() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-5 h-5"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
}
function IconTemplate() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-5 h-5"><path d="M4 6h16M4 12h16M4 18h7"/></svg>
}
function IconSupplier() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-5 h-5"><circle cx="12" cy="7" r="4"/><path d="M5.5 21a9 9 0 0113 0"/></svg>
}
function IconSearch() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-4 h-4"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
}
function IconAdmin() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-5 h-5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
}
function IconLogout() {
  return <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} className="w-4 h-4"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"/></svg>
}
