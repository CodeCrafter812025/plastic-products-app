package ir.codecrafter.plasticproducts.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.PriceHistory
import ir.codecrafter.plasticproducts.data.model.StockChangeReason
import ir.codecrafter.plasticproducts.data.model.StockHistory

@Composable
fun ProductHistoryScreen(viewModel: ProductHistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_price_history)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_stock_history)) },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.errorMessage != null -> Text(
                        text = state.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )

                    selectedTab == 0 -> PriceHistoryList(state.priceHistory)

                    else -> StockHistoryList(state.stockHistory)
                }
            }
        }
    }
}

@Composable
private fun PriceHistoryList(entries: List<PriceHistory>) {
    if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.empty_price_history),
            modifier = Modifier.fillMaxSize().padding(24.dp),
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { "price_${it.id}" }) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.label_price_history_change,
                            entry.oldPrice ?: "-",
                            entry.newPrice,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.label_history_changed_at_by,
                            entry.changedAt,
                            entry.changedByName ?: "-",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StockHistoryList(entries: List<StockHistory>) {
    if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.empty_stock_history),
            modifier = Modifier.fillMaxSize().padding(24.dp),
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { "stock_${it.id}" }) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.label_stock_history_change,
                            entry.oldStock ?: "-",
                            entry.newStock,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stockReasonLabel(entry.reason),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.label_history_changed_at_by,
                            entry.changedAt,
                            entry.changedByName ?: "-",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun stockReasonLabel(reason: StockChangeReason): String = when (reason) {
    StockChangeReason.INITIAL -> stringResource(R.string.stock_reason_initial)
    StockChangeReason.SALE -> stringResource(R.string.stock_reason_sale)
    StockChangeReason.RESTOCK -> stringResource(R.string.stock_reason_restock)
    StockChangeReason.ADJUSTMENT -> stringResource(R.string.stock_reason_adjustment)
}
