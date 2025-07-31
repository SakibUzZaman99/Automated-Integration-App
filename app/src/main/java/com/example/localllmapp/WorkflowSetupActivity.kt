package com.example.localllmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import java.io.File
import org.json.JSONObject
import android.widget.Toast

class WorkflowSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    WorkflowSetupScreen(
                        onSaveClick = { source, sourceAccount, destination, destinationAccount, instructions ->
                            saveWorkflowToJson(source, sourceAccount, destination, destinationAccount, instructions)
                        },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }

    private fun saveWorkflowToJson(
        source: String,
        sourceAccount: String,
        destination: String,
        destinationAccount: String,
        instructions: String
    ) {
        try {
            val workflow = JSONObject().apply {
                put("source", source)
                put("sourceAccount", sourceAccount)
                put("destination", destination)
                put("destinationAccount", destinationAccount)
                put("instructions", instructions)
                put("timestamp", System.currentTimeMillis())
            }

            val fileName = "workflow_${System.currentTimeMillis()}.json"
            val file = File(filesDir, fileName)
            file.writeText(workflow.toString())

            Toast.makeText(this, "Workflow saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving workflow: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun WorkflowSetupScreen(
    onSaveClick: (String, String, String, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedSource by remember { mutableStateOf("") }
    var sourceAccount by remember { mutableStateOf(TextFieldValue("")) }
    var selectedDestination by remember { mutableStateOf("") }
    var destinationAccount by remember { mutableStateOf(TextFieldValue("")) }
    var instructions by remember { mutableStateOf(TextFieldValue("")) }
    var showSourceDropdown by remember { mutableStateOf(false) }
    var showDestinationDropdown by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Validation functions
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() &&
                email.contains("@gmail.com") &&
                email.matches(Regex("^[a-zA-Z0-9._%+-]+@gmail\\.com$"))
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        return phone.isNotEmpty() &&
                phone.matches(Regex("^[+]?[1-9]\\d{6,14}$")) // Minimum 7 digits total
    }

    fun validateInput(): String? {
        if (selectedSource.isEmpty()) return "Please select a source app"
        if (selectedDestination.isEmpty()) return "Please select a destination app"
        if (instructions.text.isEmpty()) return "Please enter instructions"

        // Validate source account if provided
        if (sourceAccount.text.isNotEmpty()) {
            if (selectedSource == "Google" && !isValidEmail(sourceAccount.text)) {
                return "Please enter a valid Gmail address"
            }
            if (selectedSource == "Telegram" && !isValidPhoneNumber(sourceAccount.text)) {
                return "Please enter a valid phone number"
            }
        }

        // Validate destination account (mandatory)
        if (destinationAccount.text.isEmpty()) {
            return "Please enter destination ${if (selectedDestination == "Google") "Gmail account" else "phone number"}"
        }
        if (selectedDestination == "Google" && !isValidEmail(destinationAccount.text)) {
            return "Please enter a valid Gmail address"
        }
        if (selectedDestination == "Telegram" && !isValidPhoneNumber(destinationAccount.text)) {
            return "Please enter a valid phone number"
        }

        return null // All validation passed
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF191919), Color(0xFF333639))
    )
    val chatPanelColor = Color(0xFF23272A)
    val appBarColor = Color(0xFF282C34)
    val inputBackgroundColor = Color(0xFF23272A)

    val appOptions = listOf("Google", "Telegram")

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
                    "Workflow Setup",
                    color = Color(0xFF8AB4F8),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Select Source Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Source",
                            color = Color(0xFF8AB4F8),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " *",
                            color = Color(0xFFFF4444),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Source Dropdown
                    Box {
                        OutlinedTextField(
                            value = selectedSource,
                            onValueChange = { },
                            readOnly = true,
                            placeholder = { Text("Choose source app", color = Color(0xFFB0B0B0)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color.White
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8AB4F8),
                                unfocusedBorderColor = Color(0xFF666666),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF8AB4F8)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Invisible clickable overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showSourceDropdown = !showSourceDropdown }
                        )

                        DropdownMenu(
                            expanded = showSourceDropdown,
                            onDismissRequest = { showSourceDropdown = false },
                            modifier = Modifier.background(inputBackgroundColor)
                        ) {
                            appOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
                                    onClick = {
                                        selectedSource = option
                                        sourceAccount = TextFieldValue("") // Clear input when source changes
                                        showSourceDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Source Account Input - Only show if source is selected
                    if (selectedSource.isNotEmpty()) {
                        Text(
                            text = if (selectedSource == "Google") "Gmail Account (Optional)" else "Phone Number (Optional)",
                            color = Color(0xFFB0B0B0),
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = sourceAccount,
                            onValueChange = { sourceAccount = it },
                            placeholder = {
                                Text(
                                    if (selectedSource == "Google") "Enter Gmail account" else "Enter phone number",
                                    color = Color(0xFFB0B0B0)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (sourceAccount.text.isNotEmpty() &&
                                    ((selectedSource == "Google" && !isValidEmail(sourceAccount.text)) ||
                                            (selectedSource == "Telegram" && !isValidPhoneNumber(sourceAccount.text))))
                                    Color(0xFFFF4444) else Color(0xFF8AB4F8),
                                unfocusedBorderColor = if (sourceAccount.text.isNotEmpty() &&
                                    ((selectedSource == "Google" && !isValidEmail(sourceAccount.text)) ||
                                            (selectedSource == "Telegram" && !isValidPhoneNumber(sourceAccount.text))))
                                    Color(0xFFFF4444) else Color(0xFF666666),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF8AB4F8)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Select Destination Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Destination",
                            color = Color(0xFF8AB4F8),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " *",
                            color = Color(0xFFFF4444),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Destination Dropdown
                    Box {
                        OutlinedTextField(
                            value = selectedDestination,
                            onValueChange = { },
                            readOnly = true,
                            placeholder = { Text("Choose destination app", color = Color(0xFFB0B0B0)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color.White
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8AB4F8),
                                unfocusedBorderColor = Color(0xFF666666),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF8AB4F8)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Invisible clickable overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDestinationDropdown = !showDestinationDropdown }
                        )

                        DropdownMenu(
                            expanded = showDestinationDropdown,
                            onDismissRequest = { showDestinationDropdown = false },
                            modifier = Modifier.background(inputBackgroundColor)
                        ) {
                            appOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
                                    onClick = {
                                        selectedDestination = option
                                        destinationAccount = TextFieldValue("") // Clear input when destination changes
                                        showDestinationDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Destination Account Input - Only show if destination is selected
                    if (selectedDestination.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedDestination == "Google") "Gmail Account" else "Phone Number",
                                color = Color(0xFFB0B0B0),
                                fontSize = 14.sp
                            )
                            Text(
                                text = " *",
                                color = Color(0xFFFF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedTextField(
                            value = destinationAccount,
                            onValueChange = { destinationAccount = it },
                            placeholder = {
                                Text(
                                    if (selectedDestination == "Google") "Enter Gmail account" else "Enter phone number",
                                    color = Color(0xFFB0B0B0)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (destinationAccount.text.isNotEmpty() &&
                                    ((selectedDestination == "Google" && !isValidEmail(destinationAccount.text)) ||
                                            (selectedDestination == "Telegram" && !isValidPhoneNumber(destinationAccount.text))))
                                    Color(0xFFFF4444) else Color(0xFF8AB4F8),
                                unfocusedBorderColor = if (destinationAccount.text.isNotEmpty() &&
                                    ((selectedDestination == "Google" && !isValidEmail(destinationAccount.text)) ||
                                            (selectedDestination == "Telegram" && !isValidPhoneNumber(destinationAccount.text))))
                                    Color(0xFFFF4444) else Color(0xFF666666),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF8AB4F8)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Instructions Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instructions",
                            color = Color(0xFF8AB4F8),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " *",
                            color = Color(0xFFFF4444),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        placeholder = {
                            Text(
                                "Enter your instructions here",
                                color = Color(0xFFB0B0B0)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8AB4F8),
                            unfocusedBorderColor = Color(0xFF666666),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF8AB4F8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 6,
                        singleLine = false
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        val validationError = validateInput()
                        if (validationError == null) {
                            showConfirmationDialog = true
                        } else {
                            Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3EECAC),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "SAVE WORKFLOW",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Confirmation Dialog
        if (showConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                containerColor = Color(0xFF23272A),
                title = {
                    Text(
                        "Confirm Workflow",
                        color = Color(0xFF8AB4F8),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Source: $selectedSource", color = Color.White)
                        Text(
                            "${if (selectedSource == "Google") "Gmail" else "Number"}: ${if (sourceAccount.text.isEmpty()) "Any" else sourceAccount.text}",
                            color = Color.White
                        )
                        Text("Destination: $selectedDestination", color = Color.White)
                        Text(
                            "${if (selectedDestination == "Google") "Gmail" else "Number"}: ${destinationAccount.text}",
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Instructions:", color = Color(0xFF8AB4F8), fontWeight = FontWeight.SemiBold)
                        Text(instructions.text, color = Color.White)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmationDialog = false
                            onSaveClick(
                                selectedSource,
                                if (sourceAccount.text.isEmpty()) "Any" else sourceAccount.text,
                                selectedDestination,
                                destinationAccount.text,
                                instructions.text
                            )
                        }
                    ) {
                        Text("CONFIRM", color = Color(0xFF3EECAC), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmationDialog = false }
                    ) {
                        Text("CANCEL", color = Color(0xFFEA3838), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}