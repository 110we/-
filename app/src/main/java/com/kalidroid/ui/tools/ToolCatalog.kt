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
        Tool("系统时间", "开发", "当前时间。命令模板：date", "date")
    )

    fun find(name: String): Tool? = items.find { it.name == name }
    fun categories(): List<String> = listOf("全部") + items.map { it.category }.distinct()
}
