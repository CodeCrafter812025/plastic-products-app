package ir.codecrafter.plasticproducts.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.util.PersianDateFormatter
import ir.codecrafter.plasticproducts.util.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrderListScreen(
    onOrderClick: (Int) -> Unit,
    onBackClick: () -> Unit = {},
    viewModel: BuyerOrderListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.btn_my_orders),
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
    ) { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.isLoading && state.orders.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.errorMessage != null ->
                    ErrorWithRetry(
                        message = state.errorMessage.orEmpty(),
                        onRetry = viewModel::loadOrders,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )

                state.orders.isEmpty() ->
                    Text(
                        text = stringResource(R.string.empty_buyer_orders),
                        modifier = Modifier.align(Alignment.Center),
                    )

                else -> LazyVerticalGridOrList(
                    orders = state.orders,
                    onOrderClick = onOrderClick,
                )
            }
        }
    }
}

@Composable
private fun LazyVerticalGridOrList(
    orders: List<Order>,
    onOrderClick: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(orders, key = { it.id }) { order ->
            BuyerOrderRow(order = order, onClick = { onOrderClick(order.id) })
        }
    }
}

@Composable
private fun BuyerOrderRow(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.label_order_id_value, order.id.toString()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.label_order_status_value, statusLabel(order.status)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.label_order_total_value, PriceFormatter.format(order.totalPrice)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(
                    R.string.label_order_created_at_value,
                    PersianDateFormatter.toJalaliDate(order.createdAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun statusLabel(status: String): String = when (status) {
    "pending" -> stringResource(R.string.status_pending)
    "assigned" -> stringResource(R.string.status_assigned)
    "loading" -> stringResource(R.string.status_loading)
    "delivered" -> stringResource(R.string.status_delivered)
    "cancelled" -> stringResource(R.string.status_cancelled)
    else -> status
}
