package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class TelefonLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var phoneNumberEditText: TextInputEditText
    private lateinit var codeEditText: TextInputEditText
    private lateinit var sendCodeButton: Button
    private lateinit var verifyButton: Button
    private lateinit var resendTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var codeInputLayout: TextInputLayout
    private lateinit var backButton: ImageView

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    companion object {
        private const val TAG = "TelefonLogin"
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telefon_login)

        auth = FirebaseAuth.getInstance()
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        phoneNumberEditText = findViewById(R.id.phone_number_edit_text)
        codeEditText = findViewById(R.id.code_edit_text)
        sendCodeButton = findViewById(R.id.send_code_button)
        verifyButton = findViewById(R.id.verify_button)
        resendTextView = findViewById(R.id.resend_text_view)
        statusTextView = findViewById(R.id.status_text_view)
        codeInputLayout = findViewById(R.id.code_input_layout)
        backButton = findViewById(R.id.back_button)

        phoneNumberEditText.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        updateUI(isSent = false)
    }

    private fun setupClickListeners() {
        sendCodeButton.setOnClickListener {
            closeKeyboard()
            sendVerificationCode()
        }
        verifyButton.setOnClickListener {
            closeKeyboard()
            verifyCode()
        }
        backButton.setOnClickListener { finish() }
    }

    private fun sendVerificationCode() {
        val number = phoneNumberEditText.text.toString().trim()
        if (number.isEmpty() || number.length < 10) {
            phoneNumberEditText.error = "Geçerli bir numara girin"
            return
        }

        val formatted = formatPhoneNumber(number)
        setLoading(true)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formatted)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    setLoading(false)
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    setLoading(false)
                    Toast.makeText(this@TelefonLoginActivity, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    setLoading(false)
                    verificationId = id
                    resendToken = token
                    updateUI(isSent = true)
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode() {
        val code = codeEditText.text.toString().trim()
        if (code.length != 6) {
            codeEditText.error = "6 haneli kodu girin"
            return
        }

        setLoading(true)
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                checkAndCreateUser(task.result.user?.uid ?: "")
            } else {
                setLoading(false)
                Toast.makeText(this, "Giriş başarısız!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndCreateUser(userId: String) {
        val db = FirebaseDatabase.getInstance(DATABASE_URL).reference.child("users").child(userId)
        db.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val user = StreetMeUser(
                    id = userId,
                    adSoyad = "", // Kişiselleştirme için boş bırakıp sonra doldurtalım
                    telefon = auth.currentUser?.phoneNumber ?: "",
                    uyelikTarihi = System.currentTimeMillis()
                )
                db.setValue(user).addOnSuccessListener { goToHomeActivity() }
            } else {
                goToHomeActivity()
            }
        }
    }

    private fun formatPhoneNumber(number: String): String {
        val digits = number.replace("[^0-9]".toRegex(), "")
        return when {
            digits.startsWith("90") -> "+$digits"
            digits.startsWith("0") -> "+90${digits.substring(1)}"
            else -> "+90$digits"
        }
    }

    private fun updateUI(isSent: Boolean) {
        sendCodeButton.visibility = if (isSent) View.GONE else View.VISIBLE
        codeInputLayout.visibility = if (isSent) View.VISIBLE else View.GONE
        verifyButton.visibility = if (isSent) View.VISIBLE else View.GONE
        resendTextView.visibility = if (isSent) View.VISIBLE else View.GONE
        phoneNumberEditText.isEnabled = !isSent
    }

    private fun setLoading(isLoading: Boolean) {
        statusTextView.visibility = if (isLoading) View.VISIBLE else View.GONE
        sendCodeButton.isEnabled = !isLoading
        verifyButton.isEnabled = !isLoading
    }

    private fun closeKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun goToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}