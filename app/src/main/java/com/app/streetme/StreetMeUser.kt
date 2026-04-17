package com.streetme.app

data class StreetMeUser(
    val id: String = "",
    val adSoyad: String = "",
    val email: String = "",
    val telefon: String = "",
    val profilFotoUrl: String = "",
    val ilanSayisi: Int = 0,
    val uyelikTarihi: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String = if (adSoyad.isNotEmpty()) adSoyad else "Kullanıcı ${id.takeLast(4)}"
}