from rest_framework import serializers
from .models import SystemSetting, Notification

class SystemSettingSerializer(serializers.ModelSerializer):
    class Meta:
        model = SystemSetting
        fields = ['key', 'value', 'description', 'updated_at']
        read_only_fields = ['updated_at']

class NotificationSerializer(serializers.ModelSerializer):
    class Meta:
        model = Notification
        fields = ['id', 'user', 'related_type', 'related_id', 'type', 'message', 'sent_at', 'is_read']
        read_only_fields = ['id', 'sent_at']