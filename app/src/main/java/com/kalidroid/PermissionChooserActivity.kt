package com.kalidroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalidroid.permission.ExecutorMode
import com.kalidroid.ui.theme.KaliDroidTheme
import com.kalidroid.utils.RootUtils
import com.kalidroid.utils.ShizukuUtils

class PermissionChooserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = KaliDroidApp.instance.permissionManager
        val detected = pm.detect()

        setContent {
            KaliDroidTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("KaliDroid", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("启动权限选择", style = MaterialTheme.typography.headlineMedium)
                        Text("PermissionManager 检测、存储、获取执行器。")
                        Text("normal=${detected["normal"]}  adb=${detected["adb"]}  root=${detected["root"]}")
                        Text("su=${RootUtils.hasSu()}  Shizuku=${ShizukuUtils.isRunning()}")

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { enter(ExecutorMode.NORMAL) }
                        ) { Text("NormalExecutor  ·  Runtime.exec()") }

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { enter(ExecutorMode.ADB, if (ShizukuUtils.isRunning()) "Shizuku 已授权" else "Shizuku 未运行，将降级 NORMAL") }
                        ) { Text("AdbExecutor  ·  Shizuku") }

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { enter(ExecutorMode.ROOT, if (RootUtils.hasSu()) "已检测到 su" else "未检测到 su，将降级 NORMAL") }
                        ) { Text("RootExecutor  ·  su -c") }

                        Text("高级通道可用时启用，不可用时自动回退普通执行器。", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    private fun enter(mode: ExecutorMode, notice: String? = null) {
        // v2.1: 持久化用户选择的模式（不可用时 PermissionManager 内部降级）
        KaliDroidApp.instance.permissionManager.persist(mode)
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("requestedMode", mode.name)
        if (!notice.isNullOrBlank()) intent.putExtra("notice", notice)
        startActivity(intent)
        finish()
    }
}
