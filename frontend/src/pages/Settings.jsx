import { useEffect, useState } from 'react'
import { api } from '../api'

export default function Settings() {
  const [settings, setSettings] = useState({ executor: 'NORMAL', channel: 'shell' })
  const [dark, setDark] = useState(true)
  const [msg, setMsg] = useState('')

  useEffect(() => {
    api.settings().then(setSettings)
  }, [])

  async function choose(mode) {
    const r = await api.choose(mode)
    setMsg(r.notice || `已选择 ${r.executor}`)
    setSettings((s) => ({ ...s, executor: r.executor, requestedMode: r.requestedMode }))
  }

  async function setChannel(channel) {
    const r = await api.saveSettings({ channel, theme: dark ? 'dark' : 'light' })
    setSettings(r)
    setMsg(channel === 'native' ? '通道：Android 原生执行器（WebSocket）' : '通道：Android Shell / ADB（系统命令）')
  }

  async function container(action) {
    const r = await api.container(action)
    setMsg(r.message)
  }

  const danger = msg.startsWith('拒绝') || msg.includes('锁定') || msg.includes('禁用') || msg.includes('未连接')

  return (
    <div className="page stack">
      <h1>设置</h1>
      <p className="muted">权限切换 / 执行通道 / 容器管理 / 主题</p>
      <div className="card">
        <p>当前执行器: {settings.executor}</p>
        <p>通道: {settings.channel === 'native' ? 'Android 原生执行器' : 'Android Shell / ADB'}</p>
        <p>原生桥接: {settings.androidOnline ? '在线' : '未连接'}</p>
        <p>主题: {dark ? '深色' : '浅色'}</p>
      </div>
      <button className="btn" onClick={() => setChannel('shell')}>通道：调用系统命令 → Android Shell / ADB</button>
      <button className="btn-outline" onClick={() => setChannel('native')}>通道：WebSocket → Android 原生执行器</button>
      <button className="btn" onClick={() => choose('NORMAL')}>权限：普通执行器</button>
      <button className="btn-outline" onClick={() => choose('ADB')}>权限：ADB / Shizuku（锁定）</button>
      <button className="btn-outline" onClick={() => choose('ROOT')}>权限：Root su -c（锁定）</button>
      <button className="btn" onClick={() => container('start')}>容器：start()</button>
      <button className="btn-outline" onClick={() => container('stop')}>容器：stop()</button>
      <button className="btn-outline" onClick={() => container('exec')}>容器：exec()</button>
      <button className="btn" onClick={() => setDark((v) => !v)}>主题：{dark ? '切换浅色' : '切换深色'}</button>
      <p className={danger ? 'danger' : 'ok'}>{msg}</p>
    </div>
  )
}
