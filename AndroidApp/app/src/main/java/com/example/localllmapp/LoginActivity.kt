package com.example.localllmapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize spinner (ProgressBar)
        progressBar = findViewById(R.id.progress_bar)
        progressBar.visibility = View.GONE  // Hide by default

        // Initialize Sign-In Button
        val signInButton: SignInButton = findViewById(R.id.sign_in_button)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        // Check if user already logged in
        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Sign-In Button Click Logic
        signInButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            signInUser()
        }
    }

    private fun signInUser() {
        // You can replace this with actual sign-in logic.
        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Login complete", Toast.LENGTH_SHORT).show()

            // Navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000) // Simulates a 3-second delay
    }
}
