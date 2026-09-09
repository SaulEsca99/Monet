package com.mifinanza.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.DatePickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifinanza.app.ui.theme.*
import kotlin.math.PI

// ── Swipe delete background ───────────────────────────────────────────────────
@Composable
fun SwipeDeleteBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
        .background(Color(0xFFEF4444)).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text("Borrar", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Circular / Donut Section Header ──────────────────────────────────────────

@Composable
fun CircularSectionHeader(
    label: String,
    mainValue: String,
    progress: Float,           // 0.0–1.0 for the arc
    accentColor: Color,
    stat1Label: String? = null,
    stat1Value: String? = null,
    stat2Label: String? = null,
    stat2Value: String? = null,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "scale"
    )
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(accentColor.copy(.1f))
            .clickable { pressed = !pressed; pressed = false }
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Donut chart ────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val inset = strokeWidth / 2
                    // Background ring
                    drawArc(
                        color = accentColor.copy(.18f),
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth)
                    )
                    // Progress arc
                    if (animProgress > 0f) {
                        drawArc(
                            color = accentColor,
                            startAngle = -90f, sweepAngle = 360f * animProgress,
                            useCenter = false,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round),
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth)
                        )
                    }
                }
                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        mainValue.replace("MX$", "$").replace(",", ","),
                        fontSize = if (mainValue.length > 8) 13.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        maxLines = 1
                    )
                    Text(
                        "${(animProgress * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = accentColor.copy(.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Stats ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(.7f),
                    letterSpacing = 0.8.sp
                )
                if (stat1Label != null && stat1Value != null) {
                    StatRow(stat1Label, stat1Value, accentColor)
                }
                if (stat2Label != null && stat2Value != null) {
                    StatRow(stat2Label, stat2Value, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Standard Section Header — visual colored card ─────────────────────────────
@Composable
fun SectionHeader(
    label: String,
    value: String,
    accentColor: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(accentColor.copy(.18f), accentColor.copy(.06f))
                )
            )
            .border(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(accentColor.copy(.25f), accentColor.copy(.08f))
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(.75f),
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                letterSpacing = (-1).sp
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Material 3 Date Field — beautiful calendar picker ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    // Parse current value to pre-select in picker
    val initialMillis = remember(value) {
        if (value.isEmpty()) System.currentTimeMillis()
        else try {
            val parts = value.split("-")
            val cal = Calendar.getInstance()
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
            cal.timeInMillis
        } catch (e: Exception) { System.currentTimeMillis() }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    val display = if (value.isEmpty()) "" else try {
        val p = value.split("-"); "${p[2]}/${p[1]}/${p[0]}"
    } catch (e: Exception) { value }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            enabled = false,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = accent, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledTrailingIconColor = accent
            )
        )
        Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
    }

    // Material 3 DatePickerDialog — much more beautiful than Android native
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = millis
                        onDateSelected("${cal.get(Calendar.YEAR)}-${(cal.get(Calendar.MONTH)+1).toString().padStart(2,'0')}-${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0')}")
                    }
                    showPicker = false
                }) { Text("Confirmar", fontWeight = FontWeight.Bold, color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = accent,
                headlineContentColor = accent,
                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedDayContainerColor = accent,
                todayContentColor = accent,
                todayDateBorderColor = accent
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = accent,
                    headlineContentColor = accent,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = accent,
                    todayContentColor = accent,
                    todayDateBorderColor = accent
                )
            )
        }
    }
}

// ── Standard field colors (use for ALL forms) ─────────────────────────────────
@Composable
fun appFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = accent,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = accent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
)

// ── App Card ─────────────────────────────────────────────────────────────────

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.surface else color
    val shape = RoundedCornerShape(20.dp)
    val base = Modifier.fillMaxWidth().clip(shape).background(bgColor).then(modifier)

    if (onClick != null) {
        Column(modifier = base.clickable(onClick = onClick).padding(16.dp), content = content)
    } else {
        Column(modifier = base.padding(16.dp), content = content)
    }
}

// ── Stat chip ─────────────────────────────────────────────────────────────────

@Composable
fun StatChip(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ── Color dot ─────────────────────────────────────────────────────────────────

@Composable
fun ColorDot(hex: String, size: androidx.compose.ui.unit.Dp = 10.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(parseHex(hex))
    )
}

fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color.Gray }

// ── Tag badge ─────────────────────────────────────────────────────────────────

@Composable
fun TagBadge(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// ── Section title ─────────────────────────────────────────────────────────────

@Composable
fun SectionTitle(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
        action?.invoke()
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(.6f))
    }
}

// ── Primary button ────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = GreenBrand) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == GreenBrand) Color(0xFF021A12) else Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// ── FAB ───────────────────────────────────────────────────────────────────────

@Composable
fun AppFab(icon: @Composable () -> Unit, onClick: () -> Unit, color: Color = GreenBrand) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = color,
        contentColor = if (color == GreenBrand) Color(0xFF021A12) else Color.White,
        shape = CircleShape,
        modifier = Modifier.size(58.dp)
    ) {
        icon()
    }
}
