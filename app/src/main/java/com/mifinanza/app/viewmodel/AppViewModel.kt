package com.mifinanza.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mifinanza.app.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app.applicationContext)

    val state: StateFlow<AppState> = repo.appStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppState()
    )

    private val current get() = state.value

    // ── Accounts ──────────────────────────────────────────────────────────────
    fun setUserName(name: String) = viewModelScope.launch {
        repo.setUserName(current, name)
    }

    // Atomic: saves multiple accounts + username in one operation
    fun setupInitial(userName: String, accounts: List<Triple<String, String, Double>>, colors: List<String>) = viewModelScope.launch {
        var state = current
        if (userName.isNotBlank()) state = state.copy(userName = userName.trim())
        val newAccounts = accounts.mapIndexed { i, (name, type, balance) ->
            Account(name = name, type = type, balance = balance, color = colors.getOrElse(i) { "#6366F1" })
        }
        state = state.copy(accounts = state.accounts + newAccounts)
        repo.saveState(state)
    }

    fun toggleDarkMode() = viewModelScope.launch {
        val newState = current.copy(darkMode = !current.darkMode)
        repo.saveState(newState)
    }

    fun addAccount(name: String, type: String, balance: Double, color: String) = viewModelScope.launch {
        repo.addAccount(current, Account(name = name, type = type, balance = balance, color = color))
    }
    fun updateAccount(account: Account) = viewModelScope.launch {
        repo.updateAccount(current, account)
    }
    fun deleteAccount(id: String) = viewModelScope.launch {
        repo.deleteAccount(current, id)
    }

    // ── Transactions ──────────────────────────────────────────────────────────
    fun addTransaction(accountId: String, type: String, amount: Double, description: String, category: String, date: String = today()) = viewModelScope.launch {
        repo.addTransaction(current, Transaction(accountId = accountId, type = type, amount = amount, description = description, category = category, date = date))
    }
    fun deleteTransaction(id: String) = viewModelScope.launch {
        repo.deleteTransaction(current, id)
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────
    fun addSubscription(name: String, amount: Double, billingDay: Int, color: String) = viewModelScope.launch {
        repo.addSubscription(current, Subscription(name = name, amount = amount, billingDay = billingDay, color = color))
    }
    fun updateSubscription(sub: Subscription) = viewModelScope.launch {
        repo.updateSubscription(current, sub)
    }
    fun deleteSubscription(id: String) = viewModelScope.launch {
        repo.deleteSubscription(current, id)
    }
    fun toggleSubscriptionActive(id: String) = viewModelScope.launch {
        val sub = current.subscriptions.find { it.id == id } ?: return@launch
        repo.updateSubscription(current, sub.copy(active = !sub.active))
    }
    fun paySubscription(subId: String, accountId: String) = viewModelScope.launch {
        repo.paySubscription(current, subId, accountId)
    }

    // ── MSI ───────────────────────────────────────────────────────────────────
    fun addMsiPlan(name: String, desc: String, total: Double, months: Int, day: Int, startDate: String, initialPaid: Int = 0) = viewModelScope.launch {
        val plan = MsiPlan(
            name = name, description = desc, totalAmount = total, months = months,
            monthlyPayment = (total / months * 100).toLong() / 100.0,
            paymentDay = day, startDate = startDate,
            initialPaidMonths = initialPaid, paidMonths = initialPaid,
            status = if (initialPaid >= months) "paid_off" else "active"
        )
        repo.addMsiPlan(current, plan)
    }
    fun confirmMsiPayment(planId: String, accountId: String) = viewModelScope.launch {
        repo.confirmMsiPayment(current, planId, accountId)
    }
    fun markMsiMonthNoDeduct(planId: String) = viewModelScope.launch {
        repo.markMsiMonthNoDeduct(current, planId)
    }
    fun liquidateMsi(planId: String, accountId: String) = viewModelScope.launch {
        repo.liquidateMsi(current, planId, accountId)
    }
    fun deleteMsiPlan(id: String) = viewModelScope.launch {
        repo.deleteMsiPlan(current, id)
    }

    // ── Loans ─────────────────────────────────────────────────────────────────
    // ── Custom income types ────────────────────────────────────────────────────
    fun addCustomIncomeType(name: String, emoji: String = "💵") = viewModelScope.launch {
        repo.addCustomIncomeType(current, CustomIncomeType(name = name.trim(), emoji = emoji))
    }
    fun deleteCustomIncomeType(id: String) = viewModelScope.launch {
        repo.deleteCustomIncomeType(current, id)
    }

    fun addLoan(personName: String, amount: Double, description: String, givenDate: String, expectedDate: String) = viewModelScope.launch {
        repo.addLoan(current, Loan(personName = personName, amount = amount, description = description, givenDate = givenDate, expectedReturnDate = expectedDate))
    }
    fun markLoanPaid(id: String) = viewModelScope.launch {
        repo.markLoanPaid(current, id)
    }
    fun deleteLoan(id: String) = viewModelScope.launch {
        repo.deleteLoan(current, id)
    }

    // ── Computed ──────────────────────────────────────────────────────────────
    fun totalBalance() = current.accounts.sumOf { it.balance }
    fun monthlyMsiCommitment() = current.msiPlans.filter { it.status == "active" }.sumOf { it.monthlyPayment }
    fun monthlySubCommitment() = current.subscriptions.filter { it.active }.sumOf { it.amount }
    fun pendingLoanTotal() = current.loans.filter { it.status == "pending" }.sumOf { it.amount }
    fun overdueLoans() = current.loans.filter { it.status == "pending" && daysUntilDate(it.expectedReturnDate) < 0 }
}
