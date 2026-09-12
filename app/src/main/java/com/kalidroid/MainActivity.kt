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
import com.kalidroid.model.Tool
import com.kalidroid.ui.dashboard.DashboardScreen
import com.kalidroid.ui.settings.SettingsScreen
import com.kalidroid.ui.theme.KaliDroidTheme
import com.kalidroid.ui.tools.ToolCatalog
import com.kalidroid.ui.tools.ToolDetailScreen
import com.kalidroid.ui.tools.ToolLibraryScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notice = intent.getStringExtra("notice").orEmpty()
        val requestedMode = intent.getStringExtra("requestedMode").orEmpty()

        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(true) }
            KaliDroidTheme(darkTheme = darkTheme) {
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

    private fun toolRoute(tool: Tool): String {
        val name = URLEncoder.encode(tool.name, StandardCharsets.UTF_8.name())
        return "tool/$name"
    }
}
