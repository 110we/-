package com.kalidroid.permission

import android.content.Context
import com.kalidroid.utils.RootUtils
import com.kalidroid.utils.ShizukuUtils

enum class ExecutorMode {
    NORMAL,
    ADB,
    ROOT
}

class PermissionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("kalidroid.permission", Context.MODE_PRIVATE)

    private val normalExecutor = NormalExecutor()
    private val adbExecutor = AdbExecutor()
    private val rootExecutor = RootExecutor()

    fun detect(): Map<String, Boolean> = mapOf(
        "normal" to true,
        "adb" to ShizukuUtils.isRunning(),
        "root" to RootUtils.hasSu()
    )

    fun currentMode(): ExecutorMode {
        val stored = prefs.getString(KEY_MODE, ExecutorMode.NORMAL.name) ?: ExecutorMode.NORMAL.name
        val mode = runCatching { ExecutorMode.valueOf(stored) }.getOrDefault(ExecutorMode.NORMAL)
        // 请求的高级通道若当前不可用，降级 NORMAL 但保留请求值
        return when (mode) {
            ExecutorMode.ADB -> if (ShizukuUtils.isRunning()) mode else ExecutorMode.NORMAL
            ExecutorMode.ROOT -> if (RootUtils.hasSu()) mode else ExecutorMode.NORMAL
            ExecutorMode.NORMAL -> mode
        }
    }

    fun persist(mode: ExecutorMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getExecutor(): CommandExecutor = when (currentMode()) {
        ExecutorMode.NORMAL -> normalExecutor
        ExecutorMode.ADB -> adbExecutor
        ExecutorMode.ROOT -> rootExecutor
    }

    fun executorFor(mode: ExecutorMode): CommandExecutor = when (mode) {
        ExecutorMode.NORMAL -> normalExecutor
        ExecutorMode.ADB -> adbExecutor
        ExecutorMode.ROOT -> rootExecutor
    }

    companion object {
        private const val KEY_MODE = "executor_mode"
    }
}
