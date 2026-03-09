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
    private val locks = ConcurrentHashMap<UUID, Mutex>()

    suspend fun <T> execute(
        executionKey: UUID,
        block: suspend () -> T
    ): T = withContext(handler) {
        val mutex = locks.computeIfAbsent(executionKey) { Mutex() }

        return@withContext mutex.withLock { block() }
    }
}