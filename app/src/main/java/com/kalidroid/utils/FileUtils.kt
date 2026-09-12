package com.kalidroid.utils

import android.content.Context
import java.io.File

object FileUtils {
    fun appDir(context: Context): File = context.filesDir

    fun read(context: Context, name: String): String {
        val file = File(appDir(context), name)
        return if (file.exists()) file.readText() else ""
    }

    fun write(context: Context, name: String, content: String) {
        File(appDir(context), name).writeText(content)
    }

    fun list(context: Context): List<String> {
        return appDir(context).list()?.toList().orEmpty()
    }
}
