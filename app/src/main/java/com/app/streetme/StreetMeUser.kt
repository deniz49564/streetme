package com.streetme.app

import java.text.SimpleDateFormat
import java.util.*

data class StreetMeUser(
    val id: String = "",
    val adSoyad: String = "",
    val email: String = "",
    val telefon: String = "",
    val profilFotoUrl: String = "",
    val ilanSayisi: Int = 0,
    val uyelikTarihi: Long = System.currentTimeMillis()
) {
    // Arayüzde görünecek isim mantığını optimize ediyoruz
    fun getDisplayName(): String {
        return when {
            adSoyad.isNotBlank() -> adSoyad
            email.isNotEmpty() -> email.substringBefore("@")
            else -> "Kullanıcı #${id.takeLast(4)}"
        }
    }

    // Telefon numarasını maskeleyerek göstermek için (Örn: 05xx xxx xx 12)
    // İlan detay sayfasında güvenliği artırır
    fun getMaskedPhone(): String {
        return if (telefon.length >= 10) {
            telefon.replaceRange(4, 9, "****")
        } else {
            "Telefon eklenmemiş"
        }
    }
    data class StreetMeUser(
        // ... eski alanlar ...
        val streetCred: Int = 0,         // Kullanıcı puanı (XP)
        val unlockedAvatars: List<String> = listOf("default"), // Açılan özel avatarlar
        val badge: String = "Yeni Komşu"  // Rozet
    )

    // Profil sayfasında üyelik süresini göstermek için
    fun getMemberSince(): String {
        return try {
            val date = Date(uyelikTarihi)
            val format = SimpleDateFormat("MMMM yyyy", Locale("tr", "TR"))
            format.format(date) + " tarihinden beri üye"
        } catch (e: Exception) {
            "Yeni Üye"
        }
    }
}