package ir.codecrafter.plasticproducts.ui.visitor

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.model.OrderItem
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.util.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorOrderDetailScreen(
    onBackClick: () -> Unit = {},
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.order?.let { stringResource(R.string.label_order_id_value, it.id.toString()) }
                            ?: "\u062c\u0632\u0626\u06cc\u0627\u062a \u0645\u0623\u0645\u0648\u0631\u06cc\u062a",
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

                state.order != null -> VisitorOrderDetailContent(
                    order = state.order!!,
                    isUpdatingStatus = state.isUpdatingStatus,
                    onStartLoading = { viewModel.advanceStatus("loading") },
                    onRequestMarkDelivered = { showDeliveredConfirm = true },
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
    val context = LocalContext.current

    val dutyInstruction = when (order.status) {
        "assigned" -> "\uD83D\uDCE6 \u0631\u0627\u0647\u0646\u0645\u0627\u06cc \u0645\u0623\u0645\u0648\u0631\u06cc\u062a (\u06af\u0627\u0645 \u06f1 \u0627\u0632 \u06f2):\n\u0627\u0642\u0644\u0627\u0645 \u0632\u06cc\u0631 \u0631\u0627 \u0627\u0632 \u0627\u0646\u0628\u0627\u0631 \u062a\u062d\u0648\u06cc\u0644 \u0628\u06af\u06cc\u0631\u06cc\u062f \u0648 \u062f\u06a9\u0645\u0647 \u00ab\u0634\u0631\u0648\u0639 \u0628\u0627\u0631\u06af\u06cc\u0631\u06cc\u00bb \u062f\u0631 \u067e\u0627\u06cc\u06cc\u0646 \u0635\u0641\u062d\u0647 \u0631\u0627 \u0628\u0632\u0646\u06cc\u062f."
        "loading" -> "\uD83D\uDE9A \u0631\u0627\u0647\u0646\u0645\u0627\u06cc \u0645\u0623\u0645\u0648\u0631\u06cc\u062a (\u06af\u0627\u0645 \u06f2 \u0627\u0632 \u06f2):\n\u0628\u0627\u0631 \u0631\u0627 \u0628\u0647 \u0622\u062f\u0631\u0633 \u062e\u0631\u06cc\u062f\u0627\u0631 \u0628\u0628\u0631\u06cc\u062f \u0648 \u067e\u0633 \u0627\u0632 \u062a\u062d\u0648\u06cc\u0644 \u06a9\u0627\u0645\u0644\u060c \u062f\u06a9\u0645\u0647 \u00ab\u062a\u062d\u0648\u06cc\u0644 \u062f\u0627\u062f\u0647 \u0634\u062f\u00bb \u0631\u0627 \u0628\u0632\u0646\u06cc\u062f."
        "delivered" -> "\u2705 \u0627\u06cc\u0646 \u0645\u0623\u0645\u0648\u0631\u06cc\u062a \u0628\u0627 \u0645\u0648\u0641\u0642\u06cc\u062a \u0628\u0647 \u067e\u0627\u06cc\u0627\u0646 \u0631\u0633\u06cc\u062f\u0647 \u0648 \u0641\u0627\u06a9\u062a\u0648\u0631 \u0628\u0631\u0627\u06cc \u0645\u0634\u062a\u0631\u06cc \u0635\u0627\u062f\u0631 \u0634\u062f\u0647 \u0627\u0633\u062a."
        else -> ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                VisitorStepper(status = order.status)
            }

            if (dutyInstruction.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = dutyInstruction,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            // کارت مشخصات، آدرس و تماس مستقیم با خریدار
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "\u0627\u0637\u0644\u0627\u0639\u0627\u062a \u0648 \u0622\u062f\u0631\u0633 \u062a\u062d\u0648\u06cc\u0644\u200c\u06af\u06cc\u0631\u0646\u062f\u0647",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.label_buyer_name_value, order.buyerName.orEmpty()),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = stringResource(R.string.label_buyer_phone_value, order.buyerPhone.orEmpty()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.label_buyer_address_value,
                                order.buyerAddress ?: stringResource(R.string.msg_address_not_registered),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = stringResource(R.string.label_order_total_value, PriceFormatter.format(order.totalPrice)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )

                        if (!order.buyerPhone.isNullOrBlank()) {
                            FilledTonalButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.buyerPhone}"))
                                    context.startActivity(dialIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                Text("\u062a\u0645\u0627\u0633 \u062a\u0644\u0641\u0646\u06cc \u0628\u0627 \u062e\u0631\u06cc\u062f\u0627\u0631 (${order.buyerPhone})")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "\u0644\u06cc\u0633\u062a \u0627\u0642\u0644\u0627\u0645 \u062c\u0647\u062a \u0628\u0627\u0631\u06af\u06cc\u0631\u06cc \u0648 \u062a\u062d\u0648\u06cc\u0644:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
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
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                if (isUpdatingStatus) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = "\uD83D\uDCE6 " + stringResource(R.string.btn_start_loading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            "loading" -> Button(
                onClick = onRequestMarkDelivered,
                enabled = !isUpdatingStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                if (isUpdatingStatus) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = "\u2705 " + stringResource(R.string.btn_mark_delivered),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun VisitorStepper(status: String) {
    val steps = listOf(
        "assigned" to "\u06f1. \u062a\u062e\u0635\u06cc\u0635 \u0628\u0647 \u0634\u0645\u0627",
        "loading" to "\u06f2. \u0628\u0627\u0631\u06af\u06cc\u0631\u06cc \u0627\u0632 \u0627\u0646\u0628\u0627\u0631",
        "delivered" to "\u06f3. \u062a\u062d\u0648\u06cc\u0644 \u0628\u0647 \u0645\u0634\u062a\u0631\u06cc",
    )
    val currentIndex = steps.indexOfFirst { it.first == status }.coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, (_, label) ->
                val isDone = index <= currentIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
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
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    val unitLabel = item.productDetail.unitLabel?.takeIf { it.isNotBlank() } ?: "\u0648\u0627\u062d\u062f"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.productDetail.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${stringResource(R.string.label_order_item_quantity_value, PriceFormatter.formatQuantity(item.quantity))} \u0628\u0631 \u062d\u0633\u0628 $unitLabel",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.label_order_item_unit_price_value, PriceFormatter.format(item.unitPrice)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
