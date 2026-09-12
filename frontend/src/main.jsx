import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App.jsx'
import './styles.css'

createRoot(document.getElementById('app')).render(
  <ConfigProvider
    locale={zhCN}
    theme={{
      algorithm: theme.darkAlgorithm,
      token: {
        colorPrimary: '#3de0c5',
        colorBgBase: '#07111b',
        colorBgContainer: '#0c1a27',
        colorBgElevated: '#0f2135',
        colorText: '#dff7f1',
        colorTextSecondary: '#8fb4b0',
        colorBorder: 'rgba(61, 224, 197, 0.18)',
        colorBorderSecondary: 'rgba(61, 224, 197, 0.1)',
        borderRadius: 12,
        colorDanger: '#ff6b6b',
        colorSuccess: '#4ade80',
      },
      components: {
        Card: {
          colorBgContainer: 'rgba(7, 19, 29, 0.9)',
        },
        Button: {
          fontWeight: 500,
        },
      },
    }}
  >
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </ConfigProvider>
)
