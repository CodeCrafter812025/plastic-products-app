
# Create your models here.
from django.db import models
from users.models import User

class Product(models.Model):
    QUALITY_CHOICES = (
        ('اولیه', 'اولیه'),
        ('بازیافتی', 'بازیافتی'),
    )
    
    title = models.CharField(max_length=200)
    price = models.DecimalField(max_digits=15, decimal_places=2)
    weight = models.DecimalField(max_digits=10, decimal_places=2)  # وزن محصول (ویژگی)
    color = models.CharField(max_length=50, blank=True, null=True)
    quality = models.CharField(max_length=20, choices=QUALITY_CHOICES)
    description = models.TextField(blank=True)
    image_urls = models.JSONField(default=list)  # آرایه‌ای از آدرس تصاویر
    stock = models.IntegerField(default=0)
    is_active = models.BooleanField(default=True)
    created_by = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='products')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.title

class PriceHistory(models.Model):
    product = models.ForeignKey(Product, on_delete=models.RESTRICT, related_name='price_histories')
    old_price = models.DecimalField(max_digits=15, decimal_places=2, null=True, blank=True)
    new_price = models.DecimalField(max_digits=15, decimal_places=2)
    changed_by = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='price_changes')
    changed_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.product.title} - {self.old_price} → {self.new_price}"

class StockHistory(models.Model):
    REASON_CHOICES = (
        ('initial', 'اولیه'),
        ('sale', 'فروش'),
        ('restock', 'تامین مجدد'),
        ('adjustment', 'تعدیل'),
    )
    
    product = models.ForeignKey(Product, on_delete=models.RESTRICT, related_name='stock_histories')
    old_stock = models.IntegerField(null=True, blank=True)
    new_stock = models.IntegerField()
    reason = models.CharField(max_length=50, choices=REASON_CHOICES)
    changed_by = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='stock_changes')
    changed_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.product.title} - {self.old_stock} → {self.new_stock} ({self.reason})"
    
