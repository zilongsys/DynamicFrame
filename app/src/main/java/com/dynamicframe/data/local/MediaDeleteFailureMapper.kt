package com.dynamicframe.data.local

import com.dynamicframe.domain.model.DeleteMediaFailure
import com.dynamicframe.domain.model.DeleteMediaFailureAction
import com.dynamicframe.domain.model.DeleteMediaFailureReason

object MediaDeleteFailureMapper {

    fun notLocalSource(): DeleteMediaFailure = DeleteMediaFailure.notLocalSource()

    fun unsupportedUri(): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.UNSUPPORTED_URI,
        title = "Tipo de archivo no compatible",
        explanation = "La app no reconoce la ubicación de este archivo.",
        solution = "Bórralo desde la Galería o la app Archivos del sistema.",
        action = DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY,
    )

    fun fileNotWritable(uri: String): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.FILE_NOT_WRITABLE,
        title = "Sin permiso de escritura",
        explanation = "El archivo existe pero la app no puede modificarlo.",
        solution = "Comprueba que la carpeta no sea de solo lectura o bórralo con la app Archivos.",
        action = DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY,
        mediaUri = uri,
    )

    fun readOnlyStorage(uri: String): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.READ_ONLY_STORAGE,
        title = "Almacenamiento de solo lectura",
        explanation = "El archivo está en un volumen protegido (p. ej. tarjeta SD bloqueada o USB).",
        solution = "Desbloquea la tarjeta SD o mueve la foto al almacenamiento interno y vuelve a intentarlo.",
        mediaUri = uri,
    )

    fun safNoWriteAccess(uri: String): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.SAF_NO_WRITE_ACCESS,
        title = "Carpeta sin permiso de borrado",
        explanation = "La carpeta se añadió con acceso de solo lectura o Android no permite borrar en ella.",
        solution = "Ve a Ajustes → Contenido, elimina la carpeta y vuelve a añadirla concediendo acceso completo cuando el sistema lo pida.",
        action = DeleteMediaFailureAction.OPEN_CONTENT_SETTINGS,
        mediaUri = uri,
    )

    fun permissionDenied(uri: String): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.PERMISSION_DENIED,
        title = "Permiso denegado",
        explanation = "Android no autorizó a la app a borrar este archivo.",
        solution = "Cuando aparezca el diálogo del sistema, pulsa «Permitir». Si no sale, bórralo desde Galería o Archivos.",
        action = DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY,
        mediaUri = uri,
    )

    fun systemDeleteBlocked(uri: String): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.SYSTEM_DELETE_BLOCKED,
        title = "El sistema bloqueó el borrado",
        explanation = "Este archivo está protegido por las reglas de privacidad de Android.",
        solution = "Abre la foto en Galería o Archivos y bórrala desde allí, o concede el permiso cuando el sistema lo solicite.",
        action = DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY,
        mediaUri = uri,
    )

    fun fileInUse(uri: String): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.FILE_IN_USE,
        title = "Archivo en uso",
        explanation = "Otra app o el propio sistema está usando el archivo ahora mismo.",
        solution = "Espera unos segundos, cierra otras apps que lo tengan abiertas e inténtalo de nuevo.",
        mediaUri = uri,
    )

    fun unknown(uri: String, detail: String? = null): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.UNKNOWN,
        title = "No se pudo borrar",
        explanation = detail?.takeIf { it.isNotBlank() }
            ?: "Ocurrió un error inesperado al eliminar el archivo.",
        solution = "Prueba a borrarlo desde Galería o Archivos. Si la foto viene de una carpeta personalizada, revisa los permisos en Ajustes → Contenido.",
        action = DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY,
        mediaUri = uri,
    )

    fun consentRequestFailed(): DeleteMediaFailure = DeleteMediaFailure(
        reason = DeleteMediaFailureReason.SYSTEM_DELETE_BLOCKED,
        title = "No se pudo pedir permiso al sistema",
        explanation = "La app no pudo abrir el diálogo de confirmación de Android.",
        solution = "Reinicia la app e inténtalo de nuevo, o borra el archivo desde Galería.",
        action = DeleteMediaFailureAction.OPEN_FILE_EXTERNALLY,
    )
}
