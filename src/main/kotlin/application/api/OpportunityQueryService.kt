//package application.api
//
//class OpportunityQueryService {
//    get("/api/opportunities/latest") {
//
//        when (
//            val auth = paymentGate.authorize(
//                PaymentRequestContext.from(call)
//            )
//        ) {
//
//            is PaymentAuthorization.PaymentRequired -> {
//                call.respond(
//                    HttpStatusCode.PaymentRequired,
//                    auth.requirements
//                )
//            }
//
//            is PaymentAuthorization.Rejected -> {
//                call.respond(
//                    HttpStatusCode.Unauthorized,
//                    auth.reason
//                )
//            }
//
//            is PaymentAuthorization.Authorized -> {
//
//                val opportunity =
//                    opportunityQueryService.findLatest()
//
//                if (opportunity == null) {
//                    call.respond(
//                        HttpStatusCode.NoContent
//                    )
//                    return@get
//                }
//
//                call.respond(
//                    HttpStatusCode.OK,
//                    opportunity
//                )
//            }
//        }
//    }
//}