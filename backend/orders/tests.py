from datetime import datetime, timezone as dt_timezone
from decimal import Decimal
from unittest.mock import patch
from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

from products.models import Product
from orders.models import Order, OrderItem, Invoice

User = get_user_model()


class OrderEditItemsQuantityTests(APITestCase):
    """
    Regression tests: edit_items used to do arithmetic directly between the
    Decimal quantity fields and whatever the client sent (float, str, etc.),
    so a non-integer or non-numeric quantity raised an unhandled TypeError
    (500) instead of a clean validation error.
    """

    def setUp(self):
        self.client = APIClient()
        self.buyer = User.objects.create(
            phone='09121111111', username='09121111111', full_name='Buyer', role='buyer'
        )
        refresh = RefreshToken.for_user(self.buyer)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')

        self.product = Product.objects.create(
            title='Test Product', price=Decimal('10.00'), weight=Decimal('1.0'),
            quality='اولیه', stock=Decimal('100.00'), is_active=True,
            created_by=self.buyer
        )

        self.order = Order.objects.create(buyer=self.buyer, total_price=Decimal('20.00'), status='pending')
        self.order_item = OrderItem.objects.create(
            order=self.order, product=self.product, quantity=Decimal('2.00'),
            unit_price=Decimal('10.00'), total_price=Decimal('20.00')
        )
        self.product.stock = Decimal('98.00')  # 2 units already reserved by the existing order item
        self.product.save()

        self.edit_items_url = reverse('order-edit-items', args=[self.order.id])

    def test_edit_items_with_decimal_string_quantity_succeeds(self):
        response = self.client.patch(
            self.edit_items_url,
            {'items': [{'product_id': self.product.id, 'quantity': '5.5'}]},
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.order_item.refresh_from_db()
        self.assertEqual(self.order_item.quantity, Decimal('5.5'))
        self.assertEqual(self.order_item.total_price, Decimal('55.0'))

    def test_edit_items_with_decimal_float_quantity_succeeds(self):
        response = self.client.patch(
            self.edit_items_url,
            {'items': [{'product_id': self.product.id, 'quantity': 5.5}]},
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.order_item.refresh_from_db()
        self.assertEqual(self.order_item.quantity, Decimal('5.5'))

    def test_edit_items_with_non_numeric_quantity_returns_clean_400(self):
        response = self.client.patch(
            self.edit_items_url,
            {'items': [{'product_id': self.product.id, 'quantity': 'not-a-number'}]},
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('error', response.data)

    def test_edit_items_with_zero_quantity_returns_clean_400(self):
        response = self.client.patch(
            self.edit_items_url,
            {'items': [{'product_id': self.product.id, 'quantity': 0}]},
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('error', response.data)

    def test_edit_items_with_negative_quantity_returns_clean_400(self):
        response = self.client.patch(
            self.edit_items_url,
            {'items': [{'product_id': self.product.id, 'quantity': -3}]},
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('error', response.data)


class OrderSerializerBuyerInfoTests(APITestCase):
    """
    buyer_phone/buyer_address were added to OrderSerializer so that whoever
    already has access to an order (buyer, assigned visitor, admin) can see
    contact info without a separate lookup.
    """

    def setUp(self):
        self.buyer = User.objects.create(
            phone='09121234567', username='09121234567', full_name='Buyer',
            role='buyer', address='تهران، خیابان آزادی'
        )
        self.admin = User.objects.create(
            phone='09120000000', username='09120000000', full_name='Admin', role='admin'
        )
        self.visitor = User.objects.create(
            phone='09121111111', username='09121111111', full_name='Visitor', role='visitor'
        )
        self.product = Product.objects.create(
            title='Test', price=Decimal('10.00'), weight=Decimal('1.0'),
            quality='اولیه', stock=Decimal('50'), is_active=True,
            created_by=self.admin
        )

        self.order = Order.objects.create(buyer=self.buyer, total_price=Decimal('20.00'), status='pending')
        OrderItem.objects.create(
            order=self.order, product=self.product, quantity=Decimal('2'),
            unit_price=Decimal('10.00'), total_price=Decimal('20.00')
        )

        client_admin = APIClient()
        refresh_admin = RefreshToken.for_user(self.admin)
        client_admin.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh_admin.access_token}')
        assign_url = reverse('order-assignment-list')
        response = client_admin.post(assign_url, {
            'order_id': self.order.id,
            'new_visitor_id': self.visitor.id,
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        self.client_visitor = APIClient()
        refresh_visitor = RefreshToken.for_user(self.visitor)
        self.client_visitor.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh_visitor.access_token}')

    def test_assigned_visitor_sees_buyer_phone_and_address_in_list(self):
        response = self.client_visitor.get(reverse('order-list'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        orders = [o for o in response.data if o['id'] == self.order.id]
        self.assertEqual(len(orders), 1)
        self.assertEqual(orders[0]['buyer_phone'], self.buyer.phone)
        self.assertEqual(orders[0]['buyer_address'], self.buyer.address)

    def test_assigned_visitor_sees_buyer_phone_and_address_in_detail(self):
        response = self.client_visitor.get(reverse('order-detail', args=[self.order.id]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['buyer_phone'], self.buyer.phone)
        self.assertEqual(response.data['buyer_address'], self.buyer.address)

    def test_buyer_address_null_serializes_to_none(self):
        self.buyer.address = None
        self.buyer.save()
        response = self.client_visitor.get(reverse('order-detail', args=[self.order.id]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsNone(response.data['buyer_address'])


class JalaliDisplayDateTests(APITestCase):
    """to_jalali_display() is a pure helper used only for the invoice PDF's
    display text; the database and the rest of the API stay Gregorian."""

    def test_known_gregorian_date_converts_to_expected_jalali_string(self):
        from orders.views import to_jalali_display
        dt = datetime(2026, 9, 8, 12, 0, tzinfo=dt_timezone.utc)
        self.assertEqual(to_jalali_display(dt), '۱۷ شهریور ۱۴۰۵')


class InvoicePdfJalaliDateTests(APITestCase):
    """
    invoice_pdf used to print the Gregorian issued_at date straight into the
    PDF (e.g. "08-09-2026"). It should now show the Jalali equivalent.
    """

    def setUp(self):
        self.client = APIClient()
        self.buyer = User.objects.create(
            phone='09121234567', username='09121234567', full_name='Buyer', role='buyer'
        )
        refresh = RefreshToken.for_user(self.buyer)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')

        self.order = Order.objects.create(buyer=self.buyer, total_price=Decimal('20.00'), status='delivered')
        self.invoice = Invoice.objects.create(
            order=self.order,
            invoice_number='INV-000001',
            buyer_name=self.buyer.full_name,
            buyer_phone=self.buyer.phone,
            items_snapshot=[],
            total_price=Decimal('20.00'),
        )
        # issued_at is auto_now_add=True, so it must be overridden with a
        # queryset update (bypassing save()) to pin it to a known date.
        fixed_issued_at = datetime(2026, 9, 8, 12, 0, tzinfo=dt_timezone.utc)
        Invoice.objects.filter(pk=self.invoice.pk).update(issued_at=fixed_issued_at)
        self.invoice.refresh_from_db()

    def test_invoice_pdf_contains_jalali_date_text(self):
        url = reverse('order-invoice-pdf', args=[self.order.id])
        # fa() reshapes/reorders characters for RTL rendering, which makes a
        # substring check on the final PDF bytes unreliable. Patching fa()
        # to be a passthrough lets us assert on the exact text handed to it
        # (the real code path, before cosmetic reshaping) instead.
        with patch('orders.views.fa', side_effect=lambda text: text) as mock_fa:
            response = self.client.get(url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response['Content-Type'], 'application/pdf')

        texts_passed_to_fa = [call.args[0] for call in mock_fa.call_args_list]
        self.assertTrue(
            any('۱۷ شهریور ۱۴۰۵' in text for text in texts_passed_to_fa),
            f"Jalali date not found among texts rendered into the PDF: {texts_passed_to_fa}"
        )
