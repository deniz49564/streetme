package com.streetme.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class IlanPaylasActivity : AppCompatActivity() {

    private lateinit var resimImageView: ImageView
    private lateinit var baslikEdit: EditText
    private lateinit var fiyatEdit: EditText
    private lateinit var aciklamaEdit: EditText
    private lateinit var kategoriSpinner: Spinner
    private lateinit var paylasButton: Button
    private lateinit var progressBar: ProgressBar

    private var secilenGorsel: Uri? = null
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ilan_paylas)

        setupViews()
        setupSpinner()

        // Görsel seçme işlemi
        val resimSecici = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                secilenGorsel = result.data?.data
                resimImageView.setImageURI(secilenGorsel)
            }
        }

        resimImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            resimSecici.launch(intent)
        }

        paylasButton.setOnClickListener { ilaniHazirla() }
    }

    private fun setupViews() {
        resimImageView = findViewById(R.id.resim_image_view)
        baslikEdit = findViewById(R.id.baslik_edit_text)
        fiyatEdit = findViewById(R.id.fiyat_edit_text)
        aciklamaEdit = findViewById(R.id.aciklama_edit_text)
        kategoriSpinner = findViewById(R.id.kategori_spinner)
        paylasButton = findViewById(R.id.paylas_button)
        progressBar = findViewById(R.id.progress_bar)
        findViewById<ImageView>(R.id.geri_button).setOnClickListener { finish() }
    }

    private fun setupSpinner() {
        val kategoriler = arrayOf("Elektronik", "Moda", "Ev Eşyası", "Hizmet", "Diğer")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategoriler)
        kategoriSpinner.adapter = adapter
    }

    private fun ilaniHazirla() {
        val baslik = baslikEdit.text.toString()
        val fiyat = fiyatEdit.text.toString()

        if (baslik.isEmpty() || fiyat.isEmpty() || secilenGorsel == null) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun ve resim seçin!", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        paylasButton.isEnabled = false

        // Mevcut konumu al ve yükle
        getLocationAndUpload(baslik, fiyat.toDouble())
    }

    private fun getLocationAndUpload(baslik: String, fiyat: Double) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0
                uploadImage(baslik, fiyat, lat, lng)
            }
        } else {
            // İzin yoksa varsayılan veya hata
            uploadImage(baslik, fiyat, 0.0, 0.0)
        }
    }

    private fun uploadImage(baslik: String, fiyat: Double, lat: Double, lng: Double) {
        val uuid = UUID.randomUUID().toString()
        val gorselYolu = storage.child("ilanlar/$uuid.jpg")

        gorselYolu.putFile(secilenGorsel!!).addOnSuccessListener {
            gorselYolu.downloadUrl.addOnSuccessListener { uri ->
                saveToDatabase(baslik, fiyat, lat, lng, uri.toString())
            }
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            paylasButton.isEnabled = true
            Toast.makeText(this, "Resim yüklenemedi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToDatabase(baslik: String, fiyat: Double, lat: Double, lng: Double, url: String) {
        val ilanId = database.child("ilans").push().key ?: return
        val aciklama = aciklamaEdit.text.toString()
        val kategori = kategoriSpinner.selectedItem.toString()

        val ilanData = mapOf(
            "id" to ilanId,
            "saticiId" to auth.currentUser?.uid,
            "baslik" to baslik,
            "fiyat" to fiyat,
            "aciklama" to aciklama,
            "kategori" to kategori,
            "imageUrl" to url,
            "latitude" to lat,
            "longitude" to lng,
            "durum" to "aktif",
            "timestamp" to System.currentTimeMillis()
        )

        database.child("ilans").child(ilanId).setValue(ilanData).addOnSuccessListener {
            Toast.makeText(this, "İlan başarıyla sokağa bırakıldı!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}