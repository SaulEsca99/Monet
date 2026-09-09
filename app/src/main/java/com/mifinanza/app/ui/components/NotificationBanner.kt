package com.mifinanza.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mifinanza.app.data.*
import com.mifinanza.app.ui.theme.*
import kotlinx.coroutines.delay

// ── In-App Notification Model ─────────────────────────────────────────────────

enum class NotifType { SUCCESS, WARNING, INFO, DANGER }

data class AppNotification(
    val id: String = generateId(),
    val type: NotifType,
    val title: String,
    val message: String,
    val autoDismiss: Boolean = true
)

// ── Global notification state ─────────────────────────────────────────────────

object NotificationQueue {
    val active = mutableStateListOf<AppNotification>()

    fun show(notif: AppNotification) {
        // Prevent duplicate
        if (active.none { it.title == notif.title && it.message == notif.message }) {
            active.add(0, notif) // newest first
            if (active.size > 3) active.removeLastOrNull()
        }
    }

    fun dismiss(id: String) { active.removeAll { it.id == id } }
    fun dismissAll() { active.clear() }
}

// ── Notification Banner Overlay ───────────────────────────────────────────────

@Composable
fun NotificationOverlay() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NotificationQueue.active.toList().forEach { notif ->
            key(notif.id) {
                NotificationCard(notif = notif, onDismiss = { NotificationQueue.dismiss(notif.id) })
            }
        }
    }
}

@Composable
private fun NotificationCard(notif: AppNotification, onDismiss: () -> Unit) {
    val (color, icon) = when (notif.type) {
        NotifType.SUCCESS -> Color(0xFF10B981) to Icons.Default.CheckCircle
        NotifType.WARNING -> Color(0xFFF59E0B) to Icons.Default.Warning
        NotifType.DANGER  -> Color(0xFFEF4444) to Icons.Default.Error
        NotifType.INFO    -> Color(0xFF3B82F6) to Icons.Default.Info
    }

    LaunchedEffect(notif.id) {
        if (notif.autoDismiss) {
            delay(4000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { -40 } + fadeIn(),
        exit = slideOutVertically { -40 } + fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(.12f),
            border = BorderStroke(1.dp, color.copy(.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color.copy(.2f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(notif.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                    Text(notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ── Notification triggers (call these from business logic) ────────────────────

fun notifySubscriptionPaid(name: String, amount: Double) = NotificationQueue.show(
    AppNotification(type = NotifType.SUCCESS, title = "✓ Pago confirmado", message = "$name · ${formatMXN(amount)} registrado este mes")
)

fun notifyUpcomingMSI(planName: String, days: Int, amount: Double) = NotificationQueue.show(
    AppNotification(type = NotifType.WARNING, title = "⏰ MSI próximo cobro", message = "$planName · ${formatMXN(amount)} en $days días", autoDismiss = false)
)

fun notifyUpcomingSubscription(name: String, days: Int) = NotificationQueue.show(
    AppNotification(type = NotifType.INFO, title = "📅 Suscripción próxima", message = "$name se cobra en $days días")
)

fun notifyOverdueLoan(personName: String, amount: Double) = NotificationQueue.show(
    AppNotification(type = NotifType.DANGER, title = "⚠️ Préstamo vencido", message = "$personName te debe ${formatMXN(amount)}", autoDismiss = false)
)

fun notifyTransactionAdded(type: String, amount: Double) = NotificationQueue.show(
    AppNotification(type = NotifType.SUCCESS, title = if (type=="income") "↑ Ingreso registrado" else "↓ Gasto registrado", message = "${formatMXN(amount)} añadido correctamente")
)
