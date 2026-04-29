package com.streetme.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.streetme.app.models.Product // Klasör adın models olduğu için bu şekilde
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode

class ArIlanActivity : AppCompatActivity() {

    private lateinit var sceneView: ArSceneView
    private lateinit var txtIlanBaslik: TextView
    private lateinit var txtIlanFiyat: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_ilan)

        sceneView = findViewById(R.id.sceneViewIlan)
        txtIlanBaslik = findViewById(R.id.txt_ar_ilan_baslik)
        txtIlanFiyat = findViewById(R.id.txt_ar_ilan_fiyat)

        // Geri dön butonu
        findViewById<View>(R.id.btn_close_ar_ilan).setOnClickListener {
            finish()
        }

        // 3D Modeli sahneye ekle (Test için bir model yüklüyoruz)
        setupArModel()
    }

    private fun setupArModel() {
        val modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = "https://sceneview.github.io/assets/models/DamagedHelmet.glb",
                autoAnimate = true,
                scaleToUnits = 0.5f // İlan olduğu için biraz daha küçük (50cm)
            )
        }
        sceneView.addChild(modelNode)

        // Model yüklendiğinde bir mesaj verelim
        Toast.makeText(this, "İlan 3D olarak hazır!", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }
} // Parantezi unutmadık :)