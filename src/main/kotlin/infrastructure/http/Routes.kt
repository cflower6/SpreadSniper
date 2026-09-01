package infrastructure.http

import application.health.HealthState
import configurations.AppConfig
import infrastructure.http.dto.HealthResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.configureRoutes(
    healthState: HealthState
) {

    get("/") {
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "service" to "SpreadSniper",
                "status" to "running"
            )
        )
    }

    get("/health") {

        val scannerRunning =
            healthState.scannerRunning.get()

        val redisConnected =
            healthState.redisConnected.get()

        val healthy =
            scannerRunning &&
                    redisConnected

        val response =
            HealthResponse(
                status =
                    if (healthy) "UP"
                    else "DEGRADED",

                service =
                    "SpreadSniper",

                scanner =
                    if (scannerRunning) "UP"
                    else "DOWN",

                redis =
                    if (redisConnected) "UP"
                    else "DOWN",

                markets =
                    healthState.marketCount.get(),

                executionEnabled =
                    AppConfig.executionEnabled,

                lastProcessedBlock =
                    healthState
                        .lastProcessedBlock
                        .get()
                        .takeIf { it > 0 },

                lastScanDurationMs =
                    healthState
                        .lastScanDurationMs
                        .get()
                        .takeIf { it > 0 }
            )

        call.respond(
            if (healthy)
                HttpStatusCode.OK
            else
                HttpStatusCode.ServiceUnavailable,

            response
        )
    }
}