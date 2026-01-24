package com.katorabian.llm.llamacpp

import java.io.File

class LlamaServerProcess(
    private val llamaDir: File,
    private val modelPath: File,
    private val port: Int = 8081,
    private val ctxSize: Int = 8192,
    private val gpuLayers: Int = 99
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
            "-m", modelPath.absolutePath,
            "--port", port.toString(),
            "--ctx-size", ctxSize.toString(),
            "--n-gpu-layers", gpuLayers.toString()
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
