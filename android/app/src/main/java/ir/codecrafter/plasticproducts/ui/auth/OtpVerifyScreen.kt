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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.codecrafter.plasticproducts.R
import kotlinx.coroutines.flow.Flow

/**
 * Length comes from the backend itself, not a guess: OTPCode.code is a
 * CharField(max_length=5) and generate_otp() in backend/users/views.py returns
 * f"{random.randint(10000, 99999)}" — always exactly 5 digits.
 */
private const val OTP_CODE_LENGTH = 5

@Composable
fun OtpVerifyScreen(
    state: AuthUiState,
    navigationEvents: Flow<AuthNavigationEvent>,
    onCodeChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onResend: () -> Unit,
    onSubmit: () -> Unit,
    onVerified: (role: String) -> Unit,
) {
    LaunchedEffect(navigationEvents) {
        navigationEvents.collect { event ->
            when (event) {
                is AuthNavigationEvent.VerifiedSuccessfully -> onVerified(event.role)
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
                text = stringResource(R.string.otp_sent_to_phone_message, state.phone),
                style = MaterialTheme.typography.bodyLarge,
            )

            OutlinedTextField(
                value = state.otpCode,
                onValueChange = { value ->
                    if (value.length <= OTP_CODE_LENGTH) onCodeChange(value)
                },
                label = { Text(stringResource(R.string.label_otp_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = state.otpError != null,
                supportingText = {
                    state.otpError?.let { Text(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )

            if (state.purpose == AuthPurpose.REGISTER) {
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = onFullNameChange,
                    label = { Text(stringResource(R.string.label_full_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }

            TextButton(
                onClick = onResend,
                enabled = state.resendCooldownSecondsRemaining == 0 && !state.isRequestingOtp,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(
                    if (state.resendCooldownSecondsRemaining > 0) {
                        stringResource(R.string.btn_resend_code_cooldown, state.resendCooldownSecondsRemaining)
                    } else {
                        stringResource(R.string.btn_resend_code)
                    }
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !state.isVerifyingOtp && state.otpCode.length == OTP_CODE_LENGTH,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = 16.dp),
            ) {
                if (state.isVerifyingOtp) {
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
