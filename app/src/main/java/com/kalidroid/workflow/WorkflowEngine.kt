package com.kalidroid.workflow

import com.kalidroid.model.Workflow
import com.kalidroid.permission.CommandExecutor
import com.kalidroid.permission.ExecResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NodeLog(
    val nodeId: String,
    val tool: String,
    val ok: Boolean,
    val output: String
)

class WorkflowEngine(
    private val executor: CommandExecutor,
    private val parser: WorkflowParser = WorkflowParser()
) {
    private val allowTools = setOf(
        "system.info",
        "files.list",
        "hash.file",
        "terminal.run",
        "report.generate"
    )

    fun parse(json: String): Workflow = parser.parse(json)

    suspend fun execute(workflow: Workflow): List<NodeLog> = withContext(Dispatchers.Default) {
        val done = mutableSetOf<String>()
        val logs = mutableListOf<NodeLog>()
        val pending = workflow.nodes.toMutableList()
        var guard = 0
        while (pending.isNotEmpty() && guard < 32) {
            guard += 1
            val ready = pending.filter { node -> node.depends.all { it in done } }
            if (ready.isEmpty()) {
                logs += NodeLog("", "engine", false, "存在循环依赖或缺失依赖")
                break
            }
            for (node in ready) {
                logs += runNode(node)
                done += node.id
                pending.remove(node)
            }
        }
        logs
    }

    private suspend fun runNode(node: WorkflowNode): NodeLog {
        if (node.tool !in allowTools) {
            return NodeLog(node.id, node.tool, false, "节点类型不在白名单: ${node.tool}")
        }
        val command = node.args["command"] ?: when (node.tool) {
            "system.info" -> "uname"
            "files.list" -> "ls"
            "terminal.run" -> "date"
            else -> "echo"
        }
        val result: ExecResult = executor.execAsync(command)
        return NodeLog(node.id, node.tool, result.ok && !result.denied, result.stdout.ifBlank { result.stderr })
    }
}
