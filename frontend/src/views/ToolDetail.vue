<template>
  <div class="page stack">
    <p class="brand">TOOLDETAILSCREEN</p>
    <h1>{{ tool.name || '工具详情' }}</h1>
    <p class="muted">{{ tool.description }}</p>
    <div class="card">
      <p>分类: {{ tool.category }}</p>
      <p>命令模板: {{ tool.command }}</p>
    </div>
    <input class="input" v-model="extra" placeholder="附加参数（不会绕过白名单）" />
    <button class="btn" @click="run">用当前执行器运行</button>
    <pre class="term">{{ output }}</pre>
    <button class="btn-outline" @click="$router.back()">返回</button>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

const route = useRoute()
const tool = ref({})
const extra = ref('')
const output = ref('尚未执行')

async function run() {
  const result = await api.runTool(tool.value.name)
  const ignored = extra.value ? `附加参数已忽略: ${extra.value}\n` : ''
  output.value = ignored + (result.stdout || result.stderr || JSON.stringify(result, null, 2))
}

onMounted(async () => {
  tool.value = await api.tool(route.params.name)
})
</script>
