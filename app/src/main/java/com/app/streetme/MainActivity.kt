package com.streetme.app

import android.Manifest
import android.content.Intent  // BU İMPORT EKLENDİ!
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
    private val ilanListesi = arrayListOf<Ilan>()
    private var currentLocation: Location? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: IlanAdapter

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val NEARBY_RADIUS = 5000.0 // 5 km
        private const val DATABASE_URL = "https://streetme-b19f5-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupRecyclerView()
        checkUser()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_nearby)
        adapter = IlanAdapter(ilanListesi) { ilan ->
            IlanDetayActivity.start(this, ilan)
        }
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
    }

    private fun checkUser() {
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))  // DÜZELTİLDİ
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
                    updateMyLocation(it)
                }
            }
        }
    }

    private fun updateMyLocation(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "lastSeen" to System.currentTimeMillis()
        )
        database.child("locations").child(userId).setValue(locationData)

        val userLatLng = LatLng(location.latitude, location.longitude)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))

        loadNearbyIlans(location)
    }

    private fun loadIlans() {
        database.child("ilans").orderByChild("durum").equalTo("aktif")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ilanListesi.clear()
                    for (ilanSnapshot in snapshot.children) {
                        val ilan = ilanSnapshot.getValue(Ilan::class.java)
                        ilan?.let {
                            ilanListesi.add(it)
                            if (currentLocation != null) {
                                val distance = calculateDistance(
                                    currentLocation!!.latitude,
                                    currentLocation!!.longitude,
                                    it.latitude,
                                    it.longitude
                                )
                                if (distance <= NEARBY_RADIUS) {
                                    addIlanMarker(it)
                                }
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadNearbyIlans(location: Location) {
        database.child("ilans").orderByChild("durum").equalTo("aktif")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ilanSnapshot in snapshot.children) {
                        val ilan = ilanSnapshot.getValue(Ilan::class.java)
                        ilan?.let {
                            val distance = calculateDistance(
                                location.latitude,
                                location.longitude,
                                it.latitude,
                                it.longitude
                            )
                            if (distance <= NEARBY_RADIUS) {
                                addIlanMarker(it)
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addIlanMarker(ilan: Ilan) {
        ilanMarkers[ilan.id]?.remove()

        val markerOptions = MarkerOptions()
            .position(LatLng(ilan.latitude, ilan.longitude))
            .title(ilan.baslik)
            .snippet("${ilan.getFiyatText()} - ${ilan.kategori}")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))

        val marker = mMap.addMarker(markerOptions)
        if (marker != null) {
            marker.tag = ilan.id
            ilanMarkers[ilan.id] = marker
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (checkLocationPermission()) {
            mMap.isMyLocationEnabled = true
        }

        mMap.setOnMarkerClickListener { marker ->
            val ilanId = marker.tag as? String ?: return@setOnMarkerClickListener false
            val ilan = ilanListesi.find { it.id == ilanId }
            ilan?.let {
                IlanDetayActivity.start(this, it)
            }
            true
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c * 1000
    }
}