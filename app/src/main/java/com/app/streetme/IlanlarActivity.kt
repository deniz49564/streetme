package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class IlanlarActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var kategoriSpinner: Spinner
    private lateinit var siralamaSpinner: Spinner
    private lateinit var fiyatMinEditText: EditText
    private lateinit var fiyatMaxEditText: EditText
    private lateinit var filtreleButton: Button
    private lateinit var temizleButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageView
    private lateinit var haritaButton: ImageView

    private val ilanListesi = arrayListOf<Ilan>()
    private val tumIlanlar = arrayListOf<Ilan>()
    private lateinit var adapter: IlanAdapter

    private val kategoriler = listOf("Tümü", "Elektronik", "Ev Eşyası", "Giyim", "Kitap", "Diğer")
    private val siralamaSecenekleri = listOf("En Yeni", "En Ucuz", "En Pahalı")

    companion object {
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ilanlar)

        initViews()
        setupSpinners()
        setupSearch()
        setupClickListeners()
        loadIlans()
    }

    private fun initViews() {
        searchEditText = findViewById(R.id.search_edit_text)
        kategoriSpinner = findViewById(R.id.kategori_spinner)
        siralamaSpinner = findViewById(R.id.siralama_spinner)
        fiyatMinEditText = findViewById(R.id.fiyat_min_edit_text)
        fiyatMaxEditText = findViewById(R.id.fiyat_max_edit_text)
        filtreleButton = findViewById(R.id.filtrele_button)
        temizleButton = findViewById(R.id.temizle_button)
        recyclerView = findViewById(R.id.recycler_ilanlar)
        emptyText = findViewById(R.id.empty_text)
        progressBar = findViewById(R.id.progress_bar)
        backButton = findViewById(R.id.back_button)
        haritaButton = findViewById(R.id.harita_button)

        backButton.setOnClickListener { finish() }
        haritaButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = IlanAdapter(ilanListesi) { ilan ->
            IlanDetayActivity.start(this, ilan)
        }
        recyclerView.adapter = adapter
    }

    private fun setupSpinners() {
        val kategoriAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriler)
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        kategoriSpinner.adapter = kategoriAdapter

        val siralamaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, siralamaSecenekleri)
        siralamaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        siralamaSpinner.adapter = siralamaAdapter
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrele()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        filtreleButton.setOnClickListener { filtrele() }
        temizleButton.setOnClickListener { temizle() }
    }

    private fun loadIlans() {
        progressBar.visibility = View.VISIBLE

        // DÜZELTİLDİ: FirebaseDatabase.getInstance() kullan
        FirebaseDatabase.getInstance(DATABASE_URL).reference.child("ilans")
            .orderByChild("durum").equalTo("aktif")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tumIlanlar.clear()
                    for (ilanSnapshot in snapshot.children) {
                        val ilan = ilanSnapshot.getValue(Ilan::class.java)
                        ilan?.let {
                            tumIlanlar.add(it)
                        }
                    }
                    filtrele()
                    progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@IlanlarActivity, "İlanlar yüklenemedi", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filtrele() {
        val aramaMetni = searchEditText.text.toString().trim().lowercase()
        val secilenKategori = kategoriler[kategoriSpinner.selectedItemPosition]
        val minFiyat = fiyatMinEditText.text.toString().trim().toDoubleOrNull() ?: 0.0
        val maxFiyat = fiyatMaxEditText.text.toString().trim().toDoubleOrNull() ?: Double.MAX_VALUE
        val siralama = siralamaSpinner.selectedItemPosition

        val filtrelenmis = tumIlanlar.filter { ilan ->
            val kategoriUygun = if (secilenKategori == "Tümü") true else ilan.kategori == secilenKategori
            val fiyatUygun = ilan.fiyat >= minFiyat && ilan.fiyat <= maxFiyat
            val aramaUygun = aramaMetni.isEmpty() ||
                    ilan.baslik.lowercase().contains(aramaMetni) ||
                    ilan.aciklama.lowercase().contains(aramaMetni)

            kategoriUygun && fiyatUygun && aramaUygun
        }.toMutableList()

        when (siralama) {
            0 -> filtrelenmis.sortByDescending { it.ilanTarihi }
            1 -> filtrelenmis.sortBy { it.fiyat }
            2 -> filtrelenmis.sortByDescending { it.fiyat }
        }

        ilanListesi.clear()
        ilanListesi.addAll(filtrelenmis)
        adapter.notifyDataSetChanged()

        emptyText.visibility = if (ilanListesi.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun temizle() {
        searchEditText.text.clear()
        kategoriSpinner.setSelection(0)
        fiyatMinEditText.text.clear()
        fiyatMaxEditText.text.clear()
        siralamaSpinner.setSelection(0)
        filtrele()
    }
}