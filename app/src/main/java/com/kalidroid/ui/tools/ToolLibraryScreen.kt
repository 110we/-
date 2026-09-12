package com.kalidroid.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalidroid.model.Tool

@Composable
fun ToolLibraryScreen(onOpen: (Tool) -> Unit) {
    val categories = remember { ToolCatalog.categories() }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val filtered = ToolCatalog.items.filter { tool ->
        val catOk = tab == 0 || tool.category == categories[tab]
        val q = query.trim()
        val qOk = q.isEmpty() || tool.name.contains(q, true) || tool.description.contains(q, true) || tool.command.contains(q, true)
        catOk && qOk
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("工具库", style = MaterialTheme.typography.headlineMedium)
        Text("分类 + 搜索", color = MaterialTheme.colorScheme.secondary)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索名称 / 说明 / 命令") },
            singleLine = true
        )
        ScrollableTabRow(selectedTabIndex = tab) {
            categories.forEachIndexed { i, c ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(c) })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.name }) { tool ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(tool) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tool.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(tool.name, style = MaterialTheme.typography.titleMedium)
                        Text(tool.description, color = MaterialTheme.colorScheme.secondary)
                        Text("command = ${tool.command}")
                    }
                }
            }
        }
    }
}
