package com.kalidroid.utils

import java.io.File

object RootUtils {
    fun hasSu(): Boolean {
        val paths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su")
        return paths.any { File(it).exists() }
    }
}
