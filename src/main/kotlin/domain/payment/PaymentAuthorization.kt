package domain.payment

sealed class PaymentAuthorization {

    data class Authorized(
        val payer: String?,
        val paymentId: String?
    ) : PaymentAuthorization()

//    data class PaymentRequired(
//        val requirements: X402PaymentRequirements
//    ) : PaymentAuthorization()

    data class Rejected(
        val reason: String
    ) : PaymentAuthorization()
}