package com.streetme.app.models

data class Product(
    val id: String = "",
    val name: String = "",
    val price: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0, // AR için yükseklik
    val category: String = ""
)