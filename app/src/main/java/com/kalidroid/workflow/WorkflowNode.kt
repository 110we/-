package com.kalidroid.workflow

data class WorkflowNode(
    val id: String,
    val tool: String,
    val args: Map<String, String> = emptyMap(),
    val depends: List<String> = emptyList(),
    val label: String = tool
)
