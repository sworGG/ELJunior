package ru.ugrasu.eljunior.ui.screens.deadlines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.ugrasu.eljunior.data.model.Deadline
import ru.ugrasu.eljunior.data.model.DeadlineType
import ru.ugrasu.eljunior.data.repository.CourseRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class DeadlinesRange {
    Week,
    Month,
    All
}

data class DeadlinesUiState(
    val isLoading: Boolean = true,
    val deadlines: List<Deadline> = emptyList(),
    val error: String? = null,
    val query: String = "",
    val selectedType: DeadlineType? = null,
    val selectedRange: DeadlinesRange = DeadlinesRange.Month
)

@HiltViewModel
class DeadlinesViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeadlinesUiState())
    val uiState: StateFlow<DeadlinesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val now = Instant.now().epochSecond
                val (to, limit) = when (_uiState.value.selectedRange) {
                    DeadlinesRange.Week -> Instant.now().plus(7, ChronoUnit.DAYS).epochSecond to 200
                    DeadlinesRange.Month -> Instant.now().plus(30, ChronoUnit.DAYS).epochSecond to 400
                    DeadlinesRange.All -> null to 1000
                }

                val data = courseRepository.getDeadlines(
                    timeFrom = now,
                    timeTo = to,
                    limit = limit
                )
                _uiState.update { it.copy(isLoading = false, deadlines = data) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки дедлайнов") }
            }
        }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun setType(type: DeadlineType?) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun setRange(range: DeadlinesRange) {
        _uiState.update { it.copy(selectedRange = range) }
        load()
    }
}

