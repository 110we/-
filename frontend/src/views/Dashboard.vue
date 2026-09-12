<template>
  <div class="page stack">
    <div class="brand">MAINACTIVITY</div>
    <h1>仪表盘</h1>
    <p class="muted">状态 + 快捷工具</p>
    <p v-if="notice" class="danger">{{ notice }}</p>
    <div class="card">
      <h3>运行状态</h3>
      <p>请求权限: {{ requestedMode }}</p>
      <p>当前执行器: {{ status.executor }}</p>
      <p>JNI: {{ status.jni }}</p>
      <p>主机: {{ status.host }}</p>
      <p>{{ status.container }}</p>
      <p v-for="line in status.hardware || []" :key="line">{{ line }}</p>
    </div>
    <div class="row">
      <button class="btn small" @click="$router.push('/app/terminal')">全屏终端</button>
      <button class="btn-outline small" @click="$router.push('/app/tools')">工具库</button>
      <button class="btn-outline small" @click="$router.push('/app/settings')">设置</button>
    </div>
    <h3>快捷工具</h3>
    <div class="card" v-for="tool in shortcuts" :key="tool.name">
      <h3>{{ tool.name }}</h3>
      <p class="muted">{{ tool.description }}</p>
      <button class="btn small" @click="$router.push('/app/tools/' + encodeURIComponent(tool.name))">打开</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const status = ref({})
const shortcuts = ref([])
const notice = sessionStorage.getItem('kd-notice') || ''
const requestedMode = sessionStorage.getItem('kd-mode') || 'NORMAL'

onMounted(async () => {
  status.value = await api.status()
  const data = await api.tools()
  shortcuts.value = (data.items || []).slice(0, 4)
})
</script>
