package ir.codecrafter.plasticproducts.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** Root destinations the auth graph hands off to once a user is authenticated. */
object RootRoutes {
    const val BUYER_ROOT = "buyer_root"
    const val VISITOR_ROOT = "visitor_root"
    const val ADMIN_ROOT = "admin_root"
}

/**
 * Top level of the app: the auth graph plus one root destination per role's own
 * (not-yet-built) graph, so authGraph's onAuthenticated has somewhere real to
 * navigate. BuyerRootPlaceholder/VisitorRootPlaceholder/AdminRootPlaceholder below
 * are deliberately minimal stand-ins — out of scope for this task — meant to be
 * replaced by each role's actual nav graph when that's built.
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

        composable(RootRoutes.BUYER_ROOT) { BuyerRootPlaceholder() }
        composable(RootRoutes.VISITOR_ROOT) { VisitorRootPlaceholder() }
        composable(RootRoutes.ADMIN_ROOT) { AdminRootPlaceholder() }
    }
}

@Composable
private fun BuyerRootPlaceholder() = RolePlaceholder("خریدار")

@Composable
private fun VisitorRootPlaceholder() = RolePlaceholder("ویزیتور")

@Composable
private fun AdminRootPlaceholder() = RolePlaceholder("ادمین")

@Composable
private fun RolePlaceholder(roleLabel: String) {
    Scaffold { paddingValues ->
        Text(
            text = "ورود موفق ($roleLabel) — این بخش هنوز ساخته نشده",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
        )
    }
}
