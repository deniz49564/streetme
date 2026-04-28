package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class IlanDetayActivity : AppCompatActivity() {

    private lateinit var ilanImage: ImageView
    private lateinit var baslikText: TextView
    private lateinit var aciklamaText: TextView
    private lateinit var fiyatText: TextView
    private lateinit var kategoriText: TextView
    private lateinit var satanKisiText: TextView
    private lateinit var mesajButton: Button
    private lateinit var silButton: Button
    private lateinit var geriButton: ImageView

    private var ilanId: String = ""
    private var ilan: Ilan? = null
    private var isIlanSahibi = false

    companion object {
        fun start(activity: FragmentActivity, ilan: Ilan) {
            val intent = Intent(activity, IlanDetayActivity::class.java)
            intent.putExtra("ilan_id", ilan.id)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ilan_detay)

        initViews()

        ilanId = intent.getStringExtra("ilan_id") ?: ""
        if (ilanId.isEmpty()) {
            Toast.makeText(this, "Geçersiz ilan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadIlan()
    }

    private fun initViews() {
        ilanImage = findViewById(R.id.ilan_image)
        baslikText = findViewById(R.id.baslik_text)
        aciklamaText = findViewById(R.id.aciklama_text)
        fiyatText = findViewById(R.id.fiyat_text)
        kategoriText = findViewById(R.id.kategori_text)
        satanKisiText = findViewById(R.id.satan_kisi_text)
        mesajButton = findViewById(R.id.mesaj_button)
        silButton = findViewById(R.id.sil_button)
        geriButton = findViewById(R.id.geri_button)

        geriButton.setOnClickListener { finish() }
        mesajButton.setOnClickListener { mesajGonder() }
        silButton.setOnClickListener { ilanSil() }

        // İlk başta butonları gizle, veri gelince duruma göre göster
        silButton.visibility = View.GONE
        mesajButton.visibility = View.GONE
    }

    private fun loadIlan() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        Firebase.database.reference.child("ilans").child(ilanId)
            .addValueEventListener(object : ValueEventListener { // Dinamik olması için addValueEventListener daha iyi
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // İlan başkası tarafından silindiyse ekrandan at
                        if (!isFinishing) {
                            Toast.makeText(this@IlanDetayActivity, "İlan artık mevcut değil", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return
                    }

                    ilan = snapshot.getValue(Ilan::class.java)
                    ilan?.let {
                        updateUI(it)

                        isIlanSahibi = (currentUserId != null && currentUserId == it.userId)

                        if (isIlanSahibi) {
                            silButton.visibility = View.VISIBLE
                            mesajButton.visibility = View.GONE
                        } else {
                            silButton.visibility = View.GONE
                            mesajButton.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@IlanDetayActivity, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(ilan: Ilan) {
        baslikText.text = ilan.baslik
        aciklamaText.text = ilan.aciklama
        fiyatText.text = ilan.getFiyatText()
        kategoriText.text = ilan.kategori

        Glide.with(this)
            .load(ilan.resimUrl)
            .placeholder(R.drawable.ic_ilan_default)
            .error(R.drawable.ic_ilan_default)
            .into(ilanImage)

        // Satan kişinin adını alırken 'get()' kullanarak tek seferlik çekim yapıyoruz
        Firebase.database.reference.child("users").child(ilan.userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(StreetMeUser::class.java)
                satanKisiText.text = "Satıcı: ${user?.adSoyad ?: "Kullanıcı"}"
            }
    }

    private fun mesajGonder() {
        val targetIlan = ilan ?: return
        val myId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (myId == targetIlan.userId) {
            Toast.makeText(this, "Bu senin kendi ilanın!", Toast.LENGTH_SHORT).show()
            return
        }

        // ChatActivity'ye giderken gereken tüm bilgileri kullanıcı tablosundan doğrula
        Firebase.database.reference.child("users").child(targetIlan.userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(StreetMeUser::class.java)
                if (user != null) {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("user_id", targetIlan.userId)
                    intent.putExtra("user_name", user.adSoyad ?: "Kullanıcı")
                    startActivity(intent)
                }
            }
    }

    private fun ilanSil() {
        AlertDialog.Builder(this)
            .setTitle("İlanı Kaldır")
            .setMessage("Bu ilanı kalıcı olarak silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                // Veritabanından sil
                Firebase.database.reference.child("ilans").child(ilanId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "İlan başarıyla kaldırıldı", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}