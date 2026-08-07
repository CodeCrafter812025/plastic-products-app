# core/exception_handlers.py
from rest_framework.views import exception_handler as drf_exception_handler
from datetime import datetime


def custom_exception_handler(exc, context):
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