package com.kalidroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kalidroid.kali.ProotDownloader
import com.kalidroid.model.Tool
import com.kalidroid.ui.dashboard.DashboardScreen
import com.kalidroid.ui.init.InitScreen
import com.kalidroid.ui.settings.SettingsScreen
import com.kalidroid.ui.theme.KaliDroidTheme
import com.kalidroid.ui.tools.ToolCatalog
import com.kalidroid.ui.tools.ToolDetailScreen
import com.kalidroid.ui.tools.ToolLibraryScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notice = intent.getStringExtra("notice").orEmpty()
        val requestedMode = intent.getStringExtra("requestedMode").orEmpty()

        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(true) }
            KaliDroidTheme(darkTheme = darkTheme) {
                // ---- 初始化状态 ----
                var initialized by rememberSaveable { mutableStateOf(checkInitialized()) }
                var downloading by rememberSaveable { mutableStateOf(false) }
                var progress by rememberSaveable { mutableStateOf(0f) }
                var status by rememberSaveable { mutableStateOf("") }

                if (!initialized) {
                    // ---- 初始化门禁：动画页 + 一键下载核心资源 ----
                    InitScreen(
                        downloading = downloading,
                        progress = progress,
                        status = status,
                        onInit = {
                            if (downloading) return@InitScreen
                            downloading = true
                            progress = 0f
                            status = "正在获取 proot 引擎…"
                            scope.launch {
                                val path = withContext(Dispatchers.IO) {
                                    ProotDownloader.install(this@MainActivity) { p ->
                                        // 进度回调在 IO 线程，切主线程更新 UI
                                        scope.launch {
                                            progress = p
                                            status = when {
                                                p < 0.15f -> "连接 Termux 源…"
                                                p < 0.8f -> "下载 proot 引擎… ${(p * 100).toInt()}%"
                                                p < 0.95f -> "解析 deb 包…"
                                                else -> "释放二进制…"
                                            }
                                        }
                                    }
                                }
                                if (path != null) {
                                    initialized = true
                                    status = "✅ 初始化完成"
                                } else {
                                    downloading = false
                                    status = "❌ 初始化失败，请检查网络后重试"
                                }
                            }
                        }
                    )
                    return@KaliDroidTheme
                }

                // ---- 主界面 ----
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route.orEmpty()
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = route.startsWith("dashboard"),
                                onClick = { nav.navigate("dashboard") { launchSingleTop = true } },
                                icon = { Text("D") },
                                label = { Text("仪表盘") }
                            )
                            NavigationBarItem(
                                selected = route.startsWith("tools") || route.startsWith("tool/"),
                                onClick = { nav.navigate("tools") { launchSingleTop = true } },
                                icon = { Text("T") },
                                label = { Text("工具库") }
                            )
                            NavigationBarItem(
                                selected = route.startsWith("settings"),
                                onClick = { nav.navigate("settings") { launchSingleTop = true } },
                                icon = { Text("S") },
                                label = { Text("设置") }
                            )
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                notice = notice,
                                requestedMode = requestedMode,
                                onOpenTools = { nav.navigate("tools") },
                                onOpenSettings = { nav.navigate("settings") },
                                onOpenTool = { tool -> nav.navigate(toolRoute(tool)) }
                            )
                        }
                        composable("tools") {
                            ToolLibraryScreen { tool -> nav.navigate(toolRoute(tool)) }
                        }
                        composable(
                            route = "tool/{name}",
                            arguments = listOf(navArgument("name") { type = NavType.StringType })
                        ) { entry ->
                            val name = URLDecoder.decode(
                                entry.arguments?.getString("name").orEmpty(),
                                StandardCharsets.UTF_8.name()
                            )
                            val tool = ToolCatalog.find(name) ?: Tool(name, "未知", "", "echo")
                            ToolDetailScreen(tool = tool, onBack = { nav.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(
                                darkTheme = darkTheme,
                                onToggleTheme = { darkTheme = !darkTheme }
                            )
                        }
                    }
                }
            }
        }
    }

    /** 检查核心资源（proot 引擎）是否就绪 */
    private fun checkInitialized(): Boolean {
        val binDir = filesDir.resolve("bin")
        val proot = binDir.resolve("proot")
        return proot.isFile && proot.length() > 100_000
    }

    private fun toolRoute(tool: Tool): String {
        val name = URLEncoder.encode(tool.name, StandardCharsets.UTF_8.name())
        return "tool/$name"
    }
}
