package configurations

import domain.interfaces.DexQuoter
import domain.models.Chain
import domain.models.Dex
import infrastructure.dex.AerodromeQuoterService
import infrastructure.dex.UniV2QuoterService

object QuoterConfiguration {

    fun create(): List<DexQuoter> {

        val aerodrome =
            DexConfigurations.get(
                Chain.BASE,
                Dex.AERODROME
            )

        val uniswap =
            DexConfigurations.get(
                Chain.BASE,
                Dex.UNISWAP
            )

        val aerodromeQuoter =
            AerodromeQuoterService(
                routerAddress = aerodrome.routerAddress,
                factoryAddress = aerodrome.factoryAddress
                    ?: error("Aerodrome factory missing")
            )

        val uniswapQuoter =
            UniV2QuoterService(
                routerAddress = uniswap.routerAddress
            )

        return listOf(
            aerodromeQuoter,
            uniswapQuoter
        )
    }
}