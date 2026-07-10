# **لیست نهایی Endpoint ها**

## **اپلیکیشن فروش محصولات پلاستیکی**

---

## **۱) روش طراحی و قرارداد پاسخ**

### **اصول کلی**

* مسیرها بر پایه‌ی **Resource** طراحی شده‌اند، نه بر پایه‌ی فعل.  
* از اسامی جمع استفاده شده است: `users`, `products`, `orders`.  
* نسخه‌گذاری با `/api/v1` انجام شده است.  
* همه‌ی داده‌ها با `application/json` تبادل می‌شوند، مگر Endpoint فاکتور PDF.  
* احراز هویت با JWT انجام می‌شود.  
* همه‌ی مسیرها به‌جز ثبت‌نام، ورود و مشاهده عمومی محصولات نیازمند توکن هستند.

### **ساختار پاسخ استاندارد**

#### **پاسخ موفق**

{  
  "success": true,  
  "data": { ... },  
  "message": "عملیات با موفقیت انجام شد",  
  "timestamp": "2025-07-06T10:30:00Z"  
}

#### **پاسخ خطا**

{  
  "success": false,  
  "error": {  
    "code": "INVALID\_INPUT",  
    "message": "شماره تلفن نامعتبر است",  
    "details": { "phone": "شماره باید ۱۱ رقم باشد" }  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

## **۲) کدهای خطای استاندارد**

| Code | توضیح |
| ----- | ----- |
| `INVALID_INPUT` | ورودی نامعتبر است |
| `UNAUTHORIZED` | کاربر وارد نشده یا توکن نامعتبر است |
| `FORBIDDEN` | کاربر دسترسی لازم را ندارد |
| `NOT_FOUND` | منبع موردنظر پیدا نشد |
| `CONFLICT` | تداخل با وضعیت فعلی یا داده تکراری |
| `RATE_LIMIT_EXCEEDED` | تعداد درخواست بیش از حد مجاز است |
| `OTP_EXPIRED` | کد OTP منقضی شده است |
| `OTP_INVALID` | کد OTP اشتباه است |
| `ACCOUNT_LOCKED` | حساب موقتاً قفل شده است |
| `OUT_OF_STOCK` | موجودی کافی نیست |
| `ORDER_STATUS_INVALID` | تغییر وضعیت سفارش مجاز نیست |
| `SERVER_ERROR` | خطای داخلی سرور |

---

# **۳) Endpoint ها**

---

## **A) احراز هویت و نشست‌ها**

### **1\) درخواست OTP**

* **Method:** `POST`  
* **Path:** `/api/v1/auth/otp/request`  
* **Auth:** ندارد  
* **Use Case:** UC-01, UC-02, UC-05, UC-46

#### **Request Body**

{  
  "phone": "09123456789",  
  "purpose": "register"  
}

#### **Purpose**

* `register`  
* `login`  
* `change_phone`

#### **پاسخ موفق**

{  
  "success": true,  
  "data": {  
    "message": "کد OTP به شماره شما ارسال شد",  
    "expires\_in": 300  
  }  
}

#### **خطاهای مهم**

* `400 Bad Request`  
* `409 Conflict`  
* `429 Too Many Requests`  
* `500 Internal Server Error`

---

### **2\) تأیید OTP و ورود / ثبت‌نام**

* **Method:** `POST`  
* **Path:** `/api/v1/auth/otp/verify`  
* **Auth:** ندارد  
* **Use Case:** UC-01, UC-02, UC-22, UC-41, UC-47

#### **Request Body**

{  
  "phone": "09123456789",  
  "code": "12345",  
  "purpose": "login",  
  "full\_name": "علی قویدل"  
}

#### **پاسخ موفق**

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
  }  
}

#### **خطاهای مهم**

* `400 Bad Request`  
* `403 Forbidden`  
* `409 Conflict`

---

### **3\) تمدید توکن**

* **Method:** `POST`  
* **Path:** `/api/v1/auth/refresh`  
* **Auth:** دارد

#### **پاسخ موفق**

{  
  "success": true,  
  "data": {  
    "token": "new.jwt.token"  
  }  
}

---

### **4\) خروج از سیستم**

* **Method:** `POST`  
* **Path:** `/api/v1/auth/logout`  
* **Auth:** دارد

#### **پاسخ موفق**

{  
  "success": true,  
  "data": {  
    "message": "با موفقیت خارج شدید"  
  }  
}

---

## **B) پروفایل کاربر**

### **5\) مشاهده پروفایل**

* **Method:** `GET`  
* **Path:** `/api/v1/profile`  
* **Auth:** دارد  
* **Use Case:** UC-03

---

### **6\) ویرایش پروفایل**

* **Method:** `PUT`  
* **Path:** `/api/v1/profile`  
* **Auth:** دارد  
* **Use Case:** UC-04

#### **Request Body**

{  
  "full\_name": "علی محمدی",  
  "address": "تهران، خیابان انقلاب، پلاک ۴۵"  
}

---

### **7\) تغییر شماره تلفن**

* **Method:** `POST`  
* **Path:** `/api/v1/profile/phone`  
* **Auth:** دارد  
* **Use Case:** UC-05

#### **Request Body**

{  
  "new\_phone": "09123456788",  
  "otp\_code": "67890"  
}

---

### **8\) ثبت درخواست حذف حساب کاربری**

* **Method:** `POST`  
* **Path:** `/api/v1/profile/deletion-request`  
* **Auth:** دارد  
* **Use Case:** UC-21

---

### **9\) مشاهده وضعیت درخواست حذف حساب**

* **Method:** `GET`  
* **Path:** `/api/v1/profile/deletion-request`  
* **Auth:** دارد  
* **Use Case:** UC-21, UC-40

---

## **C) آپلود فایل / تصویر**

### **10\) آپلود تصویر**

* **Method:** `POST`  
* **Path:** `/api/v1/uploads/images`  
* **Auth:** دارد  
* **Use Case:** UC-24

#### **Request**

`multipart/form-data`

#### **محدودیت‌ها**

* فرمت‌های مجاز: `jpg`, `jpeg`, `png`, `webp`  
* حداکثر حجم هر تصویر: 5 MB  
* حداکثر تعداد تصویر در هر درخواست: 5 فایل

---

## **D) محصولات**

### **11\) ثبت محصول جدید**

**Method:** `POST`  
**Path:** `/api/v1/products`  
**Auth:** دارد  
**Role:** `admin`  
**Use Case:** `UC-24`

### **هدف**

ثبت یک محصول جدید توسط ادمین.

### **Request Body**

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

### **قوانین**

* `image_urls` حداکثر ۵ URL دارد.  
* هر URL باید معتبر باشد.  
* اگر `image_urls` ارسال نشود، مقدار پیش‌فرض `[]` ذخیره می‌شود.

### **پاسخ موفق**

**Status:** `201 Created`

{  
  "success": true,  
  "data": {  
    "id": 1,  
    "message": "محصول با موفقیت ثبت شد"  
  },  
  "timestamp": "2025-07-06T10:30:00Z"  
}

---

### **12\) لیست محصولات**

* **Method:** `GET`  
* **Path:** `/api/v1/products`  
* **Auth:** ندارد  
* **Use Case:** UC-06, UC-07, UC-08

#### **Query Params**

* `search`  
* `quality`  
* `color`  
* `min_price`  
* `max_price`  
* `in_stock`  
* `page`  
* `limit`

#### **توضیح `in_stock`**

* `true` → فقط محصولات موجود  
* `false` → فقط محصولات ناموجود  
* خالی → همه محصولات

---

### **13\) مشاهده جزئیات محصول**

* **Method:** `GET`  
* **Path:** `/api/v1/products/{id}`  
* **Auth:** ندارد  
* **Use Case:** UC-09, UC-10

---

### **14\) ثبت محصول جدید**

* **Method:** `POST`  
* **Path:** `/api/v1/products`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-24

#### **Request Body**

{  
  "title": "پلی‌اتیلن سنگین",  
  "price": 25000,  
  "weight": 25,  
  "color": "شیری",  
  "quality": "اولیه",  
  "description": "مناسب برای تولید کیسه‌های ضخیم",  
  "stock": 1000,  
  "image\_urls": \["https://cdn.example.com/1.jpg"\]  
}

---

### **15\) ویرایش محصول**

* **Method:** `PUT`  
* **Path:** `/api/v1/products/{id}`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-25

---

### **16\) تغییر قیمت محصول**

* **Method:** `PATCH`  
* **Path:** `/api/v1/products/{id}/price`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-26

#### **Request Body**

{ "price": 27000 }

---

### **17\) تغییر موجودی محصول**

* **Method:** `PATCH`  
* **Path:** `/api/v1/products/{id}/stock`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-27

#### **Request Body**

{  
  "stock": 1500,  
  "reason": "restock"  
}

---

### **18\) فعال/غیرفعال کردن محصول**

* **Method:** `PATCH`  
* **Path:** `/api/v1/products/{id}/toggle`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-28

---

### **19\) حذف محصول**

* **Method:** `DELETE`  
* **Path:** `/api/v1/products/{id}`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-29

#### **پاسخ موفق**

`204 No Content`

---

### **20\) تاریخچه قیمت محصول**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/products/{id}/price-history`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-38

---

### **21\) تاریخچه موجودی محصول**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/products/{id}/stock-history`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-38

---

### **22\) تاریخچه کامل محصول**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/products/{id}/history`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-38

---

## **E) سبد خرید**

### **23\) مشاهده سبد خرید**

* **Method:** `GET`  
* **Path:** `/api/v1/cart`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-11, UC-12, UC-13, UC-14

---

### **24\) افزودن آیتم به سبد**

* **Method:** `POST`  
* **Path:** `/api/v1/cart/items`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-11

#### **Request Body**

{  
  "product\_id": 1,  
  "quantity": 100  
}

---

### **25\) تغییر مقدار آیتم سبد**

* **Method:** `PUT`  
* **Path:** `/api/v1/cart/items/{id}`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-12

#### **Request Body**

{  
  "quantity": 150  
}

---

### **26\) حذف آیتم از سبد**

* **Method:** `DELETE`  
* **Path:** `/api/v1/cart/items/{id}`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-13

---

### **27\) خالی کردن سبد**

* **Method:** `DELETE`  
* **Path:** `/api/v1/cart`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-14

---

## **F) سفارش‌ها**

### **28\) ثبت سفارش جدید**

* **Method:** `POST`  
* **Path:** `/api/v1/orders`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-15

---

### **29\) مشاهده سفارش‌های من**

* **Method:** `GET`  
* **Path:** `/api/v1/orders`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-18

#### **Query Params**

* `status`  
* `page`  
* `limit`

---

### **30\) مشاهده جزئیات سفارش**

* **Method:** `GET`  
* **Path:** `/api/v1/orders/{id}`  
* **Auth:** دارد  
* **Use Case:** UC-19, UC-31, UC-43

---

### **31\) ویرایش سفارش قبل از تخصیص**

* **Method:** `PUT`  
* **Path:** `/api/v1/orders/{id}`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-16

#### **Request Body**

{  
  "items": \[  
    { "product\_id": 1, "quantity": 200 }  
  \]  
}

---

### **32\) لغو سفارش قبل از تخصیص**

* **Method:** `DELETE`  
* **Path:** `/api/v1/orders/{id}`  
* **Auth:** دارد  
* **Role:** `buyer`  
* **Use Case:** UC-17

---

### **33\) مشاهده همه سفارش‌ها**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/orders`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-30

#### **Query Params**

* `buyer_id`  
* `visitor_id`  
* `status`  
* `page`  
* `limit`

---

### **34\) لغو سفارش توسط ادمین**

* **Method:** `POST`  
* **Path:** `/api/v1/admin/orders/{id}/cancel`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-33

#### **Request Body**

{  
  "reason": "خطا در ثبت سفارش"  
}

---

## **G) تخصیص و وضعیت سفارش**

### **35\) تخصیص سفارش به ویزیتور**

* **Method:** `POST`  
* **Path:** `/api/v1/admin/orders/{id}/assign`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-32

#### **Request Body**

{  
  "visitor\_id": 5,  
  "reason": "ویزیتور اصلی در دسترس نبود"  
}

---

### **36\) مشاهده سفارش‌های تخصیص‌یافته به ویزیتور**

* **Method:** `GET`  
* **Path:** `/api/v1/visitor/orders`  
* **Auth:** دارد  
* **Role:** `visitor`  
* **Use Case:** UC-42

#### **Query Params**

* `status`  
* `page`  
* `limit`

---

### **37\) تغییر وضعیت سفارش توسط ویزیتور**

* **Method:** `PUT`  
* **Path:** `/api/v1/visitor/orders/{id}/status`  
* **Auth:** دارد  
* **Role:** `visitor`  
* **Use Case:** UC-44, UC-45

#### **Request Body**

{  
  "status": "loading"  
}

#### **مقادیر مجاز**

* `loading`  
* `delivered`

---

### **38\) تاریخچه وضعیت سفارش**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/orders/{id}/status-history`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-39

---

## **H) مدیریت کاربران و ویزیتورها**

### **39\) مدیریت کاربران**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/users`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-34

#### **Query Params**

* `search`  
* `role`  
* `is_active`  
* `page`  
* `limit`

---

### **40\) فعال/غیرفعال کردن کاربر**

* **Method:** `PATCH`  
* **Path:** `/api/v1/admin/users/{id}/toggle`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-34

---

### **41\) ایجاد ویزیتور جدید**

* **Method:** `POST`  
* **Path:** `/api/v1/admin/visitors`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-35

#### **Request Body**

{  
  "phone": "09123456788",  
  "full\_name": "رضا کریمی"  
}

---

### **42\) فعال/غیرفعال کردن ویزیتور**

* **Method:** `PATCH`  
* **Path:** `/api/v1/admin/visitors/{id}/toggle`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-35

---

### **43\) مشاهده عملکرد ویزیتور**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/visitors/{id}/performance`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-36

---

## **I) درخواست حذف حساب**

### **44\) لیست درخواست‌های حذف حساب**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/deletion-requests`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-40

#### **Query Params**

* `status`  
* `page`  
* `limit`

---

### **45\) بررسی درخواست حذف حساب**

* **Method:** `PATCH`  
* **Path:** `/api/v1/admin/deletion-requests/{id}`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-40

#### **Request Body**

{  
  "status": "approved",  
  "admin\_note": "درخواست تأیید شد"  
}

#### **مقادیر مجاز**

* `approved`  
* `rejected`

---

## **J) گزارش‌ها**

### **46\) مشاهده گزارش‌ها**

* **Method:** `GET`  
* **Path:** `/api/v1/admin/reports`  
* **Auth:** دارد  
* **Role:** `admin`  
* **Use Case:** UC-37

#### **Query Params**

* `type`  
* `start_date`  
* `end_date`

#### **مقادیر `type`**

* `sales`  
* `users`  
* `products`  
* `visitors`  
* `inventory`  
* `price_changes`

---

### **`47)`داشبورد مدیریتی**

**`Method`**`` : `GET` ``  

**`Path`**`` : `/api/v1/admin/dashboard` ``  

**`Auth`**`: دارد`  

**`Role`**`: admin`  

**`Use Case`**`: UC-23`

**`هدف`**

`نمایش خلاصه وضعیت سیستم برای مدیر.`

**`پاسخ موفق`**

**`Status`**`` : `200 OK` ``

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

---

## **K) اعلان‌ها**

### **48\) مشاهده اعلان‌های من**

* **Method:** `GET`  
* **Path:** `/api/v1/notifications`  
* **Auth:** دارد  
* **Use Case:** UC-20, UC-51

---

### **49\) علامت‌گذاری اعلان به‌عنوان خوانده‌شده**

* **Method:** `PATCH`  
* **Path:** `/api/v1/notifications/{id}/read`  
* **Auth:** دارد  
* **Use Case:** UC-20, UC-51

---

### **50\) علامت‌گذاری همه اعلان‌ها به‌عنوان خوانده‌شده**

* **Method:** `PATCH`  
* **Path:** `/api/v1/notifications/read-all`  
* **Auth:** دارد

---

## **L) تنظیمات سیستمی**

### **51\) دریافت تنظیمات سیستم**

* **Method:** `GET`  
* **Path:** `/api/v1/system/settings`  
* **Auth:** ندارد  
* **Use Case:** UC-10

#### **پاسخ نمونه**

{  
  "success": true,  
  "data": {  
    "factory\_phone": "021-12345678"  
  }  
}

---

### **52\) ویرایش تنظیمات سیستم**

* **Method:** `PATCH`  
* **Path:** `/api/v1/admin/system-settings/{key}`  
* **Auth:** دارد  
* **Role:** `admin`

#### **Request Body**

{  
  "value": "021-87654321"  
}

---

## **M) فاز بعد / اختیاری**

### **53\) دریافت فاکتور PDF**

* **Method:** `GET`  
* **Path:** `/api/v1/orders/{id}/invoice`  
* **Auth:** دارد  
* **Access:** خریدار صاحب سفارش یا ادمین  
* **Use Case:** UC-53  
* **Output:** `application/pdf`

---

# **۴) محدودیت‌های مهم**

## **فیلتر محصولات**

پارامتر `in_stock` اضافه شد و به این صورت کار می‌کند:

* `true` → فقط محصولات موجود  
* `false` → فقط محصولات ناموجود  
* خالی → همه محصولات

## **آپلود تصویر**

* فرمت مجاز: `jpg`, `jpeg`, `png`, `webp`  
* حداکثر حجم: 5MB  
* حداکثر تعداد: 5 تصویر در هر درخواست

## **اعلان‌ها**

* `read` برای یک اعلان  
* `read-all` برای همه اعلان‌ها

---

# **۵) جمع‌بندی**

این نسخه نسبت به نسخه قبلی کامل‌تر است چون:

* `in_stock` به فیلتر محصولات اضافه شد  
* `read-all` برای اعلان‌ها اضافه شد  
* محدودیت‌های آپلود تصویر مشخص شد  
* جدول کدهای خطا اضافه شد  
* endpoint فاکتور PDF هم به بخش اختیاری/فاز بعد اضافه شد  
* endpointهای حذف حساب برای ادمین کامل شدند

