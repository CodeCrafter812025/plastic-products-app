from rest_framework import serializers
from .models import Order, OrderItem, OrderAssignment, OrderStatusHistory, CartItem
from products.serializers import ProductSerializer

class OrderItemSerializer(serializers.ModelSerializer):
    product_detail = ProductSerializer(source='product', read_only=True)
    
    class Meta:
        model = OrderItem
        fields = ['id', 'order', 'product', 'product_detail', 'quantity', 'unit_price', 'total_price']
        read_only_fields = ['id', 'total_price']

class OrderSerializer(serializers.ModelSerializer):
    buyer_name = serializers.StringRelatedField(source='buyer', read_only=True)
    visitor_name = serializers.StringRelatedField(source='visitor', read_only=True)
    items = OrderItemSerializer(many=True, read_only=True)
    
    class Meta:
        model = Order
        fields = [
            'id', 'buyer', 'buyer_name', 'visitor', 'visitor_name',
            'total_price', 'status', 'items', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'buyer', 'total_price', 'created_at', 'updated_at']

class OrderCreateSerializer(serializers.Serializer):
    # برای ثبت سفارش از سبد خرید فعلی کاربر
    pass  # بدون نیاز به فیلد خاصی، چون از سبد خرید کاربر استفاده می‌شود

class CartItemSerializer(serializers.ModelSerializer):
    product_detail = ProductSerializer(source='product', read_only=True)
    subtotal = serializers.SerializerMethodField()
    
    class Meta:
        model = CartItem
        fields = ['id', 'user', 'product', 'product_detail', 'quantity', 'added_at', 'updated_at', 'subtotal']
        read_only_fields = ['id', 'user', 'added_at', 'updated_at', 'subtotal']
    
    def get_subtotal(self, obj):
        return obj.quantity * obj.product.price

class CartItemAddSerializer(serializers.Serializer):
    product_id = serializers.IntegerField()
    quantity = serializers.DecimalField(max_digits=10, decimal_places=2, min_value=0.01)

class OrderAssignmentSerializer(serializers.ModelSerializer):
    order_detail = OrderSerializer(source='order', read_only=True)
    assigned_by_name = serializers.StringRelatedField(source='assigned_by', read_only=True)
    new_visitor_name = serializers.StringRelatedField(source='new_visitor', read_only=True)
    
    class Meta:
        model = OrderAssignment
        fields = [
            'id', 'order', 'order_detail', 'old_visitor', 'new_visitor',
            'new_visitor_name', 'assigned_by', 'assigned_by_name', 
            'reason', 'assigned_at'
        ]
        read_only_fields = ['id', 'assigned_at']

class OrderStatusHistorySerializer(serializers.ModelSerializer):
    changed_by_name = serializers.StringRelatedField(source='changed_by', read_only=True)
    
    class Meta:
        model = OrderStatusHistory
        fields = ['id', 'order', 'old_status', 'new_status', 'changed_by', 'changed_by_name', 'note', 'changed_at']
        read_only_fields = ['id', 'changed_at']