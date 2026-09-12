package com.kalidroid.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalidroid.KaliDroidApp
import com.kalidroid.bridge.NativeExecutorBridge
import com.kalidroid.permission.ExecutorMode
import com.kalidroid.utils.FileUtils
import com.kalidroid.utils.RootUtils
import com.kalidroid.utils.ShizukuUtils

@Composable
fun SettingsScreen(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val app = KaliDroidApp.instance
    val pm = app.permissionManager
    val scope = rememberCoroutineScope()
    val bridge = remember { NativeExecutorBridge(scope) }
    var wsUrl by remember { mutableStateOf("ws://127.0.0.1:3001/ws") }
    var message by remember { mutableStateOf("当前执行器: ${pm.currentMode()}") }

    DisposableEffect(Unit) {
        onDispose { bridge.disconnect() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        Text("权限切换 / WebSocket 桥接 / 容器管理 / 主题", color = MaterialTheme.colorScheme.secondary)

        Text("su=${RootUtils.hasSu()}  Shizuku=${ShizukuUtils.isRunning()}")
        Text("原生桥接: ${if (bridge.connected) "在线" else "未连接"}")

        OutlinedTextField(
            value = wsUrl,
            onValueChange = { wsUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Node.js WebSocket") }
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            bridge.connect(wsUrl)
            message = "正在连接 Android 原生执行器: $wsUrl"
        }) { Text("通道：WebSocket → 原生执行器") }

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            bridge.disconnect()
            message = "已断开原生执行器"
        }) { Text("断开 WebSocket") }

        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            pm.persist(ExecutorMode.NORMAL)
            message = "已切换到 NormalExecutor"
        }) { Text("权限：普通执行器") }

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            pm.persist(ExecutorMode.ADB)
            val alive = ShizukuUtils.isRunning()
            message = if (alive) "已切换到 ADB / Shizuku 执行器" else "Shizuku 未运行，已降级 NORMAL"
        }) { Text("权限：ADB / Shizuku") }

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            pm.persist(ExecutorMode.ROOT)
            val hasSu = RootUtils.hasSu()
            message = if (hasSu) "已切换到 Root 执行器" else "未检测到 su，已降级 NORMAL"
        }) { Text("权限：Root su -c") }

        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            message = app.kaliContainer.start().message
        }) { Text("容器：start()") }

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            message = app.kaliContainer.stop().message
        }) { Text("容器：stop()") }

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            val r = app.kaliContainer.exec("uname -a")
            message = if (r.ok) "容器输出: ${r.stdout.trim()}" else "容器执行失败: ${r.stderr}"
        }) { Text("容器：exec(uname)") }

        Button(modifier = Modifier.fillMaxWidth(), onClick = onToggleTheme) {
            Text(if (darkTheme) "主题：切换浅色" else "主题：切换深色")
        }

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            FileUtils.write(app, "settings.txt", "theme=${if (darkTheme) "dark" else "light"}")
            message = "已写入 filesDir: ${FileUtils.list(app)}"
        }) { Text("写入 /data/data 应用目录") }

        Text(message, color = MaterialTheme.colorScheme.primary)
    }
}
