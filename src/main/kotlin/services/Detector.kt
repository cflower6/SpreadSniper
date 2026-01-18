package services

import interfaces.DexQuoter
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import registries.Token
import utils.currentBlockCtx
import java.math.BigInteger

private val logger = LoggerFactory.getLogger("Detector")

data class QuoteSnapshot(
    val dexName: String,
    val amountOutRaw: BigInteger,
    val feeRate: Double
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
        logger.debug("Detector block = {}", blockCtx.number)

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
                    logger.warn("{} returned null", q.name)
                    continue
                }

                logger.debug("{} quote = {} (fee: {}%)", q.name, out, q.feeRate * 100)
                quotes += QuoteSnapshot(q.name, out, q.feeRate)

            } catch (e: Exception) {
                logger.error("{} threw: {}", q.name, e.message)
            }
        }

        if (quotes.size < 2) {
            logger.debug("Not enough quotes ({})", quotes.size)
            return null
        }

        logger.debug("Detected spread with {} quotes", quotes.size)

        return DetectedSpread(
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            amountInRaw = amountInRaw,
            blockNumber = blockCtx.number,
            quotes = quotes
        )
    }
}

