package com.mifinanza.app.ui.screens

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifinanza.app.data.*
import com.mifinanza.app.ui.components.*
import com.mifinanza.app.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.mifinanza.app.viewmodel.AppViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrestamosScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmPay by remember { mutableStateOf<Loan?>(null) }

    val pending  = state.loans.filter { it.status == "pending" }
    val paid     = state.loans.filter { it.status == "paid" }
    val overdue  = vm.overdueLoans()
    val total    = vm.pendingLoanTotal()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AppFab(icon = { Icon(Icons.Default.Add, null) }, onClick = { showAdd = true }, color = AmberBrand)
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val loanSubtitle = buildString {
                append("${pending.size} pendiente${if (pending.size != 1) "s" else ""}")
                if (overdue.isNotEmpty()) append(" · ${overdue.size} vencido${if (overdue.size != 1) "s" else ""}")
                if (paid.isNotEmpty()) append(" · ${paid.size} cobrado${if (paid.size != 1) "s" else ""}")
            }
            CircularSectionHeader(
                label = "PRÉSTAMOS",
                mainValue = formatMXN(total),
                progress = if (state.loans.isEmpty()) 0f else paid.size.toFloat() / state.loans.size,
                accentColor = AmberBrand,
                stat1Label = "Pendientes de cobro",
                stat1Value = "${pending.size} por ${formatMXN(total)}",
                stat2Label = "Cobrados",
                stat2Value = "${paid.size} préstamo${if (paid.size != 1) "s" else ""}"
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            if (state.loans.isEmpty()) {
                EmptyState("🤝", "Sin préstamos", "Registra el dinero que prestas y lleva el control")
            }

            // Pending
            if (pending.isNotEmpty()) {
                SectionTitle("Pendientes de cobro")
                pending.forEach { loan ->
                    val dsm = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
                        if (v == SwipeToDismissBoxValue.EndToStart) { vm.deleteLoan(loan.id); true } else false
                    })
                    SwipeToDismissBox(state = dsm, enableDismissFromStartToEnd = false, backgroundContent = { SwipeDeleteBackground() }) {
                    val days = daysUntilDate(loan.expectedReturnDate)
                    val isOverdue = days < 0
                    val isUrgent  = days in 0..3
                    val accentColor = when {
                        isOverdue -> RedBrand
                        isUrgent  -> AmberBrand
                        else      -> GreenBrand
                    }

                    AppCard {
                        // Top color stripe
                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(accentColor.copy(.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(loan.personName.first().uppercaseChar().toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(loan.personName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(6.dp))
                                    if (isOverdue) TagBadge("⚠ Vencido ${Math.abs(days)}d", RedBrand.copy(.15f), RedBrand)
                                    else if (isUrgent) TagBadge("¡$days días!", AmberBrand.copy(.15f), AmberBrand)
                                }
                                if (loan.description.isNotEmpty()) Text(loan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                            }
                            IconButton(onClick = { vm.deleteLoan(loan.id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f), modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(formatMXN(loan.amount), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)

                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)) {
                                Text("PRESTADO EL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                                Text(formatDate(loan.givenDate), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            }
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(.1f)).border(1.dp, accentColor.copy(.2f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                                Text("DEVOLVER EL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                                Text(formatDate(loan.expectedReturnDate), style = MaterialTheme.typography.bodyMedium, color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(accentColor.copy(.08f)).padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    when {
                                        isOverdue  -> "Lleva ${Math.abs(days)} día${if (Math.abs(days) != 1) "s" else ""} vencido. Habla con ${loan.personName}."
                                        days == 0  -> "¡Hoy ${loan.personName} te devuelve el dinero!"
                                        else       -> "${loan.personName} tiene $days día${if (days != 1) "s" else ""} para pagarte."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accentColor
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { confirmPay = loan },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBrand, contentColor = Color(0xFF021A12)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ya me pagó ${loan.personName}", fontWeight = FontWeight.Bold)
                        }
                    }
                    } // SwipeToDismissBox
                }
            }

            // Paid
            if (paid.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${paid.size} cobrado${if (paid.size != 1) "s" else ""}", color = GreenBrand)
                }
                if (expanded) {
                    paid.forEach { loan ->
                        AppCard() {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(GreenBrand.copy(.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, null, tint = GreenBrand, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(loan.personName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Cobrado el ${if (loan.paidDate.isNotEmpty()) formatDate(loan.paidDate) else "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                                }
                                Text(formatMXN(loan.amount), fontWeight = FontWeight.Bold, color = GreenBrand)
                                IconButton(onClick = { vm.deleteLoan(loan.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
            } // end content Column
        }
    }

    if (showAdd) AddLoanSheet(vm = vm) { showAdd = false }

    confirmPay?.let { loan ->
        ConfirmLoanPaidDialog(loan = loan, onConfirm = { vm.markLoanPaid(loan.id); confirmPay = null }, onDismiss = { confirmPay = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLoanSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    var personName   by remember { mutableStateOf("") }
    var amount       by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }
    var givenDate    by remember { mutableStateOf(today()) }
    var expectedDate by remember { mutableStateOf("") }

    val days = if (expectedDate.isNotEmpty()) daysUntilDate(expectedDate) else null

    val context = LocalContext.current
    val cal = Calendar.getInstance()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header — mismo estilo que Nuevo ingreso/gasto
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(AmberBrand.copy(.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Handshake, null, tint = AmberBrand, modifier = Modifier.size(26.dp))
                }
                Text("Registrar préstamo", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }

            OutlinedTextField(value = personName, onValueChange = { personName = it }, label = { Text("¿A quién le prestaste?") }, modifier = Modifier.fillMaxWidth(), colors = loanFieldColors())

            if (personName.isNotEmpty()) {
                Row(Modifier.clip(RoundedCornerShape(10.dp)).background(AmberBrand.copy(.1f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(AmberBrand), contentAlignment = Alignment.Center) {
                        Text(personName.first().uppercaseChar().toString(), fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Préstamo para $personName", color = AmberBrand, fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("¿Cuánto prestaste?") }, prefix = { Text("$") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), colors = loanFieldColors())

            if (amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0) {
                Text(formatMXN(amount.toDouble()), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AmberBrand, modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("¿Para qué? (opcional)") }, modifier = Modifier.fillMaxWidth(), colors = loanFieldColors())

            // Date pickers — DateField con Box overlay fix
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField("Fecha prestado", givenDate, AmberBrand, Modifier.weight(1f)) { givenDate = it }
                DateField("¿Cuándo devuelve?", expectedDate, AmberBrand, Modifier.weight(1f)) { expectedDate = it }
            }

            days?.let { d ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (d < 0) RedBrand.copy(.1f) else if (d <= 7) AmberBrand.copy(.1f) else GreenBrand.copy(.1f))
                    .padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = if (d < 0) RedBrand else if (d <= 7) AmberBrand else GreenBrand, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (d < 0) "Fecha ya pasó" else "Faltan $d días para que te devuelvan el dinero",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (d < 0) RedBrand else if (d <= 7) AmberBrand else GreenBrand
                        )
                    }
                }
            }

            PrimaryButton("Registrar préstamo", color = AmberBrand, onClick = {
                if (personName.isNotEmpty() && amount.isNotEmpty() && expectedDate.isNotEmpty()) {
                    vm.addLoan(personName, amount.toDouble(), description, givenDate, expectedDate)
                    onDismiss()
                }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmLoanPaidDialog(loan: Loan, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Confirmar cobro", style = MaterialTheme.typography.headlineMedium, color = GreenBrand)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GreenBrand.copy(.1f)).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓", fontSize = 36.sp, color = GreenBrand)
                    Spacer(Modifier.height(8.dp))
                    Text("${loan.personName} te pagó", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMXN(loan.amount), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = GreenBrand)
                }
            }
            Text("Se archivará en el historial de préstamos cobrados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f), modifier = Modifier.align(Alignment.CenterHorizontally))
            PrimaryButton("Confirmar cobro", color = GreenBrand, onClick = onConfirm)
        }
    }
}

@Composable
private fun loanFieldColors() = appFieldColors(accent = AmberBrand)
