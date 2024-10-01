package com.stabledata.endpoint

import io.ktor.server.application.*
fun Application.configureSchemaRouting() {
    configureCreateCollectionRoute()
    configureUpdateCollectionRoute()
    configureDeleteCollectionRoute()

}
