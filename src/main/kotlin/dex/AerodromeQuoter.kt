package dex

import interfaces.DexQuoter
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import registries.Token
import utils.web3Helper
import java.math.BigInteger

class AerodromeQuoter(
    override val name: String,
    private val router: String,
    private val factory: String,
    private val stable: Boolean // false = volatile
) : DexQuoter {

    class AeroRoute(from: Address, to: Address, stable: Bool, factory: Address) :
        StaticStruct(from, to, stable, factory)

    override fun quote(
        web3: Web3j,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger,
        block: DefaultBlockParameter
    ): BigInteger? {
        val routes = DynamicArray(
            AeroRoute::class.java,
            listOf(
                AeroRoute(
                    Address(tokenIn.address),
                    Address(tokenOut.address),
                    Bool(stable),
                    Address(factory)
                )
            )
        )

        val function = Function(
            "getAmountsOut",
            listOf(Uint256(amountIn), routes),
            listOf(object : TypeReference<DynamicArray<Uint256>>() {})
        )

        return web3Helper(function, web3, block, router)
    }
}
