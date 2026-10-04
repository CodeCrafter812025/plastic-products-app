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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.LowStockProduct
import ir.codecrafter.plasticproducts.data.model.SignupCount
import ir.codecrafter.plasticproducts.data.model.TopProduct
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry
import ir.codecrafter.plasticproducts.ui.common.JalaliDatePickerDialog
import ir.codecrafter.plasticproducts.util.PersianDateFormatter
import ir.codecrafter.plasticproducts.util.PriceFormatter
import java.util.Locale

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
                        state.orderCountsError != null -> SectionError(
                            message = state.orderCountsError.orEmpty(),
                            onRetry = viewModel::loadOrderCounts,
                        )
                        else -> OrderStatusGrid(counts = state.orderCounts)
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
                        state.topProductsError != null -> SectionError(
                            message = state.topProductsError.orEmpty(),
                            onRetry = viewModel::loadTopProducts,
                        )
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
                        state.lowStockError != null -> SectionError(
                            message = state.lowStockError.orEmpty(),
                            onRetry = viewModel::loadLowStock,
                        )
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
                                state.signupsError != null -> SectionError(
                                    message = state.signupsError.orEmpty(),
                                    onRetry = viewModel::loadSignups,
                                )
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
private fun OrderStatusGrid(counts: Map<String, Int>) {
    // طراحی مدرن کارت‌های رنگی برای داشبورد ادمین
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusCard(
                label = "\u062b\u0628\u062a \u0634\u062f\u0647",
                count = counts["pending"] ?: 0,
                bgColor = Color(0xFFFFF8E1),
                textColor = Color(0xFFF57F17),
                modifier = Modifier.weight(1f)
            )
            StatusCard(
                label = "\u062a\u062e\u0635\u06cc\u0635 \u06cc\u0627\u0641\u062a\u0647",
                count = counts["assigned"] ?: 0,
                bgColor = Color(0xFFE3F2FD),
                textColor = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusCard(
                label = "\u062f\u0631 \u062d\u0627\u0644 \u0627\u0631\u0633\u0627\u0644",
                count = counts["loading"] ?: 0,
                bgColor = Color(0xFFF3E5F5),
                textColor = Color(0xFF6A1B9A),
                modifier = Modifier.weight(1f)
            )
            StatusCard(
                label = "\u062a\u062d\u0648\u06cc\u0644 \u0634\u062f\u0647",
                count = counts["delivered"] ?: 0,
                bgColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusCard(
                label = "\u0644\u063a\u0648 \u0634\u062f\u0647",
                count = counts["cancelled"] ?: 0,
                bgColor = Color(0xFFFFEBEE),
                textColor = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.weight(1f)) // Empty box to keep grid aligned
        }
    }
}

@Composable
private fun StatusCard(label: String, count: Int, bgColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DashboardSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.padding(top = 12.dp)) {
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
private fun SectionError(message: String, onRetry: () -> Unit) {
    ErrorWithRetry(message = message, onRetry = onRetry)
}

@Composable
private fun SectionEmpty() {
    Text(text = stringResource(R.string.msg_no_data))
}

@Composable
private fun TopProductRow(product: TopProduct) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = product.productTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(
            // رفع مشکل فرمت اعشاری و اضافه کردن هزارگان
            text = stringResource(R.string.label_quantity_sold_value, PriceFormatter.formatQuantity(product.totalQuantitySold.toString())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LowStockRow(product: LowStockProduct) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = product.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(
            // رفع مشکل فرمت اعشاری
            text = stringResource(R.string.label_product_stock_value, PriceFormatter.formatQuantity(product.stock.toString())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
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
            ErrorWithRetry(
                message = errorMessage,
                onRetry = onShowClick,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else if (totalRevenue != null) {
            Text(
                // نمایش سه‌رقم سه‌رقم درآمد
                text = stringResource(R.string.label_revenue_value, PriceFormatter.format(totalRevenue)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp),
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

private fun isoToJalaliMonth(isoDate: String): Pair<Int, Int>? = try {
    val parts = isoDate.substring(0, 10).split("-")
    val (jy, jm, _) = PersianDateFormatter.gregorianToJalali(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    jy to jm
} catch (e: Exception) {
    null
}

private fun jalaliToIsoDate(jy: Int, jm: Int, jd: Int): String {
    val (gy, gm, gd) = PersianDateFormatter.jalaliToGregorian(jy, jm, jd)
    return "%04d-%02d-%02d".format(gy, gm, gd)
}
