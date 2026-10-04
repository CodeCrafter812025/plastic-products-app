package ir.codecrafter.plasticproducts.ui.orders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.model.OrderItem
import ir.codecrafter.plasticproducts.data.model.OrderStatusHistoryEntry
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.util.PersianDateFormatter
import ir.codecrafter.plasticproducts.util.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrderDetailScreen(
    onEditOrder: (Int) -> Unit,
    onViewInvoice: (Int) -> Unit,
    onBackClick: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    viewModel: BuyerOrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BuyerOrderDetailEvent.ActionFailed -> snackbarHostState.showSnackbar(event.message)
                BuyerOrderDetailEvent.ReorderSuccess -> onNavigateToCart()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.order?.let { stringResource(R.string.label_order_id_value, it.id.toString()) }
                            ?: stringResource(R.string.btn_my_orders),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back_to_list),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.errorMessage != null -> ErrorWithRetry(
                    message = state.errorMessage.orEmpty(),
                    onRetry = viewModel::loadOrder,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                state.order != null -> BuyerOrderDetailContent(
                    order = state.order!!,
                    statusHistory = state.statusHistory,
                    isCancelling = state.isCancelling,
                    isReordering = state.isReordering,
                    isBuyer = viewModel.isBuyer,
                    onEditOrder = { onEditOrder(state.order!!.id) },
                    onRequestCancel = { showCancelConfirm = true },
                    onViewInvoice = { onViewInvoice(state.order!!.id) },
                    onReorder = viewModel::reorderAllItems,
                )

                else -> ErrorWithRetry(
                    message = stringResource(R.string.msg_loading_failed_generic),
                    onRetry = viewModel::loadOrder,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { if (!state.isCancelling) showCancelConfirm = false },
            title = { Text(stringResource(R.string.btn_cancel_order)) },
            text = { Text(stringResource(R.string.msg_cancel_order_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        viewModel.cancelOrder()
                    },
                    enabled = !state.isCancelling,
                ) {
                    Text(stringResource(R.string.btn_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelConfirm = false },
                    enabled = !state.isCancelling,
                ) {
                    Text(stringResource(R.string.btn_no))
                }
            },
        )
    }
}

@Composable
private fun BuyerOrderDetailContent(
    order: Order,
    statusHistory: List<OrderStatusHistoryEntry>,
    isCancelling: Boolean,
    isReordering: Boolean,
    isBuyer: Boolean,
    onEditOrder: () -> Unit,
    onRequestCancel: () -> Unit,
    onViewInvoice: () -> Unit,
    onReorder: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
        ) {
            item {
                // نوار تصویری ۴ مرحله‌ای وضعیت سفارش (Order Status Stepper)
                OrderStatusStepper(status = order.status)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.label_order_total_value, PriceFormatter.format(order.totalPrice)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            items(order.items, key = { "item_${it.id}" }) { item ->
                OrderItemRow(item)
            }

            if (isBuyer) {
                item {
                    // دکمه تکرار خرید (سفارش مجدد تمام اقلام این فاکتور با یک لمس)
                    FilledTonalButton(
                        onClick = onReorder,
                        enabled = !isReordering && !isCancelling,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(48.dp),
                    ) {
                        if (isReordering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                text = "\u062a\u06a9\u0631\u0627\u0631 \u062e\u0631\u06cc\u062f (\u0627\u0641\u0632\u0648\u062f\u0646 \u0645\u062c\u062f\u062f \u0628\u0647 \u0633\u0628\u062f)",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (order.status == "pending" && isBuyer) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onEditOrder,
                            enabled = !isCancelling,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.btn_edit_order))
                        }
                        Button(
                            onClick = onRequestCancel,
                            enabled = !isCancelling,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isCancelling) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(stringResource(R.string.btn_cancel_order))
                            }
                        }
                    }
                }
            }

            if (order.status == "delivered") {
                item {
                    Button(
                        onClick = onViewInvoice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text(stringResource(R.string.btn_view_invoice))
                    }
                }
            }

            if (statusHistory.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.title_status_history),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                }
                items(statusHistory, key = { "history_${it.id}" }) { entry ->
                    Text(
                        text = stringResource(
                            R.string.label_status_change_entry,
                            statusLabel(entry.newStatus),
                            PersianDateFormatter.toJalaliDateTime(entry.changedAt),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderStatusStepper(status: String) {
    if (status == "cancelled") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "\u0648\u0636\u0639\u06cc\u062a \u0633\u0641\u0627\u0631\u0634: \u0644\u063a\u0648 \u0634\u062f\u0647",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val steps = listOf(
        "pending" to "\u062b\u0628\u062a \u0633\u0641\u0627\u0631\u0634",
        "assigned" to "\u062a\u062e\u0635\u06cc\u0635 \u0648\u06cc\u0632\u06cc\u062a\u0648\u0631",
        "loading" to "\u0628\u0627\u0631\u06af\u06cc\u0631\u06cc",
        "delivered" to "\u062a\u062d\u0648\u06cc\u0644 \u0634\u062f\u0647",
    )
    val currentIndex = steps.indexOfFirst { it.first == status }.coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "\u0645\u0631\u0627\u062d\u0644 \u067e\u06cc\u06af\u06cc\u0631\u06cc \u0633\u0641\u0627\u0631\u0634",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                steps.forEachIndexed { index, (_, label) ->
                    val isDone = index <= currentIndex
                    val isCurrent = index == currentIndex

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDone) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    val unitLabel = item.productDetail.unitLabel?.takeIf { it.isNotBlank() } ?: ""
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.productDetail.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${stringResource(R.string.label_order_item_quantity_value, PriceFormatter.formatQuantity(item.quantity))} $unitLabel",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.label_order_item_unit_price_value, PriceFormatter.format(item.unitPrice)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
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
