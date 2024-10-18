package com.stabledata

import com.stabledata.context.configureAuth
import com.stabledata.context.configureDocsRouting
import com.stabledata.endpoint.configureApplicationRouting
import com.stabledata.endpoint.configureChoresRouting
import com.stabledata.grpc.GrpcContextInterceptor
import com.stabledata.grpc.GrpcService
import com.stabledata.grpc.SchemaService
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

fun main() {
    runBlocking {
        val port = envInt("PORT")
        val devPort = envInt("GRPC_PORT")
        val grpcPort = if (devPort > 0) devPort else port

        val grpcServer = NettyServerBuilder
            .forPort(grpcPort)
            .intercept(GrpcContextInterceptor())
            .intercept(ExceptionHandlingInterceptor())
            .addService(GrpcService())
            .addService(SchemaService())
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
