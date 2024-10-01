package com.stabledata

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.concurrent.Volatile


val DB_LOCK = Any()

@Volatile
var hikariDS: HikariDataSource? = null
fun hikari (): HikariDataSource {
    return hikariDS ?: synchronized(DB_LOCK) {
        hikariDS ?: HikariDataSource(HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = envString("STABLE_JDBC_URL")
            username = envString("STABLE_DB_USER")
            password = envString("STABLE_DB_PASSWORD")
            maximumPoolSize = envInt("STABLE_DB_MAX_CONNECTIONS")
        }).also { hikariDS = it }
    }
}

fun convertPath (path: String): String {
    return path.replace(".", "_")
}

object DatabaseOperations {
    fun createTableAtPathSQL(path: String): String {
        val convertedPath = convertPath(path)
        return """
            CREATE TABLE $convertedPath (id UUID PRIMARY KEY)
        """.trimIndent()
    }

    fun dropTableAtPath(path: String): String {
        val convertedPath = convertPath(path)
        return """
            DROP TABLE $convertedPath
        """.trimIndent()
    }

    fun tableExistsAtPath(path: String): Boolean {
        val convertedPath = convertPath(path)
        val existsQuery = """
            SELECT EXISTS (
                SELECT 1 
                FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = '$convertedPath'
            );
            """.trimIndent()
        val existsResult = transaction {
            exec(existsQuery) {
                it.next() // Move to the first result
                it.getBoolean(1) // Get the boolean value of the result
            }
        }
        return existsResult ?: false
    }

}