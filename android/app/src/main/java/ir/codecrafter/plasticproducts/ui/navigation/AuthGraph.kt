package ir.codecrafter.plasticproducts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import ir.codecrafter.plasticproducts.ui.auth.AuthChoiceScreen
import ir.codecrafter.plasticproducts.ui.auth.AuthViewModel
import ir.codecrafter.plasticproducts.ui.auth.OtpVerifyScreen
import ir.codecrafter.plasticproducts.ui.auth.PhoneEntryScreen

object AuthRoutes {
    const val GRAPH = "auth_graph"
    const val CHOICE = "auth_choice"
    const val PHONE_ENTRY = "auth_phone_entry"
    const val OTP_VERIFY = "auth_otp_verify"
}

/**
 * Registers the three auth screens under one nested nav graph. [onAuthenticated] is
 * called once, after a successful OTP verification, with the authenticated user's
 * role; the caller (the top-level NavHost, where the buyer/visitor/admin graphs also
 * live) decides where that leads and how to clear the auth graph from the back stack.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onAuthenticated: (role: String) -> Unit,
) {
    navigation(startDestination = AuthRoutes.CHOICE, route = AuthRoutes.GRAPH) {
        composable(AuthRoutes.CHOICE) { backStackEntry ->
            val viewModel = sharedAuthViewModel(navController, backStackEntry)
            AuthChoiceScreen(
                onChoosePurpose = { purpose ->
                    viewModel.selectPurpose(purpose)
                    navController.navigate(AuthRoutes.PHONE_ENTRY)
                },
            )
        }

        composable(AuthRoutes.PHONE_ENTRY) { backStackEntry ->
            val viewModel = sharedAuthViewModel(navController, backStackEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            PhoneEntryScreen(
                state = uiState,
                onPhoneChange = viewModel::onPhoneChange,
                onSubmit = {
                    viewModel.requestOtp {
                        navController.navigate(AuthRoutes.OTP_VERIFY)
                    }
                },
            )
        }

        composable(AuthRoutes.OTP_VERIFY) { backStackEntry ->
            val viewModel = sharedAuthViewModel(navController, backStackEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            OtpVerifyScreen(
                state = uiState,
                navigationEvents = viewModel.navigationEvents,
                onCodeChange = viewModel::onCodeChange,
                onFullNameChange = viewModel::onFullNameChange,
                onResend = viewModel::resendOtp,
                onSubmit = viewModel::verifyOtp,
                onVerified = onAuthenticated,
            )
        }
    }
}

/** Scopes AuthViewModel to the auth graph's own back stack entry, not the screen's, so all three destinations share one instance. */
@Composable
private fun sharedAuthViewModel(navController: NavController, backStackEntry: NavBackStackEntry): AuthViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AuthRoutes.GRAPH)
    }
    return hiltViewModel(parentEntry)
}
