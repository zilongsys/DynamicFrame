package com.dynamicframe.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dynamicframe.MainActivity
import com.dynamicframe.data.local.SettingsBootCache

/**
 * Arranque automático tras reiniciar (orientado a Android TV).
 *
 * En Android TV el lanzamiento de la Activity desde `BOOT_COMPLETED` es el patrón soportado
 * para apps tipo marco digital. En móviles modernos puede estar restringido por las reglas de
 * inicio en background; por eso se envuelve en try/catch para no provocar crashes silenciosos.
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_AUTO_STARTED = "AUTO_STARTED"
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val prefs = context.getSharedPreferences(SettingsBootCache.PREFS_NAME, Context.MODE_PRIVATE)
        val autoStart = prefs.getBoolean(SettingsBootCache.AUTO_START_BOOT, false)
        if (!autoStart) return

        runCatching {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_AUTO_STARTED, true)
            }
            context.startActivity(launchIntent)
        }.onFailure {
            Log.w(TAG, "No se pudo iniciar la Activity tras el arranque: ${it.message}")
        }
    }
}
