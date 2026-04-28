package com.streetme.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ViewNode

class ArIlanActivity : AppCompatActivity() {

    private lateinit var arSceneView: ArSceneView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML'inizde ArSceneView ID'sinin arSceneView olduğundan emin olun
        setContentView(R.layout.activity_ar_camera)

        arSceneView = findViewById<ArSceneView>(R.id.arSceneView).apply {
            // Lifecycle otomatik bağlanır, ancak val hatası almamak için
            // sadece nesneyi başlatıyoruz.
        }

        database = FirebaseDatabase.getInstance().reference.child("ilans")
        fetchIlansFromFirebase()
    }

    private fun fetchIlansFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Yeni veriler geldiğinde eski AR düğümlerini temizle
                arSceneView.children.filterIsInstance<ViewNode>().forEach { it.parent = null }

                for (postSnapshot in snapshot.children) {
                    val ilan = postSnapshot.getValue(Ilan::class.java)
                    ilan?.let {
                        renderIlanInAR(it)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ArIlanActivity, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun renderIlanInAR(ilan: Ilan) {
        // ViewNode oluştururken val hatası almamak için engine'i constructor'da geçiyoruz
        val node = ViewNode(arSceneView.engine).apply {
            // Rastgele bir konum (Kameranın 2-5 metre önü)
            position = Position(
                x = (Math.random().toFloat() * 4f) - 2f,
                y = (Math.random().toFloat() * 1f),
                z = -3f - (Math.random().toFloat() * 2f)
            )

            // Görünümü (XML) yükle
            loadView(context = this@ArIlanActivity, layoutResId = R.layout.ar_item_card) { _, view ->
                // Görünüm içindeki metinleri doldur
                view.findViewById<TextView>(R.id.ar_ilan_title).text = ilan.baslik
                view.findViewById<TextView>(R.id.ar_ilan_price).text = "${ilan.fiyat} TL"
            }

            // Tıklama dinleyicisi
            onTap = { _, _ ->
                Toast.makeText(this@ArIlanActivity, "${ilan.baslik} detayları açılıyor...", Toast.LENGTH_SHORT).show()
                true
            }
        }

        arSceneView.addChild(node)
    }
}