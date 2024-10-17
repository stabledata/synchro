package com.stabledata.dao

import com.stabledata.SQLConflictException
import com.stabledata.model.Access
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
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

    val logger = KotlinLogging.logger {}

    fun insertFromRequest(type: String, team: String, record: Access) {
        try {
            AccessTable.insert { row ->
                row[accessId] = UUID.fromString(record.id)
                row[kind] = type
                row[teamId] = team
                row[role] = record.role
                row[path] = record.path
            }
        } catch (e: ExposedSQLException) {
            logger.error { "Error inserting access record: ${e.localizedMessage}" }
            throw SQLConflictException(e.localizedMessage)
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