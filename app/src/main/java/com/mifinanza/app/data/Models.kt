package com.mifinanza.app.data

import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ── Helpers ──────────────────────────────────────────────────────────────────

fun generateId() = System.currentTimeMillis().toString(36) + (0..99999).random().toString(36)

fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

fun currentYearMonth(): String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

fun formatMXN(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "MX")).format(amount)

fun monthLabel(ym: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale("es", "MX"))
        val cal = Calendar.getInstance().apply { time = sdf.parse("$ym-01")!! }
        val month = SimpleDateFormat("MMM", Locale("es", "MX")).format(cal.time)
        month.replaceFirstChar { it.uppercase() }
    } catch (e: Exception) { ym }
}

fun daysUntilDay(day: Int): Int {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, day)
        if (get(Calendar.DAY_OF_MONTH) <= now.get(Calendar.DAY_OF_MONTH)) {
            add(Calendar.MONTH, 1)
        }
    }
    val diff = target.timeInMillis - now.timeInMillis
    return (diff / 86400000).toInt()
}

fun daysUntilDate(dateStr: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val target = sdf.parse(dateStr) ?: return 0
        val diff = target.time - Date().time
        (diff / 86400000).toInt()
    } catch (e: Exception) { 0 }
}

fun formatDate(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val out = SimpleDateFormat("d MMM yyyy", Locale("es", "MX"))
        out.format(sdf.parse(dateStr)!!)
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
