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

    private lateinit var adapter: IlanAdapter
    private var databaseQuery: Query? = null
    private var databaseListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        setupRecyclerView()
        loadUserInfo()
        loadUserIlans()

        cikisButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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
    }

    private fun setupRecyclerView() {
        // Adapter'ı bir kez başlatıyoruz
        adapter = IlanAdapter { ilan ->
            IlanDetayActivity.start(this, ilan)
        }
        ilanlarRecyclerView.layoutManager = LinearLayoutManager(this)
        ilanlarRecyclerView.adapter = adapter
    }

    private fun loadUserInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (!isFinishing) {
                val user = snapshot.getValue(StreetMeUser::class.java)
                adSoyadText.text = user?.adSoyad ?: "İsimsiz Kullanıcı"
                telefonText.text = user?.telefon ?: "Telefon eklenmemiş"
                uyelikText.text = "Üyelik: ${user?.uyelikTarihi?.let { formatDate(it) } ?: "-"}"
            }
        }
    }

    private fun loadUserIlans() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Query'yi değişkene atıyoruz ki sonra durdurabilelim
        databaseQuery = FirebaseDatabase.getInstance().reference.child("ilans")
            .orderByChild("userId")
            .equalTo(userId)

        databaseListener = databaseQuery?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = arrayListOf<Ilan>()
                for (ilanSnapshot in snapshot.children) {
                    val ilan = ilanSnapshot.getValue(Ilan::class.java)
                    ilan?.let { list.add(it) }
                }

                // İlan sayısını direkt gerçek veriden güncelliyoruz
                ilanSayisiText.text = "Aktif İlanlarım: ${list.size}"

                // ListAdapter sayesinde sadece değişenleri günceller, performansı korur
                adapter.submitList(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val date = Date(timestamp)
            SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR")).format(date)
        } catch (e: Exception) {
            "Bilinmiyor"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Bellek sızıntısını önlemek için dinleyiciyi kapatıyoruz
        databaseListener?.let { databaseQuery?.removeEventListener(it) }
    }
}