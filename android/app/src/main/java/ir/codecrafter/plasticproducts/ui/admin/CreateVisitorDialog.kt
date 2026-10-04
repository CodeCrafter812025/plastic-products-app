package ir.codecrafter.plasticproducts.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R

@Composable
fun CreateVisitorDialog(
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateVisitorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CreateVisitorEvent.Created -> {
                    viewModel.resetForm()
                    onCreated()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.resetForm()
            onDismiss()
        },
        title = { Text(stringResource(R.string.btn_add_new_visitor)) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text(stringResource(R.string.label_phone_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = { Text(stringResource(R.string.label_full_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::submit,
                enabled = state.phone.isNotBlank() && state.fullName.isNotBlank() && !state.isSaving,
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resetForm()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}
