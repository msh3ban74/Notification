# Notification — دليل التشغيل والإعداد

تطبيق شامل لإدارة التذكيرات، الديون، الجمعيات، والمنبهات مع مساعد ذكاء اصطناعي، بهوية بصرية نبيتي/بلاتينيوم.

---

## ✅ ما تم إصلاحه في هذه النسخة

- **تسجيل الدخول بجوجل حقيقي 100%** عبر Credential Manager الرسمي + Firebase Auth (مش وهمي ولا Demo User)
- **النسخ الاحتياطي حقيقي** عبر Firestore، بيرفع وينزّل بيانات التذكيرات/الديون/الجمعيات/المنبهات/الملاحظات فعليًا
- **اسم الحزمة (Package)** اتغيّر من `com.example` الافتراضي إلى `com.notification.app`
- **تصحيح خطأ في ملف أيقونة التطبيق** كان ممكن يسبب Crash عند فتح التطبيق
- كل النصوص خالية من أي ذكر لـ "Gemini" في الواجهة

---

## 🔧 خطوات التشغيل (لازم تعملها قبل أي بناء)

### 1) مفتاح Gemini API
أنشئ ملف `.env` في جذر المشروع وحط فيه:
```
GEMINI_API_KEY=مفتاحك_هنا
```

### 2) إعداد Firebase (لتسجيل الدخول والنسخ الاحتياطي)
1. روح على [Firebase Console](https://console.firebase.google.com) وأنشئ مشروع جديد
2. أضف تطبيق أندرويد بحزمة اسمها بالظبط: `com.notification.app`
3. نزّل ملف `google-services.json` وحطه جوه مجلد `/app` في المشروع
4. من القائمة الجانبية: **Authentication → Sign-in method** وفعّل **Google**
5. من **Project Settings → General → Your apps**، انسخ **Web client ID**
6. أنشئ ملف `local.properties` في جذر المشروع (لو مش موجود) وضيف فيه السطر ده:
   ```
   GOOGLE_WEB_CLIENT_ID=الكود_اللي_نسخته.apps.googleusercontent.com
   ```
7. من **Firestore Database**، أنشئ قاعدة بيانات جديدة، وحط قواعد الأمان دي (مهم جدًا حتى لا يقدر أي شخص يقرأ بيانات غيره):
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

بدون الخطوات دي، زر "تسجيل الدخول بجوجل" هيديك رسالة خطأ واضحة (مش هيتظاهر إنه نجح).

### 3) توقيع التطبيق (Signing)
شغّل `create-release-key.sh` لإنشاء الـ Keystore، أو احذف سطر `signingConfig` من `app/build.gradle.kts` مؤقتًا لو عايز تبني نسخة Debug بس للتجربة.

### 4) ⚠️ مشكلة gradle-wrapper.jar (مهم لو بتشتغل من الموبايل)
ملف `gradle/wrapper/gradle-wrapper.jar` غير موجود في هذه الحزمة (الأدوات اللي بتولّد المشاريع زي AI Studio مش دايمًا بترفعه). لازم تضيفه قبل أي بناء عن طريق واحدة من الطريقتين:

**الطريقة الأسهل من الموبايل (GitHub Codespaces):**
1. ادخل ريبو المشروع على GitHub
2. دوس **Code → Codespaces → Create codespace on main**
3. لما يفتح التيرمينال، اكتب:
   ```
   gradle wrapper
   ```
4. بعد ما يخلص، اعمل commit و push للملف الجديد اللي هيظهر في `gradle/wrapper/gradle-wrapper.jar`

**من جهاز فيه Android Studio:**
افتح المشروع في Android Studio وهو هيولّد الملف تلقائيًا أول ما يعمل Sync.

---

## 📦 البنية العامة
- `data/auth` — تسجيل الدخول بجوجل (Credential Manager + Firebase)
- `data/repository/BackupRepository.kt` — النسخ الاحتياطي مع Firestore
- `data/local` — قاعدة البيانات المحلية (Room)
- `ui/screens` — كل شاشات التطبيق
- `domain` — منطق الحسابات (الديون، الجمعيات، مواقيت الصلاة)

---

## ما زال يحتاج مراجعتك اليدوية
- إدخال بيانات Firebase الحقيقية بتاعتك (خطوة 2 أعلاه) — الكود جاهز لكن محتاج مفاتيحك الخاصة
- توليد `gradle-wrapper.jar` (خطوة 4 أعلاه)
- بعد أول بناء ناجح، جرب تسجيل الدخول والنسخ الاحتياطي فعليًا وتأكد إن البيانات بتظهر في Firestore Console
