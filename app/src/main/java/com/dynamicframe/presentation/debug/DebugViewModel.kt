package com.dynamicframe.presentation.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicframe.domain.repository.AppDebugLogger
import com.dynamicframe.domain.usecase.ClearDebugLogsUseCase
import com.dynamicframe.domain.usecase.ExportDebugLogsUseCase
import com.dynamicframe.domain.usecase.LoadDebugLoggerUseCase
import com.dynamicframe.domain.usecase.ObserveDebugLogsUseCase
import com.dynamicframe.domain.usecase.ObserveDebugModeUseCase
import com.dynamicframe.domain.usecase.SetDebugModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
  private val debugLogger: AppDebugLogger,
  observeDebugMode: ObserveDebugModeUseCase,
  observeDebugLogs: ObserveDebugLogsUseCase,
  private val setDebugMode: SetDebugModeUseCase,
  private val clearDebugLogs: ClearDebugLogsUseCase,
  private val exportDebugLogs: ExportDebugLogsUseCase,
  loadDebugLogger: LoadDebugLoggerUseCase
) : ViewModel() {

    val debugEnabled: StateFlow<Boolean> = observeDebugMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val logs = observeDebugLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { loadDebugLogger() }
    }

    fun setDebugEnabled(enabled: Boolean) {
        viewModelScope.launch { setDebugMode(enabled) }
    }

    fun clearLogs() = clearDebugLogs()

    fun exportLogsText(): String = exportDebugLogs()

    fun logInfo(tag: String, message: String) = debugLogger.i(tag, message)

    fun logWarn(tag: String, message: String, detail: String? = null) =
        debugLogger.w(tag, message, detail)

    fun logError(tag: String, message: String, detail: String? = null) =
        debugLogger.e(tag, message, detail)
}
