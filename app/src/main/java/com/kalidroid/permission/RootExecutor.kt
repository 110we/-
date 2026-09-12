package com.kalidroid.permission

import com.kalidroid.utils.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Root 执行器（v2.1 条件解锁）。
 *
 * 策略：
 * - 检测 su 是否可用（RootUtils.hasSu）
 * - 每次执行拉起 su -c，超时保护 + 输出截断
 * - 无 root 时明确 denied，由上层回退 NORMAL
 */
class RootExecutor : CommandExecutor {

    override fun exec(command: String): ExecResult {
        if (!RootUtils.hasSu()) {
            return ExecResult(ok = false, stderr = "未检测到 su，Root 通道不可用", denied = true)
        }
        return try {
            val su = locateSu() ?: return ExecResult(ok = false, stderr = "找不到 su 二进制", denied = true)
            val pb = ProcessBuilder(su, "-c", command)
            pb.redirectErrorStream(false)
            val p = pb.start()
            val stdout = p.inputStream.bufferedReader().readText()
            val stderr = p.errorStream.bufferedReader().readText()
            val finished = p.waitFor(10, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                ExecResult(ok = false, stdout = stdout.take(64_000), stderr = "su 执行超时", denied = false)
            } else {
                ExecResult(ok = p.exitValue() == 0, stdout = stdout.take(64_000), stderr = stderr.take(16_000), exitCode = p.exitValue())
            }
        } catch (e: Exception) {
            ExecResult(ok = false, stderr = "su 执行失败: ${e.message}", denied = false)
        }
    }

    override suspend fun execAsync(command: String): ExecResult = withContext(Dispatchers.IO) {
        exec(command)
    }

    private fun locateSu(): String? {
        val candidates = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/data/adb/magisk/busybox" // magisk su 代理
        )
        return candidates.firstOrNull { File(it).canExecute() } ?: "/system/bin/su"
    }
}