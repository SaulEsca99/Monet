package com.mifinanza.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifinanza.app.R
import com.mifinanza.app.data.*
import com.mifinanza.app.ui.theme.*
import com.mifinanza.app.viewmodel.AppViewModel

private val TEAL = Color(0xFF00C6A0)
private val NAVY = Color(0xFF0F1926)

@Composable
fun WelcomeScreen(vm: AppViewModel) {
    var step by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf("") }

    AnimatedContent(
        targetState = step,
        transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) },
        label = "step"
    ) { s ->
        when (s) {
            0 -> SplashStep(onStart = { step = 1 })
            1 -> NameStep(userName = userName, onNameChange = { userName = it }, onNext = { step = 2 })
            2 -> AccountsStep(userName = userName, onFinish = { drafts ->
                // Atomic save — fixes race condition with multiple accounts
                val validDrafts = drafts.filter { it.name.isNotBlank() }
                vm.setupInitial(
                    userName = userName,
                    accounts = validDrafts.map { Triple(it.name, it.type, it.balance) },
                    colors = validDrafts.map { it.color }
                )
            })
        }
    }
}

// ── Step 0: Splash ────────────────────────────────────────────────────────────

@Composable
private fun SplashStep(onStart: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NAVY, Color(0xFF071220))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(1.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Logo
                Box(
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.monet_logo),
                        contentDescription = "Monet",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Title
                Text("Monet", fontSize = 56.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-2).sp)

                // Tagline
                Text(
                    "Tu dinero, bajo control",
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TEAL
                )

                // Features
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("💰 Saldo y cuentas en tiempo real", "📊 Gastos e ingresos detallados", "🔄 MSI y suscripciones", "🤝 Préstamos personales").forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(feature, fontSize = 14.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            // CTA
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TEAL, contentColor = Color(0xFF021A12)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text("Comenzar →", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Text(
                    "Sin registro · Sin internet · 100% privado",
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    fontSize = 12.sp, color = Color(0xFF4B5563)
                )
            }
        }
    }
}

// ── Step 1: Name ──────────────────────────────────────────────────────────────

@Composable
private fun NameStep(userName: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val firstName = userName.trim().split(" ").firstOrNull()?.takeIf { it.isNotEmpty() }

    Column(
        modifier = Modifier.fillMaxSize().background(NAVY)
    ) {
        Spacer(Modifier.height(56.dp))

        // Top dark section — all on dark background so white text is visible
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))) {
                Image(painterResource(R.drawable.monet_logo), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.height(18.dp))
            Text("¿Cómo te llamas?", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("Personalizamos tu experiencia", fontSize = 14.sp, color = Color(0xFF8B95A8))
        }

        Spacer(Modifier.height(8.dp))

        // Input card
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 4.dp) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = onNameChange,
                        label = { Text("Tu nombre completo") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (userName.isNotBlank()) onNext() }),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TEAL, unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = TEAL, unfocusedLabelColor = Color(0xFF94A3B8),
                            cursorColor = TEAL, focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    AnimatedVisibility(visible = firstName != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(TEAL.copy(.1f)).padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("¡Hola, $firstName! 👋  Listo para empezar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TEAL)
                        }
                    }
                }
            }

            Button(
                onClick = onNext, enabled = userName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TEAL, contentColor = Color.Black),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Siguiente →", fontWeight = FontWeight.Black, fontSize = 16.sp) }

            TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Saltar por ahora", color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Step 2: Accounts ──────────────────────────────────────────────────────────

private data class AccountDraft(
    val name: String = "", val type: String = "debit",
    val balance: Double = 0.0, val color: String = "#6366F1"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsStep(userName: String, onFinish: (List<AccountDraft>) -> Unit) {
    val firstName = userName.trim().split(" ").firstOrNull()?.takeIf { it.isNotEmpty() }
    val drafts = remember { mutableStateListOf(AccountDraft()) }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()) {
                    Button(
                        onClick = { onFinish(drafts.toList()) },
                        enabled = drafts.any { it.name.isNotBlank() },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TEAL, contentColor = Color.Black),
                        shape = RoundedCornerShape(20.dp)
                    ) { Text("Entrar a Monet →", fontWeight = FontWeight.Black, fontSize = 18.sp) }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(NAVY, Color(0xFF132030)))).padding(24.dp).padding(top = 32.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))) {
                            Image(painterResource(R.drawable.monet_logo), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Monet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(
                        if (firstName != null) "¡Perfecto, $firstName! 💰" else "Configuración 💰",
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp
                    )
                    Text("¿Cuánto dinero tienes ahora mismo?", color = Color(0xFF8B95A8), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                drafts.forEachIndexed { i, draft ->
                    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 2.dp) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Type + delete
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                listOf("debit" to "💳 Débito", "cash" to "💵 Efectivo").forEach { (t, label) ->
                                    Button(
                                        onClick = { drafts[i] = draft.copy(type = t) },
                                        modifier = Modifier.height(42.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (draft.type == t) NAVY else Color(0xFFF1F5F9),
                                            contentColor = if (draft.type == t) Color.White else Color(0xFF64748B)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                                }
                                Spacer(Modifier.weight(1f))
                                if (drafts.size > 1) {
                                    IconButton(onClick = { drafts.removeAt(i) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.RemoveCircleOutline, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            // Name field
                            OutlinedTextField(
                                value = draft.name,
                                onValueChange = { drafts[i] = draft.copy(name = it) },
                                label = { Text(if (draft.type == "debit") "Nombre del banco" else "Ej: Efectivo en cartera") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TEAL, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedLabelColor = TEAL, unfocusedLabelColor = Color(0xFF94A3B8),
                                    focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            // Balance field
                            OutlinedTextField(
                                value = if (draft.balance == 0.0) "" else draft.balance.toString(),
                                onValueChange = { drafts[i] = draft.copy(balance = it.toDoubleOrNull() ?: 0.0) },
                                label = { Text("Saldo actual") },
                                prefix = { Text("$ ", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TEAL, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedLabelColor = TEAL, unfocusedLabelColor = Color(0xFF94A3B8),
                                    focusedTextColor = Color(0xFF0F172A), unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            if (draft.balance > 0) {
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TEAL.copy(.1f)).padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text(formatMXN(draft.balance), fontSize = 24.sp, fontWeight = FontWeight.Black, color = TEAL)
                                }
                            }
                        }
                    }
                }

                // Add another account
                OutlinedButton(
                    onClick = { drafts.add(AccountDraft()) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF94A3B8))
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar otra cuenta", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
