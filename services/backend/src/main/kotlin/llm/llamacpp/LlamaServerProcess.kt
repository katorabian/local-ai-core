package com.katorabian.llm.llamacpp

import com.katorabian.domain.Constants.REQUEST_TIMEOUT_WARMUP_MS
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import java.io.File

class LlamaServerProcess(
    private val llamaDir: File,
    private val modelPath: File,
    private val port: Int = 8081,
    private val ctxSize: Int = 8192,
) {

    private var process: Process? = null

    fun start() {
        if (process?.isAlive == true) return

        val exe = File(llamaDir, "llama-server.exe")
        require(exe.exists()) {
            "llama-server.exe not found in $llamaDir"
        }

        val cmd = listOf(
            exe.absolutePath,
            "--model", modelPath.absolutePath,

            "--host", "127.0.0.1",
            "--port", port.toString(),

            "--ctx-size", ctxSize.toString(),

            "--gpu-layers", "all",
            "--device", "CUDA0",

            "--threads", "4",
            "--threads-batch", "4",
            "--threads-http", "4",

            "--batch-size", "2048",

            "--no-webui",
            "--log-verbosity", "3"
        )

        process = ProcessBuilder(cmd)
            .directory(llamaDir)
            .redirectErrorStream(true)
            .start()

        Thread {
            process!!
                .inputStream
                .bufferedReader()
                .useLines { lines ->
                    lines.forEach { println("[llama] $it") }
                }
        }.start()
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    fun isAlive(): Boolean = process?.isAlive == true

    fun ensureAlive() {
        if (process?.isAlive != true) {
            println("llama-server is not alive, restarting")
            start()
        }
    }

    suspend fun waitUntilReady(
        timeoutMs: Long = REQUEST_TIMEOUT_WARMUP_MS,
        pollDelayMs: Long = 500
    ) {
        val client = HttpClient(CIO)
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                client.get("http://127.0.0.1:$port/health")
                return // сервер готов
            } catch (_: Exception) {
                delay(pollDelayMs)
            }
        }

        error("llama-server did not become ready in $timeoutMs ms")
    }

}
