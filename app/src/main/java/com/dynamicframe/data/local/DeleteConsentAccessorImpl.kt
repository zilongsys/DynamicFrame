package com.dynamicframe.data.local

import com.dynamicframe.domain.repository.DeleteConsentAccessor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteConsentAccessorImpl @Inject constructor(
    private val store: PendingDeleteConsentStore,
) : DeleteConsentAccessor {
    override fun take(handle: String): Any? = store.consume(handle)
}
