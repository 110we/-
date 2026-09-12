import { Navigate, Route, Routes } from 'react-router-dom'
import PermissionChooser from './pages/PermissionChooser.jsx'
import Shell from './pages/Shell.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Tools from './pages/Tools.jsx'
import ToolDetail from './pages/ToolDetail.jsx'
import Terminal from './pages/Terminal.jsx'
import Settings from './pages/Settings.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<PermissionChooser />} />
      <Route path="/app" element={<Shell />}>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="tools" element={<Tools />} />
        <Route path="tools/:name" element={<ToolDetail />} />
        <Route path="terminal" element={<Terminal />} />
        <Route path="settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}
