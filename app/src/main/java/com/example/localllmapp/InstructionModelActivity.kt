package com.example.localllmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight

class InstructionModelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    InstructionChatScreen(
                        onSend = { userMessage, onResult -> runTextGeneration(userMessage, onResult) },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }

    private fun runTextGeneration(prompt: String, onResult: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // TODO: Implement MediaPipe text generation here
                val result = "MediaPipe integration pending. Input received: $prompt"

                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Error: ${e.message}")
                }
            }
        }
    }
}

// --- COMPOSABLES ---

@Composable
fun InstructionChatScreen(
    onSend: (String, (String) -> Unit) -> Unit,
    onBackClick: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var isLoading by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF191919), Color(0xFF333639))
    )
    val chatPanelColor = Color(0xFF23272A)
    val appBarColor = Color(0xFF282C34)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(chatPanelColor)
        ) {
            // Top app bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(appBarColor)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "IntraAI (Instruction Model)",
                    color = Color(0xFF8AB4F8), // Blue accent for IntraAI
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { (message, isUser) ->
                    ChatBubble(text = message, isUser = isUser)
                }
                if (isLoading) {
                    item {
                        ChatBubble(text = "Thinking...", isUser = false)
                    }
                }
            }

            // Input panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181A1B))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF23272A), shape = RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.White)
                )
                Spacer(modifier = Modifier.width(8.dp))

                val buttonEnabled = !isLoading
                val buttonColor = if (buttonEnabled) MaterialTheme.colorScheme.primary else Color(0xFFB0B0B0) // nice light grey
                Button(
                    onClick = {
                        val prompt = input.text
                        if (prompt.isNotBlank() && buttonEnabled) { // <--- Don't send if loading
                            messages = messages + (prompt to true)
                            input = TextFieldValue("")
                            isLoading = true
                            onSend(prompt) { response ->
                                isLoading = false
                                messages = messages + (response to false)
                            }
                        }
                    },
                    enabled = buttonEnabled, // <--- Button is disabled when loading
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White,      // Always white text (no fade!)
                        disabledContainerColor = buttonColor, // Ensures grey when disabled
                        disabledContentColor = Color.White // Keeps white text when disabled
                    )
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    val bubbleColor = if (isUser) Color(0xFF3A89FF) else Color(0xFF36393F)
    val textColor = if (isUser) Color.White else Color(0xFFECECEC)
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
                .widthIn(max = 300.dp)
        ) {
            Text(text = text, fontSize = 15.sp, color = textColor)
        }
    }
}