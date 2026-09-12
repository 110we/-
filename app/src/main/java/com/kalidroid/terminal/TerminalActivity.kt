package com.kalidroid.terminal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kalidroid.KaliDroidApp
import kotlinx.coroutines.launch

class TerminalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val executor = KaliDroidApp.instance.permissionManager.getExecutor()

        val terminalView = TerminalView(this).apply {
            setPadding(16, 16, 16, 16)
        }
        val input = EditText(this).apply {
            hint = "白名单命令: uname"
            setText("uname")
            setTextColor(0xFFDFF7F1.toInt())
            setHintTextColor(0xFF8FB4B0.toInt())
            setBackgroundColor(0xFF07131D.toInt())
        }
        val run = Button(this).apply { text = "运行" }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(run)
        }
        val scroll = ScrollView(this).apply { addView(terminalView) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF07111B.toInt())
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(row)
        }
        setContentView(root)

        fun execNow() {
            val cmd = input.text.toString()
            terminalView.append("$ $cmd")
            lifecycleScope.launch {
                val result = executor.execAsync(cmd)
                val body = result.stdout.ifBlank { result.stderr }
                terminalView.append(body.ifBlank { "(empty)" })
                scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
        run.setOnClickListener { execNow() }
    }
}
