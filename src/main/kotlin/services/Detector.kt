package services

import interfaces.DexQuoter
import org.web3j.protocol.Web3j
import registries.Token
import utils.currentBlockCtx
import java.math.BigInteger

data class QuoteSnapshot(
    val dexName: String,
    val amountOutRaw: BigInteger,
)

data class DetectedSpread(
    val tokenIn: Token,
    val tokenOut: Token,
    val amountInRaw: BigInteger,
    val blockNumber: BigInteger,
    val quotes: List<QuoteSnapshot>
)

object Detector {

    fun detectOnce(
        web3: Web3j,
        tokenIn: Token,
        tokenOut: Token,
        amountInRaw: BigInteger,
        quoters: List<DexQuoter>
    ): DetectedSpread? {
        val blockCtx = currentBlockCtx(web3)
        println("🔍 Detector block = ${blockCtx.number}")

        val quotes = mutableListOf<QuoteSnapshot>()

        for (q in quoters) {
            try {
                val out = q.quote(
                    web3 = web3,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = amountInRaw,
                    block = blockCtx.param
                )

                if (out == null) {
                    println("⚠️ ${q.name} returned null")
                    continue
                }

                println("✅ ${q.name} quote = $out")
                quotes += QuoteSnapshot(q.name, out)

            } catch (e: Exception) {
                println("❌ ${q.name} threw: ${e.message}")
            }
        }

        if (quotes.size < 2) {
            println("🚫 Not enough quotes (${quotes.size})")
            return null
        }

        println("<UNK> Detected spread ${quotes.size} quotes")

        return DetectedSpread(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountInRaw = amountInRaw,
            blockNumber = blockCtx.number,
            quotes = quotes
        )
    }
}

