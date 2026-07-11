# 🏭 Plastic Products App

مخزن رسمی مستندات و فایل‌های فنی پروژه **اپلیکیشن فروش محصولات پلاستیکی**.

---

## 📌 وضعیت پروژه

| **مرحله**                            |  **وضعیت**  |
| :----------------------------------- | :---------: |
| تحلیل نیازمندی‌ها                    | ✅ انجام شده |
| مستندات نیازمندی‌های نرم‌افزار (SRS) | ✅ انجام شده |
| Use Case Specification               | ✅ انجام شده |
| Use Case Diagram                     | ✅ انجام شده |
| Activity Diagram                     | ✅ انجام شده |
| ERD                                  | ✅ انجام شده |
| API Specification                    | ✅ انجام شده |
| لیست Endpointها                      | ✅ انجام شده |
| Database DDL                         | ✅ انجام شده |
| معماری کلان (Architecture Diagram)   | ✅ انجام شده |
| پیاده‌سازی Backend                   |  ⏳ در آینده |
| پیاده‌سازی اپلیکیشن اندروید          |  ⏳ در آینده |

---

## 📂 ساختار مخزن

```text
plastic-products-app/
├── README.md
├── .gitignore
├── docs/
│   ├── source/
│   │   ├── requirements.md
│   │   ├── srs.md
│   │   ├── use-case-specification.md
│   │   ├── use-case-diagram.puml
│   │   ├── activity-diagram.puml
│   │   ├── erd.md
│   │   ├── api-specification.md
│   │   ├── endpoint-list.md
│   │   └── architecture-diagram.puml
│   └── export/
│       ├── requirements.pdf
│       ├── srs.pdf
│       ├── use-case-specification.pdf
│       ├── use-case-diagram.png
│       ├── activity-diagram.png
│       ├── erd.png
│       ├── api-specification.pdf
│       ├── endpoint-list.pdf
│       └── architecture-diagram.png
├── database/
│   ├── schema.sql
│   ├── seed.sql
│   └── down.sql
└── backend/
    └── (کدهای Django آینده)
```

---

### 📁 `docs/source/`

فایل‌های منبع (قابل ویرایش) مستندات پروژه:

| **فایل**                    | **توضیح**                                  |
| :-------------------------- | :----------------------------------------- |
| `requirements.md`           | پرسشنامه نیازمندی‌های تکمیل‌شده با کارفرما |
| `srs.md`                    | مستندات نیازمندی‌های نرم‌افزار             |
| `use-case-specification.md` | مشخصات کامل ۵۴ Use Case                    |
| `use-case-diagram.puml`     | نمودار Use Case (PlantUML)                 |
| `activity-diagram.puml`     | نمودار فعالیت (PlantUML)                   |
| `erd.md`                    | مستندات نمودار موجودیت‌-رابطه              |
| `api-specification.md`      | مستندات کامل APIها                         |
| `endpoint-list.md`          | لیست کامل Endpointها                       |
| `architecture-diagram.puml` | نمودار معماری کلان سیستم (PlantUML)        |

---

### 📁 `docs/export/`

خروجی‌های نهایی مستندات برای ارائه (PDF و PNG):

| **فایل**                     | **توضیح**                       |
| :--------------------------- | :------------------------------ |
| `requirements.pdf`           | نسخه PDF پرسشنامه               |
| `srs.pdf`                    | نسخه PDF SRS                    |
| `use-case-specification.pdf` | نسخه PDF Use Case Specification |
| `use-case-diagram.png`       | تصویر نمودار Use Case           |
| `activity-diagram.png`       | تصویر نمودار فعالیت             |
| `erd.png`                    | تصویر ERD                       |
| `api-specification.pdf`      | نسخه PDF مستندات API            |
| `endpoint-list.pdf`          | نسخه PDF لیست Endpointها        |
| `architecture-diagram.png`   | تصویر نمودار معماری کلان        |

---

### 📁 `database/`

اسکریپت‌های دیتابیس PostgreSQL:

| **فایل**     | **توضیح**                           |
| :----------- | :---------------------------------- |
| `schema.sql` | اسکریپت اصلی ایجاد ساختار دیتابیس   |
| `seed.sql`   | اسکریپت وارد کردن داده‌های اولیه    |
| `down.sql`   | اسکریپت حذف کامل دیتابیس (Rollback) |

---

### 📁 `backend/`

پوشه‌ی خالی برای کدهای پیاده‌سازی آینده (Django REST Framework).

---

## 🛠️ فناوری‌ها

| **بخش**                     | **فناوری**                         |
| :-------------------------- | :--------------------------------- |
| **پایگاه داده**             | PostgreSQL                         |
| **مستندات**                 | Markdown, PlantUML                 |
| **بک‌اند (آینده)**          | Django REST Framework (Python)     |
| **اپلیکیشن موبایل (آینده)** | Android / Kotlin + Jetpack Compose |
| **احراز هویت**              | JWT                                |
| **سرویس پیامک**             | وب‌سرویس SMS Gateway               |
| **ذخیره‌سازی تصاویر**       | فضای ابری (Cloud Storage)          |

---

## 🏗️ معماری کلان سیستم

نمودار معماری کلان سیستم در فایل‌های زیر قابل مشاهده است:

* **منبع:** `docs/source/architecture-diagram.puml`
* **خروجی:** `docs/export/architecture-diagram.png`

این نمودار مؤلفه‌های اصلی سیستم را نشان می‌دهد:

```text
کاربر (موبایل) → اپلیکیشن اندروید → API Backend → PostgreSQL
                                  ↘
                                   سرویس پیامک
                                  ↘
                                   ذخیره‌سازی تصاویر
```

احراز هویت با JWT در لایه‌ی Backend مدیریت می‌شود.

---

## 🧾 مستندات اصلی

| **مستند**                            | **مسیر**                                |
| :----------------------------------- | :-------------------------------------- |
| پرسشنامه نیازمندی‌ها                 | `docs/source/requirements.md`           |
| مستندات نیازمندی‌های نرم‌افزار (SRS) | `docs/source/srs.md`                    |
| Use Case Specification               | `docs/source/use-case-specification.md` |
| Use Case Diagram                     | `docs/source/use-case-diagram.puml`     |
| Activity Diagram                     | `docs/source/activity-diagram.puml`     |
| ERD                                  | `docs/source/erd.md`                    |
| API Specification                    | `docs/source/api-specification.md`      |
| لیست Endpointها                      | `docs/source/endpoint-list.md`          |
| معماری کلان سیستم                    | `docs/source/architecture-diagram.puml` |

---

## 🗄️ دیتابیس

فایل‌های دیتابیس در مسیر زیر قرار دارند:

| **فایل**              | **توضیح**                |
| :-------------------- | :----------------------- |
| `database/schema.sql` | ایجاد ساختار جداول       |
| `database/seed.sql`   | وارد کردن داده‌های اولیه |
| `database/down.sql`   | حذف کامل ساختار دیتابیس  |

---

## ▶️ ترتیب اجرای اسکریپت‌ها

1. اجرای `schema.sql` برای ایجاد ساختار دیتابیس
2. اجرای `seed.sql` برای وارد کردن داده‌های اولیه
3. در صورت نیاز، اجرای `down.sql` برای حذف کامل ساختار دیتابیس

---

## 👤 توسعه‌دهنده

* **Mohammad Qavidel Heydari** ([@CodeCrafter812025](https://github.com/CodeCrafter812025))
* **نام پروژه:** اپلیکیشن فروش محصولات پلاستیکی
* **سال توسعه:** ۱۴۰۵

---

## 📝 نکته

این مخزن در حال توسعه است و فایل‌های جدید به‌مرور به آن اضافه خواهند شد. برای مشاهده‌ی آخرین تغییرات، لاگ کامیت‌ها را بررسی کنید.
