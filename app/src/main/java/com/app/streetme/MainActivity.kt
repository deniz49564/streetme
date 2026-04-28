package com.streetme.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val ilanMarkers = hashMapOf<String, Marker?>()
    private var currentLocation: Location? = null
    private var isFirstLocationUpdate = true

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: IlanAdapter

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val NEARBY_RADIUS = 5000.0
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupRecyclerView()
        checkUser()
    }

    // --- YENİLİKÇİ 3D HARİTA AYARLARI ---
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 3D Binaları ve İç Mekanları Aktif Et
        mMap.isBuildingsEnabled = true
        mMap.isIndoorEnabled = true

        // UI Ayarları: Kullanıcıya özgürlük tanı
        mMap.uiSettings.apply {
            isMyLocationButtonEnabled = true
            isTiltGesturesEnabled = true   // İki parmakla eğme
            isRotateGesturesEnabled = true // Döndürme
            isCompassEnabled = false       // Daha temiz UI için kapattık
        }

        if (checkLocationPermission()) {
            mMap.isMyLocationEnabled = true
        }

        // --- EĞLENCELİ ETKİLEŞİM ---
        mMap.setOnMarkerClickListener { marker ->
            val ilanId = marker.tag as? String ?: return@setOnMarkerClickListener false

            // Markera tıklandığında kamerayı 3D açıyla yaklaştır (Sinematik etki)
            val cameraPosition = CameraPosition.Builder()
                .target(marker.position)
                .zoom(18f)    // Yakın çekim
                .tilt(60f)    // Sert eğim (3D binaları vurgular)
                .bearing(marker.rotation) // Markera doğru dön
                .build()

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null)

            // Burada alt paneli (BottomSheet) açabilirsin
            true
        }
    }

    private fun centerMapOnLocation(location: Location) {
        if (::mMap.isInitialized) {
            val userLatLng = LatLng(location.latitude, location.longitude)

            // Uygulama ilk açıldığında 45 derece eğik (3D) başlasın
            val initial3DPosition = CameraPosition.Builder()
                .target(userLatLng)
                .zoom(16f)
                .tilt(45f) // 3D perspektifi başlatan ana komut
                .build()

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(initial3DPosition))
        }
    }

    private fun loadIlans() {
        database.child("ilans").orderByChild("durum").equalTo("aktif")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val filteredList = arrayListOf<Ilan>()
                    // Haritayı tamamen temizlemek yerine marker yönetimi yapılabilir
                    // ama basitlik için temizliyoruz
                    mMap.clear()
                    ilanMarkers.clear()

                    for (ilanSnapshot in snapshot.children) {
                        val ilan = ilanSnapshot.getValue(Ilan::class.java)
                        ilan?.let {
                            if (currentLocation != null) {
                                val distance = calculateDistance(
                                    currentLocation!!.latitude, currentLocation!!.longitude,
                                    it.latitude, it.longitude
                                )
                                if (distance <= NEARBY_RADIUS) {
                                    filteredList.add(it)
                                    addIlanMarker(it)
                                }
                            }
                        }
                    }
                    adapter.submitList(filteredList)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addIlanMarker(ilan: Ilan) {
        val markerOptions = MarkerOptions()
            .position(LatLng(ilan.latitude, ilan.longitude))
            .title(ilan.baslik)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            // İnovasyon: Marker'ı hafif eğik yapabilirsin (İsteğe bağlı)
            .flat(true)

        val marker = mMap.addMarker(markerOptions)
        marker?.tag = ilan.id
        ilanMarkers[ilan.id] = marker
    }

    // --- MEVCUT DİĞER FONKSİYONLAR (setupRecyclerView, checkUser vb.) ---
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_nearby)
        adapter = IlanAdapter { ilan ->
            IlanDetayActivity.start(this, ilan)
        }
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
    }

    private fun checkUser() {
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            startLocationUpdates()
            loadIlans()
        }
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    updateUserLocationInFirebase(it)
                    if (isFirstLocationUpdate) {
                        centerMapOnLocation(it)
                        isFirstLocationUpdate = false
                    }
                }
            }
        }
    }

    private fun updateUserLocationInFirebase(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "lastSeen" to System.currentTimeMillis()
        )
        database.child("locations").child(userId).updateChildren(locationData)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    private fun checkLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}