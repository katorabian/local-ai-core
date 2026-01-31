package com.katorabian.llm.llamacpp

import com.katorabian.domain.Constants.REQUEST_TIMEOUT_WARMUP_MS
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit

abstract class LlamaServerProcess(
    protected val llamaDir: File,
    protected val modelPath: File,
    protected val port: Int,
    protected val ctxSize: Int,
) {

    @Volatile
    private var starting = false

    protected var process: Process? = null

    fun start() {
        if (process?.isAlive == true) return

        val exe = File(llamaDir, "llama-server.exe")
        require(exe.exists()) {
            "llama-server.exe not found in $llamaDir"
        }

        val cmd = buildCommand(exe)

        process = ProcessBuilder(cmd)
            .directory(llamaDir)
            .redirectErrorStream(true)
            .start()

        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })

        Thread {
            process!!
                .inputStream
                .bufferedReader()
                .useLines { lines ->
                    lines.forEach { println("[llama:$port] $it") }
                }
        }.start()
    }

    fun stop() {
        val p = process ?: return
        try {
            p.destroy()
            if (!p.waitFor(3, TimeUnit.SECONDS)) {
                p.destroyForcibly()
            }
        } catch (ex: Exception) {
            println(ex.buildSimpleLog())
        } finally {
            process = null
        }
    }

    fun ensureAlive() {
        if (process?.isAlive == true || starting) return

        synchronized(this) {
            if (process?.isAlive == true || starting) return
            starting = true
            println("llama-server on port $port is not alive, starting")
            start()
            starting = false
        }
    }

    suspend fun waitUntilReady(
        timeoutMs: Long = REQUEST_TIMEOUT_WARMUP_MS,
        pollDelayMs: Long = 500
    ) {
        val client = HttpClient(CIO)
        val start = System.currentTimeMillis()

        client.use {
            while (System.currentTimeMillis() - start < timeoutMs) {
                try {
                    it.get("http://127.0.0.1:$port")
                    return
                } catch (ex: Exception) {
                    println(ex.buildSimpleLog())
                    delay(pollDelayMs)
                }
            }
        }

        error("llama-server on port $port did not become ready in $timeoutMs ms")
    }

    fun Exception.buildSimpleLog(): String {
        val ex = this
        val tag = this@LlamaServerProcess::class.simpleName
        return tag + '\n' + ex.message + '\n' + ex.stackTraceToString()
    }

    protected abstract fun buildCommand(exe: File): List<String>
}
