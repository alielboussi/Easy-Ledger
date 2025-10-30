package com.easyledger.app.feature.business

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.easyledger.app.core.data.SupabaseBusinessRepository
import kotlinx.coroutines.launch

@Composable
fun CreateBusinessScreen(navController: NavController, repo: SupabaseBusinessRepository = SupabaseBusinessRepository()) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var primaryCurrency by remember { mutableStateOf(TextFieldValue("USD")) }
    var secondaryCurrency by remember { mutableStateOf(TextFieldValue("")) }
    var currencySymbol by remember { mutableStateOf(TextFieldValue("$")) }
    var currencyFormat by remember { mutableStateOf(TextFieldValue("#,##0.00")) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Create Main Business")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Business name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = primaryCurrency, onValueChange = { primaryCurrency = it }, label = { Text("Primary currency (e.g., USD)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = secondaryCurrency, onValueChange = { secondaryCurrency = it }, label = { Text("Secondary currency (optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = currencySymbol, onValueChange = { currencySymbol = it }, label = { Text("Currency symbol (e.g., $)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = currencyFormat, onValueChange = { currencyFormat = it }, label = { Text("Currency format (e.g., #,##0.00)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Text("Error: ${'$'}error")
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
                val result = repo.createBusiness(
                    com.easyledger.app.core.data.CreateBusinessParams(
                        name = name.text.trim(),
                        logoUrl = null, // TODO: upload to Storage and set URL
                        primaryCurrency = primaryCurrency.text.trim().uppercase(),
                        secondaryCurrency = secondaryCurrency.text.trim().uppercase().ifBlank { null },
                        currencySymbol = currencySymbol.text,
                        currencyFormat = currencyFormat.text
                    )
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
