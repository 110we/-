import cors from 'cors'
import express from 'express'
import fs from 'node:fs'
import http from 'node:http'
import os from 'os'
import path from 'node:path'
import { execFile } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { WebSocketServer } from 'ws'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const DATA_DIR = path.join(__dirname, 'data')
const SETTINGS_FILE = path.join(DATA_DIR, 'settings.json')
const PORT = Number(process.env.PORT || 3001)

fs.mkdirSync(DATA_DIR, { recursive: true })

const TOOLS = [
  { name: '系统信息', category: '主机', description: '读取内核与架构。命令模板：uname', command: 'uname' },
  { name: '当前用户', category: '主机', description: '显示当前用户。命令模板：whoami', command: 'whoami' },
  { name: '用户标识', category: '主机', description: '显示 uid/gid。命令模板：id', command: 'id' },
  { name: '主机名', category: '主机', description: '读取主机名。命令模板：hostname', command: 'hostname' },
  { name: '运行时长', category: '主机', description: '系统 uptime。命令模板：uptime', command: 'uptime' },
  { name: '当前目录', category: '文件', description: '打印工作目录。命令模板：pwd', command: 'pwd' },
  { name: '文件列表', category: '文件', description: '列出工作区。命令模板：ls', command: 'ls' },
  { name: '磁盘用量', category: '存储', description: '根分区只读容量。命令模板：df', command: 'df' },
  { name: '内存摘要', category: '主机', description: '内存占用。命令模板：free', command: 'free' },
  { name: '系统时间', category: '开发', description: '当前时间。命令模板：date', command: 'date' }
]

const ALLOWED = {
  date: ['date'],
  uname: ['uname', '-a'],
  hostname: ['hostname'],
  whoami: ['whoami'],
  id: ['id'],
  uptime: ['uptime'],
  pwd: ['pwd'],
  df: ['df', '-h', '/'],
  free: ['free', '-h'],
  ls: ['ls', '-lah', path.resolve(__dirname, '..')]
}

const DEFAULT_SETTINGS = {
  executor: 'NORMAL',
  requestedMode: 'NORMAL',
  theme: 'dark',
  productName: 'KaliDroid Ops',
  channel: 'shell'
}

function readSettings() {
  try {
    return { ...DEFAULT_SETTINGS, ...JSON.parse(fs.readFileSync(SETTINGS_FILE, 'utf8')) }
  } catch {
    return { ...DEFAULT_SETTINGS }
  }
}

function writeSettings(value) {
  fs.writeFileSync(SETTINGS_FILE, JSON.stringify(value, null, 2))
}

if (!fs.existsSync(SETTINGS_FILE)) writeSettings(DEFAULT_SETTINGS)

function detect() {
  return { normal: true, adb: false, root: false, hasSu: false, shizuku: false }
}

function choose(mode) {
  const requested = String(mode || 'NORMAL').toUpperCase()
  const settings = readSettings()
  if (requested === 'ADB') {
    settings.executor = 'NORMAL'
    settings.requestedMode = 'ADB'
    writeSettings(settings)
    return {
      requestedMode: 'ADB',
      executor: 'NORMAL',
      notice: 'AdbExecutor 已锁定，Shizuku 通道未启用。已回退 NormalExecutor。'
    }
  }
  if (requested === 'ROOT') {
    settings.executor = 'NORMAL'
    settings.requestedMode = 'ROOT'
    writeSettings(settings)
    return {
      requestedMode: 'ROOT',
      executor: 'NORMAL',
      notice: 'RootExecutor 已锁定，拒绝 su -c。已回退 NormalExecutor。'
    }
  }
  settings.executor = 'NORMAL'
  settings.requestedMode = 'NORMAL'
  writeSettings(settings)
  return { requestedMode: 'NORMAL', executor: 'NORMAL', notice: '' }
}

function runAllowed(command) {
  const key = String(command || '').trim().split(/\s+/)[0]
  const spec = ALLOWED[key]
  if (!spec) {
    return Promise.resolve({
      ok: false,
      command: key,
      stdout: '',
      stderr: `命令不在白名单: ${key}。允许: ${Object.keys(ALLOWED).join(' ')}`,
      denied: true
    })
  }
  return new Promise((resolve) => {
    execFile(spec[0], spec.slice(1), { timeout: 4000 }, (err, stdout, stderr) => {
      resolve({
        ok: !err,
        command: key,
        stdout: String(stdout || '').slice(0, 8000),
        stderr: String(stderr || (err ? err.message : '')).slice(0, 2000),
        denied: false
      })
    })
  })
}

function kali(action) {
  // v2.1: 容器状态由 Android 原生侧实际状态决定。
  // 后端作为代理，把 start/stop/status/exec 转发给原生执行器（容器控制）。
  // 若原生未连接，返回明确的未连接提示。
  if (!androidOnline()) {
    return { running: false, message: 'Android 原生执行器未连接，容器控制不可用。请先在 App 设置页连接 WebSocket。' }
  }
  return { running: false, message: '容器控制已转发原生侧，请在 App 设置页操作。', pending: true }
}

const uiClients = new Set()
const androidClients = new Set()
const pendingNative = new Map()

function sendJson(ws, payload) {
  if (ws.readyState === 1) ws.send(JSON.stringify(payload))
}

function broadcastUi(payload) {
  for (const ws of uiClients) sendJson(ws, payload)
}

function androidOnline() {
  return androidClients.size > 0
}

function hubStatus() {
  const settings = readSettings()
  return {
    websocket: true,
    androidOnline: androidOnline(),
    androidCount: androidClients.size,
    uiCount: uiClients.size,
    channel: settings.channel,
    executor: settings.executor,
    requestedMode: settings.requestedMode
  }
}

async function runShell(command) {
  const result = await runAllowed(command)
  return {
    ...result,
    via: 'Android Shell / ADB',
    channel: 'shell',
    executor: 'SHELL'
  }
}

function runNativeLocked(command, requestedMode) {
  const mode = String(requestedMode || 'NORMAL').toUpperCase()
  if (mode === 'ROOT') {
    return {
      ok: false,
      command,
      stdout: '',
      stderr: 'RootExecutor 已锁定，拒绝 su -c。',
      denied: true,
      via: 'Android 原生执行器',
      channel: 'native',
      executor: 'ROOT'
    }
  }
  if (mode === 'ADB') {
    return {
      ok: false,
      command,
      stdout: '',
      stderr: 'AdbExecutor 已锁定，Shizuku 通道未启用。',
      denied: true,
      via: 'Android 原生执行器',
      channel: 'native',
      executor: 'ADB'
    }
  }
  return null
}

function forwardNative(command) {
  const locked = runNativeLocked(command)
  if (locked) return Promise.resolve(locked)
  if (!androidOnline()) {
    return Promise.resolve({
      ok: false,
      command,
      stdout: '',
      stderr: 'Android 原生执行器未连接。请在 App 设置里打开 WebSocket 桥接。',
      denied: true,
      via: 'Android 原生执行器',
      channel: 'native',
      executor: 'NORMAL'
    })
  }
  const id = `n-${Date.now()}-${Math.random().toString(16).slice(2)}`
  const payload = {
    type: 'exec',
    id,
    command,
    mode: 'NORMAL'
  }
  return new Promise((resolve) => {
    const timer = setTimeout(() => {
      pendingNative.delete(id)
      resolve({
        ok: false,
        command,
        stdout: '',
        stderr: 'Android 原生执行器超时。',
        denied: true,
        via: 'Android 原生执行器',
        channel: 'native',
        executor: 'NORMAL'
      })
    }, 8000)
    pendingNative.set(id, { resolve, timer, command })
    for (const ws of androidClients) sendJson(ws, payload)
  })
}

async function execute(command, channel) {
  const ch = channel === 'native' ? 'native' : 'shell'
  if (ch === 'native') return forwardNative(command)
  return runShell(command)
}

const app = express()
app.use(cors())
app.use(express.json({ limit: '256kb' }))

app.get('/api/health', (_req, res) => {
  res.json({ ok: true, product: 'KaliDroid', mode: 'ops-console', ws: '/ws' })
})

app.get('/api/architecture', (_req, res) => {
  res.json({
    frontend: 'Vite + React',
    transport: 'WebSocket',
    backend: 'Node.js',
    paths: [
      { id: 'shell', from: '后端服务 Node.js', via: '调用系统命令', to: 'Android Shell / ADB' },
      { id: 'native', from: '后端服务 Node.js', via: 'WebSocket', to: 'Android 原生执行器 → Root / ADB / Normal' }
    ],
    hub: hubStatus()
  })
})

app.get('/api/permission/detect', (_req, res) => res.json(detect()))

app.post('/api/permission/choose', (req, res) => {
  const result = choose(req.body?.mode)
  broadcastUi({ type: 'status', ...hubStatus() })
  res.json(result)
})

app.get('/api/status', (_req, res) => {
  const settings = readSettings()
  const hub = hubStatus()
  res.json({
    executor: settings.executor,
    requestedMode: settings.requestedMode,
    channel: settings.channel,
    jni: 'kalidroid-jni-1.0.0-ops',
    host: `${os.type()} ${os.release()} ${os.arch()}`,
    container: kali('status').message,
    websocket: true,
    androidOnline: hub.androidOnline,
    hardware: [
      'Wi-Fi 扫描已关闭。',
      '蓝牙扫描已关闭。',
      '摄像头取证已关闭。',
      'GPS 采集已关闭。'
    ]
  })
})

app.get('/api/hardware', (_req, res) => {
  res.json({
    wifi: 'Wi-Fi 扫描已关闭。',
    bluetooth: '蓝牙扫描已关闭。',
    camera: '摄像头取证已关闭。',
    gps: 'GPS 采集已关闭。'
  })
})

app.get('/api/tools', (_req, res) => res.json({ items: TOOLS }))

app.get('/api/tools/:name', (req, res) => {
  const tool = TOOLS.find((t) => t.name === decodeURIComponent(req.params.name))
  if (!tool) return res.status(404).json({ error: '工具不存在' })
  res.json(tool)
})

app.post('/api/tools/:name/run', async (req, res) => {
  const tool = TOOLS.find((t) => t.name === decodeURIComponent(req.params.name))
  if (!tool) return res.status(404).json({ error: '工具不存在' })
  const channel = req.body?.channel || readSettings().channel
  res.json(await execute(tool.command, channel))
})

app.post('/api/terminal', async (req, res) => {
  const channel = req.body?.channel || readSettings().channel
  const result = await execute(req.body?.command, channel)
  res.status(result.ok ? 200 : 400).json(result)
})

app.post('/api/kali/:action', (req, res) => {
  res.json(kali(req.params.action))
})

app.get('/api/settings', (_req, res) => res.json({ ...readSettings(), androidOnline: androidOnline() }))

app.put('/api/settings', (req, res) => {
  const current = readSettings()
  const channel = req.body?.channel === 'native' ? 'native' : 'shell'
  const next = {
    ...current,
    theme: req.body?.theme === 'light' ? 'light' : 'dark',
    channel,
    executor: 'NORMAL'
  }
  writeSettings(next)
  broadcastUi({ type: 'status', ...hubStatus() })
  res.json({ ...next, androidOnline: androidOnline() })
})

const server = http.createServer(app)
const wss = new WebSocketServer({ server, path: '/ws' })

wss.on('connection', (ws) => {
  ws.role = 'unknown'
  sendJson(ws, { type: 'ready', ...hubStatus() })

  ws.on('message', async (raw) => {
    let msg = null
    try {
      msg = JSON.parse(String(raw))
    } catch {
      sendJson(ws, { type: 'error', error: 'invalid json' })
      return
    }

    if (msg.type === 'hello') {
      ws.role = msg.role === 'android' ? 'android' : 'ui'
      if (ws.role === 'android') androidClients.add(ws)
      else uiClients.add(ws)
      const status = { type: 'status', ...hubStatus() }
      sendJson(ws, status)
      broadcastUi(status)
      return
    }

    if (msg.type === 'result' && ws.role === 'android') {
      const pending = pendingNative.get(msg.id)
      if (!pending) return
      clearTimeout(pending.timer)
      pendingNative.delete(msg.id)
      pending.resolve({
        ok: Boolean(msg.ok),
        command: pending.command,
        stdout: String(msg.stdout || ''),
        stderr: String(msg.stderr || ''),
        denied: Boolean(msg.denied),
        via: 'Android 原生执行器',
        channel: 'native',
        executor: msg.executor || 'NORMAL'
      })
      return
    }

    if (msg.type === 'exec') {
      const channel = msg.channel || readSettings().channel
      const result = await execute(msg.command, channel)
      sendJson(ws, { type: 'result', id: msg.id, ...result })
    }
  })

  ws.on('close', () => {
    uiClients.delete(ws)
    androidClients.delete(ws)
    broadcastUi({ type: 'status', ...hubStatus() })
  })
})

server.listen(PORT, '0.0.0.0', () => {
  console.log(`KaliDroid API+WS listening on ${PORT}`)
})
