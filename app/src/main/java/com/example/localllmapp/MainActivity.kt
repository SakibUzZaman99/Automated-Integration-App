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

class MainActivity : AppCompatActivity() {

    private lateinit var outputView: TextView
    private lateinit var textInput: EditText
    private lateinit var loginStatus: TextView
    private lateinit var googleSignInClient: GoogleSignInClient
    private val PICK_IMAGE_REQUEST = 1
    private var imageBytes: ByteArray? = null

    // This companion object block is equivalent to a static block in Java.
    // It's used to load the native library when the MainActivity class is first loaded.
    companion object {
        // Used to load the 'my_native_lib' library on application startup.
        // The name here MUST match the name defined in CMakeLists.txt (add_library)
        init {
            System.loadLibrary("my_native_lib")
        }
    }

    // Declare the native method using the 'external' keyword.
    // The method name 'stringFromJNI' corresponds to the C++ function name suffix.
    external fun stringFromJNI(): String


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        textInput = findViewById(R.id.textInput)
        outputView = findViewById(R.id.outputView)
        loginStatus = findViewById(R.id.loginStatus)

        outputView.text = "JNI Status: " + stringFromJNI() + "\n"

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
