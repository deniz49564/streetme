package com.streetme.app

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
    fun getFiyatText(): String = "₺${"%.2f".format(fiyat)}"
}