<template>
  <div>
    <div class="page-head">
      <div>
        <h1>报告</h1>
        <p>生成系统快照报告，用于巡检归档。不含漏洞扫描结果。</p>
      </div>
      <button class="btn" @click="generate">生成快照</button>
    </div>
    <div class="grid grid-2">
      <div class="card">
        <h2>历史</h2>
        <table class="table">
          <thead><tr><th>时间</th><th>标题</th></tr></thead>
          <tbody>
            <tr v-for="r in reports" :key="r.id" @click="current = r" style="cursor:pointer">
              <td>{{ r.createdAt }}</td>
              <td>{{ r.title }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="card">
        <h2>{{ current?.title || '未选择报告' }}</h2>
        <p class="muted">{{ current?.summary }}</p>
        <pre class="term">{{ current ? JSON.stringify(current.sections, null, 2) : '选择左侧报告查看详情' }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const reports = ref([])
const current = ref(null)

async function load() {
  const data = await api.reports()
  reports.value = data.items || []
  current.value = reports.value[0] || null
}

async function generate() {
  const report = await api.generateReport('系统快照报告')
  await load()
  current.value = report
}

onMounted(load)
</script>
