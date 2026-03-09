package com.katorabian.service.orchestration

import com.katorabian.application.corExcHandler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/** put requests in stack*/

class SessionExecutionManager {

    private val handler = corExcHandler()
    private val locks = ConcurrentHashMap<UUID, SessionLock>()

    suspend fun <T> execute(
        executionKey: UUID,
        block: suspend () -> T
    ): T = withContext(handler) {

        val lock = checkNotNull(
            locks.compute(executionKey) { _, existing ->
                existing?.updateLastAccess() ?: SessionLock()
            }
        )
        cleanup()
        return@withContext lock.mutex.withLock { block() }
    }

    private fun cleanup() {
        val now = System.currentTimeMillis()
        val ttl = 10 * 60 * 1000L // 10 minutes

        locks.entries.removeIf {
            now - it.value.lastAccess > ttl
        }
    }
}

private data class SessionLock(
    val mutex: Mutex = Mutex(),
    var lastAccess: Long = System.currentTimeMillis()
) {
    fun updateLastAccess(): SessionLock = apply { lastAccess = System.currentTimeMillis() }
}