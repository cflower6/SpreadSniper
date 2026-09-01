package configurations

import domain.models.Chain
import domain.models.Dex

data class DexConfig(
    val routerAddress: String,
    val factoryAddress: String? = null
)

object DexConfigurations {

    private val configs =
        mapOf(
            (Chain.BASE to Dex.AERODROME) to DexConfig(
                routerAddress =
                    "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43",
                factoryAddress =
                    "0x420dd381b31aef6683db6b902084cb0ffece40da"
            ),

            (Chain.BASE to Dex.UNISWAP) to DexConfig(
                routerAddress =
                    "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24",
                factoryAddress =
                    "0x8909Dc15e40173Ff4699343b6eB8132c65e18eC6"
            )
        )

    fun get(
        chain: Chain,
        dex: Dex
    ): DexConfig {
        return configs[chain to dex]
            ?: error(
                "No configuration for $dex on $chain"
            )
    }
}