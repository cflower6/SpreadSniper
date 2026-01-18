package services

import com.notificationapi.NotificationApi
import com.notificationapi.model.NotificationRequest
import com.notificationapi.model.SmsOptions
import com.notificationapi.model.User
import configurations.AppConfig
import org.slf4j.LoggerFactory

object NotifierService {
    private val logger = LoggerFactory.getLogger("NotifierService")

    private val clientId: String by lazy {
        AppConfig.clientId
    }

    private val clientSecret: String by lazy {
        AppConfig.clientSecret
    }

    private val toEmail: String by lazy {
        AppConfig.emailTo
    }

    private val phoneNum: String by lazy {
        AppConfig.phoneNumber
    }

    fun send(subject: String, body: String) {
        try {
            val api = NotificationApi(clientId, clientSecret)

            val user = User(toEmail).setNumber(phoneNum)

            val notificationReq = NotificationRequest(subject, user)
                .setSms(
                    SmsOptions()
                        .setMessage(body)
                )

            val response = api.send(notificationReq)

            logger.debug("Notification sent successfully! Response: $response")
        } catch (
            e: Exception,
        ) {
            logger.error("Failed to send notification: ${e.message}")
        }

    }

}