@file:Suppress("DEPRECATION")

package com.example.localllmapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var outputView: TextView
    private lateinit var textInput: EditText
    private lateinit var loginStatus: TextView
    private val PICK_IMAGE_REQUEST = 1
    private var imageBytes: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        textInput = findViewById(R.id.textInput)
        outputView = findViewById(R.id.outputView)
        loginStatus = findViewById(R.id.loginStatus)

        // Button Actions
        findViewById<Button>(R.id.runLLM).setOnClickListener {
            val input = textInput.text.toString()
            outputView.text = mockRunLLM(input)
        }

        findViewById<Button>(R.id.runMultimodal).setOnClickListener {
            if (imageBytes != null) {
                outputView.text = mockRunMultimodal(imageBytes!!)
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

    private fun mockRunLLM(input: String): String {
        return "🤖 [Mock LLM Output]: $input"
    }

    private fun mockRunMultimodal(image: ByteArray): String {
        return "🖼️ [Mock Multimodal Output]: Processed image (${image.size} bytes)"
    }
}
