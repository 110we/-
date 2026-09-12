import { useEffect, useRef, useState } from 'react'
import { api, wsUrl } from '../api'

const COMMANDS = ['date', 'uname', 'hostname', 'whoami', 'id', 'uptime', 'pwd', 'df', 'free', 'ls']

export default function Terminal() {
  const [input, setInput] = useState('uname')
  const [output, setOutput] = useState('KaliDroid WS terminal ready.\n路径1: Android Shell / ADB\n路径2: Android 原生执行器 → Root / ADB / Normal')
  const [channel, setChannel] = useState('shell')
  const wsRef = useRef(null)

  useEffect(() => {
    api.settings().then((s) => setChannel(s.channel || 'shell'))
    const socket = new WebSocket(wsUrl())
    wsRef.current = socket
    socket.onopen = () => socket.send(JSON.stringify({ type: 'hello', role: 'ui' }))
    socket.onmessage = (ev) => {
      const msg = JSON.parse(ev.data)
      if (msg.type === 'result') {
        const via = msg.via ? `[${msg.via} / ${msg.executor}]` : ''
        setOutput((prev) => `${prev}\n$ ${msg.command} ${via}\n${msg.stdout || msg.stderr || ''}`)
      }
    }
    return () => socket.close()
  }, [])

  function run(e) {
    e.preventDefault()
    const socket = wsRef.current
    if (!socket || socket.readyState !== 1) {
      setOutput((prev) => `${prev}\nWebSocket 未连接`)
      return
    }
    socket.send(JSON.stringify({ type: 'exec', id: `t-${Date.now()}`, command: input, channel }))
  }

  return (
    <div className="page stack">
      <h1>全屏终端</h1>
      <p className="muted">UI → WebSocket → Node.js → {channel === 'native' ? 'Android 原生执行器' : '系统命令 / Android Shell'}</p>
      <div className="tabs">
        {COMMANDS.map((c) => (
          <button key={c} className="chip" onClick={() => setInput(c)}>{c}</button>
        ))}
      </div>
      <form className="row" onSubmit={run}>
        <input className="input" value={input} onChange={(e) => setInput(e.target.value)} />
        <button className="btn small" type="submit">运行</button>
      </form>
      <pre className="term">{output}</pre>
    </div>
  )
}
