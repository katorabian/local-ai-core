package com.katorabian.llm.llamacpp

import com.katorabian.domain.Constants.MAX_CLIENTS
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

            "--threads", "8",
            "--threads-batch", "4",
            "--threads-http", "4",

            "--no-webui",
            "--log-verbosity", "2",

            // length limit
            "--n-predict", "64",
            "--temp", "0.1",
            "--top-k", "20",
            "--top-p", "0.9",
            "--repeat-penalty", "1.05",
            "--cache-ram", "1",
//            "--no-thoughts",


            // несколько клиентов
            "--parallel", MAX_CLIENTS.toString(),
//            "--n-slots", MAX_CLIENTS.toString(),
        )
}
