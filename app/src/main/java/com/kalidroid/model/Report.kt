package com.kalidroid.model

data class Report(
    val title: String,
    val content: String,
    val format: String = "markdown",
    val timestamp: Long = System.currentTimeMillis()
)
