package com.streetme.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class IlanlarFragment : Fragment() {

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

    private val ilanListesi = arrayListOf<Ilan>()
    private val tumIlanlar = arrayListOf<Ilan>()
    private lateinit var adapter: IlanAdapter

    private val kategoriler = listOf("Tümü", "Elektronik", "Ev Eşyası", "Giyim", "Kitap", "Diğer")
    private val siralamaSecenekleri = listOf("En Yeni", "En Ucuz", "En Pahalı")

    companion object {
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
        private const val TAG = "IlanlarFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ilanlar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSpinners()
        setupSearch()
        setupClickListeners()
        loadIlans()
    }

    private fun initViews(view: View) {
        searchEditText = view.findViewById(R.id.search_edit_text)
        kategoriSpinner = view.findViewById(R.id.kategori_spinner)
        siralamaSpinner = view.findViewById(R.id.siralama_spinner)
        fiyatMinEditText = view.findViewById(R.id.fiyat_min_edit_text)
        fiyatMaxEditText = view.findViewById(R.id.fiyat_max_edit_text)
        filtreleButton = view.findViewById(R.id.filtrele_button)
        temizleButton = view.findViewById(R.id.temizle_button)
        recyclerView = view.findViewById(R.id.recycler_ilanlar)
        emptyText = view.findViewById(R.id.empty_text)
        progressBar = view.findViewById(R.id.progress_bar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // DÜZELTİLDİ: IlanDetayActivity.start requireActivity() ile çağrılıyor
        adapter = IlanAdapter(ilanListesi) { ilan ->
            IlanDetayActivity.start(requireActivity(), ilan)
        }
        recyclerView.adapter = adapter

        Log.d(TAG, "Views initialized")
    }

    private fun setupSpinners() {
        val kategoriAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kategoriler)
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        kategoriSpinner.adapter = kategoriAdapter

        val siralamaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, siralamaSecenekleri)
        siralamaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        siralamaSpinner.adapter = siralamaAdapter

        Log.d(TAG, "Spinners setup")
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
        Log.d(TAG, "Loading ilans...")

        val dbRef = FirebaseDatabase.getInstance(DATABASE_URL).reference

        dbRef.child("ilans").orderByChild("durum").equalTo("aktif")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "DataSnapshot children count: ${snapshot.children.count()}")
                    tumIlanlar.clear()

                    for (ilanSnapshot in snapshot.children) {
                        val ilan = ilanSnapshot.getValue(Ilan::class.java)
                        if (ilan != null) {
                            tumIlanlar.add(ilan)
                            Log.d(TAG, "İlan eklendi: ${ilan.baslik} - ${ilan.fiyat}")
                        } else {
                            Log.e(TAG, "İlan null geldi!")
                        }
                    }

                    Log.d(TAG, "Toplam ilan sayısı: ${tumIlanlar.size}")

                    ilanListesi.clear()
                    ilanListesi.addAll(tumIlanlar)
                    adapter.notifyDataSetChanged()

                    progressBar.visibility = View.GONE

                    if (ilanListesi.isEmpty()) {
                        emptyText.visibility = View.VISIBLE
                        Log.d(TAG, "İlan listesi boş")
                    } else {
                        emptyText.visibility = View.GONE
                        Log.d(TAG, "İlan listesi dolu, ilk ilan: ${ilanListesi[0].baslik}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Database error: ${error.message}")
                    Toast.makeText(requireContext(), "İlanlar yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
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

        Log.d(TAG, "Filtrelenmiş ilan sayısı: ${ilanListesi.size}")
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