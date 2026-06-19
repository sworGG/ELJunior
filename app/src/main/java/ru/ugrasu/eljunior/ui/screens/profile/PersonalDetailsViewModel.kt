package ru.ugrasu.eljunior.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.ugrasu.eljunior.data.model.StudentPersonalData
import ru.ugrasu.eljunior.data.repository.AuthRepository
import javax.inject.Inject

data class PersonalDetailsUiState(
    val isLoading: Boolean = true,
    val data: StudentPersonalData? = null,
    val error: String? = null
)

@HiltViewModel
class PersonalDetailsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalDetailsUiState())
    val uiState: StateFlow<PersonalDetailsUiState> = _uiState.asStateFlow()

    init {
        loadPersonalData()
    }

    fun loadPersonalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.fetchStudentPersonalData()

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
                        error = result.exceptionOrNull()?.message ?: "Ошибка загрузки данных"
                    )
                }
            }
        }
    }
}
