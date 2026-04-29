package com.streetme.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streetme.app.viewmodel.StreetMeViewModel
import com.streetme.app.models.AdModel // Paket adın farklıysa ona göre düzenle
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: StreetMeViewModel) {
    val ads = viewModel.ads.value
    val isLoading = viewModel.loading.value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("StreetMe İlanlar") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ads) { ad ->
                        AdCard(ad = ad)
                    }
                }
            }
        }
    }
}

@Composable
fun AdCard(ad: AdModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = ad.title ?: "Başlıksız", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = ad.description ?: "Açıklama yok", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${ad.price} TL",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}