package com.mifinanza.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifinanza.app.data.*
import com.mifinanza.app.ui.components.*
import com.mifinanza.app.ui.theme.*
import com.mifinanza.app.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SuscripcionesScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val ym = currentYearMonth()
    var showAdd by remember { mutableStateOf(false) }
    var editingSub by remember { mutableStateOf<Subscription?>(null) }
    var payingSubId by remember { mutableStateOf<String?>(null) }

    val activeSubs = state.subscriptions.filter { it.active }
    val totalMo = activeSubs.sumOf { it.amount }
    val totalPaidAllTime = state.subscriptions.sumOf { (it.payments ?: emptyList()).sumOf { p -> p.amount } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = PurpleBrand, contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(58.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(26.dp))
            }
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {

            // Circular/donut header
            val paidThisMonth = state.subscriptions.count { it.active && it.lastPaidMonth == ym }
            CircularSectionHeader(
                label = "SUSCRIPCIONES",
                mainValue = formatMXN(totalMo),
                progress = if (activeSubs.isEmpty()) 0f else paidThisMonth.toFloat() / activeSubs.size,
                accentColor = PurpleBrand,
                stat1Label = "Pago mensual",
                stat1Value = formatMXN(totalMo),
                stat2Label = "${paidThisMonth}/${activeSubs.size} pagadas este mes",
                stat2Value = if (totalPaidAllTime > 0) "Historial: ${formatMXN(totalPaidAllTime)}" else "Sin pagos aún"
            )

            if (state.subscriptions.isEmpty()) {
                EmptyState("📱", "Sin suscripciones", "Agrega Netflix, Spotify, Gym...")
            }

            // ── Subscription list with swipe to delete ───────────────────────
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.subscriptions.forEach { sub ->
                    val paid = sub.lastPaidMonth == ym
                    val days = daysUntilDay(sub.billingDay)
                    val urgent = days in 0..5 && sub.active && !paid
                    val payments = sub.payments ?: emptyList()
                    val totalSubPaid = payments.sumOf { it.amount }

                    // Swipe to delete — LaunchedEffect pattern (no stuck animation)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v -> v == SwipeToDismissBoxValue.EndToStart },
                        positionalThreshold = { it * .45f }
                    )
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            vm.deleteSubscription(sub.id)
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.error).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Text("Eliminar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                                // Header row
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(parseHex(sub.color).copy(.15f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CreditCard, null, tint = parseHex(sub.color), modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                        // Days countdown
                                        if (sub.active) {
                                            Text(
                                                when {
                                                    paid -> "✓ Pagado este mes · botón de pago disponible el mes que viene (día ${sub.billingDay})"
                                                    days == 0 -> "🔔 ¡Hoy se cobra! · Mensual cada día ${sub.billingDay}"
                                                    days < 0 -> "Se cobró hace ${-days} día${if (-days!=1)"s" else ""}"
                                                    else -> "🔄 Mensual · cobra en $days día${if(days!=1)"s" else ""} · día ${sub.billingDay}"
                                                },
                                                fontSize = 11.sp,
                                                color = when {
                                                    paid -> Color(0xFF10B981)
                                                    urgent -> Color(0xFFF59E0B)
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatMXN(sub.amount), fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("/mes", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // Badges row
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (paid) {
                                        Surface(shape = RoundedCornerShape(50), color = Color(0xFF10B981).copy(.12f)) {
                                            Text("✓ Pagado", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        }
                                    }
                                    if (urgent && !paid) {
                                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFF59E0B).copy(.15f)) {
                                            Text("⚡ $days días", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                        }
                                    }
                                    if (!sub.active) {
                                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                                            Text("Pausada", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (payments.isNotEmpty()) {
                                        Surface(shape = RoundedCornerShape(50), color = PurpleBrand.copy(.1f)) {
                                            Text("${payments.size} meses · ${formatMXN(totalSubPaid)}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PurpleBrand)
                                        }
                                    }
                                }

                                // Actions
                                if (sub.active) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!paid && state.accounts.isNotEmpty()) {
                                            Button(
                                                onClick = { payingSubId = sub.id },
                                                modifier = Modifier.weight(1f).height(42.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PurpleBrand),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Pagar ${formatMXN(sub.amount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { vm.toggleSubscriptionActive(sub.id) },
                                            modifier = Modifier.height(42.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            shape = RoundedCornerShape(12.dp)
                                        ) { Text(if (sub.active) "Pausar" else "Activar", fontSize = 12.sp) }

                                        IconButton(onClick = { editingSub = sub }, modifier = Modifier.size(42.dp)) {
                                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else {
                                    OutlinedButton(onClick = { vm.toggleSubscriptionActive(sub.id) }, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(12.dp)) {
                                        Text("Reactivar", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showAdd) SubFormSheet(onSave = { name, amount, day, color ->
        vm.addSubscription(name, amount, day, color)
        showAdd = false
    }, onDismiss = { showAdd = false })

    editingSub?.let { sub ->
        SubFormSheet(
            initial = sub,
            onSave = { name, amount, day, color ->
                vm.updateSubscription(sub.copy(name = name, amount = amount, billingDay = day, color = color))
                editingSub = null
            },
            onDismiss = { editingSub = null }
        )
    }

    payingSubId?.let { subId ->
        val sub = state.subscriptions.find { it.id == subId } ?: return@let
        PaySubSheet(sub = sub, accounts = state.accounts, onPay = { accId ->
            vm.paySubscription(subId, accId)
            payingSubId = null
        }, onDismiss = { payingSubId = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubFormSheet(initial: Subscription? = null, onSave: (String, Double, Int, String) -> Unit, onDismiss: () -> Unit) {
    var name   by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var day    by remember { mutableStateOf(initial?.billingDay?.toString() ?: "1") }
    var color  by remember { mutableStateOf(initial?.color ?: "#6366F1") }
    val colors = listOf("#0284c7","#7C3AED","#ec4899","#f97316","#059669","#dc2626","#f59e0b","#64748b")

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header — mismo estilo que Nuevo ingreso/gasto
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(PurpleBrand.copy(.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Repeat, null, tint = PurpleBrand, modifier = Modifier.size(26.dp))
                }
                Text(
                    if (initial != null) "Editar suscripción" else "Nueva suscripción",
                    fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface
                )
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre (Netflix, Spotify...)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface), colors = subColors())

            // Live preview
            if (name.isNotEmpty() && amount.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = parseHex(color).copy(.1f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(parseHex(color).copy(.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CreditCard, null, tint = parseHex(color))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Cobra el día $day de cada mes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatMXN(amount.toDoubleOrNull() ?: 0.0), fontWeight = FontWeight.Black, fontSize = 16.sp, color = parseHex(color))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Monto mensual") }, prefix = { Text("$") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = subColors())
                OutlinedTextField(value = day, onValueChange = { day = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Día cobro") }, modifier = Modifier.width(100.dp), shape = RoundedCornerShape(14.dp), textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = subColors())
            }

            Text("Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { c ->
                    Box(modifier = Modifier.size(if (color == c) 36.dp else 30.dp).clip(CircleShape).background(parseHex(c)).then(if (color == c) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier).clickable { color = c })
                }
            }

            Button(onClick = { if (name.isNotBlank() && amount.isNotEmpty()) onSave(name, amount.toDoubleOrNull() ?: 0.0, day.toIntOrNull() ?: 1, color) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = PurpleBrand), shape = RoundedCornerShape(16.dp)) {
                Text(if (initial != null) "Guardar cambios" else "Agregar suscripción", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaySubSheet(sub: Subscription, accounts: List<Account>, onPay: (String) -> Unit, onDismiss: () -> Unit) {
    var selectedAcc by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Confirmar pago", fontSize = 20.sp, fontWeight = FontWeight.Black, color = PurpleBrand)
            Surface(shape = RoundedCornerShape(16.dp), color = PurpleBrand.copy(.08f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sub.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMXN(sub.amount), fontSize = 36.sp, fontWeight = FontWeight.Black, color = PurpleBrand)
                }
            }
            accounts.forEach { acc ->
                val sel = selectedAcc == acc.id
                Surface(onClick = { selectedAcc = acc.id }, shape = RoundedCornerShape(14.dp), color = if (sel) parseHex(acc.color).copy(.12f) else MaterialTheme.colorScheme.surfaceVariant, border = if (sel) BorderStroke(1.5.dp, parseHex(acc.color)) else null, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(acc.color, 12.dp); Spacer(Modifier.width(8.dp))
                        Text(acc.name, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(formatMXN(acc.balance), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Button(onClick = { onPay(selectedAcc) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = PurpleBrand), shape = RoundedCornerShape(16.dp)) {
                Text("Confirmar pago", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun subColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurpleBrand, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = PurpleBrand, unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = PurpleBrand, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
)
