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

    private var requestedOnce = false

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
                        Text("su=${RootUtils.hasSu()}  Shizuku=${ShizukuUtils.isRunning()}  已授权=${ShizukuUtils.isGranted()}")

                        if (!ShizukuUtils.isRunning()) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    // 自动索要 ADB 通道：先拉起 Shizuku，再请求授权
                                    if (ShizukuUtils.isInstalled(this@PermissionChooserActivity)) {
                                        ShizukuUtils.openShizuku(this@PermissionChooserActivity)
                                    } else {
                                        enter(ExecutorMode.NORMAL, "未安装 Shizuku，已选择普通执行器")
                                    }
                                }
                            ) { Text("授权 ADB 通道（Shizuku）") }
                        } else if (!ShizukuUtils.isGranted()) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    ShizukuUtils.requestPermission { granted ->
                                        if (granted) {
                                            enter(ExecutorMode.ADB, "Shizuku 已授权，ADB 通道可用")
                                        } else {
                                            enter(ExecutorMode.NORMAL, "Shizuku 授权被拒绝，已降级 NORMAL")
                                        }
                                    }
                                }
                            ) { Text("Shizuku 已运行，点击授权") }
                        } else {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { enter(ExecutorMode.ADB) }
                            ) { Text("ADB 通道已就绪，直接进入") }
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { enter(ExecutorMode.NORMAL) }
                        ) { Text("普通执行器  ·  系统命令") }

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (ShizukuUtils.isRunning()) {
                                    if (ShizukuUtils.isGranted()) enter(ExecutorMode.ADB)
                                    else ShizukuUtils.requestPermission { granted ->
                                        enter(if (granted) ExecutorMode.ADB else ExecutorMode.NORMAL,
                                            if (granted) "Shizuku 已授权" else "授权被拒绝，降级普通")
                                    }
                                } else {
                                    enter(ExecutorMode.ADB, "Shizuku 未运行，将降级普通")
                                }
                            }
                        ) { Text("ADB 执行器  ·  Shizuku") }

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { enter(ExecutorMode.ROOT, if (RootUtils.hasSu()) "已检测到 su" else "未检测到 su，将降级普通") }
                        ) { Text("Root 执行器  ·  su -c") }

                        Text("高级通道可用时启用，不可用时自动回退普通执行器。", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 自动索要：Shizuku 已运行但未授权时，自动弹一次授权请求
        if (!requestedOnce && ShizukuUtils.isRunning() && !ShizukuUtils.isGranted()) {
            requestedOnce = true
            ShizukuUtils.requestPermission { granted ->
                if (granted) {
                    enter(ExecutorMode.ADB, "Shizuku 已自动授权，ADB 通道可用")
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
