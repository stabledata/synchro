package com.stabledata

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

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