package com.streetme.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.streetme.app.models.Product
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode

class ArCameraActivity : AppCompatActivity() {

    private lateinit var sceneView: ArSceneView
    private lateinit var statusText: TextView
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_camera)

        db = FirebaseFirestore.getInstance()
        sceneView = findViewById(R.id.sceneView)
        statusText = findViewById(R.id.statusText)

        findViewById<android.view.View>(R.id.btn_back_to_map).setOnClickListener {
            finish()
        }

        // Firestore'daki ilanları AR dünyasına yükle
        loadProductsToAr()
    }

    private fun loadProductsToAr() {
        db.collection("products").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val product = document.toObject(Product::class.java)
                place3DModel(product)
            }
        }
    }

    private fun place3DModel(product: Product) {
        // Her ürün için bir 3D model node'u oluştur
        val modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            // Şimdilik standart bir model linki kullanıyoruz (Buraya kendi .glb linklerini koyabilirsin)
            loadModelGlbAsync(
                glbFileLocation = "https://sceneview.github.io/assets/models/DamagedHelmet.glb",
                autoAnimate = true,
                scaleToUnits = 1.0f // 1 metre boyutunda
            )
        }

        // TODO: Geospatial API ile koordinat sabitleme burada yapılacak
        // Şimdilik kameranın önüne 2 metre mesafeye koyuyoruz (Test için)
        sceneView.addChild(modelNode)
        statusText.text = "Gebze: ${product.name} tespit edildi!"
    }

    // ArCameraActivity.kt içindeki bu kısmı değiştir:

    override fun onPause() {
        super.onPause()
        // Eğer hata veriyorsa sceneView.pause() satırını tamamen sil
    }

    override fun onResume() {
        super.onResume()
        // Eğer hata veriyorsa sceneView.resume() satırını tamamen sil
       }
}