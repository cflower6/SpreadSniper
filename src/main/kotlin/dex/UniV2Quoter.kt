package dex

import interfaces.DexQuoter
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import registries.Token
import utils.web3Helper
import java.math.BigInteger

class UniV2Quoter(
    override val name: String,
    private val router: String
) : DexQuoter {

    override fun quote(
        web3: Web3j,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger,
        block: DefaultBlockParameter
    ): BigInteger? {

        val function = Function(
            "getAmountsOut",
            listOf(
                Uint256(amountIn),
                DynamicArray(Address::class.java, listOf(Address(tokenIn.address), Address(tokenOut.address)))
            ),
            listOf(object : TypeReference<DynamicArray<Uint256>>() {})
        )

        return web3Helper(function, web3, block, router)
    }
}
