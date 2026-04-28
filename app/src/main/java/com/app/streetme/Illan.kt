package com.streetme.app

import java.text.SimpleDateFormat
import java.util.*

data class Ilan(
    val id: String = "",
    val userId: String = "",
    val baslik: String = "",
    val aciklama: String = "",
    val fiyat: Double = 0.0,
    val kategori: String = "",
    val resimUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val ilanTarihi: Long = System.currentTimeMillis(),
    val durum: String = "aktif" // aktif, satıldı, pasif
) {
    // Fiyatı formatlı getiren fonksiyon
    fun getFiyatText(): String {
        return try {
            // Türkiye lokasyonu kullanarak virgül/nokta ayrımını standartlaştırıyoruz
            String.format(Locale("tr", "TR"), "₺%.2f", fiyat)
        } catch (e: Exception) {
            "₺$fiyat"
        }
    }
    data class Ilan(
        // ... eski alanlar ...
        val isEmergency: Boolean = false, // Acil ilan sinyali için
        val viewCount: Int = 0,           // Popülerlik ısı haritası için
        val arModelId: String = ""        // Ürünün AR dünyasındaki 3D modeli
    )
    // İlanın yayınlanma tarihini okunabilir formatta getiren yardımcı fonksiyon
    fun getFormattedDate(): String {
        val date = Date(ilanTarihi)
        val format = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
        return format.format(date)
    }
}