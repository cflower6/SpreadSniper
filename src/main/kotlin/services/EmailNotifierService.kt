package services

import configurations.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object EmailNotifierService {

    private val apiKey: String by lazy {
        AppConfig.emailApiKey
    }

    private val toEmail: String by lazy {
        AppConfig.emailTo
    }

    private val fromEmail: String by lazy {
        AppConfig.emailFrom
    }

    private val http: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }
                )
            }
        }
    }

    @Serializable
    private data class SendEmailRequest(
        val from: String,
        val to: List<String>,
        val subject: String,
        val html: String
    )

    @Serializable
    private data class SendEmailResponse(
        val id: String? = null,
        @SerialName("error") val error: ResendError? = null
    )

    @Serializable
    private data class ResendError(
        val message: String? = null,
        val name: String? = null
    )

    suspend fun send(subject: String, body: String) {
        // keep your plain-text body but send as HTML safely
        val html = """
            <h3>$subject</h3>
            <pre style="font-family: ui-monospace, SFMono-Regular, Menlo, monospace; white-space: pre-wrap;">${escapeHtml(body)}</pre>
        """.trimIndent()

        val resp: SendEmailResponse = http.post("https://api.resend.com/emails") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            setBody(
                SendEmailRequest(
                    from = fromEmail,
                    to = listOf(toEmail),
                    subject = subject,
                    html = html
                )
            )
        }.body()

        if (resp.id == null) {
            val msg = resp.error?.message ?: "Unknown error"
            throw IllegalStateException("Resend email failed: $msg")
        }
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
