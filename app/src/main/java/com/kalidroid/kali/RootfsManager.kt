package com.kalidroid.kali

import android.content.Context
import com.kalidroid.KaliDroidApp
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

data class RootfsStatus(
    val present: Boolean,
    val message: String,
    val path: String = "",
    val version: String = ""
)

/**
 * Rootfs 管理器（v2.1 真实现）。
 * 解压使用内部流式 Tar 解析器，不依赖系统 tar/busybox。
 * 支持 .tar / .tar.gz / .tgz。
 */
class RootfsManager(private val context: Context = KaliDroidApp.instance) {

    private val rootfsDir: File get() = File(context.filesDir, "kali-rootfs")
    private val stagingBase: File get() = File(context.cacheDir, "rootfs-staging")

    @Synchronized
    fun extract(archivePath: String): RootfsStatus {
        val archive = File(archivePath)
        if (!archive.isFile) return RootfsStatus(false, "归档不存在: $archivePath")
        return try {
            if (rootfsDir.exists()) rootfsDir.deleteRecursively()
            rootfsDir.mkdirs()
            val staging = File(stagingBase, System.currentTimeMillis().toString()).apply { mkdirs() }
            val ok = TarExtractor.extract(archive, staging)
            if (!ok) {
                staging.deleteRecursively()
                return RootfsStatus(false, "解压失败：归档损坏或不支持")
            }
            val top = singleTopDir(staging) ?: staging
            moveContents(top, rootfsDir)
            staging.deleteRecursively()
            verify()
        } catch (e: Exception) {
            RootfsStatus(false, "导入异常: ${e.message}")
        }
    }

    fun verify(): RootfsStatus {
        val osRelease = File(rootfsDir, "etc/os-release")
        return if (osRelease.isFile) {
            val text = osRelease.readText()
            val pretty = Regex("""PRETTY_NAME=["']?([^"'\n]+)""").find(text)?.groupValues?.get(1) ?: "unknown"
            RootfsStatus(true, "rootfs 就绪 ($pretty)", rootfsDir.absolutePath, pretty)
        } else {
            RootfsStatus(false, "缺少 etc/os-release，rootfs 不完整", rootfsDir.absolutePath)
        }
    }

    fun update(archivePath: String): RootfsStatus = extract(archivePath)

    fun status(): RootfsStatus = verify()

    fun rootfs(): File = rootfsDir

    private fun singleTopDir(dir: File): File? {
        val entries = dir.listFiles()?.filter { it.name != "__MACOSX" } ?: return null
        return if (entries.size == 1 && entries[0].isDirectory) entries[0] else null
    }

    private fun moveContents(src: File, dst: File) {
        src.listFiles()?.forEach { f ->
            val target = File(dst, f.name)
            if (!f.renameTo(target)) {
                if (f.isDirectory) {
                    target.mkdirs()
                    moveContents(f, target)
                    f.deleteRecursively()
                } else {
                    f.copyTo(target, overwrite = true)
                    f.delete()
                }
            }
        }
    }

    object TarExtractor {
        private const val BLOCK = 512

        fun extract(archive: File, target: File): Boolean {
            val input = openInput(archive) ?: return false
            return try {
                input.use { raw ->
                    val header = ByteArray(BLOCK)
                    while (true) {
                        val read = readFully(raw, header)
                        if (read == 0) return true
                        if (read < BLOCK) return false
                        if (header.all { it == 0.toByte() }) continue
                        val name = str(header, 0, 100)
                        val size = oct(header, 124, 12) ?: return false
                        val type = header[156].toInt().toChar()
                        if (name.isBlank() || name == "./") { skip(raw, size); continue }
                        val out = File(target, sanitize(name))
                        when (type) {
                            '5' -> out.mkdirs()
                            '2' -> skip(raw, size)
                            else -> {
                                out.parentFile?.mkdirs()
                                FileOutputStream(out).use { fos ->
                                    var remaining = size
                                    val buf = ByteArray(64 * 1024)
                                    while (remaining > 0) {
                                        val n = raw.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                                        if (n <= 0) return false
                                        fos.write(buf, 0, n)
                                        remaining -= n
                                    }
                                }
                            }
                        }
                        skip(raw, (BLOCK - (size % BLOCK)) % BLOCK)
                    }
                }
            } catch (e: Exception) {
                false
            }
        }

        private fun openInput(archive: File): InputStream? = try {
            val head = ByteArray(2)
            FileInputStream(archive).use { raw ->
                if (raw.read(head) != 2) return null
            }
            val fileInput = BufferedInputStream(FileInputStream(archive), 1 shl 16)
            if (head[0] == 0x1f.toByte() && head[1] == 0x8b.toByte()) GZIPInputStream(fileInput)
            else if (archive.name.endsWith(".tar")) fileInput
            else { fileInput.close(); null }
        } catch (e: Exception) { null }

        private fun sanitize(name: String): String {
            val clean = name.removePrefix("./").removePrefix("/")
            val parts = clean.split('/').filter { it.isNotEmpty() && it != "." && it != ".." }
            return parts.joinToString("/")
        }

        private fun readFully(input: InputStream, buf: ByteArray): Int {
            var total = 0
            while (total < buf.size) {
                val n = input.read(buf, total, buf.size - total)
                if (n < 0) break
                total += n
            }
            return total
        }

        private fun skip(input: InputStream, bytes: Long) {
            var remaining = bytes
            val buf = ByteArray(4096)
            while (remaining > 0) {
                val n = input.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                if (n <= 0) return
                remaining -= n
            }
        }

        private fun str(buf: ByteArray, off: Int, len: Int): String {
            val sb = StringBuilder()
            for (i in off until off + len) {
                val c = buf[i].toInt().toChar()
                if (c == '\u0000') break
                sb.append(c)
            }
            return sb.toString()
        }

        private fun oct(buf: ByteArray, off: Int, len: Int): Long? = try {
            var v = 0L
            var started = false
            for (i in off until off + len) {
                val c = buf[i].toInt().toChar()
                when {
                    c == ' ' || c == '\u0000' -> if (started) break else continue
                    c in '0'..'7' -> { v = v * 8 + (c - '0'); started = true }
                    else -> return null
                }
            }
            v
        } catch (e: Exception) { null }
    }
}