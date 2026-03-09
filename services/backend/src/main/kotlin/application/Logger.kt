package com.katorabian.application

import kotlinx.coroutines.CoroutineExceptionHandler

inline fun <reified Caller> Caller.extractTag(): String = when (this@extractTag) {
    is String -> this
    is Number -> this.toString()
    null -> "NULL"
    else -> this::class.java.simpleName
}

inline fun <reified Caller> Caller.corExcHandler() = CoroutineExceptionHandler { _, exception -> this@corExcHandler.printErr(exception) }
inline fun <reified Caller> Caller.printErr(message: String) = System.err.println(extractTag() + ": " + message)
inline fun <reified Caller> Caller.printErr(message: Throwable?) = printErr(message?.stackTraceToString()?: "MISSING EXCEPTION")