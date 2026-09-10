package com.mifinanza.app.data

import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ── Helpers ──────────────────────────────────────────────────────────────────

// Locale fijo — fechas consistentes en cualquier dispositivo
private val ISO_DATE   = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val ISO_MONTH  = SimpleDateFormat("yyyy-MM",    Locale.US)
private val ES_DISPLAY = Locale("es", "MX")

fun generateId() = System.currentTimeMillis().toString(36) + (0..99999).random().toString(36)

fun today(): String = ISO_DATE.format(Date())

fun currentYearMonth(): String = ISO_MONTH.format(Date())

fun formatMXN(amount: Double): String =
    NumberFormat.getCurrencyInstance(ES_DISPLAY).format(amount)

fun monthLabel(ym: String): String {
    return try {
        val cal = Calendar.getInstance().apply { time = SimpleDateFormat("yyyy-MM", Locale.US).parse("$ym-01")!! }
        SimpleDateFormat("MMM", ES_DISPLAY).format(cal.time).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) { ym }
}

// Medianoche de hoy — base para todos los cálculos de días
private fun todayMidnight(): Calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
}

// Días hasta el próximo cobro (mes actual si aún no ha pasado, si no mes siguiente)
fun daysUntilDay(day: Int): Int {
    val today = todayMidnight()
    val target = todayMidnight().apply {
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
        // Si el día ya pasó (o es hoy y queremos el próximo), mover al mes siguiente
        if (get(Calendar.DAY_OF_MONTH) < today.get(Calendar.DAY_OF_MONTH)) {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
    }
    return ((target.timeInMillis - today.timeInMillis) / 86400000).toInt().coerceAtLeast(0)
}

// Siempre cuenta al MES SIGUIENTE (cuando ya pagaste este mes)
fun daysUntilNextMonthDay(day: Int): Int {
    val today = todayMidnight()
    val target = todayMidnight().apply {
        add(Calendar.MONTH, 1)
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
    }
    return ((target.timeInMillis - today.timeInMillis) / 86400000).toInt().coerceAtLeast(1)
}

// Días hasta una fecha específica "yyyy-MM-dd" — comparación por día, sin hora
fun daysUntilDate(dateStr: String): Int {
    return try {
        val target = todayMidnight().apply { time = ISO_DATE.parse(dateStr)!!
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val today = todayMidnight()
        ((target.timeInMillis - today.timeInMillis) / 86400000).toInt()
    } catch (e: Exception) { 0 }
}

// Etiqueta "22 Oct" para el próximo mes
fun nextMonthLabel(day: Int): String {
    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
    val month = SimpleDateFormat("MMM", ES_DISPLAY).format(cal.time).replaceFirstChar { it.uppercase() }
    return "${day.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH))} $month"
}

// Etiqueta "22 Sep" para el mes actual
fun currentMonthLabel(day: Int): String {
    val cal = Calendar.getInstance()
    val month = SimpleDateFormat("MMM", ES_DISPLAY).format(cal.time).replaceFirstChar { it.uppercase() }
    return "${day.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH))} $month"
}

fun formatDate(dateStr: String): String {
    return try {
        SimpleDateFormat("d MMM yyyy", ES_DISPLAY).format(ISO_DATE.parse(dateStr)!!)
    } catch (e: Exception) { dateStr }
}

// ── Data models ───────────────────────────────────────────────────────────────

@Serializable
data class Account(
    val id: String = generateId(),
    val name: String,
    val type: String = "debit",  // "cash" | "debit"
    val balance: Double,
    val color: String = "#6366F1",
    val createdAt: String = today()
)

@Serializable
data class Transaction(
    val id: String = generateId(),
    val accountId: String,
    val type: String,             // "income" | "expense"
    val amount: Double,
    val description: String,
    val category: String = "other",
    val date: String = today(),
    val time: String = SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date()),
    val source: String = "manual" // "manual" | "subscription" | "msi"
)

@Serializable
data class SubscriptionPayment(
    val id: String = generateId(),
    val month: String,
    val amount: Double,
    val date: String = today(),
    val accountId: String
)

@Serializable
data class Subscription(
    val id: String = generateId(),
    val name: String,
    val amount: Double,
    val billingDay: Int,
    val active: Boolean = true,
    val color: String = "#6366F1",
    val createdMonth: String = currentYearMonth(),
    val lastPaidMonth: String = "",
    val payments: List<SubscriptionPayment> = emptyList()
)

@Serializable
data class MsiPayment(
    val id: String = generateId(),
    val month: Int,
    val amount: Double,
    val date: String = today(),
    val type: String = "monthly",  // "monthly" | "liquidated"
    val accountId: String
)

@Serializable
data class MsiPlan(
    val id: String = generateId(),
    val name: String,
    val description: String = "",
    val totalAmount: Double,
    val months: Int,
    val monthlyPayment: Double,
    val paymentDay: Int,
    val startDate: String = today(),
    val initialPaidMonths: Int = 0,
    val paidMonths: Int = 0,
    val status: String = "active",  // "active" | "paid_off"
    val payments: List<MsiPayment> = emptyList()
)

@Serializable
data class Loan(
    val id: String = generateId(),
    val personName: String,
    val amount: Double,
    val description: String = "",
    val givenDate: String = today(),
    val expectedReturnDate: String,
    val status: String = "pending",  // "pending" | "paid"
    val paidDate: String = "",
    val createdAt: String = today()
)

@Serializable
data class AppState(
    val userName: String = "",
    val darkMode: Boolean = false,
    val customIncomeTypes: List<CustomIncomeType> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val msiPlans: List<MsiPlan> = emptyList(),
    val loans: List<Loan> = emptyList()
)

// ── Custom income type ────────────────────────────────────────────────────────

@Serializable
data class CustomIncomeType(
    val id: String = generateId(),
    val name: String,
    val emoji: String = "💵",
    val createdAt: String = today()
)

// ── Category meta ─────────────────────────────────────────────────────────────

data class CategoryMeta(val id: String, val name: String, val emoji: String, val color: Long)

val CATEGORIES = listOf(
    CategoryMeta("food",           "Comida",          "🍔", 0xFFF97316),
    CategoryMeta("transport",      "Transporte",      "🚗", 0xFF3B82F6),
    CategoryMeta("entertainment",  "Entretenimiento", "🎮", 0xFF8B5CF6),
    CategoryMeta("health",         "Salud",           "💊", 0xFFEF4444),
    CategoryMeta("clothing",       "Ropa",            "👕", 0xFFEC4899),
    CategoryMeta("tech",           "Tecnología",      "💻", 0xFF06B6D4),
    CategoryMeta("home",           "Hogar",           "🏠", 0xFF22C55E),
    CategoryMeta("subscriptions",  "Suscripciones",   "📱", 0xFF6366F1),
    CategoryMeta("salary",         "Sueldo/Ingresos", "💰", 0xFF10B981),
    CategoryMeta("other",          "Otros",           "📌", 0xFF94A3B8),
)

fun getCategoryMeta(id: String) = CATEGORIES.find { it.id == id } ?: CATEGORIES.last()

val ACCOUNT_COLORS = listOf(
    "#6366F1", "#10B981", "#3B82F6", "#F59E0B",
    "#EF4444", "#8B5CF6", "#06B6D4", "#F97316"
)
