package com.stabledata.dao

import com.stabledata.convertPath
import com.stabledata.endpoint.io.CreateCollectionRequestBody
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object CollectionsTable : UUIDTable("stable.collections") {
    val path = text("path")
    val type = text("type").nullable()
    val label = text("label").nullable()
    val icon = text("icon").nullable()
    val description = text("description").nullable()

    fun insertRowFromRequest(body: CreateCollectionRequestBody): UUID {
        val insertedId = transaction {
            CollectionsTable.insert { row ->
                row[path] = body.path
                row[type] = body.type
                row[label] = body.label
                row[icon] = body.icon
                row[description] = body.description
            } get CollectionsTable.id
        }
        return UUID.fromString(insertedId.value.toString())
    }

}
