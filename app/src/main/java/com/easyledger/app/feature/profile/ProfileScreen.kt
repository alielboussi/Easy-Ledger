package com.easyledger.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.easyledger.app.core.data.ProfileRepository
import com.easyledger.app.core.data.ProfileUpdate
import kotlinx.coroutines.launch
import com.easyledger.app.core.data.CountryData
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val repo = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var username by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var countryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val red = Color(0xFFB00020)
    val glowColor = Color(0x66B00020)
    val shape = RoundedCornerShape(12.dp)

    LaunchedEffect(Unit) {
        runCatching { repo.getCurrent() }.onSuccess { p ->
            if (p != null) {
                username = p.username
                country = p.country ?: ""
                countryCode = p.country_code ?: ""
                phone = p.phone ?: ""
            }
            loading = false
        }.onFailure {
            error = it.message
            loading = false
        }
    }

    // DOB removed

    val countries = remember { CountryData.countries }

    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        if (error != null) {
                            Text(error!!, color = Color.Red)
                            Spacer(Modifier.height(12.dp))
                        }

                        GlowyField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Username",
                            red = red,
                            glow = glowColor
                        )
                        Spacer(Modifier.height(12.dp))

                        // DOB removed from profile UI

                        // Country dropdown that sets both country and calling code
                        Card(shape = shape, colors = CardDefaults.cardColors(containerColor = glowColor), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                            Box(Modifier.fillMaxWidth().background(Color.White)) {
                                OutlinedTextField(
                                    value = if (country.isBlank()) "" else "$country ($countryCode)",
                                    onValueChange = {},
                                    label = { Text("Country") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable { countryExpanded = true },
                                    readOnly = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = red,
                                        unfocusedBorderColor = red,
                                        focusedLabelColor = red,
                                        cursorColor = red
                                    )
                                )
                            }
                            DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                                countries.forEach { item ->
                                    DropdownMenuItem(text = { Text("${item.name} (${item.code})") }, onClick = {
                                        country = item.name
                                        countryCode = item.code
                                        countryExpanded = false
                                    })
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            GlowyField(
                                value = countryCode,
                                onValueChange = { countryCode = it },
                                label = "Code",
                                red = red,
                                glow = glowColor,
                                keyboardType = KeyboardType.Text,
                                modifier = Modifier.weight(0.35f)
                            )
                            GlowyField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = "Phone",
                                red = red,
                                glow = glowColor,
                                keyboardType = KeyboardType.Phone,
                                modifier = Modifier.weight(0.65f)
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    error = null
                                    val update = ProfileUpdate(
                                        username = username.trim().ifBlank { null },
                                        country = country.trim().ifBlank { null },
                                        country_code = countryCode.trim().ifBlank { null },
                                        phone = phone.trim().ifBlank { null }
                                    )
                                    runCatching { repo.update(update) }.onFailure { error = it.message }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) { Text("Save Changes") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlowyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    red: Color,
    glow: Color,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = glow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp)
                .let { if (onClick != null) it.clickable { onClick() } else it },
            singleLine = true,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = red,
                unfocusedBorderColor = red,
                focusedLabelColor = red,
                cursorColor = red
            )
        )
    }
}
