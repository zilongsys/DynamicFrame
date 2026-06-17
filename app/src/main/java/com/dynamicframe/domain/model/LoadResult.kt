package com.dynamicframe.domain.model

/** Resultado sellado para fuentes de medios (local y remotas futuras). */
sealed class LoadResult<out T> {
    data class Loading(val progress: Float? = null) : LoadResult<Nothing>()
    data class Success<T>(val data: T) : LoadResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : LoadResult<Nothing>()
}
