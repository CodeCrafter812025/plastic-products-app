# core/renderers.py
from rest_framework.renderers import JSONRenderer
from datetime import datetime


class CustomJSONRenderer(JSONRenderer):
    def render(self, data, accepted_media_type=None, renderer_context=None):
        # Already wrapped? (e.g. by our exception handler)
        if isinstance(data, dict) and 'success' in data:
            return super().render(data, accepted_media_type, renderer_context)

        # Skip wrapping for DRF-Spectacular schema endpoint
        if renderer_context:
            view = renderer_context.get('view')
            if view and view.__class__.__module__.startswith('drf_spectacular'):
                return super().render(data, accepted_media_type, renderer_context)

        response = renderer_context.get('response') if renderer_context else None
        status_code = response.status_code if response else 200
        now = datetime.utcnow().isoformat() + 'Z'

        if status_code >= 400:
            # Build error envelope
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

            wrapped = {
                'success': False,
                'error': error,
                'timestamp': now,
            }
        else:
            wrapped = {
                'success': True,
                'data': data,
                'message': None,
                'timestamp': now,
            }

        return super().render(wrapped, accepted_media_type, renderer_context)