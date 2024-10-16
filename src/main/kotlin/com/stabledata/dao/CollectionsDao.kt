package com.stabledata.dao

import com.stabledata.endpoint.io.CollectionRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class CollectionUpdateFailedException(path: String) : Exception("Failed to update collection at path $path")
class CollectionDeleteFailedException(path: String) : Exception("Failed to delete collection at path $path")

object CollectionsTable : Table("stable.collections") {
    val id = uuid("id")
    val team_id = text("team_id")
    val path = text("path")
    val type = text("type").nullable()
    val label = text("label").nullable()
    val icon = text("icon").nullable()
    val description = text("description").nullable()

    fun insertRowFromRequest(team: String, insert: CollectionRequest): UUID {
            CollectionsTable.insert { row ->
                row[path] = insert.path
                row[id] = UUID.fromString(insert.id)
                row[team_id] = team
                row[type] = insert.type
                row[label] = insert.label
                row[icon] = insert.icon
                row[description] = insert.description
            }

        return UUID.fromString(insert.id)
    }

    fun updateAtPath(path: String, update: CollectionRequest): CollectionRequest {
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
            throw CollectionUpdateFailedException(path)
        }
    }

    fun getCollection(collectionId: String): ResultRow? {
        return transaction {
            CollectionsTable.select{ CollectionsTable.id eq UUID.fromString(collectionId) }
                .singleOrNull()
        }
    }

    fun deleteAtPath(path: String) {
        val numRecordsDeleted = CollectionsTable.deleteWhere {
            CollectionsTable.path eq path
        }
        if (numRecordsDeleted != 1) {
            throw CollectionDeleteFailedException(path)
        }
    }
}
