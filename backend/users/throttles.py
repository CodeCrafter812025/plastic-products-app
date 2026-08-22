from rest_framework.throttling import AnonRateThrottle, SimpleRateThrottle

class OTPRequestThrottle(AnonRateThrottle):
    scope = 'otp_request'


class OTPPhoneThrottle(SimpleRateThrottle):
    scope = 'otp_request_phone'

    def get_cache_key(self, request, view):
        phone = request.data.get('phone')
        if not phone:
            return None
        return self.cache_format % {'scope': self.scope, 'ident': phone}