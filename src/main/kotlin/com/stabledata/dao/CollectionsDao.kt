package com.stabledata.dao

import com.stabledata.endpoint.io.CreateCollectionRequestBody
import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object CollectionsTable : Table("stable.collections") {
    val id = uuid("id")
    val path = text("path")
    val type = text("type").nullable()
    val label = text("label").nullable()
    val icon = text("icon").nullable()
    val description = text("description").nullable()

    fun insertRowFromRequest(body: CreateCollectionRequestBody): UUID {
        transaction {
            CollectionsTable.insert { row ->
                row[id] = UUID.fromString(body.id)
                row[path] = body.path
                row[type] = body.type
                row[label] = body.label
                row[icon] = body.icon
                row[description] = body.description
            }
        }
        return UUID.fromString(body.id)
    }

}
