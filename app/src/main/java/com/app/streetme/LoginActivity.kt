package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var sifreEditText: EditText
    private lateinit var girisButton: Button
    private lateinit var kayitOlText: TextView
    private lateinit var telefonGirisText: TextView
    private lateinit var progressBar: android.widget.ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Oturum zaten açıksa direkt yönlendir
        if (auth.currentUser != null) {
            goToHomeActivity()
            return
        }

        initViews()
        setupClickListeners()
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
        girisButton.setOnClickListener {
            closeKeyboard()
            login()
        }

        kayitOlText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        telefonGirisText.setOnClickListener {
            // Telefonla giriş modülün hazırsa buraya gider
            // startActivity(Intent(this, TelefonLoginActivity::class.java))
        }
    }

    private fun login() {
        val email = emailEditText.text.toString().trim()
        val sifre = sifreEditText.text.toString().trim()

        // Gelişmiş Doğrulama
        if (email.isEmpty()) {
            emailEditText.error = "E-posta boş olamaz"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Geçerli bir e-posta adresi girin"
            return
        }
        if (sifre.length < 6) {
            sifreEditText.error = "Şifre en az 6 karakter olmalıdır"
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, sifre)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    goToHomeActivity()
                } else {
                    setLoading(false)
                    val errorMsg = when(task.exception?.message) {
                        null -> "Bilinmeyen bir hata oluştu"
                        else -> "Giriş yapılamadı. Bilgilerinizi kontrol edin."
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        girisButton.isEnabled = !isLoading
        emailEditText.isEnabled = !isLoading
        sifreEditText.isEnabled = !isLoading
    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}