package infrastructure.http

import domain.payment.PaymentAuthorization

interface PaymentGate {
    suspend fun authorize(
        request: String
    ): PaymentAuthorization
}