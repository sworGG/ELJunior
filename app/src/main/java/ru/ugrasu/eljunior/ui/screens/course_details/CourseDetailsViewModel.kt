package ru.ugrasu.eljunior.ui.screens.course_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.ugrasu.eljunior.data.api.CourseSection
import ru.ugrasu.eljunior.data.repository.CourseRepository
import javax.inject.Inject

data class CourseDetailsUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val sections: List<CourseSection> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CourseDetailsViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val courseId: Int? = savedStateHandle.get<Int>("courseId")

    private val _uiState = MutableStateFlow(CourseDetailsUiState())
    val uiState: StateFlow<CourseDetailsUiState> = _uiState.asStateFlow()

    init {
        if (courseId != null) {
            load()
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Ошибка: ID курса не найден"
                )
            }
        }
    }

    fun load() {
        if (courseId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Ошибка: ID курса не найден"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val course = courseRepository.getCourseDetails(courseId!!)
                val sections = courseRepository.getCourseContents(courseId!!)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = course.name,
                        sections = sections
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки курса"
                    )
                }
            }
        }
    }
}

