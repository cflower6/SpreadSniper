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
import org.web3j.abi.datatypes.Function
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction

class UniV2PoolResolver(
    private val web3: Web3j,
    private val factoryAddress: String
) : PoolResolver {

    override val dex = Dex.UNISWAP

    override fun resolve(
        chain: Chain,
        tokenA: Token,
        tokenB: Token
    ): Pool? {

        val function = Function(
            "getPair",
            listOf(
                Address(tokenA.address),
                Address(tokenB.address)
            ),
            listOf(
                object : TypeReference<Address>() {}
            )
        )

        val encoded =
            FunctionEncoder.encode(function)

        val result =
            web3.ethCall(
                Transaction.createEthCallTransaction(
                    ZERO_ADDRESS,
                    factoryAddress,
                    encoded
                ),
                DefaultBlockParameterName.LATEST
            ).send()

        if (result.hasError()) {
            error(
                "Uniswap getPair failed: ${result.error.message}"
            )
        }

        val decoded =
            FunctionReturnDecoder.decode(
                result.value,
                function.outputParameters
            )

        val pairAddress =
            (decoded.firstOrNull() as? Address)
                ?.value
                ?: return null

        if (pairAddress.equals(ZERO_ADDRESS, true)) {
            return null
        }

        return Pool(
            dex = Dex.UNISWAP,
            chain = chain,
            token0 = tokenA,
            token1 = tokenB,
            poolAddress = pairAddress
        )
    }

    companion object {
        private const val ZERO_ADDRESS =
            "0x0000000000000000000000000000000000000000"
    }
}