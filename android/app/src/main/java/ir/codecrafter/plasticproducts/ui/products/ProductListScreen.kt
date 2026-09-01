package ir.codecrafter.plasticproducts.ui.products

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.model.ProductQuality

@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMoreFilters by remember { mutableStateOf(false) }

    Scaffold { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            OutlinedTextField(
                value = state.searchText,
                onValueChange = viewModel::onSearchTextChange,
                label = { Text(stringResource(R.string.label_product_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter.quality == null,
                    onClick = { viewModel.onQualityChange(null) },
                    label = { Text(stringResource(R.string.filter_quality_all)) },
                )
                FilterChip(
                    selected = state.filter.quality == ProductQuality.PRIMARY,
                    onClick = { viewModel.onQualityChange(ProductQuality.PRIMARY) },
                    label = { Text(stringResource(R.string.filter_quality_primary)) },
                )
                FilterChip(
                    selected = state.filter.quality == ProductQuality.RECYCLED,
                    onClick = { viewModel.onQualityChange(ProductQuality.RECYCLED) },
                    label = { Text(stringResource(R.string.filter_quality_recycled)) },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.filter_in_stock_only), modifier = Modifier.weight(1f))
                Switch(
                    checked = state.filter.inStock == true,
                    onCheckedChange = viewModel::onInStockOnlyChange,
                )
            }

            TextButton(onClick = { showMoreFilters = !showMoreFilters }) {
                Text(
                    if (showMoreFilters) {
                        stringResource(R.string.btn_close_more_filters)
                    } else {
                        stringResource(R.string.btn_more_filters)
                    }
                )
            }

            if (showMoreFilters) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = state.filter.minPrice.orEmpty(),
                        onValueChange = viewModel::onMinPriceChange,
                        label = { Text(stringResource(R.string.label_min_price)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.filter.maxPrice.orEmpty(),
                        onValueChange = viewModel::onMaxPriceChange,
                        label = { Text(stringResource(R.string.label_max_price)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
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
                        items(state.products, key = { it.id }) { product ->
                            ProductCard(product)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: navigate to a product detail screen once it exists.
            },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbnailUrl = product.imageUrls.firstOrNull()
            Box(
                modifier = Modifier
                    .size(64.dp)
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
                Text(text = product.title, style = MaterialTheme.typography.titleMedium)
                Text(text = product.quality, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = stringResource(R.string.product_price_toman, product.price),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
