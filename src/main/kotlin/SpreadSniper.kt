import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

const val RPC_URL = "https://eth-mainnet.g.alchemy.com/v2/{update}"

fun main() = runBlocking {
    val web3 = Web3j.build(HttpService(RPC_URL))
    val threshold = 0.002
    val amountIn = BigInteger("1000000000000000000") // 1 DAI
    val client = HttpClient(CIO)

    while (true) {
        try {
            val uniPrice = DexOracle.getUniswapPrice(web3, amountIn)
            val sushiPrice = DexOracle.getSushiPrice(web3, amountIn)

            if (uniPrice == null || sushiPrice == null) {
                println("⚠️ Could not fetch prices")
            } else {
                TriggerService.evaluateAndTrigger(uniPrice, sushiPrice, threshold, client)
            }
        } catch (e: Exception) {
            println("🔥 Error: ${e.message}")
        }

        delay(15000) // Check every 5 seconds
    }
}