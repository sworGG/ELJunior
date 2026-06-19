package ru.ugrasu.eljunior.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.ugrasu.eljunior.ui.screens.auth.AuthScreen
import ru.ugrasu.eljunior.ui.screens.auth.AuthViewModel
import ru.ugrasu.eljunior.ui.screens.course_details.CourseDetailsScreen
import ru.ugrasu.eljunior.ui.screens.courses.CoursesScreen
import ru.ugrasu.eljunior.ui.screens.deadlines.DeadlinesScreen
import ru.ugrasu.eljunior.ui.screens.debts.DebtsScreen
import ru.ugrasu.eljunior.ui.screens.home.HomeScreen
import ru.ugrasu.eljunior.ui.screens.profile.AcademicPerformanceScreen
import ru.ugrasu.eljunior.ui.screens.profile.NotificationsScreen
import ru.ugrasu.eljunior.ui.screens.profile.PersonalDetailsScreen
import ru.ugrasu.eljunior.ui.screens.profile.ProfileScreen
import ru.ugrasu.eljunior.ui.screens.schedule.ScheduleScreen
import ru.ugrasu.eljunior.ui.theme.PrimaryRed
import ru.ugrasu.eljunior.ui.theme.TextSecondary

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Главная", Icons.Filled.Home, Icons.Outlined.Home)
    object Schedule : Screen("schedule", "Расписание", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday)
    object Courses : Screen("courses", "Курсы", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    object Profile : Screen("profile", "Профиль", Icons.Filled.Person, Icons.Outlined.Person)
    object CourseDetails : Screen("course/{courseId}", "Курс", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    object Deadlines : Screen("deadlines", "Дедлайны", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday)
    object Debts : Screen("debts", "Задолженности", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday)
    object PersonalDetails : Screen("profile/personal_details", "Личные данные", Icons.Filled.Person, Icons.Outlined.Person)
    object AcademicPerformance : Screen("profile/academic", "Успеваемость", Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Notifications : Screen("profile/notifications", "Уведомления", Icons.Filled.Notifications, Icons.Outlined.Notifications)
}

// Удален конфликтующий класс AuthScreen, так как он затеняет импорт Composable AuthScreen
// и вызывает ошибки. Маршруты авторизации лучше хранить в отдельном файле или использовать строку.

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Schedule,
    Screen.Courses
)

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)

    if (isLoggedIn) {
        MainNavigation(
            onLogout = { authViewModel.logout() }
        )
    } else {
        AuthScreen(
            onLoginSuccess = { /* Навигация управляется состоянием isLoggedIn */ }
        )
    }
}

@Composable
fun MainNavigation(
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    val selected = when (screen) {
                        Screen.Profile -> currentDestination?.route?.startsWith("profile") == true
                        else -> currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    }

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryRed,
                            selectedTextColor = PrimaryRed,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onShowAllDeadlines = { navController.navigate(Screen.Deadlines.route) },
                    onOpenNotifications = { navController.navigate(Screen.Notifications.route) },
                    onOpenCourse = { courseId -> navController.navigate("course/$courseId") },
                    onOpenPersonalDetails = { navController.navigate(Screen.PersonalDetails.route) },
                    onOpenAcademicPerformance = { navController.navigate(Screen.AcademicPerformance.route) },
                    onLogout = onLogout
                )
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen()
            }
            composable(Screen.Courses.route) {
                CoursesScreen(
                    onOpenCourse = { courseId ->
                        navController.navigate("course/$courseId")
                    }
                )
            }
            composable(Screen.Profile.route) {
                // Здесь мы передаем колбэк onLogout в экран профиля.
                // В ProfileScreen при нажатии кнопки "Выход" нужно вызвать этот лямбда-метод.
                ProfileScreen(
                    onLogout = onLogout,
                    onOpenPersonalDetails = {
                        navController.navigate(Screen.PersonalDetails.route)
                    },
                    onOpenAcademicPerformance = {
                        navController.navigate(Screen.AcademicPerformance.route)
                    },
                    onOpenNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            composable(
                "course/{courseId}",
                arguments = listOf(
                    navArgument("courseId") { type = NavType.IntType }
                )
            ) {
                CourseDetailsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Deadlines.route) {
                DeadlinesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCourse = { courseId -> navController.navigate("course/$courseId") }
                )
            }

            composable(Screen.Debts.route) {
                DebtsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PersonalDetails.route) {
                PersonalDetailsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AcademicPerformance.route) {
                AcademicPerformanceScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
