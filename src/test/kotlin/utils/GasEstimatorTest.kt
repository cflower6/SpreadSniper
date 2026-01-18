package utils

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.response.EthGasPrice
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GasEstimatorTest {

    @Test
    fun `estimateGasCostUsd calculates correctly for Base chain gas prices`() {
        // Mock Web3j
        val web3 = mockk<Web3j>()
        val gasPrice = mockk<EthGasPrice>()
        val request = mockk<Request<*, EthGasPrice>>()

        // Base chain typically has very low gas: ~0.001 gwei = 1000000 wei
        every { request.send() } returns gasPrice
        every { gasPrice.gasPrice } returns BigInteger("1000000") // 0.001 gwei
        every { web3.ethGasPrice() } returns request

        // Default gas limit 300,000, ETH price ~$3000
        // Gas cost = 1000000 * 300000 = 300,000,000,000 wei = 0.0000003 ETH
        // USD = 0.0000003 * 3000 = ~$0.0009
        val result = GasEstimator.estimateGasCostUsd(web3, 300_000)

        assertTrue(result < 0.01, "Base gas should be very cheap: $result")
        assertTrue(result > 0.0, "Gas cost should be positive")
    }

    @Test
    fun `estimateGasCostUsd calculates correctly for Ethereum mainnet gas prices`() {
        val web3 = mockk<Web3j>()
        val gasPrice = mockk<EthGasPrice>()
        val request = mockk<Request<*, EthGasPrice>>()

        // Ethereum mainnet: ~30 gwei = 30,000,000,000 wei
        every { request.send() } returns gasPrice
        every { gasPrice.gasPrice } returns BigInteger("30000000000") // 30 gwei
        every { web3.ethGasPrice() } returns request

        // Gas cost = 30 gwei * 300000 = 9,000,000 gwei = 0.009 ETH
        // USD = 0.009 * 3000 = $27
        val result = GasEstimator.estimateGasCostUsd(web3, 300_000)

        assertTrue(result > 20.0, "Mainnet gas should be significant: $result")
        assertTrue(result < 35.0, "Mainnet gas should be in expected range: $result")
    }

    @Test
    fun `estimateGasCostUsd respects custom gas limit`() {
        val web3 = mockk<Web3j>()
        val gasPrice = mockk<EthGasPrice>()
        val request = mockk<Request<*, EthGasPrice>>()

        every { request.send() } returns gasPrice
        every { gasPrice.gasPrice } returns BigInteger("1000000000") // 1 gwei
        every { web3.ethGasPrice() } returns request

        val result150k = GasEstimator.estimateGasCostUsd(web3, 150_000)
        val result300k = GasEstimator.estimateGasCostUsd(web3, 300_000)

        // 300k should be exactly 2x the cost of 150k
        assertEquals(result150k * 2, result300k, 0.0001)
    }

    @Test
    fun `getGasPriceGwei returns correct gwei value`() {
        val web3 = mockk<Web3j>()
        val gasPrice = mockk<EthGasPrice>()
        val request = mockk<Request<*, EthGasPrice>>()

        // 25 gwei
        every { request.send() } returns gasPrice
        every { gasPrice.gasPrice } returns BigInteger("25000000000")
        every { web3.ethGasPrice() } returns request

        val result = GasEstimator.getGasPriceGwei(web3)

        assertEquals(25.0, result!!, 0.001)
    }
}
