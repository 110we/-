<template>
  <div class="page stack">
    <h1>全屏终端</h1>
    <p class="muted">TerminalActivity + TerminalView ANSI 渲染</p>
    <div class="tabs">
      <button class="chip" v-for="c in commands" :key="c" @click="input = c">{{ c }}</button>
    </div>
    <form class="row" @submit.prevent="run">
      <input class="input" v-model="input" />
      <button class="btn small" type="submit">运行</button>
    </form>
    <pre class="term">{{ output }}</pre>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { api } from '../api'

const commands = ['date', 'uname', 'hostname', 'whoami', 'id', 'uptime', 'pwd', 'df', 'free', 'ls']
const input = ref('uname')
const output = ref('KaliDroid terminal ready.\nAllowed: date uname hostname whoami id uptime pwd df free ls')

async function run() {
  try {
    const r = await api.terminal(input.value)
    output.value += `\n$ ${r.command}\n${r.stdout || r.stderr || ''}`
  } catch (e) {
    output.value += `\n拒绝: ${e.message}`
  }
}
</script>
