package domain.interfaces

import domain.models.Dex
import domain.models.DexRoute
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
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

    val dex: Dex

    fun quote(
        web3: Web3j,
        route: DexRoute,
        amountIn: BigInteger,
        block: DefaultBlockParameter
    ): BigInteger?
}