package org.example

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.math.BigInteger
import kotlinx.coroutines.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

const val RPC_URL = "https://spreadsheets.com/spreadsheets/"
const val UNISWAP_ROUTER = "0xERJIEWOJREIWJRO"
const val TOKEN_A = "0xur83u48302u4"
const val TOKEN_B = "0xA8u48302u4"
val AMOUNT = BigInteger("1000000000") // 100 USDC (6 decimals)
const val NODE_ENDPOINT = "http://localhost:3000"
const val PRICE_SPREAD_THRESHOULD = 0.002 // $0.002

suspend fun main() = coroutineScope {
    val web3 = Web3j.build(HttpService(RPC_URL))
    val client = HttpClient(CIO)

    while (true) {
        try {
            val priceUniswap = getMockPriceFromUniswap() // Replace with actual Web3 call
            val priceSushi = getMockPriceFromSushi() // Replace with actual Web3 call

            val spread = priceSushi - priceUniswap
            val response = client.get("$NODE_ENDPOINT/healthcheck") {
                contentType(ContentType.Application.Json)
            }
            println(response)
            if(spread >= PRICE_SPREAD_THRESHOULD) {
                println("\uD83D\uDFE2 Arbitrage Detected! Spread = $spread")

                val response = client.post("$NODE_ENDPOINT/trigger-flashloan") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"token":"DAI", "amount":"10000000"}""")
                }

                println("✅ Triggered flash loan. Status: ${response.status}")
            } else {
                println("🔴 No opportunity. Spread = $spread")
            }
        } catch (e: Exception) {
            println("⚠️ Error: ${e.message}")
        }

        delay(5000) // Check every 5 seconds
    }
}

// TEMP MOCKS — Replace with web3 Uniswap/Sushi `getAmountsOut()` calls
fun getMockPriceFromUniswap(): Double = 1.000
fun getMockPriceFromSushi(): Double = 1.002