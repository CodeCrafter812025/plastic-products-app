from django.shortcuts import render

# Create your views here.
from rest_framework import viewsets, permissions
from .models import SystemSetting, Notification
from .serializers import SystemSettingSerializer, NotificationSerializer

class SystemSettingViewSet(viewsets.ModelViewSet):
    """
    مدیریت تنظیمات سیستم (فقط ادمین)
    """
    queryset = SystemSetting.objects.all()
    serializer_class = SystemSettingSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_permissions(self):
        # فقط ادمین می‌تواند تغییر دهد، همه می‌توانند بخوانند
        if self.action in ['create', 'update', 'partial_update', 'destroy']:
            return [permissions.IsAuthenticated()]
        return [permissions.AllowAny()]

class NotificationViewSet(viewsets.ModelViewSet):
    """
    مدیریت اعلان‌های کاربر
    """
    serializer_class = NotificationSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        # هر کاربر فقط اعلان‌های خودش را می‌بیند
        return Notification.objects.filter(user=self.request.user)

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)
