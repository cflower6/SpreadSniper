package services

import interfaces.DexQuoter
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.StaticStruct
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import models.registries.Token
import utils.Web3Utils
import java.math.BigInteger

/**
 * Aerodrome Decentralized Exchange that implements the DexQuoter interface
 */
class AerodromeQuoterService(
    override val name: String,
    private val router: String,
    private val factory: String,
    private val stable: Boolean, // false = volatile
    override val feeRate: Double = if (stable) 0.0001 else 0.003 // 0.01% stable, 0.3% volatile
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

        return Web3Utils().web3Helper(Web3Utils().getAmountsOut(amountIn, routes), web3, block, router)
    }
}