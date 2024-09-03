package com.stabledata

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger

const val DEFAULT_LOGGER_NAME = "stable"

var defaultLogger: Logger? = null

class LoggerNotInitializedException: Exception("Logger not initialized, call configureLogging() or setLogger()")
fun getLogger(): Logger {
    return defaultLogger ?: throw LoggerNotInitializedException()
}

fun setLogger (name: String = DEFAULT_LOGGER_NAME) {
    defaultLogger = LoggerFactory.getLogger(name) as Logger
}
fun configureLogging() {
    val logBack = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
    val logLevel = System.getenv("LOG_LEVEL") ?: "INFO"

    val level = when (logLevel.uppercase()) {
        "DEBUG" -> Level.DEBUG
        "INFO" -> Level.INFO
        "WARN" -> Level.WARN
        "ERROR" -> Level.ERROR
        "TRACE" -> Level.TRACE
        else -> Level.INFO  // Default to INFO if the value is unrecognized
    }
    println("LOG LEVEL SET TO: $logLevel")
    logBack.level = level
    setLogger()
}