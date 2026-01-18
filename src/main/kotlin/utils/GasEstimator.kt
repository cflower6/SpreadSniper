package utils

import configurations.AppConfig
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.SocketTimeoutException

private val logger = LoggerFactory.getLogger("GasEstimator")

object GasEstimator {
    // Typical gas for a DEX arbitrage (2 swaps + overhead)
    private const val DEFAULT_GAS_LIMIT = 300_000L

    /**
     * Estimates the gas cost in USD for executing an arbitrage trade.
     * Uses retry logic for transient RPC failures.
     *
     * @param web3 Web3j instance to fetch current gas price
     * @param gasLimit Optional override for gas units (default 300k)
     * @return Estimated cost in USD, or fallback to static estimate on failure
     */
    fun estimateGasCostUsd(
        web3: Web3j,
        gasLimit: Long = DEFAULT_GAS_LIMIT
    ): Double {
        val gasPriceWei = withRetrySync(
            maxAttempts = 3,
            initialDelayMs = 50,
            maxDelayMs = 500,
            retryOn = { e -> e is IOException || e is SocketTimeoutException }
        ) {
            web3.ethGasPrice().send().gasPrice
        }

        if (gasPriceWei == null) {
            logger.warn("Failed to fetch gas price after retries, using static estimate")
            return AppConfig.gasCostEstimate
        }

        val gasCostWei = gasPriceWei.multiply(BigInteger.valueOf(gasLimit))

        // Convert wei to ETH (18 decimals)
        val gasCostEth = BigDecimal(gasCostWei)
            .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP)

        // Convert ETH to USD
        val gasCostUsd = gasCostEth.multiply(BigDecimal(AppConfig.ethPriceUsd)).toDouble()

        logger.debug("Gas estimate: {} gwei × {} units = \${} USD",
            gasPriceWei.divide(BigInteger.valueOf(1_000_000_000)),
            gasLimit,
            "%.4f".format(gasCostUsd))

        return gasCostUsd
    }

    /**
     * Fetches current gas price in gwei for logging/monitoring.
     */
    fun getGasPriceGwei(web3: Web3j): Double? {
        return try {
            val gasPriceWei = web3.ethGasPrice().send().gasPrice
            gasPriceWei.toDouble() / 1_000_000_000.0
        } catch (e: Exception) {
            logger.warn("Failed to fetch gas price: {}", e.message)
            null
        }
    }
}
