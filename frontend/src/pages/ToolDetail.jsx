import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, wsUrl } from '../api'

export default function ToolDetail() {
  const { name } = useParams()
  const navigate = useNavigate()
  const [tool, setTool] = useState({})
  const [extra, setExtra] = useState('')
  const [output, setOutput] = useState('尚未执行')
  const [channel, setChannel] = useState('shell')

  useEffect(() => {
    api.tool(name).then(setTool)
    api.settings().then((s) => setChannel(s.channel || 'shell'))
  }, [name])

  function run() {
    const socket = new WebSocket(wsUrl())
    const id = `ui-${Date.now()}`
    socket.onopen = () => {
      socket.send(JSON.stringify({ type: 'hello', role: 'ui' }))
      socket.send(JSON.stringify({ type: 'exec', id, command: tool.command, channel }))
    }
    socket.onmessage = (ev) => {
      const msg = JSON.parse(ev.data)
      if (msg.type !== 'result' || msg.id !== id) return
      const ignored = extra ? `附加参数已忽略: ${extra}\n` : ''
      const via = msg.via ? `[${msg.via} / ${msg.executor}]\n` : ''
      setOutput(ignored + via + (msg.stdout || msg.stderr || '(empty)'))
      socket.close()
    }
    socket.onerror = () => setOutput('WebSocket 连接失败')
  }

  return (
    <div className="page stack">
      <p className="brand">TOOLDETAILSCREEN</p>
      <h1>{tool.name || '工具详情'}</h1>
      <p className="muted">{tool.description}</p>
      <div className="card">
        <p>分类: {tool.category}</p>
        <p>命令模板: {tool.command}</p>
        <p>通道: {channel === 'native' ? 'Android 原生执行器' : 'Android Shell / ADB'}</p>
      </div>
      <input className="input" value={extra} onChange={(e) => setExtra(e.target.value)} placeholder="附加参数（不会绕过白名单）" />
      <button className="btn" onClick={run}>经 WebSocket 运行</button>
      <pre className="term">{output}</pre>
      <button className="btn-outline" onClick={() => navigate(-1)}>返回</button>
    </div>
  )
}
