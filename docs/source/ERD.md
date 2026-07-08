# **ERD نهایی و به‌روزشده**

## **اپلیکیشن فروش محصولات پلاستیکی**

---

## **۱) رویکرد طراحی**

این ERD بر اساس Use Case Diagram، Use Case Specification و Activity Diagramهای نهایی طراحی شده است.  
در این مدل، سه سطح اصلی در نظر گرفته شده است:

* **هسته‌ی کاربری و احراز هویت**  
* **محصولات، سفارش و تاریخچه‌ها**  
* **اعلان‌ها، تنظیمات و درخواست حذف حساب**

این نسخه برای پیاده‌سازی در PostgreSQL مناسب است و علاوه بر موجودیت‌های اصلی، مواردی مانند `AccountDeletionRequest` و `SystemSetting` را هم پوشش می‌دهد.

---

## **۲) موجودیت‌ها**

### **۲-۱. User**

کاربران سیستم شامل خریدار، ادمین و ویزیتور هستند.

**فیلدها**

* `id` : BIGINT / UUID  
* `phone` : VARCHAR(11) NOT NULL UNIQUE  
* `full_name` : VARCHAR(100) NOT NULL  
* `address` : TEXT NULL  
* `role` : VARCHAR(20) NOT NULL  
* `is_active` : BOOLEAN DEFAULT TRUE  
* `created_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()  
* `updated_at` : TIMESTAMP WITH TIME ZONE NULL

**نکته**

* `role` فقط یکی از `admin`, `buyer`, `visitor` است.

---

### **۲-۲. Product**

محصولات پلاستیکی ثبت‌شده توسط ادمین.

**فیلدها**

* `id` : BIGINT / UUID  
* `title` : VARCHAR(200) NOT NULL  
* `price` : DECIMAL(15,2) NOT NULL  
* `weight` : DECIMAL(10,2) NOT NULL  
* `color` : VARCHAR(50) NULL  
* `quality` : VARCHAR(20) NOT NULL  
* `description` : TEXT NULL  
* `image_urls` : JSONB NULL  
* `stock` : INTEGER NOT NULL DEFAULT 0  
* `is_active` : BOOLEAN DEFAULT TRUE  
* `created_by` : BIGINT NOT NULL  
* `created_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()  
* `updated_at` : TIMESTAMP WITH TIME ZONE NULL

**نکته**

* `created_by` باید به یک `User` با نقش `admin` اشاره کند.

---

### **۲-۳. CartItem**

آیتم‌های موقت سبد خرید خریدار.

**فیلدها**

* `id` : BIGINT / UUID  
* `user_id` : BIGINT NOT NULL  
* `product_id` : BIGINT NOT NULL  
* `quantity` : DECIMAL(10,2) NOT NULL  
* `added_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()  
* `updated_at` : TIMESTAMP WITH TIME ZONE NULL

**نکته**

* برای جلوگیری از تکرار، `UNIQUE(user_id, product_id)` پیشنهاد می‌شود.

---

### **۲-۴. Order**

سفارش نهایی ثبت‌شده توسط خریدار.

**فیلدها**

* `id` : BIGINT / UUID  
* `buyer_id` : BIGINT NOT NULL  
* `visitor_id` : BIGINT NULL  
* `total_price` : DECIMAL(15,2) NOT NULL  
* `status` : VARCHAR(20) NOT NULL  
* `created_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()  
* `updated_at` : TIMESTAMP WITH TIME ZONE NULL

**نکته**

* `visitor_id` ویزیتور فعلی سفارش را نگه می‌دارد.  
* `status` یکی از `pending`, `assigned`, `loading`, `delivered`, `cancelled` است.

---

### **۲-۵. OrderItem**

آیتم‌های داخل هر سفارش.

**فیلدها**

* `id` : BIGINT / UUID  
* `order_id` : BIGINT NOT NULL  
* `product_id` : BIGINT NOT NULL  
* `quantity` : DECIMAL(10,2) NOT NULL  
* `unit_price` : DECIMAL(15,2) NOT NULL  
* `total_price` : DECIMAL(15,2) NOT NULL

**نکته**

* `unit_price` قیمت فریز شده در زمان ثبت سفارش است.  
* `total_price = quantity × unit_price`

---

### **۲-۶. OrderAssignment**

تاریخچه تخصیص سفارش به ویزیتور.

**فیلدها**

* `id` : BIGINT / UUID  
* `order_id` : BIGINT NOT NULL  
* `old_visitor_id` : BIGINT NULL  
* `new_visitor_id` : BIGINT NOT NULL  
* `assigned_by` : BIGINT NOT NULL  
* `reason` : TEXT NULL  
* `assigned_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()

**نکته**

* این جدول برای ثبت تاریخچه تخصیص و تخصیص مجدد استفاده می‌شود.

---

### **۲-۷. OrderStatusHistory**

تاریخچه تغییر وضعیت سفارش.

**فیلدها**

* `id` : BIGINT / UUID  
* `order_id` : BIGINT NOT NULL  
* `old_status` : VARCHAR(20) NOT NULL  
* `new_status` : VARCHAR(20) NOT NULL  
* `changed_by` : BIGINT NOT NULL  
* `note` : TEXT NULL  
* `changed_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()

**نکته**

* این موجودیت برای ردیابی کامل مسیر سفارش ضروری است.

---

### **۲-۸. PriceHistory**

تاریخچه تغییرات قیمت محصول.

**فیلدها**

* `id` : BIGINT / UUID  
* `product_id` : BIGINT NOT NULL  
* `old_price` : DECIMAL(15,2) NULL  
* `new_price` : DECIMAL(15,2) NOT NULL  
* `changed_by` : BIGINT NOT NULL  
* `changed_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()

**نکته**

* `old_price` در رکورد اولیه می‌تواند `NULL` باشد.

---

### **۲-۹. StockHistory**

تاریخچه تغییرات موجودی محصول.

**فیلدها**

* `id` : BIGINT / UUID  
* `product_id` : BIGINT NOT NULL  
* `old_stock` : INTEGER NULL  
* `new_stock` : INTEGER NOT NULL  
* `reason` : VARCHAR(50) NOT NULL  
* `changed_by` : BIGINT NOT NULL  
* `changed_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()

**نکته**

* `old_stock` در رکورد اولیه می‌تواند `NULL` باشد.

---

### **۲-۱۰. OTPCode**

کدهای یک‌بارمصرف برای ثبت‌نام، ورود و تغییر شماره تلفن.

**فیلدها**

* `id` : BIGINT / UUID  
* `phone` : VARCHAR(11) NOT NULL  
* `code` : VARCHAR(5) NOT NULL  
* `purpose` : VARCHAR(20) NOT NULL  
* `expires_at` : TIMESTAMP WITH TIME ZONE NOT NULL  
* `is_used` : BOOLEAN DEFAULT FALSE  
* `attempt_count` : INTEGER DEFAULT 0  
* `locked_until` : TIMESTAMP WITH TIME ZONE NULL  
* `created_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()

**نکته**

* `purpose` می‌تواند `register`, `login`, `change_phone` باشد.

---

### **۲-۱۱. Notification**

تاریخچه پیامک‌ها و اعلان‌های ارسالی.

**فیلدها**

* `id` : BIGINT / UUID  
* `user_id` : BIGINT NOT NULL  
* `related_type` : VARCHAR(30) NULL  
* `related_id` : BIGINT NULL  
* `type` : VARCHAR(10) NOT NULL  
* `message` : TEXT NOT NULL  
* `sent_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()  
* `is_read` : BOOLEAN DEFAULT FALSE

---

### **۲-۱۲. AccountDeletionRequest**

درخواست حذف حساب کاربری.

**فیلدها**

* `id` : BIGINT / UUID  
* `user_id` : BIGINT NOT NULL  
* `status` : VARCHAR(20) NOT NULL  
* `requested_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()  
* `reviewed_by` : BIGINT NULL  
* `reviewed_at` : TIMESTAMP WITH TIME ZONE NULL  
* `admin_note` : TEXT NULL

**نکته**

* `status` یکی از `pending`, `approved`, `rejected` است.

---

### **۲-۱۳. SystemSetting**

تنظیمات کلی سیستم.

**فیلدها**

* `key` : VARCHAR(100) PRIMARY KEY  
* `value` : TEXT NOT NULL  
* `description` : TEXT NULL  
* `updated_at` : TIMESTAMP WITH TIME ZONE DEFAULT NOW()

**نکته**

* برای شماره تماس کارخانه، یک رکورد با `key = factory_phone` نگهداری می‌شود.

---

## **۳) روابط و کاردینالیتی‌ها**

### **۳-۱. روابط اصلی**

* یک `User` با نقش `buyer` می‌تواند چندین `Order` ثبت کند.  
* یک `User` با نقش `admin` می‌تواند چندین `Product` ایجاد کند.  
* یک `User` با نقش `visitor` می‌تواند چندین `Order` تحویل دهد.  
* هر `Order` شامل چندین `OrderItem` است.  
* هر `OrderItem` به یک `Product` اشاره می‌کند.  
* هر `User` با نقش `buyer` می‌تواند چندین `CartItem` داشته باشد.  
* هر `Product` می‌تواند در چندین `CartItem` و `OrderItem` ظاهر شود.  
* هر `Product` می‌تواند چندین رکورد `PriceHistory` و `StockHistory` داشته باشد.  
* هر `Order` می‌تواند چندین رکورد `OrderAssignment` و `OrderStatusHistory` داشته باشد.  
* هر `User` می‌تواند چندین `Notification` دریافت کند.  
* هر `User` می‌تواند چندین `AccountDeletionRequest` ثبت کند.  
* هر `OTPCode` فقط به یک شماره تلفن و یک هدف مشخص تعلق دارد.

---

### **۳-۲. کاردینالیتی پیشنهادی**

* `User 1 --- * Product`  
* `User 1 --- * Order`  
* `User 1 --- * CartItem`  
* `Product 1 --- * CartItem`  
* `Order 1 --- * OrderItem`  
* `Product 1 --- * OrderItem`  
* `Order 1 --- * OrderAssignment`  
* `Order 1 --- * OrderStatusHistory`  
* `Product 1 --- * PriceHistory`  
* `Product 1 --- * StockHistory`  
* `User 1 --- * PriceHistory`  
* `User 1 --- * StockHistory`  
* `User 1 --- * OrderAssignment`  
* `User 1 --- * OrderStatusHistory`  
* `User 1 --- * Notification`  
* `User 1 --- * AccountDeletionRequest`  
* `User 1 --- * AccountDeletionRequest` as reviewer

---

## **۴) محدودیت‌ها و قیود مهم**

### **۴-۱. User**

* `phone` باید یکتا باشد.  
* `role` فقط یکی از `admin`, `buyer`, `visitor` باشد.  
* کاربر غیرفعال حق ورود ندارد.

### **۴-۲. Product**

* `price >= 0`  
* `stock >= 0`  
* `quality` فقط `اولیه` یا `بازیافتی`  
* `image_urls` حداکثر ۵ تصویر را نگه دارد.

### **۴-۳. CartItem**

* `quantity > 0`  
* برای هر `user_id` و `product_id` فقط یک رکورد فعال وجود داشته باشد.

### **۴-۴. Order / OrderItem**

* `status` فقط یکی از وضعیت‌های مجاز باشد.  
* قیمت `unit_price` در زمان ثبت سفارش فریز شود.  
* `total_price` برابر مجموع آیتم‌ها باشد.  
* پس از ثبت موفق سفارش، `CartItem`های مرتبط حذف شوند.

### **۴-۵. OTPCode**

* کد باید زمان‌دار و یک‌بارمصرف باشد.  
* پس از ۵ تلاش ناموفق، قفل موقت اعمال شود.

### **۴-۶. AccountDeletionRequest**

* درخواست فقط یکی از وضعیت‌های `pending`, `approved`, `rejected` را داشته باشد.  
* `reviewed_by` و `reviewed_at` فقط پس از بررسی مقدار می‌گیرند.

### **۴-۷. SystemSetting**

* `key` باید یکتا باشد.  
* `factory_phone` به‌عنوان تنظیم کل سیستم ذخیره می‌شود.

---

## **۵) ملاحظات فیزیکی و Indexing**

این موارد برای Physical ERD و DDL نهایی مهم هستند:

### **ایندکس‌های پیشنهادی**

* `User.phone`  
* `Order.buyer_id`  
* `Order.status`  
* `OrderItem.order_id`  
* `Product.title`  
* `OrderAssignment.order_id`  
* `AccountDeletionRequest.user_id`  
* `OTPCode.phone`  
* `Notification.user_id`

### **CHECK Constraintهای پیشنهادی**

* `price >= 0`  
* `stock >= 0`  
* `quantity > 0`  
* `status` در بازه‌ی مقادیر مجاز  
* `role` در بازه‌ی مقادیر مجاز

---

## **۶) PlantUML نهایی ERD**

@startuml  
title ERD \- اپلیکیشن فروش محصولات پلاستیکی  
left to right direction  
skinparam classAttributeIconSize 0  
skinparam shadowing false  
skinparam backgroundColor white  
skinparam defaultFontName Vazirmatn

class User {  
  \+id: BIGINT  
  phone: VARCHAR(11)  
  full\_name: VARCHAR(100)  
  address: TEXT  
  role: VARCHAR(20)  
  is\_active: BOOLEAN  
  created\_at: TIMESTAMP  
  updated\_at: TIMESTAMP  
}

class Product {  
  \+id: BIGINT  
  title: VARCHAR(200)  
  price: DECIMAL(15,2)  
  weight: DECIMAL(10,2)  
  color: VARCHAR(50)  
  quality: VARCHAR(20)  
  description: TEXT  
  image\_urls: JSONB  
  stock: INTEGER  
  is\_active: BOOLEAN  
  created\_by: BIGINT  
  created\_at: TIMESTAMP  
  updated\_at: TIMESTAMP  
}

class CartItem {  
  \+id: BIGINT  
  user\_id: BIGINT  
  product\_id: BIGINT  
  quantity: DECIMAL(10,2)  
  added\_at: TIMESTAMP  
  updated\_at: TIMESTAMP  
}

class Order {  
  \+id: BIGINT  
  buyer\_id: BIGINT  
  visitor\_id: BIGINT  
  total\_price: DECIMAL(15,2)  
  status: VARCHAR(20)  
  created\_at: TIMESTAMP  
  updated\_at: TIMESTAMP  
}

class OrderItem {  
  \+id: BIGINT  
  order\_id: BIGINT  
  product\_id: BIGINT  
  quantity: DECIMAL(10,2)  
  unit\_price: DECIMAL(15,2)  
  total\_price: DECIMAL(15,2)  
}

class OrderAssignment {  
  \+id: BIGINT  
  order\_id: BIGINT  
  old\_visitor\_id: BIGINT  
  new\_visitor\_id: BIGINT  
  assigned\_by: BIGINT  
  reason: TEXT  
  assigned\_at: TIMESTAMP  
}

class OrderStatusHistory {  
  \+id: BIGINT  
  order\_id: BIGINT  
  old\_status: VARCHAR(20)  
  new\_status: VARCHAR(20)  
  changed\_by: BIGINT  
  note: TEXT  
  changed\_at: TIMESTAMP  
}

class PriceHistory {  
  \+id: BIGINT  
  product\_id: BIGINT  
  old\_price: DECIMAL(15,2)  
  new\_price: DECIMAL(15,2)  
  changed\_by: BIGINT  
  changed\_at: TIMESTAMP  
}

class StockHistory {  
  \+id: BIGINT  
  product\_id: BIGINT  
  old\_stock: INTEGER  
  new\_stock: INTEGER  
  reason: VARCHAR(50)  
  changed\_by: BIGINT  
  changed\_at: TIMESTAMP  
}

class OTPCode {  
  \+id: BIGINT  
  phone: VARCHAR(11)  
  code: VARCHAR(5)  
  purpose: VARCHAR(20)  
  expires\_at: TIMESTAMP  
  is\_used: BOOLEAN  
  attempt\_count: INTEGER  
  locked\_until: TIMESTAMP  
  created\_at: TIMESTAMP  
}

class Notification {  
  \+id: BIGINT  
  user\_id: BIGINT  
  related\_type: VARCHAR(30)  
  related\_id: BIGINT  
  type: VARCHAR(10)  
  message: TEXT  
  sent\_at: TIMESTAMP  
  is\_read: BOOLEAN  
}

class AccountDeletionRequest {  
  \+id: BIGINT  
  user\_id: BIGINT  
  status: VARCHAR(20)  
  requested\_at: TIMESTAMP  
  reviewed\_by: BIGINT  
  reviewed\_at: TIMESTAMP  
  admin\_note: TEXT  
}

class SystemSetting {  
  \+key: VARCHAR(100)  
  value: TEXT  
  description: TEXT  
  updated\_at: TIMESTAMP  
}

User "1" \-- "0..\*" Product : created\_by  
User "1" \-- "0..\*" Order : buyer\_id  
User "1" \-- "0..\*" Order : visitor\_id  
User "1" \-- "0..\*" CartItem  
Product "1" \-- "0..\*" CartItem  
Order "1" \-- "0..\*" OrderItem  
Product "1" \-- "0..\*" OrderItem  
Order "1" \-- "0..\*" OrderAssignment  
Order "1" \-- "0..\*" OrderStatusHistory  
Product "1" \-- "0..\*" PriceHistory  
Product "1" \-- "0..\*" StockHistory  
User "1" \-- "0..\*" PriceHistory : changed\_by  
User "1" \-- "0..\*" StockHistory : changed\_by  
User "1" \-- "0..\*" OrderAssignment : assigned\_by  
User "1" \-- "0..\*" OrderStatusHistory : changed\_by  
User "1" \-- "0..\*" Notification  
User "1" \-- "0..\*" AccountDeletionRequest : requester  
User "1" \-- "0..\*" AccountDeletionRequest : reviewer  
@enduml

---

## **۷) نکات اجرایی مهم**

* `total_price` در `Order` یک denormalization حساب‌شده است و بعد از ذخیره `OrderItem`ها محاسبه می‌شود.  
* پس از ثبت موفق سفارش، `CartItem`های مرتبط حذف می‌شوند.  
* `AccountDeletionRequest` وضعیت مستقل و قابل رهگیری دارد.  
* `SystemSetting` برای ذخیره‌ی شماره تماس کارخانه استفاده می‌شود.  
* `old_price` و `old_stock` در رکورد اولیه می‌توانند `NULL` باشند.

---

## **۸) جمع‌بندی**

این نسخه‌ی ERD برای تحلیل، مستندسازی و پیاده‌سازی قابل استفاده است و اصلاحات کلیدی زیر را پوشش می‌دهد:

* تاریخچه وضعیت سفارش  
* درخواست حذف حساب  
* شماره تماس کارخانه در تنظیمات سیستمی  
* قفل OTP  
* تاریخچه قیمت و موجودی  
* سبد خرید موقت  
* تخصیص و تخصیص مجدد ویزیتور

این نسخه هم برای **Logical ERD** مناسب است و هم به‌راحتی می‌تواند به **Physical ERD** و DDL نهایی PostgreSQL تبدیل شود.

