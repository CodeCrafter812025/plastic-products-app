
# Create your models here.
from django.db import models
from users.models import User

from django.db import models
from users.models import User

class Product(models.Model):
    QUALITY_CHOICES = (
        ('اولیه', 'اولیه'),
        ('بازیافتی', 'بازیافتی'),
    )

    title = models.CharField(max_length=200)
    price = models.DecimalField(max_digits=15, decimal_places=2)
    weight = models.DecimalField(max_digits=10, decimal_places=2)  # وزن پایه
    color = models.CharField(max_length=50, blank=True, null=True)
    quality = models.CharField(max_length=20, choices=QUALITY_CHOICES)
    description = models.TextField(blank=True)
    image_urls = models.JSONField(default=list)
    stock = models.DecimalField(max_digits=10, decimal_places=2, default=0)
    is_active = models.BooleanField(default=True)
    
    # --- فیلدهای اضافه‌شده بر اساس کاتالوگ PDF ---
    category = models.CharField(max_length=100, blank=True, null=True, verbose_name="دسته‌بندی اصلی")
    sub_category = models.CharField(max_length=100, blank=True, null=True, verbose_name="کاربرد / زیردسته")
    brand = models.CharField(max_length=100, blank=True, null=True, verbose_name="برند")
    unit_label = models.CharField(max_length=50, blank=True, null=True, verbose_name="واحد شمارش")
    packaging_info = models.CharField(max_length=200, blank=True, null=True, verbose_name="اطلاعات بسته‌بندی")
    is_bestseller = models.BooleanField(default=False, verbose_name="تگ پرفروش")

    created_by = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='products')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.title} - {self.brand or ''}"

# مدل‌های PriceHistory و StockHistory بدون تغییر باقی می‌مانند.
# ... کدهای ادامه فایل ...

class PriceHistory(models.Model):
    product = models.ForeignKey(Product, on_delete=models.RESTRICT, related_name='price_histories')
    old_price = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    new_price = models.DecimalField(max_digits=10, decimal_places=2)
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
    old_stock = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True) # اصلاح شد
    new_stock = models.DecimalField(max_digits=10, decimal_places=2)                      # اصلاح شد
    reason = models.CharField(max_length=50, choices=REASON_CHOICES)
    changed_by = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='stock_changes')
    changed_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.product.title} - {self.old_stock} → {self.new_stock} ({self.reason})"
    
