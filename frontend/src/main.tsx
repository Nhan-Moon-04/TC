import React from 'react'
import ReactDOM from 'react-dom/client'
import { Toaster } from 'react-hot-toast'
import App from './App'
import './i18n'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
    <Toaster
      position="top-right"
      toastOptions={{
        duration: 3000,
        style: {
          background: '#1e293b',
          color: '#f1f5f9',
          borderRadius: '8px',
          fontSize: '14px'
        },
        success: {
          iconTheme: { primary: '#22c55e', secondary: '#f1f5f9' }
        },
        error: {
          iconTheme: { primary: '#ef4444', secondary: '#f1f5f9' }
        }
      }}
    />
  </React.StrictMode>
)
