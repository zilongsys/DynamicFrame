package com.dynamicframe.data.debug

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.dynamicframe.BuildConfig
import com.dynamicframe.data.local.SlideshowPreferencesKeys
import com.dynamicframe.domain.debug.DebugLevel
import com.dynamicframe.domain.debug.DebugLogEntry
import com.dynamicframe.domain.repository.AppDebugLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.debugPrefsDataStore by preferencesDataStore(
    name = SlideshowPreferencesKeys.DATASTORE_FILE
)

private val DEBUG_MODE = booleanPreferencesKey("debug_mode")

@Singleton
class AndroidAppDebugLogger @Inject constructor(
    @ApplicationContext private val context: Context
) : AppDebugLogger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _enabled = MutableStateFlow(BuildConfig.DEBUG_TOOLS_DEFAULT)
    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    override val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()

    private var nextId = 0L

    override suspend fun load() {
        val stored = runCatching {
            context.debugPrefsDataStore.data.first()[DEBUG_MODE]
        }.getOrNull()
        _enabled.value = stored ?: BuildConfig.DEBUG_TOOLS_DEFAULT
        if (_enabled.value) {
            i("Debug", "Modo depuración activo (build=${BuildConfig.BUILD_TYPE}, v${BuildConfig.VERSION_NAME})")
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        context.debugPrefsDataStore.edit { prefs ->
            prefs[DEBUG_MODE] = enabled
        }
        i("Debug", if (enabled) "Modo depuración activado" else "Modo depuración desactivado")
        if (!enabled) clear()
    }

    override fun clear() {
        scope.launch {
            lock.withLock { _logs.value = emptyList() }
        }
    }

    override fun log(level: DebugLevel, tag: String, message: String, detail: String?) {
        if (!_enabled.value) return

        val entry = DebugLogEntry(
            id = ++nextId,
            timestampMs = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            detail = detail
        )

        val line = buildString {
            append("[${timeFormat.format(Date(entry.timestampMs))}] ")
            append("${level.name}/$tag: $message")
            detail?.let { append(" | $it") }
        }

        when (level) {
            DebugLevel.VERBOSE -> Log.v(LOG_TAG, line)
            DebugLevel.DEBUG -> Log.d(LOG_TAG, line)
            DebugLevel.INFO -> Log.i(LOG_TAG, line)
            DebugLevel.WARN -> Log.w(LOG_TAG, line)
            DebugLevel.ERROR -> Log.e(LOG_TAG, line)
        }

        scope.launch {
            lock.withLock {
                _logs.value = (listOf(entry) + _logs.value).take(MAX_LOGS)
            }
        }
    }

    override fun exportText(): String {
        val snapshot = _logs.value
        if (snapshot.isEmpty()) return "DynamicFrame debug log vacío.\n"

        return buildString {
            appendLine("=== DynamicFrame debug log ===")
            appendLine("Versión: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Flavor TV: ${BuildConfig.IS_TV}")
            appendLine("Build: ${BuildConfig.BUILD_TYPE}")
            appendLine("Entradas: ${snapshot.size}")
            appendLine()
            snapshot.asReversed().forEach { entry ->
                append("[${timeFormat.format(Date(entry.timestampMs))}] ")
                append("${entry.level.name}/${entry.tag}: ${entry.message}")
                entry.detail?.let { append(" | $it") }
                appendLine()
            }
        }
    }

    companion object {
        private const val LOG_TAG = "DynamicFrame"
        private const val MAX_LOGS = 400
    }
}
