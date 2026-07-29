from django.db import transaction
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response

from products.models import Product
from users.models import User
from users.permissions import IsAdminUserRole  # added for role check
from .models import Order, OrderItem, OrderAssignment, OrderStatusHistory, CartItem
from .serializers import (
    OrderSerializer, OrderItemSerializer, OrderAssignmentSerializer,
    OrderStatusHistorySerializer, CartItemSerializer, CartItemAddSerializer
)


class CartViewSet(viewsets.GenericViewSet):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = CartItemSerializer

    def get_queryset(self):
        return CartItem.objects.filter(user=self.request.user)

    def list(self, request, *args, **kwargs):
        queryset = self.get_queryset()
        serializer = self.get_serializer(queryset, many=True)
        total = sum(item.quantity * item.product.price for item in queryset)
        return Response({
            'items': serializer.data,
            'total': total
        })

    @transaction.atomic
    def create(self, request, *args, **kwargs):
        serializer = CartItemAddSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        product_id = serializer.validated_data['product_id']
        quantity = serializer.validated_data['quantity']

        try:
            product = Product.objects.get(id=product_id, is_active=True)
        except Product.DoesNotExist:
            return Response(
                {'error': 'محصول مورد نظر یافت نشد یا غیرفعال است.'},
                status=status.HTTP_404_NOT_FOUND
            )

        if product.stock < quantity:
            return Response(
                {'error': f'موجودی محصول {product.title} کافی نیست (موجودی: {product.stock}).'},
                status=status.HTTP_400_BAD_REQUEST
            )

        cart_item, created = CartItem.objects.get_or_create(
            user=request.user,
            product=product,
            defaults={'quantity': quantity}
        )
        if not created:
            cart_item.quantity = quantity
            cart_item.save()

        out_serializer = CartItemSerializer(cart_item)
        return Response(out_serializer.data, status=status.HTTP_201_CREATED)

    def update(self, request, pk=None, *args, **kwargs):
        try:
            cart_item = CartItem.objects.get(id=pk, user=request.user)
        except CartItem.DoesNotExist:
            return Response(
                {'error': 'آیتمی با این شناسه در سبد خرید شما وجود ندارد.'},
                status=status.HTTP_404_NOT_FOUND
            )

        serializer = CartItemAddSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        quantity = serializer.validated_data['quantity']
        product = cart_item.product

        if product.stock < quantity:
            return Response(
                {'error': f'موجودی محصول {product.title} کافی نیست (موجودی: {product.stock}).'},
                status=status.HTTP_400_BAD_REQUEST
            )

        cart_item.quantity = quantity
        cart_item.save()

        out_serializer = CartItemSerializer(cart_item)
        return Response(out_serializer.data)

    partial_update = update

    @transaction.atomic
    def destroy(self, request, pk=None):
        try:
            item = CartItem.objects.get(id=pk, user=request.user)
            item.delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        except CartItem.DoesNotExist:
            return Response({'error': 'آیتم یافت نشد'}, status=status.HTTP_404_NOT_FOUND)

    @action(detail=False, methods=['delete'])
    def clear(self, request):
        CartItem.objects.filter(user=request.user).delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class OrderViewSet(viewsets.ModelViewSet):
    serializer_class = OrderSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        if user.role == 'admin':
            return Order.objects.all()
        elif user.role == 'visitor':
            return Order.objects.filter(visitor=user)
        return Order.objects.filter(buyer=user)

    @transaction.atomic
    def create(self, request, *args, **kwargs):
        cart_items = CartItem.objects.filter(user=request.user)
        if not cart_items.exists():
            return Response({'error': 'سبد خرید شما خالی است.'}, status=status.HTTP_400_BAD_REQUEST)

        product_ids = list(cart_items.values_list('product_id', flat=True))
        locked_products = Product.objects.select_for_update().filter(id__in=product_ids)
        product_dict = {p.id: p for p in locked_products}

        total_price = 0
        order_items_to_create = []

        for item in cart_items:
            product = product_dict.get(item.product_id)
            if not product:
                return Response(
                    {'error': f'محصول با شناسه {item.product_id} یافت نشد.'},
                    status=status.HTTP_400_BAD_REQUEST
                )
            if product.stock < item.quantity:
                return Response(
                    {'error': f'موجودی محصول {product.title} کافی نیست (موجودی: {product.stock}).'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            unit_price = product.price
            total_price += unit_price * item.quantity
            order_items_to_create.append({
                'product': product,
                'quantity': item.quantity,
                'unit_price': unit_price,
            })

            product.stock -= item.quantity
            product.save(update_fields=['stock'])

        order = Order.objects.create(
            buyer=request.user,
            total_price=total_price,
            status='pending'
        )

        for item_data in order_items_to_create:
            OrderItem.objects.create(
                order=order,
                product=item_data['product'],
                quantity=item_data['quantity'],
                unit_price=item_data['unit_price'],
                total_price=item_data['unit_price'] * item_data['quantity']
            )

        OrderStatusHistory.objects.create(
            order=order,
            old_status=None,
            new_status='pending',
            changed_by=request.user,
            note='سفارش ایجاد شد'
        )

        cart_items.delete()

        return Response({
            'order_id': order.id,
            'status': order.status,
            'total_price': order.total_price,
            'message': 'سفارش با موفقیت ثبت شد'
        }, status=status.HTTP_201_CREATED)

    @action(detail=True, methods=['delete'])
    def cancel(self, request, pk=None):
        order = self.get_object()
        if order.status != 'pending':
            return Response({'error': 'سفارش قابل لغو نیست'}, status=status.HTTP_400_BAD_REQUEST)

        order.status = 'cancelled'
        order.save()

        for item in order.items.all():
            product = item.product
            product.stock += item.quantity
            product.save()

        OrderStatusHistory.objects.create(
            order=order,
            old_status='pending',
            new_status='cancelled',
            changed_by=request.user,
            note='لغو توسط خریدار'
        )
        return Response({'message': 'سفارش لغو شد'})


class OrderAssignmentViewSet(viewsets.ModelViewSet):
    serializer_class = OrderAssignmentSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        if user.role == 'admin':
            return OrderAssignment.objects.all()
        elif user.role == 'visitor':
            return OrderAssignment.objects.filter(new_visitor=user)
        return OrderAssignment.objects.none()

    def get_permissions(self):
        # Only admin can create/update/destroy assignments
        if self.action in ['create', 'update', 'partial_update', 'destroy']:
            return [IsAdminUserRole()]
        return [permissions.IsAuthenticated()]

    @transaction.atomic
    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        order_id = request.data.get('order_id')
        visitor_id = request.data.get('new_visitor_id')
        reason = request.data.get('reason', '')

        try:
            order = Order.objects.get(id=order_id, status='pending')
        except Order.DoesNotExist:
            return Response({'error': 'سفارش یافت نشد یا قابل تخصیص نیست'}, status=status.HTTP_400_BAD_REQUEST)

        try:
            visitor = User.objects.get(id=visitor_id, role='visitor', is_active=True)
        except User.DoesNotExist:
            return Response({'error': 'ویزیتور نامعتبر است'}, status=status.HTTP_400_BAD_REQUEST)

        old_visitor = order.visitor
        assignment = OrderAssignment.objects.create(
            order=order,
            old_visitor=old_visitor,
            new_visitor=visitor,
            assigned_by=request.user,
            reason=reason
        )

        order.visitor = visitor
        order.status = 'assigned'
        order.save()

        OrderStatusHistory.objects.create(
            order=order,
            old_status='pending',
            new_status='assigned',
            changed_by=request.user,
            note=f'تخصیص به {visitor.full_name}'
        )

        return Response(OrderAssignmentSerializer(assignment).data, status=status.HTTP_201_CREATED)


class VisitorOrderStatusViewSet(viewsets.GenericViewSet):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = OrderSerializer

    @action(detail=True, methods=['patch'])
    def status(self, request, pk=None):
        try:
            order = Order.objects.get(id=pk, visitor=request.user)
        except Order.DoesNotExist:
            return Response({'error': 'سفارش یافت نشد یا به شما تعلق ندارد'}, status=status.HTTP_404_NOT_FOUND)

        new_status = request.data.get('status')
        if new_status not in ['loading', 'delivered']:
            return Response({'error': 'وضعیت نامعتبر است'}, status=status.HTTP_400_BAD_REQUEST)

        if order.status == 'assigned' and new_status == 'loading':
            order.status = new_status
            order.save()
            OrderStatusHistory.objects.create(
                order=order,
                old_status='assigned',
                new_status='loading',
                changed_by=request.user,
                note='بارگیری شد'
            )
        elif order.status == 'loading' and new_status == 'delivered':
            order.status = new_status
            order.save()
            OrderStatusHistory.objects.create(
                order=order,
                old_status='loading',
                new_status='delivered',
                changed_by=request.user,
                note='تحویل داده شد'
            )
        else:
            return Response({'error': 'تغییر وضعیت مجاز نیست'}, status=status.HTTP_400_BAD_REQUEST)

        return Response({'message': f'وضعیت به {new_status} تغییر یافت'})