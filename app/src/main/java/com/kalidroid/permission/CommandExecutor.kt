package com.kalidroid.permission

interface CommandExecutor {
    fun exec(command: String): ExecResult
    suspend fun execAsync(command: String): ExecResult
}

data class ExecResult(
    val ok: Boolean,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = -1,
    val denied: Boolean = false
)
