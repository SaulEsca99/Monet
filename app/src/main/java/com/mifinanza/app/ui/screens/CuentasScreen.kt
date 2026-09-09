package com.mifinanza.app.ui.screens

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentasScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var quickUpdateAccount by remember { mutableStateOf<Account?>(null) }

    val totalBalance = state.accounts.sumOf { it.balance }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = GreenBrand,
                contentColor = Color(0xFF021A12),
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp)) }
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular header
            CircularSectionHeader(
                label = "MI BILLETERA",
                mainValue = formatMXN(totalBalance),
                progress = 1f,
                accentColor = GreenBrand,
                stat1Label = "Cuentas registradas",
                stat1Value = "${state.accounts.size} cuenta${if (state.accounts.size != 1) "s" else ""}",
                stat2Label = "Mayor saldo",
                stat2Value = state.accounts.maxByOrNull { it.balance }?.let { "${it.name}: ${formatMXN(it.balance)}" } ?: "—"
            )

            if (state.accounts.isEmpty()) {
                EmptyState("💳", "Sin cuentas", "Agrega tu primera cuenta con el + abajo")
            }

            state.accounts.forEach { account ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color dot
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(parseHex(account.color).copy(.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (account.type == "cash") Icons.Default.Money else Icons.Default.AccountBalance,
                                null, tint = parseHex(account.color), modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(account.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(if (account.type == "cash") "Efectivo" else "Débito", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMXN(account.balance), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            // Quick add/subtract
                            IconButton(onClick = { quickUpdateAccount = account }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.AddCard, null, tint = GreenBrand, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { editingAccount = account }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { vm.deleteAccount(account.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, null, tint = RedBrand.copy(.6f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    // Quick deposit/withdraw sheet
    quickUpdateAccount?.let { acc ->
        QuickBalanceSheet(account = acc,
            onApply = { amount, isDeposit ->
                val newBalance = if (isDeposit) acc.balance + amount else acc.balance - amount
                vm.updateAccount(acc.copy(balance = newBalance))
                quickUpdateAccount = null
            },
            onDismiss = { quickUpdateAccount = null }
        )
    }

    if (showAdd) {
        AccountFormSheet(
            title = "Nueva cuenta",
            onSave = { name, type, balance, color ->
                vm.addAccount(name, type, balance, color)
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    editingAccount?.let { acc ->
        AccountFormSheet(
            title = "Editar cuenta",
            initial = acc,
            onSave = { name, type, balance, color ->
                vm.updateAccount(acc.copy(name = name, type = type, balance = balance, color = color))
                editingAccount = null
            },
            onDismiss = { editingAccount = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFormSheet(
    title: String,
    initial: Account? = null,
    onSave: (String, String, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name    by remember { mutableStateOf(initial?.name ?: "") }
    var type    by remember { mutableStateOf(initial?.type ?: "debit") }
    var balance by remember { mutableStateOf(if (initial != null && initial.balance != 0.0) initial.balance.toString() else "") }
    var color   by remember { mutableStateOf(initial?.color ?: "#6366F1") }

    val colors = listOf("#059669","#6366F1","#3B82F6","#F59E0B","#EF4444","#8B5CF6","#06B6D4","#F97316")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header — mismo estilo que Nuevo ingreso/gasto
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(GreenBrand.copy(.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = GreenBrand, modifier = Modifier.size(26.dp))
                }
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }

            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("debit" to "💳 Débito", "cash" to "💵 Efectivo").forEach { (t, label) ->
                    Button(
                        onClick = { type = t },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor   = if (type == t) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
            }

            // Name
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(if (type == "debit") "Nombre del banco" else "Ej: Cartera, Efectivo") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                colors = fieldColors()
            )

            // Balance
            OutlinedTextField(
                value = balance, onValueChange = { balance = it },
                label = { Text(if (initial != null) "Saldo actual" else "Saldo inicial") },
                prefix = { Text("$  ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors()
            )

            // Color picker
            Text("Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { c ->
                    Box(
                        modifier = Modifier.size(if (color == c) 38.dp else 32.dp).clip(CircleShape)
                            .background(parseHex(c))
                            .then(if (color == c) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                            .clickable { color = c }
                    )
                }
            }

            // Save button
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, type, balance.toDoubleOrNull() ?: 0.0, color) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenBrand, contentColor = Color(0xFF021A12)),
                shape = RoundedCornerShape(18.dp)
            ) { Text(if (initial != null) "Guardar cambios" else "Crear cuenta", fontWeight = FontWeight.Black, fontSize = 16.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickBalanceSheet(account: Account, onApply: (Double, Boolean) -> Unit, onDismiss: () -> Unit) {
    var amount     by remember { mutableStateOf("") }
    var isDeposit  by remember { mutableStateOf(true) }
    val resultBalance = amount.toDoubleOrNull()?.let {
        if (isDeposit) account.balance + it else account.balance - it
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(parseHex(account.color).copy(.2f)), contentAlignment = Alignment.Center) {
                    Icon(if (account.type == "cash") Icons.Default.Money else Icons.Default.AccountBalance, null, tint = parseHex(account.color))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Actualizar saldo", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(account.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Text(formatMXN(account.balance), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = GreenBrand)
            }

            // Type toggle: Abonar / Descontar
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { isDeposit = true }, modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDeposit) GreenBrand else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor   = if (isDeposit) Color(0xFF021A12) else MaterialTheme.colorScheme.onSurfaceVariant
                    ), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Abonar", fontWeight = FontWeight.Bold)
                }
                Button(onClick = { isDeposit = false }, modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isDeposit) RedBrand else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor   = if (!isDeposit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.RemoveCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Descontar", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("¿Cuánto?") },
                prefix = { Text("$  ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors()
            )

            // Result preview
            resultBalance?.let { rb ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo resultante", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMXN(rb), fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (rb >= 0) GreenBrand else RedBrand)
                }
            }

            Button(
                onClick = { amount.toDoubleOrNull()?.let { onApply(it, isDeposit) } },
                enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeposit) GreenBrand else RedBrand,
                    contentColor   = if (isDeposit) Color(0xFF021A12) else Color.White
                ),
                shape = RoundedCornerShape(18.dp)
            ) { Text(if (isDeposit) "Abonar al saldo" else "Descontar del saldo", fontWeight = FontWeight.Black, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = GreenBrand, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor    = GreenBrand, unfocusedLabelColor  = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor          = GreenBrand,
    focusedTextColor     = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
)
