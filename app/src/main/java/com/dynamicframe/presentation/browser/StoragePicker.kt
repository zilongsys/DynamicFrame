package com.dynamicframe.presentation.browser

import android.content.Context
import android.content.Intent

object StoragePicker {

    /** True si el dispositivo tiene un selector SAF (muchos TV box no). */
    fun isSystemFolderPickerAvailable(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return intent.resolveActivity(context.packageManager) != null
    }

    fun shouldUseInAppBrowser(isTv: Boolean, context: Context): Boolean =
        isTv || !isSystemFolderPickerAvailable(context)
}
