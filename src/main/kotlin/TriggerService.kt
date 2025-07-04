import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

object TriggerService {
    const val NODE_ENDPOINT = "http://localhost:3000"
    //const val NODE_ENDPOINT = "https://loanshot.onrender.com"
    suspend fun evaluateAndTrigger(uniPrice: Double, sushiPrice: Double, threshold: Double, client: HttpClient): Boolean {
        val spread = sushiPrice - uniPrice
        val inverse = uniPrice - sushiPrice
        val formatted = formatSpread(spread, inverse)


        if(spread > threshold) {
            println("🟢 Opportunity (Sushi → Uni): $spread")

            val response = client.post("$NODE_ENDPOINT/trigger-flashloan") {
                contentType(ContentType.Application.Json)
                setBody("""{"token":"DAI", "amount":"10000000"}""")
            }

            println("✅ Triggered flash loan. Status: ${response.status}")
            return true
        }

        if (inverse > threshold) {
            println("🟢 Opportunity (Uni → Sushi): $inverse")

            val response = client.post("$NODE_ENDPOINT/trigger-flashloan") {
                contentType(ContentType.Application.Json)
                setBody("""{"token":"DAI", "amount":"10000000"}""")
            }

            println("✅ Triggered flash loan. Status: ${response.status}")
            return true
        }

        println(formatted)
        return false
    }

    private fun formatSpread(spread: Double, inverseSpread: Double): String {
        return """
            🔴 No opportunity.
                ➤ Spread:         ${"%.6f".format(spread)} (Sushi → Uni)
                ➤ Inverse Spread: ${"%.6f".format(inverseSpread)} (Uni → Sushi)
        """.trimIndent()
    }
}