package ir.codecrafter.plasticproducts.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.codecrafter.plasticproducts.R
import kotlinx.coroutines.flow.Flow

private const val MIN_ADMIN_PIN_LENGTH = 4

/** Second step of admin login, reached only after verifyOtp() sends AuthNavigationEvent.PinRequired. */
@Composable
fun AdminPinScreen(
    state: AuthUiState,
    navigationEvents: Flow<AuthNavigationEvent>,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVerified: (role: String) -> Unit,
) {
    LaunchedEffect(navigationEvents) {
        navigationEvents.collect { event ->
            when (event) {
                is AuthNavigationEvent.VerifiedSuccessfully -> onVerified(event.role)
                // Not reachable from here — verifyAdminPin() never emits PinRequired — but
                // the sealed class is shared with OtpVerifyScreen so this branch must exist.
                is AuthNavigationEvent.PinRequired -> Unit
            }
        }
    }

    Scaffold { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.msg_enter_admin_pin),
                style = MaterialTheme.typography.bodyLarge,
            )

            OutlinedTextField(
                value = state.pinCode,
                onValueChange = onPinChange,
                label = { Text(stringResource(R.string.label_admin_pin)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = state.pinError != null,
                supportingText = {
                    state.pinError?.let { Text(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )

            Button(
                onClick = onSubmit,
                enabled = !state.isVerifyingPin && state.pinCode.length >= MIN_ADMIN_PIN_LENGTH,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = 16.dp),
            ) {
                if (state.isVerifyingPin) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.btn_verify))
                }
            }
        }
    }
}
