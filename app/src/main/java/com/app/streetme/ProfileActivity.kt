package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var geriButton: ImageView
    private lateinit var adSoyadText: TextView
    private lateinit var telefonText: TextView
    private lateinit var ilanSayisiText: TextView
    private lateinit var uyelikText: TextView
    private lateinit var cikisButton: Button
    private lateinit var ilanlarRecyclerView: RecyclerView

    private var kullaniciIlaniListe = arrayListOf<Ilan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        loadUserInfo()
        loadUserIlans()

        cikisButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
            startActivity(Intent(this, LoginActivity::class.java))
        }

        geriButton.setOnClickListener { finish() }
    }

    private fun initViews() {
        geriButton = findViewById(R.id.geri_button)
        adSoyadText = findViewById(R.id.ad_soyad_text)
        telefonText = findViewById(R.id.telefon_text)
        ilanSayisiText = findViewById(R.id.ilan_sayisi_text)
        uyelikText = findViewById(R.id.uyelik_text)
        cikisButton = findViewById(R.id.cikis_button)
        ilanlarRecyclerView = findViewById(R.id.ilanlar_recycler)

        ilanlarRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadUserInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Firebase.database.reference.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(StreetMeUser::class.java)
                adSoyadText.text = user?.adSoyad ?: "İsimsiz"
                telefonText.text = user?.telefon ?: "Telefon yok"
                ilanSayisiText.text = "İlan Sayısı: ${user?.ilanSayisi ?: 0}"
                uyelikText.text = "Üyelik Tarihi: ${user?.uyelikTarihi?.let { formatDate(it) } ?: "Bilinmiyor"}"
            }
    }

    private fun loadUserIlans() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Firebase.database.reference.child("ilans")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    kullaniciIlaniListe.clear()
                    for (ilanSnapshot in snapshot.children) {
                        val ilan = ilanSnapshot.getValue(Ilan::class.java)
                        ilan?.let {
                            kullaniciIlaniListe.add(it)
                        }
                    }
                    ilanlarRecyclerView.adapter = IlanAdapter(kullaniciIlaniListe) { ilan ->
                        IlanDetayActivity.start(this@ProfileActivity, ilan)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return format.format(date)
    }
}