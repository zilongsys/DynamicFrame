package com.dynamicframe.domain.usecase

import com.dynamicframe.domain.debug.DebugLogEntry
import com.dynamicframe.domain.repository.AppDebugLogger
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveDebugModeUseCase @Inject constructor(
    private val logger: AppDebugLogger
) {
    operator fun invoke(): StateFlow<Boolean> = logger.enabled
}

class ObserveDebugLogsUseCase @Inject constructor(
    private val logger: AppDebugLogger
) {
    operator fun invoke(): StateFlow<List<DebugLogEntry>> = logger.logs
}

class SetDebugModeUseCase @Inject constructor(
    private val logger: AppDebugLogger
) {
    suspend operator fun invoke(enabled: Boolean) = logger.setEnabled(enabled)
}

class ClearDebugLogsUseCase @Inject constructor(
    private val logger: AppDebugLogger
) {
    operator fun invoke() = logger.clear()
}

class ExportDebugLogsUseCase @Inject constructor(
    private val logger: AppDebugLogger
) {
    operator fun invoke(): String = logger.exportText()
}

class LoadDebugLoggerUseCase @Inject constructor(
    private val logger: AppDebugLogger
) {
    suspend operator fun invoke() = logger.load()
}
