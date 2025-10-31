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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
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
    var countryIso2 by remember { mutableStateOf("US") }
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

                        Text("Username", style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Color.Black, modifier = Modifier.padding(bottom = 6.dp))
                        GlowyField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Username",
                            red = red,
                            glow = glowColor
                        )
                        Spacer(Modifier.height(12.dp))

                        // Integrated phone number field with country code dropdown
                        Text("Phone", style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Color.Black, modifier = Modifier.padding(bottom = 6.dp))
                        PhoneNumberField(
                            countryName = country,
                            countryCode = countryCode.ifBlank { "+1" },
                            countryIso2 = countryIso2,
                            onCountrySelected = { name, code, iso2 ->
                                country = name
                                countryCode = code
                                countryIso2 = iso2
                            },
                            phone = phone,
                            onPhoneChange = { phone = it },
                            red = red,
                            glow = glowColor
                        )

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
private fun PhoneNumberField(
    countryName: String,
    countryCode: String,
    countryIso2: String,
    onCountrySelected: (String, String, String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    red: Color,
    glow: Color
) {
    val countries = remember { com.easyledger.app.core.data.CountryData.countries }
    val countriesSorted = remember { countries.sortedWith(compareBy({ baseCallingCodeInt(it.code) }, { it.name })) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = glow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone number") },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            prefix = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(flagEmoji(countryIso2), fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(countryCode, color = Color.Black)
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Change country code",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp).padding(start = 4.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = red,
                unfocusedBorderColor = red,
                focusedLabelColor = red,
                cursorColor = red
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            countriesSorted.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${flagEmoji(c.iso2)} ${c.name} (${c.code})") },
                    onClick = {
                        onCountrySelected(c.name, c.code, c.iso2)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun flagEmoji(iso2: String): String {
    if (iso2.length != 2) return "�️"
    val first = Character.codePointAt(iso2.uppercase(), 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(iso2.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

private fun baseCallingCodeInt(code: String): Int {
    val start = code.indexOf('+') + 1
    if (start <= 0 || start >= code.length) return Int.MAX_VALUE
    var i = start
    val sb = StringBuilder()
    while (i < code.length && code[i].isDigit()) {
        sb.append(code[i])
        i++
    }
    return sb.toString().toIntOrNull() ?: Int.MAX_VALUE
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
