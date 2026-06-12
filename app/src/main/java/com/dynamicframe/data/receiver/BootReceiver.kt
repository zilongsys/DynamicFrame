package com.dynamicframe.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dynamicframe.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Verificar si el usuario activó el autostart en configuración
            val prefs = context.getSharedPreferences("settings_cache", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("auto_start_boot", false)

            if (autoStart) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("AUTO_STARTED", true)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
