package utils

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

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

object DexUtils {
    // Aerodrome Route struct: (address from, address to, bool stable, address factory)
    class AeroRoute(
        from: Address,
        to: Address,
        stable: Bool,
        factory: Address
    ) : StaticStruct(from, to, stable, factory)

    fun getPriceFromRouter(
        web3: Web3j,
        router: String,
        path: List<String>,
        amountIn: BigInteger,
        outDecimals: Int,
    ): Double? {

        val isAerodromeRouter = router.equals(
            "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43",
            ignoreCase = true
        )

        val function = if (isAerodromeRouter) {
            val tokenIn = path.first()
            val tokenOut = path.last()
            val aeroFactory = "0x420dd381b31aef6683db6b902084cb0ffece40da" // Base Aerodrome Pool Factory

            val routes = DynamicArray(
                AeroRoute::class.java,
                listOf(
                    AeroRoute(
                        Address(tokenIn),
                        Address(tokenOut),
                        Bool(false), // volatile
                        Address(aeroFactory)
                    )
                )
            )

            Function(
                "getAmountsOut",
                listOf(Uint256(amountIn), routes),
                listOf(object : TypeReference<DynamicArray<Uint256>>() {})
            )
        } else {
            Function(
                "getAmountsOut",
                listOf(
                    Uint256(amountIn),
                    DynamicArray(Address::class.java, path.map { Address(it) })
                ),
                listOf(object : TypeReference<DynamicArray<Uint256>>() {})
            )
        }

        val encoded = FunctionEncoder.encode(function)

        val ethCall = web3.ethCall(
            Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000",
                router,
                encoded
            ),
            DefaultBlockParameterName.LATEST
        ).send()

        if (ethCall.value.isNullOrBlank() || ethCall.value == "0x") return null

        return decodeAmountOut(ethCall.value, outDecimals)
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
        val amountOut = resultCheck.lastOrNull() ?: return null

        return amountOut.value
            ?.toBigDecimal()
            ?.divide(BigDecimal.TEN.pow(decimals), scale, RoundingMode.HALF_UP)
            ?.toDouble()
    }
}