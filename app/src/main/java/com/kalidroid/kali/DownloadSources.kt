package com.kalidroid.kali

import android.content.Context
import com.kalidroid.KaliDroidApp

/**
 * 下载源注册表 + 选择持久化。
 * 所有可用的 proot / rootfs 源都登记在这里，用户可在设置中自选。
 */
data class DownloadSource(
    val id: String,
    val name: String,
    val desc: String,
    val packagesUrl: String? = null,  // proot 源：Packages 索引地址
    val debBase: String? = null,      // proot 源：deb 下载基址
    val rootfsUrl: String? = null     // rootfs 源：tar.xz 直链
)

object DownloadSources {

    // ---------- proot 源（Termux 镜像，binary-aarch64） ----------
    val prootSources: List<DownloadSource> = listOf(
        DownloadSource(
            "tuna", "清华 TUNA", "国内直连 · 推荐",
            packagesUrl = "https://mirrors.tuna.tsinghua.edu.cn/termux/apt/termux-main/dists/stable/main/binary-aarch64/Packages",
            debBase = "https://mirrors.tuna.tsinghua.edu.cn/termux/apt/termux-main/"
        ),
        DownloadSource(
            "ustc", "中科大 USTC", "国内直连",
            packagesUrl = "https://mirrors.ustc.edu.cn/termux/apt/termux-main/dists/stable/main/binary-aarch64/Packages",
            debBase = "https://mirrors.ustc.edu.cn/termux/apt/termux-main/"
        ),
        DownloadSource(
            "tencent", "腾讯云", "国内直连",
            packagesUrl = "https://mirrors.cloud.tencent.com/termux/apt/termux-main/dists/stable/main/binary-aarch64/Packages",
            debBase = "https://mirrors.cloud.tencent.com/termux/apt/termux-main/"
        ),
        DownloadSource(
            "official", "Termux 官方", "海外官方源",
            packagesUrl = "https://packages.termux.dev/apt/termux-main/dists/stable/main/binary-aarch64/Packages",
            debBase = "https://packages.termux.dev/apt/termux-main/"
        )
    )

    // ---------- rootfs 源（Kali rootfs 镜像，key = arch） ----------
    private val rootfsUrlByArch = mapOf(
        "arm64" to mapOf(
            "kali-cdn" to "https://kali.download/nethunter-images/kali-2026.2/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz",
            "kali-cdn-nano" to "https://kali.download/nethunter-images/kali-2026.2/rootfs/kali-nethunter-rootfs-nano-arm64.tar.xz",
            "github" to "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Kali/arm64/kali-rootfs-arm64.tar.xz",
            "ghfast" to "https://ghfast.top/https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Kali/arm64/kali-rootfs-arm64.tar.xz"
        ),
        "arm" to mapOf(
            "kali-cdn" to "https://kali.download/nethunter-images/kali-2026.2/rootfs/kali-nethunter-rootfs-minimal-armhf.tar.xz",
            "github" to "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Kali/armhf/kali-rootfs-armhf.tar.xz"
        ),
        "x86_64" to mapOf(
            "kali-cdn" to "https://kali.download/nethunter-images/kali-2026.2/rootfs/kali-nethunter-rootfs-minimal-amd64.tar.xz",
            "github" to "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Kali/amd64/kali-rootfs-amd64.tar.xz"
        )
    )

    /** 某个架构可选的 rootfs 源列表 */
    fun rootfsSources(arch: String = "arm64"): List<DownloadSource> {
        val urls = rootfsUrlByArch[arch] ?: return emptyList()
        val names = mapOf(
            "kali-cdn" to "Kali 官方 CDN", "kali-cdn-nano" to "Kali CDN·nano 精简",
            "github" to "GitHub 原始", "ghfast" to "ghfast 加速"
        )
        val descs = mapOf(
            "kali-cdn" to "国内可直连 · 推荐", "kali-cdn-nano" to "更小体积（约100MB）",
            "github" to "海外直连", "ghfast" to "GitHub 代理加速"
        )
        return urls.map { (id, url) ->
            DownloadSource(id, names[id] ?: id, descs[id] ?: "", rootfsUrl = url)
        }
    }

    // ---------- 持久化 ----------
    private fun prefs(): android.content.SharedPreferences =
        KaliDroidApp.instance.getSharedPreferences("download_sources", Context.MODE_PRIVATE)

    fun getProotId(): String = prefs().getString("proot_source", prootSources.first().id) ?: prootSources.first().id

    fun setProotId(id: String) {
        prefs().edit().putString("proot_source", id).apply()
    }

    fun getRootfsId(arch: String = "arm64"): String =
        prefs().getString("rootfs_source_$arch", rootfsSources(arch).firstOrNull()?.id ?: "kali-cdn")
            ?: rootfsSources(arch).firstOrNull()?.id ?: "kali-cdn"

    fun setRootfsId(arch: String, id: String) {
        prefs().edit().putString("rootfs_source_$arch", id).apply()
    }

    /** 当前选的 proot 源 */
    fun selectedProot(): DownloadSource =
        prootSources.firstOrNull { it.id == getProotId() } ?: prootSources.first()

    /** 当前选的 rootfs 源 */
    fun selectedRootfs(arch: String = "arm64"): DownloadSource? =
        rootfsSources(arch).firstOrNull { it.id == getRootfsId(arch) } ?: rootfsSources(arch).firstOrNull()
}