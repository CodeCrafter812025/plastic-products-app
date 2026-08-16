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
from core.services.notifications import send_order_status_sms


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

    partial_update = update   # <-- kept as requested

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

    @action(detail=True, methods=['put', 'patch'])
    @transaction.atomic
    def edit_items(self, request, pk=None):
        order = self.get_object()
        if order.status != 'pending':
            return Response({'error': 'فقط سفارش‌های در انتظار تخصیص قابل ویرایش هستند.'}, status=status.HTTP_400_BAD_REQUEST)

        items_data = request.data.get('items', [])
        if not items_data:
            return Response({'error': 'لیست آیتم‌ها ارسال نشده است.'}, status=status.HTTP_400_BAD_REQUEST)

        current_items = {item.product_id: item for item in order.items.all()}

        product_ids = [item['product_id'] for item in items_data if item.get('quantity', 0) > 0]
        products = Product.objects.select_for_update().filter(id__in=product_ids, is_active=True)
        product_dict = {p.id: p for p in products}

        new_items_dict = {}
        for item_data in items_data:
            product_id = item_data['product_id']
            quantity = item_data.get('quantity', 0)
            if quantity < 0:
                return Response({'error': 'مقدار نمی‌تواند منفی باشد.'}, status=status.HTTP_400_BAD_REQUEST)
            if quantity == 0:
                continue
            product = product_dict.get(product_id)
            if not product:
                return Response({'error': f'محصول با شناسه {product_id} یافت نشد یا غیرفعال است.'}, status=status.HTTP_400_BAD_REQUEST)
            existing_qty = current_items[product_id].quantity if product_id in current_items else 0
            available = product.stock + existing_qty
            if available < quantity:
                return Response({'error': f'موجودی محصول {product.title} کافی نیست (موجودی قابل‌استفاده: {available}).'}, status=status.HTTP_400_BAD_REQUEST)
            new_items_dict[product_id] = {'product': product, 'quantity': quantity}

        for product_id, item in current_items.items():
            if product_id in new_items_dict:
                new_qty = new_items_dict[product_id]['quantity']
                if new_qty < item.quantity:
                    diff = item.quantity - new_qty
                    product = item.product
                    product.stock += diff
                    product.save()
            else:
                product = item.product
                product.stock += item.quantity
                product.save()
                item.delete()

        total_price = 0
        for product_id, data in new_items_dict.items():
            product = data['product']
            qty = data['quantity']
            unit_price = product.price
            subtotal = qty * unit_price
            total_price += subtotal
            if product_id in current_items:
                item = current_items[product_id]
                if qty > item.quantity:
                    product.stock -= (qty - item.quantity)
                    product.save()
                item.quantity = qty
                item.unit_price = unit_price
                item.total_price = subtotal
                item.save()
            else:
                product.stock -= qty
                product.save()
                OrderItem.objects.create(order=order, product=product, quantity=qty, unit_price=unit_price, total_price=subtotal)

        order.total_price = total_price
        order.save()
        OrderStatusHistory.objects.create(order=order, old_status=order.status, new_status=order.status, changed_by=request.user, note='ویرایش آیتم‌های سفارش')
        return Response(OrderSerializer(order).data)
        

    @action(detail=True, methods=['post'])
    @transaction.atomic
    def cancel_admin(self, request, pk=None):
        """
        لغو سفارش توسط ادمین (فقط وضعیت‌های assigned یا loading).
        """
        if request.user.role != 'admin':
            return Response(
                {'error': 'فقط ادمین می‌تواند لغو کند.'},
                status=status.HTTP_403_FORBIDDEN
            )

        order = self.get_object()
        if order.status not in ['assigned', 'loading']:
            return Response(
                {'error': 'سفارش قابل لغو نیست (فقط سفارش‌های تخصیص داده شده یا بارگیری شده قابل لغو هستند).'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # Restore stock
        for item in order.items.all():
            product = item.product
            product.stock += item.quantity
            product.save()

        old_status = order.status   # این خط رو اضافه کنید، قبل از تغییر status
        order.status = 'cancelled'
        order.save()

        OrderStatusHistory.objects.create(
            order=order,
            old_status=old_status,  # will be old status
            new_status='cancelled',
            changed_by=request.user,
            note='لغو توسط ادمین'
        )

        # Send SMS to buyer
        send_order_status_sms(order, 'cancelled')

        return Response({'message': 'سفارش با موفقیت لغو شد'})

    @action(detail=True, methods=['get'])
    def status_history(self, request, pk=None):
        """
        دریافت تاریخچه وضعیت سفارش (فقط برای خریدار، ویزیتور یا ادمین).
        """
        order = self.get_object()
        user = request.user
        if not (user == order.buyer or user == order.visitor or user.role == 'admin'):
            return Response(
                {'error': 'شما دسترسی به تاریخچه وضعیت این سفارش ندارید.'},
                status=status.HTTP_403_FORBIDDEN
            )
        history = order.status_histories.all().order_by('changed_at')
        serializer = OrderStatusHistorySerializer(history, many=True)
        return Response(serializer.data)


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
        order_id = request.data.get('order_id')
        visitor_id = request.data.get('new_visitor_id')
        reason = request.data.get('reason', '')

        if not order_id or not visitor_id:
            return Response(
                {'error': 'order_id و new_visitor_id الزامی هستند.'},
                status=status.HTTP_400_BAD_REQUEST
            )

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

        # Send SMS notification to buyer
        send_order_status_sms(order, 'assigned')

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
            send_order_status_sms(order, 'loading')
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
            send_order_status_sms(order, 'delivered')
        else:
            return Response({'error': 'تغییر وضعیت مجاز نیست'}, status=status.HTTP_400_BAD_REQUEST)

        return Response({'message': f'وضعیت به {new_status} تغییر یافت'})