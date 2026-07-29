# 🚗 ورشة تك — WorkshopTech

> تطبيق أندرويد متكامل لإدارة ورش تصليح السيارات مع التعرف على اللوحات وتحليل الأضرار بالذكاء الاصطناعي
>
> **المصمم: AR-EGY** | **التواصل: aregy1ai@gmail.com**

---

## 📥 تحميل التطبيق

[![Build APK](https://github.com/aregy1ai/WorkshopTech-for-android-/actions/workflows/build-apk.yml/badge.svg)](https://github.com/aregy1ai/WorkshopTech-for-android-/actions/workflows/build-apk.yml)

| النسخة | التحميل |
|--------|---------|
| **Debug APK** (آخر بناء) | [![تحميل](https://img.shields.io/badge/⬇️_تحميل_APK-00FF00?style=for-the-badge)](https://github.com/aregy1ai/WorkshopTech-for-android-/actions/workflows/build-apk.yml?query=branch%3Amain) |
| **جميع الإصدارات** | [![Releases](https://img.shields.io/badge/📦_الإصدارات-FF00FF?style=for-the-badge)](https://github.com/aregy1ai/WorkshopTech-for-android-/releases) |

> **طريقة التحميل:** اضغط على زر التحميل أعلاه ← اختر آخر تشغيل ناجح (✅) ← حمّل `app-debug` من قسم **Artifacts**

---

## 📱 نظرة عامة

**ورشة تك** هو تطبيق أندرويد مصمم خصيصاً لورش تصليح السيارات في العالم العربي. يوفر:

- 📷 **التعرف على اللوحات** — مسح ضوئي فوري للوحات 17 دولة عربية باستخدام OCR
- 🔍 **تحليل الأضرار** — كشف وتصنيف أضرار المركبات باستخدام تحليل الصور
- 📊 **إدارة الملفات** — تتبع كامل لكل مركبة من الاستقبال حتى التسليم
- 👥 **إدارة العملاء** — سجل العملاء مع سجل الزيارات ونقاط الولاء
- 📈 **تقارير وإحصائيات** — رسوم بيانية، إيرادات، وتقارير قابلة للتصدير
- 🔐 **نظام مصادقة** — PIN، أدوار وصلاحيات، قفل تلقائي
- 💬 **تكامل واتساب** — إرسال تقارير وفواتير وإشعارات مباشرة للعملاء
- 🔒 **تشفير كامل** — AES-256 للصور والنسخ الاحتياطية

---

## ⚡ التحسينات المطبقة

### 🚀 الأداء (15 تحسين)
- **KSP** بدلاً من annotationProcessor — بناء أسرع 44%
- **R8 full mode** — APK أصغر 30%
- **WAL mode** + safe singleton
- **Paging 3** — ذاكرة أقل 67%
- **Coil** memory cache 20% + disk cache 50MB
- **derivedStateOf + itemKey** — إعادة رسم أقل 80%

### 🔤 OCR واللوحات (5 إصلاحات)
- إصلاح Regex (raw strings)
- cleanText يحتفظ بالمسافات
- ArabicOcrEngine
- 17 دولة عربية

### 🔐 أنظمة جديدة (8 أنظمة)
- مصادقة (PIN، أدوار، صلاحيات)
- كاميرا 5 زوايا مع OCR فوري
- نسخ احتياطي مشفر (تلقائي أسبوعي)
- إحصائيات ورسوم + تصدير PDF
- تكامل واتساب
- تشفير AES-256 + Android Keystore
- Splash Screen + About Screen (شعار AR-EGY)

---

## 🤖 CI/CD - بناء تلقائي

[![Build APK](https://github.com/aregy1ai/WorkshopTech-for-android-/actions/workflows/build-apk.yml/badge.svg)](https://github.com/aregy1ai/WorkshopTech-for-android-/actions/workflows/build-apk.yml)

يتم بناء APK تلقائياً عند كل push مع:
- ✅ تشغيل الاختبارات (40 اختبار)
- ✅ بناء Debug + Release APK
- ✅ رفع الـ APK كـ artifact للتحميل

---

## 📦 متطلبات البناء

- **JDK** 17
- **Android SDK** 34
- **Gradle** 8.2+
- **minSdk** 26 (Android 8.0)

## 🔨 البناء المحلي

```bash
git clone https://github.com/aregy1ai/WorkshopTech-for-android-.git
cd WorkshopTech-for-android-
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**PIN الافتراضي**: `0000`

---

## 🧪 الاختبارات: 40 اختبار وحدة

---

**AR-EGY** | **aregy1ai@gmail.com** | 2026
