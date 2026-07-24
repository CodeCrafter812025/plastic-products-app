from django.db import models

# Create your models here.
from django.db import models
from users.models import User

class SystemSetting(models.Model):
    key = models.CharField(max_length=100, primary_key=True)
    value = models.TextField()
    description = models.TextField(blank=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.key}: {self.value}"

class Notification(models.Model):
    TYPE_CHOICES = (
        ('sms', 'پیامک'),
        ('push', 'اعلان درون‌برنامه‌ای'),
    )
    
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='notifications')
    related_type = models.CharField(max_length=30, blank=True, null=True)  # مانند 'order'
    related_id = models.BigIntegerField(null=True, blank=True)
    type = models.CharField(max_length=10, choices=TYPE_CHOICES)
    message = models.TextField()
    sent_at = models.DateTimeField(auto_now_add=True)
    is_read = models.BooleanField(default=False)

    def __str__(self):
        return f"{self.user.phone} - {self.type}: {self.message[:30]}"