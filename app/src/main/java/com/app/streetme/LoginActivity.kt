package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var sifreEditText: EditText
    private lateinit var girisButton: Button
    private lateinit var kayitOlText: TextView
    private lateinit var telefonGirisText: TextView
    private lateinit var progressBar: android.widget.ProgressBar

    companion object {
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupClickListeners()

        if (auth.currentUser != null) {
            goToHomeActivity()
        }
    }

    private fun initViews() {
        emailEditText = findViewById(R.id.email_edit_text)
        sifreEditText = findViewById(R.id.sifre_edit_text)
        girisButton = findViewById(R.id.giris_button)
        kayitOlText = findViewById(R.id.kayit_ol_text)
        telefonGirisText = findViewById(R.id.telefon_giris_text)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupClickListeners() {
        girisButton.setOnClickListener { login() }
        kayitOlText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        telefonGirisText.setOnClickListener {
            startActivity(Intent(this, TelefonLoginActivity::class.java))
        }
    }

    private fun login() {
        val email = emailEditText.text.toString().trim()
        val sifre = sifreEditText.text.toString().trim()

        if (email.isEmpty()) {
            emailEditText.error = "E-posta giriniz"
            return
        }
        if (sifre.isEmpty()) {
            sifreEditText.error = "Şifre giriniz"
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        girisButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, sifre)
            .addOnCompleteListener { task ->
                progressBar.visibility = android.view.View.GONE
                girisButton.isEnabled = true

                if (task.isSuccessful) {
                    goToHomeActivity()
                } else {
                    Toast.makeText(this, "Giriş başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}