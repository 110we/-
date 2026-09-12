<template>
  <div class="page stack">
    <h1>工具库</h1>
    <p class="muted">分类 + 搜索</p>
    <input class="input" v-model="query" placeholder="搜索名称 / 说明 / 命令" />
    <div class="tabs">
      <button v-for="c in categories" :key="c" class="chip" :class="{ active: tab === c }" @click="tab = c">{{ c }}</button>
    </div>
    <div class="card" v-for="tool in filtered" :key="tool.name" @click="$router.push('/app/tools/' + encodeURIComponent(tool.name))" style="cursor:pointer">
      <p class="brand">{{ tool.category }}</p>
      <h3>{{ tool.name }}</h3>
      <p class="muted">{{ tool.description }}</p>
      <p>command = {{ tool.command }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'

const items = ref([])
const query = ref('')
const tab = ref('全部')
const categories = computed(() => ['全部', ...new Set(items.value.map((t) => t.category))])
const filtered = computed(() => items.value.filter((t) => {
  const catOk = tab.value === '全部' || t.category === tab.value
  const q = query.value.trim().toLowerCase()
  const qOk = !q || [t.name, t.description, t.command].join(' ').toLowerCase().includes(q)
  return catOk && qOk
}))

onMounted(async () => {
  const data = await api.tools()
  items.value = data.items || []
})
</script>
