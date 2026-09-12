import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import {
  Server, Wrench, Activity, Radar, Eye, Bug, Wifi,
  ChevronRight, Cpu, HardDrive, Terminal, Search, Network
} from 'lucide-react'

const categoryIcons = {
  '主机': Cpu,
  '文件': HardDrive,
  '存储': HardDrive,
  '开发': Terminal,
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [status, setStatus] = useState({})
  const [shortcuts, setShortcuts] = useState([])
  const notice = sessionStorage.getItem('kd-notice') || ''
  const requestedMode = sessionStorage.getItem('kd-mode') || 'NORMAL'

  useEffect(() => {
    api.status().then(setStatus)
    api.tools().then((data) => setShortcuts((data.items || []).slice(0, 4)))
  }, [])

  const features = [
    { name: '信息收集', desc: '主机/网络/系统', icon: Radar, action: () => navigate('/app/tools') },
    { name: '网络扫描', desc: '端口/服务发现', icon: Network, action: () => navigate('/app/tools') },
    { name: '流量分析', desc: '抓包/协议解析', icon: Eye, action: () => navigate('/app/terminal') },
    { name: '漏洞利用', desc: 'Payload/Exploit', icon: Bug, action: () => navigate('/app/tools') },
  ]

  return (
    <div className="page stack">
      {/* 顶部品牌区 */}
      <div>
        <div className="brand">KALIDROID OPS</div>
        <h1>移动安全工具箱</h1>
        <p className="muted">Vite + React · WebSocket · Node.js</p>
      </div>

      {notice ? <p className="danger">{notice}</p> : null}

      {/* 三个统计卡片 */}
      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-icon"><Server size={22} color="#3de0c5" /></div>
          <div className="stat-label">执行模式</div>
          <div className="stat-value">{requestedMode}</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">
            <span className={`badge ${status.androidOnline ? 'ok' : 'off'}`}>
              <Wifi size={10}/>
              {status.androidOnline ? '在线' : '离线'}
            </span>
          </div>
          <div className="stat-label">执行器状态</div>
          <div className="stat-value">{status.androidOnline ? '已连接' : '未连接'}</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon"><Wrench size={22} color="#3de0c5" /></div>
          <div className="stat-label">工具数量</div>
          <div className="stat-value">10+</div>
        </div>
      </div>

      {/* 四个功能大卡片 2x2 */}
      <div className="feature-grid">
        {features.map((f) => {
          const Icon = f.icon
          return (
            <div key={f.name} className="feature-card" onClick={f.action}>
              <div className="feature-icon-wrap">
                <Icon size={26} color="#3de0c5" />
              </div>
              <div className="feature-name">{f.name}</div>
              <div className="feature-desc">{f.desc}</div>
            </div>
          )
        })}
      </div>

      {/* 运行状态摘要 */}
      <div className="card">
        <h3 style={{display:'flex', alignItems:'center', gap:8}}>
          <Activity size={15} color="#3de0c5" />
          系统状态
        </h3>
        <p>执行器: {status.executor || 'NORMAL'}</p>
        <p>通道: {status.channel === 'native' ? '原生' : 'Shell / ADB'}</p>
        <p>WebSocket: {status.websocket ? '已连接' : '未启用'}</p>
        <p>主机: {status.host || '...'}</p>
        <p>{status.container || ''}</p>
      </div>

      {/* 快捷工具 */}
      <div>
        <h3 style={{margin: '4px 0 10px', fontSize: 15}}>快捷工具</h3>
        {shortcuts.map((tool) => {
          const Icon = categoryIcons[tool.category] || Terminal
          return (
            <div key={tool.name} className="tool-item" onClick={() => navigate(`/app/tools/${encodeURIComponent(tool.name)}`)}>
              <div className="tool-icon">
                <Icon size={18} color="#3de0c5" />
              </div>
              <div className="tool-info">
                <div className="tool-name">{tool.name}</div>
                <div className="tool-desc">{tool.description}</div>
              </div>
              <ChevronRight size={16} color="var(--muted)" />
            </div>
          )
        })}
      </div>
    </div>
  )
}
