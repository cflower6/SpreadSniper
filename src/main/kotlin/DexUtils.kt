import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object DexUtils {
    fun getPriceFromRouter(
        web3: Web3j,
        router: String,
        path: List<String>,
        amountIn: BigInteger,
        decimals: Int = 6
    ): Double? {
        val function = org.web3j.abi.datatypes.Function(
            "getAmountsOut",
            listOf(
                Uint256(amountIn),
                DynamicArray(Address::class.java, path.map { Address(it) })
            ),
            listOf(object : TypeReference<DynamicArray<Uint256>>() {})
        )

        val encoded = FunctionEncoder.encode(function)

        val ethCall = web3.ethCall(
            Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000",
                router,
                encoded
            ),
            DefaultBlockParameterName.LATEST
        ).send()

        if (ethCall.value.isNullOrBlank() || ethCall.value == "0x") {
            println("⚠️ Empty response from router: $router")
            return null
        }

        val decoded = decodeAmountOut(ethCall.value, decimals)
        return decoded
    }

    private fun decodeAmountOut(responseHex: String, decimals: Int): Double? {
        val outputType = object : TypeReference<DynamicArray<Uint256>>() {}

        @Suppress("UNCHECKED_CAST")
        val decoded = FunctionReturnDecoder.decode(
            responseHex,
            listOf<TypeReference<*>>(outputType) as List<TypeReference<Type<*>>>
        )

        val result = decoded.firstOrNull()?.value as? List<*>
        val amountOut = result?.getOrNull(1) as? Uint256

        return amountOut?.value
            ?.toBigDecimal()
            ?.divide(BigDecimal.TEN.pow(decimals), 6, RoundingMode.HALF_UP)
            ?.toDouble()
    }
}