package com.dynamicframe.domain.repository

import com.dynamicframe.domain.debug.DebugLevel
import com.dynamicframe.domain.debug.DebugLogEntry
import kotlinx.coroutines.flow.StateFlow

/** Registro central de depuración (implementación en `data/debug/`). */
interface AppDebugLogger {
    val enabled: StateFlow<Boolean>
    val logs: StateFlow<List<DebugLogEntry>>

    suspend fun load()
    suspend fun setEnabled(enabled: Boolean)
    fun clear()

    fun log(level: DebugLevel, tag: String, message: String, detail: String? = null)

    fun v(tag: String, message: String) = log(DebugLevel.VERBOSE, tag, message)
    fun d(tag: String, message: String) = log(DebugLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(DebugLevel.INFO, tag, message)
    fun w(tag: String, message: String, detail: String? = null) = log(DebugLevel.WARN, tag, message, detail)
    fun e(tag: String, message: String, detail: String? = null) = log(DebugLevel.ERROR, tag, message, detail)

    fun exportText(): String
}
