from django.shortcuts import render

# Create your views here.
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.db import transaction
from .models import Product, PriceHistory, StockHistory
from .serializers import ProductSerializer, PriceHistorySerializer, StockHistorySerializer

from users.permissions import IsAdminUserRole

# P2-6 imports
from django.core.files.storage import FileSystemStorage
from django.conf import settings
from django.utils import timezone
import os


class ProductViewSet(viewsets.ModelViewSet):
    queryset = Product.objects.filter(is_active=True)
    serializer_class = ProductSerializer

    def get_permissions(self):
        if self.action in ['create', 'update', 'partial_update', 'destroy', 'price', 'stock', 'toggle', 'upload_image']:
            return [IsAdminUserRole()]  # فقط ادمین حق تغییرات دارد
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
            if quality in ['اولیه', 'بازیافتی']:
                queryset = queryset.filter(quality=quality)
            # در غیر این صورت نادیده گرفته شود

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
            if in_stock.lower() in ['true', '1', 'yes']:
                queryset = queryset.filter(stock__gt=0)
            elif in_stock.lower() in ['false', '0', 'no']:
                queryset = queryset.filter(stock=0)

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

    @action(detail=True, methods=['post'])
    def upload_image(self, request, pk=None):
        """
        بارگذاری یک تصویر برای محصول (فقط ادمین)
        - فرمت‌های مجاز: jpg, jpeg, png, gif, webp
        - حداکثر ۵ تصویر
        """
        product = self.get_object()

        # بررسی تعداد تصاویر موجود
        if len(product.image_urls) >= 5:
            return Response(
                {'error': 'حداکثر ۵ تصویر مجاز است.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # بررسی وجود فایل
        if 'image' not in request.FILES:
            return Response(
                {'error': 'فایل تصویر ارسال نشده است.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        image_file = request.FILES['image']

        # اعتبارسنجی پسوند فایل (بدون نیاز به Pillow)
        ext = os.path.splitext(image_file.name)[1].lower()
        allowed_extensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp']
        if ext not in allowed_extensions:
            return Response(
                {'error': 'فرمت فایل پشتیبانی نمی‌شود. فرمت‌های مجاز: jpg, jpeg, png, gif, webp'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # ذخیره فایل با FileSystemStorage
        fs = FileSystemStorage()
        # ایجاد نام یکتا با استفاده از ID محصول و زمان
        filename = f"product_{product.id}_{timezone.now().strftime('%Y%m%d%H%M%S')}{ext}"
        saved_path = fs.save(filename, image_file)
        url = fs.url(saved_path)

        # اضافه کردن URL به لیست تصاویر
        product.image_urls.append(url)
        product.save()

        return Response({
            'message': 'تصویر با موفقیت بارگذاری شد.',
            'url': url,
            'image_urls': product.image_urls
        })


class PriceHistoryViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = PriceHistory.objects.all()
    serializer_class = PriceHistorySerializer
    permission_classes = [permissions.IsAuthenticated]


class StockHistoryViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = StockHistory.objects.all()
    serializer_class = StockHistorySerializer
    permission_classes = [permissions.IsAuthenticated]