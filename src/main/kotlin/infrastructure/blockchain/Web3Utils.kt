package infrastructure.blockchain

import infrastructure.dex.AerodromeQuoterService
import configurations.AppConfig
import domain.models.Chain
import domain.models.registries.Token
import org.slf4j.LoggerFactory
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import utils.withRetrySync
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class Web3Utils {
    private val logger = LoggerFactory.getLogger("Web3Utils")

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
        contractAddress: String,
        maxRetries: Int = 3
    ): BigInteger? {
        val encoded = FunctionEncoder.encode(function)

        return withRetrySync(
            maxAttempts = maxRetries,
            initialDelayMs = 100,
            maxDelayMs = 1000,
            retryOn = { e -> e is IOException }
        ) {
            val ethCall = web3.ethCall(
                Transaction.createEthCallTransaction(
                    "0x0000000000000000000000000000000000000000",
                    contractAddress,
                    encoded
                ),
                block
            ).send()

            if (ethCall.hasError()) {
                logger.debug(
                    "RPC error calling {}: {}",
                    contractAddress,
                    ethCall.error?.message
                )

                throw IOException(
                    "RPC error: ${ethCall.error?.message}"
                )
            }

            if (
                ethCall.value.isNullOrBlank() ||
                ethCall.value == "0x"
            ) {
                return@withRetrySync null
            }

            val decoded =
                FunctionReturnDecoder.decode(
                    ethCall.value,
                    listOf<TypeReference<*>>(
                        object :
                            TypeReference<DynamicArray<Uint256>>() {}
                    ) as List<TypeReference<Type<*>>>
                )

            @Suppress("UNCHECKED_CAST")
            val amounts =
                decoded
                    .firstOrNull()
                    ?.value as? List<Uint256>

            amounts
                ?.lastOrNull()
                ?.value
        }
    }

    fun getWeb3ForChain(chain: Chain): Web3j {
        val rpc = when (chain) {
            Chain.ETHEREUM -> AppConfig.ethereumRpc
            Chain.BASE -> AppConfig.baseRpc
            Chain.ARBITRUM -> AppConfig.arbitrumRpc
        }
        return Web3j.build(HttpService(rpc))
    }

    /**
     * Web3 function to retrieve AmountsOut on-chain
     */
    fun getAerodromeAmountsOut(amountIn: BigInteger, routes: DynamicArray<AerodromeQuoterService.AeroRoute>): Function {
        return Function(
            "getAmountsOut",
            listOf(Uint256(amountIn), routes),
            listOf(object : TypeReference<DynamicArray<Uint256>>() {})
        )
    }

    fun getUniV2AmountsOut(
        amountIn: BigInteger,
        path: DynamicArray<Address>
    ): Function {
        return Function(
            "getAmountsOut",
            listOf(
                Uint256(amountIn),
                path
            ),
            listOf(
                object : TypeReference<DynamicArray<Uint256>>() {}
            )
        )
    }

    /**
     * Turns crypto amount into a human-readable number.
     */
    fun toHuman(amountRaw: BigInteger, token: Token, scale: Int = 8): BigDecimal {
        return amountRaw.toBigDecimal()
            .divide(BigDecimal.TEN.pow(token.decimals), scale, RoundingMode.HALF_UP)
    }

    fun currentBlockNumber(
        web3: Web3j
    ): BigInteger {
        return web3
            .ethBlockNumber()
            .send()
            .blockNumber
    }
}