import logging
import requests
from django.conf import settings
from core.models import Notification

logger = logging.getLogger(__name__)

SMS_MESSAGES = {
    'assigned': "سفارش شما با شماره {order_id} به ویزیتور اختصاص یافت و در حال ارسال است.",
    'loading': "سفارش شما با شماره {order_id} بارگیری شد و به زودی تحویل داده خواهد شد.",
    'delivered': "سفارش شما با شماره {order_id} تحویل داده شد. با تشکر از خرید شما.",
    'cancelled': "سفارش شما با شماره {order_id} توسط مدیریت لغو شد. در صورت نیاز با پشتیبانی تماس بگیرید.",
}

MELIPAYAMAK_URL = "https://rest.payamak-panel.com/api/SendSMS/SendSMS"


def send_order_status_sms(order, event):
    buyer = order.buyer
    if not buyer.phone:
        logger.warning(f"Order {order.id} buyer has no phone number, SMS not sent.")
        _create_notification(order, event, buyer, "ارسال نشد (شماره تلفن موجود نیست)")
        return

    template = SMS_MESSAGES.get(event)
    if not template:
        logger.error(f"Unknown event '{event}' for order {order.id}")
        return

    message = template.format(order_id=order.id)

    username = getattr(settings, 'MELIPAYAMAK_USERNAME', '')
    password = getattr(settings, 'MELIPAYAMAK_PASSWORD', '')
    sender = getattr(settings, 'MELIPAYAMAK_SENDER_NUMBER', '')

    if not all([username, password, sender]):
        logger.error("Melipayamak settings missing. SMS not sent.")
        _create_notification(order, event, buyer, message + " (ارسال نشد - تنظیمات ناقص)")
        return

    success = False
    try:
        data = {
            'username': username,
            'password': password,
            'to': buyer.phone,
            'from': sender,
            'text': message,
            'isflash': False,
        }
        response = requests.post(MELIPAYAMAK_URL, data=data, timeout=10)
        response.raise_for_status()
        result = response.json()
        if result.get('RetStatus') == 1:
            success = True
        else:
            logger.error(f"Melipayamak send failed for order {order.id}: {result}")
    except requests.exceptions.RequestException as e:
        logger.error(f"SMS send error for order {order.id}: {e}")

    note = message if success else f"{message} (ارسال ناموفق)"
    _create_notification(order, event, buyer, note)


def _create_notification(order, event, user, message):
    Notification.objects.create(
        user=user, related_type='order', related_id=order.id,
        type='sms', message=message, is_read=False,
    )