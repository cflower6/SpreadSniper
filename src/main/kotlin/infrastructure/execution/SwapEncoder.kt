package infrastructure.execution

import configurations.AppConfig
import org.slf4j.LoggerFactory
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

private val logger = LoggerFactory.getLogger("SwapEncoder")

/**
 * Encodes swap function calls for different DEX routers.
 */
object SwapEncoder {

    /**
     * Encodes a Uniswap V2 style swap.
     *
     * Function: swapExactTokensForTokens(
     *   uint amountIn,
     *   uint amountOutMin,
     *   address[] path,
     *   address to,
     *   uint deadline
     * )
     */
    fun encodeUniV2Swap(
        amountIn: BigInteger,
        amountOutMin: BigInteger,
        path: List<String>,
        recipient: String,
        deadlineSeconds: Long = 300 // 5 minutes
    ): String {
        val deadline = BigInteger.valueOf(System.currentTimeMillis() / 1000 + deadlineSeconds)

        val function = Function(
            "swapExactTokensForTokens",
            listOf(
                Uint256(amountIn),
                Uint256(amountOutMin),
                DynamicArray(Address::class.java, path.map { Address(it) }),
                Address(recipient),
                Uint256(deadline)
            ),
            emptyList()
        )

        return FunctionEncoder.encode(function)
    }

    /**
     * Encodes a Uniswap V2 style swap starting with ETH.
     *
     * Function: swapExactETHForTokens(
     *   uint amountOutMin,
     *   address[] path,
     *   address to,
     *   uint deadline
     * )
     *
     * Note: amountIn is sent as msg.value
     */
    fun encodeUniV2SwapETHForTokens(
        amountOutMin: BigInteger,
        path: List<String>,
        recipient: String,
        deadlineSeconds: Long = 300
    ): String {
        val deadline = BigInteger.valueOf(System.currentTimeMillis() / 1000 + deadlineSeconds)

        val function = Function(
            "swapExactETHForTokens",
            listOf(
                Uint256(amountOutMin),
                DynamicArray(Address::class.java, path.map { Address(it) }),
                Address(recipient),
                Uint256(deadline)
            ),
            emptyList()
        )

        return FunctionEncoder.encode(function)
    }

    /**
     * Calculates minimum output with slippage protection.
     *
     * @param expectedOut Expected output from quote
     * @param slippagePct Slippage tolerance as percentage (e.g., 0.5 for 0.5%)
     * @return Minimum acceptable output
     */
    fun calculateMinOutput(expectedOut: BigInteger, slippagePct: Double = AppConfig.maxSlippagePct): BigInteger {
        val slippageFactor = 1.0 - (slippagePct / 100.0)
        val minOut = expectedOut.toBigDecimal()
            .multiply(slippageFactor.toBigDecimal())
            .toBigInteger()

        logger.debug("Expected: {}, Min ({}% slippage): {}", expectedOut, slippagePct, minOut)
        return minOut
    }

    /**
     * Encodes ERC20 approve function.
     *
     * Function: approve(address spender, uint256 amount)
     */
    fun encodeApprove(spender: String, amount: BigInteger): String {
        val function = Function(
            "approve",
            listOf(
                Address(spender),
                Uint256(amount)
            ),
            emptyList()
        )

        return FunctionEncoder.encode(function)
    }

    /**
     * Max uint256 for unlimited approval.
     */
    val MAX_UINT256: BigInteger = BigInteger.TWO.pow(256).subtract(BigInteger.ONE)
}
