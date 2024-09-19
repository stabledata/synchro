package com.stabledata

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun hikari (): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        driverClassName =  "org.postgresql.Driver"
        jdbcUrl = envString("STABLE_JDBC_URL")
        username = envString("STABLE_DB_USER")
        password = envString("STABLE_DB_PASSWORD")
        maximumPoolSize = envInt("STABLE_DB_MAX_CONNECTIONS")
    }

    return HikariDataSource(hikariConfig)
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

    fun tableExistsAtPath(path: String): Boolean {
        val convertedPath = convertPath(path)
        val exists = """
            SELECT EXISTS (
                SELECT 1 
                FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = '$convertedPath'
            );
            """.trimIndent()
        return transaction {
            val result = exec(exists) {
                it.next() // Move to the first result
                it.getBoolean(1) // Get the boolean value of the result
            }
            return@transaction result ?: false
        }
    }

}