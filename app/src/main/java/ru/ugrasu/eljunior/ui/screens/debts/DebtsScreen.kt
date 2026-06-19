package ru.ugrasu.eljunior.ui.screens.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ugrasu.eljunior.ui.components.AppTopBar
import ru.ugrasu.eljunior.ui.components.DebtCard
import ru.ugrasu.eljunior.ui.components.states.EmptyState
import ru.ugrasu.eljunior.ui.components.states.ErrorState
import ru.ugrasu.eljunior.ui.components.states.LoadingState
import ru.ugrasu.eljunior.ui.theme.BackgroundGray
import ru.ugrasu.eljunior.ui.theme.PrimaryRed
import ru.ugrasu.eljunior.ui.theme.Spacing
import ru.ugrasu.eljunior.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onBack: () -> Unit,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Задолженности",
                onBack = onBack
            )
        },
        containerColor = BackgroundGray
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    FilterChip(
                        selected = uiState.showActiveOnly,
                        label = "Активные",
                        onClick = { viewModel.setShowActiveOnly(true) }
                    )
                    FilterChip(
                        selected = !uiState.showActiveOnly,
                        label = "Все",
                        onClick = { viewModel.setShowActiveOnly(false) }
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uiState.isLoading && uiState.debts.isEmpty() -> LoadingState()
                        uiState.error != null && uiState.debts.isEmpty() -> ErrorState(
                            message = uiState.error!!,
                            onRetry = { viewModel.load() }
                        )
                        uiState.filteredDebts.isEmpty() -> EmptyState(
                            icon = Icons.Default.Warning,
                            title = "Нет задолженностей",
                            subtitle = if (uiState.showActiveOnly) {
                                "Активных академических долгов нет"
                            } else {
                                "Список пуст"
                            }
                        )
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = Spacing.lg,
                                vertical = Spacing.sm
                            ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            items(uiState.filteredDebts, key = { it.id }) { debt ->
                                DebtCard(debt = debt)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) PrimaryRed.copy(alpha = 0.15f) else Color.White,
            labelColor = if (selected) PrimaryRed else TextPrimary
        )
    )
}
