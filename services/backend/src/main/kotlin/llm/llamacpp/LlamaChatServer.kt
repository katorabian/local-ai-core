package com.katorabian.llm.llamacpp

import com.katorabian.domain.Constants.MAX_CLIENTS
import java.io.File

class LlamaChatServer(
    llamaDir: File,
    modelPath: File,
    port: Int = 8081,
    ctxSize: Int = 4096,
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

            "--gpu-layers", "all",
            "--device", "CUDA0",

            "--threads", "4",
            "--threads-batch", "4",
            "--threads-http", "4",
            "--batch-size", "2048",

            "--top_p", "0.9",
            "--top_k", "40",

            "--repeat_penalty", "1.15",
            "--repeat_last_n", "128",
            "--presence_penalty", "0.4",
            "--frequency_penalty", "0.4",

            "--no-webui",
            "--log-verbosity", "3",

            // несколько клиентов
            "--parallel", MAX_CLIENTS.toString(),
//            "--n-slots", MAX_CLIENTS.toString(),
        )
}
