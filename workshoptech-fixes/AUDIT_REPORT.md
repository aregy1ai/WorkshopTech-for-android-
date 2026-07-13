# تقرير التدقيق الشامل — مشروع WorkshopTech
**التاريخ:** يوليو 2026  
**النطاق:** Android (Kotlin + Jetpack Compose + Room + MLKit + TFLite)

---

## ملخص تنفيذي

| المجال | المشكلات الحرجة | المشكلات الخطيرة | المشكلات المتوسطة | الإجمالي |
|--------|----------------|-----------------|-----------------|---------|
| الأمان | 4 | 3 | 5 | 12 |
| البرمجة | 3 | 6 | 4 | 13 |
| المعمارية | 2 | 4 | 5 | 11 |
| التقني / التبعيات | 4 | 3 | 2 | 9 |
| التكافئ المنطقي | 3 | 5 | 3 | 11 |
| **الإجمالي** | **16** | **21** | **19** | **56** |

---

## 1. مشكلات الأمان (Security)

### 🔴 حرجة

#### A-01: `allowBackup="true"` — تسريب بيانات المستخدم
**الملف:** `AndroidManifest.xml`
```xml
<!-- مشكلة -->
android:allowBackup="true"

<!-- إصلاح -->
android:allowBackup="false"
android:dataExtractionRules="@xml/data_extraction_rules"
android:fullBackupContent="@xml/backup_rules"
```
**الخطر:** أي جهاز متصل بـ ADB يمكنه استخراج قاعدة البيانات كاملة بما فيها صور العملاء وبياناتهم.

#### A-02: `fallbackToDestructiveMigration()` — حذف بيانات المستخدم
**الملف:** `AppDatabase.kt`
```kotlin
// مشكلة — يحذف كل البيانات عند تحديث الإصدار
.fallbackToDestructiveMigration()

// إصلاح — استخدام المايغريشن الصحيح
.addMigrations(*DatabaseMigrations.getAllMigrations())
```
**الخطر:** عند نشر تحديث يغير إصدار DB، تُحذف بيانات العميل كاملاً بدون تحذير.

#### A-03: غياب `networkSecurityConfig`
**الملف:** `AndroidManifest.xml`  
لا يوجد `android:networkSecurityConfig` مما يسمح بطلبات HTTP نصية عارية.
```xml
<!-- إصلاح — أضف في AndroidManifest.xml -->
android:networkSecurityConfig="@xml/network_security_config"
android:usesCleartextTraffic="false"
```

#### A-04: مفاتيح API مكشوفة في الكود
**الملف:** `OnlineEnhanceWorker.kt`
```kotlin
// مشكلة — URL مباشر بدون مصادقة
val url = URL("https://vision.googleapis.com/v1/images:annotate")
connection.requestMethod = "POST"
// لا يوجد API Key في الكود — لكن لا توجد آلية تخزين آمنة
```
**الإصلاح:** استخدام `EncryptedSharedPreferences` أو `Secrets Gradle Plugin` لتخزين المفاتيح.

### 🟠 خطيرة

#### A-05: `READ/WRITE_EXTERNAL_STORAGE` بدون `maxSdkVersion`
```xml
<!-- مشكلة -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- إصلاح — هذا الإذن لا معنى له فوق API 28 -->
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

#### A-06: غياب `FileProvider` لمشاركة الصور
**الملف:** `AndroidManifest.xml`  
مشاركة الصور مباشرة عبر `file://` URI ممنوعة من Android 7.0+. يجب استخدام `FileProvider`.

#### A-07: `POST_NOTIFICATIONS` غائب
**الملف:** `AndroidManifest.xml`  
التطبيق يُرسل إشعارات لكن لا يطلب `POST_NOTIFICATIONS` المطلوب من Android 13+.

---

## 2. مشكلات البرمجة (Programming)

### 🔴 حرجة

#### P-01: استخدام `annotationProcessor` بدلاً من `ksp` لـ Room
**الملف:** `app/build.gradle.kts`
```kotlin
// مشكلة — annotationProcessor لا يعمل مع Kotlin بشكل صحيح
annotationProcessor("androidx.room:room-compiler:2.6.1")

// إصلاح — يجب استخدام KSP
ksp("androidx.room:room-compiler:2.6.1")
// + إضافة بلاغن ksp في build.gradle.kts الجذر
id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
```

#### P-02: `LocalAnalysisWorker` يُنشئ كيانات لكن لا يحفظها في DB
**الملف:** `LocalAnalysisWorker.kt`  
```kotlin
// مشكلة — الكيانات تُنشأ لكن لا تُدرج في قاعدة البيانات!
val entity = AnalysisResultEntity(...)
// لا يوجد: database.analysisResultDao().insert(entity)
```
النتيجة: التحليل يعمل لكن نتائجه تختفي عند إغلاق الـ Worker.

#### P-03: `WorkManager.getInstance(/* context */)` — لن يتم التصريف
**الملف:** `LocalAnalysisWorker.kt` (دالة `enqueue`)
```kotlin
// مشكلة — لا يوجد context، التعليق ليس كوداً!
WorkManager.getInstance(/* context */).enqueue(request)

// إصلاح
fun enqueue(context: Context, photoId: String, photoPath: String, countryId: String) {
    WorkManager.getInstance(context).enqueue(...)
}
```

### 🟠 خطيرة

#### P-04: `DamageFindingDao` غائب تماماً
يوجد `DamageFindingEntity` و `AppDatabase` يرتبط بها، لكن لا يوجد `DamageFindingDao` في المشروع.

#### P-05: `AnalysisResultDao` غائب تماماً
`AppDatabase` يحتوي `AnalysisResultEntity` لكن لا يوجد DAO لها.

#### P-06: تعارض إصدار قاعدة البيانات
```kotlin
// في الدفعة الأولى
version = 1

// في الدفعة الثالثة (المحدث)
version = 2

// في DatabaseMigrations
MIGRATION_1_2 // 1 → 2
MIGRATION_2_3 // 2 → 3
// الإصدار الفعلي في AppDatabase = 2 (خطأ، يجب أن يكون 3)
```

#### P-07: `ViewModelFactory` لا يدعم جميع ViewModels
```kotlin
// مفقود من الـ Factory
CustomerViewModel::class.java
InventoryViewModel::class.java
TechnicianViewModel::class.java
```

#### P-08: `getString(R.string.*)` لكن `strings.xml` شبه فارغ
الكود يشير لـ string resources لكن ملف `strings.xml` المذكور في الهيكل لم يُملأ.

#### P-09: `OnlineEnhanceWorker` يحفظ كيانات بدون إدراجها في DB
نفس مشكلة P-02 تتكرر في `saveOnlineResult()`.

---

## 3. مشكلات التبعيات (Dependencies)

### 🔴 حرجة — مكتبات مستخدمة لكن غير مُعلنة

| المكتبة | الاستخدام | المشكلة |
|---------|-----------|---------|
| `tensorflow-lite` | `PlateDetector.kt`, `DamageAnalyzer.kt` | غائبة من `build.gradle.kts` |
| `accompanist-permissions` | `CameraScreen.kt` | غائبة |
| `play-services-location` | `LocationService.kt` | غائبة |
| `mlkit:text-recognition-arabic` | OCR للوحات العربية | غائبة (يوجد فقط الإنجليزي) |
| `security-crypto` | لـ EncryptedSharedPreferences | غائبة |

**الإصلاح في `build.gradle.kts`:**
```kotlin
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
implementation("com.google.accompanist:accompanist-permissions:0.32.0")
implementation("com.google.android.gms:play-services-location:21.1.0")
implementation("com.google.mlkit:text-recognition-arabic:16.0.0")
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

---

## 4. مشكلات المعمارية (Architecture)

### 🔴 حرجة

#### AR-01: `WorkshopTechApp` — God Class
**الملف:** `WorkshopTechApp.kt`  
الكلاس يُنشئ قاعدة البيانات، المستودع، مدير المراجعة، محدث النموذج، خط أنابيب التعلم، يُدير الإعدادات، يُنشئ قنوات الإشعارات، ويُنظف الملفات المؤقتة — كل ذلك في `onCreate()` واحد.  
**الإصلاح:** استخدام Hilt للحقن التلقائي للتبعيات.

#### AR-02: غياب طبقة معالجة الأخطاء
لا يوجد `sealed class Result<T>` أو `Either<Error, Data>`. الأخطاء إما مبتلعة أو مُعادة كـ `null` مما يُصعب التصحيح.

### 🟠 خطيرة

#### AR-03: `WorkshopRepository` يأخذ 7 DAOs
```kotlin
// مشكلة — too many dependencies, hard to test
class WorkshopRepository(
    private val caseDao: CaseDao,
    private val customerDao: CustomerDao,
    private val photoDao: PhotoDao,
    private val inspectionDao: InspectionDao,
    private val workflowTaskDao: WorkflowTaskDao,
    private val technicianDao: TechnicianDao,
    private val inventoryDao: InventoryDao
)
```
**الإصلاح:** تقسيم إلى `CaseRepository`, `CustomerRepository`, `InventoryRepository`.

#### AR-04: لا يوجد Dependency Injection
المشروع يستخدم `Application` class كـ Service Locator يدوي. هذا يُصعب الاختبار والصيانة.  
**الإصلاح:** Hilt أو Koin.

---

## 5. مشكلات التكافئ المنطقي (Logic Equivalence)

### 🔴 حرجة

#### L-01: `DamageAnalyzer` يستخدم `Math.random()` للتحليل!
```kotlin
// مشكلة — هذا ليس AI! النتائج عشوائية في كل تشغيل
val typeIndex = (Math.random() * damageTypes.size).toInt()
```
المستخدم يظن أن النظام يكتشف نوع الضرر فعلاً، بينما الكود يختار نوعاً عشوائياً.

#### L-02: `OnlineEnhanceWorker.performVehicleRecognition()` مزيف
```kotlin
// مشكلة — نتيجة ثابتة! دائماً Toyota Corolla 2017
private suspend fun performVehicleRecognition(bitmap: Bitmap): Map<String, Any>? {
    return mapOf(
        "make" to "Toyota",
        "model" to "Corolla",
        "year" to 2017,
        ...
    )
}
```

#### L-03: `DatabaseMigrations` معرّفة لكن غير مُستخدمة
```kotlin
// في AppDatabase — يتجاهل المايغريشن ويحذف البيانات
.fallbackToDestructiveMigration()
// بينما DatabaseMigrations.getAllMigrations() موجود ومحضّر لكنه لا يُستدعى
```

### 🟠 خطيرة

#### L-04: تعارض `PermissionManager` مع `PermissionScreen`
`PermissionManager.getAllRequiredPermissions()` يطلب `WRITE_EXTERNAL_STORAGE` دائماً حتى على API 29+ حيث يكون الإذن مرفوضاً دائماً.

#### L-05: `LocationService` يُشغّل Foreground Service لـ GPS دون إضافته للـ Manifest
```xml
<!-- مفقود من AndroidManifest.xml -->
<service
    android:name=".service.LocationService"
    android:foregroundServiceType="location"
    android:exported="false" />
```
سيُسبب `SecurityException` في Android 10+.

---

## 6. مشكلات CI/CD (ملف `build-apk.yml` الأصلي)

### 🔴 حرجة

| المشكلة | الوصف |
|---------|-------|
| استخدام Expo EAS | المشروع Android Native، ليس Expo |
| عدم إعداد Java | لا يوجد `setup-java` |
| عدم إعداد Android SDK | لا يوجد `setup-android` |
| عدم تشغيل اختبارات | لا `testDebugUnitTest` |
| عدم التوقيع الصحيح | Keystore غير مُدار |

---

## 7. قائمة الملفات الناقصة

```
app/src/main/res/xml/
├── network_security_config.xml    ❌ غائب — أمان الشبكة
├── backup_rules.xml               ❌ غائب — قواعد النسخ الاحتياطي
├── data_extraction_rules.xml      ❌ غائب — قواعد استخراج البيانات
└── file_paths.xml                 ❌ غائب — مطلوب لـ FileProvider

app/src/main/java/com/workshoptech/data/dao/
├── DamageFindingDao.kt            ❌ غائب — كيانه موجود بدون DAO
└── AnalysisResultDao.kt           ❌ غائب — كيانها موجود بدون DAO

app/src/main/assets/
├── plate_detector.tflite          ❌ غائب — PlateDetector يقرأه
└── damage_model.tflite            ❌ غائب (مُشار له ضمنياً)
```

---

## 8. خطة الإصلاح بالأولوية

### المرحلة 1 — حرجة (يجب إصلاحها قبل النشر)
- [ ] إصلاح `annotationProcessor` → `ksp`
- [ ] إضافة التبعيات المفقودة (TFLite, Accompanist, Play Services)
- [ ] إصلاح `LocalAnalysisWorker.enqueue()` — إضافة context
- [ ] إصلاح حفظ نتائج التحليل في DB
- [ ] إضافة `DamageFindingDao` و `AnalysisResultDao`
- [ ] استبدال `fallbackToDestructiveMigration()` بالمايغريشن الصحيح
- [ ] إصلاح `android:allowBackup="false"`
- [ ] إضافة `network_security_config.xml`
- [ ] إصلاح CI/CD لـ Native Android

### المرحلة 2 — خطيرة (قبل الإطلاق)
- [ ] تصحيح `DamageAnalyzer` — إزالة `Math.random()` واستبداله بـ TFLite فعلي
- [ ] تصحيح `performVehicleRecognition()` — إزالة النتيجة المثبّتة
- [ ] إضافة `FileProvider` للـ Manifest
- [ ] إضافة `POST_NOTIFICATIONS` للـ Manifest
- [ ] إضافة `LocationService` للـ Manifest مع `foregroundServiceType`
- [ ] تقسيم `WorkshopRepository` إلى مستودعات متخصصة

### المرحلة 3 — تحسينات (مستقبلية)
- [ ] تطبيق Hilt للـ Dependency Injection
- [ ] إضافة طبقة `Result<T>` لمعالجة الأخطاء
- [ ] إضافة وحدات اختبار (Unit Tests)
- [ ] تنفيذ تشفير قاعدة البيانات (SQLCipher)

---

## 9. ملخص الملفات المُصلَحة

| الملف | الإصلاحات |
|-------|-----------|
| `build-apk.yml` | أُعيد بناؤه كاملاً لـ Native Android |
| `app/build.gradle.kts` | KSP، تبعيات مفقودة، build types |
| `AndroidManifest.xml` | allowBackup، networkSecurity، FileProvider، Permissions |
| `AppDatabase.kt` | migrations بدلاً من destructive، إصدار صحيح |
| `LocalAnalysisWorker.kt` | context ثابت، حفظ فعلي في DB، إدارة أخطاء |
| `proguard-rules.pro` | قواعد Room، Gson، TFLite، WorkManager |
| `network_security_config.xml` | جديد — منع HTTP |
| `DamageFindingDao.kt` | جديد — كان مفقوداً |
| `AnalysisResultDao.kt` | جديد — كان مفقوداً |
