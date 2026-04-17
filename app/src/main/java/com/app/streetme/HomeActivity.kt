package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var userNameText: TextView
    private lateinit var logoutIcon: ImageView
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupBottomNav()
        loadUserInfo()

        // İlk fragment olarak IlanlarFragment'i göster
        if (savedInstanceState == null) {
            showIlanlarFragment()
        }

        logoutIcon.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottom_navigation)
        userNameText = findViewById(R.id.user_name_text)
        logoutIcon = findViewById(R.id.logout_icon)
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_harita -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_ilanlar -> {
                    showIlanlarFragment()
                    true
                }
                R.id.nav_paylas -> {
                    startActivity(Intent(this, IlanPaylasActivity::class.java))
                    true
                }
                R.id.nav_profil -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_sohbet -> {
                    startActivity(Intent(this, ChatListActivity::class.java))
                    true
                }
                else -> false
            }
        }
        // İlanlar seçili başlasın
        bottomNav.selectedItemId = R.id.nav_ilanlar
    }

    private fun showIlanlarFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, IlanlarFragment())
            .commit()
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return

        Firebase.database(DATABASE_URL).reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(StreetMeUser::class.java)
                    val displayName = user?.getDisplayName() ?: "StreetMe Kullanıcısı"
                    userNameText.text = "Hoş geldin, $displayName!"
                }

                override fun onCancelled(error: DatabaseError) {
                    userNameText.text = "Hoş geldin, StreetMe Kullanıcısı!"
                }
            })
    }
}