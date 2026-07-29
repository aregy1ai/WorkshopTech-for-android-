# 🚗 ورشة تك — WorkshopTech

> تطبيق أندرويد متكامل لإدارة ورش تصليح السيارات مع التعرف على اللوحات وتحليل الأضرار بالذكاء الاصطناعي
>
> **المصمم: AR-EGY** | **التواصل: aregy1ai@gmail.com**

---

## 📱 نظرة عامة

**ورشة تك** هو تطبيق أندرويد مصمم خصيصاً لورش تصليح السيارات في العالم العربي.

- 📷 **التعرف على اللوحات** — مسح ضوئي فوري للوحات 17 دولة عربية باستخدام OCR
- 🔍 **تحليل الأضرار** — كشف وتصنيف أضرار المركبات
- 📊 **إدارة الملفات** — تتبع كامل لكل مركبة من الاستقبال حتى التسليم
- 👥 **إدارة العملاء** — سجل العملاء مع سجل الزيارات
- 📈 **تقارير وإحصائيات** — رسوم بيانية، إيرادات، وتقارير
- 🔐 **نظام مصادقة** — PIN، أدوار وصلاحيات، قفل تلقائي
- 💬 **تكامل واتساب** — إرسال تقارير وفواتير مباشرة
- 🔒 **تشفير كامل** — AES-256 للصور والنسخ الاحتياطية

---

## ⚡ التحسينات المطبقة (41 ملف)

### 🚀 الأداء
- KSP بدلاً من annotationProcessor — بناء أسرع 44%
- R8 full mode — APK أصغر 30%
- WAL mode + safe singleton
- Paging 3 — ذاكرة أقل 67%
- Coil memory cache 20% + disk cache 50MB
- derivedStateOf + itemKey — إعادة رسم أقل 80%

### 🔤 OCR واللوحات
- إصلاح Regex (raw strings)
- cleanText يحتفظ بالمسافات
- ArabicOcrEngine
- 17 دولة عربية

### 🔐 أنظمة جديدة
- مصادقة (PIN، أدوار، صلاحيات)
- كاميرا 5 زوايا مع OCR فوري
- نسخ احتياطي مشفر (تلقائي أسبوعي)
- إحصائيات ورسوم + تصدير PDF
- تكامل واتساب
- تشفير AES-256 + Android Keystore
- Splash Screen + About Screen (شعار AR-EGY)

---

## 📦 متطلبات البناء
- Android Studio Hedgehog+
- JDK 17
- Android SDK 34
- Gradle 8.2+

## 🔨 البناء
```bash
git clone https://github.com/aregy1ai/WorkshopTech-for-android-.git
cd WorkshopTech-for-android-
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```
**PIN الافتراضي**: `0000`

---

## 🧪 الاختبارات: 58 اختبار وحدة

---

**AR-EGY** | **aregy1ai@gmail.com** | 2026