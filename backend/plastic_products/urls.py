from django.contrib import admin
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from rest_framework_simplejwt.views import TokenRefreshView
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView

from users.views import AuthViewSet, UserViewSet, AccountDeletionRequestViewSet
from products.views import ProductViewSet, PriceHistoryViewSet, StockHistoryViewSet
from orders.views import CartViewSet, OrderViewSet, OrderAssignmentViewSet, VisitorOrderStatusViewSet
from core.views import SystemSettingViewSet, NotificationViewSet

router = DefaultRouter()

# Users
router.register(r'auth', AuthViewSet, basename='auth')
router.register(r'users', UserViewSet)
router.register(r'deletion-requests', AccountDeletionRequestViewSet, basename='deletion-request')

# Products
router.register(r'products', ProductViewSet)
router.register(r'price-histories', PriceHistoryViewSet)
router.register(r'stock-histories', StockHistoryViewSet)

# Orders
router.register(r'cart', CartViewSet, basename='cart')
router.register(r'orders', OrderViewSet, basename='order')
router.register(r'order-assignments', OrderAssignmentViewSet, basename='order-assignment')
router.register(r'visitor/orders', VisitorOrderStatusViewSet, basename='visitor-order')

# Core
router.register(r'system-settings', SystemSettingViewSet)
router.register(r'notifications', NotificationViewSet, basename='notification')

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/v1/', include(router.urls)),
    path('api/token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    path('api/schema/', SpectacularAPIView.as_view(), name='schema'),
    path('api/docs/', SpectacularSwaggerView.as_view(url_name='schema'), name='swagger-ui'),
]