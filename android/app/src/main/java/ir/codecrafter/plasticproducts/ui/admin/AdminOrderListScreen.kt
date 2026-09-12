package ir.codecrafter.plasticproducts.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.AdminUser
import ir.codecrafter.plasticproducts.data.model.Order

private val ORDER_STATUSES = listOf("pending", "assigned", "loading", "delivered", "cancelled")

@Composable
fun AdminOrderListScreen(
    onOrderClick: (Int) -> Unit,
    viewModel: AdminOrderListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var assignDialogOrder by remember { mutableStateOf<Order?>(null) }
    var cancelDialogOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AdminOrderListEvent.ActionFailed -> snackbarHostState.showSnackbar(event.message)
                AdminOrderListEvent.Assigned -> assignDialogOrder = null
            }
        }
    }

    // Same refresh-on-return pattern as AdminProductListScreen/AdminUserListScreen.
    LaunchedEffect(Unit) { viewModel.loadOrders() }

    LaunchedEffect(assignDialogOrder) {
        if (assignDialogOrder != null) viewModel.loadActiveVisitors()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.statusFilter == null,
                    onClick = { viewModel.onStatusFilterChange(null) },
                    label = { Text(stringResource(R.string.filter_quality_all)) },
                )
                ORDER_STATUSES.forEach { status ->
                    FilterChip(
                        selected = state.statusFilter == status,
                        onClick = { viewModel.onStatusFilterChange(status) },
                        label = { Text(statusLabel(status)) },
                    )
                }
            }

            val filteredOrders = state.orders.filter { state.statusFilter == null || it.status == state.statusFilter }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
            ) {
                when {
                    state.isLoading && state.orders.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.errorMessage != null ->
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                        )

                    filteredOrders.isEmpty() ->
                        Text(
                            text = stringResource(R.string.empty_admin_orders),
                            modifier = Modifier.align(Alignment.Center),
                        )

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredOrders, key = { "order_${it.id}" }) { order ->
                            AdminOrderRow(
                                order = order,
                                isActionInProgress = state.actionInProgressOrderId == order.id,
                                onClick = { onOrderClick(order.id) },
                                onAssignClick = { assignDialogOrder = order },
                                onCancelClick = { cancelDialogOrder = order },
                            )
                        }
                    }
                }
            }
        }
    }

    assignDialogOrder?.let { order ->
        AssignVisitorDialog(
            visitors = state.activeVisitors,
            isLoading = state.isLoadingVisitors,
            isAssigning = state.isAssigning,
            onConfirm = { visitorId -> viewModel.assignOrder(order.id, visitorId) },
            onDismiss = { assignDialogOrder = null },
        )
    }

    cancelDialogOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { cancelDialogOrder = null },
            title = { Text(stringResource(R.string.btn_cancel_order)) },
            text = { Text(stringResource(R.string.msg_cancel_order_admin_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelOrderAdmin(order.id)
                        cancelDialogOrder = null
                    },
                ) {
                    Text(stringResource(R.string.btn_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelDialogOrder = null }) {
                    Text(stringResource(R.string.btn_no))
                }
            },
        )
    }
}

@Composable
private fun AdminOrderRow(
    order: Order,
    isActionInProgress: Boolean,
    onClick: () -> Unit,
    onAssignClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.label_order_id_value, order.id.toString()),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.label_buyer_name_value, order.buyerName.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.label_order_status_value, statusLabel(order.status)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.label_order_total_value, order.totalPrice),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (order.status == "pending" || order.status == "assigned" || order.status == "loading") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (order.status == "pending") {
                        Button(
                            onClick = onAssignClick,
                            enabled = !isActionInProgress,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.btn_assign_to_visitor))
                        }
                    }
                    if (order.status == "assigned" || order.status == "loading") {
                        TextButton(
                            onClick = onCancelClick,
                            enabled = !isActionInProgress,
                        ) {
                            if (isActionInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.btn_cancel_order),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignVisitorDialog(
    visitors: List<AdminUser>,
    isLoading: Boolean,
    isAssigning: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedVisitorId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_select_visitor)) },
        text = {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                visitors.isEmpty() -> Text(stringResource(R.string.empty_active_visitors))

                else -> Column {
                    visitors.forEach { visitor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVisitorId = visitor.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedVisitorId == visitor.id,
                                onClick = { selectedVisitorId = visitor.id },
                            )
                            Column {
                                Text(text = visitor.fullName.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                                Text(text = visitor.phone, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedVisitorId?.let(onConfirm) },
                enabled = selectedVisitorId != null && !isAssigning,
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
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
