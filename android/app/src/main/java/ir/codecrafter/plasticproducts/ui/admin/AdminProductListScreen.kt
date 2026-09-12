package ir.codecrafter.plasticproducts.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Product

@Composable
fun AdminProductListScreen(
    onAddProductClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onUsersClick: () -> Unit,
    onOrdersClick: () -> Unit,
    viewModel: AdminProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AdminProductListEvent.ActionFailed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // This composable's content is torn down and rebuilt fresh every time the user
    // navigates away and back (e.g. returning here after saving the create/edit
    // form), the same way ProductListScreen re-runs cartViewModel.loadCart() — so
    // this reruns loadProducts() on every return, keeping the list from going stale
    // after a create/edit/toggle/delete made on another screen.
    LaunchedEffect(Unit) { viewModel.loadProducts() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddProductClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.btn_add_new_product))
                }
                TextButton(onClick = onUsersClick) {
                    Text(stringResource(R.string.btn_users))
                }
                TextButton(onClick = onOrdersClick) {
                    Text(stringResource(R.string.btn_orders))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
            ) {
                when {
                    state.isLoading && state.products.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.errorMessage != null ->
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                        )

                    state.products.isEmpty() ->
                        Text(
                            text = stringResource(R.string.empty_products_list),
                            modifier = Modifier.align(Alignment.Center),
                        )

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Only one item type in this LazyColumn today, so a key collision
                        // isn't possible yet — prefixed anyway to match the standing habit.
                        items(state.products, key = { "product_${it.id}" }) { product ->
                            AdminProductRow(
                                product = product,
                                isActionInProgress = state.actionInProgressId == product.id,
                                onClick = { onProductClick(product.id) },
                                onToggleActive = { viewModel.toggleActive(product.id) },
                                onRequestDelete = { productPendingDelete = product },
                            )
                        }
                    }
                }
            }
        }
    }

    productPendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text(stringResource(R.string.btn_delete_product)) },
            text = { Text(stringResource(R.string.msg_delete_product_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProduct(product.id)
                        productPendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.btn_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) {
                    Text(stringResource(R.string.btn_no))
                }
            },
        )
    }
}

@Composable
private fun AdminProductRow(
    product: Product,
    isActionInProgress: Boolean,
    onClick: () -> Unit,
    onToggleActive: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .let { if (!product.isActive) it.background(MaterialTheme.colorScheme.surfaceVariant) else it },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val thumbnailUrl = product.imageUrls.firstOrNull()
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (thumbnailUrl != null) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = product.title,
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
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (product.isActive) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = stringResource(R.string.product_price_toman, product.price),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!product.isActive) {
                        Text(
                            text = stringResource(R.string.label_product_inactive_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onToggleActive,
                    enabled = !isActionInProgress,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            if (product.isActive) {
                                stringResource(R.string.btn_deactivate_product)
                            } else {
                                stringResource(R.string.btn_activate_product)
                            }
                        )
                    }
                }
                TextButton(
                    onClick = onRequestDelete,
                    enabled = !isActionInProgress,
                ) {
                    Text(
                        text = stringResource(R.string.btn_delete_product),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
