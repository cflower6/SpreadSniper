package services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType


object DiscordNotifierService {
    private val client = HttpClient(CIO)

    suspend fun send(webhookUrl: String, message: String) {
        val escaped = message
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

        client.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"content":"$escaped"}""")
        }
    }
}