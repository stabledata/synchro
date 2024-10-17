package com.stabledata.dao

import com.stabledata.SQLConflictException
import com.stabledata.SQLNotFoundException
import com.stabledata.model.Collection
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import java.util.*

object CollectionsTable : Table("stable.collections") {

    val logger = KotlinLogging.logger {}

    val id = uuid("id")
    private val team_id = text("team")
    val path = text("path")
    val type = text("type").nullable()
    val label = text("label").nullable()
    val icon = text("icon").nullable()
    val description = text("description").nullable()

    fun insertRowFromRequest(team: String, insert: Collection): UUID {
        return try {
            CollectionsTable.insert { row ->
                row[id] = UUID.fromString(insert.id)
                row[path] = insert.path
                row[team_id] = team
                row[type] = insert.type
                row[label] = insert.label
                row[icon] = insert.icon
                row[description] = insert.description
            }
            UUID.fromString(insert.id)
        } catch (e: Exception) {
            logger.error { "Exception inserting collection ${e.localizedMessage}" }
            throw SQLConflictException("Failed to insert collection")
        }

    }

    fun updateAtPath(path: String, update: Collection): Collection {
        val numRecordsUpdated = CollectionsTable.update({
            CollectionsTable.path eq path
        }) { row ->
            row[type] = update.type
            row[label] = update.label
            row[icon] = update.icon
            row[description] = update.description
        }
        // we might want to think about that number logic
        // but, should never be more than one since we check for records at path before create
        return if (numRecordsUpdated == 1) { update } else {
            throw SQLNotFoundException("Failed to update collection at path: $path")
        }
    }

    fun deleteAtPath(path: String) {
        val numRecordsDeleted = CollectionsTable.deleteWhere {
            CollectionsTable.path eq path
        }
        if (numRecordsDeleted != 1) {
            throw SQLNotFoundException("Failed to delete collection at path: $path")
        }
    }
}
