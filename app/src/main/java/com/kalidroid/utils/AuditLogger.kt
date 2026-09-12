package com.kalidroid.utils

import android.content.Context
import com.kalidroid.KaliDroidApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 审计日志（v2.1）。
 *
 * 记录每次执行：时间 / 用户 / 模式 / 命令 / 是否放行 / 结果。
 * 落盘到 filesDir/audit/audit-YYYYMMDD.log，行式 JSON。
 */
object AuditLogger {

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun log(
        mode: String,
        command: String,
        allowed: Boolean,
        denied: Boolean,
        ok: Boolean,
        note: String = ""
    ) {
        try {
            val ctx: Context = KaliDroidApp.instance
            val dir = File(ctx.filesDir, "audit").apply { mkdirs() }
            val file = File(dir, "audit-${dayFmt.format(Date())}.log")
            // 单日文件超过 2MB 轮转到 audit-<day>-<seq>.log
            if (file.length() > MAX_BYTES_PER_DAY) {
                var seq = 1
                while (File(dir, "audit-${dayFmt.format(Date())}-$seq.log").exists()) seq++
                file.renameTo(File(dir, "audit-${dayFmt.format(Date())}-$seq.log"))
            }
            val line = buildString {
                append("{\"ts\":\"").append(fmt.format(Date()))
                append("\",\"mode\":\"").append(mode)
                append("\",\"cmd\":\"").append(command.replace("\"", "\\\"").take(500))
                append("\",\"allowed\":").append(allowed)
                append(",\"denied\":").append(denied)
                append(",\"ok\":").append(ok)
                append(",\"note\":\"").append(note.replace("\"", "\\\"").take(200))
                append("\"}\n")
            }
            file.appendText(line)
        } catch (_: Throwable) {
            // 审计失败不影响主流程
        }
    }

    private const val MAX_BYTES_PER_DAY = 2L * 1024 * 1024
}