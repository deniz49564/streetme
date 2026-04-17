package com.streetme.app

import android.content.Intent  // BU İMPORT ÖNEMLİ!
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity  // BU İMPORT ÖNEMLİ!
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
            activity.startActivity(intent)  // ARTIK ÇALIŞIR
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ilan_detay)

        initViews()

        ilanId = intent.getStringExtra("ilan_id") ?: ""
        if (ilanId.isEmpty()) {
            Toast.makeText(this, "İlan bulunamadı", Toast.LENGTH_SHORT).show()
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
    }

    private fun loadIlan() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        Firebase.database.reference.child("ilans").child(ilanId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        ilan = snapshot.getValue(Ilan::class.java)
                        ilan?.let {
                            updateUI(it)

                            isIlanSahibi = currentUserId == it.userId

                            if (isIlanSahibi) {
                                silButton.visibility = View.VISIBLE
                                mesajButton.visibility = View.GONE
                            } else {
                                silButton.visibility = View.GONE
                                mesajButton.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        Toast.makeText(this@IlanDetayActivity, "İlan bulunamadı", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@IlanDetayActivity, "İlan yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun updateUI(ilan: Ilan) {
        baslikText.text = ilan.baslik
        aciklamaText.text = ilan.aciklama
        fiyatText.text = ilan.getFiyatText()
        kategoriText.text = ilan.kategori

        if (ilan.resimUrl.isNotEmpty()) {
            Glide.with(this).load(ilan.resimUrl).into(ilanImage)
        }

        Firebase.database.reference.child("users").child(ilan.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(StreetMeUser::class.java)
                    satanKisiText.text = user?.getDisplayName() ?: "Kullanıcı"
                }

                override fun onCancelled(error: DatabaseError) {
                    satanKisiText.text = "Kullanıcı"
                }
            })
    }

    private fun mesajGonder() {
        val currentIlan = ilan
        if (currentIlan == null) {
            Toast.makeText(this, "İlan yüklenemedi", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == currentIlan.userId) {
            Toast.makeText(this, "Kendi ilanına mesaj gönderemezsin", Toast.LENGTH_SHORT).show()
            return
        }

        Firebase.database.reference.child("users").child(currentIlan.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(StreetMeUser::class.java)
                    if (user != null) {
                        val intent = Intent(this@IlanDetayActivity, ChatActivity::class.java)
                        intent.putExtra("user_id", user.id)
                        intent.putExtra("user_name", user.getDisplayName())
                        intent.putExtra("user_avatar", user.profilFotoUrl)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@IlanDetayActivity, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@IlanDetayActivity, "Kullanıcı bilgileri alınamadı", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun ilanSil() {
        val currentIlan = ilan ?: return

        AlertDialog.Builder(this)
            .setTitle("İlanı Sil")
            .setMessage("Bu ilanı silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                Firebase.database.reference.child("ilans").child(currentIlan.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "İlan silindi", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Silme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }
}