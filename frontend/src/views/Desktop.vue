<template>
  <div>
    <div class="page-head">
      <div>
        <h1>桌面</h1>
        <p>运维快捷入口。这是工作台桌面，不是 Kali 图形会话。</p>
      </div>
    </div>
    <div class="desktop">
      <div class="icon-grid">
        <button class="desk-icon" v-for="app in apps" :key="app.to" @click="$router.push(app.to)">
          <div class="tile">{{ app.mark }}</div>
          {{ app.name }}
        </button>
      </div>
      <div class="window">
        <h3>会话状态</h3>
        <p class="muted">执行器：普通用户</p>
        <p class="ok">chroot / Kali 根文件系统：未挂载</p>
        <p class="ok">内核模块：未加载</p>
        <p class="muted">打开任一图标进入对应工作台模块。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
const apps = [
  { name: '仪表盘', mark: 'D', to: '/' },
  { name: '工具库', mark: 'T', to: '/tools' },
  { name: '工作流', mark: 'W', to: '/workflow' },
  { name: '终端', mark: '>', to: '/terminal' },
  { name: '报告', mark: 'R', to: '/reports' },
  { name: '设置', mark: 'S', to: '/settings' }
]
</script>
