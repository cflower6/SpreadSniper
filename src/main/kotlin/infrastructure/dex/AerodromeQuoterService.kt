package infrastructure.dex

import domain.interfaces.DexQuoter
import domain.models.Dex
import domain.models.DexRoute
import infrastructure.blockchain.Web3Utils
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.StaticStruct
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import java.math.BigInteger

class AerodromeQuoterService(
    private val routerAddress: String,
    private val factoryAddress: String
) : DexQuoter {

    override val dex: Dex =
        Dex.AERODROME

    class AeroRoute(
        from: Address,
        to: Address,
        stable: Bool,
        factory: Address
    ) : StaticStruct(
        from,
        to,
        stable,
        factory
    )

    override fun quote(
        web3: Web3j,
        route: DexRoute,
        amountIn: BigInteger,
        block: DefaultBlockParameter
    ): BigInteger? {

        require(route.dex == Dex.AERODROME) {
            "AerodromeQuoterService cannot quote route for ${route.dex}"
        }

        val stable =
            route.pool.stable
                ?: error(
                    "Aerodrome pool ${route.pool.poolAddress} is missing stable metadata"
                )

        val routes =
            DynamicArray(
                AeroRoute::class.java,
                listOf(
                    AeroRoute(
                        from = Address(
                            route.tokenIn.address
                        ),
                        to = Address(
                            route.tokenOut.address
                        ),
                        stable = Bool(stable),
                        factory = Address(
                            factoryAddress
                        )
                    )
                )
            )

        return Web3Utils().web3Helper(
            function = Web3Utils().getAerodromeAmountsOut(
                amountIn,
                routes
            ),
            web3 = web3,
            block = block,
            contractAddress = routerAddress
        )
    }
}