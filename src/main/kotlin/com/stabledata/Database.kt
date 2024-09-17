package com.stabledata

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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

object Collection {

    private fun convertPath (path: String): String {
        return path.replace(".", "_")
    }
    fun existsAtPathSQL(path: String): Boolean {
        val convertedPath = convertPath(path)
        val exists = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '$convertedPath');"
        return transaction {
            val result = exec(exists) {
                it.next() // Move to the first result
                it.getBoolean(1) // Get the boolean value of the result
            }
            return@transaction result ?: false
        }
    }

    fun createAtPathSQL(path: String): String {
        val convertedPath = convertPath(path)
        return """
            CREATE TABLE $convertedPath (id UUID PRIMARY KEY)
        """.trimIndent()
    }
}

object Tables {
    object Logs : Table("stable.logs") {
        // Note: Exposed won't let you name things id that you intend to set from external data
        // https://github.com/JetBrains/Exposed/issues/1512
        val eventId = uuid("id")
        val actorId = text("actor_id")
        val path = text("path").nullable() // Nullable since it's optional in the SQL definition
        val eventType = text("event_type")
        val createdAt = long("created_at")
//        val collectionId = uuid("collection_id").nullable() // Nullable because it's optional
//        val documentId = uuid("document_id").nullable() // Nullable because it's optional
//        val confirmedAt = long("confirmed_at").nullable() // Nullable because it's optional

        fun findById(logId: UUID): ResultRow? {
            return transaction {
                Logs
                    .select { eventId eq logId }
                    .singleOrNull()
            }
        }
    }
}
