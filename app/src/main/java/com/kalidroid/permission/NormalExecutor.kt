package com.kalidroid.permission

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NormalExecutor : CommandExecutor {
    private val allowlist = setOf(
        "date", "uname", "hostname", "whoami", "id", "uptime", "pwd", "df", "free", "ls", "echo"
    )

    override fun exec(command: String): ExecResult {
        val token = command.trim().split("\\s+".toRegex()).firstOrNull().orEmpty()
        if (token !in allowlist) {
            return ExecResult(
                ok = false,
                stderr = "命令不在白名单: $token。允许: ${allowlist.joinToString(" ")}",
                denied = true
            )
        }
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(token))
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val code = process.waitFor()
            ExecResult(ok = code == 0, stdout = stdout, stderr = stderr, exitCode = code)
        } catch (e: Exception) {
            ExecResult(ok = false, stderr = e.message ?: "exec failed")
        }
    }

    override suspend fun execAsync(command: String): ExecResult = withContext(Dispatchers.IO) {
        exec(command)
    }
}
