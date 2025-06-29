@file:Suppress("DEPRECATION")

package com.example.localllmapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        init {
            System.loadLibrary("omp")         // << Must be first
            System.loadLibrary("ggml-cpu")
            System.loadLibrary("llama")         // Prebuilt llama.so
            System.loadLibrary("LocalLLMApp")   // Your JNI wrapper
        }
    }

    external fun runTextOnlyLlama(prompt: String, modelPath: String): String
    external fun runMultimodalLlama(imageData: ByteArray, prompt: String, modelPath: String): String

    private lateinit var outputView: TextView
    private lateinit var textInput: EditText
    private lateinit var loginStatus: TextView
    private lateinit var googleSignInClient: GoogleSignInClient
    private val PICK_IMAGE_REQUEST = 1
    private var imageBytes: ByteArray? = null

    // Model paths - these should be placed in your app/src/main/assets folder
    private val TEXT_MODEL_PATH = "c:/Users/sakib/AndroidStudioProjects/LocalLLMApp/app/src/main/assets/Qwen3-0.6B-UD-Q5_K_XL.gguf"
    private val MULTIMODAL_MODEL_PATH = "app/src/main/assets/gemma-3-4b-it-Q4_1.gguf" // Update with your actual Gemma 3 model filename

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        textInput = findViewById(R.id.textInput)
        outputView = findViewById(R.id.outputView)
        loginStatus = findViewById(R.id.loginStatus)

        // Google Sign-In client config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Logout Button with confirmation dialog
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { dialog, _ ->
                    // Sign out from Google and Firebase
                    googleSignInClient.signOut().addOnCompleteListener(this) {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // Button Actions
        findViewById<Button>(R.id.runLLM).setOnClickListener {
            val input = textInput.text.toString()
            if (input.isNotEmpty()) {
                runTextLLM(input)
            } else {
                outputView.text = "Please enter some text."
            }
        }

        findViewById<Button>(R.id.runMultimodal).setOnClickListener {
            if (imageBytes != null) {
                val prompt = textInput.text.toString()
                runMultimodalLLM(imageBytes!!, prompt)
            } else {
                outputView.text = "Please select an image first."
            }
        }

        findViewById<Button>(R.id.selectImage).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Status placeholder
        loginStatus.text = "Logged in via LoginActivity"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            imageBytes = contentResolver.openInputStream(uri!!)?.readBytes()
            Toast.makeText(this, "Image loaded!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runTextLLM(input: String) {
        // Show loading state
        outputView.text = "Processing..."

        // Run in background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelPath = getModelPath("Qwen3-0.6B-UD-Q5_K_XL.gguf")
                val result = runTextOnlyLlama(input, modelPath)

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    outputView.text = result
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    outputView.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun runMultimodalLLM(image: ByteArray, prompt: String) {
        // Show loading state
        outputView.text = "Processing image and text..."

        // Run in background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = runMultimodalLlama(image, prompt, MULTIMODAL_MODEL_PATH)

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    outputView.text = result
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    outputView.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun getModelPath(assetName: String): String {
        val file = File(filesDir, assetName)
        if (!file.exists()) {
            assets.open(assetName).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file.absolutePath
    }
}