package com.kalidroid.workflow

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kalidroid.model.Workflow

class WorkflowParser(
    private val gson: Gson = Gson()
) {
    fun parse(json: String): Workflow {
        val obj = gson.fromJson(json, JsonObject::class.java)
        val nodes = obj.getAsJsonArray("nodes")?.map { el ->
            val n = el.asJsonObject
            val args = mutableMapOf<String, String>()
            n.getAsJsonObject("args")?.entrySet()?.forEach { (k, v) ->
                args[k] = v.asString
            }
            val depends = n.getAsJsonArray("depends")?.map { it.asString }.orEmpty()
            WorkflowNode(
                id = n.get("id")?.asString ?: "",
                tool = n.get("tool")?.asString ?: "",
                args = args,
                depends = depends,
                label = n.get("label")?.asString ?: n.get("tool")?.asString.orEmpty()
            )
        }.orEmpty()
        return Workflow(
            id = obj.get("id")?.asString ?: "wf-unknown",
            name = obj.get("name")?.asString ?: "未命名",
            nodes = nodes,
            status = obj.get("status")?.asString ?: "idle",
            description = obj.get("description")?.asString.orEmpty()
        )
    }
}
