from rest_framework import serializers
from .models import Product, PriceHistory, StockHistory

class ProductSerializer(serializers.ModelSerializer):
    created_by_name = serializers.StringRelatedField(source='created_by', read_only=True)
    
    class Meta:
        model = Product
        fields = [
            'id', 'title', 'price', 'weight', 'color', 'quality', 
            'description', 'image_urls', 'stock', 'is_active',
            'created_by', 'created_by_name', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_by', 'created_at', 'updated_at']

class PriceHistorySerializer(serializers.ModelSerializer):
    changed_by_name = serializers.StringRelatedField(source='changed_by', read_only=True)
    
    class Meta:
        model = PriceHistory
        fields = ['id', 'product', 'old_price', 'new_price', 'changed_by', 'changed_by_name', 'changed_at']
        read_only_fields = ['id', 'changed_at']

class StockHistorySerializer(serializers.ModelSerializer):
    changed_by_name = serializers.StringRelatedField(source='changed_by', read_only=True)
    
    class Meta:
        model = StockHistory
        fields = ['id', 'product', 'old_stock', 'new_stock', 'reason', 'changed_by', 'changed_by_name', 'changed_at']
        read_only_fields = ['id', 'changed_at']