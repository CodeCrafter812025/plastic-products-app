package ir.codecrafter.plasticproducts.ui.products

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import ir.codecrafter.plasticproducts.data.model.ProductCategory
import ir.codecrafter.plasticproducts.data.model.ProductQuality
import ir.codecrafter.plasticproducts.ui.cart.CartViewModel
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.ui.common.SkeletonBox
import ir.codecrafter.plasticproducts.ui.notifications.NotificationListViewModel
import ir.codecrafter.plasticproducts.util.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        cartViewModel.loadCart()
        notificationListViewModel.loadNotifications()
        viewModel.retryLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "\u067e\u062e\u0634 \u0645\u0633\u062a\u0642\u06cc\u0645 \u0645\u062d\u0635\u0648\u0644\u0627\u062a \u067e\u0644\u0627\u0633\u062a\u06cc\u06a9\u06cc",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "\u062e\u0631\u06cc\u062f \u0628\u06cc\u200c\u0648\u0627\u0633\u0637\u0647 \u0627\u0632 \u06a9\u0627\u0631\u062e\u0627\u0646\u0647",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                },
                actions = {
                    // دکمه تماس مستقیم با واحد فروش کارخانه
                    IconButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09035032922"))
                            context.startActivity(dialIntent)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "\u062a\u0645\u0627\u0633 \u0628\u0627 \u0648\u0627\u062d\u062f \u0641\u0631\u0648\u0634",
                        )
                    }

                    val unreadCount = notificationState.notifications.count { !it.isRead }
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(badge = {
                            if (unreadCount > 0) {
                                Badge { Text(unreadCount.toString()) }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.btn_notifications),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    label = { Text("\u06a9\u0627\u062a\u0627\u0644\u0648\u06af") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onCartClick,
                    icon = {
                        BadgedBox(badge = {
                            if (cartState.items.isNotEmpty()) {
                                Badge { Text(cartState.items.size.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(R.string.btn_cart))
                        }
                    },
                    label = { Text(stringResource(R.string.btn_cart)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onMyOrdersClick,
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = stringResource(R.string.btn_my_orders)) },
                    label = { Text(stringResource(R.string.btn_my_orders)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onProfileClick,
                    icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.btn_profile)) },
                    label = { Text(stringResource(R.string.btn_profile)) },
                )
            }
        },
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            OutlinedTextField(
                value = state.searchText,
                onValueChange = viewModel::onSearchTextChange,
                label = { Text(stringResource(R.string.label_product_search)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
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
                    label = { Text("\u0647\u0645\u0647 \u0645\u062d\u0635\u0648\u0644\u0627\u062a") },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showMoreFilters = !showMoreFilters }) {
                    Text(
                        if (showMoreFilters) {
                            stringResource(R.string.btn_close_more_filters)
                        } else {
                            stringResource(R.string.btn_more_filters)
                        }
                    )
                }
            }

            if (showMoreFilters) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
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

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.groupedProducts.isEmpty() ->
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

                    state.groupedProducts.isEmpty() ->
                        Text(
                            text = stringResource(R.string.empty_products_list),
                            modifier = Modifier.align(Alignment.Center),
                        )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(state.groupedProducts, key = { it.representative.id }) { group ->
                            ProductGroupCard(
                                group = group,
                                onClick = { onProductClick(group.representative.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGroupCard(group: ProductGroupItem, onClick: () -> Unit) {
    val product = group.representative
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column {
            val thumbnailUrl = product.imageUrls.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = group.familyTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp),
                    )
                }

                if (group.hasBestseller) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(bottomStart = 10.dp),
                        modifier = Modifier.align(Alignment.TopEnd),
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

                if (group.variantCount > 1) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(topEnd = 10.dp),
                        modifier = Modifier.align(Alignment.BottomStart),
                    ) {
                        Text(
                            text = "${group.variantCount} \u0633\u0627\u06cc\u0632 / \u0645\u062f\u0644",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = group.familyTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!group.brandsSummary.isNullOrBlank()) {
                    Text(
                        text = "\u0628\u0631\u0646\u062f: ${group.brandsSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else if (!group.subCategoriesSummary.isNullOrBlank()) {
                    Text(
                        text = "\u06a9\u0627\u0631\u0628\u0631\u062f: ${group.subCategoriesSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                val formattedMinPrice = PriceFormatter.format(group.minPrice)
                val priceLabel = if (group.minPrice != group.maxPrice) {
                    "\u0627\u0632 $formattedMinPrice \u062a\u0648\u0645\u0627\u0646"
                } else {
                    "$formattedMinPrice \u062a\u0648\u0645\u0627\u0646"
                }

                Text(
                    text = priceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f),
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
