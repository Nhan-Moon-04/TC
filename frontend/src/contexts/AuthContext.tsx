import React, { createContext, useContext, useState, useCallback } from 'react'
import client from '../api/client'

interface User {
  id: number
  username: string
  name: string
  role: 'ADMIN' | 'STAFF'
}

interface AuthContextType {
  user: User | null
  token: string | null
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    try {
      const stored = localStorage.getItem('tcgrs_user')
      return stored ? JSON.parse(stored) : null
    } catch {
      localStorage.removeItem('tcgrs_user')
      return null
    }
  })
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('tcgrs_token')
  })

  const login = useCallback(async (username: string, password: string) => {
    const res = await client.post('/auth/login', { username, password })
    const { token: newToken, user: newUser } = res.data
    localStorage.setItem('tcgrs_token', newToken)
    localStorage.setItem('tcgrs_user', JSON.stringify(newUser))
    setToken(newToken)
    setUser(newUser)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('tcgrs_token')
    localStorage.removeItem('tcgrs_user')
    setToken(null)
    setUser(null)
    window.location.href = '/login'
  }, [])

  const isAdmin = user?.role === 'ADMIN'
  const isAuthenticated = !!token && !!user

  return (
    <AuthContext.Provider value={{ user, token, isAdmin, login, logout, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
