package ir.codecrafter.plasticproducts.ui.visitor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.util.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorOrderListScreen(
    onOrderClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: VisitorOrderListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    val assignedCount = state.orders.count { it.status == "assigned" }
    val loadingCount = state.orders.count { it.status == "loading" }
    val deliveredCount = state.orders.count { it.status == "delivered" }

    val filteredOrders = state.orders.filter {
        selectedStatusFilter == null || it.status == selectedStatusFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "\u067e\u0646\u0644 \u0645\u0623\u0645\u0648\u0631\u06cc\u062a\u200c\u0647\u0627\u06cc \u0648\u06cc\u0632\u06cc\u062a\u0648\u0631 \u0648 \u067e\u062e\u0634",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "\u0645\u062f\u06cc\u0631\u06cc\u062a \u0628\u0627\u0631\u06af\u06cc\u0631\u06cc \u0648 \u062a\u062d\u0648\u06cc\u0644 \u0633\u0641\u0627\u0631\u0634\u0627\u062a",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadOrders) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "\u0628\u0631\u0648\u0632\u0631\u0633\u0627\u0646\u06cc",
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.btn_profile),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            // کارت راهنمای خلاصه وضعیت مأموریت‌های ویزیتور
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "\u062e\u0644\u0627\u0635\u0647 \u0648\u0638\u0627\u06cc\u0641 \u0634\u0645\u0627 (\u0628\u0631\u0627\u06cc \u062b\u0628\u062a \u0648\u0636\u0639\u06cc\u062a \u0631\u0648\u06cc \u0633\u0641\u0627\u0631\u0634 \u0628\u0632\u0646\u06cc\u062f):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        SummaryStatItem(count = assignedCount, label = "\u0645\u0646\u062a\u0638\u0631 \u0628\u0627\u0631\u06af\u06cc\u0631\u06cc")
                        SummaryStatItem(count = loadingCount, label = "\u062f\u0631 \u062d\u0627\u0644 \u0627\u0631\u0633\u0627\u0644")
                        SummaryStatItem(count = deliveredCount, label = "\u062a\u062d\u0648\u06cc\u0644 \u0634\u062f\u0647")
                    }
                }
            }

            // نوار فیلتر سریع وضعیت‌ها
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    label = { Text("\u0647\u0645\u0647 (${state.orders.size})") },
                )
                FilterChip(
                    selected = selectedStatusFilter == "assigned",
                    onClick = { selectedStatusFilter = "assigned" },
                    label = { Text("\u0645\u0646\u062a\u0638\u0631 \u0628\u0627\u0631\u06af\u06cc\u0631\u06cc ($assignedCount)") },
                )
                FilterChip(
                    selected = selectedStatusFilter == "loading",
                    onClick = { selectedStatusFilter = "loading" },
                    label = { Text("\u062f\u0631 \u062d\u0627\u0644 \u0627\u0631\u0633\u0627\u0644 ($loadingCount)") },
                )
                FilterChip(
                    selected = selectedStatusFilter == "delivered",
                    onClick = { selectedStatusFilter = "delivered" },
                    label = { Text("\u062a\u062d\u0648\u06cc\u0644 \u0634\u062f\u0647 ($deliveredCount)") },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
            ) {
                when {
                    state.isLoading && state.orders.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.errorMessage != null ->
                        ErrorWithRetry(
                            message = state.errorMessage.orEmpty(),
                            onRetry = viewModel::loadOrders,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                        )

                    filteredOrders.isEmpty() ->
                        Text(
                            text = stringResource(R.string.empty_visitor_orders),
                            modifier = Modifier.align(Alignment.Center),
                        )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            VisitorOrderRow(order = order, onClick = { onOrderClick(order.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VisitorOrderRow(order: Order, onClick: () -> Unit) {
    val actionHint = when (order.status) {
        "assigned" -> "\uD83D\uDCE6 \u0648\u0638\u06cc\u0641\u0647 \u0641\u0639\u0644\u06cc: \u062a\u062d\u0648\u06cc\u0644 \u06af\u0631\u0641\u062a\u0646 \u0628\u0627\u0631 \u0627\u0632 \u0627\u0646\u0628\u0627\u0631 \u0648 \u062b\u0628\u062a \u00ab\u0634\u0631\u0648\u0639 \u0628\u0627\u0631\u06af\u06cc\u0631\u06cc\u00bb"
        "loading" -> "\uD83D\uDE9A \u0648\u0638\u06cc\u0641\u0647 \u0641\u0639\u0644\u06cc: \u0627\u0631\u0633\u0627\u0644 \u0628\u0647 \u0622\u062f\u0631\u0633 \u0645\u0634\u062a\u0631\u06cc \u0648 \u062b\u0628\u062a \u00ab\u062a\u062d\u0648\u06cc\u0644 \u062f\u0627\u062f\u0647 \u0634\u062f\u00bb"
        "delivered" -> "\u2705 \u0627\u06cc\u0646 \u0633\u0641\u0627\u0631\u0634 \u0628\u0627 \u0645\u0648\u0641\u0642\u06cc\u062a \u062a\u062d\u0648\u06cc\u0644 \u062f\u0627\u062f\u0647 \u0634\u062f\u0647 \u0627\u0633\u062a"
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.label_order_id_value, order.id.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    color = if (order.status == "delivered") {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = statusLabel(order.status),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = "\u062e\u0631\u06cc\u062f\u0627\u0631: ${order.buyerName.orEmpty()} ",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (!order.buyerAddress.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = order.buyerAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = stringResource(R.string.label_order_total_value, PriceFormatter.format(order.totalPrice)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )

            if (actionHint.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(10.dp),
                ) {
                    Text(
                        text = actionHint,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: String): String = when (status) {
    "pending" -> stringResource(R.string.status_pending)
    "assigned" -> stringResource(R.string.status_assigned)
    "loading" -> stringResource(R.string.status_loading)
    "delivered" -> stringResource(R.string.status_delivered)
    "cancelled" -> stringResource(R.string.status_cancelled)
    else -> status
}
