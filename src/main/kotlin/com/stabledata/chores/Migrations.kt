package com.stabledata.chores

import com.stabledata.configureLogging
import com.stabledata.hikari
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Migrations {

    @JvmStatic
    fun main(args: Array<String>) {

        configureLogging()

        runBlocking {
            val task = args.firstOrNull() ?: "migrate"
            if (args.isNotEmpty()) {
                println("Running migration task: $task")
                when (task) {
                    "migrate" -> migrate()
                     "undo" -> undo() // flyway wants monies now for this
                    "repair" -> repair()
                    "create" -> create()
                    "help" -> printHelp()
                    else -> {
                        println("Unknown task: $task")
                        printHelp()
                    }

                }
            } else {
                printHelp()
            }

        }
    }

    fun printHelp () {
        println("To do flyway chores run './gradlew migrate --args [create|migrate|repair]'")
    }
}

fun create () {
    val migrationDir = File("src/main/resources/db/migration")

    if (!migrationDir.exists()) {
        migrationDir.mkdirs() // Create the directory if it doesn't exist
    }

    val migrationFiles = migrationDir.listFiles { _, name -> name.startsWith("V") && name.endsWith(".sql") }
    val latestVersion = migrationFiles?.mapNotNull { file ->
        Regex("""V(\d+)__.*\.sql""").find(file.name)?.groups?.get(1)?.value?.toInt()
    }?.maxOrNull() ?: 0

    val nextVersion = latestVersion + 1
    val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
    val migrationFileName = "V${nextVersion}__Migration_$timestamp.sql"
    val newMigrationFile = File(migrationDir, migrationFileName)

    newMigrationFile.writeText(
        """
        -- Migration V${nextVersion} (Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())})
      
    """.trimIndent()
    )

    println("Generated new migration file: ${newMigrationFile.path}")
}

fun flywayViaHikari (): Flyway {
    val ds = hikari()
    return Flyway.configure()
        .dataSource(ds)
        .load()
}

fun migrate() {
    flywayViaHikari().migrate()
}

fun repair() {
    flywayViaHikari().repair()
}

fun undo() {
    //  Flyway makes you pay for this now
    //  TODO: make a nice error with decent instructions (licence in env)
    //  instead of letting the paywall throw
   flywayViaHikari().undo()
}




