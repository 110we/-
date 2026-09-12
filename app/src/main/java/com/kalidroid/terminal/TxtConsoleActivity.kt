package com.kalidroid.terminal

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kalidroid.KaliDroidApp
import com.kalidroid.ui.tools.ToolCatalog
import kotlinx.coroutines.launch

class TxtConsoleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val executor = KaliDroidApp.instance.permissionManager.getExecutor()
        val tools = ToolCatalog.items

        val log = TextView(this).apply {
            setTextColor(Color.parseColor("#C8C8C8"))
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setPadding(24, 24, 24, 24)
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(log)
        }
        val input = EditText(this).apply {
            setTextColor(Color.parseColor("#C8C8C8"))
            setHintTextColor(Color.parseColor("#666666"))
            setBackgroundColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            hint = "编号或命令"
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine()
        }

        fun help(): String {
            val lines = mutableListOf("KaliDroid TXT", "输入编号或命令直接调用工具。", "", "help / status", "")
            tools.forEachIndexed { i, tool ->
                lines += "${(i + 1).toString().padStart(2, ' ')}  ${tool.command.padEnd(10)} ${tool.name}"
            }
            return lines.joinToString("\n")
        }

        log.text = help()

        fun append(chunk: String) {
            log.append("\n$chunk")
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }

        fun runLine(raw: String) {
            val cmd = raw.trim()
            if (cmd.isEmpty()) return
            append("> $cmd")
            when (cmd.lowercase()) {
                "help" -> append(help())
                "status" -> append("executor=${KaliDroidApp.instance.permissionManager.currentMode()}")
                else -> {
                    val index = cmd.toIntOrNull()
                    val tool = if (index != null && index in 1..tools.size) tools[index - 1]
                    else tools.find { it.command == cmd || it.name == cmd }
                    val command = tool?.command ?: cmd
                    lifecycleScope.launch {
                        val result = executor.execAsync(command)
                        append("$ $command\n" + result.stdout.ifBlank { result.stderr })
                    }
                }
            }
        }

        input.setOnEditorActionListener { _, _, _ ->
            runLine(input.text.toString())
            input.setText("")
            true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(input)
        }
        setContentView(root)
    }
}
