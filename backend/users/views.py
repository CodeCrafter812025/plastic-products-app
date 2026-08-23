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
    OTPVerifySerializer, AccountDeletionRequestSerializer,
    VisitorCreateSerializer
)
from .permissions import IsAdminUserRole
from .throttles import OTPRequestThrottle, OTPPhoneThrottle

User = get_user_model()

def generate_otp():
    return f"{random.randint(10000, 99999)}"

class AuthViewSet(viewsets.GenericViewSet):
    permission_classes = [AllowAny]

    @action(detail=False, methods=['post'], url_path='otp/request', throttle_classes=[OTPRequestThrottle, OTPPhoneThrottle])
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

        # Handle 'change_phone' purpose
        if purpose == 'change_phone':
            if not request.user.is_authenticated:
                return Response({'error': 'برای تغییر شماره تلفن باید وارد شده باشید.'}, status=status.HTTP_401_UNAUTHORIZED)
            user = request.user
            if User.objects.filter(phone=phone).exclude(id=user.id).exists():
                return Response({'error': 'این شماره تلفن قبلاً ثبت شده است.'}, status=status.HTTP_400_BAD_REQUEST)
            user.phone = phone
            user.username = phone
            user.save()
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
        user, created = User.objects.get_or_create(phone=phone, defaults={'username': phone})
        if created and purpose == 'register':
            user.full_name = full_name or 'کاربر'
            user.role = 'buyer'
            user.save()
        elif created:
            return Response({'error': 'PHONE_NOT_REGISTERED'}, status=status.HTTP_400_BAD_REQUEST)

        if not user.is_active:
            return Response({'error': 'ACCOUNT_INACTIVE'}, status=status.HTTP_403_FORBIDDEN)

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

    def get_permissions(self):
        if self.action in ['toggle_active', 'create_visitor']:
            return [IsAdminUserRole()]
        return [IsAuthenticated()]

    def get_queryset(self):
        user = self.request.user
        if user.role == 'admin':
            return User.objects.all()
        return User.objects.filter(id=user.id)

    @action(detail=True, methods=['post'], url_path='toggle')
    def toggle_active(self, request, pk=None):
        """
        Admin-only action to toggle the is_active status of a user.
        """
        user = self.get_object()
        # Prevent admin from deactivating their own account
        if user.id == request.user.id:
            return Response(
                {'error': 'شما نمی‌توانید حساب خودتان را غیرفعال کنید.'},
                status=status.HTTP_400_BAD_REQUEST
            )
        user.is_active = not user.is_active
        user.save()
        return Response({'is_active': user.is_active})

    @action(detail=False, methods=['post'], url_path='create_visitor')
    def create_visitor(self, request):
        """
        Admin-only action to create a new visitor user.
        Accepts phone and full_name; sets role='visitor' and is_active=True.
        """
        serializer = VisitorCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        return Response(UserSerializer(user).data, status=status.HTTP_201_CREATED)


class AccountDeletionRequestViewSet(viewsets.ModelViewSet):
    serializer_class = AccountDeletionRequestSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        if user.role == 'admin':
            return AccountDeletionRequest.objects.all()
        return AccountDeletionRequest.objects.filter(user=user)

    @action(detail=True, methods=['post'], permission_classes=[IsAdminUserRole])
    def review(self, request, pk=None):
        deletion_request = self.get_object()
        action_type = request.data.get('action')  # 'approve' or 'reject'
        admin_note = request.data.get('admin_note', '')
        if action_type not in ['approve', 'reject']:
            return Response({'error': 'Invalid action. Must be "approve" or "reject".'}, status=400)
        
        if deletion_request.status != 'pending':
            return Response({'error': 'This request has already been reviewed.'}, status=400)
        
        if action_type == 'approve':
            deletion_request.status = 'approved'
            # Deactivate user
            user = deletion_request.user
            user.is_active = False
            user.save()
        else:
            deletion_request.status = 'rejected'
        
        deletion_request.reviewed_by = request.user
        deletion_request.reviewed_at = timezone.now()
        deletion_request.admin_note = admin_note
        deletion_request.save()
        
        return Response({'status': deletion_request.status, 'message': 'Request reviewed successfully.'})

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)