import { createRouter, createWebHistory } from 'vue-router'
import PermissionChooser from './views/PermissionChooser.vue'
import Shell from './views/Shell.vue'
import Dashboard from './views/Dashboard.vue'
import Tools from './views/Tools.vue'
import ToolDetail from './views/ToolDetail.vue'
import Terminal from './views/Terminal.vue'
import Settings from './views/Settings.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: PermissionChooser },
    {
      path: '/app',
      component: Shell,
      children: [
        { path: '', redirect: '/app/dashboard' },
        { path: 'dashboard', component: Dashboard },
        { path: 'tools', component: Tools },
        { path: 'tools/:name', component: ToolDetail },
        { path: 'terminal', component: Terminal },
        { path: 'settings', component: Settings }
      ]
    }
  ]
})
