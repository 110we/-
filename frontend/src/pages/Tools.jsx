import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { Search, ChevronRight, Cpu, FileCode, HardDrive, Terminal, Box } from 'lucide-react'

const catIcons = {
  '主机': Cpu,
  '文件': FileCode,
  '存储': HardDrive,
  '开发': Terminal,
}

export default function Tools() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [query, setQuery] = useState('')
  const [tab, setTab] = useState('全部')
  const categories = useMemo(() => ['全部', ...new Set(items.map((t) => t.category))], [items])
  const filtered = items.filter((t) => {
    const catOk = tab === '全部' || t.category === tab
    const q = query.trim().toLowerCase()
    const qOk = !q || [t.name, t.description, t.command].join(' ').toLowerCase().includes(q)
    return catOk && qOk
  })

  useEffect(() => {
    api.tools().then((data) => setItems(data.items || []))
  }, [])

  return (
    <div className="page stack">
      <h1>工具库</h1>
      <p className="muted">共 {items.length} 个工具 · 分类 + 搜索</p>

      <div className="row" style={{position:'relative'}}>
        <Search size={16} style={{position:'absolute', left:12, color:'var(--muted)'}} />
        <input
          className="input"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="搜索名称 / 说明 / 命令"
          style={{paddingLeft:36}}
        />
      </div>

      <div className="tabs">
        {categories.map((c) => (
          <button key={c} className={`chip${tab === c ? ' active' : ''}`} onClick={() => setTab(c)}>{c}</button>
        ))}
      </div>

      {filtered.map((tool) => {
        const Icon = catIcons[tool.category] || Box
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

      {filtered.length === 0 && (
        <p className="muted" style={{textAlign:'center', padding:20}}>没有匹配的工具</p>
      )}
    </div>
  )
}
