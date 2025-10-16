package utils

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object DexUtils {
    /**
     * We are interfacing with the router which holds the views since we're not
     * doing actual swaps
     *
     * --- same idea of indirection and orchestration, but applied to smart contract composition
     *
     * +------------------------+
     * | Router (entry point)   |   <-- users call this
     * +------------------------+
     *           |
     *           v
     * +------------------------+
     * | Factory (registry)     |   <-- knows all pairs/pools
     * +------------------------+
     *           |
     *           v
     * +------------------------+
     * | Pair (liquidity pool)  |   <-- holds reserves & executes swaps
     * +------------------------+
     *
     */
    fun getPriceFromRouter(
        web3: Web3j,
        router: String,
        path: List<String>,
        amountIn: BigInteger,
        decimals: Int = 6
    ): Double? {
        val function = Function(
            "getAmountsOut",
            listOf(
                Uint256(amountIn),
                DynamicArray(Address::class.java, path.map { Address(it) })
            ),
            listOf(object : TypeReference<DynamicArray<Uint256>>() {})
        )

        /**
         * WE are encoding the CALLDATA --- the function selector (aka the name of the function)
         *
         * and the args we're sending
         */
        val encoded = FunctionEncoder.encode(function)

        /**
         * ethCall - is a read-only EVM simulation at a given block (no gas spent, no state change)
         */
        val ethCall = web3.ethCall(
            // Since the function we're calling is view we can send an arbitrary address
            Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000",
                router,
                encoded
            ),
            DefaultBlockParameterName.LATEST
        ).send()


        //Null check
        if (ethCall.value.isNullOrBlank() || ethCall.value == "0x") {
            println("⚠️ Empty response from router: $router")
            return null
        }

        val decoded = decodeAmountOut(ethCall.value, decimals)
        return decoded
    }

    private fun decodeAmountOut(responseHex: String, decimals: Int, scale: Int = 6): Double? {
        val outputType = object : TypeReference<DynamicArray<Uint256>>() {}

        @Suppress("UNCHECKED_CAST")
        val decoded = FunctionReturnDecoder.decode(
            responseHex,
            listOf<TypeReference<*>>(outputType) as List<TypeReference<Type<*>>>
        )

        val result = decoded.firstOrNull()?.value as? List<*>
        // Added this for better resilience
        val resultCheck = result?.filterIsInstance<Uint256>() ?: return null
        val amountOut = resultCheck.lastOrNull() ?: return null;

        return amountOut.value
            ?.toBigDecimal()
            ?.divide(BigDecimal.TEN.pow(decimals), scale, RoundingMode.HALF_UP)
            ?.toDouble()
    }
}