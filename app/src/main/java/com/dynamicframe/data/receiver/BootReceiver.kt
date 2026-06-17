package com.dynamicframe.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dynamicframe.MainActivity
import com.dynamicframe.data.local.SettingsBootCache

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(SettingsBootCache.PREFS_NAME, Context.MODE_PRIVATE)
        val autoStart = prefs.getBoolean(SettingsBootCache.AUTO_START_BOOT, false)

        if (autoStart) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("AUTO_STARTED", true)
            }
            context.startActivity(launchIntent)
        }
    }
}
