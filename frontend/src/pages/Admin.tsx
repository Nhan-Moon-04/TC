import React, { useState, useEffect } from 'react'
import api from '../api/client'

interface User {
  id: string
  username: string
  name: string
  email: string
  role: string
  active: boolean
}

export default function Admin() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filterRole, setFilterRole] = useState('')

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    try {
      const res = await api.get('/auth/users')
      setUsers(res.data)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const filteredUsers = users.filter(u => {
    const q = search.toLowerCase()
    if (q && !u.name?.toLowerCase()?.includes(q) && !u.email?.toLowerCase()?.includes(q)) return false
    if (filterRole && u.role !== filterRole) return false
    return true
  })

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Quản lý Tài khoản (Admin only)</h1>
      <p className="mb-4 text-gray-600">Tại đây Admin có thể xem và tạo tài khoản cho nhân viên.</p>
      
      {loading ? <p>Loading...</p> : (
        <div>
          <input type="text" placeholder="Tìm kiếm..." value={search} onChange={e => setSearch(e.target.value)} className="mb-4 p-2 border border-gray-300 rounded" />
          <select value={filterRole} onChange={e => setFilterRole(e.target.value)} className="mb-4 p-2 border border-gray-300 rounded">
            <option value="">Tất cả</option>
            <option value="user">User</option>
            <option value="admin">Admin</option>
          </select>
          <table className="min-w-full bg-white border border-gray-200">
            <thead>
              <tr className="bg-gray-100 border-b">
                <th className="py-2 px-4 text-left">Tên đăng nhập</th>
                <th className="py-2 px-4 text-left">Họ tên</th>
                <th className="py-2 px-4 text-left">Email</th>
                <th className="py-2 px-4 text-left">Role</th>
                <th className="py-2 px-4 text-left">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map(u => (
                <tr key={u.id} className="border-b">
                  <td className="py-2 px-4">{u.username}</td>
                  <td className="py-2 px-4">{u.name}</td>
                  <td className="py-2 px-4">{u.email}</td>
                  <td className="py-2 px-4">{u.role}</td>
                  <td className="py-2 px-4">{u.active ? 'Đang hoạt động' : 'Bị khóa'}</td>
                </tr>
              ))}
              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-4 text-center text-gray-500">Chưa có dữ liệu</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}