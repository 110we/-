package com.kalidroid.kali

data class MountStatus(
    val mounted: Boolean,
    val message: String
)

/**
 * MountManager（v2.1 proot 适配版）。
 *
 * 容器走 proot 模式：/proc /dev /sys 由 proot 的 -b 参数虚拟映射，
 * 非 root 环境无法真实 mount，故此处返回"由 proot 管理"。
 * 若启用 root 通道 + chroot 模式，可在此扩展真实挂载。
 */
class MountManager {

    /** proot 模式下挂载点由 -b 参数管理，无需真实 mount */
    fun mountProc(): MountStatus = MountStatus(true, "/proc 由 proot 虚拟映射（-b /proc）")

    fun mountDev(): MountStatus = MountStatus(true, "/dev 由 proot 虚拟映射（-b /dev）")

    fun mountSys(): MountStatus = MountStatus(true, "/sys 由 proot 虚拟映射（-b /sys）")

    /** proot 进程退出即自动卸载，无需显式 umount */
    fun unmountAll(): MountStatus = MountStatus(true, "proot 进程退出，虚拟挂载已随进程释放。")

    fun status(): MountStatus = MountStatus(true, "/proc /dev /sys 由 proot 虚拟映射管理")

    /** 返回 proot 的 -b 绑定参数列表 */
    fun prootBindArgs(): List<String> = listOf(
        "-b", "/proc",
        "-b", "/dev",
        "-b", "/sys",
        "-b", "/sdcard"
    )
}