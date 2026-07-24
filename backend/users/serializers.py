from rest_framework import serializers
from django.contrib.auth import authenticate
from .models import User, OTPCode, AccountDeletionRequest

class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'phone', 'full_name', 'address', 'role', 'is_active', 'created_at', 'updated_at']
        read_only_fields = ['id', 'created_at', 'updated_at', 'is_active']

class UserRegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, required=False)
    
    class Meta:
        model = User
        fields = ['phone', 'full_name', 'address', 'role', 'password']
        extra_kwargs = {
            'phone': {'required': True},
            'full_name': {'required': True},
        }

    def create(self, validated_data):
        password = validated_data.pop('password', None)
        user = User(**validated_data)
        if password:
            user.set_password(password)  # اگر روزی از رمز عبور استفاده کنیم
        user.save()
        return user

class OTPRequestSerializer(serializers.Serializer):
    phone = serializers.CharField(max_length=11)
    purpose = serializers.ChoiceField(choices=['register', 'login', 'change_phone'])

class OTPVerifySerializer(serializers.Serializer):
    phone = serializers.CharField(max_length=11)
    code = serializers.CharField(max_length=5)
    purpose = serializers.ChoiceField(choices=['register', 'login', 'change_phone'])
    full_name = serializers.CharField(max_length=100, required=False)

class AccountDeletionRequestSerializer(serializers.ModelSerializer):
    user = UserSerializer(read_only=True)
    
    class Meta:
        model = AccountDeletionRequest
        fields = ['id', 'user', 'status', 'requested_at', 'reviewed_by', 'reviewed_at', 'admin_note']
        read_only_fields = ['id', 'user', 'requested_at']