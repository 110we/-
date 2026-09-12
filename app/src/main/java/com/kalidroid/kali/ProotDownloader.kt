package com.kalidroid.kali

import android.content.Context
import com.kalidroid.KaliDroidApp
import java.io.ByteArrayOutputStream
import java.io.File
import org.tukaani.xz.XZInputStream

/**
 * Proot 在线下载器。
 * 从 Termux 软件源拉取 proot 的 deb 包，解析出 proot 二进制与 libproot.so，
 * 释放到 filesDir/bin/ 供 KaliContainer 使用。
 */
object ProotDownloader {

    /**
     * 在线下载并安装 proot。
     * @param onProgress 下载进度回调 0..1
     * @return 成功返回 proot 文件路径，失败返回 null
     */
    fun install(context: Context = KaliDroidApp.instance, onProgress: ((Float) -> Unit)? = null, source: DownloadSource? = null): String? {
        val binDir = File(context.filesDir, "bin").apply { mkdirs() }
        val prootFile = File(binDir, "proot")
        val libFile = File(binDir, "libproot.so")

        // 已安装直接返回
        if (prootFile.isFile && prootFile.length() > 100_000) {
            return prootFile.absolutePath
        }

        // 使用用户选择的源；未指定则用注册表默认
        val mirror = source ?: DownloadSources.selectedProot()
        val packagesUrl = mirror.packagesUrl ?: return null
        val debBase = mirror.debBase ?: return null
        println("KaliDroid proot source: ${mirror.name} ($packagesUrl)")

        return try {
            onProgress?.invoke(0.05f)
            // 1. 拉取 Packages 索引，解析 proot 的 deb 下载地址
            val packages = httpGet(packagesUrl) ?: return null
            onProgress?.invoke(0.15f)
            val filename = parseFilename(packages, "proot") ?: return null
            val debUrl = debBase + filename
            println("KaliDroid proot deb: $debUrl")

            // 2. 下载 deb（带进度）
            val deb = httpGetBytes(debUrl) { p -> onProgress?.invoke(0.15f + p * 0.6f) } ?: return null
            if (deb.size < 100_000) return null
            onProgress?.invoke(0.8f)

            // 3. 解析 ar 归档，取出 data.tar.xz
            val dataTarXz = extractFromDeb(deb, "data.tar.xz") ?: return null
            onProgress?.invoke(0.85f)

            // 4. 流式解出 proot 与 libproot.so
            extractTarXz(dataTarXz, binDir, prootFile, libFile)
            onProgress?.invoke(0.95f)

            // 5. 赋可执行权限
            prootFile.setExecutable(true)
            if (!prootFile.isFile || prootFile.length() < 100_000) return null
            onProgress?.invoke(1f)
            prootFile.absolutePath
        } catch (e: Exception) {
            println("KaliDroid proot 下载失败: ${e.message}")
            null
        }
    }

    /** 从 Termux Packages 索引里解析指定包的 Filename 行 */
    private fun parseFilename(packages: String, pkg: String): String? {
        val blocks = packages.split("\n\n")
        for (block in blocks) {
            if (block.startsWith("Package: $pkg\n") || block.contains("\nPackage: $pkg\n")) {
                return block.lineSequence()
                    .firstOrNull { it.startsWith("Filename: ") }
                    ?.removePrefix("Filename: ")
                    ?.trim()
            }
        }
        return null
    }

    private fun httpGet(url: String): String? = runCatching {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        if (conn.responseCode !in 200..299) return null
        val s = conn.inputStream.readBytes().toString(Charsets.UTF_8)
        conn.disconnect()
        s
    }.getOrNull()

    private fun httpGetBytes(url: String, onProgress: ((Float) -> Unit)? = null): ByteArray? = runCatching {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000
        conn.instanceFollowRedirects = true
        if (conn.responseCode !in 200..299) return null
        val total = conn.contentLengthLong
        val input = conn.inputStream
        val bos = ByteArrayOutputStream()
        val buf = ByteArray(128 * 1024)
        var read: Int
        var done = 0L
        while (input.read(buf).also { read = it } != -1) {
            bos.write(buf, 0, read)
            done += read
            if (total > 0) onProgress?.invoke((done.toFloat() / total).coerceIn(0f, 1f))
        }
        val b = bos.toByteArray()
        input.close()
        conn.disconnect()
        b
    }.getOrNull()

    /**
     * 解析 ar 归档（deb 格式），按名字提取成员内容。
     * ar 头部固定 60 字节，内容按 2 字节对齐。
     */
    private fun extractFromDeb(deb: ByteArray, wantName: String): ByteArray? {
        var off = 8 // "!<arch>\n"
        while (off + 60 <= deb.size) {
            val name = String(deb, off, 16).trim().trimEnd('/')
            // 60 字节头：16 name + 12 mtime + 6 uid + 6 gid + 8 mode + 10 size + 2 magic
            val sizeStr = String(deb, off + 48, 10).trim()
            val size = sizeStr.toLongOrNull() ?: return null
            val dataStart = off + 60
            if (dataStart + size > deb.size) return null
            if (name == wantName) {
                return deb.copyOfRange(dataStart, (dataStart + size).toInt())
            }
            off = dataStart + size.toInt()
            if (off % 2 != 0) off++ // ar 成员按 2 字节对齐
        }
        return null
    }

    /**
     * 流式解压 tar.xz，提取 proot 与 libproot.so。
     * 兼容 Termux 路径（./data/data/com.termux/files/usr/bin/proot）。
     */
    private fun extractTarXz(data: ByteArray, binDir: File, prootFile: File, libFile: File) {
        XZInputStream(data.inputStream()).use { input ->
            val header = ByteArray(512)
            while (true) {
                val read = readFully(input, header)
                if (read == 0) break
                if (read < 512) break
                if (header.all { it == 0.toByte() }) break
                val name = str(header, 0, 100)
                val size = oct(header, 124, 12) ?: break
                val type = header[156].toInt().toChar()
                val base = name.removePrefix("./")
                when {
                    base.endsWith("usr/bin/proot") && type != '5' -> {
                        copyEntry(input, size, prootFile)
                    }
                    base.endsWith("usr/lib/libproot.so") && type != '5' -> {
                        copyEntry(input, size, libFile)
                    }
                    else -> skip(input, size)
                }
                skip(input, (512 - (size % 512)) % 512)
            }
        }
    }

    private fun copyEntry(input: java.io.InputStream, size: Long, target: File) {
        target.parentFile?.mkdirs()
        target.outputStream().use { out ->
            var remaining = size
            val buf = ByteArray(64 * 1024)
            while (remaining > 0) {
                val n = input.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                if (n <= 0) break
                out.write(buf, 0, n)
                remaining -= n
            }
        }
    }

    private fun skip(input: java.io.InputStream, bytes: Long) {
        var remaining = bytes
        val buf = ByteArray(4096)
        while (remaining > 0) {
            val n = input.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
            if (n <= 0) return
            remaining -= n
        }
    }

    private fun readFully(input: java.io.InputStream, buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val n = input.read(buf, total, buf.size - total)
            if (n < 0) break
            total += n
        }
        return total
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

    private fun oct(buf: ByteArray, off: Int, len: Int): Long? {
            return try {
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