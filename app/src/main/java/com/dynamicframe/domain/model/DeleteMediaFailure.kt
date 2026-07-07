package com.dynamicframe.domain.model

/** Motivo por el que no se pudo borrar un medio local. */
enum class DeleteMediaFailureReason {
    NOT_LOCAL_SOURCE,
    UNSUPPORTED_URI,
    FILE_NOT_WRITABLE,
    READ_ONLY_STORAGE,
    SAF_NO_WRITE_ACCESS,
    PERMISSION_DENIED,
    SYSTEM_DELETE_BLOCKED,
    FILE_IN_USE,
    UNKNOWN,
}

/** Acción que la UI puede ofrecer para ayudar al usuario. */
enum class DeleteMediaFailureAction {
    NONE,
    /** Abrir Ajustes → Contenido (carpetas de fotos/vídeos). */
    OPEN_CONTENT_SETTINGS,
    /** Abrir el archivo con otra app del sistema (Galería, Archivos…). */
    OPEN_FILE_EXTERNALLY,
}

/**
 * Error de borrado con explicación y solución sugerida para mostrar al usuario.
 */
data class DeleteMediaFailure(
    val reason: DeleteMediaFailureReason,
    val title: String,
    val explanation: String,
    val solution: String,
    val action: DeleteMediaFailureAction = DeleteMediaFailureAction.NONE,
    val mediaUri: String? = null,
) {
    companion object {
        fun notLocalSource(): DeleteMediaFailure = DeleteMediaFailure(
            reason = DeleteMediaFailureReason.NOT_LOCAL_SOURCE,
            title = "No se puede borrar desde aquí",
            explanation = "Este archivo no está en el almacenamiento local del dispositivo.",
            solution = "Solo puedes borrar fotos y vídeos guardados en el dispositivo o en carpetas añadidas en Ajustes → Contenido.",
        )

        fun consentRequestFailed(): DeleteMediaFailure = DeleteMediaFailure(
            reason = DeleteMediaFailureReason.SYSTEM_DELETE_BLOCKED,
            title = "No se pudo pedir permiso al sistema",
            explanation = "La app no pudo abrir el diálogo de confirmación de Android.",
            solution = "Reinicia la app e inténtalo de nuevo, o borra el archivo desde Galería.",
            action = DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY,
        )

        fun userCancelled(): DeleteMediaFailure = DeleteMediaFailure(
            reason = DeleteMediaFailureReason.PERMISSION_DENIED,
            title = "Borrado cancelado",
            explanation = "No confirmaste el borrado en el diálogo del sistema.",
            solution = "Si quieres eliminar el archivo, vuelve a pulsar Borrar y acepta cuando Android lo pida.",
        )
    }
}
