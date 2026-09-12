<template>
  <div class="phone">
    <div class="frame">
      <router-view />
      <nav class="nav">
        <router-link to="/app/dashboard">仪表盘</router-link>
        <router-link to="/app/tools">工具库</router-link>
        <router-link to="/app/terminal">终端</router-link>
        <router-link to="/app/settings">设置</router-link>
      </nav>
    </div>
  </div>
</template>
