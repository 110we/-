async function request(path, options = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    const error = new Error(data.error || data.stderr || `请求失败 ${res.status}`)
    error.payload = data
    throw error
  }
  return data
}

export const api = {
  health: () => request('/api/health'),
  architecture: () => request('/api/architecture'),
  detect: () => request('/api/permission/detect'),
  choose: (mode) => request('/api/permission/choose', { method: 'POST', body: JSON.stringify({ mode }) }),
  status: () => request('/api/status'),
  tools: () => request('/api/tools'),
  tool: (name) => request(`/api/tools/${encodeURIComponent(name)}`),
  runTool: (name, channel) => request(`/api/tools/${encodeURIComponent(name)}/run`, { method: 'POST', body: JSON.stringify({ channel }) }),
  terminal: (command, channel) => request('/api/terminal', { method: 'POST', body: JSON.stringify({ command, channel }) }),
  container: (action) => request(`/api/kali/${action}`, { method: 'POST' }),
  settings: () => request('/api/settings'),
  saveSettings: (payload) => request('/api/settings', { method: 'PUT', body: JSON.stringify(payload) })
}

export function wsUrl() {
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${location.host}/ws`
}
