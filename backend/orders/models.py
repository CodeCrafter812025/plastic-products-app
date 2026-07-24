
# Create your models here.
from django.db import models
from users.models import User
from products.models import Product

class Order(models.Model):
    STATUS_CHOICES = (
        ('pending', 'در انتظار تخصیص'),
        ('assigned', 'تخصیص داده شده'),
        ('loading', 'بارگیری شد'),
        ('delivered', 'تحویل داده شد'),
        ('cancelled', 'لغو شده'),
    )
    
    buyer = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='orders')
    visitor = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, related_name='delivered_orders')
    total_price = models.DecimalField(max_digits=15, decimal_places=2, default=0)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Order #{self.id} - {self.buyer.phone}"

class OrderItem(models.Model):
    order = models.ForeignKey(Order, on_delete=models.CASCADE, related_name='items')
    product = models.ForeignKey(Product, on_delete=models.RESTRICT)
    quantity = models.DecimalField(max_digits=10, decimal_places=2)
    unit_price = models.DecimalField(max_digits=15, decimal_places=2)
    total_price = models.DecimalField(max_digits=15, decimal_places=2)  # quantity * unit_price

    def save(self, *args, **kwargs):
        self.total_price = self.quantity * self.unit_price
        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.product.title} x {self.quantity}"

class OrderAssignment(models.Model):
    order = models.ForeignKey(Order, on_delete=models.CASCADE, related_name='assignments')
    old_visitor = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, related_name='old_assignments')
    new_visitor = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='new_assignments')
    assigned_by = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='assignments_made')
    reason = models.TextField(blank=True)
    assigned_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Order #{self.order.id} → {self.new_visitor.phone}"

class OrderStatusHistory(models.Model):
    order = models.ForeignKey(Order, on_delete=models.CASCADE, related_name='status_histories')
    old_status = models.CharField(max_length=20, choices=Order.STATUS_CHOICES, null=True, blank=True)
    new_status = models.CharField(max_length=20, choices=Order.STATUS_CHOICES)
    changed_by = models.ForeignKey(User, on_delete=models.RESTRICT, related_name='status_changes')
    note = models.TextField(blank=True)
    changed_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Order #{self.order.id}: {self.old_status} → {self.new_status}"

class CartItem(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='cart_items')
    product = models.ForeignKey(Product, on_delete=models.CASCADE)
    quantity = models.DecimalField(max_digits=10, decimal_places=2)
    added_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        unique_together = ('user', 'product')

    def __str__(self):
        return f"{self.user.phone} - {self.product.title} x {self.quantity}"
