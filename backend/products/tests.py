from decimal import Decimal
from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APITestCase, APIClient
from rest_framework import status

from products.models import Product

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
