package ru.ugrasu.eljunior.ui.screens.courses

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
import ru.ugrasu.eljunior.data.model.Course
import ru.ugrasu.eljunior.data.repository.CourseRepository
import javax.inject.Inject

data class CoursesUiState(
    val isLoading: Boolean = false,
    val courses: List<Course> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoursesUiState())
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val courses = withContext(Dispatchers.IO) {
                    courseRepository.getUserCourses()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        courses = courses,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки курсов"
                    )
                }
            }
        }
    }

    fun toggleFavorite(courseId: Int) {
        viewModelScope.launch {
            val updatedCourses = _uiState.value.courses.map { course ->
                if (course.id == courseId) {
                    course.copy(isFavourite = !course.isFavourite)
                } else {
                    course
                }
            }
            _uiState.update { it.copy(courses = updatedCourses) }
        }
    }
}
