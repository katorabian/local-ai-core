package com.katorabian.service.orchestration

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SessionExecutionManager {

    private val locks = ConcurrentHashMap<UUID, Mutex>()

    suspend fun <T> execute(
        executionKey: UUID,
        block: suspend () -> T
    ): T {
        val mutex = locks.computeIfAbsent(executionKey) { Mutex() }

        return mutex.withLock {
            block()
        }
    }
}

// /** Previous variant */
//class SessionExecutionManager {
//
//    private val sessions = ConcurrentHashMap<UUID, SessionExecutionContext>()
//
//    suspend fun <T> execute(
//        userId: UUID,
//        block: suspend () -> T
//    ): T {
//        val context = sessions.computeIfAbsent(userId) {
//            SessionExecutionContext()
//        }
//
//        return context.execute(block)
//    }
//
//
//    private class SessionExecutionContext {
//
//        private val mutex = Mutex()
//        private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
//        private val scope = CoroutineScope(Dispatchers.Default)
//
//        init {
//            scope.launch {
//                for (task in queue) {
//                    mutex.withLock(queue) {
//                        task()
//                    }
//                }
//            }
//        }
//
//        suspend fun <T> execute(block: suspend () -> T): T {
//            return suspendCancellableCoroutine { cont ->
//                scope.launch { //TODO too dirty variant
//                    queue.send {
//                        try {
//                            val result = block()
//                            println("QWERTY: Successfully executed a session execution")
//                            cont.resumeWith(Result.success(result))
//                        } catch (e: Exception) {
//                            println("QWERTY: Failed to execute task: $e")
//                            cont.resumeWith(Result.failure(e))
//                        }
//                    }
//                }
//                cont.invokeOnCancellation {
//                    println("QWERTY: Session execution cancelled")
//                }
//            }
//        }
//    }
//}