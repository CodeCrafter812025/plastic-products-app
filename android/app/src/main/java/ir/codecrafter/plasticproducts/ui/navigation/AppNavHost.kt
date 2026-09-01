package ir.codecrafter.plasticproducts.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.ui.products.ProductListScreen
import ir.codecrafter.plasticproducts.ui.profile.ProfileScreen

/** Root destinations the auth graph hands off to once a user is authenticated. */
object RootRoutes {
    const val BUYER_ROOT = "buyer_root"
    const val VISITOR_ROOT = "visitor_root"
    const val ADMIN_ROOT = "admin_root"
    const val PROFILE = "profile"
}

/**
 * Top level of the app: the auth graph plus one root destination per role's own
 * (not-yet-built) graph, so authGraph's onAuthenticated has somewhere real to
 * navigate. buyer_root now shows the real ProductListScreen; VisitorRootPlaceholder/
 * AdminRootPlaceholder are still deliberately minimal stand-ins — out of scope for
 * this task — meant to be replaced by each role's actual nav graph when that's
 * built. Their "پروفایل" button is a temporary way to reach ProfileScreen for
 * testing; it goes away once each role gets its own real navigation.
 */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = AuthRoutes.GRAPH) {
        authGraph(
            navController = navController,
            onAuthenticated = { role ->
                val destination = when (role) {
                    "admin" -> RootRoutes.ADMIN_ROOT
                    "visitor" -> RootRoutes.VISITOR_ROOT
                    // "buyer" and any unrecognized role land here.
                    else -> RootRoutes.BUYER_ROOT
                }
                navController.navigate(destination) {
                    // Users can't Back their way into the auth flow post-login.
                    popUpTo(AuthRoutes.GRAPH) { inclusive = true }
                }
            },
        )

        composable(RootRoutes.BUYER_ROOT) { ProductListScreen() }
        composable(RootRoutes.VISITOR_ROOT) {
            RolePlaceholder(stringResource(R.string.role_label_visitor)) { navController.navigate(RootRoutes.PROFILE) }
        }
        composable(RootRoutes.ADMIN_ROOT) {
            RolePlaceholder(stringResource(R.string.role_label_admin)) { navController.navigate(RootRoutes.PROFILE) }
        }
        composable(RootRoutes.PROFILE) { ProfileScreen() }
    }
}

@Composable
private fun RolePlaceholder(roleLabel: String, onProfileClick: () -> Unit) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.placeholder_role_not_built_message, roleLabel),
                style = MaterialTheme.typography.bodyLarge,
            )
            // Temporary, for testing ProfileScreen this phase only.
            Button(onClick = onProfileClick, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.btn_profile))
            }
        }
    }
}
