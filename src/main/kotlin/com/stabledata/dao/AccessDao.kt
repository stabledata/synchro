package com.stabledata.dao

import com.stabledata.endpoint.io.AccessRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

data class AccessRecord(
    val id: String,
    val teamId: String,
    val type: String?,
    val role: String,
    val path: String
)

object AccessTable: Table("stable.access") {
    val accessId = uuid("id")
    val teamId = text("team")
    val kind = text("type").check {
        it inList listOf("grant", "deny")
    }
    val role = text("role")
    val path = text("path")

    fun insertFromRequest(type: String, team: String, record: AccessRequest) {
        AccessTable.insert { row ->
            row[accessId] = UUID.fromString(record.id)
            row[kind] = type
            row[teamId] = team
            row[role] = record.role
            row[path] = record.path

        }
    }

    fun deleteRulesForOperationAndRole(team: String, checkRole: String, checkPath: String) {
        AccessTable.deleteWhere {
            (teamId eq team) and
            (role eq checkRole) and
            (path eq checkPath)
        }
    }

    fun findMatchingRules(team: String, checkRole: String, checkPath: String): Pair<List<AccessRecord>, List<AccessRecord>> {
        val rules = transaction {
            // Later: make paths matchable in parts
            AccessTable
                .select {
                    (teamId eq team) and
                    (role eq checkRole) and
                    (path eq checkPath)

                }
                .map {
                    AccessRecord(
                        id = it[accessId].toString(),
                        teamId = it[teamId],
                        type = it[kind],
                        role = it[role],
                        path = it[path]
                    )
                }
        }

        val allowingRules = rules.filter { it.type == "grant" }
        val blockingRules = rules.filter { it.type == "deny" }

        return Pair(allowingRules, blockingRules)
    }


}