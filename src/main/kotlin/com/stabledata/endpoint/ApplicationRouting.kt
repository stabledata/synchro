package com.stabledata.endpoint

import io.ktor.server.application.*
fun Application.configureApplicationRouting() {
    // access controls
    configureAccessCreateRoute()

    // schema
    configureCreateCollectionRoute()
    configureUpdateCollectionRoute()
    configureDeleteCollectionRoute()

}
