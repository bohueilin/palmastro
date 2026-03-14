package com.palmastro.contracts.interfaces

import com.palmastro.contracts.*

interface ResultRepository {
    fun saveMonthlyResult(result: MonthlyResult)
    fun getMonthlyResult(monthKey: String): MonthlyResult?
    fun listHistory(limit: Int): List<MonthlyResult>
}

interface EntitlementService {
    fun hasEntitlement(productId: String): Boolean
}
