package application.orchestrator

import configurations.AppConfig
import domain.events.EventBus
import domain.events.OpportunityEvent
import domain.interfaces.DexQuoter
import domain.models.ArbitrageOpportunity
import domain.models.DexPair
import domain.models.DexRoute
import domain.models.TradeLeg
import domain.models.selectBestOpportunity
import infrastructure.blockchain.GasEstimator
import infrastructure.blockchain.Web3Utils
import infrastructure.blockchain.currentBlockCtx
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import utils.toRawAmount
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class OpportunityOrchestrator(
    private val eventBus: EventBus
) {

    private val logger =
        LoggerFactory.getLogger(OpportunityOrchestrator::class.java)

    suspend fun processOpportunities(
        dexPairs: List<DexPair>,
        web3: Web3j,
        quoters: List<DexQuoter>,
        lastEmailMs: Long
    ): Long {

        logger.info("Processing opportunities...")
        logger.info(
            "Checking {} configured pairs with {} quoters",
            dexPairs.size,
            quoters.size
        )

        val gasCostUsd =
            if (AppConfig.dynamicGasEnabled) {
                GasEstimator.estimateGasCostUsd(
                    web3,
                    AppConfig.gasLimit
                )
            } else {
                AppConfig.gasCostEstimate
            }

        val opportunities =
            detectPairsInParallel(
                dexPairs = dexPairs,
                web3 = web3,
                quoters = quoters,
                gasCostUsd = gasCostUsd
            )

        logger.info(
            "Best opportunities: {}",
            opportunities
        )

        val bestOpportunity =
            selectBestOpportunity(
                opportunities = opportunities,
                minProfitUsd = AppConfig.profitThresholdUSD
            )

        if (bestOpportunity == null) {
            logNoOpportunities(opportunities)
            return lastEmailMs
        }

        val now =
            System.currentTimeMillis()

        val updatedLastEmailMs =
            if (
                now - lastEmailMs >
                AppConfig.emailCooldownMs
            ) {
                eventBus.emit(
                    OpportunityEvent.Notification(
                        bestOpportunity
                    )
                )

                now
            } else {
                lastEmailMs
            }

        eventBus.emit(
            OpportunityEvent.OpportunityFound(
                bestOpportunity
            )
        )

        eventBus.emit(
            OpportunityEvent.ExecuteOpportunity(
                bestOpportunity
            )
        )

        return updatedLastEmailMs
    }

    private suspend fun detectPairsInParallel(
        dexPairs: List<DexPair>,
        web3: Web3j,
        quoters: List<DexQuoter>,
        gasCostUsd: Double
    ): List<ArbitrageOpportunity> =
        coroutineScope {

            dexPairs
                .map { pair ->
                    async {
                        try {
                            detectRoundTripOpportunity(
                                pair = pair,
                                web3 = web3,
                                quoters = quoters,
                                gasCostUsd = gasCostUsd
                            )
                        } catch (e: Exception) {
                            logger.warn(
                                "Error detecting {}: {}",
                                pair.label,
                                e.message,
                                e
                            )

                            null
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }

    private fun detectRoundTripOpportunity(
        pair: DexPair,
        web3: Web3j,
        quoters: List<DexQuoter>,
        gasCostUsd: Double
    ): ArbitrageOpportunity? {

        val quoterA =
            quoters.firstOrNull {
                it.dex == pair.routeA.dex
            } ?: return null

        val quoterB =
            quoters.firstOrNull {
                it.dex == pair.routeB.dex
            } ?: return null

        val blockCtx =
            currentBlockCtx(web3)

        val block =
            blockCtx.param

        val blockNumber =
            blockCtx.number

        /*
         * Actual execution size ladder.
         */
        val tradeSizes =
            AppConfig.tradeSizes
                .mapNotNull { size ->
                    runCatching {
                        size to toRawAmount(
                            amount = size,
                            token = pair.tokenIn
                        )
                    }.getOrNull()
                }
                .sortedBy {
                    it.second
                }

        if (tradeSizes.isEmpty()) {
            return null
        }

        /*
         * --------------------------------------------------
         * STAGE 1:
         * Market prefilter
         *
         * IMPORTANT:
         * This is NOT the liquidity baseline.
         * --------------------------------------------------
         */

        val probeAmountInRaw =
            probeAmountRaw(pair)

        val probe =
            probeMarket(
                pair = pair,
                web3 = web3,
                quoterA = quoterA,
                quoterB = quoterB,
                amountInRaw = probeAmountInRaw,
                block = block
            )
                ?: return null

        if (
            probe.spreadBps <
            AppConfig.prefilterMinSpreadBps
        ) {

            logger.debug(
                "[PREFILTER SKIP] {} | spread={} bps < {} bps",
                pair.label,
                "%.4f".format(probe.spreadBps),
                AppConfig.prefilterMinSpreadBps
            )

            return null
        }

        logger.info(
            "[PREFILTER PASS] {} | spread={} bps",
            pair.label,
            "%.4f".format(probe.spreadBps)
        )

        /*
         * --------------------------------------------------
         * STAGE 2:
         * Smallest executable-size liquidity baseline
         * --------------------------------------------------
         */

        val smallestSize =
            tradeSizes.first()

        val baselineAmountInRaw =
            smallestSize.second

        val baselineAB =
            simulateRoundTrip(
                web3 = web3,
                firstRoute = pair.routeA,
                secondRoute = pair.routeB,
                firstQuoter = quoterA,
                secondQuoter = quoterB,
                amountInRaw = baselineAmountInRaw,
                block = block
            )

        val baselineBA =
            simulateRoundTrip(
                web3 = web3,
                firstRoute = pair.routeB,
                secondRoute = pair.routeA,
                firstQuoter = quoterB,
                secondQuoter = quoterA,
                amountInRaw = baselineAmountInRaw,
                block = block
            )

        val baselineABValues =
            baselineAB?.let {
                DirectionBaseline(
                    firstLegInRaw =
                        it.amountInRaw,

                    firstLegOutRaw =
                        it.intermediateAmountRaw,

                    secondLegInRaw =
                        it.intermediateAmountRaw,

                    secondLegOutRaw =
                        it.finalAmountRaw
                )
            }

        val baselineBAValues =
            baselineBA?.let {
                DirectionBaseline(
                    firstLegInRaw =
                        it.amountInRaw,

                    firstLegOutRaw =
                        it.intermediateAmountRaw,

                    secondLegInRaw =
                        it.intermediateAmountRaw,

                    secondLegOutRaw =
                        it.finalAmountRaw
                )
            }

        val candidates =
            mutableListOf<ArbitrageOpportunity>()

        /*
         * Smallest trade can itself be profitable.
         */
        listOfNotNull(
            baselineAB,
            baselineBA
        )
            .filter {
                it.profitRaw > BigInteger.ZERO
            }
            .mapNotNull {
                toOpportunity(
                    pair = pair,
                    roundTrip = it,
                    gasCostUsd = gasCostUsd,
                    blockNumber = blockNumber
                )
            }
            .forEach {
                candidates += it
            }

        /*
         * --------------------------------------------------
         * STAGE 3:
         * Walk larger sizes until liquidity degrades
         * --------------------------------------------------
         */

        var scanAB =
            baselineABValues != null

        var scanBA =
            baselineBAValues != null

        for (
        (humanSize, amountInRaw)
        in tradeSizes.drop(1)
        ) {

            /*
             * ==============================
             * A -> B
             * ==============================
             */

            if (
                scanAB &&
                baselineABValues != null
            ) {

                val firstOut =
                    quoterA.quote(
                        web3 = web3,
                        route = pair.routeA,
                        amountIn = amountInRaw,
                        block = block
                    )

                if (firstOut == null) {

                    scanAB = false

                } else {

                    val firstImpact =
                        priceImpactBps(
                            baselineAmountIn =
                                baselineABValues.firstLegInRaw,

                            baselineAmountOut =
                                baselineABValues.firstLegOutRaw,

                            currentAmountIn =
                                amountInRaw,

                            currentAmountOut =
                                firstOut
                        )

                    if (
                        firstImpact >
                        AppConfig.maxPriceImpactBps
                    ) {

                        logger.info(
                            "[LIQUIDITY CUTOFF] {} | {} -> {} | size={} {} | first-leg impact={} bps",
                            pair.label,
                            quoterA.dex,
                            quoterB.dex,
                            humanSize,
                            pair.tokenIn.symbol,
                            "%.2f".format(firstImpact)
                        )

                        scanAB = false

                    } else {

                        val finalOut =
                            quoterB.quote(
                                web3 = web3,
                                route =
                                    pair.routeB.reversed(),
                                amountIn = firstOut,
                                block = block
                            )

                        if (finalOut != null) {

                            val secondImpact =
                                priceImpactBps(
                                    baselineAmountIn =
                                        baselineABValues.secondLegInRaw,

                                    baselineAmountOut =
                                        baselineABValues.secondLegOutRaw,

                                    currentAmountIn =
                                        firstOut,

                                    currentAmountOut =
                                        finalOut
                                )

                            if (
                                secondImpact >
                                AppConfig.maxPriceImpactBps
                            ) {

                                logger.info(
                                    "[LIQUIDITY CUTOFF] {} | {} -> {} | size={} {} | second-leg impact={} bps",
                                    pair.label,
                                    quoterA.dex,
                                    quoterB.dex,
                                    humanSize,
                                    pair.tokenIn.symbol,
                                    "%.2f".format(secondImpact)
                                )

                                scanAB = false

                            } else {

                                val roundTrip =
                                    RoundTripQuote(
                                        firstDex =
                                            quoterA.dex,

                                        secondDex =
                                            quoterB.dex,

                                        amountInRaw =
                                            amountInRaw,

                                        intermediateAmountRaw =
                                            firstOut,

                                        finalAmountRaw =
                                            finalOut
                                    )

                                if (
                                    roundTrip.profitRaw >
                                    BigInteger.ZERO
                                ) {
                                    toOpportunity(
                                        pair = pair,
                                        roundTrip = roundTrip,
                                        gasCostUsd = gasCostUsd,
                                        blockNumber = blockNumber
                                    )
                                        ?.let {
                                            candidates += it
                                        }
                                }
                            }
                        }
                    }
                }
            }

            /*
             * ==============================
             * B -> A
             * ==============================
             */

            if (
                scanBA &&
                baselineBAValues != null
            ) {

                val firstOut =
                    quoterB.quote(
                        web3 = web3,
                        route = pair.routeB,
                        amountIn = amountInRaw,
                        block = block
                    )

                if (firstOut == null) {

                    scanBA = false

                } else {

                    val firstImpact =
                        priceImpactBps(
                            baselineAmountIn =
                                baselineBAValues.firstLegInRaw,

                            baselineAmountOut =
                                baselineBAValues.firstLegOutRaw,

                            currentAmountIn =
                                amountInRaw,

                            currentAmountOut =
                                firstOut
                        )

                    if (
                        firstImpact >
                        AppConfig.maxPriceImpactBps
                    ) {

                        logger.info(
                            "[LIQUIDITY CUTOFF] {} | {} -> {} | size={} {} | first-leg impact={} bps",
                            pair.label,
                            quoterB.dex,
                            quoterA.dex,
                            humanSize,
                            pair.tokenIn.symbol,
                            "%.2f".format(firstImpact)
                        )

                        scanBA = false

                    } else {

                        val finalOut =
                            quoterA.quote(
                                web3 = web3,
                                route =
                                    pair.routeA.reversed(),
                                amountIn = firstOut,
                                block = block
                            )

                        if (finalOut != null) {

                            val secondImpact =
                                priceImpactBps(
                                    baselineAmountIn =
                                        baselineBAValues.secondLegInRaw,

                                    baselineAmountOut =
                                        baselineBAValues.secondLegOutRaw,

                                    currentAmountIn =
                                        firstOut,

                                    currentAmountOut =
                                        finalOut
                                )

                            if (
                                secondImpact >
                                AppConfig.maxPriceImpactBps
                            ) {

                                logger.info(
                                    "[LIQUIDITY CUTOFF] {} | {} -> {} | size={} {} | second-leg impact={} bps",
                                    pair.label,
                                    quoterB.dex,
                                    quoterA.dex,
                                    humanSize,
                                    pair.tokenIn.symbol,
                                    "%.2f".format(secondImpact)
                                )

                                scanBA = false

                            } else {

                                val roundTrip =
                                    RoundTripQuote(
                                        firstDex =
                                            quoterB.dex,

                                        secondDex =
                                            quoterA.dex,

                                        amountInRaw =
                                            amountInRaw,

                                        intermediateAmountRaw =
                                            firstOut,

                                        finalAmountRaw =
                                            finalOut
                                    )

                                if (
                                    roundTrip.profitRaw >
                                    BigInteger.ZERO
                                ) {
                                    toOpportunity(
                                        pair = pair,
                                        roundTrip = roundTrip,
                                        gasCostUsd = gasCostUsd,
                                        blockNumber = blockNumber
                                    )
                                        ?.let {
                                            candidates += it
                                        }
                                }
                            }
                        }
                    }
                }
            }

            if (!scanAB && !scanBA) {
                break
            }
        }

        return candidates
            .maxByOrNull {
                it.estimatedNetProfitUsd
            }
    }

    /*
     * ------------------------------------------------------
     * Probe sizing
     * ------------------------------------------------------
     */

    private fun probeAmountRaw(
        pair: DexPair
    ): BigInteger {

        val humanAmount =
            when {

                pair.tokenIn.symbol
                        in AppConfig.stableCoins ->
                    AppConfig.probeSizeUsd

                pair.tokenIn.symbol.equals(
                    "WETH",
                    ignoreCase = true
                ) ->
                    AppConfig.probeSizeWeth

                else ->
                    AppConfig.tradeSizes.first()
            }

        return toRawAmount(
            amount = humanAmount,
            token = pair.tokenIn
        )
    }

    /*
     * ------------------------------------------------------
     * Cheap market spread probe
     * ------------------------------------------------------
     */

    private fun probeMarket(
        pair: DexPair,
        web3: Web3j,
        quoterA: DexQuoter,
        quoterB: DexQuoter,
        amountInRaw: BigInteger,
        block: DefaultBlockParameter
    ): MarketProbe? {

        val routeAOut =
            quoterA.quote(
                web3 = web3,
                route = pair.routeA,
                amountIn = amountInRaw,
                block = block
            ) ?: return null

        val routeBOut =
            quoterB.quote(
                web3 = web3,
                route = pair.routeB,
                amountIn = amountInRaw,
                block = block
            ) ?: return null

        val spreadBps =
            relativeSpreadBps(
                routeAOut,
                routeBOut
            )

        logger.info(
            "[PREFILTER] {} | {}={} | {}={} | spread={} bps",
            pair.label,
            quoterA.dex,
            routeAOut,
            quoterB.dex,
            routeBOut,
            "%.4f".format(spreadBps)
        )

        return MarketProbe(
            amountInRaw = amountInRaw,
            routeAOutRaw = routeAOut,
            routeBOutRaw = routeBOut,
            spreadBps = spreadBps
        )
    }

    private fun relativeSpreadBps(
        quoteA: BigInteger,
        quoteB: BigInteger
    ): Double {

        if (
            quoteA <= BigInteger.ZERO ||
            quoteB <= BigInteger.ZERO
        ) {
            return 0.0
        }

        val low =
            quoteA.min(quoteB)

        val high =
            quoteA.max(quoteB)

        return BigDecimal(
            high - low
        )
            .divide(
                BigDecimal(low),
                18,
                RoundingMode.HALF_UP
            )
            .multiply(
                BigDecimal("10000")
            )
            .toDouble()
    }

    private fun priceImpactBps(
        baselineAmountIn: BigInteger,
        baselineAmountOut: BigInteger,
        currentAmountIn: BigInteger,
        currentAmountOut: BigInteger
    ): Double {

        if (
            baselineAmountIn <= BigInteger.ZERO ||
            baselineAmountOut <= BigInteger.ZERO ||
            currentAmountIn <= BigInteger.ZERO ||
            currentAmountOut <= BigInteger.ZERO
        ) {
            return Double.POSITIVE_INFINITY
        }

        val baselineRate =
            BigDecimal(
                baselineAmountOut
            )
                .divide(
                    BigDecimal(
                        baselineAmountIn
                    ),
                    30,
                    RoundingMode.HALF_UP
                )

        val currentRate =
            BigDecimal(
                currentAmountOut
            )
                .divide(
                    BigDecimal(
                        currentAmountIn
                    ),
                    30,
                    RoundingMode.HALF_UP
                )

        if (currentRate >= baselineRate) {
            return 0.0
        }

        return baselineRate
            .subtract(currentRate)
            .divide(
                baselineRate,
                18,
                RoundingMode.HALF_UP
            )
            .multiply(
                BigDecimal("10000")
            )
            .toDouble()
    }

    private fun simulateRoundTrip(
        web3: Web3j,
        firstRoute: DexRoute,
        secondRoute: DexRoute,
        firstQuoter: DexQuoter,
        secondQuoter: DexQuoter,
        amountInRaw: BigInteger,
        block: DefaultBlockParameter
    ): RoundTripQuote? {

        val intermediateAmount =
            firstQuoter.quote(
                web3 = web3,
                route = firstRoute,
                amountIn = amountInRaw,
                block = block
            ) ?: return null

        val finalAmount =
            secondQuoter.quote(
                web3 = web3,
                route =
                    secondRoute.reversed(),
                amountIn =
                    intermediateAmount,
                block = block
            ) ?: return null

        return RoundTripQuote(
            firstDex =
                firstQuoter.dex,

            secondDex =
                secondQuoter.dex,

            amountInRaw =
                amountInRaw,

            intermediateAmountRaw =
                intermediateAmount,

            finalAmountRaw =
                finalAmount
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun toOpportunity(
        pair: DexPair,
        roundTrip: RoundTripQuote,
        gasCostUsd: Double,
        blockNumber: BigInteger
    ): ArbitrageOpportunity? {

        val amountInHuman =
            Web3Utils()
                .toHuman(
                    roundTrip.amountInRaw,
                    pair.tokenIn
                )
                .toDouble()

        val intermediateHuman =
            Web3Utils()
                .toHuman(
                    roundTrip.intermediateAmountRaw,
                    pair.tokenOut
                )
                .toDouble()

        val finalAmountHuman =
            Web3Utils()
                .toHuman(
                    roundTrip.finalAmountRaw,
                    pair.tokenIn
                )
                .toDouble()

        if (
            !amountInHuman.isFinite() ||
            !intermediateHuman.isFinite() ||
            !finalAmountHuman.isFinite()
        ) {
            return null
        }

        if (amountInHuman <= 0.0) {
            return null
        }

        val grossProfitInInputToken =
            finalAmountHuman -
                    amountInHuman

        if (
            grossProfitInInputToken <= 0.0
        ) {
            return null
        }

        val grossProfitUsd =
            tokenValueUsd(
                symbol =
                    pair.tokenIn.symbol,
                amount =
                    grossProfitInInputToken
            )
                ?: return null

        /*
         * Router quote already incorporates
         * pool fee mechanics.
         */
        val estimatedFeesUsd =
            0.0

        val netProfitUsd =
            grossProfitUsd -
                    gasCostUsd

        val spreadBps =
            (
                    grossProfitInInputToken /
                            amountInHuman
                    ) * 10_000.0

        val firstLeg =
            TradeLeg(
                dex =
                    roundTrip.firstDex,

                tokenIn =
                    pair.tokenIn,

                tokenOut =
                    pair.tokenOut,

                amountInRaw =
                    roundTrip.amountInRaw,

                amountOutRaw =
                    roundTrip.intermediateAmountRaw
            )

        val secondLeg =
            TradeLeg(
                dex =
                    roundTrip.secondDex,

                tokenIn =
                    pair.tokenOut,

                tokenOut =
                    pair.tokenIn,

                amountInRaw =
                    roundTrip.intermediateAmountRaw,

                amountOutRaw =
                    roundTrip.finalAmountRaw
            )

        return ArbitrageOpportunity(
            pair = pair,
            firstLeg = firstLeg,
            secondLeg = secondLeg,

            grossProfitUsd =
                grossProfitUsd,

            estimatedGasUsd =
                gasCostUsd,

            estimatedFeesUsd =
                estimatedFeesUsd,

            estimatedNetProfitUsd =
                netProfitUsd,

            grossSpreadBps =
                spreadBps,

            blockNumber =
                blockNumber,

            observedAt =
                Clock.System.now()
        )
    }

    private fun tokenValueUsd(
        symbol: String,
        amount: Double
    ): Double? =
        when {

            symbol in AppConfig.stableCoins ->
                amount

            symbol.equals(
                "WETH",
                ignoreCase = true
            ) ->
                amount *
                        AppConfig.ethPriceUsd

            else ->
                null
        }

    private fun logNoOpportunities(
        opportunities: List<ArbitrageOpportunity>
    ) {

        if (opportunities.isEmpty()) {
            logger.info(
                "No profitable round-trip opportunities detected"
            )
            return
        }

        logger.info(
            "No opportunities above threshold. Candidates: {}",
            opportunities.joinToString {
                "${it.firstLeg.dex} -> ${it.secondLeg.dex} = " +
                        "$${"%.4f".format(it.estimatedNetProfitUsd)}"
            }
        )
    }

    private data class MarketProbe(
        val amountInRaw: BigInteger,
        val routeAOutRaw: BigInteger,
        val routeBOutRaw: BigInteger,
        val spreadBps: Double
    )

    private data class DirectionBaseline(
        val firstLegInRaw: BigInteger,
        val firstLegOutRaw: BigInteger,
        val secondLegInRaw: BigInteger,
        val secondLegOutRaw: BigInteger
    )
}