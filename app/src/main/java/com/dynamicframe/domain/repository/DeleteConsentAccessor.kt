package com.dynamicframe.domain.repository

/** Puente opaco para recuperar el IntentSender de un borrado pendiente (implementación en data). */
interface DeleteConsentAccessor {
    fun take(handle: String): Any?
}
