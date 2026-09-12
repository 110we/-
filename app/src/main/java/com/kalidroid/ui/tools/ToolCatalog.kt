package com.kalidroid.ui.tools

import com.kalidroid.model.Tool

object ToolCatalog {
    val items: List<Tool> = listOf(
        Tool("系统信息", "主机", "读取内核与架构。命令模板：uname", "uname"),
        Tool("当前用户", "主机", "显示当前用户。命令模板：whoami", "whoami"),
        Tool("用户标识", "主机", "显示 uid/gid。命令模板：id", "id"),
        Tool("主机名", "主机", "读取主机名。命令模板：hostname", "hostname"),
        Tool("运行时长", "主机", "系统 uptime。命令模板：uptime", "uptime"),
        Tool("当前目录", "文件", "打印工作目录。命令模板：pwd", "pwd"),
        Tool("文件列表", "文件", "列出工作区。命令模板：ls", "ls"),
        Tool("磁盘用量", "存储", "根分区只读容量。命令模板：df", "df"),
        Tool("内存摘要", "主机", "内存占用。命令模板：free", "free"),
        Tool("系统时间", "开发", "当前时间。命令模板：date", "date"),
        // ---------- 容器工具链（KaliContainer / proot） ----------
        Tool("容器状态", "容器", "查看 Kali 容器运行状态与 PID。命令模板：kali status", "kali status"),
        Tool("启动容器", "容器", "启动 Kali 容器（proot）。命令模板：kali start", "kali start"),
        Tool("停止容器", "容器", "停止 Kali 容器。命令模板：kali stop", "kali stop"),
        Tool("容器执行", "容器", "在容器内执行命令。命令模板：kali exec <cmd>", "kali exec"),
        Tool("Rootfs 状态", "容器", "检查 rootfs 是否就绪。命令模板：kali rootfs", "kali rootfs"),
        Tool("在线下载 Rootfs", "容器", "从 AnLinux 镜像下载并安装 Kali rootfs（约 85MB）。命令模板：kali download-rootfs", "kali download-rootfs"),
        Tool("安装 Proot", "容器", "释放 proot 二进制（需 assets 内置）。命令模板：kali proot", "kali proot"),
        // ---------- 工具链（容器内可用命令） ----------
        Tool("容器 Shell", "工具链", "进入容器内交互 shell（proot）。命令模板：kali shell", "kali shell"),
        Tool("Nmap 扫描", "工具链", "容器内网络扫描（需 rootfs 内安装）。命令模板：nmap -sP", "nmap -sP"),
        Tool("Hydra 爆破", "工具链", "容器内口令测试（需 rootfs 内安装）。命令模板：hydra", "hydra"),
        Tool("Metasploit", "工具链", "容器内 MSF 框架（需 rootfs 内安装）。命令模板：msfconsole", "msfconsole"),
        Tool("SQLMap", "工具链", "容器内注入测试（需 rootfs 内安装）。命令模板：sqlmap", "sqlmap"),
        Tool("Wireshark", "工具链", "容器内抓包分析（需 rootfs 内安装）。命令模板：tshark", "tshark"),
        Tool("Nessus", "工具链", "容器内漏洞扫描（需 rootfs 内安装）。命令模板：nessus", "nessus"),
        Tool("Burp Suite", "工具链", "容器内 Web 代理（需 rootfs 内安装）。命令模板：burpsuite", "burpsuite")
    )

    fun find(name: String): Tool? = items.find { it.name == name }
    fun categories(): List<String> = listOf("全部") + items.map { it.category }.distinct()
}
