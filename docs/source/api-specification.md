# **API Specification نهایی**

## **اپلیکیشن فروش محصولات پلاستیکی**

---

## **1\) قراردادهای عمومی**

### **1-1. پایه‌ی API**

تمام endpointها زیر مسیر زیر ارائه می‌شوند:

/api/v1

### **1-2. احراز هویت**

* احراز هویت با JWT انجام می‌شود.  
* هدر لازم:

Authorization: Bearer \<token\>

* مسیرهای بدون نیاز به احراز هویت:  
  * درخواست OTP  
  * تأیید OTP  
  * مشاهده عمومی محصولات  
  * مشاهده جزئیات عمومی محصول  
  * دریافت تنظیمات عمومی سیستم

### **1-3. فرمت داده**

* همه‌ی درخواست‌ها و پاسخ‌ها JSON هستند، مگر:  
  * آپلود تصویر: `multipart/form-data`  
  * دریافت فاکتور PDF: `application/pdf`

### **1-4. ساختار پاسخ استاندارد**

#### **پاسخ موفق**

{  
  "success": true,  
  "data": {},  
  "message": "عملیات با موفقیت انجام شد",  
  "timestamp": "2025-07-06T10:30:00Z"  
}

#### **پاسخ خطا**

{  
  "success": false,  
  "error": {  
    "code": "INVALID\_INPUT",  
    "message": "شماره تلفن نامعتبر است",  
    "details": {}  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

### **1-5. کدهای خطای استاندارد**

| Code | HTTP Status | توضیح |
| ----- | ----- | ----- |
| `INVALID_INPUT` | 400 | ورودی نامعتبر است |
| `UNAUTHORIZED` | 401 | کاربر وارد نشده یا توکن معتبر نیست |
| `FORBIDDEN` | 403 | کاربر دسترسی لازم را ندارد |
| `NOT_FOUND` | 404 | منبع موردنظر پیدا نشد |
| `CONFLICT` | 409 | تداخل با وضعیت فعلی یا داده تکراری |
| `RATE_LIMIT_EXCEEDED` | 429 | تعداد درخواست بیش از حد مجاز است |
| `OTP_EXPIRED` | 400 | کد OTP منقضی شده است |
| `OTP_INVALID` | 400 | کد OTP اشتباه است |
| `ACCOUNT_LOCKED` | 403 | حساب موقتاً قفل شده است |
| `OUT_OF_STOCK` | 400 | موجودی کافی نیست |
| `ORDER_STATUS_INVALID` | 400 | تغییر وضعیت سفارش مجاز نیست |
| `SERVER_ERROR` | 500 | خطای داخلی سرور |

---

# **2\) API Specification احراز هویت**

---

## **2-1. درخواست OTP**

**Method:** `POST`  
**Path:** `/api/v1/auth/otp/request`  
**Auth:** ندارد  
**Role:** عمومی  
**Use Case:** UC-01, UC-02, UC-05, UC-46

### **هدف**

ارسال OTP برای ثبت‌نام، ورود یا تغییر شماره تلفن.

### **Headers**

Content-Type: application/json

### **Request Body**

{  
  "phone": "09123456789",  
  "purpose": "register"  
}

### **فیلدها**

* `phone` الزامی، رشته 11 رقمی  
* `purpose` الزامی، یکی از:  
  * `register`  
  * `login`  
  * `change_phone`

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "message": "کد OTP به شماره شما ارسال شد",  
    "expires\_in": 300  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

### **خطاها**

* `400 Bad Request` — ورودی نامعتبر  
* `409 Conflict` — در ثبت‌نام، شماره قبلاً ثبت شده است  
* `429 Too Many Requests` — تعداد درخواست بیش از حد مجاز  
* `500 Internal Server Error` — خطا در سرویس پیامک

### **نکات**

* OTP باید زمان‌دار و یک‌بارمصرف باشد.  
* برای شماره‌های تکراری در حالت `register` خطای conflict برگردد.

---

## **2-2. تأیید OTP و ورود / ثبت‌نام**

**Method:** `POST`  
**Path:** `/api/v1/auth/otp/verify`  
**Auth:** ندارد  
**Role:** عمومی  
**Use Case:** UC-01, UC-02, UC-22, UC-41, UC-47

### **هدف**

تأیید OTP و صدور توکن JWT.

### **Headers**

Content-Type: application/json

### **Request Body**

{  
  "phone": "09123456789",  
  "code": "12345",  
  "purpose": "login",  
  "full\_name": "علی قویدل"  
}

### **فیلدها**

* `phone` الزامی  
* `code` الزامی  
* `purpose` الزامی  
* `full_name` فقط در `register` لازم است

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "token": "eyJhbGciOiJIUzI1NiIs...",  
    "user": {  
      "id": 1,  
      "phone": "09123456789",  
      "full\_name": "علی قویدل",  
      "role": "buyer",  
      "is\_active": true  
    }  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

### **خطاها**

* `400 Bad Request` — OTP اشتباه یا منقضی شده  
* `403 Forbidden` — حساب قفل شده  
* `409 Conflict` — شماره برای هدف دیگری در استفاده است

### **نکات**

* در `register`، اگر کاربر وجود نداشته باشد ایجاد می‌شود.  
* در `login`، فقط کاربر موجود وارد می‌شود.  
* در `change_phone` شماره جدید باید قبلاً تأیید شده باشد.

---

## **2-3. تمدید توکن**

**Method:** `POST`  
**Path:** `/api/v1/auth/refresh`  
**Auth:** دارد  
**Role:** همه نقش‌ها  
**Use Case:** پشتیبان احراز هویت

### **هدف**

صدور توکن جدید.

### **Headers**

Authorization: Bearer \<token\>  
Content-Type: application/json

### **Request Body**

بدون body

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "token": "new.jwt.token"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **2-4. خروج از سیستم**

**Method:** `POST`  
**Path:** `/api/v1/auth/logout`  
**Auth:** دارد  
**Role:** همه نقش‌ها  
**Use Case:** پشتیبان احراز هویت

### **هدف**

ابطال نشست جاری.

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "message": "با موفقیت خارج شدید"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

# **3\) API Specification پروفایل**

---

## **3-1. مشاهده پروفایل**

**Method:** `GET`  
**Path:** `/api/v1/profile`  
**Auth:** دارد  
**Role:** همه کاربران واردشده  
**Use Case:** UC-03

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "id": 1,  
    "phone": "09123456789",  
    "full\_name": "علی قویدل",  
    "address": "تهران، خیابان ولیعصر، پلاک ۱۲۳",  
    "role": "buyer",  
    "is\_active": true,  
    "created\_at": "2025-07-01T10:00:00Z"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **3-2. ویرایش پروفایل**

**Method:** `PUT`  
**Path:** `/api/v1/profile`  
**Auth:** دارد  
**Role:** همه کاربران واردشده  
**Use Case:** UC-04

### **Request Body**

{  
  "full\_name": "علی محمدی",  
  "address": "تهران، خیابان انقلاب، پلاک ۴۵"  
}

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "message": "پروفایل با موفقیت به‌روزرسانی شد",  
    "user": {  
      "id": 1,  
      "phone": "09123456789",  
      "full\_name": "علی محمدی",  
      "address": "تهران، خیابان انقلاب، پلاک ۴۵",  
      "role": "buyer"  
    }  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **3-3. تغییر شماره تلفن**

**Method:** `POST`  
**Path:** `/api/v1/profile/phone`  
**Auth:** دارد  
**Role:** همه کاربران واردشده  
**Use Case:** UC-05

### **Request Body**

{  
  "new\_phone": "09123456788",  
  "otp\_code": "67890"  
}

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "message": "شماره تلفن با موفقیت تغییر یافت"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

### **خطاها**

* `400 Bad Request`  
* `409 Conflict`  
* `OTP_INVALID`  
* `OTP_EXPIRED`

---

## **3-4. ثبت درخواست حذف حساب**

**Method:** `POST`  
**Path:** `/api/v1/profile/deletion-request`  
**Auth:** دارد  
**Role:** همه کاربران واردشده  
**Use Case:** UC-21

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "message": "درخواست حذف حساب با موفقیت ثبت شد"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

### **خطاها**

* `409 Conflict` — درخواست فعال قبلاً ثبت شده است

---

## **3-5. مشاهده وضعیت درخواست حذف حساب**

**Method:** `GET`  
**Path:** `/api/v1/profile/deletion-request`  
**Auth:** دارد  
**Role:** همه کاربران واردشده  
**Use Case:** UC-21, UC-40

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "status": "pending",  
    "requested\_at": "2025-07-06T10:00:00Z",  
    "reviewed\_at": null,  
    "admin\_note": null  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

# **4\) API Specification آپلود تصویر**

---

## **4-1. آپلود تصویر**

**Method:** `POST`  
**Path:** `/api/v1/uploads/images`  
**Auth:** دارد  
**Role:** همه کاربران واردشده، به‌ویژه admin  
**Use Case:** UC-24

### **هدف**

آپلود فایل تصویر و دریافت URL.

### **Request**

`multipart/form-data`

### **محدودیت‌ها**

* فرمت‌های مجاز: `jpg`, `jpeg`, `png`, `webp`  
* حداکثر حجم هر فایل: `5 MB`  
* حداکثر تعداد فایل در هر درخواست: `5`

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "url": "https://cdn.example.com/images/1.jpg"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

### **خطاها**

* `400 Bad Request`  
* `413 Payload Too Large`  
* `415 Unsupported Media Type`

---

# **5\) API Specification محصولات**

---

## **5-1. لیست محصولات**

**Method:** `GET`  
**Path:** `/api/v1/products`  
**Auth:** ندارد  
**Role:** عمومی  
**Use Case:** UC-06, UC-07, UC-08

### **Query Params**

* `search`  
* `quality`  
* `color`  
* `min_price`  
* `max_price`  
* `in_stock`  
* `page`  
* `limit`

### **توضیح `in_stock`**

* `true` → فقط محصولات با `stock > 0`  
* `false` → فقط محصولات با `stock = 0`  
* خالی → همه محصولات

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "items": \[  
      {  
        "id": 1,  
        "title": "پلی‌اتیلن سنگین",  
        "price": 25000,  
        "weight": 25,  
        "color": "شیری",  
        "quality": "اولیه",  
        "stock": 1000,  
        "image\_urls": \["https://cdn.example.com/1.jpg"\],  
        "is\_active": true  
      }  
    \],  
    "pagination": {  
      "total": 45,  
      "page": 1,  
      "limit": 20,  
      "total\_pages": 3  
    }  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **5-2. مشاهده جزئیات محصول**

**Method:** `GET`  
**Path:** `/api/v1/products/{id}`  
**Auth:** ندارد  
**Role:** عمومی  
**Use Case:** UC-09, UC-10

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "id": 1,  
    "title": "پلی‌اتیلن سنگین",  
    "price": 25000,  
    "weight": 25,  
    "color": "شیری",  
    "quality": "اولیه",  
    "description": "مناسب برای تولید کیسه‌های ضخیم",  
    "stock": 1000,  
    "image\_urls": \[  
      "https://cdn.example.com/1.jpg",  
      "https://cdn.example.com/2.jpg"  
    \],  
    "factory\_phone": "021-12345678",  
    "is\_active": true  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **5-3. ثبت محصول جدید**

**Method**: \`POST\`  
**Path**: \`/api/v1/products\`  
**Auth**: دارد  
**Role**: \`admin\`  
**Use Case**: \`UC-24\`

**هدف**

ثبت یک محصول جدید توسط ادمین.

**Request Body**

\`\`\`json  
{  
  "**title**": "پلی‌اتیلن سنگین",  
  "**price**": 25000,  
  "**weight**": 25,  
  "**color**": "شیری",  
  "**quality**": "اولیه",  
  "**description**": "مناسب برای تولید کیسه‌های ضخیم",  
  "**stock**": 1000,  
  "**image\_urls**": \["https://cdn.example.com/1.jpg"\]  
}  
\`\`\`

### **قوانین**

\* \`image\_urls\` حداکثر ۵ URL دارد.  
\* هر URL باید معتبر باشد.  
\* اگر \`image\_urls\` ارسال نشود، مقدار پیش‌فرض \`\[\]\` ذخیره می‌شود.

### **پاسخ موفق**

\*\*Status:\*\* \`201 Created\`

\`\`\`json  
{  
  "success": true,  
  "data": {  
    "id": 1,  
    "message": "محصول با موفقیت ثبت شد"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}  
\`\`\`

---

## **5-4. ویرایش محصول**

**Method:** `PUT`  
**Path:** `/api/v1/products/{id}`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-25

### **توضیح**

ویرایش اطلاعات اصلی محصول.  
فیلدهای قابل‌ویرایش:

* `title`  
* `price`  
* `weight`  
* `color`  
* `quality`  
* `description`  
* `stock`  
* `image_urls`  
* `is_active`

---

## **5-5. تغییر قیمت محصول**

**Method:** `PATCH`  
**Path:** `/api/v1/products/{id}/price`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-26

### **Request Body**

{ "price": 27000 }

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "message": "قیمت با موفقیت تغییر یافت"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **5-6. تغییر موجودی محصول**

**Method:** `PATCH`  
**Path:** `/api/v1/products/{id}/stock`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-27

### **Request Body**

{  
  "stock": 1500,  
  "reason": "restock"  
}

### **reason**

* `sale`  
* `restock`  
* `adjustment`

---

## **5-7. فعال/غیرفعال کردن محصول**

**Method:** `PATCH`  
**Path:** `/api/v1/products/{id}/toggle`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-28

---

## **5-8. حذف محصول**

**Method:** `DELETE`  
**Path:** `/api/v1/products/{id}`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-29

### **پاسخ موفق**

**Status:** `204 No Content`

---

## **5-9. تاریخچه قیمت محصول**

**Method:** `GET`  
**Path:** `/api/v1/admin/products/{id}/price-history`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-38

---

## **5-10. تاریخچه موجودی محصول**

**Method:** `GET`  
**Path:** `/api/v1/admin/products/{id}/stock-history`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-38

---

## **5-11. تاریخچه کامل محصول**

**Method:** `GET`  
**Path:** `/api/v1/admin/products/{id}/history`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-38

---

# **6\) API Specification سبد خرید**

---

## **6-1. مشاهده سبد خرید**

**Method:** `GET`  
**Path:** `/api/v1/cart`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-11, UC-12, UC-13, UC-14

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "items": \[  
      {  
        "id": 1,  
        "product\_id": 1,  
        "product\_title": "پلی‌اتیلن سنگین",  
        "product\_price": 25000,  
        "quantity": 100,  
        "subtotal": 2500000,  
        "added\_at": "2025-07-06T10:00:00Z"  
      }  
    \],  
    "total": 2500000  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **6-2. افزودن آیتم به سبد**

**Method:** `POST`  
**Path:** `/api/v1/cart/items`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-11

### **Request Body**

{  
  "product\_id": 1,  
  "quantity": 100  
}

### **خطاها**

* `404 Not Found`  
* `OUT_OF_STOCK`

---

## **6-3. تغییر مقدار آیتم سبد**

**Method:** `PUT`  
**Path:** `/api/v1/cart/items/{id}`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-12

### **Request Body**

{  
  "quantity": 150  
}

---

## **6-4. حذف آیتم از سبد**

**Method:** `DELETE`  
**Path:** `/api/v1/cart/items/{id}`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-13

### **پاسخ موفق**

**Status:** `204 No Content`

---

## **6-5. خالی کردن سبد**

**Method:** `DELETE`  
**Path:** `/api/v1/cart`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-14

### **پاسخ موفق**

**Status:** `204 No Content`

---

# **7\) API Specification سفارش‌ها**

---

## **7-1. ثبت سفارش جدید**

**Method:** `POST`  
**Path:** `/api/v1/orders`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-15

### **هدف**

ایجاد سفارش از روی سبد خرید.

### **Request Body**

ندارد

### **پاسخ موفق**

**Status:** `201 Created`

{  
  "success": true,  
  "data": {  
    "order\_id": 1,  
    "status": "pending",  
    "total\_price": 2500000,  
    "message": "سفارش با موفقیت ثبت شد"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

### **خطاها**

* `400 Bad Request` — سبد خرید خالی است  
* `OUT_OF_STOCK` — موجودی کافی نیست

---

## **7-2. مشاهده سفارش‌های من**

**Method:** `GET`  
**Path:** `/api/v1/orders`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-18

### **Query Params**

* `status`  
* `page`  
* `limit`

---

## **7-3. مشاهده جزئیات سفارش**

**Method:** `GET`  
**Path:** `/api/v1/orders/{id}`  
**Auth:** دارد  
**Use Case:** UC-19, UC-31, UC-43

### **Access**

* خریدار صاحب سفارش  
* ادمین  
* ویزیتور تخصیص‌یافته

---

## **7-4. ویرایش سفارش قبل از تخصیص**

**Method:** `PUT`  
**Path:** `/api/v1/orders/{id}`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-16

### **Request Body**

{  
  "items": \[  
    { "product\_id": 1, "quantity": 200 }  
  \]  
}

### **خطاها**

* `ORDER_STATUS_INVALID`  
* `OUT_OF_STOCK`

---

## **7-5. لغو سفارش قبل از تخصیص**

**Method:** `DELETE`  
**Path:** `/api/v1/orders/{id}`  
**Auth:** دارد  
**Role:** buyer  
**Use Case:** UC-17

---

## **7-6. مشاهده همه سفارش‌ها**

**Method:** `GET`  
**Path:** `/api/v1/admin/orders`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-30

### **Query Params**

* `buyer_id`  
* `visitor_id`  
* `status`  
* `page`  
* `limit`

---

## **7-7. لغو سفارش توسط ادمین**

**Method:** `POST`  
**Path:** `/api/v1/admin/orders/{id}/cancel`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-33

### **Request Body**

{  
  "reason": "خطا در ثبت سفارش"  
}

---

# **8\) API Specification تخصیص و وضعیت سفارش**

---

## **8-1. تخصیص سفارش به ویزیتور**

**Method:** `POST`  
**Path:** `/api/v1/admin/orders/{id}/assign`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-32

### **Request Body**

{  
  "visitor\_id": 5,  
  "reason": "ویزیتور اصلی در دسترس نبود"  
}

### **خطاها**

* `NOT_FOUND`  
* `FORBIDDEN`  
* `ORDER_STATUS_INVALID`

---

## **8-2. مشاهده سفارش‌های تخصیص‌یافته به ویزیتور**

**Method:** `GET`  
**Path:** `/api/v1/visitor/orders`  
**Auth:** دارد  
**Role:** visitor  
**Use Case:** UC-42

### **Query Params**

* `status`  
* `page`  
* `limit`

---

## **8-3. تغییر وضعیت سفارش توسط ویزیتور**

**Method:** `PUT`  
**Path:** `/api/v1/visitor/orders/{id}/status`  
**Auth:** دارد  
**Role:** visitor  
**Use Case:** UC-44, UC-45

### **Request Body**

{  
  "status": "loading"  
}

### **مقادیر مجاز**

* `loading`  
* `delivered`

### **خطاها**

* `ORDER_STATUS_INVALID`  
* `FORBIDDEN`

---

## **8-4. تاریخچه وضعیت سفارش**

**Method:** `GET`  
**Path:** `/api/v1/admin/orders/{id}/status-history`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-39

---

# **9\) API Specification مدیریت کاربران و ویزیتورها**

---

## **9-1. مدیریت کاربران**

**Method:** `GET`  
**Path:** `/api/v1/admin/users`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-34

### **Query Params**

* `search`  
* `role`  
* `is_active`  
* `page`  
* `limit`

---

## **9-2. فعال/غیرفعال کردن کاربر**

**Method:** `PATCH`  
**Path:** `/api/v1/admin/users/{id}/toggle`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-34

---

## **9-3. ایجاد ویزیتور جدید**

**Method:** `POST`  
**Path:** `/api/v1/admin/visitors`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-35

### **Request Body**

{  
  "phone": "09123456788",  
  "full\_name": "رضا کریمی"  
}

---

## **9-4. فعال/غیرفعال کردن ویزیتور**

**Method:** `PATCH`  
**Path:** `/api/v1/admin/visitors/{id}/toggle`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-35

---

## **9-5. مشاهده عملکرد ویزیتور**

**Method:** `GET`  
**Path:** `/api/v1/admin/visitors/{id}/performance`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-36

---

# **10\) API Specification درخواست حذف حساب**

---

## **10-1. لیست درخواست‌های حذف حساب**

**Method:** `GET`  
**Path:** `/api/v1/admin/deletion-requests`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-40

### **Query Params**

* `status`  
* `page`  
* `limit`

---

## **10-2. بررسی درخواست حذف حساب**

**Method:** `PATCH`  
**Path:** `/api/v1/admin/deletion-requests/{id}`  
**Auth:** دارد  
**Role:** admin  
**Use Case:** UC-40

### **Request Body**

{  
  "status": "approved",  
  "admin\_note": "درخواست تأیید شد"  
}

### **مقادیر مجاز**

* `approved`  
* `rejected`

---

# **11\) API Specification گزارش‌ها**

---

## **11-1. مشاهده گزارش‌ها**

**`Method`**`` : `GET` ``

**`Path`**`` : `/api/v1/admin/reports` ``

**`Auth`**`: دارد`

**`Role`**`` : `admin` ``

**`Use Case`**`` : `UC-37` ``

**`هدف`**

`نمایش گزارش‌های مدیریتی سیستم.`

## **Query Params**

* `type`  
* `start_date`  
* `end_date`

**`` مقادیر مجاز `type` ``**

 `sales`

 `users`

 `products`

 `visitors`

 `inventory`

 `price_changes`

**`پاسخ موفق`**

**`Status`**`` : `200 OK` ``

```` ```json ````

`{`

  `"success": true,`

  `"data": {`

    `"sales": {`

      `"total_revenue": 500000000,`

      `"total_orders": 120`

    `},`

    `"users": {`

      `"new_users": 15,`

      `"active_users": 200`

    `},`

    `"inventory": {`

      `"total_products": 45,`

      `"out_of_stock": 3`

    `}`

  `},`

  `"timestamp": "2025-07-06T10:30:00Z"`

`}`

```` ``` ````

`---`

---

## **11-2. داشبورد مدیریتی**

**`Method`**`` : `GET` ``

**`Path`**`` : `/api/v1/admin/dashboard` ``

**`Auth`**`: دارد`

**`Role`**`` : `admin` ``

**`Use Case`**`` : `UC-23` ``

**`هدف`**

`نمایش خلاصه وضعیت سیستم برای مدیر.`

**`پاسخ موفق`**

`` Status: `200 OK` ``

```` ```json ````

`{`

  `"success": true,`

  `"data": {`

    `"summary": {`

      `"total_users": 120,`

      `"active_users": 98,`

      `"total_products": 45,`

      `"out_of_stock_products": 3,`

      `"pending_orders": 12,`

      `"assigned_orders": 8,`

      `"delivered_orders": 260,`

      `"today_revenue": 12500000`

    `}`

  `},`

  `"timestamp": "2025-07-06T10:30:00Z"`

`}`

```` ``` ````

## **نکات**

``این endpoint مخصوص `UC-23` است.``

`` `UC-37` برای گزارش‌های جزئی باقی می‌ماند. ``

`داشبورد می‌تواند از همان داده‌های تجمیعی گزارش‌ها تغذیه شود، اما مسیر جدا دارد.`

 

---

# **12\) API Specification اعلان‌ها**

---

## **12-1. مشاهده اعلان‌های من**

**Method:** `GET`  
**Path:** `/api/v1/notifications`  
**Auth:** دارد  
**Use Case:** UC-20, UC-51

---

## **12-2. علامت‌گذاری اعلان به‌عنوان خوانده‌شده**

**Method:** `PATCH`  
**Path:** `/api/v1/notifications/{id}/read`  
**Auth:** دارد  
**Use Case:** UC-20, UC-51

---

## **12-3. علامت‌گذاری همه اعلان‌ها به‌عنوان خوانده‌شده**

**Method:** `PATCH`  
**Path:** `/api/v1/notifications/read-all`  
**Auth:** دارد

### **پاسخ موفق**

**Status:** `200 OK`

{  
  "success": true,  
  "data": {  
    "message": "همه اعلان‌ها خوانده‌شده علامت‌گذاری شدند"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

# **13\) API Specification تنظیمات سیستم**

---

## **13-1. دریافت تنظیمات سیستم**

**Method:** `GET`  
**Path:** `/api/v1/system/settings`  
**Auth:** ندارد  
**Use Case:** UC-10

### **پاسخ موفق**

{  
  "success": true,  
  "data": {  
    "factory\_phone": "021-12345678"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **13-2. ویرایش تنظیمات سیستم**

**Method:** `PATCH`  
**Path:** `/api/v1/admin/system-settings/{key}`  
**Auth:** دارد  
**Role:** admin

### **Request Body**

{  
  "value": "021-87654321"  
}

---

# **14\) API Specification فاکتور PDF (فاز بعد)**

## **14-1. دریافت فاکتور PDF**

**Method:** `GET`  
**Path:** `/api/v1/orders/{id}/invoice`  
**Auth:** دارد  
**Access:** خریدار صاحب سفارش یا ادمین  
**Use Case:** UC-53

### **Response**

* `Content-Type: application/pdf`

### **خطاها**

* `NOT_FOUND`  
* `FORBIDDEN`  
* `ORDER_STATUS_INVALID`

---

# **15\) نکات اجرایی و محدودیت‌ها**

## **محصولات**

* `in_stock=true` فقط محصولات با `stock > 0` را برمی‌گرداند.  
* `in_stock=false` فقط محصولات با `stock = 0` را برمی‌گرداند.  
* `image_urls` حداکثر ۵ لینک دارد.

## **آپلود تصویر**

* فرمت مجاز: `jpg`, `jpeg`, `png`, `webp`  
* حداکثر حجم: `5MB`  
* حداکثر تعداد فایل در هر درخواست: `5`

## **سفارش**

* `order_items.unit_price` قیمت فریز شده است.  
* `CartItem` بعد از ثبت سفارش باید در لایه برنامه حذف شود.  
* مسیرهای مجاز تغییر وضعیت سفارش باید کنترل شوند.

## **اعلان‌ها**

* `read-all` برای علامت‌گذاری همه پیام‌ها

## **OpenAPI**

* پس از نهایی شدن مستند، می‌توان همین APIها را به OpenAPI 3.0 تبدیل کرد تا Swagger UI و تست خودکار هم فعال شود.

---

# **16\) جمع‌بندی**

این نسخه:

* جدول جامع خطاها را اضافه کرده است  
* `in_stock=false` را دقیق و شفاف تعریف کرده است  
* محدودیت `image_urls` را داخل endpoint ثبت محصول آورده است  
* endpointهای اصلی و فاز بعد را کامل‌تر و یکپارچه‌تر کرده است  
* برای تبدیل به OpenAPI 3.0 در مرحله بعد آماده است

