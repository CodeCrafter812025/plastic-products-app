# لیست نهایی Use Case ها و نمودار PlantUML هماهنگ‌شده

## اپلیکیشن فروش محصولات پلاستیکی

---

## ۱) بازیگران سیستم

### بازیگران اصلی

* خریدار (Buyer)

* ادمین (Admin)

* ویزیتور (Visitor)

### بازیگران خارجی

* سرویس پیامک (SMS Gateway)

* سیستم فایل / ذخیره‌سازی تصاویر

### بازیگر پایه

* کاربر سیستم (System User)

---

## ۲) اولویت‌بندی

* **ضروری:** باید در نسخه اول پیاده‌سازی شود.

* **مهم:** بهتر است در نسخه اول باشد.

* **اختیاری / فاز بعد:** می‌تواند بعداً اضافه شود.

---

## ۳) Use Case های خریدار

| کد | Use Case | اولویت | توضیح |
| :---- | :---- | ----: | :---- |
| UC-01 | ثبت‌نام با OTP | ضروری | ثبت‌نام با شماره تلفن و کد یک‌بارمصرف |
| UC-02 | ورود با OTP | ضروری | ورود به سیستم با شماره تلفن و OTP |
| UC-03 | مشاهده پروفایل | ضروری | مشاهده اطلاعات حساب کاربری |
| UC-04 | ویرایش پروفایل | ضروری | ویرایش نام، آدرس و اطلاعات کاربر |
| UC-05 | تغییر شماره تلفن | مهم | تغییر شماره با تأیید OTP جدید |
| UC-06 | مشاهده لیست محصولات | ضروری | دیدن محصولات فعال و موجود |
| UC-07 | جستجوی محصول | ضروری | جستجو بر اساس عنوان یا توضیحات |
| UC-08 | فیلتر محصولات | ضروری | فیلتر بر اساس کیفیت، رنگ و بازه قیمت |
| UC-09 | مشاهده جزئیات محصول | ضروری | دیدن عکس، قیمت، وزن، کیفیت و موجودی |
| UC-10 | تماس با کارخانه | مهم | تماس مستقیم از صفحه محصول |
| UC-11 | افزودن به سبد خرید | ضروری | افزودن یک یا چند محصول به سبد |
| UC-12 | تغییر مقدار آیتم سبد | ضروری | تغییر وزن/تعداد آیتم‌ها |
| UC-13 | حذف آیتم از سبد | ضروری | حذف یک آیتم از سبد |
| UC-14 | خالی کردن سبد خرید | ضروری | حذف همه آیتم‌های سبد |
| UC-15 | ثبت سفارش | ضروری | نهایی‌سازی سفارش و ارسال به سیستم |
| UC-16 | ویرایش سفارش قبل از تخصیص | ضروری | اصلاح سفارش قبل از تخصیص به ویزیتور |
| UC-17 | لغو سفارش قبل از تخصیص | ضروری | لغو سفارش قبل از تخصیص |
| UC-18 | مشاهده سفارش‌های من | ضروری | دیدن لیست سفارش‌های قبلی |
| UC-19 | مشاهده جزئیات سفارش | ضروری | مشاهده آیتم‌ها، مبلغ و وضعیت سفارش |
| UC-20 | دریافت پیامک وضعیت سفارش | ضروری | دریافت پیامک هنگام تغییر وضعیت |
| UC-21 | درخواست حذف حساب کاربری | مهم | ارسال درخواست حذف به ادمین |

---

## ۴) Use Case های ادمین

| کد | Use Case | اولویت | توضیح |
| :---- | :---- | ----: | :---- |
| UC-22 | ورود ادمین | ضروری | ورود با OTP |
| UC-23 | مشاهده داشبورد مدیریتی | ضروری | آمارهای کلیدی سیستم |
| UC-24 | ثبت محصول جدید | ضروری | ثبت محصول با اطلاعات کامل |
| UC-25 | ویرایش محصول | ضروری | ویرایش اطلاعات محصول |
| UC-26 | تغییر قیمت محصول | ضروری | تغییر قیمت و ثبت تاریخچه |
| UC-27 | تغییر موجودی محصول | ضروری | افزایش/کاهش موجودی |
| UC-28 | فعال/غیرفعال کردن محصول | مهم | مخفی/نمایان کردن محصول |
| UC-29 | حذف محصول | مهم | حذف کامل با تأیید نهایی |
| UC-30 | مشاهده لیست سفارش‌ها | ضروری | مشاهده همه سفارش‌ها |
| UC-31 | مشاهده جزئیات سفارش | ضروری | مشاهده جزئیات سفارش‌های همه کاربران |
| UC-32 | تخصیص سفارش به ویزیتور | ضروری | تخصیص هر سفارش به یک ویزیتور |
| UC-33 | لغو سفارش توسط ادمین | مهم | لغو سفارش و ثبت یادداشت سیستمی |
| UC-34 | مدیریت کاربران | ضروری | مشاهده، جستجو، فعال/غیرفعال کردن کاربران |
| UC-35 | ایجاد و مدیریت ویزیتور | ضروری | ایجاد، فعال/غیرفعال و مدیریت حساب ویزیتور |
| UC-36 | مشاهده عملکرد ویزیتور | مهم | دیدن آمار تحویل‌ها و عملکرد |
| UC-37 | مشاهده گزارش‌ها | ضروری | گزارش فروش، موجودی، کاربران و محصولات |
| UC-38 | مشاهده تاریخچه تغییرات محصول | ضروری | دیدن تاریخچه قیمت و موجودی |
| UC-39 | مشاهده تاریخچه وضعیت سفارش | ضروری | بررسی تاریخچه وضعیت هر سفارش |
| UC-40 | تأیید/رد درخواست حذف حساب | مهم | بررسی و نهایی‌سازی حذف حساب کاربر |

---

## ۵) Use Case های ویزیتور

| کد | Use Case | اولویت | توضیح |
| :---- | :---- | ----: | :---- |
| UC-41 | ورود ویزیتور | ضروری | ورود با شماره تلفن و OTP |
| UC-42 | مشاهده سفارش‌های تخصیص‌یافته | ضروری | فقط سفارش‌های مربوط به خود را می‌بیند |
| UC-43 | مشاهده جزئیات سفارش | ضروری | مشاهده آدرس، شماره تماس و آیتم‌ها |
| UC-44 | تغییر وضعیت به «بارگیری شد» | ضروری | پس از دریافت کالا از انبار |
| UC-45 | تغییر وضعیت به «تحویل داده شد» | ضروری | پس از تحویل و تسویه حضوری |

---

## ۶) Use Case های سیستمی / پشتیبان

| کد | Use Case | اولویت | توضیح |
| :---- | :---- | ----: | :---- |
| UC-46 | ارسال OTP | ضروری | ارسال کد ورود/ثبت‌نام |
| UC-47 | اعتبارسنجی OTP | ضروری | بررسی درستی و اعتبار کد |
| UC-48 | کنترل موجودی هنگام سفارش | ضروری | بررسی کفایت stock |
| UC-49 | محاسبه مبلغ نهایی سفارش | ضروری | محاسبه مبلغ بر اساس قیمت فریز شده |
| UC-50 | ثبت تاریخچه تغییرات | ضروری | ثبت تغییرات قیمت، موجودی و وضعیت |
| UC-51 | ارسال پیامک اطلاع‌رسانی | ضروری | پیامک وضعیت سفارش و رویدادها |
| UC-52 | انقضای نشست کاربر | ضروری | خروج خودکار پس از ۳۰ دقیقه عدم فعالیت |

---

## ۷) Use Case های اختیاری / فاز بعد

| کد | Use Case | اولویت | توضیح |
| :---- | :---- | ----: | :---- |
| UC-53 | دریافت فاکتور PDF | اختیاری | دریافت نسخه فایل فاکتور |
| UC-54 | جستجوی پیشرفته | اختیاری | فیلترهای تخصصی‌تر برای آینده |

---

## ۸) نکات مدل‌سازی

* **مشاهده جزئیات سفارش** به‌صورت یک Use Case واحد در نظر گرفته شده و به خریدار، ادمین و ویزیتور متصل است.

* **تغییر شماره تلفن** مستقل نگه داشته شده و با OTP تأیید می‌شود.

* **مشاهده تاریخچه وضعیت سفارش** به ادمین متصل است.

* **دریافت فاکتور PDF** و **جستجوی پیشرفته** در بخش اختیاری قرار گرفته‌اند تا دیاگرام اصلی شلوغ نشود.

* **کاربر سیستم** به‌عنوان Actor پایه برای ورود مشترک خریدار، ادمین و ویزیتور در نظر گرفته شده است.

---

## 

## ۹) PlantUML نهایی

# **Use Case Diagram های نهایی و ماژول‌بندی‌شده**

## **اپلیکیشن فروش محصولات پلاستیکی**

---

## **۱) نمودار خریدار**

@startuml buyer\_uc\_01

title Use Case Diagram \- خریدار

left to right direction

skinparam packageStyle rectangle

skinparam shadowing false

skinparam backgroundColor white

skinparam defaultFontName Vazirmatn

skinparam actorStyle awesome

skinparam linetype ortho

actor "مهمان" as Guest

actor "کاربر سیستم" as SystemUser

actor "خریدار" as Buyer

actor "سرویس پیامک" as SMS

Buyer \--|\> SystemUser

rectangle "اپلیکیشن فروش محصولات پلاستیکی" as System {

  package "حساب کاربری" {

    (ثبت‌نام با OTP) as UC\_Register

    (ورود با OTP) as UC\_Login

    (مشاهده پروفایل) as UC\_ViewProfile

    (ویرایش پروفایل) as UC\_EditProfile

    (تغییر شماره تلفن) as UC\_ChangePhone

    (درخواست حذف حساب کاربری) as UC\_DeleteRequest

  }

  package "محصولات" {

    (مشاهده لیست محصولات) as UC\_ListProducts

    (جستجوی محصول) as UC\_Search

    (فیلتر محصولات) as UC\_Filter

    (مشاهده جزئیات محصول) as UC\_ProductDetails

    (تماس با کارخانه) as UC\_CallFactory

  }

  package "سبد خرید و سفارش" {

    (افزودن به سبد خرید) as UC\_AddCart

    (تغییر مقدار آیتم) as UC\_UpdateCart

    (حذف آیتم از سبد) as UC\_RemoveCart

    (خالی کردن سبد خرید) as UC\_ClearCart

    (ثبت سفارش) as UC\_PlaceOrder

    (ویرایش سفارش قبل از تخصیص) as UC\_EditOrder

    (لغو سفارش قبل از تخصیص) as UC\_CancelOrder

    (مشاهده سفارش‌های من) as UC\_MyOrders

    (مشاهده جزئیات سفارش) as UC\_ViewOrderDetails

    (دریافت پیامک وضعیت سفارش) as UC\_OrderSMS

    (دریافت فاکتور PDF) as UC\_GetInvoice

    (جستجوی پیشرفته) as UC\_AdvancedSearch

  }

  package "فرایندهای سیستمی" {

    (ارسال OTP) as UC\_SendOTP

    (اعتبارسنجی OTP) as UC\_ValidateOTP

    (کنترل موجودی هنگام ثبت سفارش) as UC\_CheckStock

    (محاسبه مبلغ نهایی سفارش) as UC\_CalcTotal

    (ثبت تاریخچه تغییرات) as UC\_LogHistory

    (انقضای نشست کاربر) as UC\_SessionTimeout

    (ارسال پیامک اطلاع‌رسانی) as UC\_SendNotification

  }

}

Guest \--\> UC\_Register

Guest \--\> UC\_Login

SystemUser \--\> UC\_ViewProfile

SystemUser \--\> UC\_EditProfile

SystemUser \--\> UC\_ChangePhone

SystemUser \--\> UC\_DeleteRequest

Buyer \--\> UC\_ListProducts

Buyer \--\> UC\_Search

Buyer \--\> UC\_Filter

Buyer \--\> UC\_ProductDetails

Buyer \--\> UC\_CallFactory

Buyer \--\> UC\_AddCart

Buyer \--\> UC\_UpdateCart

Buyer \--\> UC\_RemoveCart

Buyer \--\> UC\_ClearCart

Buyer \--\> UC\_PlaceOrder

Buyer \--\> UC\_EditOrder

Buyer \--\> UC\_CancelOrder

Buyer \--\> UC\_MyOrders

Buyer \--\> UC\_ViewOrderDetails

Buyer \--\> UC\_OrderSMS

Buyer \--\> UC\_GetInvoice

Buyer \--\> UC\_AdvancedSearch

UC\_Register ..\> UC\_SendOTP : \<\<include\>\>

UC\_Login ..\> UC\_SendOTP : \<\<include\>\>

UC\_Register ..\> UC\_ValidateOTP : \<\<include\>\>

UC\_Login ..\> UC\_ValidateOTP : \<\<include\>\>

UC\_ChangePhone ..\> UC\_SendOTP : \<\<include\>\>

UC\_ChangePhone ..\> UC\_ValidateOTP : \<\<include\>\>

UC\_PlaceOrder ..\> UC\_CheckStock : \<\<include\>\>

UC\_PlaceOrder ..\> UC\_CalcTotal : \<\<include\>\>

UC\_PlaceOrder ..\> UC\_LogHistory : \<\<include\>\>

UC\_CancelOrder ..\> UC\_LogHistory : \<\<include\>\>

UC\_EditOrder ..\> UC\_LogHistory : \<\<include\>\>

UC\_OrderSMS ..\> UC\_SendNotification : \<\<include\>\>

UC\_CallFactory ..\> UC\_ProductDetails : \<\<extend\>\>

UC\_Search ..\> UC\_ListProducts : \<\<extend\>\>

UC\_Filter ..\> UC\_ListProducts : \<\<extend\>\>

UC\_GetInvoice ..\> UC\_ViewOrderDetails : \<\<extend\>\>

UC\_AdvancedSearch ..\> UC\_Search : \<\<extend\>\>

UC\_SessionTimeout ..\> UC\_Login : \<\<extend\>\>

UC\_SendOTP \--\> SMS

UC\_SendNotification \--\> SMS

@enduml

---

## **۲) نمودار ادمین**

@startuml admin\_uc\_02

title Use Case Diagram \- ادمین

left to right direction

skinparam packageStyle rectangle

skinparam shadowing false

skinparam backgroundColor white

skinparam defaultFontName Vazirmatn

skinparam actorStyle awesome

skinparam linetype ortho

actor "مهمان" as Guest

actor "کاربر سیستم" as SystemUser

actor "ادمین" as Admin

actor "سرویس پیامک" as SMS

actor "سیستم فایل/ذخیره‌سازی تصاویر" as Storage

Admin \--|\> SystemUser

rectangle "اپلیکیشن فروش محصولات پلاستیکی" as System {

  package "حساب کاربری" {

    (ورود با OTP) as UC\_Login

    (مشاهده پروفایل) as UC\_ViewProfile

    (ویرایش پروفایل) as UC\_EditProfile

    (تغییر شماره تلفن) as UC\_ChangePhone

    (درخواست حذف حساب کاربری) as UC\_DeleteRequest

  }

  package "مدیریت محصولات" {

    (ثبت محصول جدید) as UC\_AddProduct

    (ویرایش محصول) as UC\_EditProduct

    (تغییر قیمت محصول) as UC\_ChangePrice

    (تغییر موجودی محصول) as UC\_ChangeStock

    (فعال/غیرفعال کردن محصول) as UC\_ToggleProduct

    (حذف محصول) as UC\_DeleteProduct

    (مشاهده تاریخچه تغییرات محصول) as UC\_ProductHistory

  }

  package "مدیریت سفارشات" {

    (مشاهده لیست سفارش‌ها) as UC\_OrderList

    (مشاهده جزئیات سفارش) as UC\_ViewOrderDetails

    (تخصیص سفارش به ویزیتور) as UC\_AssignVisitor

    (لغو سفارش توسط ادمین) as UC\_AdminCancelOrder

    (مشاهده تاریخچه وضعیت سفارش) as UC\_OrderStatusHistory

  }

  package "مدیریت ویزیتور" {

    (ایجاد ویزیتور جدید) as UC\_CreateVisitor

    (فعال/غیرفعال کردن ویزیتور) as UC\_ToggleVisitor

    (مشاهده عملکرد ویزیتور) as UC\_VisitorPerformance

  }

  package "پنل مدیریت و گزارش‌ها" {

    (مشاهده داشبورد مدیریتی) as UC\_Dashboard

    (مدیریت کاربران) as UC\_ManageUsers

    (مشاهده گزارش‌ها) as UC\_Reports

  }

  package "فرایندهای سیستمی" {

    (ارسال OTP) as UC\_SendOTP

    (اعتبارسنجی OTP) as UC\_ValidateOTP

    (ثبت تاریخچه تغییرات) as UC\_LogHistory

    (انقضای نشست کاربر) as UC\_SessionTimeout

    (ارسال پیامک اطلاع‌رسانی) as UC\_SendNotification

  }

}

Guest \--\> UC\_Login

SystemUser \--\> UC\_ViewProfile

SystemUser \--\> UC\_EditProfile

SystemUser \--\> UC\_ChangePhone

SystemUser \--\> UC\_DeleteRequest

Admin \--\> UC\_Dashboard

Admin \--\> UC\_AddProduct

Admin \--\> UC\_EditProduct

Admin \--\> UC\_ChangePrice

Admin \--\> UC\_ChangeStock

Admin \--\> UC\_ToggleProduct

Admin \--\> UC\_DeleteProduct

Admin \--\> UC\_ProductHistory

Admin \--\> UC\_OrderList

Admin \--\> UC\_ViewOrderDetails

Admin \--\> UC\_AssignVisitor

Admin \--\> UC\_AdminCancelOrder

Admin \--\> UC\_OrderStatusHistory

Admin \--\> UC\_CreateVisitor

Admin \--\> UC\_ToggleVisitor

Admin \--\> UC\_VisitorPerformance

Admin \--\> UC\_ManageUsers

Admin \--\> UC\_Reports

UC\_Login ..\> UC\_SendOTP : \<\<include\>\>

UC\_Login ..\> UC\_ValidateOTP : \<\<include\>\>

UC\_ChangePhone ..\> UC\_SendOTP : \<\<include\>\>

UC\_ChangePhone ..\> UC\_ValidateOTP : \<\<include\>\>

UC\_AddProduct ..\> UC\_LogHistory : \<\<include\>\>

UC\_EditProduct ..\> UC\_LogHistory : \<\<include\>\>

UC\_ChangePrice ..\> UC\_LogHistory : \<\<include\>\>

UC\_ChangeStock ..\> UC\_LogHistory : \<\<include\>\>

UC\_DeleteProduct ..\> UC\_LogHistory : \<\<include\>\>

UC\_AssignVisitor ..\> UC\_SendNotification : \<\<include\>\>

UC\_AdminCancelOrder ..\> UC\_LogHistory : \<\<include\>\>

UC\_ToggleVisitor ..\> UC\_LogHistory : \<\<include\>\>

UC\_SessionTimeout ..\> UC\_Login : \<\<extend\>\>

UC\_SendOTP \--\> SMS

UC\_SendNotification \--\> SMS

UC\_AddProduct \--\> Storage

@enduml

---

## **۳) نمودار ویزیتور**

@startuml visitor\_uc\_03

title Use Case Diagram \- ویزیتور

left to right direction

skinparam packageStyle rectangle

skinparam shadowing false

skinparam backgroundColor white

skinparam defaultFontName Vazirmatn

skinparam actorStyle awesome

skinparam linetype ortho

actor "مهمان" as Guest

actor "کاربر سیستم" as SystemUser

actor "ویزیتور" as Visitor

actor "سرویس پیامک" as SMS

Visitor \--|\> SystemUser

rectangle "اپلیکیشن فروش محصولات پلاستیکی" as System {

  package "حساب کاربری" {

    (ورود با OTP) as UC\_Login

    (مشاهده پروفایل) as UC\_ViewProfile

    (ویرایش پروفایل) as UC\_EditProfile

    (تغییر شماره تلفن) as UC\_ChangePhone

    (درخواست حذف حساب کاربری) as UC\_DeleteRequest

  }

  package "سفارش‌های تخصیص‌یافته" {

    (مشاهده سفارش‌های تخصیص‌یافته) as UC\_AssignedOrders

    (مشاهده جزئیات سفارش) as UC\_ViewOrderDetails

    (تغییر وضعیت به «بارگیری شد») as UC\_Loaded

    (تغییر وضعیت به «تحویل داده شد») as UC\_Delivered

  }

  package "فرایندهای سیستمی" {

    (ارسال OTP) as UC\_SendOTP

    (اعتبارسنجی OTP) as UC\_ValidateOTP

    (ارسال پیامک اطلاع‌رسانی) as UC\_SendNotification

    (انقضای نشست کاربر) as UC\_SessionTimeout

  }

}

Guest \--\> UC\_Login

SystemUser \--\> UC\_ViewProfile

SystemUser \--\> UC\_EditProfile

SystemUser \--\> UC\_ChangePhone

SystemUser \--\> UC\_DeleteRequest

Visitor \--\> UC\_AssignedOrders

Visitor \--\> UC\_ViewOrderDetails

Visitor \--\> UC\_Loaded

Visitor \--\> UC\_Delivered

UC\_Login ..\> UC\_SendOTP : \<\<include\>\>

UC\_Login ..\> UC\_ValidateOTP : \<\<include\>\>

UC\_ChangePhone ..\> UC\_SendOTP : \<\<include\>\>

UC\_ChangePhone ..\> UC\_ValidateOTP : \<\<include\>\>

UC\_Loaded ..\> UC\_SendNotification : \<\<include\>\>

UC\_Delivered ..\> UC\_SendNotification : \<\<include\>\>

UC\_SessionTimeout ..\> UC\_Login : \<\<extend\>\>

UC\_SendOTP \--\> SMS

UC\_SendNotification \--\> SMS

@enduml

---

## **۴) نکات هماهنگی**

* `SystemUser` فقط برای قابلیت‌های مشترک کاربران واردشده استفاده شده است.  
* `Guest` فقط برای `ثبت‌نام` و `ورود` به کار رفته است.  
* `UC_ViewOrderDetails` به‌صورت مستقل به نقش‌های مرتبط متصل است.  
* فلش بازیگران خارجی از Use Case به سمت `SMS` و `Storage` است.  
* برای خوانایی، `linetype ortho` فعال شده است.

---

## ۱۰) جمع‌بندی نهایی

این نسخه با Specification نهایی هماهنگ شده و عنوان‌ها نیز یکدست شده‌اند.  
برای مرحله بعد، این دو بخش برای ورود به **Activity Diagram**، **ERD** و سپس **API Design** آماده‌اند.