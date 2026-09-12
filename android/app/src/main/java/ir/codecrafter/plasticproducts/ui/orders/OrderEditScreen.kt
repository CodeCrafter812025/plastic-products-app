package ir.codecrafter.plasticproducts.ui.orders

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.OrderItem
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry

@Composable
fun OrderEditScreen(
    onBackToProducts: () -> Unit,
    viewModel: OrderEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSavedSuccessfully by viewModel.isSavedSuccessfully.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OrderEditEvent.ActionFailed -> snackbarHostState.showSnackbar(event.message)
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
                state.isLoading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.errorMessage != null ->
                    ErrorWithRetry(
                        message = state.errorMessage.orEmpty(),
                        onRetry = viewModel::loadOrder,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )

                state.items.isEmpty() ->
                    Text(
                        text = stringResource(R.string.empty_order_items),
                        modifier = Modifier.align(Alignment.Center),
                    )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.product }) { item ->
                            OrderEditItemRow(
                                item = item,
                                onQuantityChange = { newQuantity -> viewModel.updateItemQuantity(item.product, newQuantity) },
                                onRemove = { viewModel.removeItem(item.product) },
                            )
                        }
                    }

                    Button(
                        onClick = viewModel::saveChanges,
                        enabled = state.items.isNotEmpty() && !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(2.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.btn_save_order_changes))
                        }
                    }
                }
            }
        }
    }

    if (isSavedSuccessfully) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.msg_order_items_updated)) },
            confirmButton = {
                TextButton(onClick = onBackToProducts) {
                    Text(stringResource(R.string.btn_back_to_product_list))
                }
            },
        )
    }
}

@Composable
private fun OrderEditItemRow(
    item: OrderItem,
    onQuantityChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var quantityText by remember(item.product, item.quantity) { mutableStateOf(item.quantity) }
    val quantityChanged = quantityText.trim() != item.quantity.trim()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.productDetail.title, style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(stringResource(R.string.label_cart_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        // A second, optional path to the same onQuantityChange call the
                        // "اعمال" button below triggers — kept for anyone who does use the
                        // keyboard's Done action, not a replacement for the button.
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

                TextButton(onClick = onRemove, modifier = Modifier.padding(start = 4.dp)) {
                    Text(stringResource(R.string.btn_remove_from_cart))
                }
            }
        }
    }
}
