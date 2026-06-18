package com.blindrunner.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*

fun main() {
    Database.init()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        install(CORS) {
            anyHost()
            allowCredentials = true
            allowNonSimpleContentTypes = true
        }

        routing()
    }.start(wait = true)
}
