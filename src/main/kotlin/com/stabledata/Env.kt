package com.stabledata

import io.github.cdimascio.dotenv.Dotenv

class MissingEnvironmentVariable(k: String): Exception("Missing environment variable at key $k")
fun envString(key: String): String {
    val dotenv = Dotenv.configure().ignoreIfMissing().load()
    return dotenv[key] ?: System.getenv(key) ?: throw MissingEnvironmentVariable(key)
}

fun envInt(key: String): Int {
    val dotenv = Dotenv.configure().ignoreIfMissing().load()
    val found = dotenv[key] ?: System.getenv(key) ?: throw MissingEnvironmentVariable(key)
    return found.toInt()
}