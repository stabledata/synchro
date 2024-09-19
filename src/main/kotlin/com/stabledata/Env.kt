package com.stabledata

import io.github.cdimascio.dotenv.Dotenv

class MissingEnvironmentVariable(k: String): Exception("Missing environment variable at key $k")


val dotenv: Dotenv = Dotenv.configure().ignoreIfMissing().load()

fun envString(key: String): String {
    return dotenv[key] ?: System.getenv(key) ?: throw MissingEnvironmentVariable(key)
}

fun envInt(key: String): Int {
    val found = dotenv[key] ?: System.getenv(key) ?: throw MissingEnvironmentVariable(key)
    return found.toInt()
}