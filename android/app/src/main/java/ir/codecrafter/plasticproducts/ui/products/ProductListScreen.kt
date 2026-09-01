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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
                label = { Text("Ø¬Ø³ØªØ¬ÙˆÛŒ Ù…Ø­ØµÙˆÙ„") },
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
                    label = { Text("Ù‡Ù…Ù‡") },
                )
                FilterChip(
                    selected = state.filter.quality == ProductQuality.PRIMARY,
                    onClick = { viewModel.onQualityChange(ProductQuality.PRIMARY) },
                    label = { Text("Ø§ÙˆÙ„ÛŒÙ‡") },
                )
                FilterChip(
                    selected = state.filter.quality == ProductQuality.RECYCLED,
                    onClick = { viewModel.onQualityChange(ProductQuality.RECYCLED) },
                    label = { Text("Ø¨Ø§Ø²ÛŒØ§ÙØªÛŒ") },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ÙÙ‚Ø· Ú©Ø§Ù„Ø§Ù‡Ø§ÛŒ Ù…ÙˆØ¬ÙˆØ¯", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.filter.inStock == true,
                    onCheckedChange = viewModel::onInStockOnlyChange,
                )
            }

            TextButton(onClick = { showMoreFilters = !showMoreFilters }) {
                Text(if (showMoreFilters) "Ø¨Ø³ØªÙ† ÙÛŒÙ„ØªØ± Ø¨ÛŒØ´ØªØ±" else "ÙÛŒÙ„ØªØ± Ø¨ÛŒØ´ØªØ±")
            }

            if (showMoreFilters) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = state.filter.minPrice.orEmpty(),
                        onValueChange = viewModel::onMinPriceChange,
                        label = { Text("Ø­Ø¯Ø§Ù‚Ù„ Ù‚ÛŒÙ…Øª") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.filter.maxPrice.orEmpty(),
                        onValueChange = viewModel::onMaxPriceChange,
                        label = { Text("Ø­Ø¯Ø§Ú©Ø«Ø± Ù‚ÛŒÙ…Øª") },
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
                            text = "Ù…Ø­ØµÙˆÙ„ÛŒ ÛŒØ§ÙØª Ù†Ø´Ø¯",
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
                Text(text = "${product.price} ØªÙˆÙ…Ø§Ù†", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
