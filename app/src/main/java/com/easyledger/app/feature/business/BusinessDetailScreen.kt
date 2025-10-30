package com.easyledger.app.feature.business

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.easyledger.app.core.data.SupabaseBusinessRepository
import com.easyledger.app.core.data.UpdateBusinessParams
import com.easyledger.app.core.supabase.SupabaseProvider

@Composable
fun BusinessDetailScreen(navController: NavController, businessId: String, repo: SupabaseBusinessRepository = SupabaseBusinessRepository()) {
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf(TextFieldValue("")) }
    var primaryCurrency by remember { mutableStateOf(TextFieldValue("")) }
    var secondaryCurrency by remember { mutableStateOf(TextFieldValue("")) }
    var currencySymbol by remember { mutableStateOf(TextFieldValue("")) }
    var currencyFormat by remember { mutableStateOf(TextFieldValue("")) }
    var logoPath by remember { mutableStateOf<String?>(null) }
    var signedLogoUrl by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedLogoUri.value = uri
        }
    }
    val selectedLogoUri = remember { mutableStateOf<Uri?>(null) }

    // Sub-business and Categories state
    var subBusinesses by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var selectedScopeSubId by remember { mutableStateOf<String?>(null) } // null = business-wide
    var showAddSubDialog by remember { mutableStateOf(false) }
    var showEditSubDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // id,name
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // id,name

    LaunchedEffect(businessId) {
        loading = true
        error = null
        repo.getBusiness(businessId).onSuccess { row ->
            name = TextFieldValue((row["name"] as? String) ?: "")
            primaryCurrency = TextFieldValue((row["currency_primary"] as? String) ?: "")
            secondaryCurrency = TextFieldValue(((row["currency_secondary"] as? String) ?: ""))
            currencySymbol = TextFieldValue((row["currency_symbol"] as? String ?: ""))
            currencyFormat = TextFieldValue((row["currency_format"] as? String ?: ""))
            logoPath = row["logo_url"] as? String
            // Resolve signed URL if we have a path
            if (logoPath != null) {
                repo.createLogoSignedUrl(logoPath!!).onSuccess { signedLogoUrl = it }.onFailure { signedLogoUrl = null }
            } else {
                signedLogoUrl = null
            }
            loading = false
        }.onFailure { e ->
            error = e.message ?: "Failed to load business"
            loading = false
        }
        // Load sub-businesses
        repo.listSubBusinesses(businessId).onSuccess { subs -> subBusinesses = subs }.onFailure { /* ignore */ }
        // Load categories for business scope initially
        repo.listCategoriesForBusiness(businessId).onSuccess { cats -> categories = cats }.onFailure { /* ignore */ }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SnackbarHost(snackbarHostState)
        Text("Business Details", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        // Show preview of selected logo if any; else show current signed URL
        val previewModel: Any? = selectedLogoUri.value ?: signedLogoUrl
        if (previewModel != null) {
            AsyncImage(model = previewModel, contentDescription = "Logo", modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = { logoPicker.launch("image/*") }) {
            Text(if (selectedLogoUri.value != null) "Change Logo (selected)" else "Change/Add Logo")
        }
        Spacer(Modifier.height(16.dp))

        val nameError = remember(name) { name.text.isBlank() }
        val primaryCurrencyError = remember(primaryCurrency) { primaryCurrency.text.length !in 3..4 }
        val secondaryCurrencyError = remember(secondaryCurrency, primaryCurrency) {
            val s = secondaryCurrency.text
            s.isNotBlank() && (s.length !in 3..4 || s.equals(primaryCurrency.text, ignoreCase = true))
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Business name") },
            isError = nameError,
            supportingText = { if (nameError) Text("Name is required") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = primaryCurrency,
            onValueChange = { primaryCurrency = it },
            label = { Text("Primary currency (3-4 letters)") },
            isError = primaryCurrencyError,
            supportingText = { if (primaryCurrencyError) Text("3-4 letters, e.g., USD") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = secondaryCurrency,
            onValueChange = { secondaryCurrency = it },
            label = { Text("Secondary currency (optional)") },
            isError = secondaryCurrencyError,
            supportingText = { if (secondaryCurrencyError) Text("Must be 3-4 letters and different from primary") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = currencySymbol, onValueChange = { currencySymbol = it }, label = { Text("Currency symbol") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = currencyFormat, onValueChange = { currencyFormat = it }, label = { Text("Currency format") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Text("Error: ${'$'}error", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                error = null
                saving = true
                scope.launch {
                    var newLogoPath: String? = logoPath
                    val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
                    // If a new logo selected, upload it
                    val sel = selectedLogoUri.value
                    if (sel != null && userId != null) {
                        val mime = context.contentResolver.getType(sel) ?: "image/jpeg"
                        val bytes = runCatching { context.readAllBytes(sel) }.getOrElse {
                            error = "Failed to read selected image"
                            snackbarHostState.showSnackbar(error!!)
                            saving = false
                            return@launch
                        }
                        if (logoPath != null) {
                            // Upload to same path (upsert)
                            val up = repo.uploadLogoToPath(logoPath!!, bytes, mime)
                            up.onFailure { e ->
                                error = e.message ?: "Failed to upload logo"
                                snackbarHostState.showSnackbar(error!!)
                                saving = false
                                return@launch
                            }
                        } else {
                            // Create a new path based on mime
                            val res = repo.uploadBusinessLogo(userId, businessId, bytes, mime)
                            res.onSuccess { p -> newLogoPath = p }.onFailure { e ->
                                error = e.message ?: "Failed to upload logo"
                                snackbarHostState.showSnackbar(error!!)
                                saving = false
                                return@launch
                            }
                        }
                    }
                    if (nameError || primaryCurrencyError || secondaryCurrencyError) {
                        saving = false
                        snackbarHostState.showSnackbar("Fix validation errors")
                        return@launch
                    }
                    val params = UpdateBusinessParams(
                        name = name.text.trim(),
                        logoUrl = newLogoPath,
                        primaryCurrency = primaryCurrency.text.trim().uppercase().ifBlank { null },
                        secondaryCurrency = secondaryCurrency.text.trim().uppercase().ifBlank { null },
                        currencySymbol = currencySymbol.text,
                        currencyFormat = currencyFormat.text
                    )
                    val result = repo.updateBusiness(businessId, params)
                    saving = false
                    result.onSuccess {
                        snackbarHostState.showSnackbar("Saved")
                        // Refresh logo signed URL if changed
                        logoPath = newLogoPath
                        selectedLogoUri.value = null
                        if (logoPath != null) {
                            repo.createLogoSignedUrl(logoPath!!).onSuccess { signedLogoUrl = it }.onFailure { signedLogoUrl = null }
                        } else {
                            signedLogoUrl = null
                        }
                    }.onFailure { e ->
                        error = e.message
                        snackbarHostState.showSnackbar(error ?: "Update failed")
                    }
                }
            }, enabled = !saving) { Text(if (saving) "Saving..." else "Save Changes") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { showDeleteConfirm = true }) { Text("Delete Business", color = MaterialTheme.colorScheme.error) }
        }

        Spacer(Modifier.height(24.dp))
        Text("Sub-businesses", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        for (sub in subBusinesses) {
            val subId = sub["id"]?.toString() ?: continue
            val subName = sub["name"]?.toString() ?: ""
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(subName, modifier = Modifier.weight(1f))
                TextButton(onClick = { showEditSubDialog = subId to subName }) { Text("Edit") }
                TextButton(onClick = {
                    scope.launch {
                        repo.deleteSubBusiness(subId).onSuccess {
                            subBusinesses = subBusinesses.filter { it["id"].toString() != subId }
                            snackbarHostState.showSnackbar("Sub-business deleted")
                            // If current category scope was this sub, reset scope and reload categories
                            if (selectedScopeSubId == subId) {
                                selectedScopeSubId = null
                                repo.listCategoriesForBusiness(businessId).onSuccess { categories = it }
                            }
                        }.onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "Delete failed") }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
        TextButton(onClick = { showAddSubDialog = true }) { Text("Add Sub-business") }

        Spacer(Modifier.height(24.dp))
        Text("Categories", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        // Scope selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scope:")
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                selectedScopeSubId = null
                scope.launch { repo.listCategoriesForBusiness(businessId).onSuccess { categories = it } }
            }) { Text(if (selectedScopeSubId == null) "Business-wide ✓" else "Business-wide") }
            for (sub in subBusinesses) {
                val subId = sub["id"].toString()
                val n = sub["name"].toString()
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    selectedScopeSubId = subId
                    scope.launch { repo.listCategoriesForSubBusiness(subId).onSuccess { categories = it } }
                }) { Text(if (selectedScopeSubId == subId) "$n ✓" else n) }
            }
        }
        Spacer(Modifier.height(8.dp))
        for (cat in categories) {
            val catId = cat["id"]?.toString() ?: continue
            val catName = cat["name"]?.toString() ?: ""
            val catType = cat["type"]?.toString() ?: ""
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("$catName ($catType)", modifier = Modifier.weight(1f))
                TextButton(onClick = { showEditCategoryDialog = catId to catName }) { Text("Rename") }
                TextButton(onClick = {
                    scope.launch {
                        repo.deleteCategory(catId).onSuccess {
                            categories = categories.filter { it["id"].toString() != catId }
                            snackbarHostState.showSnackbar("Category deleted")
                        }.onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "Delete failed") }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
        TextButton(onClick = { showAddCategoryDialog = true }) { Text("Add Category") }

        // Delete confirm dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Business?") },
                text = { Text("This will delete the business and its related data. This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            repo.deleteBusiness(businessId).onSuccess {
                                snackbarHostState.showSnackbar("Business deleted")
                                navController.popBackStack()
                            }.onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "Delete failed") }
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
            )
        }

        // Add Sub-business dialog
        if (showAddSubDialog) {
            var subName by remember { mutableStateOf("") }
            val invalid = subName.isBlank()
            AlertDialog(
                onDismissRequest = { showAddSubDialog = false },
                title = { Text("Add Sub-business") },
                text = {
                    Column { OutlinedTextField(value = subName, onValueChange = { subName = it }, label = { Text("Name") }, isError = invalid) }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (invalid) return@TextButton
                        showAddSubDialog = false
                        scope.launch {
                            repo.createSubBusiness(businessId, subName).onSuccess {
                                snackbarHostState.showSnackbar("Sub-business added")
                                repo.listSubBusinesses(businessId).onSuccess { subBusinesses = it }
                            }.onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "Add failed") }
                        }
                    }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = { showAddSubDialog = false }) { Text("Cancel") } }
            )
        }

        // Edit Sub-business dialog
        val editSub = showEditSubDialog
        if (editSub != null) {
            var subName by remember { mutableStateOf(editSub.second) }
            val invalid = subName.isBlank()
            AlertDialog(
                onDismissRequest = { showEditSubDialog = null },
                title = { Text("Rename Sub-business") },
                text = { Column { OutlinedTextField(value = subName, onValueChange = { subName = it }, label = { Text("Name") }, isError = invalid) } },
                confirmButton = {
                    TextButton(onClick = {
                        if (invalid) return@TextButton
                        showEditSubDialog = null
                        scope.launch {
                            repo.updateSubBusiness(editSub.first, subName).onSuccess {
                                snackbarHostState.showSnackbar("Renamed")
                                repo.listSubBusinesses(businessId).onSuccess { subBusinesses = it }
                            }.onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "Update failed") }
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showEditSubDialog = null }) { Text("Cancel") } }
            )
        }

        // Add Category dialog
        if (showAddCategoryDialog) {
            var catName by remember { mutableStateOf("") }
            var catType by remember { mutableStateOf("income") }
            val invalid = catName.isBlank()
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add Category") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Name") }, isError = invalid)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { catType = "income" }) { Text(if (catType == "income") "Income ✓" else "Income") }
                            TextButton(onClick = { catType = "expense" }) { Text(if (catType == "expense") "Expense ✓" else "Expense") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (invalid) return@TextButton
                        showAddCategoryDialog = false
                        scope.launch {
                            if (selectedScopeSubId == null) {
                                repo.createCategory(catName, catType, businessId = businessId, subBusinessId = null)
                                    .onSuccess {
                                        snackbarHostState.showSnackbar("Category added")
                                        repo.listCategoriesForBusiness(businessId).onSuccess { categories = it }
                                    }.onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "Add failed") }
                            } else {
                                repo.createCategory(catName, catType, businessId = null, subBusinessId = selectedScopeSubId)
                                    .onSuccess {
                                        snackbarHostState.showSnackbar("Category added")
                                        repo.listCategoriesForSubBusiness(selectedScopeSubId!!).onSuccess { categories = it }
                                    }.onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "Add failed") }
                            }
                        }
                    }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") } }
            )
        }

        // Edit Category dialog
        val editCat = showEditCategoryDialog
        if (editCat != null) {
            var catName by remember { mutableStateOf(editCat.second) }
            val invalid = catName.isBlank()
            AlertDialog(
                onDismissRequest = { showEditCategoryDialog = null },
                title = { Text("Rename Category") },
                text = { Column { OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Name") }, isError = invalid) } },
                confirmButton = {
                    TextButton(onClick = {
                        if (invalid) return@TextButton
                        showEditCategoryDialog = null
                        scope.launch {
                            repo.updateCategoryName(editCat.first, catName).onSuccess {
                                snackbarHostState.showSnackbar("Renamed")
                                if (selectedScopeSubId == null) {
                                    repo.listCategoriesForBusiness(businessId).onSuccess { categories = it }
                                } else {
                                    repo.listCategoriesForSubBusiness(selectedScopeSubId!!).onSuccess { categories = it }
                                }
                            }.onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "Update failed") }
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showEditCategoryDialog = null }) { Text("Cancel") } }
            )
        }
    }
}

private fun Context.readAllBytes(uri: Uri): ByteArray {
    return contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Unable to open input stream for URI")
}
