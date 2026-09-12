<template>
  <div class="txt-wrap">
    <pre class="txt" ref="board">{{ text }}</pre>
    <form class="txt-form" @submit.prevent="submit">
      <span>&gt;</span>
      <input v-model="line" autocomplete="off" spellcheck="false" />
    </form>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { api } from '../api'

const text = ref('KaliDroid TXT\nloading...\n')
const line = ref('')
const board = ref(null)

function append(chunk) {
  text.value += chunk.endsWith('\n') ? chunk : `${chunk}\n`
  nextTick(() => {
    if (board.value) board.value.scrollTop = board.value.scrollHeight
  })
}

async function submit() {
  const cmd = line.value.trim()
  line.value = ''
  if (!cmd) return
  append(`> ${cmd}`)
  try {
    const result = await api.txt(cmd)
    append(result.text || '')
  } catch (e) {
    append(e.message)
  }
}

onMounted(async () => {
  const result = await api.txt('help')
  text.value = result.text || ''
})
</script>
