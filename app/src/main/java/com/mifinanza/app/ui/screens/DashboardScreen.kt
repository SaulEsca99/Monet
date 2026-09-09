package com.mifinanza.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifinanza.app.data.*
import com.mifinanza.app.ui.components.*
import com.mifinanza.app.ui.theme.*
import com.mifinanza.app.viewmodel.AppViewModel
import java.util.Calendar
import kotlin.math.*

// ── Animated number ──────────────────────────────────────────────────────────
@Composable
fun AnimatedMoney(value: Double, color: Color, fontSize: Float = 44f) {
    val animValue by animateFloatAsState(targetValue = value.toFloat(), animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f), label = "money")
    Text(text = formatMXN(animValue.toDouble()), fontSize = fontSize.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = (-1.5).sp)
}

// ── Sparkline ────────────────────────────────────────────────────────────────
@Composable
fun SparklineChart(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    if (values.size < 2) return
    val p by animateFloatAsState(1f, tween(1200, easing = EaseOutCubic), label = "s")
    Canvas(modifier = modifier) {
        val mn = values.min(); val mx = values.max(); val rng = (mx - mn).takeIf { it > 0 } ?: 1.0
        val pad = 6.dp.toPx()
        val pts = values.mapIndexed { i, v ->
            Offset(pad + i / (values.size - 1f) * (size.width - pad * 2), pad + (1f - ((v - mn) / rng).toFloat()) * (size.height - pad * 2))
        }
        val anim = pts.take((pts.size * p).toInt().coerceAtLeast(2))
        // Gradient fill
        val fill = Path().apply {
            moveTo(anim.first().x, size.height)
            lineTo(anim.first().x, anim.first().y)
            anim.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(anim.last().x, size.height); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(.4f), color.copy(.05f), Color.Transparent)))
        // Line
        val line = Path().apply { moveTo(anim.first().x, anim.first().y); anim.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // End dot with glow
        drawCircle(color.copy(.25f), 14.dp.toPx(), anim.last())
        drawCircle(color.copy(.5f), 8.dp.toPx(), anim.last())
        drawCircle(color, 5.dp.toPx(), anim.last())
    }
}

// ── Donut chart data ─────────────────────────────────────────────────────────
data class DonutSeg(val label: String, val emoji: String, val amount: Double, val pct: Float, val color: Color)

@Composable
fun DonutChart(segs: List<DonutSeg>, modifier: Modifier = Modifier) {
    var sel by remember { mutableIntStateOf(-1) }
    val ap by animateFloatAsState(1f, tween(900, easing = EaseOutCubic), label = "d")
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clickable { sel = -1 }) {
            val R = size.minDimension / 2 * 0.85f; val sw = R * 0.22f; val cx = size.width/2; val cy = size.height/2
            drawCircle(Color(0x20888888), R, Offset(cx, cy), style = Stroke(sw))
            var cum = 0f
            segs.forEachIndexed { i, s ->
                drawArc(s.color, -90f + cum * 360f * ap, s.pct * 360f * ap, false,
                    style = Stroke(if (sel == i) sw * 1.3f else sw, cap = StrokeCap.Butt),
                    topLeft = Offset(cx - R, cy - R), size = Size(R*2, R*2),
                    alpha = if (sel != -1 && sel != i) .35f else 1f)
                cum += s.pct
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (sel >= 0 && sel < segs.size) segs[sel].emoji else "💸", fontSize = 22.sp)
            if (sel >= 0 && sel < segs.size) Text("${(segs[sel].pct*100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = segs[sel].color)
        }
    }
}

// ── Commitment Ring ───────────────────────────────────────────────────────────
@Composable
private fun CommitmentRing(label: String, monthly: Double, remaining: Double?, color: Color, modifier: Modifier = Modifier) {
    val progress = if (remaining != null && remaining > 0) {
        val paid = (monthly * 12).let { if (it > 0) it else 1.0 }
        ((paid - remaining) / paid).coerceIn(0.0, 1.0).toFloat()
    } else 1f
    val ap by animateFloatAsState(progress, tween(900, easing = EaseOutCubic), label = "r")

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val R = size.minDimension / 2 - 8.dp.toPx(); val sw = 10.dp.toPx()
                val cx = size.width/2; val cy = size.height/2
                drawCircle(color.copy(.15f), R, Offset(cx, cy), style = Stroke(sw))
                if (ap > 0) drawArc(color, -90f, 360f * ap, false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = Offset(cx-R, cy-R), size = Size(R*2, R*2))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatMXN(monthly).replace("MX$","$").replace(",",""), fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
                Text("/mes", fontSize = 8.sp, color = color.copy(.6f))
            }
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        if (remaining != null && remaining > 0)
            Text("Restante: ${formatMXN(remaining).replace("MX$","$")}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val ym = currentYearMonth()
    val firstName = state.userName.trim().split(" ").firstOrNull()?.takeIf { it.isNotEmpty() } ?: ""
    val initials = state.userName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        .ifEmpty { "M" }

    val prevYM = run { val p=ym.split("-"); val y=p[0].toInt(); val m=p[1].toInt(); if(m==1)"${y-1}-12" else "$y-${String.format("%02d",m-1)}" }
    val mTxs = remember(state.transactions, ym) { state.transactions.filter { it.date.startsWith(ym) } }
    val pTxs = remember(state.transactions, prevYM) { state.transactions.filter { it.date.startsWith(prevYM) } }

    val inc  = mTxs.filter { it.type=="income"  }.sumOf { it.amount }
    val exp  = mTxs.filter { it.type=="expense" }.sumOf { it.amount }
    val pExp = pTxs.filter { it.type=="expense" }.sumOf { it.amount }
    val expDelta = if (pExp>0) ((exp-pExp)/pExp*100).toInt() else 0
    val savingRate = if (inc>0) ((inc-exp)/inc*100).toInt() else 0

    val total = state.accounts.sumOf { it.balance }
    // MSI: only count plans that haven't been paid this month yet
    val msiC  = state.msiPlans.filter { it.status == "active" && it.payments.none { p -> p.date.startsWith(ym) } }.sumOf { it.monthlyPayment }
    // Only count UNPAID subscriptions this month as pending commitments
    val unpaidSubC = state.subscriptions.filter { it.active && it.lastPaidMonth != ym }.sumOf { it.amount }
    val subC = unpaidSubC  // total remaining to pay this month
    val avail = total - msiC - subC
    val totalMsiDebt = state.msiPlans.filter { it.status=="active" }.sumOf { (it.months-it.paidMonths)*it.monthlyPayment }
    val loansPending = state.loans.filter { it.status=="pending" }.sumOf { it.amount }

    val spark = remember(state.transactions, total) {
        val sorted = state.transactions.sortedBy { it.date }.takeLast(12)
        var b = total; val v = mutableListOf(b)
        for (tx in sorted.reversed()) { b -= if (tx.type=="income") tx.amount else -tx.amount; v.add(0,b) }
        v.takeLast(8)
    }
    val donutSegs = remember(mTxs, exp) {
        if (exp==0.0) emptyList()
        else mTxs.filter { it.type=="expense" }.groupBy { it.category }
            .map { (cat,txs) -> val amt=txs.sumOf{it.amount}; DonutSeg(getCategoryMeta(cat).name, getCategoryMeta(cat).emoji, amt, (amt/exp).toFloat(), categoryColor(cat)) }
            .sortedByDescending { it.pct }.take(5)
    }
    val last6 = remember(state.transactions) {
        (0..5).map { ago ->
            val c=Calendar.getInstance(); c.add(Calendar.MONTH,-ago)
            val ym2="${c.get(Calendar.YEAR)}-${(c.get(Calendar.MONTH)+1).toString().padStart(2,'0')}"
            val txs=state.transactions.filter{it.date.startsWith(ym2)}
            ym2 to (txs.filter{it.type=="income"}.sumOf{it.amount} to txs.filter{it.type=="expense"}.sumOf{it.amount})
        }.reversed()
    }

    val overdueLoans = remember(state.loans) { state.loans.filter { it.status=="pending" && daysUntilDate(it.expectedReturnDate)<0 } }
    val pendingSubs  = state.subscriptions.filter { it.active && it.lastPaidMonth!=ym }
    val activeMSI    = state.msiPlans.filter { it.status=="active" }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {

        // ── HERO CARD — Balance (header handled by Navigation TopBar) ─────────
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0F1926), Color(0xFF132030)))).padding(horizontal=20.dp, vertical=20.dp)) {
            Column {
                // Subscriptions status badge (if some already paid)
                val paidSubsCount = state.subscriptions.count { it.active && it.lastPaidMonth == ym }
                val totalSubsCount = state.subscriptions.count { it.active }
                if (paidSubsCount > 0 && totalSubsCount > 0) {
                    Row(modifier=Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(GreenBrand.copy(.15f)).padding(horizontal=10.dp, vertical=4.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint=GreenBrand, modifier=Modifier.size(12.dp))
                        Text("$paidSubsCount/$totalSubsCount suscripciones pagadas este mes", fontSize=10.sp, color=GreenBrand, fontWeight=FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(4.dp))

                // Balance — premium layout with full-width sparkline
                Text("SALDO TOTAL", fontSize=9.sp, fontWeight=FontWeight.Bold, color=Color(0xFF4A5568), letterSpacing=1.5.sp)
                Spacer(Modifier.height(8.dp))
                AnimatedMoney(total, Color.White, 48f)
                Spacer(Modifier.height(12.dp))
                if (spark.size >= 2) {
                    SparklineChart(
                        values = spark,
                        color = if (avail >= 0) GreenBrand else RedBrand,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Two pills: Libre + Compromisos
                if ((msiC+subC)>0 || avail!=total) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        // LIBRE pill
                        Row(modifier=Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(.1f)).padding(horizontal=14.dp,vertical=8.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(if(avail>=0) GreenBrand else RedBrand))
                            Text("LIBRE", fontSize=9.sp, color=Color.White.copy(.6f), fontWeight=FontWeight.Bold)
                            Text(formatMXN(avail).replace("MX$","$"), fontSize=13.sp, fontWeight=FontWeight.Black, color=if(avail>=0) GreenBrand else RedBrand)
                        }
                        // PAGOS/MES pill
                        if ((msiC+subC)>0)
                        Row(modifier=Modifier.clip(RoundedCornerShape(50)).background(AmberBrand.copy(.15f)).padding(horizontal=14.dp,vertical=8.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(AmberBrand))
                            Text("PAGOS/MES", fontSize=9.sp, color=AmberBrand.copy(.8f), fontWeight=FontWeight.Bold)
                            Text(formatMXN(msiC+subC).replace("MX$","$"), fontSize=13.sp, fontWeight=FontWeight.Black, color=AmberBrand)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── MONTHLY COMMITMENTS MODULE — Visual rings, NO math ───────────────
        if ((msiC+subC)>0) {
            Surface(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp), shape=RoundedCornerShape(24.dp), color=MaterialTheme.colorScheme.surface, shadowElevation=2.dp) {
                Column(modifier=Modifier.padding(18.dp)) {
                    Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                        Text("COMPROMISOS DEL MES", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape=RoundedCornerShape(50), color=AmberBrand.copy(.1f)) {
                            Text(formatMXN(msiC+subC).replace("MX$","$"), modifier=Modifier.padding(horizontal=10.dp,vertical=4.dp), fontSize=12.sp, fontWeight=FontWeight.Black, color=AmberBrand)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceAround) {
                        if (msiC>0) CommitmentRing("MSI", msiC, totalMsiDebt, BlueBrand, Modifier.weight(1f))
                        if (subC>0) CommitmentRing("Suscripciones", subC, null, PurpleBrand, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Divider(color=MaterialTheme.colorScheme.outline.copy(.2f))
                    Spacer(Modifier.height(10.dp))
                    // Available row
                    Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                        Column {
                            Text("💰 Disponible real", style=MaterialTheme.typography.bodyMedium, fontWeight=FontWeight.SemiBold, color=MaterialTheme.colorScheme.onSurface)
                            Text("Saldo − compromisos del mes", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatMXN(avail), style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.ExtraBold, color=if(avail>=0) GreenBrand else RedBrand)
                    }
                }
            }
        }

        // ── STATS: Este mes ──────────────────────────────────────────────────
        Row(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=10.dp), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Surface(modifier=Modifier.weight(1f), shape=RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surface, shadowElevation=1.dp) {
                Column(modifier=Modifier.padding(14.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0xFF10B981).copy(.15f)), contentAlignment=Alignment.Center) { Icon(Icons.Default.TrendingUp, null, tint=Color(0xFF10B981), modifier=Modifier.size(14.dp)) }
                        Spacer(Modifier.width(5.dp))
                        Text("INGRESOS", style=MaterialTheme.typography.labelSmall, color=Color(0xFF10B981).copy(.7f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(formatMXN(inc), fontWeight=FontWeight.ExtraBold, fontSize=17.sp, color=Color(0xFF10B981))
                }
            }
            Surface(modifier=Modifier.weight(1f), shape=RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surface, shadowElevation=1.dp) {
                Column(modifier=Modifier.padding(14.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(.15f)), contentAlignment=Alignment.Center) { Icon(Icons.Default.TrendingDown, null, tint=Color(0xFFEF4444), modifier=Modifier.size(14.dp)) }
                        Spacer(Modifier.width(5.dp))
                        Column {
                            Text("GASTOS", style=MaterialTheme.typography.labelSmall, color=Color(0xFFEF4444).copy(.7f))
                            if (pExp>0) Text("${if(expDelta>0)"▲" else "▼"}${abs(expDelta)}%", fontSize=9.sp, color=if(expDelta>0) Color(0xFFEF4444) else Color(0xFF10B981), fontWeight=FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(formatMXN(exp), fontWeight=FontWeight.ExtraBold, fontSize=17.sp, color=Color(0xFFEF4444))
                }
            }
            Surface(modifier=Modifier.weight(1f), shape=RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surface, shadowElevation=1.dp) {
                Column(modifier=Modifier.padding(14.dp)) {
                    Text("AHORRO", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("$savingRate%", fontWeight=FontWeight.ExtraBold, fontSize=17.sp, color=if(savingRate>=0) Color(0xFF10B981) else Color(0xFFEF4444))
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress={abs(savingRate).coerceIn(0,100)/100f}, modifier=Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color=if(savingRate>=0) Color(0xFF10B981) else Color(0xFFEF4444), trackColor=MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }

        // ── 6-MONTH TREND ────────────────────────────────────────────────────
        Surface(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=4.dp), shape=RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surface, shadowElevation=1.dp) {
            Column(modifier=Modifier.padding(16.dp)) {
                Text("TENDENCIA 6 MESES", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(bottom=10.dp))
                MonthlyBarChart(months=last6)
            }
        }

        // ── SPENDING DONUT ───────────────────────────────────────────────────
        if (donutSegs.isNotEmpty()) {
            Surface(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=4.dp), shape=RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surface, shadowElevation=1.dp) {
                Column(modifier=Modifier.padding(18.dp)) {
                    Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                        Text("GASTOS DEL MES", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatMXN(exp), fontWeight=FontWeight.Bold, color=Color(0xFFEF4444), style=MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        DonutChart(segs=donutSegs, modifier=Modifier.size(160.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(modifier=Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            donutSegs.take(4).forEach { s ->
                                Row(modifier=Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                                    Text(s.emoji, fontSize=15.sp, modifier=Modifier.width(24.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(s.label, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurface, fontWeight=FontWeight.SemiBold)
                                        LinearProgressIndicator(progress={s.pct}, modifier=Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color=s.color, trackColor=MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("${(s.pct*100).toInt()}%", fontSize=10.sp, fontWeight=FontWeight.Bold, color=s.color)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── ALERTS ───────────────────────────────────────────────────────────
        if (overdueLoans.isNotEmpty() || pendingSubs.isNotEmpty() || activeMSI.isNotEmpty()) {
            Column(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=4.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("PENDIENTE", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(start=4.dp))
                if (overdueLoans.isNotEmpty()) AlertRow(Icons.Default.Warning, RedBrand, "${overdueLoans.size} préstamo${if(overdueLoans.size>1)"s" else ""} vencido${if(overdueLoans.size>1)"s" else ""}", "${formatMXN(overdueLoans.sumOf{it.amount})} sin cobrar", urgent=true)
                if (pendingSubs.isNotEmpty()) AlertRow(Icons.Default.Repeat, PurpleBrand, "${pendingSubs.size} suscripción${if(pendingSubs.size>1)"es" else ""}", formatMXN(subC)+"/mes")
                if (activeMSI.isNotEmpty()) AlertRow(Icons.Default.Autorenew, BlueBrand, "${activeMSI.size} MSI activo${if(activeMSI.size>1)"s" else ""}", formatMXN(msiC)+"/mes")
            }
        }

        // ── LONG-TERM DEBT — Isolated module ─────────────────────────────────
        if (totalMsiDebt>0 || loansPending>0) {
            Surface(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=4.dp), shape=RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surfaceVariant, border=BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(.2f))) {
                Column(modifier=Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Schedule, null, tint=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.size(14.dp))
                        Text("DEUDAS A FUTURO", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("No afectan tu saldo de hoy • Se pagan en meses futuros", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant.copy(.7f))
                    if (totalMsiDebt>0) FinanceRow("Deuda total MSI restante", formatMXN(totalMsiDebt), BlueBrand)
                    if (loansPending>0) FinanceRow("Préstamos pendientes de cobro", formatMXN(loansPending), AmberBrand)
                }
            }
        }

        // ── ACCOUNTS ─────────────────────────────────────────────────────────
        if (state.accounts.isNotEmpty()) {
            Column(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=4.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("MIS CUENTAS", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(start=4.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    state.accounts.take(4).forEach { acc ->
                        Surface(modifier=Modifier.weight(1f), shape=RoundedCornerShape(18.dp), color=MaterialTheme.colorScheme.surface, shadowElevation=1.dp) {
                            Column(modifier=Modifier.padding(12.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(parseHex(acc.color))); Spacer(Modifier.width(4.dp)); Text(acc.name, fontSize=10.sp, color=MaterialTheme.colorScheme.onSurfaceVariant, maxLines=1) }
                                Spacer(Modifier.height(6.dp))
                                Text(formatMXN(acc.balance), fontWeight=FontWeight.ExtraBold, fontSize=14.sp, color=MaterialTheme.colorScheme.onSurface)
                                Text(if(acc.type=="cash")"Efectivo" else "Débito", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant, fontSize=9.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── RECENT TRANSACTIONS ──────────────────────────────────────────────
        if (state.transactions.isNotEmpty()) {
            Column(modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=4.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("RECIENTES", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(start=4.dp))
                Surface(shape=RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surface, shadowElevation=1.dp) {
                    Column(modifier=Modifier.fillMaxWidth()) {
                        state.transactions.sortedByDescending { it.date }.take(5).forEachIndexed { i, tx ->
                            val acc=state.accounts.find{it.id==tx.accountId}; val meta=getCategoryMeta(tx.category)
                            Row(modifier=Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.width(3.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(categoryColor(tx.category)))
                                Spacer(Modifier.width(10.dp))
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(categoryColor(tx.category).copy(.12f)), contentAlignment=Alignment.Center) { Text(meta.emoji, fontSize=18.sp) }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(tx.description, fontWeight=FontWeight.SemiBold, fontSize=13.sp, color=MaterialTheme.colorScheme.onSurface)
                                    Text("${tx.date.substring(8)} ${monthLabel(tx.date.substring(0,7))} · ${acc?.name?:""}", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${if(tx.type=="income")"+" else "−"}${formatMXN(tx.amount)}", fontWeight=FontWeight.ExtraBold, fontSize=14.sp, color=if(tx.type=="income") Color(0xFF10B981) else Color(0xFFEF4444))
                            }
                            if (i<state.transactions.take(5).size-1) Divider(modifier=Modifier.padding(horizontal=14.dp), color=MaterialTheme.colorScheme.outline.copy(.2f))
                        }
                    }
                }
            }
        }

        if (state.accounts.isEmpty()) EmptyState("🚀","¡Bienvenido a Monet!","Toca la 👜 para crear tu primera cuenta")
    }
}

fun getGreeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when { h<12->"Buenos días"; h<19->"Buenas tardes"; else->"Buenas noches" }
}

@Composable
private fun AlertRow(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, title: String, sub: String, urgent: Boolean=false) {
    val inf = rememberInfiniteTransition(label="pulse")
    val alpha by if (urgent) inf.animateFloat(0.6f,1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label="a") else remember{mutableFloatStateOf(1f)}.let{ derivedStateOf{it.floatValue} }.let{ rememberUpdatedState(1f) }
    Surface(shape=RoundedCornerShape(14.dp), color=color.copy(.1f), modifier=Modifier.fillMaxWidth().graphicsLayer{this.alpha=if(urgent)alpha else 1f}) {
        Row(modifier=Modifier.padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(.2f)), contentAlignment=Alignment.Center) { Icon(icon, null, tint=color, modifier=Modifier.size(16.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight=FontWeight.Bold, fontSize=13.sp, color=color); Text(sub, fontSize=11.sp, color=color.copy(.7f)) }
            Icon(Icons.Default.ChevronRight, null, tint=color.copy(.5f), modifier=Modifier.size(14.dp))
        }
    }
}

@Composable
private fun MiniCommitCard(label: String, value: String, color: Color, modifier: Modifier=Modifier) {
    Surface(modifier=modifier, shape=RoundedCornerShape(14.dp), color=color.copy(.1f), border=BorderStroke(1.dp,color.copy(.2f))) {
        Column(modifier=Modifier.padding(10.dp)) { Text(label, fontSize=10.sp, color=color.copy(.7f), fontWeight=FontWeight.Bold); Spacer(Modifier.height(2.dp)); Text(value, fontSize=14.sp, fontWeight=FontWeight.ExtraBold, color=color) }
    }
}

@Composable
private fun FinanceRow(label: String, value: String, color: Color, bold: Boolean=false) {
    Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
        Text(label, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant, fontWeight=if(bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, style=MaterialTheme.typography.bodyMedium, color=color, fontWeight=if(bold) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}
