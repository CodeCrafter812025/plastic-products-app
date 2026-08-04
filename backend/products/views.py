from django.shortcuts import render

# Create your views here.
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.db import transaction
from .models import Product, PriceHistory, StockHistory
from .serializers import ProductSerializer, PriceHistorySerializer, StockHistorySerializer

from users.permissions import IsAdminUserRole

class ProductViewSet(viewsets.ModelViewSet):
    queryset = Product.objects.filter(is_active=True)
    serializer_class = ProductSerializer

    def get_permissions(self):
        if self.action in ['create', 'update', 'partial_update', 'destroy', 'price', 'stock', 'toggle']:
            return [IsAdminUserRole()] # فقط ادمین حق تغییرات دارد
        return [permissions.AllowAny()]

    def get_queryset(self):
        """
        فیلتر کردن محصولات بر اساس پارامترهای query:
        - quality: دقیقاً مطابق با یکی از دو مقدار 'اولیه' یا 'بازیافتی'
        - color: جستجوی جزئی (icontains) در فیلد color
        - min_price: قیمت حداقل
        - max_price: قیمت حداکثر
        - in_stock: اگر مقدار true یا 1 باشد، فقط محصولات با stock > 0
        """
        queryset = super().get_queryset()  # در ابتدا is_active=True اعمال شده است

        # فیلتر کیفیت
        quality = self.request.query_params.get('quality')
        if quality:
            # اعتبارسنجی ساده: فقط دو مقدار مجاز هستند
            if quality in ['اولیه', 'بازیافتی']:
                queryset = queryset.filter(quality=quality)
            else:
                # در صورت invalid، می‌توانیم خالی برگردانیم یا نادیده بگیریم – نادیده گرفتن
                pass

        # فیلتر رنگ (جستجوی جزئی)
        color = self.request.query_params.get('color')
        if color:
            queryset = queryset.filter(color__icontains=color)

        # فیلتر قیمت
        min_price = self.request.query_params.get('min_price')
        if min_price:
            try:
                min_price = float(min_price)
                queryset = queryset.filter(price__gte=min_price)
            except ValueError:
                pass

        max_price = self.request.query_params.get('max_price')
        if max_price:
            try:
                max_price = float(max_price)
                queryset = queryset.filter(price__lte=max_price)
            except ValueError:
                pass

        # فیلتر موجودی
        in_stock = self.request.query_params.get('in_stock')
        if in_stock is not None:
            # تبدیل به boolean
            if in_stock.lower() in ['true', '1', 'yes']:
                queryset = queryset.filter(stock__gt=0)
            elif in_stock.lower() in ['false', '0', 'no']:
                queryset = queryset.filter(stock=0)
            # در غیر این صورت نادیده گرفته شود

        return queryset

    def perform_create(self, serializer):
        serializer.save(created_by=self.request.user)

    @action(detail=True, methods=['patch'])
    def price(self, request, pk=None):
        product = self.get_object()
        new_price = request.data.get('price')
        if new_price is None:
            return Response({'error': 'price is required'}, status=status.HTTP_400_BAD_REQUEST)

        old_price = product.price
        product.price = new_price
        product.save()

        PriceHistory.objects.create(
            product=product,
            old_price=old_price,
            new_price=new_price,
            changed_by=request.user
        )
        return Response({'message': 'قیمت با موفقیت تغییر یافت'})

    @action(detail=True, methods=['patch'])
    def stock(self, request, pk=None):
        product = self.get_object()
        new_stock = request.data.get('stock')
        reason = request.data.get('reason', 'adjustment')
        if new_stock is None:
            return Response({'error': 'stock is required'}, status=status.HTTP_400_BAD_REQUEST)

        old_stock = product.stock
        product.stock = new_stock
        product.save()

        StockHistory.objects.create(
            product=product,
            old_stock=old_stock,
            new_stock=new_stock,
            reason=reason,
            changed_by=request.user
        )
        return Response({'message': 'موجودی با موفقیت تغییر یافت'})

    @action(detail=True, methods=['patch'])
    def toggle(self, request, pk=None):
        product = self.get_object()
        product.is_active = not product.is_active
        product.save()
        return Response({'is_active': product.is_active})

class PriceHistoryViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = PriceHistory.objects.all()
    serializer_class = PriceHistorySerializer
    permission_classes = [permissions.IsAuthenticated]

class StockHistoryViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = StockHistory.objects.all()
    serializer_class = StockHistorySerializer
    permission_classes = [permissions.IsAuthenticated]