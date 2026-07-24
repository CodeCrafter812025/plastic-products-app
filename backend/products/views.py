from django.shortcuts import render

# Create your views here.
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.db import transaction
from .models import Product, PriceHistory, StockHistory
from .serializers import ProductSerializer, PriceHistorySerializer, StockHistorySerializer

class ProductViewSet(viewsets.ModelViewSet):
    queryset = Product.objects.filter(is_active=True)
    serializer_class = ProductSerializer

    def get_permissions(self):
        if self.action in ['create', 'update', 'partial_update', 'destroy']:
            return [permissions.IsAuthenticated()]
        return [permissions.AllowAny()]

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