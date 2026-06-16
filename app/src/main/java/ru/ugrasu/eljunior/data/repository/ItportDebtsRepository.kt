package ru.ugrasu.eljunior.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.ugrasu.eljunior.data.api.ItportApiClient
import ru.ugrasu.eljunior.data.model.Debt
import ru.ugrasu.eljunior.data.model.ItportAcademicItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItportDebtsRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val itportApiClient: ItportApiClient
) {

    companion object {
        private const val TAG = "ItportDebts"
    }

    suspend fun getDebts(): List<Debt> {
        return withContext(Dispatchers.IO) {
            loadDebts(retryOnAuthFailure = true)
        }
    }

    suspend fun getActiveDebts(): List<Debt> {
        return getDebts().filter { it.isActive }
    }

    private suspend fun loadDebts(retryOnAuthFailure: Boolean): List<Debt> {
        val loginResult = authRepository.ensureItportLogin(force = retryOnAuthFailure)
        if (loginResult.isFailure) {
            throw loginResult.exceptionOrNull()
                ?: Exception("Не удалось войти в itport")
        }

        val progressResult = itportApiClient.fetchAcademicProgress()
        if (progressResult.isFailure) {
            val error = progressResult.exceptionOrNull()
            if (retryOnAuthFailure && isAuthError(error)) {
                authRepository.ensureItportLogin(force = true)
                return loadDebts(retryOnAuthFailure = false)
            }
            Log.w(TAG, "API error", error)
            throw error ?: Exception("Не удалось загрузить академический прогресс")
        }

        val items = progressResult.getOrThrow()
        val debts = items
            .filter { it.isAcademicDebt() }
            .map { it.toDebt() }

        Log.d(TAG, "Loaded ${debts.size} debts from getAcademicProgress (${items.size} total)")
        return debts
    }

    private fun isAuthError(error: Throwable?): Boolean {
        val message = error?.message.orEmpty()
        return message.contains("419") || message.contains("401") || message.contains("403")
    }
}

private fun ItportAcademicItem.isAcademicDebt(): Boolean {
    if ((isDebt ?: 0) != 0) return true
    val label = statusLabel?.lowercase().orEmpty()
    return label.contains("нет оценки") ||
        label.contains("неявка") ||
        label.contains("задолж") ||
        label.contains("неуд")
}
