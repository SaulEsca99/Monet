package com.mifinanza.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifinanza.app.data.formatMXN
import com.mifinanza.app.ui.theme.*
import kotlin.math.*

// ── Animated Donut Chart ──────────────────────────────────────────────────────

data class DonutSegment(
    val label: String,
    val amount: Double,
    val color: Color,
    val emoji: String = "📌"
) {
    val percentage: Float get() = 0f  // computed externally
}

@Composable
fun DonutChart(
    segments: List<Pair<DonutSegment, Float>>,  // (segment, pct)
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 160.dp
) {
    var selected by remember { mutableIntStateOf(-1) }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "donut"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(segments) {
                    detectTapGestures { offset ->
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        val angle = Math.toDegrees(
                            atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                        ).toFloat() + 90f
                        val normalizedAngle = if (angle < 0) angle + 360f else angle
                        var cum = 0f
                        segments.forEachIndexed { i, (_, pct) ->
                            val sweep = pct * 360f * animProgress
                            if (normalizedAngle >= cum && normalizedAngle < cum + sweep) {
                                selected = if (selected == i) -1 else i
                            }
                            cum += pct * 360f * animProgress
                        }
                    }
                }
        ) {
            val radius = this.size.minDimension / 2 * 0.82f
            val stroke = radius * 0.22f
            val cx = this.size.width / 2
            val cy = this.size.height / 2

            // Background ring
            drawCircle(
                color = SurfaceVar,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = stroke)
            )

            var startAngle = -90f
            segments.forEachIndexed { i, (seg, pct) ->
                val sweep = pct * 360f * animProgress
                val isSelected = selected == i
                drawArc(
                    color = seg.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(
                        width = if (isSelected) stroke * 1.25f else stroke,
                        cap = StrokeCap.Butt
                    ),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                    alpha = if (selected != -1 && !isSelected) 0.4f else 1f
                )
                startAngle += pct * 360f * animProgress
            }
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (selected >= 0 && selected < segments.size) {
                val (seg, pct) = segments[selected]
                Text(
                    text = "${(pct * 100).toInt()}%",
                    color = seg.color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = seg.emoji,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "💸",
                    fontSize = 22.sp
                )
            }
        }
    }
}

// ── Sparkline ─────────────────────────────────────────────────────────────────

@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) return
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "spark"
    )

    Canvas(modifier = modifier) {
        val mn = values.min()
        val mx = values.max()
        val range = (mx - mn).takeIf { it > 0 } ?: 1.0

        val pts = values.mapIndexed { i, v ->
            Offset(
                x = i / (values.size - 1f) * size.width,
                y = (1f - ((v - mn) / range).toFloat()) * size.height
            )
        }

        val animatedPts = pts.take((pts.size * animProgress).toInt().coerceAtLeast(2))

        // Fill area
        val path = Path().apply {
            moveTo(animatedPts.first().x, size.height)
            animatedPts.forEach { lineTo(it.x, it.y) }
            lineTo(animatedPts.last().x, size.height)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        // Line
        val linePath = Path().apply {
            moveTo(animatedPts.first().x, animatedPts.first().y)
            animatedPts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Last dot
        if (animatedPts.isNotEmpty()) {
            drawCircle(color = color, radius = 4.dp.toPx(), center = animatedPts.last())
            drawCircle(color = color.copy(.3f), radius = 7.dp.toPx(), center = animatedPts.last())
        }
    }
}

// ── Category Progress Bar ─────────────────────────────────────────────────────

@Composable
fun CategoryBar(
    emoji: String,
    label: String,
    amount: Double,
    pct: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(700, easing = EaseOutCubic),
        label = "bar"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 16.sp, modifier = Modifier.width(28.dp))
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = formatMXN(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(6.dp))
                Text(text = "${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        }
        Spacer(Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            // Background
            drawRoundRect(color = SurfaceVar, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            // Fill
            drawRoundRect(
                color = color,
                size = Size(size.width * animPct, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
    }
}

// ── Weekly Bar Chart (Mon–Sun daily spending) ────────────────────────────────

@Composable
fun WeeklyBarChart(
    days: List<Triple<String, Double, Double>>, // (dateLabel, income, expense)
    todayIdx: Int,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(days) {
        days.maxOfOrNull { maxOf(it.second, it.third) }.takeIf { it != null && it > 0 } ?: 1.0
    }
    var selectedIdx by remember { mutableIntStateOf(todayIdx) }
    val dayNames = listOf("Lun","Mar","Mié","Jue","Vie","Sáb","Dom")

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip always visible
        val sel = days.getOrNull(selectedIdx)
        if (sel != null) {
            val (label, inc, exp) = sel
            val balance = inc - exp
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedIdx == todayIdx) GreenBrand else MaterialTheme.colorScheme.onSurface)
                        if (selectedIdx == todayIdx) Surface(shape = RoundedCornerShape(50), color = GreenBrand.copy(.15f)) {
                            Text("Hoy", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), fontSize = 9.sp, color = GreenBrand, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (inc > 0) Text("↑${formatMXN(inc)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        if (exp > 0) Text("↓${formatMXN(exp)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        if (inc == 0.0 && exp == 0.0) Text("Sin movimientos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Bars
        Row(modifier = Modifier.fillMaxWidth().height(70.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            days.forEachIndexed { idx, (_, inc, exp) ->
                val isToday = idx == todayIdx
                val isSelected = idx == selectedIdx
                val incPct by animateFloatAsState((inc / maxVal).toFloat().coerceIn(0f, 1f), tween(600, idx * 50, EaseOutCubic), label = "wi$idx")
                val expPct by animateFloatAsState((exp / maxVal).toFloat().coerceIn(0f, 1f), tween(600, idx * 50 + 25, EaseOutCubic), label = "we$idx")
                val alpha = if (!isSelected && selectedIdx != -1) .4f else 1f

                Column(modifier = Modifier.weight(1f).fillMaxHeight().clickable { selectedIdx = idx },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (isToday) { Box(Modifier.size(4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(GreenBrand)); Spacer(Modifier.height(2.dp)) }
                    else Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                        if (inc > 0) Box(Modifier.weight(1f).fillMaxHeight(incPct.coerceAtLeast(0.03f)).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0xFF10B981).copy(alpha)))
                        else Box(Modifier.weight(1f).fillMaxHeight(0.03f).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0x0F10B981)))
                        Spacer(Modifier.width(2.dp))
                        if (exp > 0) Box(Modifier.weight(1f).fillMaxHeight(expPct.coerceAtLeast(0.03f)).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0xFFEF4444).copy(alpha)))
                        else Box(Modifier.weight(1f).fillMaxHeight(0.03f).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0x0FEF4444)))
                    }
                }
            }
        }

        // Day labels
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayNames.forEachIndexed { idx, name ->
                Text(name, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 9.sp,
                    color = when { idx == todayIdx -> GreenBrand; idx == selectedIdx -> MaterialTheme.colorScheme.onSurface; else -> MaterialTheme.colorScheme.onSurfaceVariant },
                    fontWeight = if (idx == todayIdx || idx == selectedIdx) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

// ── Monthly Bar Chart (6 months income vs expense) ───────────────────────────

@Composable
fun MonthlyBarChart(
    months: List<Pair<String, Pair<Double, Double>>>, // ym → (income, expense)
    modifier: Modifier = Modifier
) {
    val currentYM = remember { com.mifinanza.app.data.currentYearMonth() }
    val currentIdx = remember(months) { months.indexOfFirst { it.first == currentYM }.takeIf { it >= 0 } ?: (months.size - 1) }
    var selectedIdx by remember { mutableIntStateOf(currentIdx) }

    val maxVal = remember(months) {
        months.maxOfOrNull { maxOf(it.second.first, it.second.second) }.takeIf { it != null && it > 0 } ?: 1.0
    }

    // Tooltip data — always shows selected (defaults to current month)
    val (selYM, selData) = months.getOrNull(selectedIdx) ?: return
    val (selInc, selExp) = selData
    val selBalance = selInc - selExp

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Always-visible tooltip ────────────────────────────────────────────
        val monthName = try {
            val p = selYM.split("-")
            val cal = java.util.Calendar.getInstance().apply { set(p[0].toInt(), p[1].toInt()-1, 1) }
            java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("es","MX")).format(cal.time).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) { selYM }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        monthName,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (selectedIdx == currentIdx) GreenBrand else MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedIdx == currentIdx) {
                        Surface(shape = RoundedCornerShape(50), color = GreenBrand.copy(.15f)) {
                            Text("Este mes", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, color = GreenBrand, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("INGRESOS", fontSize = 8.sp, color = Color(0xFF10B981).copy(.7f), fontWeight = FontWeight.Bold)
                        Text(formatMXN(selInc), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GASTOS", fontSize = 8.sp, color = Color(0xFFEF4444).copy(.7f), fontWeight = FontWeight.Bold)
                        Text(formatMXN(selExp), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BALANCE", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(
                            formatMXN(selBalance), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (selBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Bars ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEachIndexed { idx, (ym, data) ->
                val (inc, exp) = data
                val isCurrent = ym == currentYM
                val isSelected = selectedIdx == idx
                val incPct by animateFloatAsState((inc / maxVal).toFloat().coerceIn(0f, 1f), tween(700, idx * 60, EaseOutCubic), label = "i$idx")
                val expPct by animateFloatAsState((exp / maxVal).toFloat().coerceIn(0f, 1f), tween(700, idx * 60 + 30, EaseOutCubic), label = "e$idx")

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { selectedIdx = idx },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Dot indicator for current/selected
                    if (isSelected) {
                        Box(Modifier.size(5.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (isCurrent) GreenBrand else MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(Modifier.height(3.dp))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val alpha = if (!isSelected && selectedIdx != -1) .45f else if (isCurrent) 1f else .8f
                        Box(Modifier.weight(1f).fillMaxHeight(incPct.coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (inc > 0) Color(0xFF10B981).copy(alpha) else Color(0x0F10B981)))
                        Spacer(Modifier.width(2.dp))
                        Box(Modifier.weight(1f).fillMaxHeight(expPct.coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (exp > 0) Color(0xFFEF4444).copy(alpha) else Color(0x0FEF4444)))
                    }
                }
            }
        }

        // Month labels
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            months.forEachIndexed { idx, (ym, _) ->
                val label = try {
                    val p = ym.split("-")
                    val cal = java.util.Calendar.getInstance().apply { set(p[0].toInt(), p[1].toInt()-1, 1) }
                    java.text.SimpleDateFormat("MMM", java.util.Locale("es","MX")).format(cal.time).replaceFirstChar { it.uppercase() }
                } catch (e: Exception) { ym.takeLast(2) }
                Text(
                    label, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center, fontSize = 9.sp,
                    color = when {
                        ym == currentYM -> GreenBrand
                        selectedIdx == idx -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (ym == currentYM || selectedIdx == idx) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Legend
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF10B981)))
                Spacer(Modifier.width(3.dp))
                Text("Ingresos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFEF4444)))
                Spacer(Modifier.width(3.dp))
                Text("Gastos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
