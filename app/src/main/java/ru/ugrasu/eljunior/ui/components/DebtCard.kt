package ru.ugrasu.eljunior.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.ugrasu.eljunior.data.model.Debt
import ru.ugrasu.eljunior.data.model.DebtStatus
import ru.ugrasu.eljunior.ui.theme.AppShapes
import ru.ugrasu.eljunior.ui.theme.PrimaryRed
import ru.ugrasu.eljunior.ui.theme.Spacing
import ru.ugrasu.eljunior.ui.theme.SuccessColor
import ru.ugrasu.eljunior.ui.theme.TextPrimary
import ru.ugrasu.eljunior.ui.theme.TextSecondary

@Composable
fun DebtCard(
    debt: Debt,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .background(
                        color = if (debt.isActive) PrimaryRed else SuccessColor,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.subject,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (debt.debtType.isNotBlank()) {
                    Text(
                        text = debt.debtType,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (debt.teacher.isNotBlank()) {
                    Text(
                        text = debt.teacher,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (debt.dueDate != null) {
                    Text(
                        text = debt.getFormattedDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
                Text(
                    text = debt.status.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (debt.status == DebtStatus.ACTIVE) PrimaryRed else SuccessColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
