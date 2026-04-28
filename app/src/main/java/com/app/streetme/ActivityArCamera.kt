package com.streetme.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ViewNode

class ActivityArCamera : AppCompatActivity() {

    // 'lateinit var' olduğu için buna sadece bir kez atama yapabiliriz.
    private lateinit var arSceneView: ArSceneView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_camera)

        // DÜZELTME 1: Atamayı en sade haliyle yapıyoruz.
        // Eğer findViewById hata veriyorsa XML'deki ismin 'io.github.sceneview.ar.ArSceneView'
        // olduğundan emin olun.
        arSceneView = findViewById(R.id.arSceneView)

        // DÜZELTME 2: 'lifecycle = ...' satırı 'val' hatası veriyorsa SİLDİK.
        // SceneView genellikle bunu arka planda kendisi halleder.

        database = FirebaseDatabase.getInstance().reference.child("ilans")
        loadIlansFromFirebase()

        findViewById<android.widget.ImageView>(R.id.btn_close_ar).setOnClickListener { finish() }
    }

    private fun loadIlansFromFirebase() {
        database.orderByChild("durum").equalTo("aktif").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // DÜZELTME 3: 'children' listesi temizlenirken hata almamak için güvenli yöntem
                arSceneView.children.filterIsInstance<ViewNode>().forEach { it.parent = null }

                for (ilanSnapshot in snapshot.children) {
                    val ilan = ilanSnapshot.getValue(Ilan::class.java)
                    ilan?.let { addIlanToArWorld(it) }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ActivityArCamera, "Veri yüklenemedi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addIlanToArWorld(ilan: Ilan) {
        // DÜZELTME 4: ViewNode constructor parametreleri.
        // Eğer 'arSceneView.engine' kısmında 'val' hatası varsa,
        // kütüphane o mülke erişimi kapatmış olabilir.
        val node = ViewNode(arSceneView.engine).apply {

            // DÜZELTME 5: 'position = ...' satırı hata veriyorsa 'worldPosition' kullanın.
            // O da hata verirse: position.x = ... şeklinde tek tek atayın.
            position = Position(
                x = (Math.random().toFloat() * 4) - 2f,
                y = 0.5f,
                z = -3f
            )

            // DÜZELTME 6: loadView parametrelerini açıkça isimlendiriyoruz.
            loadView(
                context = this@ActivityArCamera,
                layoutResId = R.layout.ar_item_card
            ) { _, view ->
                view.findViewById<TextView>(R.id.ar_ilan_title).text = ilan.baslik
                view.findViewById<TextView>(R.id.ar_ilan_price).text = "${ilan.fiyat} TL"
            }

            // DÜZELTME 7: onTap parametreleri (_) ile boş geçildi.
            onTap = { _, _ ->
                Toast.makeText(this@ActivityArCamera, "${ilan.baslik} tıklandı", Toast.LENGTH_SHORT).show()
                true
            }
        }

        arSceneView.addChild(node)
    }
}