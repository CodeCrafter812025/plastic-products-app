package ir.codecrafter.plasticproducts.ui.visitor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.model.OrderItem

@Composable
fun VisitorOrderDetailScreen(
    viewModel: VisitorOrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeliveredConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is VisitorOrderDetailEvent.ActionFailed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.errorMessage != null -> Text(
                    text = state.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                state.order != null -> VisitorOrderDetailContent(
                    order = state.order!!,
                    isUpdatingStatus = state.isUpdatingStatus,
                    onStartLoading = { viewModel.advanceStatus("loading") },
                    onRequestMarkDelivered = { showDeliveredConfirm = true },
                )
            }
        }
    }

    if (showDeliveredConfirm) {
        AlertDialog(
            onDismissRequest = { if (!state.isUpdatingStatus) showDeliveredConfirm = false },
            title = { Text(stringResource(R.string.btn_mark_delivered)) },
            text = { Text(stringResource(R.string.msg_confirm_delivered)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeliveredConfirm = false
                        viewModel.advanceStatus("delivered")
                    },
                    enabled = !state.isUpdatingStatus,
                ) {
                    Text(stringResource(R.string.btn_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeliveredConfirm = false },
                    enabled = !state.isUpdatingStatus,
                ) {
                    Text(stringResource(R.string.btn_no))
                }
            },
        )
    }
}

@Composable
private fun VisitorOrderDetailContent(
    order: Order,
    isUpdatingStatus: Boolean,
    onStartLoading: () -> Unit,
    onRequestMarkDelivered: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.label_order_id_value, order.id.toString()),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.label_buyer_name_value, order.buyerName.orEmpty()),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.label_buyer_phone_value, order.buyerPhone.orEmpty()),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(
                        R.string.label_buyer_address_value,
                        order.buyerAddress ?: stringResource(R.string.msg_address_not_registered),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(R.string.label_order_status_value, statusLabel(order.status)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(R.string.label_order_total_value, order.totalPrice),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }

            items(order.items, key = { it.id }) { item ->
                OrderItemRow(item)
            }
        }

        when (order.status) {
            "assigned" -> Button(
                onClick = onStartLoading,
                enabled = !isUpdatingStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                if (isUpdatingStatus) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.btn_start_loading))
                }
            }

            "loading" -> Button(
                onClick = onRequestMarkDelivered,
                enabled = !isUpdatingStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                if (isUpdatingStatus) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.btn_mark_delivered))
                }
            }

            // delivered, cancelled, pending: no valid next transition — status-only display above.
            else -> Unit
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.productDetail.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.label_order_item_quantity_value, item.quantity),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.label_order_item_unit_price_value, item.unitPrice),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
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
