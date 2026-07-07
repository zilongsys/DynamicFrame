package com.dynamicframe.domain.model

/** Resultado de intentar borrar un medio local del dispositivo. */
sealed interface DeleteMediaResult {
    data object Deleted : DeleteMediaResult
    /** El sistema Android pide confirmación al usuario; [consentHandle] identifica la petición. */
    data class NeedsUserConsent(val consentHandle: String) : DeleteMediaResult
    data class Failed(val failure: DeleteMediaFailure) : DeleteMediaResult
}
