package com.stabledata.dao

import com.stabledata.endpoint.io.AccessRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class AccessRecord(
    val id: String,
    val teamId: String,
    val type: String,
    val role: String,
    val operation: String?,
    val path: String?
)

object AccessTable: Table("stable.access") {
    val accessId = uuid("id")
    val teamId = varchar("team_id", 255)
    val kind = varchar("type", 5).check {
        it inList listOf("grant", "deny")
    }
    val role = varchar("role", 255)
    val operation = varchar("operation", 255).nullable()
    val path = varchar("path", 255).nullable()

    init {
        check("either_operation_or_path") {
            (operation.isNotNull() and path.isNull()) or (operation.isNull() and path.isNotNull())
        }
    }

    fun insertFromRequest(type: String, team: String, record: AccessRequest) {
        AccessTable.insert { row ->
            row[accessId] = UUID.fromString(record.id)
            row[kind] = type
            row[teamId] = team
            row[role] = record.role
            row[path] = record.path
            row[operation] = record.operation



        }
    }

    fun findMatchingRules(operationOrPath: String, team: String, checkRole: String): Pair<List<AccessRecord>, List<AccessRecord>> {
        val rules = transaction {
            // Later: make paths matchable in parts
            AccessTable
                .select {
                    (teamId eq team) and
                    (role eq checkRole) and
                    (
                        (operation eq operationOrPath) or
                        (path eq operationOrPath)
                    )
                }
                .map {
                    AccessRecord(
                        id = it[accessId].toString(),
                        teamId = it[teamId],
                        type = it[kind],
                        role = it[role],
                        operation = it[operation],
                        path = it[path]
                    )
                }
        }

        val allowingRules = rules.filter { it.type == "grant" }
        val blockingRules = rules.filter { it.type == "deny" }

        return Pair(allowingRules, blockingRules)
    }
}