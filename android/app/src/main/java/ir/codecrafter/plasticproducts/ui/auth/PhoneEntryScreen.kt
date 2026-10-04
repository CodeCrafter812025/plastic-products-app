package ir.codecrafter.plasticproducts.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.codecrafter.plasticproducts.R

@Composable
fun PhoneEntryScreen(
    state: AuthUiState,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
        ) {
            Text(
                text = if (state.purpose == AuthPurpose.REGISTER) {
                    stringResource(R.string.title_register)
                } else {
                    stringResource(R.string.title_login)
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.label_phone_number)) },
                placeholder = { Text("09xxxxxxxxx") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = state.phoneError != null,
                supportingText = {
                    state.phoneError?.let { Text(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )

            if (state.isRateLimited) {
                Text(
                    text = state.rateLimitMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else if (state.otpRequestError != null) {
                Text(
                    text = state.otpRequestError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !state.isRequestingOtp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = 16.dp),
            ) {
                if (state.isRequestingOtp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.btn_send_code))
                }
            }
        }
    }
}
