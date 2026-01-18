import models.DexPair
import org.junit.jupiter.api.Test
import registries.Token
import services.DetectedSpread
import services.QuoteSnapshot
import services.TriggerService
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TriggerServiceTest {

    // Test tokens
    private val weth = Token("WETH", "0x4200000000000000000000000000000000000006", 18)
    private val usdc = Token("USDC", "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", 6)

    // 1 WETH in raw units (18 decimals)
    private val oneWethRaw = BigInteger("1000000000000000000")

    @Test
    fun `toOpportunity returns null when less than 2 quotes`() {
        val snap = DetectedSpread(
            tokenIn = weth,
            tokenOut = usdc,
            amountInRaw = oneWethRaw,
            blockNumber = BigInteger.ONE,
            quotes = listOf(
                QuoteSnapshot("DEX1", BigInteger("3000000000"), 0.003) // 3000 USDC
            )
        )

        val result = TriggerService.toOpportunity(DexPair.BASE_AERO_UNI_WETH, snap, 0.25)
        assertNull(result)
    }

    @Test
    fun `toOpportunity calculates correct spread and profit`() {
        // Simulate: Buy at 3000 USDC, Sell at 3010 USDC
        // Spread = 10 USDC per WETH
        val snap = DetectedSpread(
            tokenIn = weth,
            tokenOut = usdc,
            amountInRaw = oneWethRaw,
            blockNumber = BigInteger.ONE,
            quotes = listOf(
                QuoteSnapshot("AERO", BigInteger("3000000000"), 0.003),  // 3000 USDC (worst = buy here)
                QuoteSnapshot("UNIV2", BigInteger("3010000000"), 0.003) // 3010 USDC (best = sell here)
            )
        )

        val result = TriggerService.toOpportunity(DexPair.BASE_AERO_UNI_WETH, snap, 0.25)

        assertNotNull(result)
        assertEquals(3000.0, result.buyPrice, 0.01)
        assertEquals(3010.0, result.sellPrice, 0.01)
        assertEquals(10.0, result.spread, 0.01)

        // Gross profit = 10 * 1 = 10 USDC
        // Buy fee = 0.003 * 3000 = 9 USDC
        // Sell fee = 0.003 * 3010 = 9.03 USDC
        // Total fees = 18.03 USDC
        // Net = 10 - 18.03 - 0.25 = -8.28 (unprofitable with these fees)
        assertTrue(result.adjustedProfit < 0)
    }

    @Test
    fun `toOpportunity uses per-DEX fee rates correctly`() {
        // Low fee DEX for buying, higher fee for selling
        val snap = DetectedSpread(
            tokenIn = weth,
            tokenOut = usdc,
            amountInRaw = oneWethRaw,
            blockNumber = BigInteger.ONE,
            quotes = listOf(
                QuoteSnapshot("STABLE_DEX", BigInteger("3000000000"), 0.0001), // 0.01% fee
                QuoteSnapshot("VOLATILE_DEX", BigInteger("3050000000"), 0.003) // 0.3% fee
            )
        )

        val result = TriggerService.toOpportunity(DexPair.BASE_AERO_UNI_WETH, snap, 0.25)

        assertNotNull(result)
        // Spread = 50 USDC
        assertEquals(50.0, result.spread, 0.01)

        // Buy fee = 0.0001 * 3000 = 0.30 USDC
        // Sell fee = 0.003 * 3050 = 9.15 USDC
        // Total fees = 9.45 USDC
        // Net = 50 - 9.45 - 0.25 = 40.30 (profitable!)
        assertTrue(result.adjustedProfit > 40.0)
        assertTrue(result.adjustedProfit < 41.0)
    }

    @Test
    fun `toOpportunity returns null for non-stablecoin output`() {
        // WETH -> WETH (not a stablecoin)
        val snap = DetectedSpread(
            tokenIn = weth,
            tokenOut = weth, // Not a stablecoin
            amountInRaw = oneWethRaw,
            blockNumber = BigInteger.ONE,
            quotes = listOf(
                QuoteSnapshot("DEX1", BigInteger("1000000000000000000"), 0.003),
                QuoteSnapshot("DEX2", BigInteger("1100000000000000000"), 0.003)
            )
        )

        val result = TriggerService.toOpportunity(DexPair.BASE_AERO_UNI_WETH, snap, 0.25)
        assertNull(result)
    }

    @Test
    fun `findBestOpportunity returns highest profit above threshold`() {
        val opp1 = TriggerService.Opportunity(
            pair = DexPair.BASE_AERO_UNI_WETH,
            buyPrice = 3000.0,
            sellPrice = 3005.0,
            spread = 5.0,
            adjustedProfit = 2.0
        )
        val opp2 = TriggerService.Opportunity(
            pair = DexPair.BASE_AERO_UNI_WETH,
            buyPrice = 3000.0,
            sellPrice = 3015.0,
            spread = 15.0,
            adjustedProfit = 8.0
        )
        val opp3 = TriggerService.Opportunity(
            pair = DexPair.BASE_AERO_UNI_WETH,
            buyPrice = 3000.0,
            sellPrice = 3010.0,
            spread = 10.0,
            adjustedProfit = 5.0
        )

        val result = TriggerService.findBestOpportunity(listOf(opp1, opp2, opp3), minProfitUSD = 1.0)

        assertNotNull(result)
        assertEquals(8.0, result.adjustedProfit)
    }

    @Test
    fun `findBestOpportunity returns null when none meet threshold`() {
        val opp1 = TriggerService.Opportunity(
            pair = DexPair.BASE_AERO_UNI_WETH,
            buyPrice = 3000.0,
            sellPrice = 3001.0,
            spread = 1.0,
            adjustedProfit = 0.3
        )

        val result = TriggerService.findBestOpportunity(listOf(opp1), minProfitUSD = 0.5)
        assertNull(result)
    }

    @Test
    fun `findBestOpportunity returns null for empty list`() {
        val result = TriggerService.findBestOpportunity(emptyList(), minProfitUSD = 0.5)
        assertNull(result)
    }
}
