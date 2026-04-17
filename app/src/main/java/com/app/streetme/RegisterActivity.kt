package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var adSoyadEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var sifreEditText: EditText
    private lateinit var sifreTekrarEditText: EditText
    private lateinit var kayitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var girisYapText: TextView
    private lateinit var geriButton: ImageView

    private lateinit var auth: FirebaseAuth

    companion object {
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        adSoyadEditText = findViewById(R.id.ad_soyad_edit_text)
        emailEditText = findViewById(R.id.email_edit_text)
        sifreEditText = findViewById(R.id.sifre_edit_text)
        sifreTekrarEditText = findViewById(R.id.sifre_tekrar_edit_text)
        kayitButton = findViewById(R.id.kayit_button)
        progressBar = findViewById(R.id.progress_bar)
        girisYapText = findViewById(R.id.giris_yap_text)
        geriButton = findViewById(R.id.geri_button)
    }

    private fun setupClickListeners() {
        geriButton.setOnClickListener { finish() }
        girisYapText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        kayitButton.setOnClickListener { registerUser() }
    }

    private fun registerUser() {
        val adSoyad = adSoyadEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val sifre = sifreEditText.text.toString().trim()
        val sifreTekrar = sifreTekrarEditText.text.toString().trim()

        if (adSoyad.isEmpty()) {
            adSoyadEditText.error = "Ad soyad giriniz"
            return
        }
        if (email.isEmpty()) {
            emailEditText.error = "Email giriniz"
            return
        }
        if (sifre.isEmpty()) {
            sifreEditText.error = "Şifre giriniz"
            return
        }
        if (sifre != sifreTekrar) {
            sifreTekrarEditText.error = "Şifreler eşleşmiyor"
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        kayitButton.isEnabled = false

        // Email ve şifre ile Firebase Auth'da kullanıcı oluştur
        auth.createUserWithEmailAndPassword(email, sifre)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    val user = StreetMeUser(
                        id = userId,
                        adSoyad = adSoyad,
                        email = email,
                        telefon = ""  // Telefon opsiyonel
                    )

                    Firebase.database(DATABASE_URL).reference
                        .child("users").child(userId)
                        .setValue(user)
                        .addOnSuccessListener {
                            progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = android.view.View.GONE
                            kayitButton.isEnabled = true
                            Toast.makeText(this, "Kayıt hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    progressBar.visibility = android.view.View.GONE
                    kayitButton.isEnabled = true
                    Toast.makeText(this, "Kayıt başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}