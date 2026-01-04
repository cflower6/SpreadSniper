package interfaces

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import registries.Token
import java.math.BigInteger

/**
 * We are interfacing with the router which holds the views since we're not
 * doing actual swaps
 *
 * --- same idea of indirection and orchestration, but applied to smart contract composition
 *
 * +------------------------+
 * | Router (entry point)   |   <-- users call this
 * +------------------------+
 *           |
 *           v
 * +------------------------+
 * | Factory (registry)     |   <-- knows all pairs/pools
 * +------------------------+
 *           |
 *           v
 * +------------------------+
 * | Pair (liquidity pool)  |   <-- holds reserves & executes swaps
 * +------------------------+
 *
 */
interface DexQuoter {
    val name: String

    fun quote(
        web3: Web3j,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger,
        block: DefaultBlockParameter
    ): BigInteger? // amountOut raw
}