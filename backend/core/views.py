from django.shortcuts import render

# Create your views here.
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.exceptions import MethodNotAllowed

from .models import SystemSetting, Notification
from .serializers import SystemSettingSerializer, NotificationSerializer
from users.permissions import IsAdminUserRole

# P2-3 imports



from django.db.models import Count, Q, Subquery, OuterRef, Value, Avg, ExpressionWrapper, F, fields, Sum
from django.db.models.functions import Extract, Coalesce, TruncDate, TruncWeek
from users.models import User
from orders.models import Order, OrderAssignment, OrderStatusHistory, OrderItem
from products.models import Product
from datetime import datetime, timedelta
from django.utils import timezone


class SystemSettingViewSet(viewsets.ModelViewSet):
    """
    مدیریت تنظیمات سیستم (فقط ادمین)
    """
    queryset = SystemSetting.objects.all()
    serializer_class = SystemSettingSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_permissions(self):
        # فقط ادمین می‌تواند تغییر دهد، همه می‌توانند بخوانند
        if self.action in ['create', 'update', 'partial_update', 'destroy']:
            return [IsAdminUserRole()]
        return [permissions.AllowAny()]


class NotificationViewSet(viewsets.ModelViewSet):
    """
    مدیریت اعلان‌های کاربر – فقط خواندنی برای مشتریان.
    ایجاد اعلان‌ها فقط توسط backend (مثلاً هنگام ارسال SMS) انجام می‌شود.
    """
    serializer_class = NotificationSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        # هر کاربر فقط اعلان‌های خودش را می‌بیند
        return Notification.objects.filter(user=self.request.user)

    def get_permissions(self):
        # اجازه نمی‌دهیم هیچ کاربری (حتی ادمین) اعلان ایجاد/ویرایش/حذف کند
        if self.action in ['create', 'update', 'partial_update', 'destroy']:
            return [permissions.IsAuthenticated()]  # ولی متدهای مربوطه override شده‌اند
        return [permissions.IsAuthenticated()]

    def create(self, request, *args, **kwargs):
        raise MethodNotAllowed('POST')

    def update(self, request, *args, **kwargs):
        raise MethodNotAllowed('PUT')

    def partial_update(self, request, *args, **kwargs):
        raise MethodNotAllowed('PATCH')

    def destroy(self, request, *args, **kwargs):
        raise MethodNotAllowed('DELETE')

    @action(detail=True, methods=['post'])
    def mark_read(self, request, pk=None):
        """علامت‌گذاری یک اعلان به عنوان خوانده شده"""
        notification = self.get_object()
        notification.is_read = True
        notification.save()
        return Response({'status': 'marked read'})

    @action(detail=False, methods=['post'])
    def mark_all_read(self, request):
        """علامت‌گذاری تمام اعلان‌های خوانده نشده کاربر جاری"""
        notifications = self.get_queryset().filter(is_read=False)
        notifications.update(is_read=True)
        return Response({'status': 'all marked read'})


class AdminReportsViewSet(viewsets.GenericViewSet):
    """
    گزارش‌های مدیریتی – فقط ادمین
    """
    permission_classes = [permissions.IsAuthenticated, IsAdminUserRole]

    @action(detail=False, methods=['get'], url_path='visitor-performance')
    def visitor_performance(self, request):
        visitors = User.objects.filter(role='visitor')

        delivered_subquery = Subquery(
            OrderAssignment.objects.filter(
                new_visitor=OuterRef('pk'),
                order__status='delivered'
            ).values('new_visitor').annotate(cnt=Count('order', distinct=True)).values('cnt')
        )

        cancelled_subquery = Subquery(
            OrderAssignment.objects.filter(
                new_visitor=OuterRef('pk'),
                order__status='cancelled'
            ).values('new_visitor').annotate(cnt=Count('order', distinct=True)).values('cnt')
        )

        avg_time_subquery = Subquery(
            Order.objects.filter(
                assignments__new_visitor=OuterRef('pk')
            ).distinct().annotate(
                assigned_at=Subquery(
                    OrderStatusHistory.objects.filter(
                        order=OuterRef('pk'),
                        new_status='assigned'
                    ).values('changed_at')[:1]
                ),
                delivered_at=Subquery(
                    OrderStatusHistory.objects.filter(
                        order=OuterRef('pk'),
                        new_status='delivered'
                    ).values('changed_at')[:1]
                )
            ).filter(
                assigned_at__isnull=False,
                delivered_at__isnull=False
            ).annotate(
                avg_diff=Avg(
                    ExpressionWrapper(
                        Extract('delivered_at', 'epoch') - Extract('assigned_at', 'epoch'),
                        output_field=fields.FloatField()
                    )
                )
            ).values('avg_diff')[:1]
        )

        visitors = visitors.annotate(
            total_assigned=Count('new_assignments'),
            delivered=Coalesce(delivered_subquery, Value(0)),
            cancelled=Coalesce(cancelled_subquery, Value(0)),
            avg_delivery_seconds=Coalesce(avg_time_subquery, Value(None, output_field=fields.FloatField()))
        )

        data = []
        for visitor in visitors:
            data.append({
                'id': visitor.id,
                'phone': visitor.phone,
                'full_name': visitor.full_name,
                'total_assigned': visitor.total_assigned,
                'delivered': visitor.delivered,
                'cancelled': visitor.cancelled,
                'avg_delivery_seconds': visitor.avg_delivery_seconds,
            })

        return Response(data)

    @action(detail=False, methods=['get'], url_path='order-counts')
    def order_counts(self, request):
        counts = Order.objects.values('status').annotate(count=Count('id'))
        result = {item['status']: item['count'] for item in counts}
        status_choices = dict(Order.STATUS_CHOICES)
        for status_code, _ in status_choices.items():
            if status_code not in result:
                result[status_code] = 0
        return Response(result)

    @action(detail=False, methods=['get'], url_path='revenue')
    def revenue(self, request):
        from_date = request.query_params.get('from')
        to_date = request.query_params.get('to')

        if not from_date or not to_date:
            return Response(
                {'error': 'لطفاً پارامترهای "from" و "to" را به فرمت YYYY-MM-DD وارد کنید.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            from_dt = timezone.make_aware(datetime.strptime(from_date, '%Y-%m-%d'))
            to_dt = timezone.make_aware(datetime.strptime(to_date, '%Y-%m-%d')) + timedelta(days=1)
        except ValueError:
            return Response(
                {'error': 'فرمت تاریخ نامعتبر است. از YYYY-MM-DD استفاده کنید.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        orders = Order.objects.filter(
            status='delivered',
            created_at__gte=from_dt,
            created_at__lt=to_dt
        )
        total_revenue = orders.aggregate(total=Sum('total_price'))['total'] or 0

        return Response({'from': from_date, 'to': to_date, 'total_revenue': total_revenue})

    @action(detail=False, methods=['get'], url_path='top-products')
    def top_products(self, request):
        try:
            limit = int(request.query_params.get('limit', 10))
        except ValueError:
            return Response({'error': 'limit باید عدد باشد.'}, status=status.HTTP_400_BAD_REQUEST)
        if limit <= 0:
            limit = 10

        top_items = OrderItem.objects.filter(
            order__status__in=['pending', 'assigned', 'loading', 'delivered']
        ).values('product').annotate(total_quantity=Sum('quantity')).order_by('-total_quantity')[:limit]

        product_ids = [item['product'] for item in top_items]
        products = Product.objects.filter(id__in=product_ids).only('id', 'title')
        product_map = {p.id: p.title for p in products}

        return Response([
            {'product_id': item['product'], 'product_title': product_map.get(item['product'], 'نامشخص'), 'total_quantity_sold': item['total_quantity']}
            for item in top_items
        ])

    @action(detail=False, methods=['get'], url_path='low-stock')
    def low_stock(self, request):
        try:
            threshold = int(request.query_params.get('threshold', 10))
        except ValueError:
            return Response({'error': 'threshold باید عدد باشد.'}, status=status.HTTP_400_BAD_REQUEST)
        if threshold < 0:
            threshold = 10

        products = Product.objects.filter(stock__lt=threshold, is_active=True).values('id', 'title', 'stock').order_by('stock')
        return Response(list(products))

    @action(detail=False, methods=['get'], url_path='signups')
    def signups(self, request):
        period = request.query_params.get('period', 'day')
        if period not in ['day', 'week']:
            period = 'day'

        queryset = User.objects.filter(role='buyer')
        truncated = TruncDate('created_at') if period == 'day' else TruncWeek('created_at')

        signups = queryset.annotate(period_start=truncated).values('period_start').annotate(count=Count('id')).order_by('period_start')

        return Response([
            {'period': item['period_start'].isoformat() if item['period_start'] else None, 'count': item['count']}
            for item in signups
        ])