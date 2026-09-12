package com.kalidroid.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import rikka.shizuku.Shizuku

/**
 * Shizuku 授权工具（v2.1）。
 *
 * 策略：
 * - 依赖 rikka Shizuku API（已加入 build.gradle）
 * - 检测 Shizuku 是否运行 / 已授权
 * - 未授权时自动发起授权请求（requestPermission）
 * - 提供监听授权结果的能力
 */
object ShizukuUtils {

    private const val SHIZUKU_PKG = "moe.shizuku.privileged.api"

    fun isRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PKG, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) {
        false
    }

    /** 自动发起 Shizuku 授权请求（回调 onResult 返回是否同意） */
    fun requestPermission(onResult: ((Boolean) -> Unit)? = null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Shizuku 13.x: addRequestPermissionResultListener + requestPermission(int)
                val listener = object : Shizuku.OnRequestPermissionResultListener {
                    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                        onResult?.invoke(grantResult == PackageManager.PERMISSION_GRANTED)
                        runCatching { Shizuku.removeRequestPermissionResultListener(this) }
                    }
                }
                Shizuku.addRequestPermissionResultListener(listener)
                Shizuku.requestPermission(CODE_REQUEST_PERMISSION)
            }
        } catch (e: Throwable) {
            onResult?.invoke(false)
        }
    }

    private const val CODE_REQUEST_PERMISSION = 10086

    /** 跳转 Shizuku 应用（当用户需要手动启动/授权时） */
    fun openShizuku(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PKG)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}