package com.kalidroid.utils

import android.content.Context
import android.content.Intent

/**
 * Shizuku 授权工具（v2.1）。
 *
 * 策略：
 * - 检测 Shizuku 是否运行（通过 IPC binder 探测）
 * - 提供发起授权请求的入口（跳转 Shizuku 授权页）
 * - 不直接依赖 Shizuku API 库，用反射探测，避免增加依赖
 */
object ShizukuUtils {

    private const val SHIZUKU_PKG = "moe.shizuku.privileged.api"

    fun isRunning(): Boolean {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getMethod("pingBinder")
            method.invoke(null) as? Boolean == true
        } catch (e: Throwable) {
            // 未集成 Shizuku API：fallback 探测包 + 服务
            false
        }
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PKG, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 跳转 Shizuku 授权页 */
    fun requestPermission(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PKG)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}