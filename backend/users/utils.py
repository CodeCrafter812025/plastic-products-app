import requests
import logging
from django.conf import settings

logger = logging.getLogger(__name__)

def send_otp_sms(phone_number, otp_code):
    """
    ارسال پیامک کد تایید از طریق وب‌سرویس خدماتی (پترن) ملی پیامک
    """
    # در صورت فعال بودن DEBUG، پیامک واقعی ارسال نمی‌شود تا شارژ هدر نرود.
    # برای تست واقعی روی لوکال، می‌توانید این شرط را موقتاً غیرفعال کنید.
    if settings.DEBUG:
        print(f"\n{'='*40}\n🚀 MOCK SMS: OTP {otp_code} sent to {phone_number}\n{'='*40}\n")
        return True

    # مقادیر زیر در مرحله استقرار از متغیرهای محیطی (.env) خوانده می‌شوند
    username = getattr(settings, 'MELIPAYAMAK_USERNAME', '')
    password = getattr(settings, 'MELIPAYAMAK_PASSWORD', '')
    body_id = getattr(settings, 'MELIPAYAMAK_OTP_PATTERN_ID', '')
    
    # اگر اطلاعات حساب وارد نشده باشد، خطا لاگ می‌شود
    if not username or not password or not body_id:
        logger.error("SMS Error: MeliPayamak credentials are not fully configured in settings.")
        return False

    url = "https://rest.payamak-panel.com/api/SendSMS/BaseServiceNumber"
    
    data = {
        "username": username,
        "password": password,
        "text": [str(otp_code)],
        "to": phone_number,
        "bodyId": int(body_id)
    }
    
    try:
        response = requests.post(url, json=data, timeout=10)
        result = response.json()
        
        # در API ملی پیامک، RetStatus = 1 یعنی ارسال موفق
        if result.get("RetStatus") == 1:
            logger.info(f"OTP successfully sent to {phone_number} via MeliPayamak.")
            return True
        else:
            logger.error(f"Failed to send SMS to {phone_number}. MeliPayamak Error: {result.get('StrRetStatus')}")
            return False
            
    except requests.exceptions.RequestException as e:
        logger.error(f"Network error while calling MeliPayamak API for {phone_number}: {e}")
        return False
    except Exception as e:
        logger.error(f"Unexpected error in send_otp_sms for {phone_number}: {e}")
        return False
