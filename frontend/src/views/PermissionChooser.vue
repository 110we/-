<template>
  <div class="phone">
    <div class="frame">
      <div class="page stack">
        <div class="brand">KALIDROID</div>
        <h1>启动权限选择</h1>
        <p class="muted">PermissionChooserActivity · 检测、存储、获取执行器</p>
        <div class="card">
          <h3>PermissionManager.detect()</h3>
          <p>normal={{ detect.normal }}</p>
          <p>adb={{ detect.adb }}</p>
          <p>root={{ detect.root }}</p>
          <p class="muted">su={{ detect.hasSu }} · Shizuku={{ detect.shizuku }}</p>
        </div>
        <button class="btn" @click="choose('NORMAL')">NormalExecutor · Runtime.exec()</button>
        <button class="btn-outline" @click="choose('ADB')">AdbExecutor · Shizuku（锁定）</button>
        <button class="btn-outline" @click="choose('ROOT')">RootExecutor · su -c（锁定）</button>
        <p v-if="error" class="danger">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const detect = ref({ normal: true, adb: false, root: false, hasSu: false, shizuku: false })
const error = ref('')

async function choose(mode) {
  error.value = ''
  try {
    const result = await api.choose(mode)
    sessionStorage.setItem('kd-mode', result.requestedMode)
    sessionStorage.setItem('kd-notice', result.notice || '')
    router.push('/app/dashboard')
  } catch (e) {
    error.value = e.message
  }
}

onMounted(async () => {
  try {
    detect.value = await api.detect()
  } catch (e) {
    error.value = e.message
  }
})
</script>
