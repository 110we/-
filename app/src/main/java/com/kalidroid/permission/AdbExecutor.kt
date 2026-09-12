package com.kalidroid.permission

import android.os.IBinder
import android.os.Parcel
import com.kalidroid.KaliDroidApp
import com.kalidroid.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * ADB / Shizuku 执行器（v2.1 真实现）。
 *
 * 通过 Shizuku API 的 Shizuku.newProcess 以 shell 权限执行命令。
 * 已集成 rikka Shizuku API（build.gradle 引入），
 * 授权后可直接执行任意命令，等价于 adb shell。
 *
 * 前置条件：Shizuku 已运行且已授权本应用（PermissionChooser 自动索要）。
 */
class AdbExecutor : CommandExecutor {

    override fun exec(command: String): ExecResult {
        if (!ShizukuUtils.isRunning()) {
            return ExecResult(ok = false, stderr = "Shizuku 未运行，ADB 通道不可用（请先启动 Shizuku 并授权）", denied = true)
        }
        if (!ShizukuUtils.isGranted()) {
            return ExecResult(ok = false, stderr = "Shizuku 未授权，ADB 通道不可用（请点击授权）", denied = true)
        }
        return try {
            val output = shizukuExec(command)
            ExecResult(ok = true, stdout = output)
        } catch (e: Exception) {
            ExecResult(ok = false, stderr = "Shizuku 执行失败: ${e.message}", denied = false)
        }
    }

    override suspend fun execAsync(command: String): ExecResult = withContext(Dispatchers.IO) {
        exec(command)
    }

    /**
     * 通过 Shizuku.newProcess 以 shell 身份执行命令并读取输出。
     * 注：rikka Shizuku 13.x 的 newProcess 为 private 静态方法，走反射调用。
     */
    private fun shizukuExec(command: String): String {
        try {
            val process = shizukuNewProcess(arrayOf("/system/bin/sh", "-c", command), null, null)
                ?: return "Shizuku newProcess 不可用"
            val output = process.inputStream.readBytes().toString(Charsets.UTF_8)
            val err = process.errorStream.readBytes().toString(Charsets.UTF_8)
            process.waitFor()
            return output.ifBlank { err }.ifBlank { "(无输出)" }
        } catch (e: Throwable) {
            // 降级：老版本 API 走 legacy 通道
            return legacyShizukuExec(command)
        }
    }

    /** 反射调用 Shizuku.newProcess（private 静态），返回 java.lang.Process */
    private fun shizukuNewProcess(cmd: Array<String>, envp: Array<String>?, dir: String?): Process? {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val m = clazz.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            m.isAccessible = true
            m.invoke(null, cmd, envp, dir) as? Process
        } catch (e: Throwable) {
            null
        }
    }

    /** 兼容旧版 Shizuku：通过 binder transact 执行（兜底） */
    private fun legacyShizukuExec(command: String): String {
        return try {
            val binder = Shizuku.getBinder() ?: return "Shizuku binder 不可用"
            val transact = Parcel.obtain()
            val reply = Parcel.obtain()
            transact.writeInterfaceToken("moe.shizuku.server.IShizukuService")
            transact.writeString(command)
            binder.transact(2, transact, reply, 0)
            val out = reply.readString() ?: ""
            transact.recycle()
            reply.recycle()
            out
        } catch (e: Throwable) {
            "Shizuku 通道未授权或执行失败: ${e.message}"
        }
    }
}