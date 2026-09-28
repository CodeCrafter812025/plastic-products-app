package ir.codecrafter.plasticproducts.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
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
import ir.codecrafter.plasticproducts.ui.cart.CartViewModel
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.ui.common.SkeletonBox
import ir.codecrafter.plasticproducts.ui.notifications.NotificationListViewModel

@Composable
fun ProductListScreen(
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit,
    onMyOrdersClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    notificationListViewModel: NotificationListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val notificationState by notificationListViewModel.uiState.collectAsStateWithLifecycle()
    var showMoreFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cartViewModel.loadCart()
        notificationListViewModel.loadNotifications()
        viewModel.retryLoad()
    }

    Scaffold { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onNotificationsClick) {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.btn_notifications),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    val unreadCount = notificationState.notifications.count { !it.isRead }
                    BadgedBox(badge = {
                        if (unreadCount > 0) {
                            Badge { Text(unreadCount.toString()) }
                        }
                    }) {
                        Text(stringResource(R.string.btn_notifications))
                    }
                }
                TextButton(onClick = onMyOrdersClick) {
                    IconButton(onClick = onMyOrdersClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = stringResource(R.string.btn_my_orders),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.btn_my_orders))
                }
                TextButton(onClick = onCartClick) {
                    IconButton(onClick = onCartClick) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = stringResource(R.string.btn_cart),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    BadgedBox(badge = {
                        if (cartState.items.isNotEmpty()) {
                            Badge { Text(cartState.items.size.toString()) }
                        }
                    }) {
                        Text(stringResource(R.string.btn_cart))
                    }
                }
                TextButton(onClick = onProfileClick) {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.btn_profile),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.btn_profile))
                }
            }

            OutlinedTextField(
                value = state.searchText,
                onValueChange = viewModel::onSearchTextChange,
                label = { Text(stringResource(R.string.label_product_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // نوار افقی انتخاب دسته‌بندی‌های ۷گانه کاتالوگ + فیلتر پرفروش‌ها
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.filter.category == null && state.filter.isBestseller != true,
                    onClick = {
                        viewModel.onCategoryChange(null)
                        viewModel.onBestsellerToggle(false)
                    },
                    label = { Text("\u0647\u0645\u0647 \u062f\u0633\u062a\u0647\u200c\u0647\u0627") },
                )
                FilterChip(
                    selected = state.filter.isBestseller == true,
                    onClick = { viewModel.onBestsellerToggle(state.filter.isBestseller != true) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text("\u067e\u0631\u0641\u0631\u0648\u0634\u200c\u0647\u0627") },
                )
                ProductCategory.ALL.forEach { cat ->
                    FilterChip(
                        selected = state.filter.category == cat,
                        onClick = {
                            viewModel.onCategoryChange(if (state.filter.category == cat) null else cat)
                        },
                        label = { Text(cat) },
                    )
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.filter_in_stock_only), modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.filter.inStock == true,
                            onCheckedChange = viewModel::onInStockOnlyChange,
                        )
                    }
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
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
            ) {
                when {
                    state.isLoading && state.products.isEmpty() ->
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(6) {
                                ProductCardSkeleton()
                            }
                        }

                    state.errorMessage != null ->
                        ErrorWithRetry(
                            message = state.errorMessage.orEmpty(),
                            onRetry = viewModel::retryLoad,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                        )

                    state.products.isEmpty() ->
                        Text(
                            text = stringResource(R.string.empty_products_list),
                            modifier = Modifier.align(Alignment.Center),
                        )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.products, key = { it.id }) { product ->
                            ProductCard(product, onClick = { onProductClick(product.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            val thumbnailUrl = product.imageUrls.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
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
                if (product.isBestseller) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart),
                    ) {
                        Text(
                            text = "\u2605 \u067e\u0631\u0641\u0631\u0648\u0634",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!product.brand.isNullOrBlank()) {
                    Text(
                        text = "\u0628\u0631\u0646\u062f: ${product.brand}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else if (!product.subCategory.isNullOrBlank()) {
                    Text(
                        text = "\u06a9\u0627\u0631\u0628\u0631\u062f: ${product.subCategory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                val unitText = product.unitLabel?.takeIf { it.isNotBlank() } ?: "\u0648\u0627\u062d\u062f"
                Text(
                    text = "${stringResource(R.string.product_price_toman, product.price)} / $unitText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f),
                shape = RoundedCornerShape(0.dp),
            )

            Column(modifier = Modifier.padding(12.dp)) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp),
                    shape = RoundedCornerShape(4.dp),
                )
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(4.dp),
                )
            }
        }
    }
}
