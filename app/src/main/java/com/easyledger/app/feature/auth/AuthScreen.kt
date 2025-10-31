package com.easyledger.app.feature.auth
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.easyledger.app.core.auth.AuthState
import com.easyledger.app.core.auth.SessionManager
import com.easyledger.app.core.data.CountryData
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.SignInButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding as paddingAlias

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthenticated: () -> Unit, viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        if (state is AuthState.SignedIn) onAuthenticated()
    }

    // Show auth errors as snackbars
    LaunchedEffect(Unit) {
        SessionManager.authErrors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            when (state) {
                AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...") }
                is AuthState.SignedIn -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Signed in. Redirecting...") }
                AuthState.SignedOut -> AuthForm(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthForm(viewModel: AuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Easy Ledger", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
        Spacer(Modifier.height(32.dp))
        OfficialGoogleSignInButton { viewModel.signInWithGoogle() }
        Spacer(Modifier.height(12.dp))
        Text("Continue with Google", color = Color.Gray)
    }

    // No DOB picker
}

@Composable
private fun OfficialGoogleSignInButton(onClick: () -> Unit) {
    AndroidView(
        factory = { context ->
            SignInButton(context).apply {
                setSize(SignInButton.SIZE_WIDE)
                setColorScheme(SignInButton.COLOR_LIGHT)
                setOnClickListener { onClick() }
                // Let layout params stretch to parent width
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { view ->
            view.setOnClickListener { onClick() }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private enum class AuthMode { SignIn, SignUp }

@Composable
private fun FieldHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = Color.Black,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun GlowyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    red: Color,
    glow: Color,
    readOnly: Boolean = false,
    trailingIcon: (@Composable (() -> Unit))? = null,
    textColor: Color? = null
) {
    // Simulate glow using an outer card with translucent red, then an OutlinedTextField
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = glow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            singleLine = true,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = red,
                unfocusedBorderColor = red,
                focusedLabelColor = red,
                cursorColor = red
            ),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                color = textColor ?: Color.Black
            )
        )
    }
}

@Composable
private fun CountryDropdown(
    label: String,
    selected: String,
    onSelected: (String, String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    red: Color,
    glow: Color
) {
    val countries = remember { com.easyledger.app.core.data.CountryData.countries }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = glow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxWidth().background(Color.White)) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = red,
                    unfocusedBorderColor = red,
                    focusedLabelColor = red,
                    cursorColor = red
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .background(Color.Transparent)
                    .padding(0.dp)
                    .let { it },
                contentAlignment = Alignment.CenterStart
            ) {}
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            countries.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c.name} (${c.code})") },
                    onClick = {
                        onSelected(c.name, c.code)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = { onExpandedChange(true) }) { Text(if (selected.isBlank()) "Choose country" else "Change country") }
}

@Composable
private fun PhoneWithCode(
    selectedCode: String,
    onCodeSelected: (String) -> Unit,
    codeExpanded: Boolean,
    onCodeExpandedChange: (Boolean) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    red: Color,
    glow: Color
) {
    val codes = remember { CountryData.countries.map { it.code }.distinct() }

    RowWithSpacing {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = glow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Box(Modifier.fillMaxWidth().background(Color.White)) {
                OutlinedTextField(
                    value = selectedCode,
                    onValueChange = {},
                    label = { Text("Code") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = red,
                        unfocusedBorderColor = red,
                        focusedLabelColor = red,
                        cursorColor = red
                    )
                )
            }
            DropdownMenu(expanded = codeExpanded, onDismissRequest = { onCodeExpandedChange(false) }) {
                codes.forEach { code ->
                    DropdownMenuItem(text = { Text(code) }, onClick = {
                        onCodeSelected(code)
                        onCodeExpandedChange(false)
                    })
                }
            }
        }
        TextButton(onClick = { onCodeExpandedChange(true) }) { Text("Change") }

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = glow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),) {
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = red,
                    unfocusedBorderColor = red,
                    focusedLabelColor = red,
                    cursorColor = red
                )
            )
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
    val countries = remember { CountryData.countries }
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
                    androidx.compose.material3.Icon(
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
    // Extract digits after '+' until a non-digit or end (so '+1-684' => 1)
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
private fun RowWithSpacing(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}
