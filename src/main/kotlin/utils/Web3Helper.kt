package utils

import org.slf4j.LoggerFactory
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
import java.io.IOException
import java.math.BigInteger
import java.net.SocketTimeoutException

private val logger = LoggerFactory.getLogger("Web3Helper")

/**
 * Executes a smart contract view function with retry logic.
 *
 * Takes in the solidity contract function, web3 SDK, current block and router.
 * Encodes the function, sends it via eth_call, then decodes the response.
 * Retries on transient network errors with exponential backoff.
 */
fun web3Helper(
    function: Function,
    web3: Web3j,
    block: DefaultBlockParameter,
    router: String,
    maxRetries: Int = 3
): BigInteger? {
    val encoded = FunctionEncoder.encode(function)

    return withRetrySync(
        maxAttempts = maxRetries,
        initialDelayMs = 100,
        maxDelayMs = 1000,
        retryOn = { e -> e is IOException || e is SocketTimeoutException }
    ) {
        val ethCall = web3.ethCall(
            Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000",
                router,
                encoded
            ),
            block
        ).send()

        if (ethCall.hasError()) {
            logger.debug("RPC error: {}", ethCall.error?.message)
            throw IOException("RPC error: ${ethCall.error?.message}")
        }

        if (ethCall.value.isNullOrBlank() || ethCall.value == "0x") {
            return@withRetrySync null
        }

        val decoded = FunctionReturnDecoder.decode(
            ethCall.value,
            listOf<TypeReference<*>>(object : TypeReference<DynamicArray<Uint256>>() {}) as List<TypeReference<Type<*>>>
        )

        @Suppress("UNCHECKED_CAST")
        val arr = decoded.firstOrNull()?.value as? List<Uint256>
        arr?.lastOrNull()?.value
    }
}