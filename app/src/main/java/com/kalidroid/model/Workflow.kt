package com.kalidroid.model

import com.kalidroid.workflow.WorkflowNode

data class Workflow(
    val id: String,
    val name: String,
    val nodes: List<WorkflowNode>,
    val status: String = "idle",
    val description: String = ""
)
