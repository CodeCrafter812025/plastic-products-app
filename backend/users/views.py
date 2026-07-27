# backend/users/views.py
from django.shortcuts import render

# Create your views here.
from rest_framework import viewsets, generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.decorators import action
from django.contrib.auth import get_user_model
from django.utils import timezone
from datetime import timedelta
import random
from .models import OTPCode, AccountDeletionRequest
from .serializers import (
    UserSerializer, UserRegisterSerializer, OTPRequestSerializer,
    OTPVerifySerializer, AccountDeletionRequestSerializer
)

User = get_user_model()

def generate_otp():
    return f"{random.randint(10000, 99999)}"

class AuthViewSet(viewsets.GenericViewSet):
    permission_classes = [AllowAny]

    @action(detail=False, methods=['post'], url_path='otp/request')
    def request_otp(self, request):
        serializer = OTPRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        phone = serializer.validated_data['phone']
        purpose = serializer.validated_data['purpose']

        # حذف کدهای قبلی منقضی‌نشده (اختیاری)
        OTPCode.objects.filter(phone=phone, is_used=False, expires_at__gt=timezone.now()).delete()

        # تولید و ذخیره کد جدید
        code = generate_otp()
        expires_at = timezone.now() + timedelta(minutes=5)
        otp = OTPCode.objects.create(
            phone=phone,
            code=code,
            purpose=purpose,
            expires_at=expires_at
        )

        # در محیط توسعه، کد را در پاسخ برگردان (برای تست)
        # در تولید، این خط را حذف کنید و پیامک واقعی ارسال کنید
        return Response({
            'message': 'کد OTP ارسال شد',
            'code': code,  # فقط برای تست
            'expires_in': 300
        })

    @action(detail=False, methods=['post'], url_path='otp/verify')
    def verify_otp(self, request):
        serializer = OTPVerifySerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        phone = serializer.validated_data['phone']
        code = serializer.validated_data['code']
        purpose = serializer.validated_data['purpose']
        full_name = serializer.validated_data.get('full_name', '')

        # بررسی OTP
        try:
            otp = OTPCode.objects.get(phone=phone, purpose=purpose, is_used=False)
        except OTPCode.DoesNotExist:
            return Response({'error': 'کد معتبری برای این شماره یافت نشد.'}, status=status.HTTP_400_BAD_REQUEST)

        if otp.expires_at < timezone.now():
            return Response({'error': 'OTP_EXPIRED'}, status=status.HTTP_400_BAD_REQUEST)

        if otp.attempt_count >= 5:
            return Response({'error': 'ACCOUNT_LOCKED'}, status=status.HTTP_403_FORBIDDEN)

        otp.attempt_count += 1
        otp.save(update_fields=['attempt_count'])

        if otp.code != code:
            return Response({'error': 'OTP_INVALID'}, status=status.HTTP_400_BAD_REQUEST)

        otp.is_used = True
        otp.save(update_fields=['is_used'])

        # Handle 'change_phone' purpose – update the user's phone number
        if purpose == 'change_phone':
            # The request must come from an authenticated user; we can get it from the token.
            # Since this endpoint is public, we need the user to be authenticated.
            # However, the original design allows this for authenticated users.
            # We'll assume the request includes the current user's ID or we can extract from the JWT if present.
            # For simplicity, we'll require that the user is authenticated and we have the user object.
            # But the viewset has permission_classes = [AllowAny], so we need to check if user is authenticated.
            if not request.user.is_authenticated:
                return Response({'error': 'برای تغییر شماره تلفن باید وارد شده باشید.'}, status=status.HTTP_401_UNAUTHORIZED)
            # Update the logged-in user's phone
            user = request.user
            # Ensure the new phone is not already taken by another user
            if User.objects.filter(phone=phone).exclude(id=user.id).exists():
                return Response({'error': 'این شماره تلفن قبلاً ثبت شده است.'}, status=status.HTTP_400_BAD_REQUEST)
            user.phone = phone
            user.username = phone  # keep username in sync
            user.save()
            # Issue a new JWT for the updated user (optional, but recommended)
            from rest_framework_simplejwt.tokens import RefreshToken
            refresh = RefreshToken.for_user(user)
            access_token = str(refresh.access_token)
            user_serializer = UserSerializer(user)
            return Response({
                'token': access_token,
                'user': user_serializer.data,
                'message': 'شماره تلفن با موفقیت تغییر یافت.'
            })

        # ایجاد یا بازیابی کاربر (برای register/login)
        # Explicitly set username=phone to avoid duplicate empty username
        user, created = User.objects.get_or_create(phone=phone, defaults={'username': phone})
        if created and purpose == 'register':
            user.full_name = full_name or 'کاربر'
            user.role = 'buyer'  # نقش پیش‌فرض
            user.save()
        elif created:
            # اگر شماره جدید باشد ولی هدف register نباشد، خطا بده
            return Response({'error': 'PHONE_NOT_REGISTERED'}, status=status.HTTP_400_BAD_REQUEST)

        if not user.is_active:
            return Response({'error': 'ACCOUNT_INACTIVE'}, status=status.HTTP_403_FORBIDDEN)

        # تولید JWT
        from rest_framework_simplejwt.tokens import RefreshToken
        refresh = RefreshToken.for_user(user)
        access_token = str(refresh.access_token)

        user_serializer = UserSerializer(user)
        return Response({
            'token': access_token,
            'user': user_serializer.data
        })


class UserViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = UserSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        if user.role == 'admin':
            return User.objects.all()
        return User.objects.filter(id=user.id)


class AccountDeletionRequestViewSet(viewsets.ModelViewSet):
    serializer_class = AccountDeletionRequestSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        if user.role == 'admin':
            return AccountDeletionRequest.objects.all()
        return AccountDeletionRequest.objects.filter(user=user)