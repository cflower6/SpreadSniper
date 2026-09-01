package infrastructure.execution

import configurations.AppConfig
import domain.interfaces.ArbitrageExecutionStrategy
import domain.models.ArbitrageExecutionResult
import domain.models.ArbitrageOpportunity
import domain.models.TradeLeg
import infrastructure.blockchain.Web3Utils
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import java.math.BigInteger

class WalletFundedArbitrageExecutionStrategy(
    private val web3Utils: Web3Utils,
    private val routerRegistry: RouterRegistry,
    private val tradeExecutor: TradeExecutor
) : ArbitrageExecutionStrategy {

    private val logger =
        LoggerFactory.getLogger(WalletFundedArbitrageExecutionStrategy::class.java)

    override suspend fun execute(
        opportunity: ArbitrageOpportunity
    ): ArbitrageExecutionResult {

        val web3 =
            web3Utils.getWeb3ForChain(
                opportunity.chain
            )

        val recipient =
            tradeExecutor.getAddress()
                ?: return ArbitrageExecutionResult.Failed(
                    reason = "Wallet address unavailable"
                )

        logger.info(
            "Executing opportunity {} | {} -> {}",
            opportunity.opportunityKey.value,
            opportunity.firstLeg.dex,
            opportunity.secondLeg.dex
        )

        /*
         * FIRST LEG
         */
        val firstResult =
            executeLeg(
                web3 = web3,
                chain = opportunity.chain,
                leg = opportunity.firstLeg,
                recipient = recipient
            )

        when (firstResult) {

            is TradeResult.Failed -> {
                logger.error(
                    "First leg failed: {}",
                    firstResult.reason
                )

                return ArbitrageExecutionResult.Failed(
                    reason = "First leg failed: ${firstResult.reason}"
                )
            }

            is TradeResult.DryRun -> {
                /*
                 * Simulate second leg too so the full route
                 * gets validated during dry-run.
                 */
                val secondResult =
                    executeLeg(
                        web3 = web3,
                        chain = opportunity.chain,
                        leg = opportunity.secondLeg,
                        recipient = recipient
                    )

                logger.info(
                    "[DRY-RUN] First leg: {} {} -> {}",
                    opportunity.firstLeg.dex,
                    opportunity.firstLeg.tokenIn.symbol,
                    opportunity.firstLeg.tokenOut.symbol
                )

                logger.info(
                    "[DRY-RUN] Second leg: {} {} -> {}",
                    opportunity.secondLeg.dex,
                    opportunity.secondLeg.tokenIn.symbol,
                    opportunity.secondLeg.tokenOut.symbol
                )

                return when (secondResult) {
                    is TradeResult.Failed ->
                        ArbitrageExecutionResult.Failed(
                            reason = "Dry-run second leg failed: ${secondResult.reason}"
                        )

                    else ->
                        ArbitrageExecutionResult.DryRun
                }
            }

            is TradeResult.Success -> {
                logger.info(
                    "First leg submitted successfully: {}",
                    firstResult.txHash
                )
            }
        }

        /*
         * SECOND LEG
         */
        val secondResult =
            executeLeg(
                web3 = web3,
                chain = opportunity.chain,
                leg = opportunity.secondLeg,
                recipient = recipient
            )

        return when (secondResult) {

            is TradeResult.Success -> {

                logger.info(
                    "Arbitrage completed | First tx: {} | Second tx: {}",
                    firstResult.txHash,
                    secondResult.txHash
                )

                ArbitrageExecutionResult.Success(
                    buyTxHash = firstResult.txHash,
                    sellTxHash = secondResult.txHash,
                    actualProfitUsd = opportunity.estimatedNetProfitUsd
                )
            }

            is TradeResult.Failed -> {

                logger.error(
                    "SECOND LEG FAILED after successful first leg!"
                )

                logger.error(
                    "First tx: {}",
                    firstResult.txHash
                )

                ArbitrageExecutionResult.Failed(
                    reason = "Second leg failed: ${secondResult.reason}",
                    buyTxHash = firstResult.txHash
                )
            }

            is TradeResult.DryRun -> {
                ArbitrageExecutionResult.DryRun
            }
        }
    }

    private fun executeLeg(
        web3: Web3j,
        chain: domain.models.Chain,
        leg: TradeLeg,
        recipient: String
    ): TradeResult {

        val router =
            routerRegistry.routerFor(
                chain = chain,
                dex = leg.dex
            )

        val path =
            listOf(
                leg.tokenIn.address,
                leg.tokenOut.address
            )

        val minimumOutput =
            SwapEncoder.calculateMinOutput(
                leg.amountOutRaw
            )

        logger.info(
            "Executing leg | DEX: {} | {} -> {} | amountInRaw: {} | expectedOutRaw: {}",
            leg.dex,
            leg.tokenIn.symbol,
            leg.tokenOut.symbol,
            leg.amountInRaw,
            leg.amountOutRaw
        )

        val encodedSwap =
            SwapEncoder.encodeUniV2Swap(
                amountIn = leg.amountInRaw,
                amountOutMin = minimumOutput,
                path = path,
                recipient = recipient
            )

        return tradeExecutor.executeSwap(
            web3 = web3,
            routerAddress = router,
            encodedSwapData = encodedSwap,
            gasLimit = BigInteger.valueOf(
                AppConfig.gasLimit
            )
        )
    }
}