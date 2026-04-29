package com.streetme.app.models // Paket isminin doğruluğunu kontrol et

data class AdModel(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val price: String? = null,
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val userId: String? = null
)