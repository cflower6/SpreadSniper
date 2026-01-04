package utils

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import java.math.BigInteger

/**
 * Don't know a good name for this,
 * but it takes in the solidity contract function,
 * web3 SDK, current block and router
 *
 * It encodes the function sends it
 * then decodes the response
 */
fun web3Helper(function: Function, web3: Web3j, block: DefaultBlockParameter, router: String): BigInteger? {
    val encoded = FunctionEncoder.encode(function)

    val ethCall = web3.ethCall(
        Transaction.createEthCallTransaction(
            "0x0000000000000000000000000000000000000000",
            router,
            encoded
        ),
        block
    ).send()

    if (ethCall.value.isNullOrBlank() || ethCall.value == "0x") return null

    val decoded = FunctionReturnDecoder.decode(
        ethCall.value,
        listOf<TypeReference<*>>(object : TypeReference<DynamicArray<Uint256>>() {}) as List<TypeReference<Type<*>>>
    )

    @Suppress("UNCHECKED_CAST")
    val arr = decoded.firstOrNull()?.value as? List<Uint256> ?: return null

    return arr.lastOrNull()?.value
}