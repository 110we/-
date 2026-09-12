package com.kalidroid.ui.dashboard

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kalidroid.KaliDroidApp
import com.kalidroid.hardware.BluetoothController
import com.kalidroid.hardware.CameraController
import com.kalidroid.hardware.GpsController
import com.kalidroid.hardware.WifiController
import com.kalidroid.jni.NativeLib
import com.kalidroid.model.Tool
import com.kalidroid.terminal.TerminalActivity
import com.kalidroid.ui.tools.ToolCatalog

@Composable
fun DashboardScreen(
    notice: String,
    requestedMode: String,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTool: (Tool) -> Unit
) {
    val context = LocalContext.current
    val app = KaliDroidApp.instance
    val shortcuts = remember { ToolCatalog.items.take(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("仪表盘", style = MaterialTheme.typography.headlineMedium)
        Text("状态 + 快捷工具", color = MaterialTheme.colorScheme.secondary)
        if (notice.isNotBlank()) {
            Text(notice, color = MaterialTheme.colorScheme.error)
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("运行状态", style = MaterialTheme.typography.titleMedium)
                Text("请求权限: ${requestedMode.ifBlank { "NORMAL" }}")
                Text("当前执行器: ${app.permissionManager.currentMode()}")
                Text("JNI: ${runCatching { NativeLib.version() }.getOrDefault("jni-unloaded")}")
                Text("主机: ${runCatching { NativeLib.hostFingerprint() }.getOrDefault("unavailable")}")
                Text(app.kaliContainer.statusLight().message)
                Text(WifiController().scan().message)
                Text(BluetoothController().scanDevices().message)
                Text(CameraController().captureEvidence().message)
                Text(GpsController().read().message)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                context.startActivity(Intent(context, TerminalActivity::class.java))
            }) { Text("全屏终端") }
            TextButton(onClick = onOpenTools) { Text("工具库") }
            TextButton(onClick = onOpenSettings) { Text("设置") }
        }

        Text("快捷工具", style = MaterialTheme.typography.titleMedium)
        shortcuts.forEach { tool ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tool.name, style = MaterialTheme.typography.titleMedium)
                    Text(tool.description, color = MaterialTheme.colorScheme.secondary)
                    Button(onClick = { onOpenTool(tool) }) { Text("打开") }
                }
            }
        }
    }
}
