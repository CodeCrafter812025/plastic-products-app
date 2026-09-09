from decimal import Decimal
from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

from products.models import Product
from orders.models import Order, OrderItem

User = get_user_model()


class ProductSearchTests(APITestCase):
    def setUp(self):
        self.client = APIClient()
        self.list_url = reverse('product-list')
        self.admin = User.objects.create(
            phone='09120000001', username='09120000001', full_name='Admin', role='admin'
        )
        self.blue_bucket = Product.objects.create(
            title='سطل آبی', description='سطل پلاستیکی با درب',
            price=Decimal('100.00'), weight=Decimal('1.00'),
            color='آبی', quality='اولیه', stock=Decimal('10.00'),
            is_active=True, created_by=self.admin,
        )
        self.red_bucket = Product.objects.create(
            title='سطل قرمز', description='سطل بازیافتی مقاوم',
            price=Decimal('80.00'), weight=Decimal('1.00'),
            color='قرمز', quality='بازیافتی', stock=Decimal('5.00'),
            is_active=True, created_by=self.admin,
        )
        self.chair = Product.objects.create(
            title='صندلی پلاستیکی', description='صندلی سبک و محکم',
            price=Decimal('150.00'), weight=Decimal('2.00'),
            color='سفید', quality='اولیه', stock=Decimal('3.00'),
            is_active=True, created_by=self.admin,
        )

    def test_search_by_title_returns_only_matching_products(self):
        response = self.client.get(self.list_url, {'search': 'سطل'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        returned_ids = {p['id'] for p in response.data}
        self.assertEqual(returned_ids, {self.blue_bucket.id, self.red_bucket.id})

    def test_search_with_no_match_returns_empty_list(self):
        response = self.client.get(self.list_url, {'search': 'نامرتبط'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data, [])

    def test_search_combined_with_quality_filter(self):
        response = self.client.get(self.list_url, {'search': 'سطل', 'quality': 'بازیافتی'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        returned_ids = {p['id'] for p in response.data}
        self.assertEqual(returned_ids, {self.red_bucket.id})


class ProductDeleteRestrictedErrorTests(APITestCase):
    """
    OrderItem.product uses on_delete=models.RESTRICT, so deleting a product
    referenced by an order used to raise an unhandled RestrictedError (500).
    The custom exception handler now turns that into a clean 400.
    """

    def setUp(self):
        self.client = APIClient()
        self.admin = User.objects.create(
            phone='09120000001', username='09120000001', full_name='Admin', role='admin'
        )
        refresh = RefreshToken.for_user(self.admin)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')

        self.used_product = Product.objects.create(
            title='محصول استفاده‌شده', price=Decimal('50.00'), weight=Decimal('1.00'),
            quality='اولیه', stock=Decimal('10.00'), is_active=True, created_by=self.admin,
        )
        buyer = User.objects.create(phone='09121234567', username='09121234567', full_name='Buyer', role='buyer')
        order = Order.objects.create(buyer=buyer, total_price=Decimal('50.00'), status='pending')
        OrderItem.objects.create(
            order=order, product=self.used_product, quantity=Decimal('1'),
            unit_price=Decimal('50.00'), total_price=Decimal('50.00')
        )

        self.unused_product = Product.objects.create(
            title='محصول بدون سفارش', price=Decimal('30.00'), weight=Decimal('1.00'),
            quality='اولیه', stock=Decimal('5.00'), is_active=True, created_by=self.admin,
        )

    def test_deleting_product_used_in_order_returns_clean_400(self):
        url = reverse('product-detail', args=[self.used_product.id])
        response = self.client.delete(url)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(response.data['success'])
        self.assertIn('غیرفعال', response.data['error']['message'])
        self.assertTrue(Product.objects.filter(id=self.used_product.id).exists())

    def test_deleting_product_not_in_any_order_succeeds(self):
        url = reverse('product-detail', args=[self.unused_product.id])
        response = self.client.delete(url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Product.objects.filter(id=self.unused_product.id).exists())
