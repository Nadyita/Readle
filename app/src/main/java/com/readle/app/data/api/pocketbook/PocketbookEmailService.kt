package com.readle.app.data.api.pocketbook

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Service for sending eBooks to Pocketbook via email using SMTP.
 */
@Singleton
class PocketbookEmailService @Inject constructor() {

    /**
     * Sends an eBook file to Pocketbook via email.
     * 
     * @param epubFile The EPUB file to send
     * @param bookTitle Title of the book (used in email subject)
     * @param smtpServer SMTP server address (e.g., "smtp.gmail.com")
     * @param smtpPort SMTP port (e.g., 587 for TLS, 465 for SSL)
     * @param username SMTP username (usually the email address)
     * @param password SMTP password or app-specific password
     * @param fromEmail Sender email address
     * @param toEmail Pocketbook email address (e.g., "username@pbsync.com")
     * @return Result<Unit> Success or failure
     */
    suspend fun sendBookByEmail(
        epubFile: File,
        bookTitle: String,
        smtpServer: String,
        smtpPort: Int,
        username: String,
        password: String,
        fromEmail: String,
        toEmail: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(
                "PocketbookEmail",
                "Preparing to send '$bookTitle' to $toEmail via $smtpServer:$smtpPort"
            )
            Log.d(
                "PocketbookEmail",
                "From: $fromEmail | Username: $username | File: ${epubFile.name} (${epubFile.length()} bytes)"
            )

            // Validate file exists and is readable
            if (!epubFile.exists() || !epubFile.canRead()) {
                return@withContext Result.failure(
                    Exception("File not found or not readable: ${epubFile.path}")
                )
            }

            // Configure SMTP properties
            val useSSL = smtpPort == 465
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", if (useSSL) "false" else "true") // TLS
                put("mail.smtp.host", smtpServer)
                put("mail.smtp.port", smtpPort.toString())
                put("mail.smtp.ssl.protocols", "TLSv1.2")
                
                // If port is 465, use SSL instead of TLS
                if (useSSL) {
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                }
            }
            
            Log.d(
                "PocketbookEmail",
                "SMTP Config: host=$smtpServer, port=$smtpPort, auth=true, SSL=$useSSL, TLS=${!useSSL}"
            )

            // Create session with authentication
            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })

            // Create message
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = bookTitle
                
                // Create multipart message with attachment
                val multipart = MimeMultipart()
                
                // Optional: Add text body
                val textPart = MimeBodyPart().apply {
                    setText("Buch: $bookTitle\n\nGesendet von Readle App")
                }
                multipart.addBodyPart(textPart)
                
                // Add EPUB attachment
                val attachmentPart = MimeBodyPart().apply {
                    attachFile(epubFile)
                    fileName = epubFile.name
                    setHeader("Content-Type", "application/epub+zip")
                }
                multipart.addBodyPart(attachmentPart)
                
                setContent(multipart)
            }
            
            Log.d(
                "PocketbookEmail",
                "Email created | Subject: '$bookTitle' | From: $fromEmail | To: $toEmail"
            )
            Log.d(
                "PocketbookEmail",
                "Attachment: ${epubFile.name} (${epubFile.length()} bytes, type: application/epub+zip)"
            )

            // Send the message
            Log.d(
                "PocketbookEmail",
                "Connecting to SMTP server: $smtpServer:$smtpPort (SSL: ${smtpPort == 465}, TLS: ${smtpPort != 465})"
            )
            
            // Get transport and send with detailed logging
            val transport = session.getTransport("smtp")
            try {
                transport.connect(smtpServer, smtpPort, username, password)
                Log.d("PocketbookEmail", "Connected to SMTP server successfully")
                
                transport.sendMessage(message, message.allRecipients)
                Log.d("PocketbookEmail", "Message sent to transport")
                
                // Get message ID after sending
                val messageId = message.messageID
                Log.d("PocketbookEmail", "Email sent successfully | Message-ID: $messageId")
            } finally {
                transport.close()
                Log.d("PocketbookEmail", "SMTP connection closed")
            }

            Log.d("PocketbookEmail", "Successfully sent '$bookTitle' to $toEmail")
            Log.d("PocketbookEmail", "Summary: From=$fromEmail, To=$toEmail, Subject='$bookTitle', Attachment=${epubFile.name} (${epubFile.length()} bytes)")
            Log.i(
                "PocketbookEmail",
                "⚠️ IMPORTANT: Email was accepted by SMTP server, but delivery to Pocketbook may take a few minutes. " +
                "Check: 1) Spam folder, 2) Pocketbook email address is correct ($toEmail), 3) Email provider allows large attachments (${epubFile.length() / 1024 / 1024}MB)"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PocketbookEmail", "Failed to send email: ${e.message}", e)
            Log.e("PocketbookEmail", "Error details: Server=$smtpServer:$smtpPort, From=$fromEmail, To=$toEmail, Exception type: ${e.javaClass.simpleName}")
            if (e is javax.mail.AuthenticationFailedException) {
                Log.e("PocketbookEmail", "Authentication failed! Check username and password.")
            } else if (e is javax.mail.MessagingException) {
                Log.e("PocketbookEmail", "Messaging error: ${e.message}")
            }
            Result.failure(e)
        }
    }

    /**
     * Tests the email configuration by attempting to authenticate.
     * 
     * @return Result<String> Success message or error
     */
    suspend fun testEmailConfiguration(
        smtpServer: String,
        smtpPort: Int,
        username: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", smtpServer)
                put("mail.smtp.port", smtpPort.toString())
                put("mail.smtp.ssl.protocols", "TLSv1.2")
                put("mail.smtp.connectiontimeout", "10000") // 10 seconds
                put("mail.smtp.timeout", "10000")
                
                if (smtpPort == 465) {
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.starttls.enable", "false")
                }
            }

            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })

            // Try to connect to the SMTP server
            val transport = session.getTransport("smtp")
            transport.connect(smtpServer, smtpPort, username, password)
            transport.close()

            Result.success("Verbindung erfolgreich!")
        } catch (e: Exception) {
            Log.e("PocketbookEmail", "Email configuration test failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

