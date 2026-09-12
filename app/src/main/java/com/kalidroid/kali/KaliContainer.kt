package com.kalidroid.kali

import android.content.Context
import com.kalidroid.KaliDroidApp
import java.io.File
import java.util.concurrent.TimeUnit

data class ContainerState(
    val running: Boolean,
    val message: String,
    val pid: Int = -1
)

/**
 * Kali 容器（v2.1 proot 引擎）。
 *
 * 架构：
 * - proot 静态二进制从 assets/proot 释放到 filesDir/bin/proot
 * - 启动: proot -0 -r rootfs -b /proc -b /dev -b /sys -b /sdcard /bin/sh
 * - 非 root 环境用 proot 虚拟化，无需真实 chroot/mount
 */
class KaliContainer(
    private val context: Context = KaliDroidApp.instance,
    private val rootfsManager: RootfsManager = RootfsManager(context),
    private val mountManager: MountManager = MountManager()
) {
    private val binDir: File get() = File(context.filesDir, "bin")
    private val prootFile: File get() = File(binDir, "proot")
    private var process: Process? = null
    private val lock = Object()

    enum class State { EXITED, STARTING, RUNNING, STOPPING, FAILED, MISSING_ROOTFS, MISSING_PROOT }

    @Volatile
    var state: State = State.EXITED
        private set

    // ---------- proot 安装 ----------

    /** 从 assets/proot 释放 proot 静态二进制 */
    fun installProot(stream: java.io.InputStream): Result<Boolean> = runCatching {
        binDir.mkdirs()
        val tmp = File(binDir, "proot.tmp")
        stream.use { input ->
            tmp.outputStream().use { out -> input.copyTo(out) }
        }
        if (!tmp.setExecutable(true)) throw IllegalStateException("无法设置 proot 可执行权限")
        if (prootFile.exists()) prootFile.delete()
        if (!tmp.renameTo(prootFile)) {
            tmp.copyTo(prootFile, overwrite = true)
            tmp.delete()
        }
        prootFile.setExecutable(true)
        prootFile.length() > 0
    }

    fun prootInstalled(): Boolean = prootFile.isFile && prootFile.canExecute()

    /** rootfs 状态（代理） */
    fun rootfsStatus(): RootfsStatus = rootfsManager.status()

    /** 在线下载并安装 rootfs（代理，返回状态） */
    fun downloadRootfs(onProgress: ((Float) -> Unit)? = null): RootfsStatus =
        rootfsManager.downloadAndInstall(onProgress = onProgress)

    // ---------- 启动 / 停止 ----------

    fun start(): ContainerState {
        synchronized(lock) {
            if (process?.isAlive == true) return ContainerState(true, "容器已在运行", pidOf(process!!))
            val rootfs = rootfsManager.verify()
            if (!rootfs.present) {
                state = State.MISSING_ROOTFS
                return ContainerState(false, "缺少 rootfs：请先导入 Kali rootfs 归档")
            }
            if (!prootInstalled()) {
                state = State.MISSING_PROOT
                return ContainerState(false, "缺少 proot：请先从 assets 安装 proot")
            }
            state = State.STARTING
            return try {
                val shell = shellOf(rootfsManager.rootfs()) ?: "/bin/sh"
                val cmd = buildCommand(rootfsManager.rootfs(), shell)
                val pb = ProcessBuilder(cmd)
                    .directory(rootfsManager.rootfs())
                    .redirectErrorStream(false)
                process = pb.start()
                state = State.RUNNING
                ContainerState(true, "Kali 容器运行中 (shell=$shell)", pidOf(process!!))
            } catch (e: Exception) {
                state = State.FAILED
                ContainerState(false, "启动失败: ${e.message}")
            }
        }
    }

    fun stop(): ContainerState {
        synchronized(lock) {
            process?.let { p ->
                if (p.isAlive) {
                    p.destroy()
                    if (!p.waitFor(3, TimeUnit.SECONDS)) p.destroyForcibly()
                }
            }
            process = null
            mountManager.unmountAll()
            state = State.EXITED
            return ContainerState(false, "容器已停止")
        }
    }

    fun status(): ContainerState {
        val alive = process?.isAlive == true
        if (!alive && state == State.RUNNING) state = State.EXITED
        val rootfs = rootfsManager.status()
        return ContainerState(
            running = alive,
            message = "state=${state.name}; rootfs=${rootfs.message}; proot=${if (prootInstalled()) "ok" else "missing"}",
            pid = pidOf(process)
        )
    }

    /**
     * 轻量状态（不读文件，适合 UI 高频刷新）。
     * 仅基于内存中的 state 枚举与进程存活状态。
     */
    fun statusLight(): ContainerState {
        val alive = process?.isAlive == true
        if (!alive && state == State.RUNNING) state = State.EXITED
        return ContainerState(
            running = alive,
            message = "state=${state.name}",
            pid = pidOf(process)
        )
    }

    // ---------- 执行 ----------

    /**
     * 在容器内执行命令。
     * 实现：每次执行起一个新的 proot 进程（开销可接受），
     * 命令通过 rootfs 内的 /bin/sh -c 运行，天然处于容器环境。
     */
    fun exec(command: String, timeoutMs: Long = 20_000, maxChars: Int = 65536): ContainerExecResult {
        if (command.isBlank()) return ContainerExecResult(ok = false, stderr = "空命令")
        val rootfs = rootfsManager.verify()
        if (!rootfs.present) return ContainerExecResult(ok = false, stderr = "缺少 rootfs：${rootfs.message}")
        if (!prootInstalled()) return ContainerExecResult(ok = false, stderr = "缺少 proot：请先安装 proot")
        return try {
            val cmd = buildCommand(rootfsManager.rootfs(), shellOf(rootfsManager.rootfs()) ?: "/bin/sh", "-c", command)
            val pb = ProcessBuilder(cmd)
                .directory(rootfsManager.rootfs())
                .redirectErrorStream(true) // 合并 stdout/stderr，避免双管道死锁
            val ep = pb.start()
            // 单独线程读输出，主线程 waitFor 带超时，防止管道填满互相等待
            val outputFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                readLimited(ep.inputStream, maxChars)
            }
            val finished = ep.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                ep.destroyForcibly()
                ContainerExecResult(ok = false, stderr = "执行超时(>${timeoutMs}ms)", timedOut = true)
            } else {
                val output = runCatching { outputFuture.get(2, TimeUnit.SECONDS) }.getOrDefault("")
                ContainerExecResult(ok = ep.exitValue() == 0, stdout = output, stderr = "", exitCode = ep.exitValue())
            }
        } catch (e: Exception) {
            ContainerExecResult(ok = false, stderr = "exec 异常: ${e.message}")
        }
    }

    // ---------- 内部 ----------

    private fun buildCommand(rootfs: File, shell: String, vararg shellArgs: String): List<String> {
        val args = mutableListOf(
            prootFile.absolutePath,
            "-0",                    // 伪装 root
            "-r", rootfs.absolutePath,
            "--kill-on-exit"
        )
        args += mountManager.prootBindArgs()
        args += shell
        args += shellArgs
        return args
    }

    private fun shellOf(rootfs: File): String? {
        val candidates = listOf("bin/bash", "bin/sh", "usr/bin/bash", "usr/bin/sh")
        return candidates.firstOrNull { File(rootfs, it).isFile }?.let { "/$it" }
    }

    private fun readLimited(stream: java.io.InputStream, max: Int): String {
        val bytes = stream.readBytes()
        val limited = if (bytes.size > max) bytes.copyOf(max) else bytes
        return String(limited, Charsets.UTF_8)
    }

    /** 兼容反射获取进程 pid（Android java.lang.Process 无公有 pid()）。 */
    private fun pidOf(p: Process?): Int {
        if (p == null) return -1
        return try {
            val m = p.javaClass.getMethod("pid")
            m.invoke(p) as? Int ?: -1
        } catch (e: Exception) {
            -1
        }
    }
}

data class ContainerExecResult(
    val ok: Boolean,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = -1,
    val timedOut: Boolean = false
)