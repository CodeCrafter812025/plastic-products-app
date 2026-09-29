package ir.codecrafter.plasticproducts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.codecrafter.plasticproducts.ui.admin.AdminDashboardScreen
import ir.codecrafter.plasticproducts.ui.admin.AdminOrderListScreen
import ir.codecrafter.plasticproducts.ui.admin.AdminProductFormScreen
import ir.codecrafter.plasticproducts.ui.admin.AdminProductListScreen
import ir.codecrafter.plasticproducts.ui.admin.AdminUserListScreen
import ir.codecrafter.plasticproducts.ui.admin.ProductHistoryScreen
import ir.codecrafter.plasticproducts.ui.admin.VisitorPerformanceScreen
import ir.codecrafter.plasticproducts.ui.cart.CartScreen
import ir.codecrafter.plasticproducts.ui.invoice.InvoiceScreen
import ir.codecrafter.plasticproducts.ui.notifications.NotificationListScreen
import ir.codecrafter.plasticproducts.ui.orders.BuyerOrderDetailScreen
import ir.codecrafter.plasticproducts.ui.orders.BuyerOrderListScreen
import ir.codecrafter.plasticproducts.ui.orders.OrderEditScreen
import ir.codecrafter.plasticproducts.ui.products.ProductDetailScreen
import ir.codecrafter.plasticproducts.ui.products.ProductListScreen
import ir.codecrafter.plasticproducts.ui.profile.ProfileScreen
import ir.codecrafter.plasticproducts.ui.visitor.VisitorOrderDetailScreen
import ir.codecrafter.plasticproducts.ui.visitor.VisitorOrderListScreen

object RootRoutes {
    const val BUYER_ROOT = "buyer_root"
    const val VISITOR_ROOT = "visitor_root"
    const val ADMIN_ROOT = "admin_root"
    const val PROFILE = "profile"
}

object ProductRoutes {
    const val PRODUCT_ID_ARG = "productId"
    const val DETAIL_PATTERN = "product_detail/{$PRODUCT_ID_ARG}"

    fun detail(productId: Int) = "product_detail/$productId"
}

object CartRoutes {
    const val CART = "cart"
}

object OrderRoutes {
    const val ORDER_ID_ARG = "orderId"
    const val EDIT_PATTERN = "order_edit/{$ORDER_ID_ARG}"

    fun edit(orderId: Int) = "order_edit/$orderId"
}

object VisitorOrderRoutes {
    const val ORDER_ID_ARG = "orderId"
    const val DETAIL_PATTERN = "visitor_order_detail/{$ORDER_ID_ARG}"

    fun detail(orderId: Int) = "visitor_order_detail/$orderId"
}

object BuyerOrderRoutes {
    const val LIST = "buyer_order_list"
    const val ORDER_ID_ARG = "orderId"
    const val DETAIL_PATTERN = "buyer_order_detail/{$ORDER_ID_ARG}"

    fun detail(orderId: Int) = "buyer_order_detail/$orderId"
}

object InvoiceRoutes {
    const val ORDER_ID_ARG = "orderId"
    const val DETAIL_PATTERN = "invoice/{$ORDER_ID_ARG}"

    fun detail(orderId: Int) = "invoice/$orderId"
}

object NotificationRoutes {
    const val LIST = "notifications"
}

object AdminProductRoutes {
    const val CREATE = "admin_product_create"
    const val PRODUCT_ID_ARG = "productId"
    const val EDIT_PATTERN = "admin_product_edit/{$PRODUCT_ID_ARG}"

    fun edit(productId: Int) = "admin_product_edit/$productId"
}

object ProductHistoryRoutes {
    const val PRODUCT_ID_ARG = "productId"
    const val DETAIL_PATTERN = "product_history/{$PRODUCT_ID_ARG}"

    fun detail(productId: Int) = "product_history/$productId"
}

object AdminUserRoutes {
    const val LIST = "admin_users"
}

object VisitorPerformanceRoutes {
    const val VISITOR_ID_ARG = "visitorId"
    const val DETAIL_PATTERN = "visitor_performance/{$VISITOR_ID_ARG}"

    fun detail(visitorId: Int) = "visitor_performance/$visitorId"
}

object AdminOrderRoutes {
    const val LIST = "admin_orders"
}

object AdminDashboardRoutes {
    const val HOME = "admin_dashboard"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = AuthRoutes.GRAPH) {
        authGraph(
            navController = navController,
            onAuthenticated = { role ->
                val destination = when (role) {
                    "admin" -> RootRoutes.ADMIN_ROOT
                    "visitor" -> RootRoutes.VISITOR_ROOT
                    else -> RootRoutes.BUYER_ROOT
                }
                navController.navigate(destination) {
                    popUpTo(AuthRoutes.GRAPH) { inclusive = true }
                }
            },
        )

        composable(RootRoutes.BUYER_ROOT) {
            ProductListScreen(
                onProductClick = { productId -> navController.navigate(ProductRoutes.detail(productId)) },
                onCartClick = { navController.navigate(CartRoutes.CART) },
                onMyOrdersClick = { navController.navigate(BuyerOrderRoutes.LIST) },
                onProfileClick = { navController.navigate(RootRoutes.PROFILE) },
                onNotificationsClick = { navController.navigate(NotificationRoutes.LIST) },
            )
        }
        composable(NotificationRoutes.LIST) {
            NotificationListScreen(
                onOrderClick = { orderId -> navController.navigate(BuyerOrderRoutes.detail(orderId)) },
            )
        }
        composable(BuyerOrderRoutes.LIST) {
            BuyerOrderListScreen(
                onOrderClick = { orderId -> navController.navigate(BuyerOrderRoutes.detail(orderId)) },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(
            route = BuyerOrderRoutes.DETAIL_PATTERN,
            arguments = listOf(navArgument(BuyerOrderRoutes.ORDER_ID_ARG) { type = NavType.IntType }),
        ) {
            BuyerOrderDetailScreen(
                onEditOrder = { orderId -> navController.navigate(OrderRoutes.edit(orderId)) },
                onViewInvoice = { orderId -> navController.navigate(InvoiceRoutes.detail(orderId)) },
                onBackClick = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(CartRoutes.CART) },
            )
        }
        composable(
            route = InvoiceRoutes.DETAIL_PATTERN,
            arguments = listOf(navArgument(InvoiceRoutes.ORDER_ID_ARG) { type = NavType.IntType }),
        ) {
            InvoiceScreen()
        }
        composable(
            route = ProductRoutes.DETAIL_PATTERN,
            arguments = listOf(navArgument(ProductRoutes.PRODUCT_ID_ARG) { type = NavType.IntType }),
        ) {
            ProductDetailScreen(onBackToList = { navController.popBackStack() })
        }
        composable(CartRoutes.CART) {
            CartScreen(
                onBackToProducts = {
                    navController.popBackStack(RootRoutes.BUYER_ROOT, inclusive = false)
                },
                onEditOrder = { orderId -> navController.navigate(OrderRoutes.edit(orderId)) },
            )
        }
        composable(
            route = OrderRoutes.EDIT_PATTERN,
            arguments = listOf(navArgument(OrderRoutes.ORDER_ID_ARG) { type = NavType.IntType }),
        ) {
            OrderEditScreen(
                onBackToProducts = {
                    navController.popBackStack(RootRoutes.BUYER_ROOT, inclusive = false)
                },
            )
        }
        composable(RootRoutes.VISITOR_ROOT) {
            VisitorOrderListScreen(
                onOrderClick = { orderId -> navController.navigate(VisitorOrderRoutes.detail(orderId)) },
                onProfileClick = { navController.navigate(RootRoutes.PROFILE) },
            )
        }
        composable(
            route = VisitorOrderRoutes.DETAIL_PATTERN,
            arguments = listOf(navArgument(VisitorOrderRoutes.ORDER_ID_ARG) { type = NavType.IntType }),
        ) {
            VisitorOrderDetailScreen()
        }
        composable(RootRoutes.ADMIN_ROOT) {
            AdminProductListScreen(
                onAddProductClick = { navController.navigate(AdminProductRoutes.CREATE) },
                onProductClick = { productId -> navController.navigate(AdminProductRoutes.edit(productId)) },
                onUsersClick = { navController.navigate(AdminUserRoutes.LIST) },
                onOrdersClick = { navController.navigate(AdminOrderRoutes.LIST) },
                onDashboardClick = { navController.navigate(AdminDashboardRoutes.HOME) },
                onProfileClick = { navController.navigate(RootRoutes.PROFILE) },
            )
        }
        composable(AdminOrderRoutes.LIST) {
            AdminOrderListScreen(
                onOrderClick = { orderId -> navController.navigate(BuyerOrderRoutes.detail(orderId)) },
            )
        }
        composable(AdminDashboardRoutes.HOME) {
            AdminDashboardScreen(
                onViewVisitorPerformance = { navController.navigate(VisitorPerformanceRoutes.detail(-1)) },
            )
        }
        composable(AdminUserRoutes.LIST) {
            AdminUserListScreen(
                onViewVisitorPerformance = { visitorId ->
                    navController.navigate(VisitorPerformanceRoutes.detail(visitorId))
                },
            )
        }
        composable(
            route = VisitorPerformanceRoutes.DETAIL_PATTERN,
            arguments = listOf(navArgument(VisitorPerformanceRoutes.VISITOR_ID_ARG) { type = NavType.IntType }),
        ) {
            VisitorPerformanceScreen()
        }
        composable(AdminProductRoutes.CREATE) {
            AdminProductFormScreen(
                onSaved = { navController.popBackStack() },
                onViewHistory = {},
            )
        }
        composable(
            route = AdminProductRoutes.EDIT_PATTERN,
            arguments = listOf(navArgument(AdminProductRoutes.PRODUCT_ID_ARG) { type = NavType.IntType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt(AdminProductRoutes.PRODUCT_ID_ARG)
            AdminProductFormScreen(
                onSaved = { navController.popBackStack() },
                onViewHistory = {
                    if (productId != null) navController.navigate(ProductHistoryRoutes.detail(productId))
                },
            )
        }
        composable(
            route = ProductHistoryRoutes.DETAIL_PATTERN,
            arguments = listOf(navArgument(ProductHistoryRoutes.PRODUCT_ID_ARG) { type = NavType.IntType }),
        ) {
            ProductHistoryScreen()
        }
        composable(RootRoutes.PROFILE) {
            ProfileScreen(
                onLoggedOut = {
                    navController.navigate(AuthRoutes.GRAPH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
