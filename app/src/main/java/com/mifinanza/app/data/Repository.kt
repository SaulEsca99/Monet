package com.mifinanza.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mifinanza")
private val STATE_KEY = stringPreferencesKey("app_state")

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class AppRepository(private val context: Context) {

    val appStateFlow: Flow<AppState> = context.dataStore.data.map { prefs ->
        val raw = prefs[STATE_KEY] ?: return@map AppState()
        try { json.decodeFromString<AppState>(raw) } catch (e: Exception) { AppState() }
    }

    suspend fun saveState(state: AppState) = save(state)

    private suspend fun save(state: AppState) {
        context.dataStore.edit { prefs ->
            prefs[STATE_KEY] = json.encodeToString(state)
        }
    }

    // ── User name ─────────────────────────────────────────────────────────────

    suspend fun setUserName(current: AppState, name: String): AppState =
        current.copy(userName = name).also { save(it) }

    // ── Accounts ──────────────────────────────────────────────────────────────

    suspend fun addAccount(current: AppState, account: Account): AppState =
        current.copy(accounts = current.accounts + account).also { save(it) }

    // Add multiple accounts atomically (fixes race condition in setup)
    suspend fun addMultipleAccounts(current: AppState, accounts: List<Account>): AppState =
        current.copy(accounts = current.accounts + accounts).also { save(it) }

    suspend fun updateAccount(current: AppState, account: Account): AppState =
        current.copy(accounts = current.accounts.map { if (it.id == account.id) account else it }).also { save(it) }

    suspend fun deleteAccount(current: AppState, id: String): AppState =
        current.copy(
            accounts = current.accounts.filter { it.id != id },
            transactions = current.transactions.filter { it.accountId != id }
        ).also { save(it) }

    // ── Transactions ──────────────────────────────────────────────────────────

    suspend fun addTransaction(current: AppState, tx: Transaction): AppState {
        val delta = if (tx.type == "income") tx.amount else -tx.amount
        val newAccounts = current.accounts.map {
            if (it.id == tx.accountId) it.copy(balance = it.balance + delta) else it
        }
        return current.copy(
            accounts = newAccounts,
            transactions = listOf(tx) + current.transactions
        ).also { save(it) }
    }

    suspend fun deleteTransaction(current: AppState, id: String): AppState {
        val tx = current.transactions.find { it.id == id } ?: return current
        val delta = if (tx.type == "income") -tx.amount else tx.amount
        val newAccounts = current.accounts.map {
            if (it.id == tx.accountId) it.copy(balance = it.balance + delta) else it
        }
        return current.copy(
            accounts = newAccounts,
            transactions = current.transactions.filter { it.id != id }
        ).also { save(it) }
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    suspend fun addSubscription(current: AppState, sub: Subscription): AppState =
        current.copy(subscriptions = current.subscriptions + sub).also { save(it) }

    suspend fun updateSubscription(current: AppState, sub: Subscription): AppState =
        current.copy(subscriptions = current.subscriptions.map { if (it.id == sub.id) sub else it }).also { save(it) }

    suspend fun deleteSubscription(current: AppState, id: String): AppState =
        current.copy(subscriptions = current.subscriptions.filter { it.id != id }).also { save(it) }

    suspend fun paySubscription(current: AppState, subId: String, accountId: String): AppState {
        val sub = current.subscriptions.find { it.id == subId } ?: return current
        val ym = currentYearMonth()
        val payment = SubscriptionPayment(month = ym, amount = sub.amount, accountId = accountId)
        val tx = Transaction(
            accountId = accountId, type = "expense", amount = sub.amount,
            description = sub.name, category = "subscriptions", source = "subscription"
        )
        val delta = -sub.amount
        val newAccounts = current.accounts.map {
            if (it.id == accountId) it.copy(balance = it.balance + delta) else it
        }
        val newSubs = current.subscriptions.map {
            if (it.id == subId) it.copy(
                lastPaidMonth = ym,
                payments = it.payments + payment
            ) else it
        }
        return current.copy(
            accounts = newAccounts,
            transactions = listOf(tx) + current.transactions,
            subscriptions = newSubs
        ).also { save(it) }
    }

    // ── MSI ───────────────────────────────────────────────────────────────────

    suspend fun addMsiPlan(current: AppState, plan: MsiPlan): AppState =
        current.copy(msiPlans = current.msiPlans + plan).also { save(it) }

    suspend fun confirmMsiPayment(current: AppState, planId: String, accountId: String): AppState {
        val plan = current.msiPlans.find { it.id == planId } ?: return current
        val payment = MsiPayment(month = plan.paidMonths + 1, amount = plan.monthlyPayment, accountId = accountId)
        val tx = Transaction(
            accountId = accountId, type = "expense", amount = plan.monthlyPayment,
            description = "MSI Mes ${plan.paidMonths + 1}/${plan.months}: ${plan.name}", source = "msi"
        )
        val newPaid = plan.paidMonths + 1
        val newPlans = current.msiPlans.map {
            if (it.id == planId) it.copy(
                paidMonths = newPaid,
                status = if (newPaid >= it.months) "paid_off" else "active",
                payments = it.payments + payment
            ) else it
        }
        val delta = -plan.monthlyPayment
        val newAccounts = current.accounts.map {
            if (it.id == accountId) it.copy(balance = it.balance + delta) else it
        }
        return current.copy(
            accounts = newAccounts,
            transactions = listOf(tx) + current.transactions,
            msiPlans = newPlans
        ).also { save(it) }
    }

    suspend fun markMsiMonthNoDeduct(current: AppState, planId: String): AppState {
        val newPlans = current.msiPlans.map {
            if (it.id == planId) {
                val newPaid = it.paidMonths + 1
                it.copy(paidMonths = newPaid, status = if (newPaid >= it.months) "paid_off" else "active")
            } else it
        }
        return current.copy(msiPlans = newPlans).also { save(it) }
    }

    suspend fun liquidateMsi(current: AppState, planId: String, accountId: String): AppState {
        val plan = current.msiPlans.find { it.id == planId } ?: return current
        val remaining = (plan.months - plan.paidMonths) * plan.monthlyPayment
        val payment = MsiPayment(month = plan.paidMonths + 1, amount = remaining, type = "liquidated", accountId = accountId)
        val tx = Transaction(
            accountId = accountId, type = "expense", amount = remaining,
            description = "Liquidación MSI: ${plan.name}", source = "msi"
        )
        val newPlans = current.msiPlans.map {
            if (it.id == planId) it.copy(
                paidMonths = it.months, status = "paid_off", payments = it.payments + payment
            ) else it
        }
        val delta = -remaining
        val newAccounts = current.accounts.map {
            if (it.id == accountId) it.copy(balance = it.balance + delta) else it
        }
        return current.copy(
            accounts = newAccounts,
            transactions = listOf(tx) + current.transactions,
            msiPlans = newPlans
        ).also { save(it) }
    }

    suspend fun deleteMsiPlan(current: AppState, id: String): AppState =
        current.copy(msiPlans = current.msiPlans.filter { it.id != id }).also { save(it) }

    // ── Custom income types ───────────────────────────────────────────────────

    suspend fun addCustomIncomeType(current: AppState, type: CustomIncomeType): AppState =
        current.copy(customIncomeTypes = current.customIncomeTypes + type).also { save(it) }

    suspend fun deleteCustomIncomeType(current: AppState, id: String): AppState =
        current.copy(customIncomeTypes = current.customIncomeTypes.filter { it.id != id }).also { save(it) }

    // ── Loans ─────────────────────────────────────────────────────────────────

    suspend fun addLoan(current: AppState, loan: Loan): AppState =
        current.copy(loans = current.loans + loan).also { save(it) }

    suspend fun markLoanPaid(current: AppState, id: String): AppState {
        val newLoans = current.loans.map {
            if (it.id == id) it.copy(status = "paid", paidDate = today()) else it
        }
        return current.copy(loans = newLoans).also { save(it) }
    }

    suspend fun deleteLoan(current: AppState, id: String): AppState =
        current.copy(loans = current.loans.filter { it.id != id }).also { save(it) }
}
