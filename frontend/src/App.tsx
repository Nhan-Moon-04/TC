import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Shipments from './pages/Shipments'
import ShipmentDetail from './pages/ShipmentDetail'
import Products from './pages/Products'
import Companies from './pages/Companies'
import TcGrs from './pages/TcGrs'
import Templates from './pages/Templates'
import Suppliers from './pages/Suppliers'
import Search from './pages/Search'
import Admin from './pages/Admin'

function ProtectedRoute({ children, adminOnly = false }: { children: React.ReactNode; adminOnly?: boolean }) {
  const { isAuthenticated, isAdmin } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (adminOnly && !isAdmin) return <Navigate to="/" replace />
  return <>{children}</>
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={
        <ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>
      } />
      <Route path="/shipments" element={
        <ProtectedRoute><Layout><Shipments /></Layout></ProtectedRoute>
      } />
      <Route path="/shipments/:id" element={
        <ProtectedRoute><Layout><ShipmentDetail /></Layout></ProtectedRoute>
      } />
      <Route path="/products" element={
        <ProtectedRoute><Layout><Products /></Layout></ProtectedRoute>
      } />
      <Route path="/companies" element={
        <ProtectedRoute><Layout><Companies /></Layout></ProtectedRoute>
      } />
      <Route path="/tcgrs" element={
        <ProtectedRoute><Layout><TcGrs /></Layout></ProtectedRoute>
      } />
      <Route path="/templates" element={
        <ProtectedRoute><Layout><Templates /></Layout></ProtectedRoute>
      } />
      <Route path="/suppliers" element={
        <ProtectedRoute><Layout><Suppliers /></Layout></ProtectedRoute>
      } />
      <Route path="/search" element={
        <ProtectedRoute><Layout><Search /></Layout></ProtectedRoute>
      } />
      <Route path="/admin" element={
        <ProtectedRoute adminOnly><Layout><Admin /></Layout></ProtectedRoute>
      } />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
