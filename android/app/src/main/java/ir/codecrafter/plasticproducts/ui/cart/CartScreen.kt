package ir.codecrafter.plasticproducts.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.CartItem
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.util.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBackToProducts: () -> Unit,
    onEditOrder: (Int) -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isPlacingOrder by viewModel.isPlacingOrder.collectAsStateWithLifecycle()
    val placeOrderError by viewModel.placeOrderError.collectAsStateWithLifecycle()
    val placeOrderResult by viewModel.placeOrderResult.collectAsStateWithLifecycle()
    val isCancellingOrder by viewModel.isCancellingOrder.collectAsStateWithLifecycle()
    val orderCancelled by viewModel.orderCancelled.collectAsStateWithLifecycle()
    var showCancelConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CartEvent.ActionFailed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.btn_cart), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToProducts) {
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
                state.isLoading && state.items.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.errorMessage != null ->
                    ErrorWithRetry(
                        message = state.errorMessage.orEmpty(),
                        onRetry = viewModel::loadCart,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )

                state.items.isEmpty() ->
                    Text(
                        text = stringResource(R.string.empty_cart),
                        modifier = Modifier.align(Alignment.Center),
                    )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            CartItemRow(
                                item = item,
                                onQuantityChange = { newQuantity -> viewModel.updateQuantity(item.id, newQuantity) },
                                onRemove = { viewModel.removeItem(item.id) },
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.label_cart_total, PriceFormatter.format(state.total)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            if (placeOrderError != null) {
                                Text(
                                    text = placeOrderError.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }

                            Button(
                                onClick = viewModel::placeOrder,
                                enabled = state.items.isNotEmpty() && !isPlacingOrder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .height(48.dp),
                            ) {
                                if (isPlacingOrder) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Text(stringResource(R.string.btn_place_order), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val orderResult = placeOrderResult
    when {
        orderCancelled -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.title_order_cancelled)) },
            confirmButton = {
                TextButton(onClick = onBackToProducts) {
                    Text(stringResource(R.string.btn_back_to_product_list))
                }
            },
        )

        showCancelConfirm && orderResult != null -> AlertDialog(
            onDismissRequest = { if (!isCancellingOrder) showCancelConfirm = false },
            title = { Text(stringResource(R.string.btn_cancel_order)) },
            text = { Text(stringResource(R.string.msg_cancel_order_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.cancelOrder(orderResult.orderId) },
                    enabled = !isCancellingOrder,
                ) {
                    Text(stringResource(R.string.btn_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelConfirm = false },
                    enabled = !isCancellingOrder,
                ) {
                    Text(stringResource(R.string.btn_no))
                }
            },
        )

        orderResult != null -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.title_order_placed)) },
            text = {
                Column {
                    Text(stringResource(R.string.label_order_id_value, orderResult.orderId.toString()))
                    Text(
                        text = stringResource(R.string.label_order_total_value, PriceFormatter.format(orderResult.totalPrice)),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.consumePlaceOrderResult()
                    onBackToProducts()
                }) {
                    Text(stringResource(R.string.btn_back_to_product_list))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onEditOrder(orderResult.orderId) }) {
                        Text(stringResource(R.string.btn_edit_order))
                    }
                    TextButton(onClick = { showCancelConfirm = true }) {
                        Text(stringResource(R.string.btn_cancel_order))
                    }
                }
            },
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onQuantityChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val cleanQty = PriceFormatter.formatQuantity(item.quantity)
    var quantityText by remember(item.id, item.quantity) { mutableStateOf(cleanQty) }
    val unitLabel = item.productDetail.unitLabel?.takeIf { it.isNotBlank() } ?: "\u0648\u0627\u062d\u062f"

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbnailUrl = item.productDetail.imageUrls.firstOrNull()
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = item.productDetail.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = item.productDetail.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                val quantityChanged = quantityText.trim() != cleanQty

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("${stringResource(R.string.label_cart_quantity)} \u0628\u0631 \u062d\u0633\u0628 $unitLabel") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (quantityChanged) onQuantityChange(quantityText) },
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    if (quantityChanged) {
                        TextButton(
                            onClick = { onQuantityChange(quantityText) },
                            modifier = Modifier.padding(start = 4.dp),
                        ) {
                            Text(stringResource(R.string.btn_apply_quantity))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.label_cart_item_subtotal, PriceFormatter.format(item.subtotal)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_remove_from_cart),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
