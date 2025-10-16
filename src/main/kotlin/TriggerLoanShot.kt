import configurations.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true })
    }
}

@Serializable
data class TriggerPayload(
    val pair: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val profit: Double
)

suspend fun sendToLoanShot(payload: TriggerPayload): Boolean {
    return try {
        val response = httpClient.post(AppConfig.loanshotUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        println("✅ LoanShot response: ${response.status}")
        response.status == HttpStatusCode.OK
    } catch (e: Exception) {
        println("❌ Failed to trigger LoanShot: ${e.message}")
        false
    }
}