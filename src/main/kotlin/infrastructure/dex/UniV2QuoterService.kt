package infrastructure.dex

import domain.interfaces.DexQuoter
import domain.models.Dex
import infrastructure.blockchain.Web3Utils
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import java.math.BigInteger
import domain.models.DexRoute

/**
 * UniswapV2 Decentralized Exchange that implements the DexQuoter interface
 */


class UniV2QuoterService(
    private val routerAddress: String
) : DexQuoter {

    override val dex: Dex =
        Dex.UNISWAP

    override fun quote(
        web3: Web3j,
        route: DexRoute,
        amountIn: BigInteger,
        block: DefaultBlockParameter
    ): BigInteger? {

        require(route.dex == Dex.UNISWAP) {
            "UniV2QuoterService cannot quote route for ${route.dex}"
        }

        val path =
            DynamicArray(
                Address::class.java,
                route.path.map {
                    Address(it)
                }
            )

        return Web3Utils().web3Helper(
            function = Web3Utils().getUniV2AmountsOut(
                amountIn = amountIn,
                path = path
            ),
            web3 = web3,
            block = block,
            contractAddress = routerAddress
        )
    }
}