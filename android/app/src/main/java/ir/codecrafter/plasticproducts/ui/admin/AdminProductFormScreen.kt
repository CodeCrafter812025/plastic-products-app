package ir.codecrafter.plasticproducts.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.ProductQuality
import ir.codecrafter.plasticproducts.data.model.StockChangeReason
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry

@Composable
fun AdminProductFormScreen(
    onSaved: () -> Unit,
    onViewHistory: () -> Unit,
    viewModel: AdminProductFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSavedConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AdminProductFormEvent.ActionFailed -> snackbarHostState.showSnackbar(event.message)
                AdminProductFormEvent.Saved -> showSavedConfirm = true
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.errorMessage != null && state.title.isBlank() -> ErrorWithRetry(
                    message = state.errorMessage.orEmpty(),
                    onRetry = viewModel::retryLoad,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                else -> AdminProductFormContent(state = state, viewModel = viewModel, onViewHistory = onViewHistory)
            }
        }
    }

    if (showSavedConfirm) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.msg_product_saved)) },
            confirmButton = {
                TextButton(onClick = onSaved) {
                    Text(stringResource(R.string.btn_back_to_product_list))
                }
            },
        )
    }
}

@Composable
private fun AdminProductFormContent(
    state: AdminProductFormUiState,
    viewModel: AdminProductFormViewModel,
    onViewHistory: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isEditMode) {
            item {
                TextButton(onClick = onViewHistory, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_view_price_stock_history))
                }
            }
        }
        // The full-page error branch above only covers a failed *initial* load (title
        // still blank). If a later reload fails after the form already has data, that
        // error would otherwise be silently dropped — this keeps the form usable while
        // still surfacing it, instead of only the transient ActionFailed snackbar.
        if (state.errorMessage != null && state.title.isNotBlank()) {
            item {
                ErrorWithRetry(
                    message = state.errorMessage,
                    onRetry = viewModel::retryLoad,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.label_product_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.label_description)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.color,
                onValueChange = viewModel::onColorChange,
                label = { Text(stringResource(R.string.label_product_color)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.isEditMode) {
            item {
                ProductPriceRow(
                    price = state.price,
                    isUpdating = state.isUpdatingPrice,
                    onConfirm = viewModel::updatePrice,
                )
            }
        } else {
            item {
                OutlinedTextField(
                    value = state.price,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text(stringResource(R.string.label_product_price)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            OutlinedTextField(
                value = state.weight,
                onValueChange = viewModel::onWeightChange,
                label = { Text(stringResource(R.string.label_product_weight)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.isEditMode) {
            item {
                ProductStockRow(
                    stock = state.stock,
                    isUpdating = state.isUpdatingStock,
                    onConfirm = viewModel::updateStock,
                )
            }
        } else {
            item {
                OutlinedTextField(
                    value = state.stock,
                    onValueChange = viewModel::onStockChange,
                    label = { Text(stringResource(R.string.label_product_stock)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.quality == ProductQuality.PRIMARY,
                    onClick = { viewModel.onQualityChange(ProductQuality.PRIMARY) },
                    label = { Text(stringResource(R.string.filter_quality_primary)) },
                )
                FilterChip(
                    selected = state.quality == ProductQuality.RECYCLED,
                    onClick = { viewModel.onQualityChange(ProductQuality.RECYCLED) },
                    label = { Text(stringResource(R.string.filter_quality_recycled)) },
                )
            }
        }
        // isActive only has meaning once a product exists to toggle — in create mode
        // it's always sent as true (see AdminProductFormViewModel.save()) and there's
        // nothing useful to show here yet.
        if (state.isEditMode) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.label_product_active), modifier = Modifier.weight(1f))
                    Switch(checked = state.isActive, onCheckedChange = viewModel::onIsActiveChange)
                }
            }
        }
        item {
            Text(stringResource(R.string.title_product_images), style = MaterialTheme.typography.titleSmall)
        }
        if (state.isEditMode) {
            item {
                ProductImagesSection(
                    imageUrls = state.imageUrls,
                    isUploading = state.isUploadingImage,
                    onImagePicked = viewModel::uploadImage,
                )
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.msg_save_product_before_images),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            val canSave = state.title.isNotBlank() &&
                state.price.isNotBlank() &&
                state.weight.isNotBlank() &&
                state.stock.isNotBlank() &&
                state.quality != null &&
                !state.isSaving
            Button(
                onClick = viewModel::save,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.btn_save))
                }
            }
        }
    }
}

/**
 * Only shown in edit mode — upload_image needs a real product id (see
 * AdminProductFormViewModel.uploadImage's KDoc). No per-image delete button
 * here: products/views.py upload_image() only ever appends to image_urls,
 * there is no endpoint to remove a single image — a real server limitation,
 * not something left out of this screen by mistake.
 */
@Composable
private fun ProductImagesSection(
    imageUrls: List<String>,
    isUploading: Boolean,
    onImagePicked: (Uri) -> Unit,
) {
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onImagePicked) }

    Column {
        if (imageUrls.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(imageUrls) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
        }

        val maxImagesReached = imageUrls.size >= 5
        Button(
            onClick = {
                pickImageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            enabled = !maxImagesReached && !isUploading,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.btn_add_product_image))
            }
        }
        if (maxImagesReached) {
            Text(
                text = stringResource(R.string.msg_max_images_reached),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Read-only in edit mode — price changes go through PATCH products/{id}/price/, not the general form save. */
@Composable
private fun ProductPriceRow(price: String, isUpdating: Boolean, onConfirm: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.label_product_price_value, price),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { showDialog = true }, enabled = !isUpdating) {
            Text(stringResource(R.string.btn_change_price))
        }
    }

    if (showDialog) {
        ChangePriceDialog(
            onConfirm = { newPrice ->
                onConfirm(newPrice)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun ChangePriceDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var priceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_change_price)) },
        text = {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text(stringResource(R.string.label_product_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(priceText) }, enabled = priceText.isNotBlank()) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}

/** Read-only in edit mode — stock changes go through PATCH products/{id}/stock/, not the general form save. */
@Composable
private fun ProductStockRow(
    stock: String,
    isUpdating: Boolean,
    onConfirm: (String, StockChangeReason) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.label_product_stock_value, stock),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { showDialog = true }, enabled = !isUpdating) {
            Text(stringResource(R.string.btn_change_stock))
        }
    }

    if (showDialog) {
        ChangeStockDialog(
            onConfirm = { newStock, reason ->
                onConfirm(newStock, reason)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun ChangeStockDialog(onConfirm: (String, StockChangeReason) -> Unit, onDismiss: () -> Unit) {
    var stockText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(StockChangeReason.ADJUSTMENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_change_stock)) },
        text = {
            Column {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text(stringResource(R.string.label_product_stock)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = reason == StockChangeReason.INITIAL,
                        onClick = { reason = StockChangeReason.INITIAL },
                        label = { Text(stringResource(R.string.stock_reason_initial)) },
                    )
                    FilterChip(
                        selected = reason == StockChangeReason.SALE,
                        onClick = { reason = StockChangeReason.SALE },
                        label = { Text(stringResource(R.string.stock_reason_sale)) },
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = reason == StockChangeReason.RESTOCK,
                        onClick = { reason = StockChangeReason.RESTOCK },
                        label = { Text(stringResource(R.string.stock_reason_restock)) },
                    )
                    FilterChip(
                        selected = reason == StockChangeReason.ADJUSTMENT,
                        onClick = { reason = StockChangeReason.ADJUSTMENT },
                        label = { Text(stringResource(R.string.stock_reason_adjustment)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(stockText, reason) }, enabled = stockText.isNotBlank()) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}
