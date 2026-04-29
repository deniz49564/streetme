package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.streetme.app.databinding.ActivityMainBinding
import com.streetme.app.models.Product

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Haritayı hazırla - getMapReadyCallback DEĞİL getMapAsync olmalı
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // BottomSheet ayarları
        val bottomSheet = findViewById<View>(R.id.bottom_sheet_product)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // AR Butonu
        binding.btnOpenAr.setOnClickListener {
            val intent = Intent(this, ArCameraActivity::class.java)
            startActivity(intent)
        }
    } // onCreate BURADA BİTMELİ

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.isBuildingsEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true

        val gebzeMeydan = LatLng(40.7915, 29.4300)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gebzeMeydan, 18f))

        fetchProductsFromFirestore()

        mMap.setOnMarkerClickListener { marker ->
            val product = marker.tag as? Product
            product?.let { showProductDetails(it) }
            true
        }
    }

    private fun fetchProductsFromFirestore() {
        db.collection("products").addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener

            mMap.clear()
            for (doc in snapshots) {
                val product = doc.toObject(Product::class.java).copy(id = doc.id)
                val pos = LatLng(product.latitude, product.longitude)

                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(product.name)
                )
                marker?.tag = product
            }
        }
    }

    private fun showProductDetails(product: Product) {
        // ID'leri binding üzerinden veya doğrudan root'tan buluyoruz
        findViewById<TextView>(R.id.txt_product_name).text = product.name
        findViewById<TextView>(R.id.txt_product_price).text = product.price

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        findViewById<Button>(R.id.btn_message_owner).setOnClickListener {
            Toast.makeText(this, "${product.name} için mesaj gönderiliyor...", Toast.LENGTH_SHORT).show()
        }
    }
}