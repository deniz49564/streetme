package com.streetme.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class IlanPaylasActivity : AppCompatActivity() {

    private lateinit var baslikEditText: EditText
    private lateinit var aciklamaEditText: EditText
    private lateinit var fiyatEditText: EditText
    private lateinit var kategoriSpinner: Spinner
    private lateinit var resimImageView: ImageView
    private lateinit var paylasButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var geriButton: ImageView

    private var secilenResimUri: Uri? = null

    private val kategoriler = arrayOf("Elektronik", "Ev Eşyası", "Giyim", "Kitap", "Diğer")

    companion object {
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ilan_paylas)

        initViews()
        setupSpinner()
        setupImagePicker()

        paylasButton.setOnClickListener { paylasIlan() }
        geriButton.setOnClickListener { finish() }
    }

    private fun initViews() {
        baslikEditText = findViewById(R.id.baslik_edit_text)
        aciklamaEditText = findViewById(R.id.aciklama_edit_text)
        fiyatEditText = findViewById(R.id.fiyat_edit_text)
        kategoriSpinner = findViewById(R.id.kategori_spinner)
        resimImageView = findViewById(R.id.resim_image_view)
        paylasButton = findViewById(R.id.paylas_button)
        progressBar = findViewById(R.id.progress_bar)
        geriButton = findViewById(R.id.geri_button)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriler)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        kategoriSpinner.adapter = adapter
    }

    private fun setupImagePicker() {
        resimImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            secilenResimUri = data?.data
            resimImageView.setImageURI(secilenResimUri)
        }
    }

    private fun paylasIlan() {
        val baslik = baslikEditText.text.toString().trim()
        val aciklama = aciklamaEditText.text.toString().trim()
        val fiyat = fiyatEditText.text.toString().trim().toDoubleOrNull() ?: 0.0
        val kategori = kategoriler[kategoriSpinner.selectedItemPosition]

        if (baslik.isEmpty()) {
            baslikEditText.error = "Başlık giriniz"
            return
        }

        progressBar.visibility = View.VISIBLE
        paylasButton.isEnabled = false

        val ilanId = Firebase.database(DATABASE_URL).reference.child("ilans").push().key ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (secilenResimUri != null) {
            // Storage'ı doğru şekilde başlat
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("ilan_resimleri/$ilanId.jpg")

            imageRef.putFile(secilenResimUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        kaydetIlan(ilanId, userId, baslik, aciklama, fiyat, kategori, uri.toString())
                    }.addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        paylasButton.isEnabled = true
                        Toast.makeText(this, "Resim URL alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    paylasButton.isEnabled = true
                    Toast.makeText(this, "Resim yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            kaydetIlan(ilanId, userId, baslik, aciklama, fiyat, kategori, "")
        }
    }

    private fun kaydetIlan(ilanId: String, userId: String, baslik: String, aciklama: String, fiyat: Double, kategori: String, resimUrl: String) {
        val konumRef = Firebase.database(DATABASE_URL).reference.child("locations").child(userId)
        konumRef.get().addOnSuccessListener { snapshot ->
            val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
            val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0

            val ilan = Ilan(
                id = ilanId,
                userId = userId,
                baslik = baslik,
                aciklama = aciklama,
                fiyat = fiyat,
                kategori = kategori,
                resimUrl = resimUrl,
                latitude = latitude,
                longitude = longitude
            )

            Firebase.database(DATABASE_URL).reference.child("ilans").child(ilanId).setValue(ilan)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "İlan paylaşıldı!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    paylasButton.isEnabled = true
                    Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            progressBar.visibility = View.GONE
            paylasButton.isEnabled = true
            Toast.makeText(this, "Konum alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}