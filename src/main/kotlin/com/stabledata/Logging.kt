package com.stabledata

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import org.slf4j.Logger


const val DEFAULT_LOGGER_NAME = "stable"

var defaultLogger: Logger? = null

class LoggerNotInitializedException: Exception("Logger not initialized, call configureLogging() or setLogger()")
fun getLogger(): Logger {
    return defaultLogger ?: throw LoggerNotInitializedException()
}

fun setLogger (name: String = DEFAULT_LOGGER_NAME) {
    defaultLogger = LoggerFactory.getLogger(name)
}
fun configureLogging(): Logger {
    val logLevel = System.getenv("LOG_LEVEL") ?: "INFO"

    val level = when (logLevel.uppercase()) {
        "DEBUG" -> Level.DEBUG
        "INFO" -> Level.INFO
        "WARN" -> Level.WARN
        "ERROR" -> Level.ERROR
        "TRACE" -> Level.TRACE
        else -> Level.INFO  // Default to INFO if the value is unrecognized
    }

    // set level at the factory context level
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
    rootLogger.level = level

    setLogger()
    val logger = getLogger() as ch.qos.logback.classic.Logger
    logger.level = level
    logger.info("default logger initialized")
    return logger
}