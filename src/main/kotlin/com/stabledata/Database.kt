package com.stabledata

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlin.concurrent.Volatile


val DB_LOCK = Any()

@Volatile
var hikariDS: HikariDataSource? = null
fun hikari (): HikariDataSource {
    val hikariConfig = if (envFlag("PROD")) HikariConfig().apply {
        jdbcUrl = envString("NEON_FULL_JDBC_URL")
    } else HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = envString("STABLE_JDBC_URL")
        username = envString("STABLE_DB_USER")
        password = envString("STABLE_DB_PASSWORD")
        maximumPoolSize = envInt("STABLE_DB_MAX_CONNECTIONS")
    }

    return hikariDS ?: synchronized(DB_LOCK) {
        hikariDS ?: HikariDataSource(hikariConfig).also { hikariDS = it }
    }
}

fun convertPath (path: String): String {
    return path.replace(".", "_")
}

fun sanitizeString(input: String): String {
    val regex = Regex("^[a-zA-Z0-9_]+\$")

    // Check if the input matches the allowed pattern
    if (!regex.matches(input)) {
        throw IllegalArgumentException("Dirty SQL string: $input")
    }

    return input
}

object DatabaseOperations {
    fun createTableAtPathSQL(team: String, path: String): String {
        val tableName = sanitizeString(convertPath(path))
        val cleanTeamName = sanitizeString(team)
        return """
            CREATE SCHEMA IF NOT EXISTS $team;
            CREATE TABLE $cleanTeamName.$tableName (
                id uuid PRIMARY KEY,
                nano_id varchar(8) NOT NULL
            );
            CREATE INDEX ${cleanTeamName}_${tableName}_idx_nano ON $cleanTeamName.$tableName (nano_id);
        """.trimIndent()
    }

    fun dropTableAtPath(team: String, path: String): String {
        val tableName = sanitizeString(convertPath(path))
        val cleanTeamName = sanitizeString(team)
        return """
            DROP TABLE $cleanTeamName.$tableName
        """.trimIndent()
    }
}