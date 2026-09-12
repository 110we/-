package com.kalidroid.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kalidroid.KaliDroidApp
import com.kalidroid.model.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ToolDetailScreen(tool: Tool, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var extra by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("尚未执行") }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(tool.name, style = MaterialTheme.typography.headlineMedium)
        Text(tool.description, color = MaterialTheme.colorScheme.secondary)
        Text("分类: ${tool.category}")
        Text("命令模板: ${tool.command}")
        if (tool.command.startsWith("kali ") || tool.category == "容器" || tool.category == "工具链") {
            Text("⚡ 该工具走 Kali 容器通道（proot）", color = MaterialTheme.colorScheme.tertiary)
        }
        OutlinedTextField(
            value = extra,
            onValueChange = { extra = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("附加参数（容器命令将拼接到尾部）") }
        )
        Button(onClick = {
            scope.launch {
                output = runTool(tool, extra)
            }
        }) { Text("用当前执行器运行") }
        Text(output, fontFamily = FontFamily.Monospace)
        Button(onClick = onBack) { Text("返回") }
    }
}

/** 工具执行路由：容器/工具链命令走 KaliContainer，其余走当前执行器 */
private suspend fun runTool(tool: Tool, extra: String): String {
    val container = KaliDroidApp.instance.kaliContainer
    val cmd = tool.command
    val full = if (extra.isBlank()) cmd else "$cmd $extra"
    return when {
        // ---- 容器管理 ----
        cmd == "kali status" -> {
            val st = container.status()
            "state=${st.message}\npid=${st.pid}"
        }
        cmd == "kali start" -> {
            val st = container.start()
            "${if (st.running) "✅" else "❌"} ${st.message}"
        }
        cmd == "kali stop" -> {
            val st = container.stop()
            st.message
        }
        cmd == "kali rootfs" -> {
            val rs = container.status()
            rs.message
        }
        cmd == "kali download-rootfs" -> {
            val rs = container.rootfsStatus()
            if (rs.present) return "rootfs 已就绪：${rs.message}，无需重新下载"
            // 阻塞式下载+解压（内含进度回调），后台线程执行
            val result = withContext(Dispatchers.IO) {
                container.downloadRootfs { pct ->
                    println("KaliDroid rootfs 下载进度: ${(pct * 100).toInt()}%")
                }
            }
            if (result.present) "✅ rootfs 安装完成：${result.message}"
            else "❌ 下载/安装失败：${result.message}"
        }
        cmd == "kali proot" -> {
            "proot 安装状态: ${if (container.prootInstalled()) "已安装" else "未安装（请将 proot 放入 assets 后更新 APK）"}"
        }
        // ---- 容器内执行（含工具链 / kali exec / kali shell）----
        cmd.startsWith("kali ") || tool.category == "工具链" -> {
            val inner = when {
                cmd == "kali exec" -> extra
                cmd == "kali shell" -> "echo '进入容器（交互 shell 需终端 App，此处执行一次命令）' && uname -a"
                else -> full.removePrefix("kali ")
            }
            if (inner.isBlank()) return "缺少要执行的命令"
            val r = container.exec(inner)
            r.stdout.ifBlank { r.stderr }.ifBlank { "(无输出) exit=${r.exitCode}" }
        }
        // ---- 常规工具走当前执行器 ----
        else -> {
            val result = KaliDroidApp.instance.permissionManager.getExecutor().execAsync(full)
            listOfNotNull(
                if (extra.isNotBlank() && !isContainerTool(tool)) "附加参数已忽略: $extra" else null,
                result.stdout.ifBlank { result.stderr }
            ).joinToString("\n")
        }
    }
}

private fun isContainerTool(tool: Tool): Boolean =
    tool.command.startsWith("kali ") || tool.category == "容器" || tool.category == "工具链"
