package infrastructure.execution

import domain.models.Chain
import domain.models.Dex

class RouterRegistry {

    fun routerFor(
        chain: Chain,
        dex: Dex
    ): String =
        when (chain to dex) {

            Chain.BASE to Dex.AERODROME ->
                BASE_AERODROME_ROUTER

            Chain.BASE to Dex.UNISWAP ->
                BASE_UNISWAP_ROUTER

            else ->
                error(
                    "No router configured for $dex on $chain"
                )
        }

    companion object {
        private const val BASE_AERODROME_ROUTER =
            "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43"

        private const val BASE_UNISWAP_ROUTER =
            "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24"
    }
}