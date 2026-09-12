<template>
  <div>
    <div class="page-head">
      <div>
        <h1>工作流编排</h1>
        <p>节点只允许系统信息、文件审计、白名单命令和报告生成。</p>
      </div>
      <button class="btn" @click="save">保存当前工作流</button>
    </div>
    <div class="grid grid-2">
      <div class="card">
        <h2>已保存</h2>
        <div class="chips" style="margin-bottom:12px">
          <button v-for="w in workflows" :key="w.id" class="chip" :class="{ active: current?.id === w.id }" @click="select(w)">
            {{ w.name }}
          </button>
        </div>
        <input class="input" v-model="draft.name" placeholder="名称" />
        <textarea v-model="draft.description" style="margin-top:8px;min-height:70px"></textarea>
        <div class="flow" style="margin-top:14px">
          <template v-for="(node, i) in draft.nodes" :key="node.id">
            <div class="node">
              <strong>{{ node.label }}</strong>
              <small>{{ node.type }}</small>
            </div>
            <span v-if="i < draft.nodes.length - 1" class="arrow">-</span>
          </template>
        </div>
        <p class="muted" style="margin-top:12px">可添加节点：</p>
        <div class="chips">
          <button class="chip" v-for="n in palette" :key="n.type" @click="addNode(n)">{{ n.label }}</button>
        </div>
      </div>
      <div class="card">
        <h2>执行日志</h2>
        <button class="btn" @click="run">运行</button>
        <pre class="term" style="margin-top:12px">{{ logText }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'

const workflows = ref([])
const current = ref(null)
const logText = ref('尚未运行')
const draft = reactive({ id: '', name: '', description: '', nodes: [] })
const palette = [
  { type: 'system.info', label: '系统信息' },
  { type: 'hardware.snapshot', label: '硬件状态' },
  { type: 'network.summary', label: '网络摘要' },
  { type: 'files.list', label: '列出文件' },
  { type: 'hash.file', label: '文件哈希' },
  { type: 'report.generate', label: '生成报告' }
]

function select(w) {
  current.value = w
  draft.id = w.id
  draft.name = w.name
  draft.description = w.description
  draft.nodes = JSON.parse(JSON.stringify(w.nodes || []))
}

function addNode(n) {
  draft.nodes.push({
    id: `n${Date.now()}`,
    type: n.type,
    label: n.label,
    params: n.type === 'hash.file' ? { path: 'README.md' } : {}
  })
}

async function load() {
  const data = await api.workflows()
  workflows.value = data.items || []
  if (workflows.value[0]) select(workflows.value[0])
}

async function save() {
  const saved = await api.saveWorkflow({
    id: draft.id || `wf-${Date.now()}`,
    name: draft.name,
    description: draft.description,
    nodes: draft.nodes
  })
  await load()
  select(saved)
}

async function run() {
  if (!draft.id) await save()
  const result = await api.runWorkflow(draft.id)
  logText.value = JSON.stringify(result, null, 2)
}

onMounted(load)
</script>
