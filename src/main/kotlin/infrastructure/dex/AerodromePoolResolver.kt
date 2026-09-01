package infrastructure.dex

import domain.interfaces.PoolResolver
import domain.models.Chain
import domain.models.Dex
import domain.models.Pool
import domain.models.registries.Token
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction

class AerodromePoolResolver(
    private val web3: Web3j,
    private val factoryAddress: String
) : PoolResolver {

    override val dex = Dex.AERODROME

    override fun resolve(
        chain: Chain,
        tokenA: Token,
        tokenB: Token
    ): Pool? {

        val volatile =
            getPool(
                tokenA = tokenA,
                tokenB = tokenB,
                stable = false
            )

        if (volatile != null) {
            return Pool(
                dex = Dex.AERODROME,
                chain = chain,
                token0 = tokenA,
                token1 = tokenB,
                poolAddress = volatile,
                stable = false
            )
        }

        val stable =
            getPool(
                tokenA = tokenA,
                tokenB = tokenB,
                stable = true
            )

        if (stable != null) {
            return Pool(
                dex = Dex.AERODROME,
                chain = chain,
                token0 = tokenA,
                token1 = tokenB,
                poolAddress = stable,
                stable = true
            )
        }

        return null
    }

    private fun getPool(
        tokenA: Token,
        tokenB: Token,
        stable: Boolean
    ): String? {

        val function = Function(
            "getPool",
            listOf(
                Address(tokenA.address),
                Address(tokenB.address),
                Bool(stable)
            ),
            listOf(
                object : TypeReference<Address>() {}
            )
        )

        val result =
            web3.ethCall(
                Transaction.createEthCallTransaction(
                    ZERO_ADDRESS,
                    factoryAddress,
                    FunctionEncoder.encode(function)
                ),
                DefaultBlockParameterName.LATEST
            ).send()

        if (result.hasError()) {
            return null
        }

        val decoded =
            FunctionReturnDecoder.decode(
                result.value,
                function.outputParameters
            )

        val poolAddress =
            (decoded.firstOrNull() as? Address)
                ?.value
                ?: return null

        return poolAddress.takeUnless {
            it.equals(ZERO_ADDRESS, true)
        }
    }

    companion object {
        private const val ZERO_ADDRESS =
            "0x0000000000000000000000000000000000000000"
    }
}