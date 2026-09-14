from decimal import Decimal
from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from rest_framework_simplejwt.tokens import RefreshToken

User = get_user_model()


class VisitorPerformanceAccessTests(APITestCase):
    """
    visitor_performance used to be admin-only and always returned every
    visitor. Visitors should now be able to see only their own row via the
    same endpoint; admins keep seeing everyone.
    """

    def setUp(self):
        self.url = reverse('admin-reports-visitor-performance')
        self.admin = User.objects.create(
            phone='09120000001', username='09120000001', full_name='Admin', role='admin'
        )
        self.visitor_a = User.objects.create(
            phone='09121111111', username='09121111111', full_name='Visitor A', role='visitor'
        )
        self.visitor_b = User.objects.create(
            phone='09122222222', username='09122222222', full_name='Visitor B', role='visitor'
        )
        self.buyer = User.objects.create(
            phone='09123333333', username='09123333333', full_name='Buyer', role='buyer'
        )

    def _client_for(self, user):
        client = APIClient()
        refresh = RefreshToken.for_user(user)
        client.credentials(HTTP_AUTHORIZATION=f'Bearer {refresh.access_token}')
        return client

    def test_visitor_sees_only_own_record(self):
        response = self._client_for(self.visitor_a).get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        returned_ids = {row['id'] for row in response.data}
        self.assertEqual(returned_ids, {self.visitor_a.id})

    def test_visitor_cannot_see_another_visitors_record(self):
        response = self._client_for(self.visitor_a).get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        returned_ids = {row['id'] for row in response.data}
        self.assertNotIn(self.visitor_b.id, returned_ids)

    def test_buyer_gets_403(self):
        response = self._client_for(self.buyer).get(self.url)
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_admin_still_sees_all_visitors(self):
        response = self._client_for(self.admin).get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        returned_ids = {row['id'] for row in response.data}
        self.assertEqual(returned_ids, {self.visitor_a.id, self.visitor_b.id})
