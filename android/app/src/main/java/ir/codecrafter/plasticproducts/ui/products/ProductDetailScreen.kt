package ir.codecrafter.plasticproducts.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.model.ProductQuality
import java.math.BigDecimal

@Composable
fun ProductDetailScreen(
    onBackToList: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues: PaddingValues ->
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

                state.product != null -> ProductDetailContent(
                    product = state.product!!,
                    onBackToList = onBackToList,
                )
            }
        }
    }
}

@Composable
private fun ProductDetailContent(product: Product, onBackToList: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ImageGallery(imageUrls = product.imageUrls, contentDescription = product.title)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(text = product.title, style = MaterialTheme.typography.headlineSmall)

            AssistChip(
                onClick = {},
                label = { Text(qualityLabel(product.quality)) },
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                text = stringResource(R.string.product_price_toman, product.price),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )

            if (product.color != null) {
                Text(
                    text = stringResource(R.string.label_color_value, product.color),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Text(
                text = stringResource(R.string.label_weight_value, product.weight),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                text = stringResource(R.string.label_stock_status_value, stockStatusLabel(product.stock)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (product.description.isNotBlank()) {
                Text(
                    text = stringResource(R.string.label_description),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            TextButton(onClick = onBackToList, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.btn_back_to_list))
            }
        }
    }
}

@Composable
private fun ImageGallery(imageUrls: List<String>, contentDescription: String) {
    if (imageUrls.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) { page ->
        AsyncImage(
            model = imageUrls[page],
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun qualityLabel(quality: String): String = when (quality) {
    ProductQuality.PRIMARY -> stringResource(R.string.filter_quality_primary)
    ProductQuality.RECYCLED -> stringResource(R.string.filter_quality_recycled)
    else -> quality
}

@Composable
private fun stockStatusLabel(stock: String): String {
    val inStock = stock.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } ?: false
    return stringResource(
        if (inStock) R.string.stock_status_available else R.string.stock_status_unavailable,
    )
}
