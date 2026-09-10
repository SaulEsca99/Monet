package com.mifinanza.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.LocalContext
import com.mifinanza.app.viewmodel.AppViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val ym = currentYearMonth()
    var filterYM by remember { mutableStateOf(ym) }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var addType by remember { mutableStateOf("expense") }

    val filtered = remember(state.transactions, filterYM, filterCategory) {
        state.transactions
            .filter { it.date.startsWith(filterYM) }
            .let { list -> if (filterCategory != null) list.filter { it.category == filterCategory } else list }
            .sortedByDescending { it.date }
    }
    val inc = filtered.filter { it.type == "income" }.sumOf { it.amount }
    val exp = filtered.filter { it.type == "expense" }.sumOf { it.amount }

    val months = remember(state.transactions) {
        val s = state.transactions.map { it.date.substring(0, 7) }.toMutableSet()
        s.add(ym)
        s.sortedDescending()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // TWO circular FABs side by side
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                // Income FAB — exact same size as expense
                FloatingActionButton(
                    onClick = { addType = "income"; showAdd = true },
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(58.dp)
                ) { Icon(Icons.Default.Add, "Ingreso", modifier = Modifier.size(26.dp)) }

                // Expense FAB — exact same size
                FloatingActionButton(
                    onClick = { addType = "expense"; showAdd = true },
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(58.dp)
                ) { Icon(Icons.Default.Remove, "Gasto", modifier = Modifier.size(26.dp)) }
            }
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxWidth().padding(pad).background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {

            // Summary cards
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = GreenBrand.copy(.12f), shadowElevation = 0.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(GreenBrand.copy(.2f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.TrendingUp, null, tint = GreenBrand, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("INGRESOS", style = MaterialTheme.typography.labelSmall, color = GreenBrand)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(formatMXN(inc), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = GreenBrand)
                        }
                    }
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = RedBrand.copy(.12f), shadowElevation = 0.dp) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(RedBrand.copy(.2f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.TrendingDown, null, tint = RedBrand, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("GASTOS", style = MaterialTheme.typography.labelSmall, color = RedBrand)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(formatMXN(exp), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = RedBrand)
                        }
                    }
                }
                // Progress bar: % of income spent
                if (inc > 0) {
                    Spacer(Modifier.height(8.dp))
                    val pct = (exp / inc).toFloat().coerceIn(0f, 1f)
                    val pctColor = if (pct > 0.8f) RedBrand else if (pct > 0.5f) AmberBrand else GreenBrand
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${(pct * 100).toInt()}% del ingreso gastado", fontSize = 11.sp, color = pctColor)
                        Text(formatMXN(inc - exp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (inc > exp) GreenBrand else RedBrand)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = pctColor, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            // Month chips
            ScrollableTabRow(
                selectedTabIndex = months.indexOf(filterYM).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = GreenBrand,
                edgePadding = 16.dp,
                indicator = {}, divider = {}
            ) {
                months.forEach { m ->
                    val sel = filterYM == m
                    Tab(selected = sel, onClick = { filterYM = m },
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(
                            if (sel) GreenBrand.copy(.15f) else Color.Transparent
                        ).padding(horizontal = 4.dp)
                    ) {
                        Text(
                            monthLabel(m),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) GreenBrand else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category filter chips
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filterCategory == null, onClick = { filterCategory = null },
                    label = { Text("Todos", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(.2f)))
                CATEGORIES.forEach { cat ->
                    val catColor = categoryColor(cat.id)
                    FilterChip(selected = filterCategory == cat.id, onClick = { filterCategory = if (filterCategory == cat.id) null else cat.id },
                        label = { Text("${cat.emoji} ${cat.name}", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = catColor.copy(.2f), selectedLabelColor = catColor))
                }
            }

            // Transaction list
            if (filtered.isEmpty()) {
                EmptyState("💸", "Sin movimientos", "Usa el + o − para registrar")
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtered.forEach { tx ->
                        key(tx.id) {  // key evita que dismissState se reutilice tras borrar
                        val acc = state.accounts.find { it.id == tx.accountId }
                        val meta = getCategoryMeta(tx.category)
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { v -> v == SwipeToDismissBoxValue.EndToStart },
                            positionalThreshold = { it * .45f }
                        )
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                vm.deleteTransaction(tx.id)
                            }
                        }
                        SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = false, backgroundContent = {
                            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.error).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    Text("Borrar", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }) {
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                val txColor = if (tx.type == "income") Color(0xFF10B981) else Color(0xFFEF4444)
                                val catColor = if (tx.type == "income") Color(0xFF10B981) else categoryColor(tx.category)
                                // Left accent bar — color de categoría
                                Box(modifier = Modifier.width(3.dp).height(50.dp).clip(RoundedCornerShape(2.dp)).background(catColor))
                                Spacer(Modifier.width(10.dp))
                                // Icon — color de categoría
                                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(catColor.copy(.15f)), contentAlignment = Alignment.Center) {
                                    Text(meta.emoji, fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                // Info
                                Column(Modifier.weight(1f)) {
                                    Text(tx.description, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(meta.name, fontSize = 11.sp, color = catColor)
                                        Text(" · ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${tx.date.substring(8)} ${monthLabel(tx.date.substring(0,7))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (tx.time.isNotEmpty()) {
                                            Text(" ${tx.time}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.7f))
                                        }
                                    }
                                    Text(acc?.name ?: "", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                                }
                                // Amount
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${if (tx.type == "income") "+" else "−"}${formatMXN(tx.amount)}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = txColor)
                                    Surface(shape = RoundedCornerShape(6.dp), color = txColor.copy(.12f)) {
                                        Text(if(tx.type=="income") "Ingreso" else "Gasto", modifier=Modifier.padding(horizontal=6.dp, vertical=2.dp), fontSize=9.sp, color=txColor, fontWeight=FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        } // end SwipeToDismissBox
                        } // end key(tx.id)
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAdd) AddTransactionSheet(vm = vm, type = addType) { showAdd = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(vm: AppViewModel, type: String, onDismiss: () -> Unit) {
    val state by vm.state.collectAsState()
    var amount      by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category    by remember { mutableStateOf(if (type == "income") "salary" else "other") }
    var accountId   by remember { mutableStateOf(state.accounts.firstOrNull()?.id ?: "") }
    var date        by remember { mutableStateOf(today()) }
    var showNewTypeDialog by remember { mutableStateOf(false) }
    var newTypeName by remember { mutableStateOf("") }
    val isIncome    = type == "income"
    val accentColor = if (isIncome) GreenBrand else RedBrand
    val context     = LocalContext.current
    val cal         = Calendar.getInstance()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(.15f)), contentAlignment = Alignment.Center) {
                    Icon(if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown, null, tint = accentColor)
                }
                Spacer(Modifier.width(10.dp))
                Text(if (isIncome) "Nuevo ingreso" else "Nuevo gasto", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }

            // Amount
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Monto") },
                prefix = { Text("$  ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = txFieldColors(accentColor)
            )

            // Amount preview
            if (amount.isNotEmpty() && amount.toDoubleOrNull() != null) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(accentColor.copy(.1f)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Text(formatMXN(amount.toDouble()), fontSize = 24.sp, fontWeight = FontWeight.Black, color = accentColor)
                }
            }

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text(if (isIncome) "Ej: Sueldo, freelance..." else "Ej: Super, gasolina...") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                colors = txFieldColors(accentColor)
            )

            // Category chips
            Text("Categoría", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val cats = if (isIncome) CATEGORIES.filter { it.id in listOf("salary", "other") } else CATEGORIES
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEach { cat ->
                    val sel = category == cat.id
                    FilterChip(selected = sel, onClick = { category = cat.id },
                        label = { Text("${cat.emoji} ${cat.name}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor.copy(.2f), selectedLabelColor = accentColor))
                }
                // Custom income types (only for income)
                if (isIncome) {
                    state.customIncomeTypes.forEach { ct ->
                        val sel = description == ct.name
                        FilterChip(selected = sel,
                            onClick = { description = ct.name; category = "salary" },
                            label = { Text("${ct.emoji} ${ct.name}", fontSize = 12.sp) },
                            trailingIcon = { IconButton(onClick = { vm.deleteCustomIncomeType(ct.id) }, modifier = Modifier.size(14.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp)) } },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor.copy(.2f), selectedLabelColor = accentColor))
                    }
                    // + Crear tipo
                    FilterChip(selected = false, onClick = { showNewTypeDialog = true },
                        label = { Text("+ Crear tipo", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant))
                }
            }

            // Date picker — uses DateField with Box overlay fix
            DateField(label = "Fecha", value = date, accent = accentColor) { date = it }

            // Account selector with quick create wallet
            var showNewWalletDialog by remember { mutableStateOf(false) }
            var newWalletName by remember { mutableStateOf("") }
            var newWalletType by remember { mutableStateOf("debit") }
            var selectNewestAccount by remember { mutableStateOf(false) }
            LaunchedEffect(state.accounts.size) {
                if (selectNewestAccount && state.accounts.isNotEmpty()) {
                    accountId = state.accounts.last().id
                    selectNewestAccount = false
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                    Text("Cuenta / Billetera", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Quick create wallet button (only on income)
                    if (isIncome) {
                        TextButton(onClick = { showNewWalletDialog = true }, contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Default.AddCard, null, modifier=Modifier.size(14.dp), tint=accentColor)
                            Spacer(Modifier.width(3.dp))
                            Text("+ Nueva billetera", fontSize=11.sp, color=accentColor, fontWeight=FontWeight.Bold)
                        }
                    }
                }
                Row(modifier=Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.accounts.forEach { acc ->
                        val sel = accountId == acc.id
                        Surface(
                            onClick = { accountId = acc.id },
                            shape = RoundedCornerShape(14.dp),
                            color = if (sel) parseHex(acc.color).copy(.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (sel) BorderStroke(1.5.dp, parseHex(acc.color)) else null,
                            modifier = Modifier.height(52.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.Center) {
                                Text(acc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (sel) parseHex(acc.color) else MaterialTheme.colorScheme.onSurface)
                                Text(formatMXN(acc.balance), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Quick create wallet dialog
            if (showNewWalletDialog) {
                AlertDialog(
                    onDismissRequest = { showNewWalletDialog = false; newWalletName = "" },
                    title = {
                        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AddCard, null, tint=GreenBrand)
                            Text("Nueva billetera", fontWeight=FontWeight.Black)
                        }
                    },
                    text = {
                        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text("Crea una nueva cuenta o billetera al instante", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value=newWalletName, onValueChange={newWalletName=it}, label={Text("Nombre (Banco, Efectivo, Ahorro...)")}, modifier=Modifier.fillMaxWidth(), singleLine=true, colors=appFieldColors(GreenBrand))
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                listOf("debit" to "💳 Débito", "cash" to "💵 Efectivo").forEach { (t,label) ->
                                    FilterChip(selected=newWalletType==t, onClick={newWalletType=t}, label={Text(label, fontSize=12.sp)}, colors=FilterChipDefaults.filterChipColors(selectedContainerColor=GreenBrand.copy(.2f), selectedLabelColor=GreenBrand))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newWalletName.isNotBlank()) {
                                vm.addAccount(newWalletName.trim(), newWalletType, 0.0, "#059669")
                                selectNewestAccount = true  // triggers LaunchedEffect to auto-select
                                showNewWalletDialog = false; newWalletName = ""
                            }
                        }, colors=ButtonDefaults.buttonColors(containerColor=GreenBrand)) {
                            Text("Crear billetera", color=Color(0xFF021A12), fontWeight=FontWeight.Bold)
                        }
                    },
                    dismissButton = { TextButton(onClick={showNewWalletDialog=false;newWalletName=""}) { Text("Cancelar") } }
                )
            }

            // Save button
            Button(
                onClick = {
                    if (amount.isNotEmpty() && description.isNotEmpty() && accountId.isNotEmpty()) {
                        vm.addTransaction(accountId, type, amount.toDouble(), description, category, date)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = if (isIncome) Color(0xFF021A12) else Color.White),
                shape = RoundedCornerShape(18.dp)
            ) { Text(if (isIncome) "Registrar ingreso" else "Registrar gasto", fontWeight = FontWeight.Black, fontSize = 16.sp) }
        }
    }

    // Dialog: crear tipo de ingreso personalizado
    if (showNewTypeDialog) {
        AlertDialog(
            onDismissRequest = { showNewTypeDialog = false; newTypeName = "" },
            title = { Text("Nuevo tipo de ingreso", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = newTypeName, onValueChange = { newTypeName = it },
                    label = { Text("Ej: Dinero universidad, Freelance...") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = appFieldColors(GreenBrand)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newTypeName.isNotBlank()) {
                        vm.addCustomIncomeType(newTypeName)
                        showNewTypeDialog = false; newTypeName = ""
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = GreenBrand)) {
                    Text("Guardar", color = Color(0xFF021A12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTypeDialog = false; newTypeName = "" }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun txFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = accent, unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = accent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
)
