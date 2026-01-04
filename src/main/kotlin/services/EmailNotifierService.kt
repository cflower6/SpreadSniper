package services

import configurations.AppConfig
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

object EmailNotifierService {
    private val from = AppConfig.emailFrom
    private val to = AppConfig.emailTo
    private val password = AppConfig.emailPassword

    private val session: Session? by lazy {
        if (from.isBlank() || password.isBlank()) return@lazy null

        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

        Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(from, password)
            }
        })
    }

    fun send(subject: String, body: String) {
        if (session == null || to.isBlank()) {
            println("⚠️ Email not configured; skipping notification")
            return
        }

        try {
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(from as String?))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject)
                setText(body)
            }

            Transport.send(msg)
            println("📧 Email sent")
        } catch (e: Exception) {
            println("❌ Email failed: ${e.message}")
        }
    }
}
