from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

User = get_user_model()


class VisitorCreateDuplicatePhoneTests(APITestCase):
    """
    User.phone is unique=True, and VisitorCreateSerializer.create() called
    User.objects.create() with no prior check, so a duplicate phone raised
    a raw IntegrityError (500) instead of a clean validation error.
    """

    def setUp(self):
        self.client = APIClient()
        self.admin = User.objects.create(
            phone='09120000001', username='09120000001', full_name='Admin', role='admin'
        )
        refresh = RefreshToken.for_user(self.admin)
        self.client.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')
        self.create_visitor_url = reverse('user-create-visitor')

    def test_duplicate_phone_from_buyer_returns_clean_400(self):
        User.objects.create(phone='09121234567', username='09121234567', full_name='Buyer', role='buyer')
        response = self.client.post(
            self.create_visitor_url, {'phone': '09121234567', 'full_name': 'New Visitor'}
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(response.data['success'])
        self.assertIn('قبلاً ثبت شده است', str(response.data['error']['message']))

    def test_duplicate_phone_from_visitor_returns_clean_400(self):
        User.objects.create(phone='09131111111', username='09131111111', full_name='Old Visitor', role='visitor')
        response = self.client.post(
            self.create_visitor_url, {'phone': '09131111111', 'full_name': 'New Visitor'}
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(response.data['success'])

    def test_duplicate_phone_from_admin_returns_clean_400(self):
        response = self.client.post(
            self.create_visitor_url, {'phone': self.admin.phone, 'full_name': 'New Visitor'}
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(response.data['success'])

    def test_new_phone_creates_visitor_successfully(self):
        response = self.client.post(
            self.create_visitor_url, {'phone': '09129999999', 'full_name': 'New Visitor'}
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(User.objects.filter(phone='09129999999', role='visitor').exists())
