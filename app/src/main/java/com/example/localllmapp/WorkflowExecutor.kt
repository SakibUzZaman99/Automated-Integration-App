package com.example.localllmapp.workflow

import android.content.Context
import android.util.Log
import com.example.localllmapp.GmailApiHandler
import com.example.localllmapp.TelegramApiHandler
import com.example.localllmapp.helpers.LlmInferenceHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * WorkflowExecutor handles the complete workflow pipeline:
 * 1. Matches notifications to workflows
 * 2. Fetches content from source
 * 3. Processes with LLM
 * 4. Sends to destination
 */
class WorkflowExecutor(private val context: Context) {

    companion object {
        private const val TAG = "WorkflowExecutor"
        private const val PROMPT_TEMPLATE = """
            You are an AI assistant processing messages according to user instructions.
            
            Instructions: %s
            
            Original Message:
            From: %s
            Subject/Title: %s
            Content: %s
            
            Please process this message according to the instructions above.
            """
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val gmailHandler = GmailApiHandler(context)
    private val telegramHandler = TelegramApiHandler(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class Workflow(
        val source: String,
        val sourceAccount: String,
        val destination: String,
        val destinationAccount: String,
        val instructions: String,
        val active: Boolean = true
    )

    data class ProcessingResult(
        val success: Boolean,
        val message: String? = null,
        val error: String? = null
    )

    /**
     * Main entry point - called when a notification is detected
     */
    fun processNotification(
        appName: String,
        notificationSender: String? = null,
        notificationTitle: String? = null
    ) {
        scope.launch {
            try {
                Log.d(TAG, "Processing notification from $appName")

                // Load matching workflows
                val workflows = loadMatchingWorkflows(appName, notificationSender)

                if (workflows.isEmpty()) {
                    Log.d(TAG, "No matching workflows found for $appName")
                    return@launch
                }

                // Process each matching workflow
                workflows.forEach { workflow ->
                    processWorkflow(workflow, notificationTitle)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    /**
     * Process a single workflow
     */
    private suspend fun processWorkflow(workflow: Workflow, notificationHint: String? = null) {
        try {
            Log.d(TAG, "Executing workflow: ${workflow.source} -> ${workflow.destination}")

            // Step 1: Fetch content from source
            val content = fetchSourceContent(workflow.source, workflow.sourceAccount, notificationHint)
            if (content == null) {
                Log.e(TAG, "Failed to fetch content from ${workflow.source}")
                logWorkflowExecution(workflow, false, "Failed to fetch source content")
                return
            }

            // Step 2: Process with LLM
            val processedContent = processWithLLM(content, workflow.instructions)
            if (processedContent == null) {
                Log.e(TAG, "Failed to process content with LLM")
                logWorkflowExecution(workflow, false, "LLM processing failed")
                return
            }

            // Step 3: Send to destination
            val result = sendToDestination(
                workflow.destination,
                workflow.destinationAccount,
                processedContent
            )

            // Step 4: Log execution
            logWorkflowExecution(workflow, result.success, result.message ?: result.error)

        } catch (e: Exception) {
            Log.e(TAG, "Error in workflow execution", e)
            logWorkflowExecution(workflow, false, e.message)
        }
    }

    /**
     * Fetch content from the source app
     */
    private suspend fun fetchSourceContent(
        source: String,
        sourceAccount: String,
        notificationHint: String? = null
    ): MessageContent? {
        return when (source) {
            "Google", "Gmail" -> fetchGmailContent(sourceAccount, notificationHint)
            "Telegram" -> fetchTelegramContent(sourceAccount, notificationHint)
            else -> null
        }
    }

    /**
     * Fetch Gmail content
     */
    private suspend fun fetchGmailContent(
        sourceAccount: String,
        subjectHint: String? = null
    ): MessageContent? {
        return withContext(Dispatchers.IO) {
            try {
                // Initialize Gmail API if needed
                if (!gmailHandler.initializeGmailApi()) {
                    Log.e(TAG, "Failed to initialize Gmail API")
                    return@withContext null
                }

                val messages = suspendCancellableCoroutine<List<com.example.localllmapp.GmailMessage>> { cont ->
                    gmailHandler.fetchLatestEmails { emails ->
                        cont.resume(emails) { }
                    }
                }

                // Filter by account if specified
                val filteredMessages = if (sourceAccount != "Any" && sourceAccount.isNotEmpty()) {
                    messages.filter { it.to.contains(sourceAccount) || it.from.contains(sourceAccount) }
                } else {
                    messages
                }

                // Find the most relevant message
                val targetMessage = if (subjectHint != null) {
                    filteredMessages.firstOrNull { it.subject.contains(subjectHint, ignoreCase = true) }
                        ?: filteredMessages.firstOrNull()
                } else {
                    filteredMessages.firstOrNull()
                }

                targetMessage?.let {
                    MessageContent(
                        from = it.from,
                        to = it.to,
                        subject = it.subject,
                        body = it.body,
                        timestamp = it.timestamp
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Gmail content", e)
                null
            }
        }
    }

    /**
     * Fetch Telegram content
     */
    private suspend fun fetchTelegramContent(
        sourceAccount: String,
        messageHint: String? = null
    ): MessageContent? {
        // TODO: Implement Telegram content fetching
        // This will require setting up Telegram Bot API to receive messages
        Log.w(TAG, "Telegram content fetching not yet implemented")
        return null
    }

    /**
     * Process content with LLM
     */
    private suspend fun processWithLLM(
        content: MessageContent,
        instructions: String
    ): ProcessedMessage? {
        return withContext(Dispatchers.Main) {
            try {
                // Ensure LLM is initialized
                if (!LlmInferenceHelper.initInstructionModel(context)) {
                    Log.e(TAG, "Failed to initialize LLM")
                    return@withContext null
                }

                // Format prompt
                val prompt = String.format(
                    PROMPT_TEMPLATE,
                    instructions,
                    content.from,
                    content.subject,
                    content.body
                )

                // Generate response
                val response = withContext(Dispatchers.IO) {
                    LlmInferenceHelper.generateResponse(prompt)
                }

                // Parse response and create processed message
                ProcessedMessage(
                    originalContent = content,
                    processedText = response,
                    instructions = instructions
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error processing with LLM", e)
                null
            }
        }
    }

    /**
     * Send processed content to destination
     */
    private suspend fun sendToDestination(
        destination: String,
        destinationAccount: String,
        content: ProcessedMessage
    ): ProcessingResult {
        return when (destination) {
            "Google", "Gmail" -> sendToGmail(destinationAccount, content)
            "Telegram" -> sendToTelegram(destinationAccount, content)
            else -> ProcessingResult(false, error = "Unknown destination: $destination")
        }
    }

    /**
     * Send to Gmail
     */
    private suspend fun sendToGmail(
        destinationEmail: String,
        content: ProcessedMessage
    ): ProcessingResult {
        return try {
            val subject = "Processed: ${content.originalContent.subject}"
            val body = """
                This message was processed by your workflow.
                
                Original from: ${content.originalContent.from}
                Instructions: ${content.instructions}
                
                Processed Content:
                ${content.processedText}
            """.trimIndent()

            val success = gmailHandler.sendEmail(destinationEmail, subject, body)

            if (success) {
                ProcessingResult(true, "Email sent successfully to $destinationEmail")
            } else {
                ProcessingResult(false, error = "Failed to send email")
            }
        } catch (e: Exception) {
            ProcessingResult(false, error = e.message)
        }
    }

    /**
     * Send to Telegram
     */
    private suspend fun sendToTelegram(
        phoneNumber: String,
        content: ProcessedMessage
    ): ProcessingResult {
        return try {
            val message = """
                📧 Processed Message
                From: ${content.originalContent.from}
                Subject: ${content.originalContent.subject}
                
                ${content.processedText}
            """.trimIndent()

            val success = telegramHandler.sendMessage(phoneNumber, message, useMasterBot = true)

            if (success) {
                ProcessingResult(true, "Message sent to Telegram: $phoneNumber")
            } else {
                ProcessingResult(false, error = "Failed to send Telegram message")
            }
        } catch (e: Exception) {
            ProcessingResult(false, error = e.message)
        }
    }

    /**
     * Load workflows that match the notification
     */
    private suspend fun loadMatchingWorkflows(
        appName: String,
        sender: String? = null
    ): List<Workflow> {
        val workflows = mutableListOf<Workflow>()

        // Load from local files
        val localWorkflows = loadLocalWorkflows()
        workflows.addAll(localWorkflows.filter {
            it.active && (it.source == appName ||
                    (appName == "Gmail" && it.source == "Google") ||
                    (appName == "Telegram" && it.source == "Telegram"))
        })

        // TODO: Also load from Firestore

        return workflows
    }

    /**
     * Load workflows from local JSON files
     */
    private fun loadLocalWorkflows(): List<Workflow> {
        val workflows = mutableListOf<Workflow>()

        try {
            val workflowFiles = context.filesDir.listFiles { file ->
                file.name.startsWith("workflow_") && file.name.endsWith(".json")
            }

            workflowFiles?.forEach { file ->
                try {
                    val json = JSONObject(file.readText())
                    workflows.add(
                        Workflow(
                            source = json.getString("source"),
                            sourceAccount = json.getString("sourceAccount"),
                            destination = json.getString("destination"),
                            destinationAccount = json.getString("destinationAccount"),
                            instructions = json.getString("instructions"),
                            active = json.optBoolean("active", true)
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing workflow file: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading workflows", e)
        }

        return workflows
    }

    /**
     * Log workflow execution to Firestore
     */
    private fun logWorkflowExecution(
        workflow: Workflow,
        success: Boolean,
        message: String?
    ) {
        try {
            val userId = auth.currentUser?.uid ?: return

            val log = hashMapOf(
                "userId" to userId,
                "workflow" to hashMapOf(
                    "source" to workflow.source,
                    "destination" to workflow.destination,
                    "instructions" to workflow.instructions
                ),
                "success" to success,
                "message" to (message ?: ""),
                "timestamp" to Date()
            )

            firestore.collection("workflow_logs")
                .add(log)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error logging workflow execution", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in logWorkflowExecution", e)
        }
    }

    // Data classes for content handling
    data class MessageContent(
        val from: String,
        val to: String,
        val subject: String,
        val body: String,
        val timestamp: Long
    )

    data class ProcessedMessage(
        val originalContent: MessageContent,
        val processedText: String,
        val instructions: String
    )

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }
}