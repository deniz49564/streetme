package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
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
        kayitButton.setOnClickListener {
            closeKeyboard()
            registerUser()
        }
    }

    private fun registerUser() {
        val adSoyad = adSoyadEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val sifre = sifreEditText.text.toString().trim()
        val sifreTekrar = sifreTekrarEditText.text.toString().trim()

        // --- GELİŞMİŞ DOĞRULAMA ---
        if (adSoyad.isEmpty()) {
            adSoyadEditText.error = "Lütfen adınızı ve soyadınızı girin"
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
        if (sifre != sifreTekrar) {
            sifreTekrarEditText.error = "Şifreler eşleşmiyor"
            return
        }

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, sifre)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    val user = StreetMeUser(
                        id = userId,
                        adSoyad = adSoyad,
                        email = email,
                        uyelikTarihi = System.currentTimeMillis() // Üyelik tarihini ekledik
                    )

                    // Veritabanına kaydet
                    Firebase.database(DATABASE_URL).reference
                        .child("users").child(userId)
                        .setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Aramıza hoş geldin!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            setLoading(false)
                            Toast.makeText(this, "Bilgiler kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    setLoading(false)
                    Toast.makeText(this, "Hata: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        kayitButton.isEnabled = !isLoading
        adSoyadEditText.isEnabled = !isLoading
        emailEditText.isEnabled = !isLoading
        sifreEditText.isEnabled = !isLoading
        sifreTekrarEditText.isEnabled = !isLoading
    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}