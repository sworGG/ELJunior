package ru.ugrasu.eljunior.ui.screens.course_details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ugrasu.eljunior.data.api.CourseModule
import ru.ugrasu.eljunior.data.api.CourseSection
import ru.ugrasu.eljunior.ui.theme.BackgroundGray
import ru.ugrasu.eljunior.ui.theme.TextPrimary
import ru.ugrasu.eljunior.ui.theme.TextSecondary
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(
    onBack: () -> Unit,
    viewModel: CourseDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "Курс" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.error != null -> ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.load() }
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.sections.forEach { section ->
                        item(key = section.id) {
                            CourseSectionCard(
                                section = section,
                                onOpenUrl = { url -> uriHandler.openUri(url) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseSectionCard(
    section: CourseSection,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section title
            Text(
                text = section.name.ifBlank { "Раздел" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // If section has summary (often used for announcements / descriptions) — render in an accented box
            val summary = section.summary.orEmpty().trim()
            if (summary.isNotEmpty()) {
                val (borderColor, bgColor, textColor) = styleForSection(section.name, summary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(BorderStroke(2.dp, borderColor), shape = RoundedCornerShape(6.dp))
                        .background(bgColor, shape = RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }

            // Modules list (each module as an interactive row card)
            val modules = section.modules.orEmpty().filter { (it.visible ?: 1) != 0 }
            if (modules.isEmpty()) {
                Text(
                    text = "Нет материалов",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                    color = TextSecondary
                )
            } else {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modules.forEach { module ->
                        CourseModuleRow(module = module, onOpenUrl = onOpenUrl)
                    }
                }
            }
        }
    }
}

private fun styleForSection(title: String, summary: String): Triple<Color, Color, Color> {
    val lower = (title + " " + summary).lowercase()
    return when {
        lower.contains("консультац") || lower.contains("консультации") -> {
            // blue highlighted box
            Triple(Color(0xFF1E88E5), Color(0xFFEEF7FF), Color.Black)
        }
        lower.contains("объявлен") || lower.contains("объявлени") || lower.contains("объявления") -> {
            // red accent for announcements
            Triple(Color(0xFFD32F2F), Color(0xFFFFF3F3), Color.Black)
        }
        lower.contains("аттестац") || lower.contains("тест") || lower.contains("промежуточ") -> {
            // pale green / purple for assessment info
            Triple(Color(0xFF8E24AA), Color(0xFFF7F1FF), Color.Black)
        }
        else -> {
            // default gray box
            Triple(Color(0xFFBDBDBD), Color(0xFFF5F5F5), Color.Black)
        }
    }
}

@Composable
private fun CourseModuleRow(
    module: CourseModule,
    onOpenUrl: (String) -> Unit
) {
    val url = module.url
    val enabled = !url.isNullOrBlank()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { if (enabled) onOpenUrl(url!!) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon depending on module type (fallback to file icon)
            val icon = when (module.modname?.lowercase()) {
                "url", "resource" -> Icons.Filled.Link
                "forum" -> Icons.Filled.Notifications
                "assign" -> Icons.Filled.UploadFile
                else -> Icons.Filled.NavigateNext
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) TextPrimary else TextSecondary
                )

                val subtype = module.modplural ?: module.modname
                if (!subtype.isNullOrBlank()) {
                    Text(
                        text = subtype,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            if (enabled) {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = "Открыть",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) {
            Text("Повторить")
        }
    }
}