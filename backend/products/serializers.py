from rest_framework import serializers
from .models import Product, PriceHistory, StockHistory
from decimal import Decimal

class ProductSerializer(serializers.ModelSerializer):
    created_by_name = serializers.StringRelatedField(source='created_by', read_only=True)
    stock = serializers.DecimalField(max_digits=10, decimal_places=2, min_value=Decimal('0.00'))
    image_urls = serializers.SerializerMethodField()

    class Meta:
        model = Product
        fields = [
            'id', 'title', 'price', 'weight', 'color', 'quality',
            'description', 'image_urls', 'stock', 'is_active',
            'created_by', 'created_by_name', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_by', 'created_at', 'updated_at']

    def get_image_urls(self, obj):
        request = self.context.get('request')
        urls = []
        for url in obj.image_urls:
            if url.startswith('http://') or url.startswith('https://'):
                urls.append(url)
            elif request is not None:
                urls.append(request.build_absolute_uri(url))
            else:
                urls.append(url)
        return urls

    def validate_image_urls(self, value):
        if len(value) > 5:
            raise serializers.ValidationError("حداکثر ۵ تصویر مجاز است.")
        return value

class PriceHistorySerializer(serializers.ModelSerializer):
    changed_by_name = serializers.StringRelatedField(source='changed_by', read_only=True)
    
    class Meta:
        model = PriceHistory
        fields = ['id', 'product', 'old_price', 'new_price', 'changed_by', 'changed_by_name', 'changed_at']
        read_only_fields = ['id', 'changed_at']

class StockHistorySerializer(serializers.ModelSerializer):
    changed_by_name = serializers.StringRelatedField(source='changed_by', read_only=True)
    old_stock = serializers.DecimalField(max_digits=10, decimal_places=2, min_value=Decimal('0.00'), allow_null=True, required=False)
    new_stock = serializers.DecimalField(max_digits=10, decimal_places=2, min_value=Decimal('0.00'))
    class Meta:
        model = StockHistory
        fields = ['id', 'product', 'old_stock', 'new_stock', 'reason', 'changed_by', 'changed_by_name', 'changed_at']
        read_only_fields = ['id', 'changed_at']