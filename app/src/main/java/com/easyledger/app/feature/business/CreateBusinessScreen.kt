package com.easyledger.app.feature.business

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.easyledger.app.core.data.SupabaseBusinessRepository
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import com.easyledger.app.core.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import java.util.UUID

@Composable
fun CreateBusinessScreen(navController: NavController, repo: SupabaseBusinessRepository = SupabaseBusinessRepository()) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var primaryCurrency by remember { mutableStateOf(TextFieldValue("USD")) }
    var secondaryCurrency by remember { mutableStateOf(TextFieldValue("")) }
    var currencySymbol by remember { mutableStateOf(TextFieldValue("$")) }
    var currencyFormat by remember { mutableStateOf(TextFieldValue("#,##0.00")) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var selectedLogo by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedLogo = uri }
    )

    val glowColor = Color(0x66B00020)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Create Main Business")
        Spacer(Modifier.height(12.dp))
        GlowyField(value = name, onValueChange = { name = it }, label = "Business name", glow = glowColor)
        Spacer(Modifier.height(8.dp))
        GlowyField(value = primaryCurrency, onValueChange = { primaryCurrency = it }, label = "Primary currency (e.g., USD)", glow = glowColor)
        Spacer(Modifier.height(8.dp))
        GlowyField(value = secondaryCurrency, onValueChange = { secondaryCurrency = it }, label = "Secondary currency (optional)", glow = glowColor)
        Spacer(Modifier.height(8.dp))
    GlowyField(value = currencySymbol, onValueChange = { currencySymbol = it }, label = "Currency symbol (e.g., \$)", glow = glowColor)
        Spacer(Modifier.height(8.dp))
        GlowyField(value = currencyFormat, onValueChange = { currencyFormat = it }, label = "Currency format (e.g., #,##0.00)", glow = glowColor)
        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Text("Error: ${'$'}error")
            Spacer(Modifier.height(8.dp))
        }

        Button(onClick = { logoPicker.launch("image/*") }) {
            Text(if (selectedLogo != null) "Change Logo" else "Pick Logo (optional)")
        }
        Spacer(Modifier.height(8.dp))
        if (selectedLogo != null) {
            Text("Logo selected")
            Spacer(Modifier.height(8.dp))
        }

        Button(onClick = {
            error = null
            if (name.text.isBlank() || primaryCurrency.text.length !in 3..4) {
                error = "Enter name and valid 3-4 letter currency"
                return@Button
            }
            saving = true
            scope.launch {
                val businessId = UUID.randomUUID().toString()
                val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
                var logoPath: String? = null

                if (selectedLogo != null && userId != null) {
                    val uri = selectedLogo!!
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val bytes = runCatching { context.readAllBytes(uri) }.getOrElse {
                        error = "Failed to read selected image"
                        saving = false
                        return@launch
                    }
                    val upload = repo.uploadBusinessLogo(userId, businessId, bytes, mime)
                    upload.onSuccess { path -> logoPath = path }
                        .onFailure { e ->
                            error = e.message ?: "Failed to upload logo"
                            saving = false
                            return@launch
                        }
                }

                val result = repo.createBusiness(
                    com.easyledger.app.core.data.CreateBusinessParams(
                        name = name.text.trim(),
                        logoUrl = logoPath,
                        primaryCurrency = primaryCurrency.text.trim().uppercase(),
                        secondaryCurrency = secondaryCurrency.text.trim().uppercase().ifBlank { null },
                        currencySymbol = currencySymbol.text,
                        currencyFormat = currencyFormat.text
                    ),
                    explicitId = businessId
                )
                saving = false
                result.onSuccess { navController.popBackStack() }
                    .onFailure { error = it.message }
            }
        }, enabled = !saving) {
            Text(if (saving) "Saving..." else "Save Business")
        }
    }
}

private fun Context.readAllBytes(uri: Uri): ByteArray {
    return contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Unable to open input stream for URI")
}

@Composable
private fun GlowyField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    glow: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = glow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFB00020),
                unfocusedBorderColor = Color(0xFFB00020),
                focusedLabelColor = Color(0xFFB00020),
                cursorColor = Color(0xFFB00020)
            )
        )
    }
}
