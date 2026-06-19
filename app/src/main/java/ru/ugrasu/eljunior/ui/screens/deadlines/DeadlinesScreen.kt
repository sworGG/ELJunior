package ru.ugrasu.eljunior.ui.screens.deadlines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import ru.ugrasu.eljunior.data.model.Deadline
import ru.ugrasu.eljunior.data.model.DeadlineType
import ru.ugrasu.eljunior.ui.theme.BackgroundGray
import ru.ugrasu.eljunior.ui.theme.PrimaryRed
import ru.ugrasu.eljunior.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinesScreen(
    onBack: () -> Unit,
    onOpenCourse: (Int) -> Unit,
    viewModel: DeadlinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    val filtered = uiState.deadlines
        .asSequence()
        .filter { d -> uiState.selectedType == null || d.type == uiState.selectedType }
        .filter { d ->
            val q = uiState.query.trim()
            q.isEmpty() || d.title.contains(q, ignoreCase = true) || d.courseName.contains(q, ignoreCase = true)
        }
        .toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Дедлайны",
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
                actions = {
                    TextButton(onClick = { viewModel.load() }) { Text("Обновить") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Поиск по дедлайнам") },
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RangeChip(
                    selected = uiState.selectedRange == DeadlinesRange.Week,
                    label = "7 дней",
                    onClick = { viewModel.setRange(DeadlinesRange.Week) }
                )
                RangeChip(
                    selected = uiState.selectedRange == DeadlinesRange.Month,
                    label = "30 дней",
                    onClick = { viewModel.setRange(DeadlinesRange.Month) }
                )
                RangeChip(
                    selected = uiState.selectedRange == DeadlinesRange.All,
                    label = "Все",
                    onClick = { viewModel.setRange(DeadlinesRange.All) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeChip(
                    selected = uiState.selectedType == null,
                    label = "Все",
                    onClick = { viewModel.setType(null) }
                )
                TypeChip(
                    selected = uiState.selectedType == DeadlineType.ASSIGNMENT,
                    label = DeadlineType.ASSIGNMENT.displayName,
                    onClick = { viewModel.setType(DeadlineType.ASSIGNMENT) }
                )
                TypeChip(
                    selected = uiState.selectedType == DeadlineType.QUIZ,
                    label = DeadlineType.QUIZ.displayName,
                    onClick = { viewModel.setType(DeadlineType.QUIZ) }
                )
                TypeChip(
                    selected = uiState.selectedType == DeadlineType.FORUM,
                    label = DeadlineType.FORUM.displayName,
                    onClick = { viewModel.setType(DeadlineType.FORUM) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    uiState.isLoading -> CircularProgressIndicator()
                    uiState.error != null -> ErrorState(message = uiState.error!!, onRetry = viewModel::load)
                    filtered.isEmpty() -> Text("Нет дедлайнов по выбранным фильтрам")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { deadline ->
                            DeadlineRow(
                                deadline = deadline,
                                onOpen = { d ->
                                    when {
                                        d.url != null -> uriHandler.openUri(d.url)
                                        d.courseId != null -> onOpenCourse(d.courseId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineRow(deadline: Deadline, onOpen: (Deadline) -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpen(deadline) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = deadline.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = deadline.courseName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Text(
                text = "${deadline.getFormattedDate()} ${deadline.getFormattedTime()} · ${deadline.getTimeRemaining()}",
                style = MaterialTheme.typography.bodySmall,
                color = if (deadline.isUrgent) PrimaryRed else TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun RangeChip(selected: Boolean, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) PrimaryRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) PrimaryRed else TextPrimary
        )
    )
}

@Composable
private fun TypeChip(selected: Boolean, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) PrimaryRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) PrimaryRed else TextPrimary
        )
    )
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) { Text("Повторить") }
    }
}

