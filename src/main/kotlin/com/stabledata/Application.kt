package com.stabledata

import com.stabledata.endpoint.configureApplicationRouting
import com.stabledata.endpoint.configureChoresRouting
import com.stabledata.plugins.configureAuth
import com.stabledata.plugins.configureDocsRouting
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
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

fun main() {
    runBlocking {
        val port = envInt("PORT")
        val devPort = envInt("GRPC_PORT")
        val grpcPort = if (devPort > 0) devPort else port

        val grpcServer = NettyServerBuilder
            .forPort(grpcPort)
            .addService(GrpcService())
            .addService(ProtoReflectionService.newInstance())
            .build()

        launch(Dispatchers.IO) {
            grpcServer.start()
        }

        launch(Dispatchers.IO) {
            embeddedServer(
                Netty,
                port = port,
            ) {
                module()
            }.start(wait = true)
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
fun Application.configureErrorHandling() {

    val logger = KotlinLogging.logger {}

    install(StatusPages) {

        exception<SQLConflictException> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.Conflict, err.localizedMessage)
        }

        exception<SQLNotFoundException> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.NotFound, "Not found: $ref")
        }

        exception<Throwable> { call, err ->
            val ref = uuidString()
            logger.error { "${err.localizedMessage} ref: $ref" }
            call.respond(HttpStatusCode.InternalServerError, "Synchro failed. Ref: $ref")
        }
    }
}
