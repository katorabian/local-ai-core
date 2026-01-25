package com.katorabian.domain

object Constants {
    const val ZERO = 0
    const val NOT_FOUND = -1

    // IO
    const val LLM_READ_BUFFER = 8 * 1024
    const val MAX_SSE_CHUNK_SIZE = 512

    // rest api
    const val CONNECT_TIMEOUT_MS = 5_000L
    const val REQUEST_TIMEOUT_WARMUP_MS = 120_000L
}