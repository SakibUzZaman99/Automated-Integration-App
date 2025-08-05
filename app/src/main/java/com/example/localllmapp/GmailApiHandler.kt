package com.example.localllmapp

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.Date

data class GmailMessage(
    val id: String,
    val from: String,
    val to: String,
    val subject: String,
    val body: String,
    val timestamp: Long
)

class GmailApiHandler(private val context: Context) {

    companion object {
        private const val TAG = "GmailApiHandler"
        private const val APPLICATION_NAME = "LocalLLMApp"
        private val GMAIL_SCOPES = listOf(
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_MODIFY
        )
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var gmailService: Gmail? = null

    suspend fun initializeGmailApi(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Log.e(TAG, "No authenticated user")
                    return@withContext false
                }

                val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (googleAccount == null) {
                    Log.e(TAG, "No Google account found")
                    return@withContext false
                }

                if (!hasGmailPermissions(googleAccount)) {
                    Log.e(TAG, "Missing Gmail permissions")
                    return@withContext false
                }

                gmailService = createGmailService(googleAccount)
                storeTokensInFirestore(googleAccount)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Gmail API", e)
                false
            }
        }
    }

    private fun hasGmailPermissions(account: GoogleSignInAccount): Boolean {
        val grantedScopes = account.grantedScopes
        return GMAIL_SCOPES.all { scope ->
            grantedScopes.any { it.scopeUri == scope }
        }
    }

    private fun createGmailService(account: GoogleSignInAccount): Gmail {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            GMAIL_SCOPES
        ).apply {
            selectedAccount = account.account
        }

        return Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APPLICATION_NAME).build()
    }

    private suspend fun storeTokensInFirestore(account: GoogleSignInAccount) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val tokenData = hashMapOf(
                "email" to (account.email ?: ""),
                "displayName" to (account.displayName ?: ""),
                "idToken" to (account.idToken ?: ""),
                "serverAuthCode" to (account.serverAuthCode ?: ""),
                "lastUpdated" to Date(),
                "scopes" to account.grantedScopes.map { it.scopeUri }
            )

            firestore.collection("users")
                .document(userId)
                .collection("tokens")
                .document("gmail")
                .set(tokenData)
                .await()

            Log.d(TAG, "Tokens stored in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing tokens", e)
        }
    }

    suspend fun requestGmailScopes(activity: ComponentActivity): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .requestScopes(
                        Scope(GmailScopes.GMAIL_READONLY),
                        Scope(GmailScopes.GMAIL_SEND),
                        Scope(GmailScopes.GMAIL_MODIFY)
                    )
                    .build()

                val googleSignInClient = GoogleSignIn.getClient(activity, gso)
                googleSignInClient.signOut().await()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting scopes", e)
                false
            }
        }
    }

    fun fetchLatestEmails(callback: (List<GmailMessage>) -> Unit) {
        Thread {
            try {
                val service = gmailService
                if (service == null) {
                    Log.e(TAG, "Gmail service not initialized")
                    callback(emptyList())
                    return@Thread
                }

                val response = service.users().messages()
                    .list("me")
                    .setQ("is:unread in:inbox")
                    .setMaxResults(10)
                    .execute()

                val messages = mutableListOf<GmailMessage>()
                response.messages?.forEach { messageRef ->
                    try {
                        val fullMessage = service.users().messages()
                            .get("me", messageRef.id)
                            .execute()
                        val gmailMessage = parseGmailMessage(fullMessage)
                        messages.add(gmailMessage)
                        logEmailToFirestore(gmailMessage)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching message ${messageRef.id}", e)
                    }
                }
                callback(messages)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching emails", e)
                callback(emptyList())
            }
        }.start()
    }

    private fun parseGmailMessage(message: Message): GmailMessage {
        val headers = message.payload.headers
        val from = headers.find { it.name == "From" }?.value ?: ""
        val to = headers.find { it.name == "To" }?.value ?: ""
        val subject = headers.find { it.name == "Subject" }?.value ?: ""
        val body = extractBody(message)

        return GmailMessage(
            id = message.id,
            from = from,
            to = to,
            subject = subject,
            body = body,
            timestamp = message.internalDate ?: 0L
        )
    }

    private fun extractBody(message: Message): String {
        return when {
            message.payload.body?.data != null -> {
                String(Base64.getUrlDecoder().decode(message.payload.body.data))
            }
            message.payload.parts != null -> {
                message.payload.parts
                    .firstOrNull { it.mimeType == "text/plain" }
                    ?.body?.data?.let {
                        String(Base64.getUrlDecoder().decode(it))
                    } ?: ""
            }
            else -> ""
        }
    }

    private fun logEmailToFirestore(email: GmailMessage) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val emailLog = hashMapOf(
                "userId" to userId,
                "messageId" to email.id,
                "from" to email.from,
                "subject" to email.subject,
                "timestamp" to Date(email.timestamp),
                "processedAt" to Date()
            )
            firestore.collection("email_logs")
                .add(emailLog)
                .addOnFailureListener { e -> Log.e(TAG, "Error logging email", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Error in logEmailToFirestore", e)
        }
    }

    suspend fun sendEmail(to: String, subject: String, body: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val service = gmailService
                if (service == null) {
                    Log.e(TAG, "Gmail service not initialized")
                    return@withContext false
                }

                val email = createEmail(to, subject, body)
                service.users().messages().send("me", email).execute()
                Log.d(TAG, "Email sent successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending email", e)
                false
            }
        }
    }

    private fun createEmail(to: String, subject: String, bodyText: String): Message {
        val emailContent = """
            To: $to
            Subject: $subject
            
            $bodyText
        """.trimIndent()

        val encodedEmail = Base64.getUrlEncoder()
            .encodeToString(emailContent.toByteArray())
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")

        return Message().apply { raw = encodedEmail }
    }
<<<<<<< Updated upstream
}
=======
}
>>>>>>> Stashed changes
