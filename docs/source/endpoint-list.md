# لیست کامل Endpointها

این سند مستقیماً از کد origin/main در تاریخ 2026-09-14 استخراج شده.

جزئیات کامل هر endpoint (بدنه‌ی دقیق درخواست، شکل دقیق پاسخ، خطاهای
واقعی، یافته‌های رفتاری) در `api-specification.md` است؛ این فایل فقط
یک فهرست سریع (مسیر + متد + دسترسی) است، برای مرور کلی.

> توضیح ستون «Auth»: «-» یعنی بدون نیاز به توکن. «هر کاربر» یعنی هر
> کاربر لاگین‌شده (JWT معتبر)، صرف‌نظر از نقش. «admin» یعنی
> `IsAdminUserRole` (role=admin) الزامی است.
>
> علامت **⚠️** یعنی این endpoint یک رفتار واقعی-ولی-غیرمنتظره دارد که
> در `api-specification.md` با جزئیات توضیح داده شده (مثلاً حذف واقعی
> به‌جای غیرفعال‌سازی، یا endpointی که عملاً کار نمی‌کند).

## Auth — پیشوند `/api/v1/auth/`

| متد | مسیر | Auth |
|---|---|---|
| POST | `/auth/otp/request/` | - |
| POST | `/auth/otp/verify/` | - (به‌جز `purpose=change_phone`: هر کاربر) |
| POST | `/auth/verify-admin-pin/` | - |

## Users — پیشوند `/api/v1/users/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/users/` | هر کاربر |
| POST | `/users/` | هر کاربر — ⚠️ عملاً همیشه با ۵۰۰ خام شکست می‌خورد |
| GET | `/users/{id}/` | هر کاربر (فقط خودش، مگر admin) |
| PUT | `/users/{id}/` | هر کاربر — ⚠️ تغییر `phone` به مقدار تکراری، ۵۰۰ خام می‌دهد |
| PATCH | `/users/{id}/` | هر کاربر — همان ⚠️ بالا |
| DELETE | `/users/{id}/` | هر کاربر — ⚠️ حذف واقعی و دائمی حساب، نه غیرفعال‌سازی؛ هر کاربر می‌تواند حساب خودش را پاک کند |
| POST | `/users/{id}/toggle/` | admin |
| POST | `/users/create_visitor/` | admin |
| POST | `/users/set-admin-pin/` | admin (فقط برای خودش) |

## Deletion Requests — پیشوند `/api/v1/deletion-requests/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/deletion-requests/` | هر کاربر |
| POST | `/deletion-requests/` | هر کاربر |
| GET | `/deletion-requests/{id}/` | هر کاربر (خودش، مگر admin) |
| PUT | `/deletion-requests/{id}/` | هر کاربر |
| PATCH | `/deletion-requests/{id}/` | هر کاربر |
| DELETE | `/deletion-requests/{id}/` | هر کاربر |
| POST | `/deletion-requests/{id}/review/` | admin |

## Products — پیشوند `/api/v1/products/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/products/` | - |
| POST | `/products/` | admin |
| GET | `/products/{id}/` | - (محصول غیرفعال فقط برای admin) |
| PUT | `/products/{id}/` | admin |
| PATCH | `/products/{id}/` | admin |
| DELETE | `/products/{id}/` | admin — اگر در سفارش/تاریخچه استفاده شده باشد، ۴۰۰ به‌جای موفقیت |
| PATCH | `/products/{id}/price/` | admin |
| PATCH | `/products/{id}/stock/` | admin — ⚠️ `reason` هیچ اعتبارسنجی‌ای ندارد |
| PATCH | `/products/{id}/toggle/` | admin |
| POST | `/products/{id}/upload_image/` | admin — ⚠️ مسیر با زیرخط، بدون محدودیت حجم فایل |

## Price / Stock Histories (فقط‌خواندنی)

| متد | مسیر | Auth |
|---|---|---|
| GET | `/price-histories/` | هر کاربر — ⚠️ کل تاریخچه‌ی همه‌ی محصولات، بدون فیلتر |
| GET | `/price-histories/{id}/` | هر کاربر |
| GET | `/stock-histories/` | هر کاربر — همان ⚠️ بالا |
| GET | `/stock-histories/{id}/` | هر کاربر |

## Cart — پیشوند `/api/v1/cart/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/cart/` | هر کاربر |
| POST | `/cart/` | هر کاربر |
| PUT | `/cart/{id}/` | هر کاربر — ⚠️ `product_id` در بدنه نادیده گرفته می‌شود |
| PATCH | `/cart/{id}/` | هر کاربر — ⚠️ کاملاً همان PUT است (partial واقعی نیست) |
| DELETE | `/cart/{id}/` | هر کاربر |
| DELETE | `/cart/clear/` | هر کاربر |

(توجه: `GET /cart/{id}/` اصلاً وجود ندارد.)

## Orders — پیشوند `/api/v1/orders/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/orders/` | هر کاربر (طبق نقش) — بدون فیلتر query param |
| POST | `/orders/` | هر کاربر — از سبد خرید فعلی |
| GET | `/orders/{id}/` | هر کاربر (طبق دسترسی) |
| PUT | `/orders/{id}/` | هر کاربر — ⚠️ بدون هیچ فیلد قابل‌نوشتن، عملاً بی‌اثر |
| PATCH | `/orders/{id}/` | هر کاربر — همان ⚠️ بالا |
| DELETE | `/orders/{id}/` | هر کاربر (طبق دسترسی) — ⚠️ حذف فیزیکی کامل سفارش، نه لغو |
| DELETE | `/orders/{id}/cancel/` | خریدار (فقط `pending`) |
| PUT/PATCH | `/orders/{id}/edit_items/` | هر کاربر (فقط `pending`) — مسیر با زیرخط |
| POST | `/orders/{id}/cancel_admin/` | admin (فقط `assigned`/`loading`) — مسیر با زیرخط |
| GET | `/orders/{id}/status_history/` | خریدار/ویزیتور تخصیص‌یافته/admin — مسیر با زیرخط |
| GET | `/orders/{id}/invoice/` | خریدار/ویزیتور تخصیص‌یافته/admin |
| GET | `/orders/{id}/invoice_pdf/` | همان بالا — ⚠️ خروجی PDF خام، بدون envelope؛ تاریخ صدور به شمسی |

## Order Assignments — پیشوند `/api/v1/order-assignments/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/order-assignments/` | هر کاربر (admin: همه، visitor: خودش، بقیه: خالی) |
| POST | `/order-assignments/` | admin (فقط سفارش `pending`) |
| GET | `/order-assignments/{id}/` | هر کاربر (طبق دسترسی بالا) |
| PUT | `/order-assignments/{id}/` | admin — ⚠️ ویرایش خام، بدون اثر روی وضعیت سفارش |
| PATCH | `/order-assignments/{id}/` | admin — همان ⚠️ بالا |
| DELETE | `/order-assignments/{id}/` | admin — بدون اثر روی وضعیت سفارش |

## Visitor Order Status — پیشوند `/api/v1/visitor/orders/`

| متد | مسیر | Auth |
|---|---|---|
| PATCH | `/visitor/orders/{id}/status/` | ویزیتور تخصیص‌یافته (فقط `loading`/`delivered`) |

## System Settings — پیشوند `/api/v1/system-settings/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/system-settings/` | - |
| POST | `/system-settings/` | admin |
| GET | `/system-settings/{key}/` | - (توجه: `{key}` رشته است، نه id عددی) |
| PUT | `/system-settings/{key}/` | admin |
| PATCH | `/system-settings/{key}/` | admin |
| DELETE | `/system-settings/{key}/` | admin |

## Notifications — پیشوند `/api/v1/notifications/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/notifications/` | هر کاربر (فقط خودش) |
| GET | `/notifications/{id}/` | هر کاربر (فقط خودش) |
| POST/PUT/PATCH/DELETE | `/notifications/` یا `/notifications/{id}/` | همیشه ۴۰۵، حتی برای admin |
| POST | `/notifications/{id}/mark_read/` | هر کاربر — مسیر با زیرخط |
| POST | `/notifications/mark_all_read/` | هر کاربر — مسیر با زیرخط |

## Admin Reports — پیشوند `/api/v1/admin-reports/`

| متد | مسیر | Auth |
|---|---|---|
| GET | `/admin-reports/visitor-performance/` | admin (همه) یا visitor (فقط خودش)؛ بقیه ۴۰۳ |
| GET | `/admin-reports/order-counts/` | admin |
| GET | `/admin-reports/revenue/` | admin — `from`/`to` الزامی |
| GET | `/admin-reports/top-products/` | admin — `limit` اختیاری |
| GET | `/admin-reports/low-stock/` | admin — `threshold` اختیاری |
| GET | `/admin-reports/signups/` | admin — `period` اختیاری |

## خارج از `/api/v1/`

| متد | مسیر | توضیح |
|---|---|---|
| POST | `/api/token/refresh/` | simplejwt پیش‌فرض — ⚠️ احتمال شکست به‌خاطر نبود `token_blacklist` در `INSTALLED_APPS` |
| GET | `/api/schema/` | OpenAPI schema خام (بدون envelope) |
| GET | `/api/docs/` | Swagger UI (HTML) |
| * | `/admin/` | پنل جنگو، خارج از این REST API |
| GET | `/media/...` | فایل‌های رسانه، فقط در `DEBUG=True` |
