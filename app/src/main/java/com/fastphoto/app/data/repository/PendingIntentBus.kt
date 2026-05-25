package com.fastphoto.app.data.repository

import android.content.IntentSender
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingIntentBus @Inject constructor() {
    private val _pending = MutableSharedFlow<IntentSender>(extraBufferCapacity = 8)
    val pending: SharedFlow<IntentSender> = _pending.asSharedFlow()

    suspend fun emit(intentSender: IntentSender) {
        _pending.emit(intentSender)
    }
}
