package com.yeyint.recipeapp.backend.mail

import org.slf4j.LoggerFactory

/** Outbound email behind an interface so a real SMTP/API provider can be dropped in. */
interface Mailer {
    suspend fun sendPasswordResetCode(toEmail: String, name: String, code: String, ttlMinutes: Long)
}

/**
 * Development mailer: writes the code to the server log instead of sending
 * email. Replace with a real provider before production.
 */
class LogMailer : Mailer {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun sendPasswordResetCode(toEmail: String, name: String, code: String, ttlMinutes: Long) {
        log.warn("[DEV MAIL] Password reset code for {} <{}>: {} (valid {} min)", name, toEmail, code, ttlMinutes)
    }
}
