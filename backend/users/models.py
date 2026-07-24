
# Create your models here.
from django.contrib.auth.models import AbstractUser
from django.db import models
from django.contrib.auth.base_user import BaseUserManager

class UserManager(BaseUserManager):
    def create_user(self, phone, full_name, password=None, **extra_fields):
        if not phone:
            raise ValueError('شماره تلفن الزامی است')
        user = self.model(phone=phone, full_name=full_name, **extra_fields)
        user.username = phone  # ← این خط را اضافه کنید
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, phone, full_name, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        if extra_fields.get('is_staff') is not True:
            raise ValueError('Superuser must have is_staff=True.')
        if extra_fields.get('is_superuser') is not True:
            raise ValueError('Superuser must have is_superuser=True.')
        return self.create_user(phone, full_name, password, **extra_fields)

class User(AbstractUser):
    ROLE_CHOICES = (
        ('admin', 'ادمین'),
        ('buyer', 'خریدار'),
        ('visitor', 'ویزیتور'),
    )
    
    phone = models.CharField(max_length=11, unique=True)
    full_name = models.CharField(max_length=100)
    address = models.TextField(blank=True, null=True)
    role = models.CharField(max_length=20, choices=ROLE_CHOICES)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    USERNAME_FIELD = 'phone'
    REQUIRED_FIELDS = ['full_name']

    objects = UserManager()


    def __str__(self):
        return f"{self.full_name} ({self.phone})"
    
    
class OTPCode(models.Model):
    PURPOSE_CHOICES = (
        ('register', 'ثبت‌نام'),
        ('login', 'ورود'),
        ('change_phone', 'تغییر شماره تلفن'),
    )
    
    phone = models.CharField(max_length=11)
    code = models.CharField(max_length=5)
    purpose = models.CharField(max_length=20, choices=PURPOSE_CHOICES)
    expires_at = models.DateTimeField()
    is_used = models.BooleanField(default=False)
    attempt_count = models.IntegerField(default=0)
    locked_until = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.phone} - {self.code} ({self.purpose})"
    

class AccountDeletionRequest(models.Model):
    STATUS_CHOICES = (
        ('pending', 'در انتظار'),
        ('approved', 'تأیید شده'),
        ('rejected', 'رد شده'),
    )
    
    user = models.ForeignKey('User', on_delete=models.CASCADE, related_name='deletion_requests')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    requested_at = models.DateTimeField(auto_now_add=True)
    reviewed_by = models.ForeignKey('User', on_delete=models.SET_NULL, null=True, blank=True, related_name='reviewed_requests')
    reviewed_at = models.DateTimeField(null=True, blank=True)
    admin_note = models.TextField(blank=True)

    def __str__(self):
        return f"{self.user.phone} - {self.status}"

