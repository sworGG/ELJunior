package ru.ugrasu.eljunior.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.ugrasu.eljunior.data.model.UserProfile
import ru.ugrasu.eljunior.data.repository.AuthRepository
import ru.ugrasu.eljunior.data.repository.CourseRepository
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val totalCourses: Int = 0,
    val completedCourses: Int = 0,
    val averageGrade: Float = 0f,
    val error: String? = null,
    val isLoggedOut: Boolean = false // Добавили флаг успешного выхода
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load user profile
                val userProfile = authRepository.getUserProfile()

                // Load courses for statistics
                val courses = try {
                    courseRepository.getUserCourses()
                } catch (e: Exception) {
                    emptyList()
                }

                val totalCourses = courses.size
                val completedCourses = courses.count { it.progress >= 100 }
                val averageGrade = if (courses.isNotEmpty()) {
                    courses.map { it.progress }.average().toFloat() / 20f // Convert to 5-point scale
                } else {
                    0f
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userProfile = userProfile,
                        totalCourses = totalCourses,
                        completedCourses = completedCourses,
                        averageGrade = averageGrade,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки профиля"
                    )
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            authRepository.refreshUserProfile()
            loadProfileData()
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                // После очистки данных обновляем состояние
                _uiState.update { it.copy(isLoggedOut = true) }
            } catch (e: Exception) {
                // Даже если произошла ошибка сети или БД при выходе,
                // всё равно пускаем пользователя на выход
                authRepository.logout() // Попытка форсированного сброса (если нужно)
                _uiState.update { it.copy(isLoggedOut = true) }
            }
        }
    }

    // Вспомогательный метод, чтобы сбросить флаг после навигации
    fun onLogoutHandled() {
        _uiState.update { it.copy(isLoggedOut = false) }
    }
}
