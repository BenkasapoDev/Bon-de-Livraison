package com.infosetgroup.delivery.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference

/**
 * Small helper to show snackbars while deduplicating the same message for a short window.
 * This prevents duplicate messages coming from different places (paging LoadState + sync events)
 * and provides a single place to tune dedupe timing.
 */
object SnackbarHelper {
    private val lastMessage = AtomicReference<String?>(null)
    private val lastTimestamp = AtomicReference<Long>(0L)
    private val mutex = Mutex()
    private val dedupeMillis = 2500L // 2.5s window

    suspend fun showIfUnique(host: SnackbarHostState, message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        // quick path: if same message within window, skip
        val now = System.currentTimeMillis()
        val prev = lastMessage.get()
        val prevTs = lastTimestamp.get()
        if (prev != null && prev == message && (now - prevTs) <= dedupeMillis) return

        mutex.withLock {
            val curPrev = lastMessage.get()
            val curPrevTs = lastTimestamp.get()
            val curNow = System.currentTimeMillis()
            if (curPrev != null && curPrev == message && (curNow - curPrevTs) <= dedupeMillis) return
            // show and record
            lastMessage.set(message)
            lastTimestamp.set(curNow)
            host.showSnackbar(message, duration = duration)
        }
    }
}

