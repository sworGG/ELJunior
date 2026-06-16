package ru.ugrasu.eljunior.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ugrasu.eljunior.ui.theme.PrimaryRed // Используем ваш цвет, если он есть, или Color.Red

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenDebts: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Логика выхода (из предыдущего шага)
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
            viewModel.onLogoutHandled()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- ЗАГОЛОВОК ---
                item {
                    Text(
                        text = "Профиль",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- ИНФО О СТУДЕНТЕ ---
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Аватарка (заглушка)
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Имя
                        Text(
                            text = uiState.userProfile?.fullName ?: "Студент",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        // Email или группа (используем email, т.к. groupName выдавал ошибку)
                        //   uiState.userProfile?.email?.let { email ->
                        //   Text(
                        //       text = email,
                        //        style = MaterialTheme.typography.bodyMedium,
                        //       color = Color.Gray
                        //   )
                        // }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- СТАТИСТИКА (Курсы, Средний балл) ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            value = uiState.totalCourses.toString(),
                            label = "Курсов"
                        )
                        StatItem(
                            value = uiState.completedCourses.toString(),
                            label = "Завершено"
                        )
                        StatItem(
                            value = String.format("%.1f", uiState.averageGrade),
                            label = "Ср. балл"
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // --- МЕНЮ (Вкладки) ---
                item {
                    ProfileMenuItem(
                        icon = Icons.Default.Person,
                        title = "Личные данные",
                        onClick = { /* Навигация на экран личных данных */ }
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.School,
                        title = "Успеваемость",
                        onClick = { onOpenDebts() }
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.Notifications,
                        title = "Уведомления",
                        onClick = { /* Навигация на настройки уведомлений */ }
                    )
                }

                // --- КНОПКА ВЫХОДА ---
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Выйти из аккаунта")
                    }
                }
            }
        }
    }
}

// Компонент для одной плашки статистики
@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

// Компонент для пункто меню
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // или белый
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
