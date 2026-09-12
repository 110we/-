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
import kotlinx.coroutines.launch

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
        OutlinedTextField(
            value = extra,
            onValueChange = { extra = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("附加参数（不会绕过白名单）") }
        )
        Button(onClick = {
            scope.launch {
                val result = KaliDroidApp.instance.permissionManager.getExecutor().execAsync(tool.command)
                output = listOfNotNull(
                    if (extra.isNotBlank()) "附加参数已忽略: $extra" else null,
                    result.stdout.ifBlank { result.stderr }
                ).joinToString("\n")
            }
        }) { Text("用当前执行器运行") }
        Text(output, fontFamily = FontFamily.Monospace)
        Button(onClick = onBack) { Text("返回") }
    }
}
