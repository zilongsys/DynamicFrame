package com.dynamicframe.data.local

import android.content.IntentSender
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingDeleteConsentStore @Inject constructor() {
    private val pending = ConcurrentHashMap<String, IntentSender>()

    fun register(intentSender: IntentSender): String {
        val id = UUID.randomUUID().toString()
        pending[id] = intentSender
        return id
    }

    fun consume(handle: String): IntentSender? = pending.remove(handle)
}
