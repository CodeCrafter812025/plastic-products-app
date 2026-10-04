package ir.codecrafter.plasticproducts.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.VisitorPerformance
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.util.PriceFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorPerformanceScreen(
    onBackClick: () -> Unit = { /* Defined in NavHost */ },
    viewModel: VisitorPerformanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\u0639\u0645\u0644\u06a9\u0631\u062f \u0648\u06cc\u0632\u06cc\u062a\u0648\u0631\u0647\u0627", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.msg_visitor_performance_all_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.errorMessage != null -> ErrorWithRetry(
                        message = state.errorMessage.orEmpty(),
                        onRetry = viewModel::load,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )

                    state.entries.isEmpty() -> Text(
                        text = stringResource(R.string.empty_visitor_performance),
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.entries, key = { "visitor_perf_${it.id}" }) { entry ->
                            VisitorPerformanceRow(
                                entry = entry,
                                isHighlighted = entry.id == viewModel.highlightVisitorId,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitorPerformanceRow(entry: VisitorPerformance, isHighlighted: Boolean) {
    val contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isHighlighted) it.background(MaterialTheme.colorScheme.primaryContainer) else it },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = entry.fullName.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor ?: Color.Unspecified,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = entry.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor ?: Color.Unspecified,
            )
            Text(
                // رفع فرمت اعشاری (.0)
                text = stringResource(R.string.label_total_assigned_value, PriceFormatter.formatQuantity(entry.totalAssigned.toString())),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor ?: Color.Unspecified,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.label_delivered_value, PriceFormatter.formatQuantity(entry.delivered.toString())),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor ?: Color.Unspecified,
            )
            Text(
                text = stringResource(R.string.label_cancelled_value, PriceFormatter.formatQuantity(entry.cancelled.toString())),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor ?: Color.Unspecified,
            )
            Text(
                text = stringResource(
                    R.string.label_avg_delivery_time_value,
                    avgDeliveryValueText(entry.avgDeliverySeconds),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor ?: Color.Unspecified,
            )
        }
    }
}

@Composable
private fun avgDeliveryValueText(seconds: Double?): String {
    if (seconds == null) return stringResource(R.string.msg_no_data)
    val hours = seconds / 3600.0
    return stringResource(R.string.label_avg_delivery_hours, String.format(Locale.US, "%.1f", hours))
}
