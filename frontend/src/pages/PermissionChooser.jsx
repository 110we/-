import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import {
  Smartphone, Bug, ShieldCheck, Lock, Terminal,
  Radio, AlertCircle
} from 'lucide-react'

export default function PermissionChooser() {
  const navigate = useNavigate()
  const [detect, setDetect] = useState({ normal: true, adb: false, root: false, hasSu: false, shizuku: false })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.detect().then(setDetect).catch((e) => setError(e.message))
  }, [])

  async function choose(mode) {
    setError('')
    setLoading(true)
    try {
      const result = await api.choose(mode)
      sessionStorage.setItem('kd-mode', result.requestedMode)
      sessionStorage.setItem('kd-notice', result.notice || '')
      navigate('/app/dashboard')
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="phone">
      <div className="frame">
        <div className="page stack">
          <div className="brand">KALIDROID</div>
          <h1>启动权限选择</h1>
          <p className="muted">Vite + React · WebSocket · Node.js</p>

          <div className="card">
            <h3 style={{display:'flex', alignItems:'center', gap:8}}>
              <ShieldCheck size={16} color="#3de0c5" />
              PermissionManager.detect()
            </h3>
            <p>normal={String(detect.normal)}</p>
            <p>adb={String(detect.adb)}</p>
            <p>root={String(detect.root)}</p>
            <p className="muted">su={String(detect.hasSu)} · Shizuku={String(detect.shizuku)}</p>
          </div>

          <button className="btn" disabled={loading} onClick={() => choose('NORMAL')}>
            <Terminal size={16} style={{marginRight:6, verticalAlign:-3}}/>
            NormalExecutor · Runtime.exec()
          </button>

          <button className="btn-outline" disabled={loading} onClick={() => choose('ADB')}>
            <Radio size={16} style={{marginRight:6, verticalAlign:-3}}/>
            AdbExecutor · Shizuku（锁定）
          </button>

          <button className="btn-outline" disabled={loading} onClick={() => choose('ROOT')}>
            <Lock size={16} style={{marginRight:6, verticalAlign:-3}}/>
            RootExecutor · su -c（锁定）
          </button>

          {error ? (
            <p className="danger" style={{display:'flex', alignItems:'center', gap:6}}>
              <AlertCircle size={14}/>
              {error}
            </p>
          ) : null}
        </div>
      </div>
    </div>
  )
}
