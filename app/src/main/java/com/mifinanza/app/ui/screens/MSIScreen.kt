package com.mifinanza.app.ui.screens

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifinanza.app.data.*
import com.mifinanza.app.ui.components.*
import com.mifinanza.app.ui.theme.*
import com.mifinanza.app.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MSIScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<Pair<MsiPlan, String>?>(null) } // plan, "monthly"|"liquidate"

    val active = state.msiPlans.filter { it.status == "active" }
    val done   = state.msiPlans.filter { it.status == "paid_off" }
    val totalMo  = active.sumOf { it.monthlyPayment }
    val totalRem = active.sumOf { (it.months - it.paidMonths) * it.monthlyPayment }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AppFab(icon = { Icon(Icons.Default.Add, null) }, onClick = { showAdd = true }, color = BlueBrand)
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Circular/donut header
            val totalPaidMsiAmount = state.msiPlans.sumOf { it.paidMonths * it.monthlyPayment }
            val totalMsiMonths = state.msiPlans.sumOf { it.months }
            val totalMsiPaidMonths = state.msiPlans.sumOf { it.paidMonths }
            CircularSectionHeader(
                label = "A MESES SIN INTERESES",
                mainValue = formatMXN(totalMo),
                progress = if (totalMsiMonths == 0) 0f else totalMsiPaidMonths.toFloat() / totalMsiMonths,
                accentColor = BlueBrand,
                stat1Label = "Mensualidad total",
                stat1Value = formatMXN(totalMo) + "/mes",
                stat2Label = "Deuda total restante",
                stat2Value = if (totalRem > 0) formatMXN(totalRem) else "Sin deuda"
            )

            // Content with padding
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {

            if (active.isEmpty() && done.isEmpty()) {
                EmptyState("🔄", "Sin planes MSI", "Agrega tus compras a meses sin intereses")
            }

            active.forEach { plan ->
                val dsm = rememberSwipeToDismissBoxState(
                    confirmValueChange = { v -> v == SwipeToDismissBoxValue.EndToStart },
                    positionalThreshold = { it * .45f }
                )
                LaunchedEffect(dsm.currentValue) {
                    if (dsm.currentValue == SwipeToDismissBoxValue.EndToStart) vm.deleteMsiPlan(plan.id)
                }
                SwipeToDismissBox(state = dsm, enableDismissFromStartToEnd = false, backgroundContent = { SwipeDeleteBackground() }) {
                MsiPlanCard(plan = plan, accounts = state.accounts,
                    onConfirm = { confirmAction = plan to "monthly" },
                    onNoDeduct = { vm.markMsiMonthNoDeduct(plan.id) },
                    onLiquidate = { confirmAction = plan to "liquidate" },
                    onDelete = { vm.deleteMsiPlan(plan.id) }
                )
                } // SwipeToDismissBox
            }

            if (done.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${done.size} plan${if (done.size > 1) "es" else ""} completado${if (done.size > 1) "s" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                }
                if (expanded) {
                    done.forEach { plan ->
                        AppCard() {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = GreenBrand, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(plan.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${plan.months} meses · ${formatMXN(plan.totalAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                                }
                                TagBadge("Pagado", GreenBrand.copy(.15f), GreenBrand)
                                IconButton(onClick = { vm.deleteMsiPlan(plan.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f), modifier = Modifier.size(16.dp))
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

    if (showAdd) AddMsiSheet(vm = vm) { showAdd = false }

    confirmAction?.let { (plan, type) ->
        ConfirmMsiDialog(plan = plan, type = type, accounts = state.accounts,
            onConfirm = { accId ->
                if (type == "monthly") vm.confirmMsiPayment(plan.id, accId)
                else vm.liquidateMsi(plan.id, accId)
                confirmAction = null
            },
            onDismiss = { confirmAction = null }
        )
    }
}

@Composable
private fun MsiPlanCard(plan: MsiPlan, accounts: List<Account>, onConfirm: () -> Unit, onNoDeduct: () -> Unit, onLiquidate: () -> Unit, onDelete: () -> Unit) {
    val ym = currentYearMonth()
    val paidThisMonth = plan.payments.any { it.date.startsWith(ym) }
    val pct = plan.paidMonths.toFloat() / plan.months.toFloat()
    val appConfirmed = (plan.paidMonths - plan.initialPaidMonths).coerceAtLeast(0)
    val appPct = appConfirmed.toFloat() / plan.months.toFloat()
    val days = daysUntilDay(plan.paymentDay)
    val urgent = days <= 5 && !paidThisMonth

    val animPct by animateFloatAsState(targetValue = pct, animationSpec = tween(700, easing = EaseOutCubic), label = "p")
    val animAppPct by animateFloatAsState(targetValue = appPct, animationSpec = tween(700, easing = EaseOutCubic), label = "ap")

    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (plan.description.isNotEmpty()) Text(plan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f), modifier = Modifier.size(16.dp)) }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip("Mensualidad", formatMXN(plan.monthlyPayment), BlueBrand, Modifier.weight(1f))
            InfoChip("Pagado",     formatMXN(plan.paidMonths * plan.monthlyPayment), GreenBrand, Modifier.weight(1f))
            InfoChip("Pendiente",  formatMXN((plan.months - plan.paidMonths) * plan.monthlyPayment), AmberBrand, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mes ${plan.paidMonths} de ${plan.months}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("${plan.months - plan.paidMonths} restantes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
        }
        Spacer(Modifier.height(6.dp))

        // Progress bar (pre-paid = lighter, app-confirmed = full)
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(BorderColor)) {
            Box(modifier = Modifier.fillMaxWidth(animPct).height(8.dp).clip(RoundedCornerShape(4.dp)).background(BlueBrand.copy(.3f)))
            Box(modifier = Modifier.fillMaxWidth(animAppPct).height(8.dp).clip(RoundedCornerShape(4.dp)).background(BlueBrand))
        }

        if (plan.initialPaidMonths > 0) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(BlueBrand.copy(.3f), "${plan.initialPaidMonths} pre-pagados")
                LegendDot(BlueBrand, "$appConfirmed en app")
            }
        }

        Spacer(Modifier.height(10.dp))
        val nextDays = if (paidThisMonth) daysUntilNextMonthDay(plan.paymentDay) else days
        val nextLabel = if (paidThisMonth) nextMonthLabel(plan.paymentDay) else "día ${plan.paymentDay}"
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(when { paidThisMonth -> GreenBrand.copy(.1f); urgent -> AmberBrand.copy(.1f); else -> MaterialTheme.colorScheme.surfaceVariant })
            .border(if (paidThisMonth || urgent) 1.dp else 0.dp, if (paidThisMonth) GreenBrand.copy(.3f) else AmberBrand.copy(.3f), RoundedCornerShape(10.dp))
            .padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (paidThisMonth) Icons.Default.CheckCircle else Icons.Default.Autorenew,
                    null,
                    tint = when { paidThisMonth -> GreenBrand; urgent -> AmberBrand; else -> MaterialTheme.colorScheme.onSurfaceVariant },
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        paidThisMonth -> "✓ Pagado · próximo cobro $nextLabel · en $nextDays días"
                        days == 0 -> "🔔 ¡Hoy se cobra! · ${currentMonthLabel(plan.paymentDay)}"
                        urgent -> "⚡ Cobro en $days días · ${currentMonthLabel(plan.paymentDay)}"
                        else -> "Próximo cobro: ${currentMonthLabel(plan.paymentDay)} · en $days días"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when { paidThisMonth -> GreenBrand; urgent -> AmberBrand; else -> MaterialTheme.colorScheme.onSurfaceVariant }
                )
            }
        }

        if (accounts.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            if (paidThisMonth) {
                // Already paid this month — show liquidate + no-deduct for next month
                OutlinedButton(onClick = onLiquidate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant), border = BorderStroke(1.dp, BorderColor), shape = RoundedCornerShape(12.dp)) {
                    Text("Liquidar plan completo", fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onNoDeduct, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.HowToVote, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                    Spacer(Modifier.width(4.dp))
                    Text("Ya pagué el mes ${plan.paidMonths + 1} (sin descontar saldo)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueBrand),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mes ${plan.paidMonths + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onLiquidate, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant), border = BorderStroke(1.dp, BorderColor), shape = RoundedCornerShape(12.dp)) {
                        Text("Liquidar", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onNoDeduct, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.HowToVote, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                    Spacer(Modifier.width(4.dp))
                    Text("Ya pagué el mes ${plan.paidMonths + 1} (sin descontar saldo)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(.1f)).padding(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(.7f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMsiSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("12") }
    var day by remember { mutableStateOf("1") }
    var prePaid by remember { mutableStateOf(false) }
    var prePaidMonths by remember { mutableStateOf("0") }

    val monthlyPayment = if (total.isNotEmpty() && months.isNotEmpty() && (months.toIntOrNull() ?: 0) > 0)
        (total.toDoubleOrNull() ?: 0.0) / (months.toIntOrNull() ?: 1) else 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header — mismo estilo que Nuevo ingreso/gasto
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(BlueBrand.copy(.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Autorenew, null, tint = BlueBrand, modifier = Modifier.size(26.dp))
                }
                Text("Nuevo plan MSI", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("¿Qué compraste?") }, modifier = Modifier.fillMaxWidth(), colors = msiFieldColors())
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth(), colors = msiFieldColors())

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = total, onValueChange = { total = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Precio total") }, prefix = { Text("$") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), colors = msiFieldColors())
                OutlinedTextField(value = months, onValueChange = { months = it.filter { c -> c.isDigit() } }, label = { Text("Meses") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), colors = msiFieldColors())
            }

            if (monthlyPayment > 0) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BlueBrand.copy(.12f)).border(1.dp, BlueBrand.copy(.2f), RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MENSUALIDAD", style = MaterialTheme.typography.labelSmall, color = BlueBrand.copy(.7f))
                        Text(formatMXN(monthlyPayment), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = BlueBrand)
                        Text("× $months meses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedTextField(value = day, onValueChange = { day = it.filter { c -> c.isDigit() } }, label = { Text("Día de cobro de la tarjeta") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), colors = msiFieldColors())

            // Pre-paid toggle
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp).clickable { prePaid = !prePaid }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("¿Ya llevas meses pagados?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Antes de registrarlo aquí", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                }
                Switch(checked = prePaid, onCheckedChange = { prePaid = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GreenBrand))
            }

            if (prePaid) {
                OutlinedTextField(value = prePaidMonths, onValueChange = { prePaidMonths = it.filter { c -> c.isDigit() } }, label = { Text("¿Cuántos meses llevas pagados?") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), colors = msiFieldColors())
                if ((prePaidMonths.toIntOrNull() ?: 0) > 0) {
                    Text("ℹ️ Los $prePaidMonths meses previos no se descontarán de tu saldo.", style = MaterialTheme.typography.bodySmall, color = AmberBrand)
                }
            }

            PrimaryButton("Agregar plan MSI", color = BlueBrand, onClick = {
                if (name.isNotEmpty() && total.isNotEmpty()) {
                    vm.addMsiPlan(name, desc, total.toDouble(), months.toIntOrNull() ?: 12, day.toIntOrNull() ?: 1, today(), if (prePaid) prePaidMonths.toIntOrNull() ?: 0 else 0)
                    onDismiss()
                }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmMsiDialog(plan: MsiPlan, type: String, accounts: List<Account>, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val isMonthly = type == "monthly"
    val amount = if (isMonthly) plan.monthlyPayment else (plan.months - plan.paidMonths) * plan.monthlyPayment
    var selectedAcc by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (isMonthly) "Pagar mes ${plan.paidMonths + 1}" else "Liquidar plan", style = MaterialTheme.typography.headlineMedium, color = BlueBrand)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BlueBrand.copy(.1f)).padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(plan.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMXN(amount), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = BlueBrand)
                    Text(if (isMonthly) "Mes ${plan.paidMonths + 1} de ${plan.months}" else "${plan.months - plan.paidMonths} meses liquidados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
                }
            }
            accounts.forEach { acc ->
                val sel = selectedAcc == acc.id
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (sel) parseHex(acc.color).copy(.15f) else MaterialTheme.colorScheme.surfaceVariant).border(if (sel) 1.dp else 0.dp, parseHex(acc.color).copy(.5f), RoundedCornerShape(12.dp)).clickable { selectedAcc = acc.id }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(acc.color, 12.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(acc.name, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(formatMXN(acc.balance), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PrimaryButton(if (isMonthly) "Confirmar y descontar" else "Liquidar ahora", color = BlueBrand, onClick = { onConfirm(selectedAcc) })
        }
    }
}

@Composable
private fun msiFieldColors() = appFieldColors(accent = BlueBrand)
