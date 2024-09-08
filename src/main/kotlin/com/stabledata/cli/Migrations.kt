package com.stabledata.cli

import com.stabledata.configureLogging
import com.stabledata.hikari
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun create () {
    // Define the path to the migration directory
    val migrationDir = File("src/main/resources/db/migration")

    if (!migrationDir.exists()) {
        migrationDir.mkdirs() // Create the directory if it doesn't exist
    }

    // List all migration files and find the latest version
    val migrationFiles = migrationDir.listFiles { _, name -> name.startsWith("V") && name.endsWith(".sql") }
    val latestVersion = migrationFiles?.mapNotNull { file ->
        // Extract the version number from the file name, e.g., V1__Initial.sql
        Regex("""V(\d+)__.*\.sql""").find(file.name)?.groups?.get(1)?.value?.toInt()
    }?.maxOrNull() ?: 0

    // Increment the version number
    val nextVersion = latestVersion + 1

    // Generate the filename for the next migration
    val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
    val migrationFileName = "V${nextVersion}__Migration_$timestamp.sql"
    val newMigrationFile = File(migrationDir, migrationFileName)

    // Create the new migration file with a template comment
    newMigrationFile.writeText(
        """
        -- Migration V${nextVersion} (Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())})
        
        -- Add your migration SQL here
    """.trimIndent()
    )

    println("Generated new migration file: ${newMigrationFile.path}")
}

fun migrate() {
    val ds = hikari()
    Flyway.configure()
        .baselineVersion("16.4")  // Use the current PostgreSQL version
        .baselineOnMigrate(true)  // Baseline the migration
        .dataSource(ds)
        .load()
        .migrate()
}


object Migrations {

    @JvmStatic
    fun main(args: Array<String>) {

        configureLogging()

        runBlocking {
            val task = args.first()
            if (args.isNotEmpty()) {
                println("Running migration task: $task")
                when (task) {
                    "migrate" -> migrate()
                    "rollback" -> null
                    "create" -> create()
                    "help" -> null
                    else -> println("Unknown task: $task")
                }
            } else {
                println("No task provided. Run './gradlew migrate --args [create]'.")
            }

        }
    }
}

