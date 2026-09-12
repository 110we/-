import { NavLink, Outlet } from 'react-router-dom'
import { LayoutDashboard, Wrench, Terminal, Settings } from 'lucide-react'

export default function Shell() {
  return (
    <div className="phone">
      <div className="frame">
        <Outlet />
        <nav className="nav">
          <NavLink to="/app/dashboard" end>
            <LayoutDashboard size={20} />
            <span>仪表盘</span>
          </NavLink>
          <NavLink to="/app/tools">
            <Wrench size={20} />
            <span>工具</span>
          </NavLink>
          <NavLink to="/app/terminal">
            <Terminal size={20} />
            <span>终端</span>
          </NavLink>
          <NavLink to="/app/settings">
            <Settings size={20} />
            <span>设置</span>
          </NavLink>
        </nav>
      </div>
    </div>
  )
}
