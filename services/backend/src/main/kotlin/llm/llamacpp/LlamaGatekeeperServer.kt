package com.katorabian.llm.llamacpp

import java.io.File

class LlamaGatekeeperServer(
    llamaDir: File,
    modelPath: File,
    port: Int = 8082,
    ctxSize: Int = 2048,
) : LlamaServerProcess(
    llamaDir = llamaDir,
    modelPath = modelPath,
    port = port,
    ctxSize = ctxSize
) {

    override fun buildCommand(exe: File): List<String> =
        listOf(
            exe.absolutePath,
            "--model", modelPath.absolutePath,

            "--host", "127.0.0.1",
            "--port", port.toString(),
            "--ctx-size", ctxSize.toString(),

            "--gpu-layers", "0",

            "--threads", "4",
            "--threads-batch", "2",
            "--threads-http", "2",

            "--top_p", "0.9",
            "--top_k", "40",

            "--no-webui",
            "--log-verbosity", "2",

            // length limit
            "--n-predict", "64",
        )
}
