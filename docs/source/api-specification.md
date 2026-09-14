# مشخصات API — استخراج‌شده از کد واقعی

این سند مستقیماً از کد origin/main در تاریخ 2026-09-14 استخراج شده.

> روش تولید: برای هر اپ (`users`, `products`, `orders`, `core`) فایل‌های
> `plastic_products/urls.py` + `views.py` + `serializers.py` + `models.py`
> + `settings.py` + `core/renderers.py` + `core/exception_handlers.py`
> مستقیماً خونده شدن. جایی که رفتار واقعی کد با انتظار معمول یا با
> نسخه‌های قبلی این سند فرق داشت، با علامت **⚠️ یافته** مشخص شده.
> نسخه‌های قبلی این فایل شامل endpointهایی بودن که اصلاً در کد وجود
> نداشتن (مثل `/api/v1/profile`, `/api/v1/admin/dashboard`,
> `/api/v1/auth/logout`) — این نسخه فقط چیزی رو مستند می‌کنه که واقعاً
> توی کد پیاده شده.

---

## ۱. قراردادهای عمومی

### Base URL

همه‌ی endpointهای این سند (به‌جز مواردی که صریحاً استثنا شدن) زیر
`/api/v1/` هستن — از `DefaultRouter` در `plastic_products/urls.py`.

سه مسیر خارج از `/api/v1/` هم وجود دارن:
- `POST /api/token/refresh/` (از `rest_framework_simplejwt`، **نه** زیر `/api/v1/`)
- `GET /api/schema/` (schema خام OpenAPI، از پوشش envelope زیر معاف است)
- `GET /api/docs/` (Swagger UI؛ HTML است، نه JSON)
- `/admin/` (پنل ادمین جنگو، بخشی از این REST API نیست)

### ⚠️ یافته: همه‌ی پاسخ‌ها (حتی موفق) یک envelope دارن

توی `settings.py`:
```python
'DEFAULT_RENDERER_CLASSES': (
    'core.renderers.CustomJSONRenderer',
    'rest_framework.renderers.BrowsableAPIRenderer',
),
```
`core/renderers.py: CustomJSONRenderer` **هر** پاسخ DRF (چه موفق چه خطا) رو —
مگر پاسخی که خودش از قبل کلید `success` داشته باشه، یا view مربوط به
`drf_spectacular` باشه — می‌پیچه. یعنی هر Response ای که توی این سند
می‌بینید («شکل پاسخ» هر endpoint)، در واقع زیر کلید `data` این envelope
قرار می‌گیره، نه مستقیم در ریشه‌ی JSON:

**پاسخ موفق (status < 400):**
```json
{
  "success": true,
  "data": { /* همون چیزی که در ادامه‌ی این سند «پاسخ» هر endpoint نامیده شده */ },
  "message": null,
  "timestamp": "2026-09-14T10:30:00.123456Z"
}
```

**پاسخ خطا (status >= 400):**
```json
{
  "success": false,
  "error": {
    "code": "400",
    "message": "<رشته یا دیکشنری، بسته به نوع خطا — پایین توضیح داده شده>"
  },
  "timestamp": "2026-09-14T10:30:00.123456Z"
}
```
`error.code` همیشه **رشته‌ی خودِ status code HTTP** است (مثل `"400"`,
`"403"`, `"404"`) — نه یه کد نمادین مثل `INVALID_INPUT` (که نسخه‌های
قبلی این سند ادعا می‌کردن). منطق پرشدن `error.message`
(در `core/renderers.py` و عیناً تکرارشده در `core/exception_handlers.py`):
- اگه بدنه‌ی خطای اصلی dict باشه و کلید `detail` داشته باشه (حالت پیش‌فرض
  خطاهای built-in خود DRF مثل `PermissionDenied`, `NotAuthenticated`,
  `NotFound`, `Throttled`) → `error.message = آن detail` (معمولاً یک رشته
  انگلیسی پیش‌فرض DRF، نه فارسی — مثلاً `"Authentication credentials were not provided."`).
- وگرنه اگه کلید `error` داشته باشه (الگوی غالب توی این کدبیس:
  `Response({'error': 'پیام فارسی'}, status=400)`) → `error.message = همون رشته‌ی فارسی`.
- وگرنه اگه کلید `non_field_errors` داشته باشه (خطای اعتبارسنجی سریالایزر
  بدون فیلد مشخص) → `error.message = آن لیست`.
- وگرنه (مثلاً خطاهای per-field سریالایزر مثل `{'phone': ['این شماره تلفن قبلاً ثبت شده است.']}`)
  → `error.message = همون دیکشنری کامل`، نه یک رشته.

استثناهای خاص در `custom_exception_handler` (`core/exception_handlers.py`):
`django.db.models.deletion.RestrictedError` / `ProtectedError` (وقتی
`on_delete=RESTRICT/PROTECT` مانع حذف بشه) به‌صورت خاص گرفته می‌شن و
همیشه پیام ثابت «این محصول قبلاً در سفارشی استفاده شده و قابل حذف نیست؛
به‌جای حذف، آن را غیرفعال کنید.» با کد ۴۰۰ برمی‌گردونن — صرف‌نظر از اینکه
واقعاً محصول باشه یا هر مدل دیگه‌ای با `RESTRICT`.

### ⚠️ یافته: پاسخ‌های `204 No Content` هم بدنه‌ی JSON دارن

`CustomJSONRenderer.render()` هیچ حالت خاصی برای `data=None`/status ۲۰۴
نداره؛ چون `status_code >= 400` نیست، وارد شاخه‌ی موفق می‌شه و
`{"success": true, "data": null, "message": null, "timestamp": ...}`
رو رندر می‌کنه. یعنی پاسخ‌های ۲۰۴ این API (مثل `DELETE /cart/{id}/`)
برخلاف انتظار معمول HTTP، بدنه‌ی JSON خالی-از-نظر-منطقی ولی غیرخالی-از-نظر-بایت
دارن.

### احراز هویت

JWT با `rest_framework_simplejwt`، هدر `Authorization: Bearer <access_token>`.
تنظیمات (`settings.py`):
```python
SIMPLE_JWT = {
    'ACCESS_TOKEN_LIFETIME': timedelta(minutes=30),
    'REFRESH_TOKEN_LIFETIME': timedelta(days=1),
    'ROTATE_REFRESH_TOKENS': True,
    'BLACKLIST_AFTER_ROTATION': True,
}
```

**⚠️ یافته مهم:** `BLACKLIST_AFTER_ROTATION=True` هست، ولی
`rest_framework_simplejwt.token_blacklist` توی `INSTALLED_APPS`
(`settings.py`) **نیست**. این اپ همون چیزیه که جدول‌های
`OutstandingToken`/`BlacklistedToken` رو می‌سازه؛ بدون اون، هر بار که
یک refresh token چرخونده می‌شه (`ROTATE_REFRESH_TOKENS=True`)، منطق
blacklist‌کردن توکن قدیمی سعی می‌کنه به مدلی بنویسه که اصلاً migrate
نشده. یعنی `POST /api/token/refresh/` با احتمال بالا با یک خطای واقعی
دیتابیس (نه یک پاسخ تمیز ۴۰۰) شکست می‌خوره. این چیزیه که مستقیماً از
`settings.py` قابل تأیید است؛ تأیید عملی (اجرای واقعی درخواست) خارج از
حوزه‌ی این سند مستندسازی‌محور است.

`DEFAULT_PERMISSION_CLASSES` عمومی: `IsAuthenticated` — یعنی هر
ViewSet/action ای که صریحاً `permission_classes`/`get_permissions`
نداشته باشه، پیش‌فرض نیازمند توکنه.

### Pagination

هیچ `DEFAULT_PAGINATION_CLASS` ای تنظیم نشده. یعنی **همه‌ی** endpointهای
`list` این API آرایه‌ی کامل و بدون صفحه‌بندی برمی‌گردونن — پارامترهای
`page`/`limit` که نسخه‌های قبلی این سند برای اکثر لیست‌ها ادعا می‌کردن،
هیچ‌جای کد پیاده نشدن و اگه بفرستیدشون کاملاً نادیده گرفته می‌شن.

### Throttling

```python
'DEFAULT_THROTTLE_CLASSES': [AnonRateThrottle, UserRateThrottle],
'DEFAULT_THROTTLE_RATES': {
    'anon': '100/day',
    'user': '1000/day',
    'otp_request': '3/hour',        # فقط روی POST /auth/otp/request/، بر اساس IP
    'otp_request_phone': '3/hour',  # فقط روی POST /auth/otp/request/، بر اساس phone
}
```
فقط `POST /auth/otp/request/` throttle اختصاصی (`otp_request` +
`otp_request_phone`، هر دو با هم، هر کدوم جدا اعمال می‌شن) داره. بقیه‌ی
همه‌ی endpointها فقط throttle عمومی `anon`/`user` رو دارن.

### قانون رشته‌شدن مقادیر Decimal (تأییدشده با اجرای واقعی کد)

این نکته مستقیماً روی شکل داده‌ی هر endpoint اثر می‌ذاره، پس یک‌بار
اینجا توضیح داده می‌شه:

- هر فیلد Decimal که از یک **سریالایزر واقعی** (`ModelSerializer` یا
  `serializers.DecimalField` صریح) رد بشه → توی JSON به‌صورت **رشته**
  درمی‌آد (مثلاً `"12.50"`) — چون `COERCE_DECIMAL_TO_STRING` پیش‌فرض DRF
  (`True`) هیچ‌جا override نشده.
- هر فیلد Decimal که مستقیم توی یک دیکشنری دستی گذاشته بشه (بدون رد شدن
  از سریالایزر — مثل `Response({'total': some_decimal})`) → توی JSON به
  **عدد خام (float)** تبدیل می‌شه (مثلاً `12.5`، با از دست رفتن صفرهای
  انتهایی!) — چون `rest_framework.utils.encoders.JSONEncoder.default()`
  مقدار `Decimal` رو با `float(obj)` تبدیل می‌کنه، نه `str(obj)`. این با
  اجرای واقعی کد تأیید شده:
  ```python
  >>> json.dumps({'x': Decimal('12.50')}, cls=JSONEncoder)
  '{"x": 12.5}'
  ```
- استثنا: مقادیری که داخل یک `JSONField` (مثل `Invoice.items_snapshot`)
  ذخیره می‌شن، چون Django موقع `save()` از `DjangoJSONEncoder` استفاده
  می‌کنه (که `Decimal` رو با `str()` تبدیل می‌کنه)، از قبل به‌صورت رشته
  توی دیتابیس ذخیره‌ان — این رشته‌شدن ربطی به DRF نداره، موقع نوشتن رخ
  می‌ده.

در ادامه، جلوی هر فیلد Decimal مشخص شده که «رشته» است یا «عدد خام».

---

## ۲. Auth (پیشوند `/auth/`)

### `POST /auth/otp/request/`

- **Auth:** لازم نیست (`AllowAny`)
- **Throttle:** `otp_request` (۳/ساعت به ازای IP) + `otp_request_phone` (۳/ساعت به ازای phone)

**بدنه‌ی درخواست:**
| فیلد | نوع | الزامی |
|---|---|---|
| `phone` | string, max 11 char | بله |
| `purpose` | یکی از `register`, `login`, `change_phone` | بله |

**پاسخ موفق (۲۰۰):**
```json
{"message": "کد OTP ارسال شد", "code": "12345", "expires_in": 300}
```
⚠️ `code` (خودِ کد OTP تولیدشده) همیشه توی پاسخ برمی‌گرده — این محیط
توسعه/تست است، نه یک placeholder مستندسازی؛ عملاً پیامکی ارسال نمی‌شه.

**رفتار داخلی:** همه‌ی رکوردهای `OTPCode` قبلیِ استفاده‌نشده برای همین
`phone`+`purpose` باطل (`is_used=True`) می‌شن، بعد یک رکورد جدید با
انقضای ۵ دقیقه ساخته می‌شه. هیچ خطای مستندی برنمی‌گردونه (چون همیشه
موفقه، مگر throttle بخوره).

**خطا:** فقط throttle (۴۲۹، پیام پیش‌فرض DRF).

---

### `POST /auth/otp/verify/`

- **Auth:** لازم نیست برای `register`/`login`؛ برای `purpose=change_phone`
  کاربر باید از قبل با JWT لاگین باشه (چک دستی داخل view، نه از طریق
  `permission_classes`).

**بدنه‌ی درخواست:**
| فیلد | نوع | الزامی |
|---|---|---|
| `phone` | string, max 11 | بله |
| `code` | string, max 5 | بله |
| `purpose` | یکی از `register`, `login`, `change_phone` | بله |
| `full_name` | string, max 100 | فقط برای `register` روی کاربر تازه؛ در غیر این صورت نادیده گرفته می‌شه |

**خطاهای واقعی (همه با کلید `error` در بدنه):**
| status | بدنه‌ی `error` | چه‌وقت |
|---|---|---|
| 400 | `"کد معتبری برای این شماره یافت نشد."` | هیچ OTP فعالی برای این phone+purpose نیست |
| 400 | `"OTP_EXPIRED"` | OTP منقضی شده |
| 403 | `"ACCOUNT_LOCKED"` | ≥۵ تلاش غلط روی همین OTP |
| 400 | `"OTP_INVALID"` | کد اشتباه |
| 401 | `"برای تغییر شماره تلفن باید وارد شده باشید."` | `purpose=change_phone` بدون احراز هویت |
| 400 | `"این شماره تلفن قبلاً ثبت شده است."` | `purpose=change_phone`، شماره‌ی جدید متعلق به کاربر دیگه |
| 400 | `"PHONE_NOT_REGISTERED"` | `purpose=login` روی شماره‌ای که کاربرش تازه ساخته شده (یعنی از قبل ثبت‌نام نکرده) |
| 403 | `"ACCOUNT_INACTIVE"` | کاربر `is_active=False` |

**پاسخ موفق — سه شکل متفاوت بسته به مسیر:**

**۱) `purpose=change_phone` موفق:**
```json
{
  "token": "<access jwt>",
  "refresh_token": "<refresh jwt>",
  "user": { "id": 1, "phone": "...", "full_name": "...", "address": null, "role": "buyer", "is_active": true, "created_at": "...", "updated_at": "..." },
  "message": "شماره تلفن با موفقیت تغییر یافت."
}
```

**۲) `purpose=login` روی یک ادمین که `admin_pin` ست کرده:**
```json
{"requires_pin": true, "phone": "09120000001"}
```
⚠️ در این حالت **هیچ token ای برنمی‌گرده** — کلاینت باید مرحله‌ی بعد
(`POST /auth/verify-admin-pin/`) رو صدا بزنه. این چک فقط روی
`purpose=='login'` اعمال می‌شه؛ `register` (چون کاربر تازه‌ست) و
`change_phone` هیچ‌وقت این رفتار رو ندارن.

**۳) بقیه‌ی موارد (register، یا login بدون admin_pin، یا هر buyer/visitor):**
```json
{
  "token": "<access jwt>",
  "refresh_token": "<refresh jwt>",
  "user": { "id": 1, "phone": "...", "full_name": "...", "address": null, "role": "buyer", "is_active": true, "created_at": "...", "updated_at": "..." }
}
```

---

### `POST /auth/verify-admin-pin/`

- **Auth:** لازم نیست (`AllowAny` — این خودش دومین مرحله‌ی احراز هویته)
- مرحله‌ی دوم لاگین برای ادمینی که در `verify_otp` پاسخ `requires_pin: true` گرفته.

**بدنه‌ی درخواست:**
| فیلد | نوع | الزامی |
|---|---|---|
| `phone` | string | بله |
| `pin` | string | بله |

**خطاها:**
| status | `error` |
|---|---|
| 400 | `"phone و pin الزامی هستند."` — یکی از دو فیلد غایب |
| 400 | `"شماره تلفن یا PIN نامعتبر است."` — شماره پیدا نشد، `role` ادمین نیست، `admin_pin` ست نشده، یا pin غلط (پیام یکسان در هر ۴ حالت، عمداً، تا وجود شماره فاش نشه) |
| 403 | `"ACCOUNT_INACTIVE"` |

**پاسخ موفق:** دقیقاً همون شکل شماره‌ی ۳ بالا (`token`/`refresh_token`/`user`).

---

## ۳. Users (پیشوند `/users/`)

`UserViewSet` یک `ModelViewSet` کامل با `queryset=User.objects.all()` است
— یعنی همه‌ی اکشن‌های استاندارد (`list`/`create`/`retrieve`/`update`/
`partial_update`/`destroy`) به‌صورت خودکار فعال‌ان، نه فقط اکشن‌های
دستی.

`get_queryset()`: ادمین همه‌ی کاربرها رو می‌بینه؛ غیر-ادمین فقط خودش رو.
`get_permissions()`: فقط `toggle_active`, `create_visitor`,
`set_admin_pin` نیاز به `role=admin` دارن؛ بقیه فقط `IsAuthenticated`.

سریالایزر: `UserSerializer` — فیلدهای قابل‌نوشتن واقعی فقط `phone`,
`full_name`, `address` هستن (`role`, `is_active` جزو `read_only_fields`ن).

### `GET /users/`
لیست کاربران قابل‌مشاهده (خودش یا همه، طبق نقش). بدون فیلتر/pagination.

### `POST /users/`
⚠️ **یافته: این endpoint عملاً کار نمی‌کنه.** چون `role` روی مدل
`User` (`models.CharField(max_length=20, choices=ROLE_CHOICES)`) نه
`null=True` داره نه مقدار پیش‌فرض، ولی `UserSerializer` اون رو
`read_only` کرده (پس از بدنه‌ی درخواست قابل‌تنظیم نیست). یعنی
`serializer.save()` سعی می‌کنه یک `User` بدون `role` بسازه که با
constraint دیتابیس (`NOT NULL`) تناقض داره → یک `IntegrityError` خام
دیتابیس (نه یک `ValidationError` تمیز DRF) که `custom_exception_handler`
هم نمی‌شناستش (فقط `RestrictedError`/`ProtectedError` رو می‌شناسه) →
پاسخ ۵۰۰ خام. برای ساخت کاربر واقعی از این API استفاده نشه؛ مسیرهای
واقعی ساخت کاربر `POST /auth/otp/verify/` (purpose=register, نقش
همیشه buyer) و `POST /users/create_visitor/` (نقش visitor) هستن.

### `GET /users/{id}/`
جزئیات یک کاربر (فقط خودش، یا هر کسی برای ادمین).

### `PUT /users/{id}/` و `PATCH /users/{id}/`
**بدنه:** `phone`, `full_name`, `address` (هر سه اختیاری در PATCH؛ در
PUT هم عملاً اجباری نیستن چون سریالایزر default ندارن ولی درخواست خالی
رد می‌شه چون این فیلدها `required=True` پیش‌فرض ModelSerializer‌ان — پس
PUT عملاً باید هر سه رو بفرسته).

⚠️ **یافته: همون کلاس باگی که در `VisitorCreateSerializer` و
`verify_otp`(`change_phone`) قبلاً فیکس شد، اینجا حل‌نشده باقی مونده.**
`UserSerializer` هیچ `validate_phone` ای نداره. اگه یک کاربر `phone`ش
رو (از طریق همین PATCH/PUT، نه از طریق OTP) به شماره‌ای که قبلاً یک
کاربر دیگه داره تغییر بده، چون `phone` روی مدل `unique=True`ست، یک
`IntegrityError` خام دیتابیس پرتاب می‌شه → پاسخ ۵۰۰ خام (نه یک ۴۰۰ تمیز).

### `DELETE /users/{id}/`
⚠️ **یافته مهم: این یک حذف واقعی و دائمی رکورد `User` است**، نه غیرفعال‌سازی
(`toggle`). چون `get_permissions()` فقط `toggle_active`/`create_visitor`/
`set_admin_pin` رو admin-only می‌کنه، **هر کاربر لاگین‌شده‌ای
(buyer/visitor/admin) می‌تونه حساب خودش رو با این endpoint پاک کنه**
(چون `get_queryset()` غیر-ادمین رو فقط به خودش محدود می‌کنه، ولی خودش
که در دسترسه). اگه اون کاربر با `on_delete=RESTRICT` جایی رفرنس شده باشه
(مثلاً یک buyer که سفارش داره — `Order.buyer` روی `RESTRICT`ه، یا یک
admin که محصول ساخته)، حذف با ۴۰۰ («این محصول قبلاً در سفارشی استفاده
شده...» — پیام گمراه‌کننده چون فقط برای Product نوشته شده ولی به هر
RestrictedError اعمال می‌شه) رد می‌شه؛ وگرنه رکورد واقعاً پاک می‌شه.

**پاسخ موفق:** `204 No Content` (با بدنه‌ی envelope خالی، طبق یافته‌ی بالا).

### `POST /users/{id}/toggle/`
- **Auth:** admin فقط.
- **بدنه:** ندارد.
- ادمین نمی‌تونه خودش رو غیرفعال کنه: `400` با `error: "شما نمی‌توانید حساب خودتان را غیرفعال کنید."`
- **پاسخ موفق:** `{"is_active": true}` یا `{"is_active": false}` (وضعیت بعد از toggle).

### `POST /users/create_visitor/`
⚠️ توجه به مسیر: `create_visitor` با **زیرخط** (نه خط‌تیره)، برخلاف
بقیه‌ی اکشن‌های جدیدتر این پروژه که از خط‌تیره استفاده می‌کنن.
- **Auth:** admin فقط.
- **بدنه:** `phone` (string, max 11)، `full_name` (string, max 100) — هر دو الزامی.
- **خطا:** `400` با بدنه‌ی per-field (نه `error` ساده): اگه `phone` تکراری
  باشه → `{"phone": ["این شماره تلفن قبلاً ثبت شده است."]}` (پس
  `error.message` توی envelope همین دیکشنری کامل می‌شه، نه یک رشته).
- **پاسخ موفق (۲۰۱):** آبجکت کامل کاربر تازه‌ساخته‌شده (شکل `UserSerializer`، `role: "visitor"`, `is_active: true`).

### `POST /users/set-admin-pin/`
- **Auth:** admin فقط (خودش، برای خودش — نه برای کاربر دیگه).
- **بدنه:** `pin` (string/عدد؛ باید حداقل ۴ رقم و فقط رقم باشه).
- **خطا:** `400` با `error: "PIN باید حداقل ۴ رقم و فقط شامل عدد باشد."`
- **پاسخ موفق:** `{"message": "PIN با موفقیت تنظیم شد."}` — `pin` با
  `django.contrib.auth.hashers.make_password` هش و در `User.admin_pin`
  ذخیره می‌شه (هیچ‌وقت خام برنمی‌گرده).

---

## ۴. Deletion Requests (پیشوند `/deletion-requests/`)

`AccountDeletionRequestViewSet` — `ModelViewSet` کامل، `permission_classes=[IsAuthenticated]`
روی همه‌ی اکشن‌ها به‌جز `review` (admin-only). `get_queryset()`: ادمین
همه رو می‌بینه، بقیه فقط درخواست‌های خودشون.

سریالایزر (`AccountDeletionRequestSerializer`): تنها فیلد قابل‌نوشتن
واقعی `admin_note` است؛ `user` (nested، read_only)، `status`،
`reviewed_by`، `reviewed_at` همه read_only.

### `GET /deletion-requests/`
لیست درخواست‌های حذف حساب (خودش یا همه).

### `POST /deletion-requests/`
- **بدنه:** عملاً هیچ فیلد اجباری‌ای نداره (`admin_note` اختیاره).
- `perform_create` خودش `user=request.user` رو ست می‌کنه.
- **پاسخ موفق (۲۰۱):** آبجکت کامل درخواست (`status: "pending"`).

### `GET /deletion-requests/{id}/`
جزئیات یک درخواست.

### `PUT`/`PATCH /deletion-requests/{id}/`
فقط `admin_note` واقعاً قابل‌تغییره (بقیه read_only).

### `DELETE /deletion-requests/{id}/`
حذف رکورد درخواست (نه حذف کاربر). `204 No Content`.

### `POST /deletion-requests/{id}/review/`
- **Auth:** admin فقط.
- **بدنه:** `action` (`"approve"` یا `"reject"`, الزامی)، `admin_note` (اختیاری، پیش‌فرض `""`).
- **خطاها:**
  - `400` `{"error": "Invalid action. Must be \"approve\" or \"reject\"."}` (این یکی به انگلیسیه، برخلاف بقیه‌ی پیام‌های فارسی این کدبیس)
  - `400` `{"error": "This request has already been reviewed."}` (اگه `status != 'pending'`)
- **پاسخ موفق:** `{"status": "approved"|"rejected", "message": "Request reviewed successfully."}`
- در صورت `approve`، کاربر مربوطه `is_active=False` می‌شه (ولی حذف واقعی رخ نمی‌ده).

---

## ۵. Products (پیشوند `/products/`)

`ProductViewSet` — `ModelViewSet` کامل، `queryset=Product.objects.all()`.

`get_permissions()`: `create`, `update`, `partial_update`, `destroy`,
`price`, `stock`, `toggle`, `upload_image` → admin فقط؛ بقیه (`list`,
`retrieve`) → `AllowAny`.

### `GET /products/`
- **Auth:** لازم نیست.
- **پیش‌فرض:** فقط `is_active=True`.
- **Query params (همه اختیاری، قابل‌ترکیب):**

| پارامتر | رفتار |
|---|---|
| `include_inactive=true` | فقط اگه کاربر لاگین‌شده admin باشه، `is_active=False` رو هم نشون می‌ده. هر مقدار دیگه یا هر کاربر دیگه → نادیده گرفته می‌شه (پیش‌فرض حفظ می‌شه). |
| `search` | `icontains` روی `title` **یا** `description` (OR) |
| `quality` | باید دقیقاً `اولیه` یا `بازیافتی` باشه؛ هر مقدار دیگه نادیده گرفته می‌شه (فیلتر اعمال نمی‌شه، نه خطا) |
| `color` | `icontains` |
| `min_price` | باید float-پذیر باشه؛ وگرنه بی‌صدا نادیده گرفته می‌شه |
| `max_price` | همون‌طور |
| `in_stock` | `true`/`1`/`yes` → `stock>0`؛ `false`/`0`/`no` → `stock=0`؛ هر مقدار دیگه نادیده گرفته می‌شه |

- **پاسخ:** آرایه‌ی محصولات، بدون pagination.

### `POST /products/`
- **Auth:** admin.
- **بدنه:**

| فیلد | نوع | الزامی |
|---|---|---|
| `title` | string, max 200 | بله |
| `price` | decimal | بله |
| `weight` | decimal | بله |
| `color` | string, max 50 | خیر (nullable) |
| `quality` | `اولیه` \| `بازیافتی` | بله |
| `description` | string | خیر |
| `stock` | decimal، `min_value=0` | خیر (پیش‌فرض ۰) |
| `is_active` | bool | خیر (پیش‌فرض `true`) |

⚠️ `image_urls` **در بدنه‌ی درخواست قابل‌ست‌کردن نیست** — روی سریالایزر
`SerializerMethodField` (فقط-خواندنی) است؛ تنها راه اضافه‌کردن تصویر
`POST /products/{id}/upload_image/` است. `created_by` خودکار از
`request.user` پر می‌شه (`perform_create`).

- **پاسخ موفق (۲۰۱):** آبجکت کامل محصول.

### `GET /products/{id}/`
- **Auth:** لازم نیست برای محصول فعال. برای محصول غیرفعال: فقط ادمین
  می‌تونه ببینه (غیر-ادمین `404` می‌گیره چون `get_queryset` برای
  اکشن‌های غیر-`list` هم فیلتر `is_active` رو برای غیر-ادمین اعمال می‌کنه).
- **پاسخ:** `id`, `title`, `price` (رشته), `weight` (رشته), `color`,
  `quality`, `description`, `image_urls` (آرایه‌ی رشته، هر URL نسبی به
  URL کامل تبدیل شده — `request.build_absolute_uri`)، `stock` (رشته),
  `is_active`, `created_by` (id), `created_by_name` (رشته، `str(user)`),
  `created_at`, `updated_at`.

### `PUT`/`PATCH /products/{id}/`
- **Auth:** admin. همون فیلدهای `POST` (بدون `image_urls`).

### `DELETE /products/{id}/`
- **Auth:** admin.
- اگه محصول در `OrderItem`/`PriceHistory`/`StockHistory` استفاده شده
  باشه (`on_delete=RESTRICT` روی هر سه) → `400` با پیام «این محصول
  قبلاً در سفارشی استفاده شده و قابل حذف نیست؛ به‌جای حذف، آن را
  غیرفعال کنید.» (سراسری، از `custom_exception_handler`).
- وگرنه: `204 No Content`.

### `PATCH /products/{id}/price/`
- **Auth:** admin.
- **بدنه:** `{"price": <decimal>}` — فقط همین یک فیلد؛ `price` الزامیه
  وگرنه `400 {"error": "price is required"}` (انگلیسی).
- یک رکورد `PriceHistory` (`old_price`, `new_price`) خودکار ساخته می‌شه.
- **پاسخ موفق:** `{"message": "قیمت با موفقیت تغییر یافت"}` (قیمت جدید
  در پاسخ echo نمی‌شه؛ باید دوباره `GET` بزنید).

### `PATCH /products/{id}/stock/`
- **Auth:** admin.
- **بدنه:** `{"stock": <decimal>, "reason": <string>}` — `stock` الزامی
  (وگرنه `400 {"error": "stock is required"}`)، `reason` اختیاری
  (پیش‌فرض `"adjustment"`؛ **هیچ اعتبارسنجی‌ای روی مقدار `reason` نیست**
  — می‌تونه هر رشته‌ای باشه، نه فقط یکی از چهار مقدار `REASON_CHOICES`
  مدل `StockHistory` (`initial`, `sale`, `restock`, `adjustment`) —
  چون این مقدار مستقیم بدون سریالایزر ذخیره می‌شه).
- **پاسخ موفق:** `{"message": "موجودی با موفقیت تغییر یافت"}`.

### `PATCH /products/{id}/toggle/`
- **Auth:** admin. بدنه ندارد.
- **پاسخ موفق:** `{"is_active": true|false}`.

### `POST /products/{id}/upload_image/`
⚠️ توجه: مسیر با **زیرخط** (`upload_image`)، نه خط‌تیره.
- **Auth:** admin.
- **بدنه:** `multipart/form-data`، فیلد فایل به‌نام `image`.
- **محدودیت‌ها:** حداکثر ۵ تصویر در کل (بررسی `len(product.image_urls) >= 5`
  قبل از آپلود)؛ پسوند مجاز: `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`
  (بدون بررسی حجم فایل! هیچ محدودیت سایزی در کد نیست، برخلاف ادعای
  نسخه‌های قبلی این سند مبنی بر «حداکثر ۵ مگابایت»).
- **خطاها:** `400 {"error": "حداکثر ۵ تصویر مجاز است."}` /
  `400 {"error": "فایل تصویر ارسال نشده است."}` /
  `400 {"error": "فرمت فایل پشتیبانی نمی‌شود. فرمت‌های مجاز: jpg, jpeg, png, gif, webp"}`
- **پاسخ موفق:**
  ```json
  {
    "message": "تصویر با موفقیت بارگذاری شد.",
    "url": "http://<host>/media/product_3_20260914123000.jpg",
    "image_urls": ["http://<host>/media/...jpg", "..."]
  }
  ```
  هر دو (`url` و `image_urls`) از همون منطق `ProductSerializer.get_image_urls`
  رد شدن، پس همیشه URL کامل‌ان.

---

## ۶. Price / Stock Histories (فقط‌خواندنی)

هر دو `ReadOnlyModelViewSet` با `permission_classes=[IsAuthenticated]`
(هر کاربر لاگین‌شده‌ای، نه فقط admin).

⚠️ **یافته:** هیچ‌کدوم `get_queryset` سفارشی ندارن — یعنی **هر کاربر
لاگین‌شده‌ای (حتی buyer عادی) کل تاریخچه‌ی قیمت/موجودی همه‌ی محصولات
سیستم رو می‌بینه**، نه فقط تاریخچه‌ی محصولات مرتبط با خودش (که اصلاً
مفهومی نداره چون خریدار مالک محصول نیست، ولی این یعنی هیچ فیلتر
per-product یا per-admin هم نیست — یک `GET` ساده کل جدول رو برمی‌گردونه).

### `GET /price-histories/`
آرایه‌ی همه‌ی رکوردهای `PriceHistory`: `id`, `product` (id), `old_price`
(رشته یا `null`), `new_price` (رشته), `changed_by` (id),
`changed_by_name`, `changed_at`.

### `GET /price-histories/{id}/`
یک رکورد.

### `GET /stock-histories/`
آرایه‌ی همه‌ی رکوردهای `StockHistory`: `id`, `product`, `old_stock`
(رشته یا `null`), `new_stock` (رشته), `reason`, `changed_by`,
`changed_by_name`, `changed_at`.

### `GET /stock-histories/{id}/`
یک رکورد.

---

## ۷. Cart (پیشوند `/cart/`)

`CartViewSet` یک `GenericViewSet` دستی‌ساز است (نه `ModelViewSet`) —
فقط متدهایی که صریحاً override شدن routing می‌شن. **`retrieve` تعریف
نشده، پس `GET /cart/{id}/` اصلاً وجود نداره** (۴۰۴، نه ۴۰۵).
`permission_classes=[IsAuthenticated]` روی کل ViewSet.

### `GET /cart/`
- **پاسخ:**
  ```json
  {"items": [ /* آرایه‌ی CartItemSerializer */ ], "total": 150.0}
  ```
  ⚠️ `total` (`sum(item.quantity * item.product.price ...)`) مستقیم
  Decimal خام است، از سریالایزر رد نمی‌شه → توی JSON **عدد خام (float)**
  می‌شه، نه رشته.
- هر آیتم داخل `items`: `id`, `user` (id), `product` (id),
  `product_detail` (آبجکت کامل `ProductSerializer`), `quantity` (رشته —
  از `DecimalField` مدل)، `added_at`, `updated_at`, `subtotal`.
  ⚠️ `subtotal` یک `SerializerMethodField` است (`quantity * product.price`)
  → برخلاف `quantity` که رشته‌ست، `subtotal` **عدد خام (float)** است —
  یعنی توی همین آبجکت، دو فیلد از یک خانواده با دو قاعده‌ی متفاوت
  رندر می‌شن.

### `POST /cart/`
- **بدنه:** `product_id` (int, الزامی)، `quantity` (decimal, `min_value=0.01`, الزامی).
- محصول باید `is_active=True` باشه وگرنه `404 {"error": "محصول مورد نظر یافت نشد یا غیرفعال است."}`.
- اگه `stock` کافی نباشه: `400 {"error": "موجودی محصول <title> کافی نیست (موجودی: <stock>)."}`.
- اگه از قبل همین محصول در سبد کاربر باشه، `quantity` **جایگزین** می‌شه (نه جمع).
- **پاسخ موفق (۲۰۱):** یک `CartItemSerializer` (شکل بالا).

### `PUT /cart/{id}/` و `PATCH /cart/{id}/`
⚠️ **یافته:** `partial_update = update` — یعنی این دو متد **کاملاً یکسان**‌ان؛
PATCH رفتار جزئی/اختیاری نداره، دقیقاً همون اعتبارسنجی PUT رو داره
(`product_id` هم الزامیه، هرچند مقدارش عملاً نادیده گرفته می‌شه — پایین
توضیح داده شده).
- **بدنه:** `product_id` (الزامی، ولی **استفاده نمی‌شه** — کد از
  `cart_item.product` موجود استفاده می‌کنه، نه از `product_id` ارسالی؛
  یعنی این endpoint نمی‌تونه محصولِ یک آیتم سبد رو عوض کنه، فقط `quantity`ش رو)،
  `quantity` (decimal، الزامی).
- بررسی موجودی مثل `POST`.
- **پاسخ موفق:** `CartItemSerializer` به‌روزشده.

### `DELETE /cart/{id}/`
حذف یک آیتم سبد. `404 {"error": "آیتمی با این شناسه در سبد خرید شما وجود ندارد."}`
اگه پیدا نشه؛ وگرنه `204 No Content`.

### `DELETE /cart/clear/`
تمام آیتم‌های سبدِ کاربر جاری پاک می‌شن. `204 No Content` (بدون شرط، حتی اگه سبد خالی بوده باشه).

---

## ۸. Orders (پیشوند `/orders/`)

`OrderViewSet` — `ModelViewSet` کامل (نه فقط اکشن‌های دستی!)،
`permission_classes=[IsAuthenticated]`. `get_queryset()`: admin → همه‌ی
سفارش‌ها؛ visitor → فقط `visitor=خودش`؛ buyer (یا هر نقش دیگه) → فقط
`buyer=خودش`.

⚠️ **هیچ فیلتر query param ای پشتیبانی نمی‌شه** (نه `?status=`، نه
`?buyer_id=`) — نسخه‌های قبلی این سند این پارامترها رو ادعا می‌کردن.

### `GET /orders/`
لیست کامل سفارش‌های قابل‌مشاهده (طبق نقش)، بدون فیلتر یا pagination.

### `POST /orders/`
- **بدنه:** ندارد — سفارش از **سبد خرید فعلی کاربر** ساخته می‌شه.
- اگه سبد خالی باشه: `400 {"error": "سبد خرید شما خالی است."}`.
- برای هر آیتم سبد، موجودی چک می‌شه؛ اگه ناکافی: `400 {"error": "موجودی محصول <title> کافی نیست (موجودی: <stock>)."}`.
- در صورت موفقیت: `stock` هر محصول کم می‌شه، `OrderItem`ها ساخته
  می‌شن، یک `OrderStatusHistory` (`pending`) ثبت می‌شه، سبد خالی می‌شه.
- **پاسخ موفق (۲۰۱):**
  ```json
  {"order_id": 12, "status": "pending", "total_price": 4500.0, "message": "سفارش با موفقیت ثبت شد"}
  ```
  ⚠️ `total_price` اینجا مستقیم از یک متغیر Decimal محلیه (نه از
  `OrderSerializer`) → **عدد خام (float)**؛ در تضاد با `GET /orders/{id}/`
  که همین فیلد رو به‌صورت رشته برمی‌گردونه.

### `GET /orders/{id}/`
جزئیات یک سفارش (طبق `get_queryset`، یعنی فقط اگه بهش دسترسی داشته باشید).
**پاسخ (`OrderSerializer`):**
```json
{
  "id": 12, "buyer": 3, "buyer_name": "علی رضایی",
  "buyer_phone": "09121234567", "buyer_address": "تهران..." ,
  "visitor": 7, "visitor_name": "رضا کریمی",
  "total_price": "4500.00", "status": "assigned",
  "items": [
    {"id": 1, "order": 12, "product": 5, "product_detail": { /* ProductSerializer */ }, "quantity": "3.00", "unit_price": "1500.00", "total_price": "4500.00"}
  ],
  "created_at": "...", "updated_at": "..."
}
```
همه‌ی مقادیر Decimal اینجا (`total_price`, `quantity`, `unit_price`)
از سریالایزر رد شدن → **رشته**. `buyer_phone`/`buyer_address` برای هر
کسی که به اصل سفارش دسترسی داره در دسترسه (خریدار خودش، ویزیتور
تخصیص‌یافته، ادمین) — هیچ محدودیت جدایی نداره.

### `PUT`/`PATCH /orders/{id}/`
⚠️ **یافته:** این دو endpoint واقعاً وجود دارن (چون `OrderViewSet`
`ModelViewSet` است) ولی **عملاً بی‌اثرن** — `OrderSerializer` هیچ فیلد
قابل‌نوشتنی نداره (`buyer_name`/`buyer_phone`/`buyer_address`/
`visitor_name`/`items` همه صریحاً `read_only=True` روی خود فیلد؛ بقیه
هم توی `read_only_fields`ن). یک `PUT {}`/`PATCH {}` معتبره و ۲۰۰
برمی‌گردونه بدون اینکه چیزی عوض بشه.

### `DELETE /orders/{id}/`
⚠️ **یافته‌ی مهم و بالقوه خطرناک:** این حذف پیش‌فرض `ModelViewSet` است،
**کاملاً مجزا از اکشن تجاری `cancel`** پایین‌تر. رفتاری که در کد
implement شده «لغو» نیست — **حذف فیزیکی و کامل رکورد `Order`** است، بدون
هیچ بازگردانی `stock`، بدون ثبت `OrderStatusHistory`، و بدون هیچ چک
وضعیتی (روی سفارش با هر status ای کار می‌کنه، نه فقط `pending`). چون
`OrderItem.order`، `Invoice.order`، `OrderStatusHistory.order` و
`OrderAssignment.order` همه `on_delete=CASCADE` هستن، حذف یک سفارش همه‌ی
این رکوردهای وابسته رو هم پاک می‌کنه. هر کسی که طبق `get_queryset` به
یک سفارش دسترسی «مشاهده» داره (خریدار خودش، ویزیتور تخصیص‌یافته، یا
ادمین) می‌تونه اون رو کامل پاک کنه. **پاسخ موفق:** `204 No Content`.

### `DELETE /orders/{id}/cancel/`
مسیر «رسمی» لغو توسط خریدار.
- فقط سفارش‌های `status=pending` قابل‌لغوان؛ وگرنه `400 {"error": "سفارش قابل لغو نیست"}`.
- موجودی محصولات برگردونده می‌شه، `OrderStatusHistory` (`pending→cancelled`) ثبت می‌شه.
- **پاسخ موفق:** `{"message": "سفارش لغو شد"}` (status ۲۰۰، نه ۲۰۴ —
  برخلاف اینکه اسمش «cancel» و method اش `DELETE` است).

### `PUT`/`PATCH /orders/{id}/edit_items/`
⚠️ مسیر با زیرخط.
- فقط سفارش‌های `status=pending`، وگرنه `400 {"error": "فقط سفارش‌های در انتظار تخصیص قابل ویرایش هستند."}`.
- **بدنه:** `{"items": [{"product_id": <int>, "quantity": <decimal یا رشته‌ی decimal>}, ...]}`.
  - `items` خالی/غایب → `400 {"error": "لیست آیتم‌ها ارسال نشده است."}`.
  - `quantity` هر آیتم با `Decimal(str(...))` تبدیل می‌شه؛ اگه تبدیل‌ناپذیر باشه →
    `400 {"error": "مقدار quantity نامعتبر است: <مقدار>"}`.
  - `quantity <= 0` → `400 {"error": "مقدار quantity باید عددی بزرگ‌تر از صفر باشد."}`
    (یعنی حذف یک محصول از سفارش با `quantity: 0` دیگه کار نمی‌کنه؛ باید
    اون آیتم رو کلاً از آرایه‌ی `items` حذف کنید).
  - محصول یافت‌نشده/غیرفعال → `400 {"error": "محصول با شناسه <id> یافت نشد یا غیرفعال است."}`.
  - موجودی ناکافی → `400 {"error": "موجودی محصول <title> کافی نیست (موجودی قابل‌استفاده: <n>)."}`.
- **پاسخ موفق:** آبجکت کامل سفارش به‌روزشده (`OrderSerializer`).

### `POST /orders/{id}/cancel_admin/`
⚠️ مسیر با زیرخط.
- **Auth:** admin (چک دستی داخل بدنه: `403 {"error": "فقط ادمین می‌تواند لغو کند."}` — نه از طریق `permission_classes`).
- فقط سفارش‌های `assigned` یا `loading` قابل‌لغوان (نه `pending`، نه `delivered`، نه `cancelled`)؛
  وگرنه `400 {"error": "سفارش قابل لغو نیست (فقط سفارش‌های تخصیص داده شده یا بارگیری شده قابل لغو هستند)."}`.
- موجودی برگردونده می‌شه، `OrderStatusHistory` ثبت می‌شه، پیامک لغو به خریدار ارسال می‌شه.
- **پاسخ موفق:** `{"message": "سفارش با موفقیت لغو شد"}`.

### `GET /orders/{id}/status_history/`
⚠️ مسیر با زیرخط.
- دسترسی: خریدار سفارش، ویزیتور تخصیص‌یافته، یا ادمین؛ وگرنه
  `403 {"error": "شما دسترسی به تاریخچه وضعیت این سفارش ندارید."}`.
- **پاسخ:** آرایه‌ی `OrderStatusHistorySerializer` (`id`, `order`,
  `old_status`, `new_status`, `changed_by`, `changed_by_name`, `note`, `changed_at`)، به ترتیب زمانی.

### `GET /orders/{id}/invoice/`
همون چک دسترسی بالا. اگه فاکتوری صادر نشده: `404 {"error": "فاکتوری برای این سفارش صادر نشده است."}`.
**پاسخ:** `InvoiceSerializer` — `total_price` رشته؛ `items_snapshot`
یک آرایه از دیکشنری‌ها که مقادیر Decimal داخلش (`quantity`,
`unit_price`, `line_total`) هم **رشته‌ان** (چون موقع ذخیره با
`DjangoJSONEncoder` رشته شدن، نه به خاطر DRF).

### `GET /orders/{id}/invoice_pdf/`
⚠️ مسیر با زیرخط. ⚠️ **این تنها endpoint این API است که envelope
ندارد** — چون `HttpResponse` خام برمی‌گردونه (نه DRF `Response`)، از
`CustomJSONRenderer` اصلاً رد نمی‌شه. خروجی: `application/pdf` خام،
هدر `Content-Disposition: attachment; filename="invoice_<شماره فاکتور>.pdf"`.
داخل PDF: عنوان، شماره فاکتور، **تاریخ صدور به شمسی** (مثل «۱۷ شهریور
۱۴۰۵» — تبدیل با کتابخانه‌ی `jdatetime`، فقط برای نمایش؛ خودِ
`Invoice.issued_at` در دیتابیس و در `GET /orders/{id}/invoice/` همچنان
میلادی است)، اطلاعات خریدار، جدول آیتم‌ها، جمع کل. همون چک دسترسی
بالا (403/404 مشابه).

---

## ۹. Order Assignments (پیشوند `/order-assignments/`)

`OrderAssignmentViewSet` — `ModelViewSet` کامل. `get_queryset()`:
admin → همه؛ visitor → فقط تخصیص‌هایی که `new_visitor=خودش`؛ بقیه →
`OrderAssignment.objects.none()` (لیست خالی، نه خطا). `get_permissions()`:
`create`/`update`/`partial_update`/`destroy` → admin فقط؛ `list`/`retrieve` → هر کاربر لاگین‌شده (طبق queryset فوق).

### `GET /order-assignments/`
لیست تخصیص‌ها (طبق نقش، بالا).

### `POST /order-assignments/`
- **Auth:** admin.
- **بدنه:** `order_id` (الزامی)، `new_visitor_id` (الزامی)، `reason` (اختیاری، پیش‌فرض `""`).
- غایب‌بودن هرکدوم → `400 {"error": "order_id و new_visitor_id الزامی هستند."}`.
- سفارش باید `status=pending` باشه؛ وگرنه (یا اگه اصلاً پیدا نشه):
  `400 {"error": "سفارش یافت نشد یا قابل تخصیص نیست"}`.
- ویزیتور باید وجود داشته باشه، `role=visitor`، `is_active=True`؛ وگرنه
  `400 {"error": "ویزیتور نامعتبر است"}`.
- در صورت موفقیت: `Order.visitor` ست می‌شه، `Order.status='assigned'`،
  یک `OrderStatusHistory` و یک پیامک تخصیص برای خریدار.
- **پاسخ موفق (۲۰۱):** `OrderAssignmentSerializer` — `id`, `order`,
  `order_detail` (آبجکت کامل سفارش، `OrderSerializer`), `old_visitor`,
  `new_visitor`, `new_visitor_name`, `assigned_by`, `assigned_by_name`,
  `reason`, `assigned_at`.

### `GET /order-assignments/{id}/`
جزئیات یک تخصیص (طبق دسترسی بالا).

### `PUT`/`PATCH /order-assignments/{id}/`
⚠️ **یافته:** این دو، پیش‌فرض `ModelViewSet` هستن — یک ویرایش **خام**
روی فیلدهای `order`, `old_visitor`, `new_visitor`, `reason` (بدون هیچ
اعتبارسنجی تجاری‌ای مثل چک `status=pending` یا محدودیت `role=visitor`
که در `create` هست)، و **هیچ اثر جانبی‌ای روی خودِ `Order` نداره**
(یعنی `Order.visitor`/`Order.status` را تغییر نمی‌ده، برخلاف `POST`).
یعنی می‌شه یک رکورد `OrderAssignment` رو مستقل از سفارش واقعی‌اش
دستکاری کرد.

### `DELETE /order-assignments/{id}/`
حذف رکورد تخصیص (باز هم بدون اثر روی `Order.visitor`/`status`). `204 No Content`.

---

## ۱۰. Visitor Order Status (پیشوند `/visitor/orders/`)

`VisitorOrderStatusViewSet` یک `GenericViewSet` با فقط یک اکشن سفارشی
است — هیچ `list`/`retrieve` ای وجود نداره.

### `PATCH /visitor/orders/{id}/status/`
- **Auth:** هر کاربر لاگین‌شده؛ ولی سفارش با `Order.objects.get(id=pk, visitor=request.user)`
  پیدا می‌شه — یعنی عملاً فقط ویزیتور تخصیص‌یافته می‌تونه موفق بشه؛
  بقیه (حتی ادمین) `404 {"error": "سفارش یافت نشد یا به شما تعلق ندارد"}` می‌گیرن.
- **بدنه:** `{"status": "loading"|"delivered"}` — هر مقدار دیگه:
  `400 {"error": "وضعیت نامعتبر است"}`.
- گذارهای مجاز: `assigned → loading`، `loading → delivered`. هر ترکیب
  دیگه (حتی معتبر به‌نظر، مثل تلاش برای `loading` وقتی سفارش از قبل
  `loading`ه): `400 {"error": "تغییر وضعیت مجاز نیست"}`.
- در گذار به `delivered`: یک `Invoice` (با snapshot آیتم‌ها) ساخته
  می‌شه — اگه از قبل فاکتوری برای این سفارش صادر شده باشه (که نباید
  اتفاق بیفته چون فقط یک بار از `loading` به `delivered` می‌ره):
  `400 {"error": "فاکتور قبلاً برای این سفارش صادر شده است."}`.
- پیامک وضعیت به خریدار در هر دو گذار ارسال می‌شه.
- **پاسخ موفق:** `{"message": "وضعیت به loading تغییر یافت"}` (یا `delivered`).

---

## ۱۱. System Settings (پیشوند `/system-settings/`)

`SystemSettingViewSet` — `ModelViewSet` کامل. `get_permissions()`:
`create`/`update`/`partial_update`/`destroy` → admin؛ `list`/`retrieve` → `AllowAny` (بدون نیاز به توکن).

⚠️ کلید primary key مدل `SystemSetting` خودِ فیلد `key` (رشته) است، نه
یک `id` عددی خودکار — یعنی `{key}` در URL جزئیات همون رشته‌ی کلید است
(مثلاً `/system-settings/factory_phone/`), نه یک عدد.

### `GET /system-settings/`
آرایه‌ی همه‌ی تنظیمات: `key`, `value`, `description`, `updated_at`. بدون احراز هویت.

### `POST /system-settings/`
- **Auth:** admin.
- **بدنه:** `key` (string, این هم primary key، الزامی)، `value` (string, الزامی)، `description` (اختیاری).
- **پاسخ موفق (۲۰۱):** رکورد ساخته‌شده.

### `GET /system-settings/{key}/`
بدون احراز هویت.

### `PUT`/`PATCH /system-settings/{key}/`
- **Auth:** admin. بدنه: `value`/`description` (و `key` در PUT، چون primary key عوض نمی‌شه عملاً بی‌فایده‌ست ولی سریالایزر قبولش می‌کنه).

### `DELETE /system-settings/{key}/`
- **Auth:** admin. `204 No Content`.

---

## ۱۲. Notifications (پیشوند `/notifications/`)

`NotificationViewSet` — از `ModelViewSet` ارث می‌بره ولی `create`,
`update`, `partial_update`, `destroy` همه صراحتاً override شدن تا
همیشه `MethodNotAllowed` (۴۰۵) بدن — این چهار متد **کاملاً غیرفعالن**،
نه فقط admin-only. `get_queryset()`: فقط اعلان‌های `user=خودش`.

### `GET /notifications/`
لیست اعلان‌های کاربر جاری: `id`, `user`, `related_type`, `related_id`,
`type` (`sms`|`push`), `message`, `sent_at`, `is_read`.

### `GET /notifications/{id}/`
جزئیات یک اعلان (فقط اگه مال خودتون باشه؛ وگرنه چون `get_queryset`
فیلتر می‌کنه، `get_object` یک `404` استاندارد DRF می‌ده).

### `POST`/`PUT`/`PATCH`/`DELETE /notifications/` یا `/notifications/{id}/`
همیشه `405 Method Not Allowed` (پیام پیش‌فرض انگلیسی DRF، نه فارسی) —
حتی برای ادمین.

### `POST /notifications/{id}/mark_read/`
⚠️ مسیر با زیرخط.
**پاسخ موفق:** `{"status": "marked read"}`.

### `POST /notifications/mark_all_read/`
⚠️ مسیر با زیرخط. تمام اعلان‌های خوانده‌نشده‌ی کاربر جاری `is_read=True` می‌شن.
**پاسخ موفق:** `{"status": "all marked read"}`.

---

## ۱۳. Admin Reports (پیشوند `/admin-reports/`)

`AdminReportsViewSet` یک `GenericViewSet` بدون هیچ `list`/`retrieve`
است — فقط شش اکشن سفارشی، همه `GET`. `get_permissions()`: همه
admin-only **به‌جز** `visitor_performance`.

### `GET /admin-reports/visitor-performance/`
- **Auth:** هر کاربر لاگین‌شده؛ منطق دسترسی داخل خودِ اکشنه:
  - `role=admin` → همه‌ی ویزیتورها.
  - `role=visitor` → فقط رکورد خودش (`pk=request.user.pk`).
  - هر نقش دیگه (buyer و…) → `403 {"error": "شما دسترسی به این گزارش را ندارید."}`.
- بدون query param.
- **پاسخ:** آرایه‌ای از:
  ```json
  {
    "id": 7, "phone": "0912...", "full_name": "رضا کریمی",
    "total_assigned": 12, "delivered": 9, "cancelled": 1,
    "avg_delivery_seconds": 3600.5
  }
  ```
  `total_assigned`/`delivered`/`cancelled` عدد صحیح (`Count`)؛
  `avg_delivery_seconds` عدد اعشاری واقعی (میانگین فاصله‌ی زمانی
  `assigned→delivered` به ثانیه، از `Extract('epoch')`) یا `null` اگه
  هیچ سفارش تحویل‌شده‌ای نداشته باشه — این یکی از قبل float واقعیه
  (نه Decimal)، پس قاعده‌ی رشته/عدد بالا روش صدق نمی‌کنه.

### `GET /admin-reports/order-counts/`
- **Auth:** admin.
- بدون query param.
- **پاسخ:** دیکشنری با کلید تمام مقادیر `Order.STATUS_CHOICES`
  (`pending`, `assigned`, `loading`, `delivered`, `cancelled`) و مقدار
  عدد صحیح (حتی اگه ۰ باشه، صریحاً پر می‌شه):
  ```json
  {"pending": 5, "assigned": 2, "loading": 1, "delivered": 40, "cancelled": 3}
  ```

### `GET /admin-reports/revenue/`
- **Auth:** admin.
- **Query params:** `from`, `to` — **هر دو الزامی**، فرمت دقیق `YYYY-MM-DD`.
  - غایب → `400 {"error": "لطفاً پارامترهای \"from\" و \"to\" را به فرمت YYYY-MM-DD وارد کنید."}`.
  - فرمت غلط → `400 {"error": "فرمت تاریخ نامعتبر است. از YYYY-MM-DD استفاده کنید."}`.
- بازه: از ابتدای روز `from` تا **پایان** روز `to` (شامل خودِ `to`، چون
  کد `to_dt = to + 1 روز` و فیلتر `created_at__lt=to_dt` می‌زنه).
- فقط سفارش‌های `status=delivered` حساب می‌شن.
- **پاسخ:** `{"from": "2026-09-01", "to": "2026-09-14", "total_revenue": 1250000.0}`
  — `total_revenue` (Decimal از `Sum`، یا `0` اگه هیچی نبود) **عدد خام**، نه رشته.

### `GET /admin-reports/top-products/`
- **Auth:** admin.
- **Query param:** `limit` (اختیاری، پیش‌فرض `10`؛ باید عدد باشه وگرنه
  `400 {"error": "limit باید عدد باشد."}`؛ اگه `<=0` بی‌صدا به `10` برمی‌گرده).
- سفارش‌های `pending`, `assigned`, `loading`, `delivered` حساب می‌شن (نه `cancelled`).
- **پاسخ:** آرایه‌ی مرتب‌شده نزولی بر اساس فروش:
  ```json
  [{"product_id": 5, "product_title": "پلی‌اتیلن سنگین", "total_quantity_sold": 340.0}]
  ```
  `total_quantity_sold` (Decimal از `Sum`) **عدد خام**.

### `GET /admin-reports/low-stock/`
- **Auth:** admin.
- **Query param:** `threshold` (اختیاری، پیش‌فرض `10`؛ باید عدد باشه
  وگرنه `400 {"error": "threshold باید عدد باشد."}`؛ اگه منفی، بی‌صدا به `10` برمی‌گرده).
- فقط محصولات `is_active=True` و `stock < threshold`، مرتب صعودی بر اساس `stock`.
- **پاسخ:** `[{"id": 3, "title": "...", "stock": 4.0}, ...]` — `stock` **عدد خام**
  (چون از `.values()` خام میاد، نه از سریالایزر).

### `GET /admin-reports/signups/`
- **Auth:** admin.
- **Query param:** `period` (`day` یا `week`؛ هر مقدار دیگه بی‌صدا به `day` برمی‌گرده).
- فقط کاربران `role=buyer` (نه visitor/admin).
- **پاسخ:** `[{"period": "2026-09-01", "count": 5}, ...]` — `period` تاریخ
  ISO شروع بازه (روز یا هفته)؛ `count` عدد صحیح.

---

## ۱۴. مسیرهای خارج از `/api/v1/`

### `POST /api/token/refresh/`
از `rest_framework_simplejwt.views.TokenRefreshView` — سریالایزر و
منطق پیش‌فرض خودِ آن کتابخونه (این پروژه هیچ override ای رویش نداره).
بدنه‌ی استاندارد: `{"refresh": "<refresh token>"}`. پاسخ موفق استاندارد
simplejwt: `{"access": "...", "refresh": "..."}` (چون `ROTATE_REFRESH_TOKENS=True`،
یک refresh token جدید هم برمی‌گرده) — **این پاسخ هم از همون envelope
عمومی رد می‌شه** (زیر `data`)، چون این view هم `DEFAULT_RENDERER_CLASSES`
سراسری رو به ارث می‌بره. ببینید یافته‌ی بخش «احراز هویت» بالا — احتمال
شکست این endpoint به خاطر نبود `token_blacklist` در `INSTALLED_APPS`.

### `GET /api/schema/` و `GET /api/docs/`
مستندات OpenAPI (drf-spectacular). `/api/schema/` از envelope معاف است
(چک صریح در `CustomJSONRenderer`). `/api/docs/` صفحه‌ی HTML سواگر است.

### `/admin/`
پنل مدیریت جنگو — بخشی از REST API نیست، احراز هویت جدا (session-based، نه JWT).

### `/media/...`
سرو فایل‌های آپلودشده، فقط وقتی `DEBUG=True`.
