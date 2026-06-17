package com.dynamicframe.domain.debug

enum class DebugLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

data class DebugLogEntry(
    val id: Long,
    val timestampMs: Long,
    val level: DebugLevel,
    val tag: String,
    val message: String,
    val detail: String? = null
)
