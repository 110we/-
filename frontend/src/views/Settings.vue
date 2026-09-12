<template>
  <div class="page stack">
    <h1>设置</h1>
    <p class="muted">权限切换 / 容器管理 / 主题</p>
    <div class="card">
      <p>当前执行器: {{ settings.executor }}</p>
      <p>主题: {{ dark ? '深色' : '浅色' }}</p>
    </div>
    <button class="btn" @click="choose('NORMAL')">权限：普通执行器</button>
    <button class="btn-outline" @click="choose('ADB')">权限：ADB / Shizuku（锁定）</button>
    <button class="btn-outline" @click="choose('ROOT')">权限：Root su -c（锁定）</button>
    <button class="btn" @click="container('start')">容器：start()</button>
    <button class="btn-outline" @click="container('stop')">容器：stop()</button>
    <button class="btn-outline" @click="container('exec')">容器：exec()</button>
    <button class="btn" @click="dark = !dark">主题：{{ dark ? '切换浅色' : '切换深色' }}</button>
    <p :class="msg.startsWith('拒绝') || msg.includes('锁定') || msg.includes('禁用') ? 'danger' : 'ok'">{{ msg }}</p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const settings = ref({ executor: 'NORMAL' })
const dark = ref(true)
const msg = ref('')

async function choose(mode) {
  const r = await api.choose(mode)
  msg.value = r.notice || `已选择 ${r.executor}`
  settings.value.executor = r.executor
}

async function container(action) {
  const r = await api.container(action)
  msg.value = r.message
}

onMounted(async () => {
  settings.value = await api.settings()
})
</script>
