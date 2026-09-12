package ir.codecrafter.plasticproducts.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.LowStockProduct
import ir.codecrafter.plasticproducts.data.model.SignupCount
import ir.codecrafter.plasticproducts.data.model.TopProduct
import ir.codecrafter.plasticproducts.ui.common.JalaliDatePickerDialog
import ir.codecrafter.plasticproducts.util.PersianDateFormatter

private val ORDER_STATUSES = listOf("pending", "assigned", "loading", "delivered", "cancelled")

@Composable
fun AdminDashboardScreen(
    onViewVisitorPerformance: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues: PaddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DashboardSection(title = stringResource(R.string.title_order_status)) {
                    when {
                        state.isLoadingOrderCounts -> SectionLoading()
                        state.orderCountsError != null -> SectionError(state.orderCountsError.orEmpty())
                        else -> Column {
                            ORDER_STATUSES.forEach { status ->
                                Text(
                                    text = "${statusLabel(status)}: ${state.orderCounts[status] ?: 0}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                DashboardSection(title = stringResource(R.string.title_revenue)) {
                    RevenueSection(
                        from = state.revenueFrom,
                        to = state.revenueTo,
                        isLoading = state.isLoadingRevenue,
                        errorMessage = state.revenueError,
                        totalRevenue = state.revenueReport?.totalRevenue,
                        onFromSelected = viewModel::onRevenueFromChange,
                        onToSelected = viewModel::onRevenueToChange,
                        onShowClick = viewModel::loadRevenue,
                    )
                }
            }

            item {
                DashboardSection(title = stringResource(R.string.title_top_products)) {
                    when {
                        state.isLoadingTopProducts -> SectionLoading()
                        state.topProductsError != null -> SectionError(state.topProductsError.orEmpty())
                        state.topProducts.isEmpty() -> SectionEmpty()
                        else -> Column {
                            state.topProducts.forEach { product -> TopProductRow(product) }
                        }
                    }
                }
            }

            item {
                DashboardSection(title = stringResource(R.string.title_low_stock)) {
                    when {
                        state.isLoadingLowStock -> SectionLoading()
                        state.lowStockError != null -> SectionError(state.lowStockError.orEmpty())
                        state.lowStock.isEmpty() -> SectionEmpty()
                        else -> Column {
                            state.lowStock.forEach { product -> LowStockRow(product) }
                        }
                    }
                }
            }

            item {
                DashboardSection(title = stringResource(R.string.title_buyer_signups)) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.signupsPeriod == "day",
                                onClick = { viewModel.onSignupsPeriodChange("day") },
                                label = { Text(stringResource(R.string.label_period_daily)) },
                            )
                            FilterChip(
                                selected = state.signupsPeriod == "week",
                                onClick = { viewModel.onSignupsPeriodChange("week") },
                                label = { Text(stringResource(R.string.label_period_weekly)) },
                            )
                        }
                        Box(modifier = Modifier.padding(top = 8.dp)) {
                            when {
                                state.isLoadingSignups -> SectionLoading()
                                state.signupsError != null -> SectionError(state.signupsError.orEmpty())
                                state.signups.isEmpty() -> SectionEmpty()
                                else -> Column {
                                    state.signups.forEach { entry -> SignupRow(entry) }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onViewVisitorPerformance,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.btn_view_all_visitor_performance))
                }
            }
        }
    }
}

@Composable
private fun DashboardSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Box(modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SectionLoading() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun SectionError(message: String) {
    Text(text = message, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun SectionEmpty() {
    Text(text = stringResource(R.string.msg_no_data))
}

@Composable
private fun TopProductRow(product: TopProduct) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = product.productTitle, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.label_quantity_sold_value, product.totalQuantitySold),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LowStockRow(product: LowStockProduct) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = product.title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.label_product_stock_value, product.stock),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SignupRow(entry: SignupCount) {
    val periodLabel = entry.period?.let { PersianDateFormatter.toJalaliDate(it) } ?: "-"
    Text(
        text = stringResource(R.string.label_signup_count_value, periodLabel, entry.count.toString()),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun RevenueSection(
    from: String?,
    to: String?,
    isLoading: Boolean,
    errorMessage: String?,
    totalRevenue: String?,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onShowClick: () -> Unit,
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f)) {
                Text(from?.let { PersianDateFormatter.toJalaliDate(it) } ?: stringResource(R.string.label_from_date))
            }
            OutlinedButton(onClick = { showToPicker = true }, modifier = Modifier.weight(1f)) {
                Text(to?.let { PersianDateFormatter.toJalaliDate(it) } ?: stringResource(R.string.label_to_date))
            }
        }
        Button(
            onClick = onShowClick,
            enabled = from != null && to != null && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.btn_show_revenue))
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else if (totalRevenue != null) {
            Text(
                text = stringResource(R.string.label_revenue_value, totalRevenue),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (showFromPicker) {
        val initial = from?.let { isoToJalaliMonth(it) }
        JalaliDatePickerDialog(
            initialJy = initial?.first,
            initialJm = initial?.second,
            onDateSelected = { jy, jm, jd -> onFromSelected(jalaliToIsoDate(jy, jm, jd)) },
            onDismiss = { showFromPicker = false },
        )
    }
    if (showToPicker) {
        val initial = to?.let { isoToJalaliMonth(it) }
        JalaliDatePickerDialog(
            initialJy = initial?.first,
            initialJm = initial?.second,
            onDateSelected = { jy, jm, jd -> onToSelected(jalaliToIsoDate(jy, jm, jd)) },
            onDismiss = { showToPicker = false },
        )
    }
}

/** The Jalali (year, month) of a "YYYY-MM-DD..." Gregorian ISO string, or null if it can't be parsed. */
private fun isoToJalaliMonth(isoDate: String): Pair<Int, Int>? = try {
    val parts = isoDate.substring(0, 10).split("-")
    val (jy, jm, _) = PersianDateFormatter.gregorianToJalali(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    jy to jm
} catch (e: Exception) {
    null
}

/** Produces the YYYY-MM-DD Gregorian string that admin-reports/revenue/ expects from a Jalali (jy, jm, jd). */
private fun jalaliToIsoDate(jy: Int, jm: Int, jd: Int): String {
    val (gy, gm, gd) = PersianDateFormatter.jalaliToGregorian(jy, jm, jd)
    return "%04d-%02d-%02d".format(gy, gm, gd)
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
