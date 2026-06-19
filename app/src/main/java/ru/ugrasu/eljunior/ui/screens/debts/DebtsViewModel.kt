package ru.ugrasu.eljunior.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ugrasu.eljunior.data.model.Debt
import ru.ugrasu.eljunior.data.repository.ItportDebtsRepository
import javax.inject.Inject

data class DebtsUiState(
    val isLoading: Boolean = false,
    val debts: List<Debt> = emptyList(),
    val showActiveOnly: Boolean = true,
    val error: String? = null
) {
    val filteredDebts: List<Debt>
        get() = if (showActiveOnly) debts.filter { it.isActive } else debts

    val activeCount: Int
        get() = debts.count { it.isActive }
}

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val debtsRepository: ItportDebtsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtsUiState())
    val uiState: StateFlow<DebtsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val debts = withContext(Dispatchers.IO) {
                    debtsRepository.getDebts()
                }
                _uiState.update {
                    it.copy(isLoading = false, debts = debts, error = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось загрузить задолженности"
                    )
                }
            }
        }
    }

    fun setShowActiveOnly(activeOnly: Boolean) {
        _uiState.update { it.copy(showActiveOnly = activeOnly) }
    }
}
