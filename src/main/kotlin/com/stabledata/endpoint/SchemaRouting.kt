package com.stabledata.endpoint

import com.stabledata.getLogger
import io.ktor.server.application.*
import org.slf4j.Logger

fun Application.configureSchemaRouting(logger: Logger = getLogger()) {
    configureCreateCollectionRoute(logger)
    configureUpdateCollectionRoute(logger)
}
