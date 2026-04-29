package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.map.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var userNameText: TextView
    private lateinit var logoutIcon: ImageView
    private lateinit var auth: FirebaseAuth

    // URL'yi sadece google-services.json okunamıyorsa kullanın.
    // Aksi takdirde Firebase.database.reference daha güvenlidir.
    companion object {
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        // Kullanıcı giriş yapmamışsa Login'e yönlendir
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        initViews()
        setupBottomNav()
        loadUserInfo()

        if (savedInstanceState == null) {
            showIlanlarFragment()
        }

        logoutIcon.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottom_navigation)
        userNameText = findViewById(R.id.user_name_text)
        logoutIcon = findViewById(R.id.logout_icon)
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_harita -> {
                    // Harita genellikle ayrı bir Activity olabilir
                    startActivity(Intent(this, MainActivity::class.java))
                    false // HomeActivity'de seçili kalmasın, geri dönünce ilanlar kalsın
                }
                R.id.nav_ilanlar -> {
                    showIlanlarFragment()
                    true
                }
                R.id.nav_paylas -> {
                    startActivity(Intent(this, IlanPaylasActivity::class.java))
                    false
                }
                R.id.nav_profil -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                R.id.nav_sohbet -> {
                    startActivity(Intent(this, ChatListActivity::class.java))
                    false
                }
                else -> false
            }
        }

        // Varsayılan olarak İlanlar sekmesini seç
        bottomNav.selectedItemId = R.id.nav_ilanlar
    }

    private fun showIlanlarFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, IlanlarFragment())
            //.addToBackStack(null) // Ana sayfa olduğu için backstack'e eklememek daha iyidir
            .commit()
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return

        // Manuel URL yerine direkt referans denenebilir: Firebase.database.reference
        Firebase.database(DATABASE_URL).reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isFinishing) { // Activity kapanıyorsa işlem yapma
                        val user = snapshot.getValue(StreetMeUser::class.java)
                        val displayName = user?.adSoyad ?: "Kullanıcı"
                        userNameText.text = "Hoş geldin, $displayName!"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    userNameText.text = "Hoş geldin!"
                }
            })
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Geri tuşuna basıldığında eğer İlanlar'da değilsek oraya dönmesini sağlamak için:
    override fun onBackPressed() {
        if (bottomNav.selectedItemId != R.id.nav_ilanlar) {
            bottomNav.selectedItemId = R.id.nav_ilanlar
        } else {
            super.onBackPressed()
        }
    }
}