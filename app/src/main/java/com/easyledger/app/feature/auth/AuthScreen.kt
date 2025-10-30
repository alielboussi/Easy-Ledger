package com.easyledger.app.feature.auth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.easyledger.app.core.auth.AuthState
import com.easyledger.app.core.auth.SessionManager
import com.easyledger.app.core.data.CountryData
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.SignInButton

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
    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // DOB removed from sign-up flow
    var country by remember { mutableStateOf("") }
    var countryExpanded by remember { mutableStateOf(false) }
    var selectedCountryCode by remember { mutableStateOf("+1") }
    var codeExpanded by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }

    val red = Color(0xFFB00020)
    val glowColor = Color(0x66B00020) // thicker translucent red glow
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
    Text("Easy Ledger", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                if (mode == AuthMode.SignUp) {
                    GlowyField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        red = red,
                        glow = glowColor,
                        textColor = Color.Black
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    // Country dropdown
                    CountryDropdown(
                        label = "Country",
                        selected = country,
                        onSelected = { name, code ->
                            country = name
                            selectedCountryCode = code
                        },
                        expanded = countryExpanded,
                        onExpandedChange = { countryExpanded = it },
                        red = red,
                        glow = glowColor
                    )
                    Spacer(Modifier.height(12.dp))
                    // Phone with country code dropdown
                    PhoneWithCode(
                        selectedCode = selectedCountryCode,
                        onCodeSelected = { selectedCountryCode = it },
                        codeExpanded = codeExpanded,
                        onCodeExpandedChange = { codeExpanded = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        red = red,
                        glow = glowColor
                    )
                    Spacer(Modifier.height(12.dp))
                }

                GlowyField(
                    value = email,
                    onValueChange = { email = it },
                    label = if (mode == AuthMode.SignIn) "Email or Username" else "Email",
                    keyboardType = KeyboardType.Email,
                    red = red,
                    glow = glowColor,
                    textColor = if (mode == AuthMode.SignIn) Color.Black else null
                )
                Spacer(Modifier.height(12.dp))

                GlowyField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    red = red,
                    glow = glowColor,
                    textColor = Color.Black
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (mode == AuthMode.SignIn) {
                            val input = email.trim()
                            if ("@" in input) {
                                viewModel.signInWithEmail(input, password)
                            } else {
                                viewModel.signInWithUsername(input, password)
                            }
                        } else {
                            viewModel.signUpWithEmail(
                                username.trim(),
                                email.trim(),
                                password,
                                country.trim().ifBlank { null },
                                selectedCountryCode.trim().ifBlank { null },
                                phone.trim().ifBlank { null }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = red, contentColor = Color.White)
                ) {
                    Text(if (mode == AuthMode.SignIn) "Sign In" else "Create Account")
                }

                TextButton(onClick = { mode = if (mode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (mode == AuthMode.SignIn) "No account? Sign up" else "Have an account? Sign in")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    androidx.compose.material3.HorizontalDivider(color = Color(0xFFE0E0E0))
        Spacer(Modifier.height(16.dp))

        if (mode == AuthMode.SignIn) {
            OfficialGoogleSignInButton { viewModel.signInWithGoogle() }
        }

        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(16.dp))
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
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                color = textColor ?: androidx.compose.material3.LocalTextStyle.current.color
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = red,
                unfocusedBorderColor = red,
                focusedLabelColor = red,
                cursorColor = red
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
private fun RowWithSpacing(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}
