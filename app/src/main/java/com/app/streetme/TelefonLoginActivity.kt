package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView  // BU İMPORT EKLENDİ!
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class TelefonLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference  // DatabaseReference olarak değiştirildi

    private lateinit var phoneNumberEditText: TextInputEditText
    private lateinit var codeEditText: TextInputEditText
    private lateinit var sendCodeButton: Button
    private lateinit var verifyButton: Button
    private lateinit var resendTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var codeInputLayout: TextInputLayout
    private lateinit var emailLoginText: TextView
    private lateinit var backButton: ImageView

    private var verificationId: String = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isCodeSent = false

    companion object {
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
        private const val TAG = "TelefonLoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telefon_login)  // Layout dosyasını oluşturman gerek

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(DATABASE_URL).reference  // DÜZELTİLDİ

        initViews()
        setupClickListeners()

        if (auth.currentUser != null) {
            goToHomeActivity()
        }
    }

    private fun initViews() {
        phoneNumberEditText = findViewById(R.id.phone_number_edit_text)
        codeEditText = findViewById(R.id.code_edit_text)
        sendCodeButton = findViewById(R.id.send_code_button)
        verifyButton = findViewById(R.id.verify_button)
        resendTextView = findViewById(R.id.resend_text_view)
        statusTextView = findViewById(R.id.status_text_view)
        codeInputLayout = findViewById(R.id.code_input_layout)
        emailLoginText = findViewById(R.id.email_login_text)
        backButton = findViewById(R.id.back_button)

        phoneNumberEditText.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        updateUIForCodeNotSent()
    }

    private fun setupClickListeners() {
        sendCodeButton.setOnClickListener { sendVerificationCode() }
        verifyButton.setOnClickListener { verifyCode() }
        resendTextView.setOnClickListener { resendCode() }
        emailLoginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        backButton.setOnClickListener { finish() }
    }

    private fun sendVerificationCode() {
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        if (phoneNumber.isEmpty()) {
            phoneNumberEditText.error = "Telefon numarası giriniz"
            return
        }

        val formattedNumber = formatPhoneNumber(phoneNumber)
        Log.d(TAG, "Kod gönderiliyor: $formattedNumber")

        statusTextView.text = "Kod gönderiliyor..."
        statusTextView.visibility = View.VISIBLE
        sendCodeButton.isEnabled = false

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "Otomatik doğrulama başarılı")
                    statusTextView.text = "Otomatik doğrulama başarılı"
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "Doğrulama başarısız: ${e.message}")
                    statusTextView.text = "Hata: ${e.message}"
                    statusTextView.visibility = View.VISIBLE
                    sendCodeButton.isEnabled = true
                    Toast.makeText(this@TelefonLoginActivity, "Doğrulama başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "Kod SMS olarak gönderildi")
                    this@TelefonLoginActivity.verificationId = verificationId
                    this@TelefonLoginActivity.resendToken = token

                    statusTextView.text = "Kod gönderildi! SMS'inizi kontrol edin."
                    isCodeSent = true

                    runOnUiThread {
                        updateUIForCodeSent()
                    }
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode() {
        val code = codeEditText.text.toString().trim()
        Log.d(TAG, "Kod doğrulanıyor: $code")

        if (code.isEmpty()) {
            codeEditText.error = "Doğrulama kodunu giriniz"
            return
        }

        if (code.length < 6) {
            codeEditText.error = "6 haneli kodu giriniz"
            return
        }

        statusTextView.text = "Doğrulanıyor..."
        statusTextView.visibility = View.VISIBLE
        verifyButton.isEnabled = false

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun resendCode() {
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        if (phoneNumber.isEmpty()) {
            phoneNumberEditText.error = "Telefon numarası giriniz"
            return
        }

        val formattedNumber = formatPhoneNumber(phoneNumber)
        Log.d(TAG, "Kod yeniden gönderiliyor: $formattedNumber")

        statusTextView.text = "Kod yeniden gönderiliyor..."
        statusTextView.visibility = View.VISIBLE
        resendTextView.isEnabled = false

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "Yeniden gönderme başarısız: ${e.message}")
                    statusTextView.text = "Hata: ${e.message}"
                    resendTextView.isEnabled = true
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "Kod tekrar gönderildi")
                    this@TelefonLoginActivity.verificationId = verificationId
                    this@TelefonLoginActivity.resendToken = token
                    statusTextView.text = "Kod tekrar gönderildi!"
                    resendTextView.isEnabled = true

                    runOnUiThread {
                        updateUIForCodeSent()
                    }
                }
            })
            .setForceResendingToken(resendToken!!)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Giriş başarılı!")
                    statusTextView.text = "Giriş başarılı! Yönlendiriliyor..."
                    statusTextView.visibility = View.VISIBLE

                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        checkAndCreateUser(userId)
                    } else {
                        goToHomeActivity()
                    }
                } else {
                    Log.e(TAG, "Giriş başarısız: ${task.exception?.message}")
                    statusTextView.text = "Giriş başarısız: ${task.exception?.message}"
                    statusTextView.visibility = View.VISIBLE
                    verifyButton.isEnabled = true
                }
            }
    }

    private fun checkAndCreateUser(userId: String) {
        val userRef = database.child("users").child(userId)

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                if (!snapshot.exists()) {
                    Log.d(TAG, "Yeni kullanıcı oluşturuluyor: $userId")
                    val phoneNumber = auth.currentUser?.phoneNumber ?: ""

                    val newUser = StreetMeUser(
                        id = userId,
                        adSoyad = "StreetMe Kullanıcısı",
                        email = "",
                        telefon = phoneNumber
                    )

                    userRef.setValue(newUser).addOnCompleteListener { saveTask ->
                        if (saveTask.isSuccessful) {
                            Log.d(TAG, "Yeni kullanıcı kaydedildi")
                        } else {
                            Log.e(TAG, "Kullanıcı kaydedilemedi: ${saveTask.exception?.message}")
                        }
                        goToHomeActivity()
                    }
                } else {
                    Log.d(TAG, "Kullanıcı zaten var: $userId")
                    goToHomeActivity()
                }
            } else {
                Log.e(TAG, "Kullanıcı kontrolü başarısız: ${task.exception?.message}")
                goToHomeActivity()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Kullanıcı kontrolü başarısız: ${e.message}")
            goToHomeActivity()
        }
    }

    private fun formatPhoneNumber(number: String): String {
        if (number.startsWith("+")) {
            return number
        }

        val digits = number.replace("[^0-9]".toRegex(), "")

        return when {
            digits.startsWith("0") && digits.length == 11 -> "+90${digits.substring(1)}"
            digits.length == 10 -> "+90$digits"
            digits.startsWith("90") && digits.length == 12 -> "+$digits"
            else -> "+90$digits"
        }
    }

    private fun updateUIForCodeSent() {
        Log.d(TAG, "UI güncelleniyor: Kod gönderildi modu")
        phoneNumberEditText.isEnabled = false
        sendCodeButton.visibility = Button.GONE

        codeInputLayout.visibility = View.VISIBLE
        codeEditText.visibility = View.VISIBLE
        verifyButton.visibility = Button.VISIBLE
        resendTextView.visibility = View.VISIBLE
    }

    private fun updateUIForCodeNotSent() {
        Log.d(TAG, "UI güncelleniyor: Kod gönderilmemiş mod")
        phoneNumberEditText.isEnabled = true
        sendCodeButton.visibility = Button.VISIBLE

        codeInputLayout.visibility = View.GONE
        codeEditText.visibility = View.GONE
        verifyButton.visibility = Button.GONE
        resendTextView.visibility = View.GONE
    }

    private fun goToHomeActivity() {
        Log.d(TAG, "HomeActivity'e yönlendiriliyor...")
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}