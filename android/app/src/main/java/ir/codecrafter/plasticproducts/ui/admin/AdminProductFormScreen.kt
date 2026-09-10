package ir.codecrafter.plasticproducts.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.ProductQuality

@Composable
fun AdminProductFormScreen(
    onSaved: () -> Unit,
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

                state.errorMessage != null && state.title.isBlank() -> Text(
                    text = state.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                else -> AdminProductFormContent(state = state, viewModel = viewModel)
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
private fun AdminProductFormContent(state: AdminProductFormUiState, viewModel: AdminProductFormViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
