package repositories

import configurations.AppConfig
import interfaces.ExecutorRepository
import models.ArbitrageOpportunity
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import services.QuoteSnapshot
import utils.SwapEncoder
import utils.TradeExecutor
import utils.TradeExecutor.initialize
import utils.TradeResult
import java.math.BigInteger

/**
 * Result of an arbitrage execution.
 */
data class ArbitrageResult(
    val success: Boolean,
    val buyTxHash: String? = null,
    val sellTxHash: String? = null,
    val actualProfit: Double? = null,
    val error: String? = null
)


/**
 * Executes complete arbitrage trades.
 *
 * Flow:
 * 1. Validate opportunity meets execution thresholds
 * 2. Execute buy on cheaper DEX
 * 3. Execute sell on expensive DEX
 * 4. Log results
 *
 * SAFETY:
 * - Disabled by default (EXECUTION_ENABLED=false)
 * - Requires minimum profit threshold
 * - Logs all actions for audit
 */
class ArbitrageExecutorRepository : ExecutorRepository {
    private val logger = LoggerFactory.getLogger("ArbitrageExecutorRepository")
    /**
     * Attempts to execute an arbitrage opportunity.
     *
     * @param web3 Web3j instance
     * @param opportunity The detected opportunity
     * @param buyQuote Quote from the buy DEX
     * @param sellQuote Quote from the sell DEX
     * @param buyRouterAddress Router address for buy DEX
     * @param sellRouterAddress Router address for sell DEX
     * @param tokenPath Token addresses [tokenIn, tokenOut]
     * @param amountIn Amount to trade (raw units)
     */
    override suspend fun execute(
        web3: Web3j,
        opportunity: ArbitrageOpportunity,
        buyQuote: QuoteSnapshot,
        sellQuote: QuoteSnapshot,
        buyRouterAddress: String,
        sellRouterAddress: String,
        tokenPath: List<String>,
        amountIn: BigInteger
    ): ArbitrageResult {
        //TODO: might need to make an endpoint that will set wallet creds
            if (AppConfig.executionEnabled) initialize()
            // Pre-flight checks
            if (!TradeExecutor.isReady()) {
                val reason = when {
                    AppConfig.privateKey == null -> "No wallet configured (PRIVATE_KEY not set)"
                    !AppConfig.executionEnabled -> "Execution disabled (set EXECUTION_ENABLED=true)"
                    else -> "TradeExecutor not initialized"
                }
                logger.info("[SKIP] {}", reason)
                return ArbitrageResult(success = false, error = reason)
            }

            // Check profit threshold
            if (opportunity.estimatedNetProfitUsd < AppConfig.minProfitForExecution) {
                logger.info("[SKIP] Profit \${} below execution threshold \${}",
                    "%.4f".format(opportunity.estimatedNetProfitUsd),
                    AppConfig.minProfitForExecution)
                return ArbitrageResult(
                    success = false,
                    error = "Profit below threshold"
                )
            }

            logger.info("=== EXECUTING ARBITRAGE ===")
            logger.info("Pair: ${opportunity.tokenIn} -> ${opportunity.tokenOut}")
            logger.info("Expected profit: \${}", "%.4f".format(opportunity.estimatedNetProfitUsd))
            logger.info("Buy on: {} @ {}", buyQuote.dexName, "%.2f".format(opportunity.buyQuote))
            logger.info("Sell on: {} @ {}", sellQuote.dexName, "%.2f".format(opportunity.sellQuote))

            // Calculate minimum outputs with slippage protection
            val minBuyOutput = SwapEncoder.calculateMinOutput(buyQuote.amountOutRaw)
            val minSellOutput = SwapEncoder.calculateMinOutput(sellQuote.amountOutRaw)

            // Step 1: Execute BUY (tokenIn -> tokenOut on cheap DEX)
            logger.info("Step 1: Buying on {}", buyQuote.dexName)
            val buyData = SwapEncoder.encodeUniV2Swap(
                amountIn = amountIn,
                amountOutMin = minBuyOutput,
                path = tokenPath,
                recipient = TradeExecutor.getAddress()!!
            )

            val buyResult = TradeExecutor.executeSwap(
                web3 = web3,
                routerAddress = buyRouterAddress,
                encodedSwapData = buyData,
                gasLimit = BigInteger.valueOf(AppConfig.gasLimit)
            )

            when (buyResult) {
                is TradeResult.DryRun -> {
                    logger.info("[DRY-RUN] Buy would execute: {}", buyData.take(66))
                    // In dry-run, simulate sell too
                    val sellData = SwapEncoder.encodeUniV2Swap(
                        amountIn = buyQuote.amountOutRaw, // Output from buy becomes input to sell
                        amountOutMin = minSellOutput,
                        path = tokenPath.reversed(), // Reverse path for sell
                        recipient = TradeExecutor.getAddress()!!
                    )
                    logger.info("[DRY-RUN] Sell would execute: {}", sellData.take(66))
                    return ArbitrageResult(success = true, error = "Dry run completed")
                }
                is TradeResult.Failed -> {
                    logger.error("Buy failed: {}", buyResult.reason)
                    return ArbitrageResult(success = false, error = "Buy failed: ${buyResult.reason}")
                }
                is TradeResult.Success -> {
                    logger.info("Buy succeeded: {}", buyResult.txHash)
                }
            }

            // Step 2: Execute SELL (tokenOut -> tokenIn on expensive DEX)
            // Note: In a real atomic arb, you'd use a flash loan or multicall
            logger.info("Step 2: Selling on {}", sellQuote.dexName)
            val sellData = SwapEncoder.encodeUniV2Swap(
                amountIn = buyQuote.amountOutRaw,
                amountOutMin = minSellOutput,
                path = tokenPath.reversed(),
                recipient = TradeExecutor.getAddress()!!
            )

            val sellResult = TradeExecutor.executeSwap(
                web3 = web3,
                routerAddress = sellRouterAddress,
                encodedSwapData = sellData,
                gasLimit = BigInteger.valueOf(AppConfig.gasLimit)
            )

            return when (sellResult) {
                is TradeResult.Success -> {
                    logger.info("=== ARBITRAGE COMPLETE ===")
                    logger.info("Buy tx: {}", buyResult.txHash)
                    logger.info("Sell tx: {}", sellResult.txHash)
                    ArbitrageResult(
                        success = true,
                        buyTxHash = buyResult.txHash,
                        sellTxHash = sellResult.txHash,
                        actualProfit = opportunity.estimatedNetProfitUsd // Estimated, actual may differ
                    )
                }
                is TradeResult.Failed -> {
                    logger.error("Sell failed after successful buy! Manual intervention needed.")
                    logger.error("Buy tx: {}", buyResult.txHash)
                    ArbitrageResult(
                        success = false,
                        buyTxHash = buyResult.txHash,
                        error = "Sell failed: ${sellResult.reason}"
                    )
                }
                is TradeResult.DryRun -> {
                    // Shouldn't happen if buy succeeded
                    ArbitrageResult(success = false, error = "Unexpected dry-run on sell")
                }
            }
        }
}