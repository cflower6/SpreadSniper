package utils

import configurations.AppConfig
import org.slf4j.LoggerFactory
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger

private val logger = LoggerFactory.getLogger("TradeExecutor")

/**
 * Result of a trade execution attempt.
 */
sealed class TradeResult {
    data class Success(
        val txHash: String,
        val gasUsed: BigInteger,
        val effectiveGasPrice: BigInteger
    ) : TradeResult()

    data class DryRun(
        val message: String
    ) : TradeResult()

    data class Failed(
        val reason: String,
        val exception: Exception? = null
    ) : TradeResult()
}

/**
 * Handles actual trade execution on-chain.
 *
 * SECURITY NOTES:
 * - Private key is loaded from environment variable only
 * - Dry-run mode is enabled by default
 * - All transactions are logged for audit
 */
object TradeExecutor {

    private var credentials: Credentials? = null

    /**
     * Initializes the executor with wallet credentials.
     * Must be called before executing trades.
     *
     * @return true if credentials loaded successfully
     */
    fun initialize(): Boolean {
        val privateKey = AppConfig.privateKey

        if (privateKey == null) {
            logger.warn("PRIVATE_KEY not configured - trade execution disabled")
            return false
        }

        return try {
            credentials = Credentials.create(privateKey)
            logger.info("Wallet initialized: {}", credentials?.address?.take(10) + "...")
            true
        } catch (e: Exception) {
            logger.error("Failed to load wallet credentials: {}", e.message)
            false
        }
    }

    /**
     * Returns the wallet address if initialized.
     */
    fun getAddress(): String? = credentials?.address

    /**
     * Checks if the executor is ready to execute trades.
     */
    fun isReady(): Boolean = credentials != null && AppConfig.executionEnabled

    /**
     * Executes a swap on a DEX router.
     *
     * @param web3 Web3j instance
     * @param routerAddress DEX router contract address
     * @param encodedSwapData ABI-encoded swap function call
     * @param value ETH value to send (for ETH->Token swaps)
     * @param gasLimit Gas limit for the transaction
     * @return TradeResult indicating success, dry-run, or failure
     */
    fun executeSwap(
        web3: Web3j,
        routerAddress: String,
        encodedSwapData: String,
        value: BigInteger = BigInteger.ZERO,
        gasLimit: BigInteger = BigInteger.valueOf(AppConfig.gasLimit)
    ): TradeResult {
        // Safety check: dry-run mode
        if (!AppConfig.executionEnabled) {
            logger.info("[DRY-RUN] Would execute swap on router {}", routerAddress)
            logger.info("[DRY-RUN] Data: {}", encodedSwapData.take(66) + "...")
            logger.info("[DRY-RUN] Value: {} wei, Gas limit: {}", value, gasLimit)
            return TradeResult.DryRun("Execution disabled - set EXECUTION_ENABLED=true to enable")
        }

        val creds = credentials ?: return TradeResult.Failed("Wallet not initialized")

        return try {
            logger.info("Executing swap on router {}", routerAddress)

            val chainId = web3.ethChainId().send().chainId.toLong()
            val txManager = RawTransactionManager(web3, creds, chainId)

            // Get current gas price
            val gasPrice = web3.ethGasPrice().send().gasPrice

            logger.info("Sending transaction: gasPrice={} gwei, gasLimit={}",
                gasPrice.divide(BigInteger.valueOf(1_000_000_000)), gasLimit)

            val response = txManager.sendTransaction(
                gasPrice,
                gasLimit,
                routerAddress,
                encodedSwapData,
                value
            )

            if (response.hasError()) {
                logger.error("Transaction failed: {}", response.error.message)
                return TradeResult.Failed(response.error.message)
            }

            val txHash = response.transactionHash
            logger.info("Transaction submitted: {}", txHash)

            // Wait for receipt
            val receipt = waitForReceipt(web3, txHash)

            if (receipt != null && receipt.isStatusOK) {
                logger.info("Transaction confirmed! Gas used: {}", receipt.gasUsed)
                val effectivePrice = try {
                    receipt.effectiveGasPrice as? BigInteger ?: gasPrice
                } catch (e: Exception) {
                    gasPrice
                }
                TradeResult.Success(
                    txHash = txHash,
                    gasUsed = receipt.gasUsed,
                    effectiveGasPrice = effectivePrice
                )
            } else {
                val status = receipt?.status ?: "unknown"
                logger.error("Transaction failed with status: {}", status)
                TradeResult.Failed("Transaction reverted: $status")
            }

        } catch (e: Exception) {
            logger.error("Swap execution failed: {}", e.message)
            TradeResult.Failed(e.message ?: "Unknown error", e)
        }
    }

    private fun waitForReceipt(web3: Web3j, txHash: String, maxAttempts: Int = 60): TransactionReceipt? {
        repeat(maxAttempts) { attempt ->
            try {
                val receipt = web3.ethGetTransactionReceipt(txHash).send()
                if (receipt.transactionReceipt.isPresent) {
                    return receipt.transactionReceipt.get()
                }
                Thread.sleep(1000) // Wait 1 second between checks
            } catch (e: Exception) {
                logger.debug("Waiting for receipt (attempt {}): {}", attempt + 1, e.message)
            }
        }
        logger.warn("Timeout waiting for transaction receipt: {}", txHash)
        return null
    }
}
