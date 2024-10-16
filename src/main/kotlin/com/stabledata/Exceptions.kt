package com.stabledata
class SQLNotFoundException(message: String) : Exception(message)

/**
 * SQLConflictException exception's message will be returned to clients
 */
class SQLConflictException(message: String) : Exception(message)
