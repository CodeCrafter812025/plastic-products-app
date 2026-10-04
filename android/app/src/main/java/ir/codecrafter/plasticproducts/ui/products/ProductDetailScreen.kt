package ir.codecrafter.plasticproducts.ui.products

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.model.ProductCategory
import ir.codecrafter.plasticproducts.data.model.ProductQuality
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.util.PriceFormatter
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onBackToList: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val addedToCartMessage = stringResource(R.string.msg_added_to_cart)
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProductDetailEvent.AddedToCart -> snackbarHostState.showSnackbar(addedToCartMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.product?.category ?: state.product?.title ?: "\u062c\u0632\u0626\u06cc\u0627\u062a \u0645\u062d\u0635\u0648\u0644",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToList) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back_to_list),
                        )
                    }
                },
                actions = {
                    state.product?.let { product ->
                        val formattedPrice = PriceFormatter.format(product.price)
                        val unitLabel = product.unitLabel?.takeIf { it.isNotBlank() } ?: "\u0648\u0627\u062d\u062f"
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${product.title} - $formattedPrice \u062a\u0648\u0645\u0627\u0646 ($unitLabel)")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.content_description_share_product),
                            )
                        }
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
                    onRetry = viewModel::loadProduct,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                state.product != null -> ProductDetailContent(
                    product = state.product!!,
                    variants = state.variants,
                    quantityInput = state.quantityInput,
                    isAddingToCart = state.isAddingToCart,
                    addToCartError = state.addToCartError,
                    onSelectVariant = viewModel::onSelectVariant,
                    onQuantityInputChange = viewModel::onQuantityInputChange,
                    onIncrementQuantity = viewModel::incrementQuantity,
                    onDecrementQuantity = viewModel::decrementQuantity,
                    onAddToCart = viewModel::addToCart,
                )

                else -> ErrorWithRetry(
                    message = stringResource(R.string.msg_loading_failed_generic),
                    onRetry = viewModel::loadProduct,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailContent(
    product: Product,
    variants: List<Product>,
    quantityInput: String,
    isAddingToCart: Boolean,
    addToCartError: String?,
    onSelectVariant: (Product) -> Unit,
    onQuantityInputChange: (String) -> Unit,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    onAddToCart: () -> Unit,
) {
    val formattedPrice = PriceFormatter.format(product.price)
    val unitLabel = product.unitLabel?.takeIf { it.isNotBlank() } ?: "\u0648\u0627\u062d\u062f"
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ImageGallery(imageUrls = product.imageUrls, contentDescription = product.title)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (product.isBestseller) {
                    AssistChip(
                        onClick = {},
                        label = { Text("\u2605 \u067e\u0631\u0641\u0631\u0648\u0634") },
                    )
                }
                if (!product.subCategory.isNullOrBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("\u06a9\u0627\u0631\u0628\u0631\u062f: ${product.subCategory}") },
                    )
                }
                if (!product.brand.isNullOrBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("\u0628\u0631\u0646\u062f: ${product.brand}") },
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(qualityLabel(product.quality)) },
                )
            }

            if (variants.size > 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "\u0627\u0646\u062a\u062e\u0627\u0628 \u0633\u0627\u06cc\u0632\u060c \u0645\u062f\u0644 \u06cc\u0627 \u0628\u0631\u0646\u062f (${variants.size} \u0645\u062f\u0644 \u0645\u0648\u062c\u0648\u062f):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            OutlinedTextField(
                                value = variantFullLabel(product),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("\u0645\u062f\u0644 \u0627\u0646\u062a\u062e\u0627\u0628\u200c\u0634\u062f\u0647") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                            ) {
                                variants.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = variantFullLabel(item),
                                                    fontWeight = if (item.id == product.id) FontWeight.Bold else FontWeight.Normal,
                                                )
                                                Text(
                                                    text = "${PriceFormatter.format(item.price)} \u062a\u0648\u0645\u0627\u0646 / ${item.unitLabel ?: ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectVariant(item)
                                            dropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            variants.forEach { item ->
                                FilterChip(
                                    selected = item.id == product.id,
                                    onClick = { onSelectVariant(item) },
                                    label = { Text(variantShortLabel(item)) },
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\u0631\u0627\u0647\u0646\u0645\u0627\u06cc \u0648\u0627\u062d\u062f \u0634\u0645\u0627\u0631\u0634 \u0648 \u062e\u0631\u06cc\u062f",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "\u0648\u0627\u062d\u062f \u067e\u0627\u06cc\u0647 \u0634\u0645\u0627\u0631\u0634: $unitLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "\u0642\u06cc\u0645\u062a \u0647\u0631 $unitLabel: $formattedPrice \u062a\u0648\u0645\u0627\u0646",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (!product.packagingInfo.isNullOrBlank()) {
                        Text(
                            text = "\u0646\u062d\u0648\u0647 \u0639\u0631\u0636\u0647 \u0648 \u0628\u0633\u062a\u0647\u200c\u0628\u0646\u062f\u06cc: ${product.packagingInfo}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.label_stock_status_value, stockStatusLabel(product.stock)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )

            val quickPresets = remember(product.category, product.unitLabel) {
                when {
                    product.category == ProductCategory.NAYLEX -> listOf(
                        "1" to "1 \u06a9\u06cc\u0644\u0648\u06cc\u06cc",
                        "5" to "5 \u06a9\u06cc\u0644\u0648\u06cc\u06cc",
                        "25" to "1 \u06a9\u06cc\u0633\u0647 (25 \u06a9\u06cc\u0644\u0648)",
                        "50" to "2 \u06a9\u06cc\u0633\u0647 (50 \u06a9\u06cc\u0644\u0648)",
                    )
                    product.category == ProductCategory.CUP -> listOf(
                        "1" to "1 \u067e\u06a9 (500 \u0639\u062f\u062f)",
                        "5" to "5 \u067e\u06a9 (2500 \u0639\u062f\u062f)",
                        "10" to "1 \u06a9\u0627\u0631\u062a\u0646 (10 \u067e\u06a9)",
                        "20" to "2 \u06a9\u0627\u0631\u062a\u0646 (20 \u067e\u06a9)",
                    )
                    else -> listOf(
                        "1" to "1 $unitLabel",
                        "5" to "5 $unitLabel",
                        "10" to "10 $unitLabel",
                        "20" to "20 $unitLabel",
                    )
                }
            }

            Text(
                text = "\u0627\u0646\u062a\u062e\u0627\u0628 \u0633\u0631\u06cc\u0639 \u0645\u0642\u062f\u0627\u0631 / \u0628\u0633\u062a\u0647",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                quickPresets.forEach { (qtyValue, label) ->
                    FilterChip(
                        selected = quantityInput.trim() == qtyValue,
                        onClick = { onQuantityInputChange(qtyValue) },
                        label = { Text(label) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(onClick = onIncrementQuantity) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "\u0627\u0641\u0632\u0627\u06cc\u0634")
                }

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = onQuantityInputChange,
                    label = { Text("\u062a\u0639\u062f\u0627\u062f \u0628\u0631 \u062d\u0633\u0628 $unitLabel") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )

                FilledTonalIconButton(onClick = onDecrementQuantity) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "\u06a9\u0627\u0647\u0634")
                }
            }

            Button(
                onClick = onAddToCart,
                enabled = quantityInput.isNotBlank() && !isAddingToCart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(50.dp),
            ) {
                if (isAddingToCart) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = stringResource(R.string.btn_add_to_cart),
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.btn_add_to_cart),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            val qtyDecimal = quantityInput.trim().toBigDecimalOrNull()
            val unitPriceDecimal = product.price.toBigDecimalOrNull()
            if (qtyDecimal != null && unitPriceDecimal != null && qtyDecimal > BigDecimal.ZERO) {
                val totalRaw = (qtyDecimal * unitPriceDecimal).toPlainString()
                val formattedTotal = PriceFormatter.format(totalRaw)
                val conversionHint = PriceFormatter.buildUnitConversionHint(product, qtyDecimal)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "\u062e\u0644\u0627\u0635\u0647 \u0633\u0641\u0627\u0631\u0634 \u0634\u0645\u0627: ${quantityInput.trim()} $unitLabel \u0627\u0632 \u00ab${product.title}\u00bb",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (conversionHint != null) {
                            Text(
                                text = "\u2728 $conversionHint",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Text(
                            text = "\u0645\u0628\u0644\u063a \u06a9\u0644: $formattedTotal \u062a\u0648\u0645\u0627\u0646",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            if (addToCartError != null) {
                Text(
                    text = addToCartError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun variantShortLabel(item: Product): String {
    val category = item.category?.trim().orEmpty()
    var shortName = if (category.isNotEmpty() && item.title.startsWith(category)) {
        item.title.removePrefix(category).trim()
    } else {
        item.title
    }
    if (shortName.isBlank()) {
        shortName = item.subCategory ?: item.brand ?: item.title
    }
    val star = if (item.isBestseller) " \u2605" else ""
    return "$shortName$star"
}

private fun variantFullLabel(item: Product): String {
    val star = if (item.isBestseller) " (\u2605 \u067e\u0631\u0641\u0631\u0648\u0634)" else ""
    val sub = item.subCategory?.takeIf { it.isNotBlank() && !item.title.contains(it) }?.let { " - $it" }.orEmpty()
    return "${item.title}$sub$star"
}

@Composable
private fun ImageGallery(imageUrls: List<String>, contentDescription: String) {
    if (imageUrls.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp),
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) { page ->
        var scale by remember { mutableFloatStateOf(1f) }
        AsyncImage(
            model = imageUrls[page],
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 3f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { scale = 1f })
                },
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
