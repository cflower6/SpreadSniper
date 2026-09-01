package infrastructure.http

import application.health.HealthState
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

class HttpServer(
    private val port: Int,
    private val healthState: HealthState
) {

    private var server: ApplicationEngine? = null

    fun start() {

        if (server != null) {
            return
        }

        server =
            embeddedServer(
                factory = Netty,
                port = port,
                host = "0.0.0.0"
            ) {

                install(ContentNegotiation) {
                    json(
                        Json {
                            prettyPrint = true
                            ignoreUnknownKeys = true
                            encodeDefaults = true
                        }
                    )
                }

                routing {
                    configureRoutes(
                        healthState
                    )
                }
            }

        server?.start(
            wait = false
        )
    }

    fun stop() {
        server?.stop(
            gracePeriodMillis = 1_000,
            timeoutMillis = 5_000
        )

        server = null
    }
}