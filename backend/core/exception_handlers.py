# core/exception_handlers.py
from django.db.models.deletion import ProtectedError, RestrictedError
from rest_framework.response import Response
from rest_framework import status as drf_status
from rest_framework.views import exception_handler as drf_exception_handler
from datetime import datetime


def custom_exception_handler(exc, context):
    # Django raises these (instead of an APIException) when a delete is
    # blocked by on_delete=RESTRICT/PROTECT. DRF's exception_handler doesn't
    # know about them and returns None, which would otherwise surface as a
    # raw 500. Handled here (not in a specific view) so any model using
    # RESTRICT/PROTECT gets the same clean 400 behavior.
    if isinstance(exc, (RestrictedError, ProtectedError)):
        now = datetime.utcnow().isoformat() + 'Z'
        return Response(
            {
                'success': False,
                'error': {
                    'code': str(drf_status.HTTP_400_BAD_REQUEST),
                    'message': 'این محصول قبلاً در سفارشی استفاده شده و قابل حذف نیست؛ به‌جای حذف، آن را غیرفعال کنید.',
                },
                'timestamp': now,
            },
            status=drf_status.HTTP_400_BAD_REQUEST,
        )

    response = drf_exception_handler(exc, context)

    if response is not None:
        now = datetime.utcnow().isoformat() + 'Z'
        status_code = response.status_code
        data = response.data

        error = {'code': str(status_code), 'message': None}
        if isinstance(data, dict):
                if 'detail' in data:
                    error['message'] = data['detail']
                elif 'error' in data:
                    error['message'] = data['error']
                elif 'non_field_errors' in data:
                    error['message'] = data['non_field_errors']
                else:
                    error['message'] = data
        else:
            error['message'] = data if data is not None else "خطایی رخ داده است"

        response.data = {
            'success': False,
            'error': error,
            'timestamp': now,
        }

    return response