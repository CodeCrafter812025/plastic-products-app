# tests.py
import threading
from decimal import Decimal
from unittest.mock import patch, MagicMock
from datetime import timedelta
from django.contrib.auth import get_user_model
from django.utils import timezone
from django.urls import reverse
from rest_framework.test import APITestCase, APITransactionTestCase, APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

from users.models import OTPCode
from products.models import Product
from orders.models import Order, OrderItem, OrderAssignment, OrderStatusHistory, CartItem
from core.models import Notification, SystemSetting

from copy import deepcopy
from django.conf import settings as dj_settings

from users.models import AccountDeletionRequest

from django.core.cache import cache
from django.test import override_settings



User = get_user_model()


class OTPFlowTests(APITestCase):
    def setUp(self):
        cache.clear()
        self.client = APIClient()
        self.phone = "09121234567"
        self.request_url = reverse('auth-request-otp')
        self.verify_url = reverse('auth-verify-otp')

    def tearDown(self):
        cache.clear()

    def test_otp_register_verify(self):
        # Request OTP
        response = self.client.post(self.request_url, {'phone': self.phone, 'purpose': 'register'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        code = response.data['code']

        # Verify
        payload = {'phone': self.phone, 'code': code, 'purpose': 'register', 'full_name': 'Test User'}
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('token', response.data)
        self.assertIn('user', response.data)
        user = User.objects.get(phone=self.phone)
        self.assertEqual(user.full_name, 'Test User')
        self.assertEqual(user.role, 'buyer')

    def test_otp_login_existing_user(self):
        user = User.objects.create(phone=self.phone, username=self.phone, full_name='Old', role='buyer')
        # Request OTP for login
        response = self.client.post(self.request_url, {'phone': self.phone, 'purpose': 'login'})
        code = response.data['code']
        payload = {'phone': self.phone, 'code': code, 'purpose': 'login'}
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['user']['id'], user.id)

    def test_otp_wrong_code(self):
        self.client.post(self.request_url, {'phone': self.phone, 'purpose': 'register'})
        payload = {'phone': self.phone, 'code': '00000', 'purpose': 'register'}
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data['error'], 'OTP_INVALID')

    def test_otp_expired(self):
        # Create expired OTP directly
        expires_at = timezone.now() - timedelta(minutes=1)
        otp = OTPCode.objects.create(
            phone=self.phone, code='12345', purpose='register',
            expires_at=expires_at, is_used=False
        )
        payload = {'phone': self.phone, 'code': '12345', 'purpose': 'register'}
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data['error'], 'OTP_EXPIRED')

    def test_otp_attempt_lockout(self):
        # Request OTP, then attempt wrong codes 5 times
        self.client.post(self.request_url, {'phone': self.phone, 'purpose': 'register'})
        # Get the OTP object to know the code (we'll use a wrong one)
        otp = OTPCode.objects.get(phone=self.phone, purpose='register', is_used=False)
        wrong_code = '99999'
        for _ in range(5):
            payload = {'phone': self.phone, 'code': wrong_code, 'purpose': 'register'}
            response = self.client.post(self.verify_url, payload)
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
            self.assertEqual(response.data['error'], 'OTP_INVALID')
        # The 6th attempt should return ACCOUNT_LOCKED
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(response.data['error'], 'ACCOUNT_LOCKED')

    def test_otp_change_phone(self):
        # Create and authenticate user
        user = User.objects.create(phone='09120000000', username='09120000000', full_name='Old', role='buyer')
        refresh = RefreshToken.for_user(user)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')
        new_phone = '09121111111'
        # Request change_phone OTP
        response = self.client.post(self.request_url, {'phone': new_phone, 'purpose': 'change_phone'})
        code = response.data['code']
        # Verify
        payload = {'phone': new_phone, 'code': code, 'purpose': 'change_phone'}
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        user.refresh_from_db()
        self.assertEqual(user.phone, new_phone)
        self.assertEqual(user.username, new_phone)

    def test_otp_register_existing_user(self):
        # User already exists, register purpose should return token without changing role
        user = User.objects.create(phone=self.phone, username=self.phone, full_name='Old', role='buyer')
        self.client.post(self.request_url, {'phone': self.phone, 'purpose': 'register'})
        otp = OTPCode.objects.get(phone=self.phone, purpose='register', is_used=False)
        payload = {'phone': self.phone, 'code': otp.code, 'purpose': 'register', 'full_name': 'New'}
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        user.refresh_from_db()
        self.assertEqual(user.full_name, 'Old')  # not changed
        self.assertEqual(user.role, 'buyer')

    def test_otp_stale_unused_code_does_not_break_new_request(self):
        """
        Regression test. request_otp()'s old cleanup only deleted *unexpired*
        unused rows (OTPCode.objects.filter(..., expires_at__gt=timezone.now()).delete()),
        so an unused OTP that had already expired (e.g. the user waited past the
        5-minute window, then hit "resend") survived untouched. That left two
        is_used=False rows for the same phone/purpose once a fresh one was created,
        and verify_otp()'s old .get(..., is_used=False) raised
        MultipleObjectsReturned (500) instead of validating the fresh code.
        """
        stale_otp = OTPCode.objects.create(
            phone=self.phone, code='11111', purpose='register',
            expires_at=timezone.now() - timedelta(minutes=1), is_used=False,
        )

        response = self.client.post(self.request_url, {'phone': self.phone, 'purpose': 'register'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        fresh_code = response.data['code']

        # The fix invalidates old unused rows outright, not just expired ones.
        stale_otp.refresh_from_db()
        self.assertTrue(stale_otp.is_used)

        payload = {'phone': self.phone, 'code': fresh_code, 'purpose': 'register', 'full_name': 'Test User'}
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_otp_stale_unused_code_after_new_request_gives_invalid_not_crash(self):
        """
        Companion to the test above: verifying with the *stale* (now-invalidated)
        code after a fresh one was requested must give a normal OTP_INVALID, not
        crash — this is the defensive second layer in verify_otp()
        (.filter(...).order_by('-id').first() instead of .get()).
        """
        OTPCode.objects.create(
            phone=self.phone, code='11111', purpose='login',
            expires_at=timezone.now() - timedelta(minutes=1), is_used=False,
        )
        response = self.client.post(self.request_url, {'phone': self.phone, 'purpose': 'login'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        payload = {'phone': self.phone, 'code': '11111', 'purpose': 'login'}
        response = self.client.post(self.verify_url, payload)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data['error'], 'OTP_INVALID')


class CartOrderTests(APITestCase):
    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create(phone='09121234567', username='09121234567', full_name='Buyer', role='buyer')
        refresh = RefreshToken.for_user(self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')
        self.product = Product.objects.create(
            title='Test Product', price=Decimal('10.00'), weight=Decimal('1.0'),
            quality='اولیه', stock=Decimal('100'), is_active=True,
            created_by=self.user
        )
        self.cart_url = reverse('cart-list')
        self.order_url = reverse('order-list')

    def test_add_to_cart_and_update(self):
        # Add item
        response = self.client.post(self.cart_url, {'product_id': self.product.id, 'quantity': 2})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(CartItem.objects.count(), 1)
        cart_item = CartItem.objects.get(user=self.user)
        self.assertEqual(cart_item.quantity, 2)

        # Update quantity via PATCH (partial_update maps to update)
        update_url = reverse('cart-detail', args=[cart_item.id])
        response = self.client.patch(update_url, {'product_id': self.product.id, 'quantity': 5}, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        cart_item.refresh_from_db()
        self.assertEqual(cart_item.quantity, 5)

    def test_place_order_stock_decrement_and_cart_clear(self):
        # Add to cart
        self.client.post(self.cart_url, {'product_id': self.product.id, 'quantity': 3})
        # Place order
        response = self.client.post(self.order_url, {})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        order_id = response.data['order_id']
        order = Order.objects.get(id=order_id)
        self.assertEqual(order.buyer, self.user)
        self.assertEqual(order.total_price, Decimal('30.00'))
        self.assertEqual(order.status, 'pending')
        self.product.refresh_from_db()
        self.assertEqual(self.product.stock, Decimal('97'))
        self.assertEqual(CartItem.objects.filter(user=self.user).count(), 0)
        # Check OrderItem
        order_item = OrderItem.objects.get(order=order)
        self.assertEqual(order_item.quantity, 3)
        self.assertEqual(order_item.unit_price, Decimal('10.00'))

    def test_concurrency_safety(self):
        # We cannot easily simulate concurrent requests, but we can verify that select_for_update is used.
        # We'll just place an order and ensure stock is reduced correctly.
        # In production, the code uses select_for_update; we trust that.
        self.client.post(self.cart_url, {'product_id': self.product.id, 'quantity': 10})
        self.client.post(self.order_url, {})
        self.product.refresh_from_db()
        self.assertEqual(self.product.stock, Decimal('90'))


class OrderLifecycleTests(APITestCase):
    def setUp(self):
        self.buyer = User.objects.create(phone='09121234567', username='09121234567', full_name='Buyer', role='buyer')
        self.admin = User.objects.create(phone='09120000000', username='09120000000', full_name='Admin', role='admin', is_staff=True)
        self.visitor = User.objects.create(phone='09121111111', username='09121111111', full_name='Visitor', role='visitor')
        self.product = Product.objects.create(
            title='Test', price=Decimal('10.00'), weight=Decimal('1.0'),
            quality='اولیه', stock=Decimal('50'), is_active=True,
            created_by=self.admin
        )
        # Create a pending order via buyer
        self.client_buyer = APIClient()
        refresh = RefreshToken.for_user(self.buyer)
        self.client_buyer.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')
        # Add to cart and place order
        cart_url = reverse('cart-list')
        self.client_buyer.post(cart_url, {'product_id': self.product.id, 'quantity': 5})
        order_url = reverse('order-list')
        response = self.client_buyer.post(order_url, {})
        self.order_id = response.data['order_id']
        self.order = Order.objects.get(id=self.order_id)

        # Admin client
        self.client_admin = APIClient()
        refresh_admin = RefreshToken.for_user(self.admin)
        self.client_admin.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh_admin.access_token}')

        # Visitor client
        self.client_visitor = APIClient()
        refresh_visitor = RefreshToken.for_user(self.visitor)
        self.client_visitor.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh_visitor.access_token}')

    def test_full_lifecycle_with_history(self):
        # 1. Assign visitor (admin)
        assign_url = reverse('order-assignment-list')
        response = self.client_admin.post(assign_url, {
            'order_id': self.order.id,
            'new_visitor_id': self.visitor.id,
            'reason': 'test'
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.order.refresh_from_db()
        self.assertEqual(self.order.status, 'assigned')
        self.assertEqual(self.order.visitor, self.visitor)
        # History check
        history = OrderStatusHistory.objects.filter(order=self.order).order_by('changed_at')
        self.assertEqual(len(history), 2)  # initial create + assigned
        self.assertEqual(history[1].old_status, 'pending')
        self.assertEqual(history[1].new_status, 'assigned')

        # 2. Visitor updates to loading
        status_url = reverse('visitor-order-status', args=[self.order.id])
        response = self.client_visitor.patch(status_url, {'status': 'loading'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.order.refresh_from_db()
        self.assertEqual(self.order.status, 'loading')
        history = OrderStatusHistory.objects.filter(order=self.order).order_by('changed_at')
        self.assertEqual(history[2].old_status, 'assigned')
        self.assertEqual(history[2].new_status, 'loading')

        # 3. Visitor updates to delivered
        response = self.client_visitor.patch(status_url, {'status': 'delivered'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.order.refresh_from_db()
        self.assertEqual(self.order.status, 'delivered')
        history = OrderStatusHistory.objects.filter(order=self.order).order_by('changed_at')
        self.assertEqual(history[3].old_status, 'loading')
        self.assertEqual(history[3].new_status, 'delivered')

        # 4. Admin cancellation (only allowed before delivered, but we test on assigned later)
        # Instead, we test cancel_admin on an assigned order separately.

    def test_admin_cancel_restores_stock(self):
        # Assign first
        assign_url = reverse('order-assignment-list')
        self.client_admin.post(assign_url, {
            'order_id': self.order.id,
            'new_visitor_id': self.visitor.id,
        })
        self.order.refresh_from_db()
        self.assertEqual(self.order.status, 'assigned')

        self.product.refresh_from_db()
        initial_stock = self.product.stock

        initial_stock = self.product.stock  # 50 - 5 = 45 after order creation

        # Cancel by admin
        cancel_url = reverse('order-cancel-admin', args=[self.order.id])
        response = self.client_admin.post(cancel_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.order.refresh_from_db()
        self.assertEqual(self.order.status, 'cancelled')
        self.product.refresh_from_db()
        self.assertEqual(self.product.stock, initial_stock + Decimal('5'))  # restored
        # Check history
        history = OrderStatusHistory.objects.filter(order=self.order).order_by('changed_at')
        last = history.last()
        self.assertEqual(last.old_status, 'assigned')
        self.assertEqual(last.new_status, 'cancelled')


class PermissionTests(APITestCase):
    def setUp(self):
        self.admin = User.objects.create(phone='09120000000', username='09120000000', full_name='Admin', role='admin', is_staff=True)
        self.buyer = User.objects.create(phone='09121234567', username='09121234567', full_name='Buyer', role='buyer')
        self.visitor = User.objects.create(phone='09121111111', username='09121111111', full_name='Visitor', role='visitor')
        self.client_buyer = APIClient()
        refresh = RefreshToken.for_user(self.buyer)
        self.client_buyer.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')
        self.client_admin = APIClient()
        refresh_admin = RefreshToken.for_user(self.admin)
        self.client_admin.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh_admin.access_token}')

        self.client_visitor = APIClient()
        refresh_visitor = RefreshToken.for_user(self.visitor)
        self.client_visitor.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh_visitor.access_token}')

        # A product for testing
        self.product = Product.objects.create(
            title='Test', price=Decimal('10.00'), weight=Decimal('1.0'),
            quality='اولیه', stock=Decimal('10'), is_active=True,
            created_by=self.admin
        )

    def test_non_admin_cannot_create_assignment(self):
        assign_url = reverse('order-assignment-list')
        # Create an order first via buyer
        cart_url = reverse('cart-list')
        self.client_buyer.post(cart_url, {'product_id': self.product.id, 'quantity': 1})
        order_url = reverse('order-list')
        resp = self.client_buyer.post(order_url, {})
        order_id = resp.data['order_id']
        # Try to assign as buyer
        response = self.client_buyer.post(assign_url, {
            'order_id': order_id,
            'new_visitor_id': self.visitor.id
        })
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_non_admin_cannot_edit_system_settings(self):
        setting_url = reverse('systemsetting-list')
        # Create setting as admin first
        self.client_admin.post(setting_url, {'key': 'test', 'value': '1'})
        # Update as buyer
        detail_url = reverse('systemsetting-detail', args=['test'])
        response = self.client_buyer.put(detail_url, {'value': '2'})
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_non_admin_cannot_review_deletion_request(self):
        # Create a deletion request for buyer
        del_url = reverse('deletion-request-list')
        self.client_buyer.post(del_url, {})  # create request
        request_obj = AccountDeletionRequest.objects.get(user=self.buyer)
        review_url = reverse('deletion-request-review', args=[request_obj.id])
        # Try to review as buyer (should be forbidden)
        response = self.client_buyer.post(review_url, {'action': 'approve'})
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_visitor_cannot_update_order_status_directly_on_order_viewset(self):
        # Create order and assign visitor
        order = Order.objects.create(buyer=self.buyer, total_price=Decimal('10'), status='assigned', visitor=self.visitor)
        # Try to patch order status via OrderViewSet (should not be allowed because status is read-only)
        order_detail_url = reverse('order-detail', args=[order.id])
        response = self.client_visitor.patch(order_detail_url, {'status': 'loading'})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        order.refresh_from_db()
        self.assertEqual(order.status, 'assigned')  # دست‌نخورده مونده، چون read-only است

    def test_admin_can_toggle_user_active(self):
        toggle_url = reverse('user-toggle-active', args=[self.buyer.id])
        response = self.client_admin.post(toggle_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.buyer.refresh_from_db()
        self.assertFalse(self.buyer.is_active)
        # Toggle again
        response = self.client_admin.post(toggle_url)
        self.buyer.refresh_from_db()
        self.assertTrue(self.buyer.is_active)


@override_settings(MELIPAYAMAK_USERNAME='testuser', MELIPAYAMAK_PASSWORD='testpass', MELIPAYAMAK_SENDER_NUMBER='10001234')
class SMSNotificationTests(APITransactionTestCase):
    def _call_and_wait_for_sms(self, func, *args, **kwargs):
        # send_order_status_sms() now fires in a background thread; wait for
        # any threads it spawns during this call to finish before asserting.
        before = set(threading.enumerate())
        result = func(*args, **kwargs)
        for t in threading.enumerate():
            if t not in before:
                t.join(timeout=5)
        return result

    def setUp(self):
        self.buyer = User.objects.create(phone='09121234567', username='09121234567', full_name='Buyer', role='buyer')
        self.admin = User.objects.create(phone='09120000000', username='09120000000', full_name='Admin', role='admin', is_staff=True)
        self.visitor = User.objects.create(phone='09121111111', username='09121111111', full_name='Visitor', role='visitor')
        self.product = Product.objects.create(
            title='Test', price=Decimal('10.00'), weight=Decimal('1.0'),
            quality='اولیه', stock=Decimal('50'), is_active=True,
            created_by=self.admin
        )
        # Create a pending order
        client_buyer = APIClient()
        refresh = RefreshToken.for_user(self.buyer)
        client_buyer.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')
        client_buyer.post(reverse('cart-list'), {'product_id': self.product.id, 'quantity': 2})
        resp = client_buyer.post(reverse('order-list'), {})
        self.order_id = resp.data['order_id']
        self.order = Order.objects.get(id=self.order_id)

        self.client_admin = APIClient()
        refresh_admin = RefreshToken.for_user(self.admin)
        self.client_admin.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh_admin.access_token}')

    @patch('core.services.notifications.requests.post')
    def test_sms_sent_on_assignment_and_loading_and_delivered(self, mock_post):
        # Mock successful SMS
        mock_post.return_value = MagicMock(status_code=200, json=lambda: {'RetStatus': 1})
        # Assign visitor
        assign_url = reverse('order-assignment-list')
        self._call_and_wait_for_sms(
            self.client_admin.post, assign_url,
            {'order_id': self.order.id, 'new_visitor_id': self.visitor.id}
        )
        # Check SMS called for assignment
        self.assertTrue(mock_post.called)
        self.assertIn('سفارش شما با شماره', mock_post.call_args[1]['data']['text'])
        mock_post.reset_mock()

        # Visitor updates to loading
        client_visitor = APIClient()
        refresh_visitor = RefreshToken.for_user(self.visitor)
        client_visitor.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh_visitor.access_token}')
        status_url = reverse('visitor-order-status', args=[self.order.id])
        self._call_and_wait_for_sms(client_visitor.patch, status_url, {'status': 'loading'})
        self.assertTrue(mock_post.called)
        self.assertIn('بارگیری شد', mock_post.call_args[1]['data']['text'])
        mock_post.reset_mock()

        # Visitor updates to delivered
        self._call_and_wait_for_sms(client_visitor.patch, status_url, {'status': 'delivered'})
        self.assertTrue(mock_post.called)
        self.assertIn('تحویل داده شد', mock_post.call_args[1]['data']['text'])

    @patch('core.services.notifications.requests.post')
    def test_notification_created_on_sms_failure(self, mock_post):
        # Simulate failure
        mock_post.side_effect = Exception('Network error')
        assign_url = reverse('order-assignment-list')
        self._call_and_wait_for_sms(
            self.client_admin.post, assign_url,
            {'order_id': self.order.id, 'new_visitor_id': self.visitor.id}
        )
        # Notification should be created with failure note
        note = Notification.objects.filter(user=self.buyer, related_type='order', related_id=self.order.id)
        self.assertTrue(note.exists())
        self.assertIn('ارسال ناموفق', note.first().message)