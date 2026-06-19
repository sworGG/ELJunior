package ru.ugrasu.eljunior.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.ugrasu.eljunior.data.model.AcademicProgressData
import ru.ugrasu.eljunior.data.repository.ItportRepository
import javax.inject.Inject

data class AcademicPerformanceUiState(
    val isLoading: Boolean = true,
    val data: AcademicProgressData? = null,
    val error: String? = null
)

@HiltViewModel
class AcademicPerformanceViewModel @Inject constructor(
    private val itportRepository: ItportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademicPerformanceUiState())
    val uiState: StateFlow<AcademicPerformanceUiState> = _uiState.asStateFlow()

    init {
        loadAcademicProgress()
    }

    fun loadAcademicProgress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = itportRepository.fetchAcademicProgress()

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        data = result.getOrNull(),
                        error = null
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
                    )
                }
            }
        }
    }
}
