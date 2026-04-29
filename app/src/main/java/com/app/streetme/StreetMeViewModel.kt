package com.streetme.app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.streetme.app.models.AdModel

class StreetMeViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _ads = mutableStateOf<List<AdModel>>(emptyList())
    val ads: State<List<AdModel>> = _ads

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    init {
        fetchAds()
    }

    fun fetchAds() {
        _loading.value = true
        db.collection("ads")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _loading.value = false
                    return@addSnapshotListener
                }

                val adList = mutableListOf<AdModel>()
                value?.forEach { doc ->
                    val ad = doc.toObject(AdModel::class.java)
                    adList.add(ad)
                }
                _ads.value = adList
                _loading.value = false
            }
    }

    fun logout() {
        auth.signOut()
    }
}