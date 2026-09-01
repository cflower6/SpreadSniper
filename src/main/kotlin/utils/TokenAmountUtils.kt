package utils

import domain.models.registries.Token
import java.math.BigDecimal
import java.math.BigInteger

fun toRawAmount(
    amount: String,
    token: Token
): BigInteger {

    return BigDecimal(amount)
        .movePointRight(token.decimals)
        .toBigIntegerExact()
}