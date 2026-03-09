package com.katorabian.service.orchestration

import com.katorabian.application.corExcHandler
import com.katorabian.application.printErr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap

//class SessionExecutionManager {
//
//    private val locks = ConcurrentHashMap<UUID, Mutex>()
//
//    suspend fun <T> execute(
//        executionKey: UUID,
//        block: suspend () -> T
//    ): T {
//        val mutex = locks.computeIfAbsent(executionKey) { Mutex() }
//
//        return mutex.withLock {
//            block()
//        }
//    }
//}

 /** Previous variant */
class SessionExecutionManager {
    private val handler = corExcHandler()
    private val sessions = ConcurrentHashMap<UUID, SessionExecutionContext>()

    suspend fun <T> execute(
        userId: UUID,
        block: suspend () -> T
    ): T = withContext(handler) {
        val context = sessions.computeIfAbsent(userId) {
            SessionExecutionContext()
        }

        return@withContext context.execute(block)
    }


    private class SessionExecutionContext {
        private val handler = corExcHandler()
        private val mutex = Mutex()
        private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
        private val scope = CoroutineScope(Dispatchers.Default)

        init {
            scope.launch(handler) {
                for (task in queue) {
                    mutex.withLock(queue) {
                        task()
                    }
                }
            }
        }
        //TODO Вернуть код и разобраться, почему застаканные запросы отменяются.
        //TODO Чёт мне подсказывает что сам стак тут вообще непричём
        suspend fun <T> execute(block: suspend () -> T): T {
            return suspendCancellableCoroutine { cont ->
                scope.launch(handler) { //TODO too dirty variant
                    queue.send {
                        try {
                            val result = block()
                            println("QWERTY: Successfully executed a session execution")
                            cont.resumeWith(Result.success(result))
                        } catch (e: Exception) {
                            println("QWERTY: Failed to execute task: $e")
                            cont.resumeWith(Result.failure(e))
                            "SessionExecutionContext.execute: ".printErr(e)
                        }
                    }
                }
                cont.invokeOnCancellation {
                    println("QWERTY: Session execution cancelled")
                }
            }
        }
    }
}