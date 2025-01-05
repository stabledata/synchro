package com.stabledata

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import com.stabledata.context.GrpcContextInterceptor
import com.stabledata.context.configureAuth
import com.stabledata.context.configureDocsRouting
import com.stabledata.endpoint.configureApplicationRouting
import com.stabledata.endpoint.configureChoresRouting
import com.stabledata.grpc.SchemaService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

fun main() {

    val envLogLevel = System.getenv("LOG_LEVEL") ?: "INFO"
    val context = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
    rootLogger.level = Level.toLevel(envLogLevel, Level.INFO)

    val logger = KotlinLogging.logger {}

    runBlocking {
        // Service can be run as both as HTTP or GRPC.
        // It can listen for both at the same time, but this
        // requires special deployment considerations
        // Locally, running nginx in front works, but have
        // yet to find a managed hosting solution where this
        // works gracefully -- cloud run was promising but didn't pan out
        val grpcPort = envIntOptional("GRPC_PORT")
        if (grpcPort !== null) {
            val grpcServer = NettyServerBuilder
                .forPort(grpcPort)
                .intercept(GrpcContextInterceptor())
                .intercept(ExceptionHandlingInterceptor())
                .addService(SchemaService())

                .addService(ProtoReflectionService.newInstance())
                .build()

            launch(Dispatchers.IO) {
                logger.debug { "Starting GRPC service on port $grpcPort" }
                grpcServer.start()
            }
        }

        val httpPort = envIntOptional("HTTP_PORT")
        if (httpPort !== null) {
            launch(Dispatchers.IO) {
                logger.debug { "Starting HTTP service on port $httpPort" }
                embeddedServer(
                    Netty,
                    port = httpPort,
                    host = "0.0.0.0"
                ) {
                    module()
                }.start(wait = true)
            }
        }
    }
}

fun Application.module() {

    // cors, auth etc. (below)
    configurePlugins()

    // db connection (if not unit testing)
    Database.connect(hikari())

    // configure routes.
    configureApplicationRouting()
    configureChoresRouting()
    configureDocsRouting()
}

fun Application.configurePlugins () {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
        gson {
            setPrettyPrinting()
        }
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
    }
    configureAuth()
    configureErrorHandling()
}
