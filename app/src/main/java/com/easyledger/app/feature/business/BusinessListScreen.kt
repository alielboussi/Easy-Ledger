package com.easyledger.app.feature.business

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.easyledger.app.core.data.SupabaseBusinessRepository

private data class BusinessListItem(
    val id: String,
    val name: String,
    val logoPath: String?
)

@Composable
fun BusinessListScreen(navController: NavController, repo: SupabaseBusinessRepository = SupabaseBusinessRepository()) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<BusinessListItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        error = null
        val res = repo.listBusinesses()
        res.onSuccess { rows ->
            items = rows.mapNotNull { row ->
                val id = row["id"] as? String ?: (row["id"]?.toString())
                val name = row["name"] as? String
                val logo = row["logo_url"] as? String
                if (id != null && name != null) BusinessListItem(id = id, name = name, logoPath = logo) else null
            }
            loading = false
        }.onFailure { e ->
            error = e.message ?: "Failed to load businesses"
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Businesses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (error != null) {
            Text("Error: ${'$'}error", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                loading = true
                error = null
                scope.launch {
                    val res2 = repo.listBusinesses()
                    res2.onSuccess { rows ->
                        items = rows.mapNotNull { row ->
                            val id = row["id"] as? String ?: (row["id"]?.toString())
                            val name = row["name"] as? String
                            val logo = row["logo_url"] as? String
                            if (id != null && name != null) BusinessListItem(id = id, name = name, logoPath = logo) else null
                        }
                        loading = false
                    }.onFailure { e ->
                        error = e.message ?: "Failed to load businesses"
                        loading = false
                    }
                }
            }) { Text("Retry") }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items) { item ->
                BusinessRow(item = item, repo = repo) {
                    navController.navigate("business/detail/${item.id}")
                }
            }
        }
    }
}

@Composable
private fun BusinessRow(item: BusinessListItem, repo: SupabaseBusinessRepository, onClick: (() -> Unit)? = null) {
    var signedUrl by remember(item.logoPath) { mutableStateOf<String?>(null) }
    var loading by remember(item.logoPath) { mutableStateOf(false) }

    LaunchedEffect(item.logoPath) {
        if (item.logoPath != null) {
            loading = true
            repo.createLogoSignedUrl(item.logoPath).onSuccess { url ->
                signedUrl = url
                loading = false
            }.onFailure {
                signedUrl = null
                loading = false
            }
        } else {
            signedUrl = null
        }
    }

    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = onClick != null) { onClick?.invoke() }
        .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (signedUrl != null) {
            AsyncImage(
                model = signedUrl,
                contentDescription = "Business Logo",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Text(item.name.take(1).uppercase())
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
