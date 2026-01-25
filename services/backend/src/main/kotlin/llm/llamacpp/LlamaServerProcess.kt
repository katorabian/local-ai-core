package com.katorabian.llm.llamacpp

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

            "--ctx-size", ctxSize.toString(),   // 8192 — нормально
            "--gpu-layers", "all",              // КЛЮЧЕВО
            "--parallel", "1",
            "--threads", "4",
            "--threads-batch", "4",
            "--threads-http", "4",
            "--sampling-seq", "temperature;top_p",
            "--batch-size", "2048",
            "--log-verbosity", "3",
            "--device", "0",
            "--kv-offload",

            "--no-webui",                       // если не нужен
        )

        process = ProcessBuilder(cmd)
            .directory(llamaDir)
            .redirectErrorStream(true)
            .start()
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    fun isAlive(): Boolean =
        process?.isAlive == true
}
